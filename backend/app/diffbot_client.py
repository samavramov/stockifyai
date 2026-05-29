from __future__ import annotations

from datetime import datetime, timedelta, timezone
from email.utils import parsedate_to_datetime
from typing import Any, Dict, List, Optional
from urllib.parse import urlparse

import requests

from app.config import get_settings


TECHCRUNCH_SEARCH_ENDPOINT = "https://techcrunch.com/wp-json/wp/v2/search"
TECHCRUNCH_POSTS_ENDPOINT = "https://techcrunch.com/wp-json/wp/v2/posts"


class DiffbotClientError(RuntimeError):
    pass


def _first(value: Any) -> Any:
    if isinstance(value, list):
        return value[0] if value else None
    return value


def _as_text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, str):
        return value.strip()
    if isinstance(value, list):
        return " ".join(_as_text(v) for v in value if v is not None).strip()
    if isinstance(value, dict):
        for key in ("rendered", "name", "text", "value", "title", "str"):
            if key in value:
                return _as_text(value[key])
        return " ".join(_as_text(v) for v in value.values()).strip()
    return str(value).strip()


def _normalize_date(value: Any) -> Optional[str]:
    value = _first(value)
    if not value:
        return None
    if isinstance(value, dict):
        value = value.get("str") or value.get("value") or value.get("date")
    if not value:
        return None

    text = str(value).strip()
    if not text:
        return None

    if text.isdigit() and len(text) >= 10:
        try:
            return datetime.fromtimestamp(int(text[:10]), tz=timezone.utc).date().isoformat()
        except Exception:
            pass

    iso_candidate = text.replace("Z", "+00:00")
    try:
        return datetime.fromisoformat(iso_candidate[:10] if len(iso_candidate) >= 10 and iso_candidate[4:5] == "-" else iso_candidate).date().isoformat()
    except Exception:
        pass

    try:
        return parsedate_to_datetime(text).date().isoformat()
    except Exception:
        pass

    # Last-resort parser for strings like "Wed, 11 Feb 2026 04:00:00 GMT" when
    # upstream has already stripped punctuation in an unexpected way.
    months = {
        "jan": 1, "january": 1, "feb": 2, "february": 2, "mar": 3, "march": 3,
        "apr": 4, "april": 4, "may": 5, "jun": 6, "june": 6, "jul": 7, "july": 7,
        "aug": 8, "august": 8, "sep": 9, "sept": 9, "september": 9,
        "oct": 10, "october": 10, "nov": 11, "november": 11, "dec": 12, "december": 12,
    }
    cleaned = text.replace(",", " ").replace("/", " ").split()
    for i in range(len(cleaned) - 2):
        try:
            day = int(cleaned[i])
            month = months.get(cleaned[i + 1].lower())
            year = int(cleaned[i + 2])
            if month and 1900 <= year <= 2100:
                return datetime(year, month, day).date().isoformat()
        except Exception:
            continue
    return None


def _within_days(date_text: Optional[str], days_back: int) -> bool:
    if not date_text:
        return True
    try:
        dt = datetime.fromisoformat(date_text[:10]).date()
        cutoff = (datetime.now(timezone.utc) - timedelta(days=days_back)).date()
        return dt >= cutoff
    except Exception:
        return True


def _extract_records(payload: Dict[str, Any]) -> List[Dict[str, Any]]:
    for key in ("data", "results", "objects", "entities"):
        value = payload.get(key)
        if isinstance(value, list):
            records = []
            for item in value:
                if not isinstance(item, dict):
                    continue
                if isinstance(item.get("entity"), dict):
                    records.append(item["entity"])
                elif isinstance(item.get("record"), dict):
                    records.append(item["record"])
                else:
                    records.append(item)
            return records
    return []


def _extract_url(raw: Dict[str, Any]) -> str:
    url = _first(raw.get("pageUrl") or raw.get("url") or raw.get("link") or raw.get("resolvedPageUrl") or raw.get("siteUrl"))
    if isinstance(url, dict):
        url = url.get("rendered") or url.get("url") or url.get("href") or url.get("uri") or url.get("value") or ""
    if url:
        return str(url)
    for key in ("allUris", "origins", "links", "uris"):
        values = raw.get(key)
        if isinstance(values, list) and values:
            first = values[0]
            if isinstance(first, dict):
                return first.get("url") or first.get("href") or first.get("uri") or first.get("value") or ""
            return str(first)
    return ""


def _extract_sentiment(raw: Dict[str, Any]) -> Optional[float]:
    sentiment = raw.get("sentiment") or raw.get("diffbot_sentiment") or raw.get("benchmark_sentiment")
    if isinstance(sentiment, dict):
        sentiment = sentiment.get("score") or sentiment.get("value")
    sentiment = _first(sentiment)
    try:
        return float(sentiment) if sentiment is not None else None
    except (TypeError, ValueError):
        return None


