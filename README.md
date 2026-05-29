# StockifyAI

StockifyAI is an equity-research dashboard for on-demand sentiment and price analysis across a fixed S&P 500 technology and technology-adjacent universe. It combines recent company coverage, Diffbot article sentiment, daily OHLCV market data, and a local next-close prediction pipeline in a FastAPI + Vue application.

## Coverage universe

The tracked universe contains 101 S&P 500 technology and technology-adjacent tickers. The app intentionally does not add non-listed companies or broad market tickers; all search, ingestion, ranking, and forecasting views are scoped to this configured universe.

## Features

- Select tickers from the 101-company universe and run fresh analysis from the dashboard
- Discover recent TechCrunch articles through the TechCrunch JSON API
- Extract article text and sentiment labels with the Diffbot Article API
- Fetch daily stock price history from free OHLCV market-data endpoints
- Carry each recorded sentiment signal forward until the next article-driven sentiment update
- Store article, sentiment, price, and prediction records in SQLite
- Visualize sentiment trends, daily close history, article labels, and next-close predictions
- Save favorite tickers locally in the browser

## Tech stack

**Frontend:** Vue, Vite, ApexCharts  
**Backend:** Python, FastAPI, SQLAlchemy, SQLite  
**Data:** Diffbot Article API, TechCrunch JSON API, Stooq/Yahoo daily prices  
**Modeling:** scikit-learn GradientBoostingRegressor with a sentiment-adjusted baseline fallback

## Modeling approach

Diffbot sentiment is treated as an input signal, not a prediction target. The backend aggregates article-level sentiment by ticker and date, then carries the latest recorded sentiment value forward across daily price rows until a new article-driven sentiment update appears. This gives the model a continuous sentiment feature that can be aligned with market data.

The next-close forecasting pipeline uses six features:

- latest close price
- carried-forward Diffbot sentiment
- article count for the active sentiment window
- one-day return
- five-day return
- trading volume

When there are enough aligned price and sentiment observations, StockifyAI trains a scikit-learn GradientBoostingRegressor. When aligned history is limited, the dashboard uses a conservative baseline forecast and labels it clearly.

## Local setup

### Backend

```bash
cd backend
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
```

Add your Diffbot token to `backend/.env`:

```env
DIFFBOT_TOKEN=your_token_here
```

Start the API:

```bash
python -m uvicorn app.main:app --reload --port 8000
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Recommended analysis run

Start with:

- AAPL, MSFT, NVDA, GOOGL, AMZN
- 5 articles per ticker
- 90-day lookback
- Diffbot extraction enabled
- Reset database enabled for the first run after schema changes

## Environment variables

```env
DIFFBOT_TOKEN=your_token_here
DATABASE_URL=sqlite:///./stockify.db
```

## Note

StockifyAI is a research prototype. Forecasts are informational and not financial advice.
