from __future__ import annotations

from bisect import bisect_left
from collections import defaultdict
from datetime import datetime, timezone
from typing import Dict, List, Optional, Tuple

import numpy as np
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import train_test_split
from sqlalchemy.orm import Session

from app.models import Article, DailyPrice, PricePrediction, SentimentObservation, Stock


def copy_diffbot_labels_to_article_sentiment(db: Session) -> None:
    for article in db.query(Article).filter(Article.diffbot_sentiment.isnot(None)).all():
        article.model_sentiment = article.diffbot_sentiment
    db.commit()


def _date_key(value: Optional[str]) -> Optional[str]:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value[:10]).date().isoformat()
    except Exception:
        return None


def _article_events(db: Session) -> Dict[str, Dict[str, Dict[str, float]]]:
    grouped: Dict[tuple, List[Article]] = defaultdict(list)
    for article in db.query(Article).filter(Article.diffbot_sentiment.isnot(None)).all():
        date = _date_key(article.published_at)
        if date:
            grouped[(article.ticker, date)].append(article)

    events: Dict[str, Dict[str, Dict[str, float]]] = defaultdict(dict)
    for (ticker, date), articles in grouped.items():
        vals = [a.diffbot_sentiment for a in articles if a.diffbot_sentiment is not None]
        if not vals:
            continue
        events[ticker][date] = {
            "sentiment": float(np.mean(vals)),
            "article_count": float(len(articles)),
        }
    return events


def recompute_observations(db: Session) -> None:
    db.query(SentimentObservation).delete()
    stocks = {s.ticker: s for s in db.query(Stock).all()}
    events = _article_events(db)

    for ticker, stock in stocks.items():
        price_rows = db.query(DailyPrice).filter(DailyPrice.ticker == ticker).order_by(DailyPrice.date).all()
        if not price_rows or ticker not in events:
            continue

        event_dates = sorted(events[ticker].keys())
        event_idx = 0
        active_sentiment = None
        active_count = 0

        for price in price_rows:
            price_date = _date_key(price.date)
            if not price_date:
                continue

            while event_idx < len(event_dates) and event_dates[event_idx] <= price_date:
                event = events[ticker][event_dates[event_idx]]
                active_sentiment = event["sentiment"]
                active_count = int(event["article_count"])
                event_idx += 1

            if active_sentiment is None:
                continue

            db.add(
                SentimentObservation(
                    ticker=ticker,
                    company=stock.company,
                    sector=stock.sector,
                    date=price_date,
                    article_count=active_count,
                    avg_diffbot_sentiment=float(active_sentiment),
                    avg_model_sentiment=float(active_sentiment),
                )
            )
    db.commit()


def _parse_date(date_text: str):
    try:
        return datetime.fromisoformat(date_text[:10]).date()
    except Exception:
        return None


def _nearest_price_index_on_or_after(price_rows: List[DailyPrice], date_text: str) -> Optional[int]:
    target = _parse_date(date_text)
    if target is None or not price_rows:
        return None
    dates = [_parse_date(p.date) for p in price_rows]
    valid = [(i, d) for i, d in enumerate(dates) if d is not None]
    only_dates = [d for _, d in valid]
    pos = bisect_left(only_dates, target)
    if pos >= len(valid):
        return None
    return valid[pos][0]


def _returns(price_rows: List[DailyPrice], idx: int) -> Tuple[float, float]:
    current = price_rows[idx].close
    ret_1d = (current - price_rows[idx - 1].close) / price_rows[idx - 1].close if idx >= 1 and price_rows[idx - 1].close else 0.0
    ret_5d = (current - price_rows[idx - 5].close) / price_rows[idx - 5].close if idx >= 5 and price_rows[idx - 5].close else ret_1d
    return float(ret_1d), float(ret_5d)


def _feature_vector(obs: SentimentObservation, price_rows: List[DailyPrice], idx: int) -> List[float]:
    price = price_rows[idx]
    ret_1d, ret_5d = _returns(price_rows, idx)
    sentiment = obs.avg_diffbot_sentiment if obs.avg_diffbot_sentiment is not None else 0.0
    return [
        float(price.close),
        float(sentiment),
        float(obs.article_count or 0),
        float(ret_1d),
        float(ret_5d),
        float(price.volume or 0.0),
    ]


