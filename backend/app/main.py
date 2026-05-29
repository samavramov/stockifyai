from __future__ import annotations

from typing import List

from fastapi import Depends, FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from sqlalchemy import desc
from sqlalchemy.orm import Session

from app.database import get_db, init_db
from app.live_ingest import run_live_ingestion
from app.models import Article, DailyPrice, IngestionRun, PricePrediction, SentimentObservation, Stock
from app.stocks import DEFAULT_STOCKS

app = FastAPI(title="StockifyAI Local Sentiment + Price Pipeline", version="4.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class IngestRequest(BaseModel):
    tickers: List[str] = Field(default_factory=lambda: ["AAPL", "MSFT", "NVDA", "GOOGL", "AMZN"])
    limit: int = Field(default=5, ge=1, le=25)
    days_back: int = Field(default=90, ge=1, le=90)
    reset: bool = False
    enrich: bool = True
    source: str = "techcrunch"


@app.on_event("startup")
def startup():
    init_db()


@app.get("/api/health")
def health(db: Session = Depends(get_db)):
    return {
        "status": "ok",
        "article_count": db.query(Article).count(),
        "price_count": db.query(DailyPrice).count(),
        "stock_count": db.query(Stock).count(),
    }


@app.get("/api/config/stocks")
def config_stocks():
    return DEFAULT_STOCKS


@app.post("/api/ingest/live")
def ingest_live(req: IngestRequest):
    try:
        return run_live_ingestion(req.tickers, req.limit, req.days_back, req.reset, req.enrich, req.source)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc))


@app.get("/api/ingest/runs")
def ingestion_runs(db: Session = Depends(get_db)):
    runs = db.query(IngestionRun).order_by(desc(IngestionRun.started_at)).limit(10).all()
    return [
        {
            "id": r.id,
            "started_at": r.started_at,
            "finished_at": r.finished_at,
            "status": r.status,
            "tickers": r.tickers,
            "articles_found": r.articles_found,
            "articles_saved": r.articles_saved,
            "price_model_mae": r.price_model_mae if hasattr(r, "price_model_mae") else r.model_mae,
            "error": r.error,
        }
        for r in runs
    ]


@app.get("/api/market/summary")
def market_summary(db: Session = Depends(get_db)):
    article_count = db.query(Article).count()
    labeled_count = db.query(Article).filter(Article.diffbot_sentiment.isnot(None)).count()
    stocks_with_data = db.query(SentimentObservation.ticker).distinct().count()
    price_count = db.query(DailyPrice).count()
    prediction_count = db.query(PricePrediction).count()
    vals = [p.predicted_return for p in db.query(PricePrediction).all() if p.predicted_return is not None]
    last_run = db.query(IngestionRun).order_by(desc(IngestionRun.started_at)).first()
    return {
        "configured_stocks": len(DEFAULT_STOCKS),
        "stocks_with_data": stocks_with_data,
        "articles": article_count,
        "labeled_articles": labeled_count,
        "daily_prices": price_count,
        "predictions": prediction_count,
        "avg_predicted_return": sum(vals) / len(vals) if vals else None,
        "last_run": None if not last_run else {
            "status": last_run.status,
            "started_at": last_run.started_at,
            "finished_at": last_run.finished_at,
            "articles_found": last_run.articles_found,
            "articles_saved": last_run.articles_saved,
            "price_model_mae": getattr(last_run, "price_model_mae", None) or last_run.model_mae,
            "error": last_run.error,
        },
    }


@app.get("/api/stocks")
def list_stocks(db: Session = Depends(get_db)):
    rows = []
    configured = {s["ticker"]: s for s in DEFAULT_STOCKS}
    for ticker, info in configured.items():
        latest_sentiment = db.query(SentimentObservation).filter(SentimentObservation.ticker == ticker).order_by(desc(SentimentObservation.date)).first()
        latest_price = db.query(DailyPrice).filter(DailyPrice.ticker == ticker).order_by(desc(DailyPrice.date)).first()
        prediction = db.query(PricePrediction).filter(PricePrediction.ticker == ticker).order_by(desc(PricePrediction.prediction_date)).first()
        article_count = db.query(Article).filter(Article.ticker == ticker).count()
        rows.append({
            "ticker": ticker,
            "company": info["company"],
            "sector": info["sector"],
            "article_count": article_count,
            "sentiment": latest_sentiment.avg_model_sentiment if latest_sentiment else None,
            "date": latest_sentiment.date if latest_sentiment else None,
            "latest_price": latest_price.close if latest_price else None,
            "price_date": latest_price.date if latest_price else None,
            "predicted_price": prediction.predicted_price if prediction else None,
            "predicted_return": prediction.predicted_return if prediction else None,
            "prediction_model": prediction.model_type if prediction else None,
        })
    return rows


