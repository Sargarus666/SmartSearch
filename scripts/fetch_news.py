#!/usr/bin/env python3
"""Build a fresh Russian tech-news JSON feed from public RSS sources."""
from __future__ import annotations
import argparse, email.utils, html, json, re, sys, urllib.error, urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timedelta, timezone
from pathlib import Path

FEEDS = [
    ("3DNews · Мобильные", "https://3dnews.ru/phone/rss/"),
    ("3DNews · Технологии", "https://3dnews.ru/news/rss/"),
    ("Mobile-Review", "https://mobile-review.com/all/feed/"),
    ("24Gadget", "https://24gadget.ru/rss.xml"),
]
KEYWORDS = re.compile(r"смартфон|мобильн|телефон|android|iphone|samsung|xiaomi|huawei|honor|realme|infinix|tecno|poco|redmi|гаджет|планшет|ноутбук|процессор|видеокарт|nvidia|amd|apple|google|windows|нейросет|искусственн интеллект", re.I)
TAG_RE = re.compile(r"<[^>]+>")
IMG_RE = re.compile(r"""<img[^>]+src=["']([^"']+)""", re.I)

def lname(tag):
    return tag.rsplit("}", 1)[-1].lower()

def child_text(node, *names):
    wanted = set(names)
    for child in node.iter():
        if child is not node and lname(child.tag) in wanted and child.text:
            return child.text.strip()
    return ""

def clean(value, limit=360):
    return re.sub(r"\s+", " ", TAG_RE.sub(" ", html.unescape(value or ""))).strip()[:limit]

def parse_date(value):
    if not value: return None
    try:
        dt = email.utils.parsedate_to_datetime(value)
    except (TypeError, ValueError, OverflowError):
        try: dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError: return None
    if dt.tzinfo is None: dt = dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)

def fetch_feed(source, url, cutoff):
    req = urllib.request.Request(url, headers={"User-Agent":"SmartScanNewsBot/1.0 (+https://github.com/Sargarus666/SmartSearch)", "Accept":"application/rss+xml, application/atom+xml, application/xml, text/xml"})
    with urllib.request.urlopen(req, timeout=20) as response: raw = response.read(4_000_000)
    root = ET.fromstring(raw)
    entries = [node for node in root.iter() if lname(node.tag) in ("item", "entry")]
    found = []
    for item in entries:
        title = clean(child_text(item, "title"), 180)
        link = child_text(item, "link")
        if not link:
            for node in item.iter():
                if lname(node.tag) == "link" and node.attrib.get("href"):
                    link = node.attrib["href"]; break
        description = child_text(item, "description", "summary", "encoded", "content")
        published = parse_date(child_text(item, "pubdate", "published", "updated", "date"))
        if not title or not link or not published or published < cutoff: continue
        if not KEYWORDS.search(title + " " + clean(description, 1000)): continue
        image = ""
        for node in item.iter():
            if lname(node.tag) in ("thumbnail", "content", "enclosure"):
                candidate = node.attrib.get("url", "")
                if candidate.startswith(("https://", "http://")) and re.search(r"\.(jpg|jpeg|png|webp)(\?|$)", candidate, re.I):
                    image = candidate; break
        if not image:
            match = IMG_RE.search(description or "")
            image = match.group(1) if match else ""
        found.append({"title":title,"url":link,"summary":clean(description),"image":image,"source":source,"published":published.isoformat(),"dateLabel":published.astimezone(timezone(timedelta(hours=3))).strftime("%d.%m.%Y · %H:%M МСК")})
    return found

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--output",required=True)
    parser.add_argument("--max-age-days",type=int,default=14)
    args=parser.parse_args()
    now=datetime.now(timezone.utc); cutoff=now-timedelta(days=args.max_age_days)
    collected={}; failures=[]
    for source,url in FEEDS:
        try:
            batch=fetch_feed(source,url,cutoff)
            for item in batch: collected[item["url"]]=item
            print(f"{source}: {len(batch)} fresh items")
        except (OSError,urllib.error.URLError,ET.ParseError,TimeoutError) as exc:
            failures.append(f"{source}: {exc}")
            print(f"WARNING {source}: {exc}",file=sys.stderr)
    items=sorted(collected.values(),key=lambda x:x["published"],reverse=True)[:40]
    payload={"generatedAt":now.isoformat(),"maxAgeDays":args.max_age_days,"sources":[x[0] for x in FEEDS],"items":items,"warnings":failures}
    output=Path(args.output); output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(payload,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(f"Wrote {len(items)} fresh articles to {output}")
    if not items: print("WARNING: no fresh articles; stale cached news is intentionally not reused.",file=sys.stderr)
    return 0

if __name__=="__main__": raise SystemExit(main())
