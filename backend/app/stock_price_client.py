from __future__ import annotations

import csv
import io
from datetime import datetime, timedelta, timezone
from typing import Dict, List, Optional

import requests
from sqlalchemy.orm import Session

from app.models import DailyPrice


STOOQ_DAILY_URL = "https://stooq.com/q/d/l/"
YAHOO_CHART_URL = "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}"


def _to_float(value) -> Optional[float]:
    try:
        if value in (None, "", "null", "None"):
            return None
        return float(value)
    except (TypeError, ValueError):
        return None


def _to_int(value) -> Optional[int]:
    try:
        if value in (None, "", "null", "None"):
            return None
        return int(float(value))
    except (TypeError, ValueError):
        return None


def _cutoff_date(days_back: int):
    return (datetime.now(timezone.utc) - timedelta(days=days_back)).date()


def _normalize_stooq_symbol(ticker: str) -> str:
    ticker = ticker.lower().strip()
    if not ticker.endswith(".us"):
        ticker = f"{ticker}.us"
    return ticker


def _normalize_yahoo_symbol(ticker: str) -> str:
    return ticker.upper().strip()


def _fetch_from_stooq(ticker: str, days_back: int) -> List[Dict]:
    symbol = _normalize_stooq_symbol(ticker)
    cutoff = _cutoff_date(days_back)

    response = requests.get(
        STOOQ_DAILY_URL,
        params={"s": symbol, "i": "d"},
        headers={
            "User-Agent": "StockifyAI/1.0 local price research project",
            "Accept": "text/csv,*/*",
        },
        timeout=25,
    )

    if response.status_code != 200:
        return []

    text = response.text.strip()

    if not text or "No data" in text or "Date,Open,High,Low,Close,Volume" not in text:
        return []

    rows = []
    reader = csv.DictReader(io.StringIO(text))

    for row in reader:
        date_text = row.get("Date")
        if not date_text:
            continue

        try:
            row_date = datetime.fromisoformat(date_text).date()
        except ValueError:
            continue

        if row_date < cutoff:
            continue

        close = _to_float(row.get("Close"))
        if close is None:
            continue

        rows.append(
            {
                "ticker": ticker.upper(),
                "date": row_date.isoformat(),
                "open": _to_float(row.get("Open")),
                "high": _to_float(row.get("High")),
                "low": _to_float(row.get("Low")),
                "close": close,
                "volume": _to_float(row.get("Volume")),
                "source": "Stooq",
            }
        )

    return sorted(rows, key=lambda r: r["date"])


def _fetch_from_yahoo(ticker: str, days_back: int) -> List[Dict]:
    symbol = _normalize_yahoo_symbol(ticker)

    end = datetime.now(timezone.utc)
    start = end - timedelta(days=days_back + 10)

    response = requests.get(
        YAHOO_CHART_URL.format(symbol=symbol),
        params={
            "period1": int(start.timestamp()),
            "period2": int(end.timestamp()),
            "interval": "1d",
            "events": "history",
            "includeAdjustedClose": "true",
        },
        headers={
            "User-Agent": "StockifyAI/1.0 local price research project",
            "Accept": "application/json,*/*",
        },
        timeout=25,
    )

    if response.status_code != 200:
        return []

    try:
        payload = response.json()
    except ValueError:
        return []

    result = payload.get("chart", {}).get("result", [])

    if not result:
        return []

    result = result[0]
    timestamps = result.get("timestamp") or []
    quote = (result.get("indicators", {}).get("quote") or [{}])[0]

    opens = quote.get("open") or []
    highs = quote.get("high") or []
    lows = quote.get("low") or []
    closes = quote.get("close") or []
    volumes = quote.get("volume") or []

    cutoff = _cutoff_date(days_back)
    rows = []

    for i, ts in enumerate(timestamps):
        try:
            row_date = datetime.fromtimestamp(ts, tz=timezone.utc).date()
        except Exception:
            continue

        if row_date < cutoff:
            continue

        close = _to_float(closes[i] if i < len(closes) else None)
        if close is None:
            continue

        rows.append(
            {
                "ticker": ticker.upper(),
                "date": row_date.isoformat(),
                "open": _to_float(opens[i] if i < len(opens) else None),
                "high": _to_float(highs[i] if i < len(highs) else None),
                "low": _to_float(lows[i] if i < len(lows) else None),
                "close": close,
                "volume": _to_float(volumes[i] if i < len(volumes) else None),
                "source": "Yahoo Finance",
            }
        )

    return sorted(rows, key=lambda r: r["date"])


def fetch_daily_prices(ticker: str, days_back: int = 90) -> List[Dict]:
    """
    Fetch recent daily OHLCV price history for a ticker.

    Tries Stooq first because it provides simple CSV with no API key.
    Falls back to Yahoo Finance's chart endpoint if Stooq returns no rows.
    """
    days_back = max(1, min(int(days_back), 90))

    rows = _fetch_from_stooq(ticker, days_back=days_back)

    if rows:
        return rows

    return _fetch_from_yahoo(ticker, days_back=days_back)


def upsert_daily_prices(db: Session, ticker: str, days_back: int = 90) -> int:
    prices = fetch_daily_prices(ticker, days_back=days_back)
    saved_or_updated = 0

    for price in prices:
        existing = (
            db.query(DailyPrice)
            .filter(
                DailyPrice.ticker == price["ticker"],
                DailyPrice.date == price["date"],
            )
            .first()
        )

        if existing:
            existing.open = price.get("open")
            existing.high = price.get("high")
            existing.low = price.get("low")
            existing.close = price.get("close")
            existing.volume = price.get("volume")
            existing.source = price.get("source")
        else:
            db.add(DailyPrice(**price))

        saved_or_updated += 1

    db.commit()
    return saved_or_updated


def refresh_prices_for_tickers(
    db: Session,
    tickers: List[str],
    days_back: int = 90,
) -> Dict[str, int]:
    results: Dict[str, int] = {}

    for ticker in tickers:
        ticker = ticker.upper().strip()
        results[ticker] = upsert_daily_prices(db, ticker, days_back=days_back)

    return results


def fetch_price_history(ticker: str, days_back: int = 90) -> List[Dict]:
    return fetch_daily_prices(ticker=ticker, days_back=days_back)