def normalize_article(raw: Dict[str, Any], ticker: str, company: str, label_source: Optional[str] = None, fallback_url: Optional[str] = None) -> Optional[Dict[str, Any]]:
    url = _extract_url(raw) or fallback_url or ""
    title = _as_text(raw.get("title") or raw.get("headline") or raw.get("name") or raw.get("displayName"))
    text = _as_text(raw.get("text") or raw.get("htmlText") or raw.get("content") or raw.get("summary") or raw.get("description") or raw.get("excerpt"))
    summary = _as_text(raw.get("summary") or raw.get("description") or raw.get("excerpt") or text[:700])

    if not title and text:
        title = text[:140]
    if not title and url:
        title = url
    if not url or not title:
        return None

    parsed = urlparse(url)
    sentiment = _extract_sentiment(raw)
    source = _as_text(raw.get("siteName") or raw.get("publisher") or parsed.netloc)
    published_at = _normalize_date(raw.get("date") or raw.get("publishedAt") or raw.get("datePublished") or raw.get("estimatedDate"))

    return {
        "ticker": ticker,
        "company": company,
        "title": title[:500],
        "text": text,
        "summary": summary[:1200],
        "url": url,
        "source": source or parsed.netloc,
        "published_at": published_at,
        "diffbot_sentiment": sentiment,
        "label_source": label_source if sentiment is not None else None,
    }


def _company_search_terms(ticker: str, company: str) -> List[str]:
    ticker = ticker.upper().strip()
    aliases = {
        "AAPL": ["Apple", "iPhone", "Mac"], "MSFT": ["Microsoft", "Azure", "Windows"],
        "NVDA": ["Nvidia", "NVIDIA"], "GOOGL": ["Google", "Alphabet", "Gemini"], "GOOG": ["Google", "Alphabet", "Gemini"],
        "AMZN": ["Amazon", "AWS"], "META": ["Meta", "Facebook", "Instagram"], "TSLA": ["Tesla"],
        "AMD": ["AMD", "Advanced Micro Devices"], "NFLX": ["Netflix"], "CRM": ["Salesforce"],
        "ORCL": ["Oracle"], "ADBE": ["Adobe"], "INTC": ["Intel"], "AVGO": ["Broadcom"],
        "QCOM": ["Qualcomm"], "NOW": ["ServiceNow"], "CSCO": ["Cisco"], "PANW": ["Palo Alto Networks"],
        "CRWD": ["CrowdStrike"], "NET": ["Cloudflare"], "PLTR": ["Palantir"], "APP": ["AppLovin"],
        "V": ["Visa"], "MA": ["Mastercard"], "FI": ["Fiserv"], "FIS": ["Fidelity National Information Services"],
        "SPGI": ["S&P Global"], "MCO": ["Moody's"], "FDS": ["FactSet"], "EFX": ["Equifax"],
    }
    base = company
    for suffix in (" Inc.", " Inc", " Corp", " Corporation", " plc", " Class A", " Class C"):
        base = base.replace(suffix, "")
    terms = aliases.get(ticker, []) + [base.strip(), company.strip(), ticker]
    seen = set()
    out = []
    for term in terms:
        key = term.lower().strip()
        if key and key not in seen:
            seen.add(key)
            out.append(term.strip())
    return out


def _is_valid_techcrunch_article_url(url: str) -> bool:
    parsed = urlparse(url or "")
    if parsed.netloc.lower() not in {"techcrunch.com", "www.techcrunch.com"}:
        return False
    blocked = ("/tag/", "/category/", "/author/", "/events/", "/video/", "/podcast/", "/page/", "/search/")
    return bool(parsed.path and parsed.path != "/" and not parsed.path.startswith(blocked))


def _extract_wp_url(record: Dict[str, Any]) -> str:
    url = record.get("url") or record.get("link")
    if isinstance(url, dict):
        url = url.get("rendered") or url.get("href") or url.get("url") or ""
    return str(url or "")


