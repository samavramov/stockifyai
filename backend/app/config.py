import os
from functools import lru_cache
from dotenv import load_dotenv

load_dotenv()

class Settings:
    diffbot_token: str | None = os.getenv("DIFFBOT_TOKEN")
    database_url: str = os.getenv("DATABASE_URL", "sqlite:///./stockify.db")
    diffbot_kg_dql_endpoint: str = os.getenv("DIFFBOT_KG_DQL_ENDPOINT", "https://kg.diffbot.com/kg/v3/dql")
    diffbot_article_endpoint: str = os.getenv("DIFFBOT_ARTICLE_ENDPOINT", "https://api.diffbot.com/v3/article")
    enrich_with_article_api: bool = os.getenv("DIFFBOT_ENRICH_WITH_ARTICLE_API", "false").lower() == "true"

@lru_cache
def get_settings() -> Settings:
    return Settings()