def _training_rows(db: Session) -> Tuple[List[List[float]], List[float]]:
    X: List[List[float]] = []
    y: List[float] = []
    by_ticker: Dict[str, List[DailyPrice]] = defaultdict(list)
    for price in db.query(DailyPrice).order_by(DailyPrice.ticker, DailyPrice.date).all():
        by_ticker[price.ticker].append(price)

    observations = db.query(SentimentObservation).filter(SentimentObservation.avg_diffbot_sentiment.isnot(None)).all()
    for obs in observations:
        prices = by_ticker.get(obs.ticker, [])
        idx = _nearest_price_index_on_or_after(prices, obs.date)
        if idx is None or idx + 1 >= len(prices):
            continue
        X.append(_feature_vector(obs, prices, idx))
        y.append(float(prices[idx + 1].close))
    return X, y


def _latest_prediction_inputs(db: Session):
    stocks = {s.ticker: s for s in db.query(Stock).all()}
    by_ticker: Dict[str, List[DailyPrice]] = defaultdict(list)
    for price in db.query(DailyPrice).order_by(DailyPrice.ticker, DailyPrice.date).all():
        by_ticker[price.ticker].append(price)

    inputs = []
    for ticker, prices in by_ticker.items():
        if not prices or ticker not in stocks:
            continue
        latest_obs = db.query(SentimentObservation).filter(
            SentimentObservation.ticker == ticker,
            SentimentObservation.avg_diffbot_sentiment.isnot(None),
        ).order_by(SentimentObservation.date.desc()).first()
        if not latest_obs:
            continue
        idx = len(prices) - 1
        inputs.append((stocks[ticker], latest_obs, prices[-1], _feature_vector(latest_obs, prices, idx)))
    return inputs


def _baseline_prediction(current_price: float, sentiment: Optional[float], ret_1d: float = 0.0) -> float:
    predicted_return = float(np.clip(0.003 * (sentiment or 0.0) + 0.15 * ret_1d, -0.035, 0.035))
    return float(current_price * (1 + predicted_return))


def train_price_model_and_predict(db: Session) -> Optional[float]:
    db.query(PricePrediction).delete()
    db.commit()

    X, y = _training_rows(db)
    latest_inputs = _latest_prediction_inputs(db)
    model = None
    mae = None
    model_type = "sentiment_price_baseline"

    if len(X) >= 16:
        X_arr = np.array(X, dtype=float)
        y_arr = np.array(y, dtype=float)
        if len(X) >= 30:
            X_train, X_test, y_train, y_test = train_test_split(X_arr, y_arr, test_size=0.25, random_state=42)
            model = GradientBoostingRegressor(random_state=42, n_estimators=120, max_depth=2, learning_rate=0.05)
            model.fit(X_train, y_train)
            mae = float(mean_absolute_error(y_test, model.predict(X_test)))
        else:
            model = GradientBoostingRegressor(random_state=42, n_estimators=80, max_depth=2, learning_rate=0.05)
            model.fit(X_arr, y_arr)
        model_type = "GradientBoostingRegressor"

    for stock, obs, latest_price, features in latest_inputs:
        predicted_price = float(model.predict(np.array([features], dtype=float))[0]) if model is not None else _baseline_prediction(latest_price.close, obs.avg_diffbot_sentiment, features[3])
        predicted_return = (predicted_price - latest_price.close) / latest_price.close if latest_price.close else 0.0
        db.add(
            PricePrediction(
                ticker=stock.ticker,
                company=stock.company,
                sector=stock.sector,
                prediction_date=latest_price.date,
                horizon_days=1,
                current_price=float(latest_price.close),
                predicted_price=float(predicted_price),
                predicted_return=float(predicted_return),
                latest_sentiment=obs.avg_diffbot_sentiment,
                article_count=obs.article_count,
                model_mae=mae,
                model_type=model_type,
            )
        )
    db.commit()
    return mae


def recompute_pipeline(db: Session) -> Optional[float]:
    copy_diffbot_labels_to_article_sentiment(db)
    recompute_observations(db)
    return train_price_model_and_predict(db)
