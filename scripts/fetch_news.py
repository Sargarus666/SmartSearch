#!/usr/bin/env python3
"""Build a fresh Russian tech-news JSON feed from public RSS/Atom sources."""
from __future__ import annotations

import argparse
import email.utils
import html
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone
from pathlib import Path

FEEDS = [
    ("3DNews · Смартфоны", "https://3dnews.ru/phone/rss/"),
    ("3DNews · Hardware", "https://3dnews.ru/hardware-news/rss"),
    ("3DNews · Технологии", "https://3dnews.ru/news/rss/"),
    ("3DNews · Software и ИИ", "https://3dnews.ru/software-news/rss/"),
    ("Mobile-Review", "https://mobile-review.com/all/feed/"),
    ("24Gadget", "https://24gadget.ru/rss.xml"),
    ("Hi-News", "https://hi-news.ru/feed"),
]
KEYWORDS = re.compile(
    r"смартфон|мобильн|телефон|android|iphone|samsung|xiaomi|huawei|honor|realme|"
    r"infinix|tecno|poco|redmi|гаджет|планшет|ноутбук|процессор|видеокарт|"
    r"nvidia|amd|apple|google|windows|нейросет|искусственн интеллект|"
    r"wearable|watch|pixel|galaxy|snapdragon|mediatek",
    re.I,
)
TAG_RE = re.compile(r"<[^>]+>")
IMG_RE = re.compile(r"""<img[^>]+(?:src|data-src)=["']([^"']+)""", re.I)
IMAGE_EXT = re.compile(r"\.(?:jpg|jpeg|png|webp|avif)(?:[?#].*)?$", re.I)


def lname(tag: str) -> str:
    return tag.rsplit("}", 1)[-1].lower()


def child_text(node: ET.Element, *names: str) -> str:
    wanted = set(names)
    for child in node.iter():
        if child is not node and lname(child.tag) in wanted and child.text:
            return child.text.strip()
    return ""


def clean(value: str, limit: int = 360) -> str:
    return re.sub(r"\s+", " ", TAG_RE.sub(" ", html.unescape(value or ""))).strip()[:limit]


def parse_date(value: str | None) -> datetime | None:
    if not value:
        return None
    try:
        dt = email.utils.parsedate_to_datetime(value)
    except (TypeError, ValueError, OverflowError):
        try:
            dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError:
            return None
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


def entry_link(item: ET.Element) -> str:
    for node in item.iter():
        if lname(node.tag) == "link":
            href = node.attrib.get("href", "").strip()
            rel = node.attrib.get("rel", "alternate")
            if href and rel in ("alternate", ""):
                return href
            if node.text and node.text.strip():
                return node.text.strip()
    return ""


def entry_image(item: ET.Element, description: str, base_url: str) -> str:
    for node in item.iter():
        tag = lname(node.tag)
        candidate = ""
        if tag in ("thumbnail", "content", "enclosure"):
            candidate = node.attrib.get("url", "")
            mime = node.attrib.get("type", "")
            if tag == "enclosure" and mime and not mime.startswith("image/"):
                continue
        if candidate and candidate.startswith(("https://", "http://")):
            return candidate
    match = IMG_RE.search(html.unescape(description or ""))
    if match:
        candidate = urllib.parse.urljoin(base_url, match.group(1))
        if candidate.startswith(("https://", "http://")):
            return candidate
    return ""


def fetch_feed(source: str, url: str, cutoff: datetime) -> list[dict]:
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0 (compatible; SmartScanNewsBot/1.1; +https://github.com/Sargarus666/SmartSearch)",
            "Accept": "application/rss+xml, application/atom+xml, application/xml, text/xml",
        },
    )
    with urllib.request.urlopen(req, timeout=18) as response:
        raw = response.read(4_000_000)
    root = ET.fromstring(raw)
    entries = [node for node in root.iter() if lname(node.tag) in ("item", "entry")]
    found = []
    for item in entries:
        title = clean(child_text(item, "title"), 180)
        link = entry_link(item)
        description = child_text(item, "description", "summary", "encoded", "content")
        published = parse_date(child_text(item, "pubdate", "published", "updated", "date", "issued"))
        if not title or not link or not published or published < cutoff:
            continue
        if not link.startswith(("https://", "http://")):
            continue
        if not KEYWORDS.search(title + " " + clean(description, 1200)):
            continue
        image = entry_image(item, description, url)
        found.append({
            "title": title,
            "url": link,
            "summary": clean(description),
            "image": image,
            "source": source,
            "published": published.isoformat(),
            "dateLabel": published.astimezone(timezone(timedelta(hours=3))).strftime("%d.%m.%Y · %H:%M МСК"),
        })
    return found


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--max-age-days", type=int, default=10)
    parser.add_argument("--limit", type=int, default=50)
    args = parser.parse_args()
    if args.max_age_days < 1 or args.limit < 1:
        parser.error("--max-age-days and --limit must be positive")
    now = datetime.now(timezone.utc)
    cutoff = now - timedelta(days=args.max_age_days)
    collected = {}
    failures = []
    successful = []
    for source, url in FEEDS:
        try:
            batch = fetch_feed(source, url, cutoff)
            successful.append(source)
            for item in batch:
                existing = collected.get(item["url"])
                if not existing or item["published"] > existing["published"]:
                    collected[item["url"]] = item
            print(f"{source}: {len(batch)} свежих материалов")
        except (OSError, urllib.error.URLError, ET.ParseError, TimeoutError, ValueError) as exc:
            failures.append(f"{source}: {type(exc).__name__}: {exc}")
            print(f"WARNING {source}: {exc}", file=sys.stderr)
    items = sorted(collected.values(), key=lambda item: item["published"], reverse=True)[:args.limit]
    payload = {
        "generatedAt": now.isoformat(),
        "maxAgeDays": args.max_age_days,
        "sources": successful,
        "sourceCount": len(successful),
        "items": items,
        "warnings": failures,
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Записано {len(items)} свежих новостей из {len(successful)}/{len(FEEDS)} источников: {output}")
    if not items:
        print("WARNING: нет свежих новостей; старые материалы намеренно не подставляются.", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
