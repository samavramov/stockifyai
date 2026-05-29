from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from typing import Dict, List

from sqlalchemy.orm import Session

from app.config import get_settings
from app.database import SessionLocal, init_db, reset_db
from app.diffbot_client import DiffbotKGClient
from app.models import Article, IngestionRun, Stock
from app.pipeline import recompute_pipeline
from app.stock_price_client import refresh_prices_for_tickers
from app.stocks import DEFAULT_STOCKS


MAX_DAYS_BACK = 90
MAX_ARTICLES_PER_TICKER = 25


def stock_map():
    return {s["ticker"]: s for s in DEFAULT_STOCKS}


def ensure_stocks(db: Session, selected_tickers: List[str]) -> None:
    all_stocks = stock_map()
    for ticker in selected_tickers:
        info = all_stocks[ticker]
        existing = db.query(Stock).filter(Stock.ticker == ticker).first()
        if not existing:
            db.add(Stock(ticker=ticker, company=info["company"], sector=info["sector"]))
    db.commit()


def save_article(db: Session, article: Dict) -> bool:
    if not article.get("url") or not article.get("title"):
        return False

    allowed = {
        "ticker",
        "company",
        "title",
        "text",
        "summary",
        "url",
        "source",
        "published_at",
        "diffbot_sentiment",
        "model_sentiment",
        "label_source",
    }
    clean = {k: v for k, v in article.items() if k in allowed}

    existing = db.query(Article).filter(Article.url == clean["url"]).first()
    if existing:
        return False

    db.add(Article(**clean))
    db.commit()
    return True


def run_live_ingestion(
    tickers: List[str],
    limit: int = 5,
    days_back: int = 90,
    reset: bool = False,
    enrich: bool = True,
    source: str = "techcrunch",
) -> Dict:
    days_back = max(1, min(int(days_back), MAX_DAYS_BACK))
    limit = max(1, min(int(limit), MAX_ARTICLES_PER_TICKER))

    if reset:
        reset_db()
    else:
        init_db()

    selected = [t.upper().strip() for t in tickers if t.upper().strip() in stock_map()]
    if not selected:
        raise ValueError("No supported tickers selected.")

    db = SessionLocal()
    run = IngestionRun(status="running", tickers=",".join(selected))
    db.add(run)
    db.commit()
    db.refresh(run)

    found = 0
    saved = 0
    errors = []
    price_refresh = {}
    client = DiffbotKGClient()
    settings = get_settings()

    try:
        ensure_stocks(db, selected)

        try:
            price_refresh = refresh_prices_for_tickers(db, selected, days_back=days_back)
        except Exception as exc:
            errors.append(f"prices: {type(exc).__name__}: {exc}")

        stocks = stock_map()
        for ticker in selected:
            info = stocks[ticker]
            try:
                articles = client.search_articles(
                    ticker=ticker,
                    company=info["company"],
                    limit=limit,
                    days_back=days_back,
                    source=source,
                    enrich=enrich,
                )
                found += len(articles)

                for article in articles:
                    if enrich or settings.enrich_with_article_api or article.get("diffbot_sentiment") is None or len(article.get("text") or "") < 300:
                        article = client.enrich_with_article_api(article)
                    if save_article(db, article):
                        saved += 1
            except Exception as exc:
                errors.append(f"{ticker}: {type(exc).__name__}: {exc}")

        price_mae = recompute_pipeline(db)
        run.status = "completed" if not errors else "completed_with_errors"
        run.finished_at = datetime.now(timezone.utc)
        run.articles_found = found
        run.articles_saved = saved
        run.model_mae = price_mae
        run.price_model_mae = price_mae
        run.error = "\n".join(errors) if errors else None
        db.commit()

        return {
            "status": run.status,
            "source": source,
            "tickers": selected,
            "articles_found": found,
            "articles_saved": saved,
            "price_rows_refreshed": price_refresh,
            "price_model_mae": price_mae,
            "errors": errors,
        }
    except Exception as exc:
        run.status = "failed"
        run.finished_at = datetime.now(timezone.utc)
        run.error = f"{type(exc).__name__}: {exc}"
        db.commit()
        raise
    finally:
        db.close()


def main():
    parser = argparse.ArgumentParser(description="Run StockifyAI live ingestion with TechCrunch, Diffbot, and free daily prices.")
    parser.add_argument("--tickers", nargs="+", default=["AAPL", "MSFT", "NVDA", "GOOGL", "AMZN"])
    parser.add_argument("--limit", type=int, default=5)
    parser.add_argument("--days-back", type=int, default=90)
    parser.add_argument("--reset", action="store_true")
    parser.add_argument("--enrich", action="store_true")
    parser.add_argument("--source", default="techcrunch")
    args = parser.parse_args()
    print(json.dumps(run_live_ingestion(args.tickers, args.limit, args.days_back, args.reset, args.enrich, args.source), indent=2))


if __name__ == "__main__":
    main()
