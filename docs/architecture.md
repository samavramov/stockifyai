# Architecture

StockifyAI is organized as a local FastAPI backend and a Vue dashboard frontend.

## Universe

The application is scoped to 101 S&P 500 technology and technology-adjacent companies. The configured list lives in `backend/app/stocks.py` and drives every analysis run, picker, ranking table, and company detail page.

## Backend responsibilities

The backend handles article discovery, sentiment extraction, price ingestion, persistence, and modeling.

Key modules:

- `stocks.py`: defines the fixed S&P 500 technology universe
- `diffbot_client.py`: discovers TechCrunch articles and enriches them through Diffbot
- `stock_price_client.py`: fetches daily OHLCV price history
- `live_ingest.py`: coordinates ticker analysis runs
- `pipeline.py`: carries sentiment forward, aligns it with prices, and updates forecasts
- `main.py`: exposes dashboard API endpoints
- `models.py`: defines SQLite-backed SQLAlchemy models

## Data model

The app stores:

- configured stock metadata
- analyzed articles
- carried-forward sentiment observations
- daily price history
- prediction records
- ingestion-run metadata

Favorites are stored locally in the browser.

## Modeling approach

Diffbot provides article-level sentiment labels. Those labels are aggregated by ticker and article date. The latest sentiment signal is then carried forward across daily price rows until a new article-driven update appears.

The forecasting pipeline predicts next close from close price, carried-forward sentiment, article count, one-day return, five-day return, and volume. With enough aligned observations, StockifyAI trains a GradientBoostingRegressor. With limited history, it displays a conservative baseline forecast.

## Frontend responsibilities

The frontend provides controls for analysis runs and displays company-level output:

- run configuration
- searchable ticker selection over the 101-stock universe
- summary metrics
- company rankings
- latest close and predicted next close
- sentiment trend chart
- daily price chart
- recent analyzed articles
