from sqlalchemy import Column, Integer, String, Float, DateTime, Text, UniqueConstraint
from sqlalchemy.sql import func
from app.database import Base


class Stock(Base):
    __tablename__ = "stocks"
    ticker = Column(String, primary_key=True, index=True)
    company = Column(String, nullable=False)
    sector = Column(String, nullable=False, default="Unknown")
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class Article(Base):
    __tablename__ = "articles"
    id = Column(Integer, primary_key=True, index=True)
    ticker = Column(String, index=True, nullable=False)
    company = Column(String, nullable=False)
    title = Column(String, nullable=False)
    text = Column(Text, nullable=True)
    summary = Column(Text, nullable=True)
    url = Column(String, nullable=False, unique=True)
    source = Column(String, nullable=True)
    published_at = Column(String, index=True, nullable=True)
    diffbot_sentiment = Column(Float, nullable=True)
    model_sentiment = Column(Float, nullable=True)
    label_source = Column(String, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class DailyPrice(Base):
    __tablename__ = "daily_prices"
    id = Column(Integer, primary_key=True, index=True)
    ticker = Column(String, index=True, nullable=False)
    date = Column(String, index=True, nullable=False)
    open = Column(Float, nullable=True)
    high = Column(Float, nullable=True)
    low = Column(Float, nullable=True)
    close = Column(Float, nullable=False)
    volume = Column(Float, nullable=True)
    source = Column(String, nullable=False, default="Stooq")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    __table_args__ = (UniqueConstraint("ticker", "date", name="uq_price_ticker_date"),)


class SentimentObservation(Base):
    __tablename__ = "sentiment_observations"
    id = Column(Integer, primary_key=True, index=True)
    ticker = Column(String, index=True, nullable=False)
    company = Column(String, nullable=False)
    sector = Column(String, nullable=False)
    date = Column(String, index=True, nullable=False)
    article_count = Column(Integer, nullable=False, default=0)
    avg_diffbot_sentiment = Column(Float, nullable=True)
    avg_model_sentiment = Column(Float, nullable=True)
    __table_args__ = (UniqueConstraint("ticker", "date", name="uq_ticker_date"),)


class PricePrediction(Base):
    __tablename__ = "price_predictions"
    id = Column(Integer, primary_key=True, index=True)
    ticker = Column(String, index=True, nullable=False)
    company = Column(String, nullable=False)
    sector = Column(String, nullable=False)
    prediction_date = Column(String, index=True, nullable=False)
    horizon_days = Column(Integer, nullable=False, default=1)
    current_price = Column(Float, nullable=False)
    predicted_price = Column(Float, nullable=False)
    predicted_return = Column(Float, nullable=False)
    latest_sentiment = Column(Float, nullable=True)
    article_count = Column(Integer, nullable=False, default=0)
    model_mae = Column(Float, nullable=True)
    model_type = Column(String, nullable=False, default="baseline")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    __table_args__ = (UniqueConstraint("ticker", "prediction_date", "horizon_days", name="uq_prediction_ticker_date_horizon"),)


class IngestionRun(Base):
    __tablename__ = "ingestion_runs"
    id = Column(Integer, primary_key=True, index=True)
    started_at = Column(DateTime(timezone=True), server_default=func.now())
    finished_at = Column(DateTime(timezone=True), nullable=True)
    status = Column(String, nullable=False, default="running")
    tickers = Column(Text, nullable=True)
    articles_found = Column(Integer, nullable=False, default=0)
    articles_saved = Column(Integer, nullable=False, default=0)
    model_mae = Column(Float, nullable=True)
    price_model_mae = Column(Float, nullable=True)
    error = Column(Text, nullable=True)