@app.get("/api/stocks/{ticker}")
def stock_detail(ticker: str, db: Session = Depends(get_db)):
    ticker = ticker.upper()
    info = next((s for s in DEFAULT_STOCKS if s["ticker"] == ticker), None)
    if not info:
        raise HTTPException(status_code=404, detail="Unknown ticker")
    article_count = db.query(Article).filter(Article.ticker == ticker).count()
    latest_sentiment = db.query(SentimentObservation).filter(SentimentObservation.ticker == ticker).order_by(desc(SentimentObservation.date)).first()
    latest_price = db.query(DailyPrice).filter(DailyPrice.ticker == ticker).order_by(desc(DailyPrice.date)).first()
    prediction = db.query(PricePrediction).filter(PricePrediction.ticker == ticker).order_by(desc(PricePrediction.prediction_date)).first()
    return {
        **info,
        "article_count": article_count,
        "sentiment": latest_sentiment.avg_model_sentiment if latest_sentiment else None,
        "diffbot_sentiment": latest_sentiment.avg_diffbot_sentiment if latest_sentiment else None,
        "latest_date": latest_sentiment.date if latest_sentiment else None,
        "latest_price": latest_price.close if latest_price else None,
        "price_date": latest_price.date if latest_price else None,
        "predicted_price": prediction.predicted_price if prediction else None,
        "predicted_return": prediction.predicted_return if prediction else None,
        "prediction_model": prediction.model_type if prediction else None,
        "price_model_mae": prediction.model_mae if prediction else None,
    }


@app.get("/api/stocks/{ticker}/history")
def stock_history(ticker: str, db: Session = Depends(get_db)):
    ticker = ticker.upper()
    sentiments = db.query(SentimentObservation).filter(SentimentObservation.ticker == ticker).order_by(SentimentObservation.date).all()
    prices = {p.date: p for p in db.query(DailyPrice).filter(DailyPrice.ticker == ticker).all()}
    return [
        {
            "date": r.date,
            "article_count": r.article_count,
            "model_sentiment": r.avg_model_sentiment,
            "diffbot_sentiment": r.avg_diffbot_sentiment,
            "close": prices[r.date].close if r.date in prices else None,
        }
        for r in sentiments
    ]


@app.get("/api/stocks/{ticker}/prices")
def stock_prices(ticker: str, db: Session = Depends(get_db)):
    rows = db.query(DailyPrice).filter(DailyPrice.ticker == ticker.upper()).order_by(DailyPrice.date).all()
    return [
        {"date": r.date, "open": r.open, "high": r.high, "low": r.low, "close": r.close, "volume": r.volume, "source": r.source}
        for r in rows
    ]


@app.get("/api/stocks/{ticker}/prediction")
def stock_prediction(ticker: str, db: Session = Depends(get_db)):
    prediction = db.query(PricePrediction).filter(PricePrediction.ticker == ticker.upper()).order_by(desc(PricePrediction.prediction_date)).first()
    if not prediction:
        return None
    return {
        "ticker": prediction.ticker,
        "prediction_date": prediction.prediction_date,
        "horizon_days": prediction.horizon_days,
        "current_price": prediction.current_price,
        "predicted_price": prediction.predicted_price,
        "predicted_return": prediction.predicted_return,
        "latest_sentiment": prediction.latest_sentiment,
        "article_count": prediction.article_count,
        "model_mae": prediction.model_mae,
        "model_type": prediction.model_type,
    }


@app.get("/api/stocks/{ticker}/articles")
def stock_articles(ticker: str, limit: int = 30, db: Session = Depends(get_db)):
    rows = db.query(Article).filter(Article.ticker == ticker.upper()).order_by(desc(Article.published_at), desc(Article.created_at)).limit(limit).all()
    return [
        {
            "id": r.id,
            "ticker": r.ticker,
            "company": r.company,
            "title": r.title,
            "summary": r.summary,
            "url": r.url,
            "source": r.source,
            "published_at": r.published_at,
            "diffbot_sentiment": r.diffbot_sentiment,
            "model_sentiment": r.model_sentiment,
            "label_source": r.label_source,
        }
        for r in rows
    ]