def _wp_post_fallback(url: str, ticker: str, company: str, record: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    record = record or {}
    return {
        "ticker": ticker,
        "company": company,
        "title": _as_text(record.get("title")) or url,
        "text": _as_text(record.get("content") or record.get("excerpt")),
        "summary": _as_text(record.get("excerpt") or record.get("content"))[:1200],
        "url": url,
        "source": "TechCrunch",
        "published_at": _normalize_date(record.get("date") or record.get("date_gmt")),
        "diffbot_sentiment": None,
        "label_source": None,
    }


def _search_techcrunch_candidates(ticker: str, company: str, limit: int) -> List[Dict[str, Any]]:
    headers = {"User-Agent": "StockifyAI/1.0", "Accept": "application/json"}
    candidates: List[Dict[str, Any]] = []
    seen = set()
    for term in _company_search_terms(ticker, company):
        endpoints = [
            (TECHCRUNCH_SEARCH_ENDPOINT, {"search": term, "subtype": "post", "per_page": min(max(limit * 2, 5), 20)}),
            (TECHCRUNCH_POSTS_ENDPOINT, {"search": term, "per_page": min(max(limit * 2, 5), 20)}),
        ]
        for endpoint, params in endpoints:
            try:
                response = requests.get(endpoint, headers=headers, params=params, timeout=25)
                if response.status_code != 200:
                    continue
                records = response.json()
            except Exception:
                continue
            if not isinstance(records, list):
                continue
            for record in records:
                if not isinstance(record, dict):
                    continue
                url = _extract_wp_url(record).split("?")[0].strip()
                if not url or url in seen or not _is_valid_techcrunch_article_url(url):
                    continue
                seen.add(url)
                candidates.append(_wp_post_fallback(url, ticker, company, record))
                if len(candidates) >= limit:
                    return candidates
    return candidates


class DiffbotKGClient:
    def __init__(self):
        self.settings = get_settings()
        if not self.settings.diffbot_token:
            raise DiffbotClientError("DIFFBOT_TOKEN is not set. Create backend/.env from .env.example and add your token.")

    def _kg_request(self, query: str, size: int) -> List[Dict[str, Any]]:
        response = requests.get(
            self.settings.diffbot_kg_dql_endpoint,
            params={"token": self.settings.diffbot_token, "query": query, "size": size},
            timeout=40,
        )
        if response.status_code != 200:
            raise DiffbotClientError(f"Diffbot KG search failed: {response.status_code} {response.text[:500]}")
        return _extract_records(response.json())

    def build_article_query(self, ticker: str, company: str, days_back: int) -> str:
        company_escaped = company.replace('"', "")
        return f'type:Article text:"{company_escaped}" date<{days_back}d language:en sortBy:date'

    def search_articles(self, ticker: str, company: str, limit: int, days_back: int, source: str = "techcrunch", enrich: bool = True) -> List[Dict[str, Any]]:
        limit = max(1, min(int(limit), 25))
        days_back = max(1, min(int(days_back), 90))
        if (source or "techcrunch") == "techcrunch":
            return self.search_source_articles(ticker, company, limit, days_back, enrich=enrich)
        return self.search_kg_articles(ticker, company, limit, days_back)

    def search_kg_articles(self, ticker: str, company: str, limit: int, days_back: int) -> List[Dict[str, Any]]:
        records = self._kg_request(self.build_article_query(ticker, company, days_back), size=limit)
        articles = []
        seen = set()
        for raw in records:
            article = normalize_article(raw, ticker, company, label_source="diffbot_kg")
            if article and article["url"] not in seen:
                seen.add(article["url"])
                articles.append(article)
        return articles

    def search_source_articles(self, ticker: str, company: str, limit: int, days_back: int, enrich: bool = True) -> List[Dict[str, Any]]:
        candidates = _search_techcrunch_candidates(ticker, company, limit)
        articles: List[Dict[str, Any]] = []
        for candidate in candidates:
            article = self.enrich_with_article_api(candidate) if enrich else candidate
            if not _within_days(article.get("published_at"), days_back):
                continue
            articles.append(article)
            if len(articles) >= limit:
                break
        return articles

    def enrich_with_article_api(self, article: Dict[str, Any]) -> Dict[str, Any]:
        try:
            response = requests.get(
                self.settings.diffbot_article_endpoint,
                params={
                    "token": self.settings.diffbot_token,
                    "url": article["url"],
                    "nl": "true",
                    "timeout": 30000,
                },
                timeout=45,
            )
        except requests.RequestException:
            return article

        if response.status_code != 200:
            return article

        try:
            objects = response.json().get("objects", [])
        except ValueError:
            return article
        if not objects:
            return article

        raw = dict(objects[0])
        raw.setdefault("pageUrl", article["url"])
        raw.setdefault("date", article.get("published_at"))
        enriched = normalize_article(raw, article["ticker"], article["company"], label_source="diffbot_article_api", fallback_url=article["url"])
        if not enriched:
            return article

        for key, value in article.items():
            if enriched.get(key) in (None, ""):
                enriched[key] = value
        enriched["source"] = "TechCrunch"
        if enriched.get("diffbot_sentiment") is None:
            enriched["label_source"] = article.get("label_source")
        return enriched

    def enrich_article_url(self, url: str) -> Optional[Dict[str, Any]]:
        article = _wp_post_fallback(url, "", "")
        enriched = self.enrich_with_article_api(article)
        return enriched if enriched.get("title") else None
