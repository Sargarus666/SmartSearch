#!/usr/bin/env python3
"""Refresh Chelny price observations from public local Price.ru offer pages.

Retailer PDPs currently return HTTP 403/429 from GitHub runners. Price.ru's
city-specific product pages expose model, physical RAM/storage, seller,
condition, delivery and seller price in server-rendered HTML.
"""
import json, re, sys
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "scripts" / "retailer_sources.json"
CATALOG = ROOT / "data" / "phones.json"
MIN_PRICE, MAX_PRICE = 15000, 20000

class Page(HTMLParser):
    def __init__(self):
        super().__init__()
        self.meta, self.jsonld, self.title = {}, [], ""
        self.in_title = self.in_jsonld = False
        self.buf, self.text = [], []
    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == "meta":
            key = a.get("property") or a.get("name") or a.get("itemprop")
            if key and a.get("content"): self.meta[key.casefold()] = a["content"]
        if tag == "title": self.in_title = True
        if tag == "script" and a.get("type", "").casefold() == "application/ld+json":
            self.in_jsonld, self.buf = True, []
    def handle_endtag(self, tag):
        if tag == "title": self.in_title = False
        if tag == "script" and self.in_jsonld:
            if self.buf: self.jsonld.append("".join(self.buf))
            self.in_jsonld = False
    def handle_data(self, data):
        self.text.append(data)
        if self.in_title: self.title += data
        if self.in_jsonld: self.buf.append(data)

def normalize(s):
    return re.sub(r"\s+", " ", s).strip()

def money_values(s):
    return [int(re.sub(r"\D", "", m.group(0))) for m in re.finditer(r"(?<!\d)(?:\d{1,3}[ \u00a0]?){1,2}\d{3}\s*₽", s)]

def scrape(source):
    url = source["url"]
    if urlsplit(url).scheme != "https": raise ValueError("HTTPS required")
    req = Request(url, headers={
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36",
        "Accept-Language": "ru-RU,ru;q=0.9,en;q=0.7",
        "Accept": "text/html,application/xhtml+xml"
    })
    try:
        with urlopen(req, timeout=30) as response:
            raw = response.read(8*1024*1024+1)
        fetched_via = "direct"
    except Exception as direct_error:
        # Public reader proxy can retrieve pages when CDNs block GitHub runner IPs.
        reader_url = "https://r.jina.ai/" + url
        reader_req = Request(reader_url, headers={
            "User-Agent": "Mozilla/5.0",
            "Accept": "text/plain",
            "X-Return-Format": "markdown"
        })
        try:
            with urlopen(reader_req, timeout=45) as response:
                raw = response.read(8*1024*1024+1)
            fetched_via = "jina_reader"
        except Exception as reader_error:
            raise ValueError(f"direct fetch failed ({direct_error}); reader fallback failed ({reader_error})")
    if len(raw) > 8*1024*1024: raise ValueError("page too large")
    decoded = raw.decode("utf-8", errors="replace")
    page = Page(); page.feed(decoded)
    if not page.title:
        page.title = next((line.strip("# ").strip() for line in decoded.splitlines() if line.strip()), "")
    title = normalize(page.title)
    text = normalize(" ".join(page.text))
    expected = source["expected"]
    if source.get("type") != "price_ru_product":
        raise ValueError("unsupported source type; use city Price.ru product page")
    norm = re.sub(r"[^a-zа-я0-9/]+", " ", title.casefold())
    if not all(term.casefold() in norm for term in expected["matchTerms"]):
        raise ValueError("product title mismatch: " + title[:140])
    # Price.ru shows physical RAM/storage in the product facts and the
    # selected seller's exact price in its city-specific offer card.
    ram_match = re.search(r"Оперативная память\s*:?\s*(8|12|16)\s*Гб", text, re.I)
    storage_match = re.search(r"Встроенная память\s*:?\s*(128|256|512)\s*Гб", text, re.I)
    if not ram_match or not storage_match:
        for label in ("оперативная память", "встроенная память"):
            pos = text.casefold().find(label)
            print("DEBUG", label, repr(text[max(0,pos-40):pos+180] if pos >= 0 else text[:900]))
        print("DEBUG title", repr(title), "reader", fetched_via)
        raise ValueError("physical RAM/storage not found on Price.ru page")
    ram, storage = int(ram_match.group(1)), int(storage_match.group(1))
    if ram != expected["ramGb"] or storage != expected["storageGb"]:
        raise ValueError("exact physical RAM/storage mismatch")
    # Locate the expected merchant and select the nearest preceding price
    # from its offer card, not the page's overall min/max range.
    seller = expected["seller"]
    seller_pos = text.casefold().find(seller.casefold())
    if seller_pos < 0: raise ValueError("expected seller not found")
    before = text[max(0, seller_pos-260):seller_pos]
    prices = money_values(before)
    if not prices: raise ValueError("seller offer price not found")
    price = prices[-1]
    if not MIN_PRICE <= price <= MAX_PRICE:
        raise ValueError(f"seller price {price} outside 15,000–20,000 RUB")
    card_after = text[seller_pos:seller_pos+220]
    if "состояние товара: новый" not in text.casefold():
        raise ValueError("new condition not confirmed")
    delivery = "delivery_available" if "доставка завтра" in text.casefold() or "доставка есть" in text.casefold() else "unknown"
    payment = "cash" if "оплата наличными" in text.casefold() else "unknown"
    pickup = "no" if "самовывоз нет" in text.casefold() else "unknown"
    print("FETCH", expected["model"], fetched_via)
    return {
        "brand": expected["brand"], "model": expected["model"],
        "ramGb": ram, "storageGb": storage, "price": price,
        "shop": seller, "url": expected["retailerUrl"],
        "localProofUrl": url,
        "checkedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "sourceType": "retailer_plus_local_comparison",
        "condition": "new", "stock": delivery, "city": "Набережные Челны",
        "warrantyMonths": expected.get("warrantyMonths"),
        "payment": payment, "pickup": pickup,
        "note": "Цена, продавец, состояние и параметры подтверждены городской карточкой Price.ru. Доставка/оплата указаны агрегатором; гарантия отдельно не подтверждена скриптом."
    }

def main():
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    sources = json.loads(SOURCES.read_text(encoding="utf-8"))
    found = []
    for source in sources:
        try:
            item = scrape(source); found.append(item)
            print("FOUND", item["shop"], item["model"], item["price"], item["stock"])
        except Exception as exc:
            print("SKIP", source.get("expected", {}).get("model"), str(exc))
    if found:
        existing = data.get("observedOffers", [])
        for item in found:
            match = next((old for old in existing if
                str(old.get("brand","")).casefold() == item["brand"].casefold() and
                str(old.get("model","")).casefold() == item["model"].casefold() and
                str(old.get("shop","")).casefold() == item["shop"].casefold()), None)
            if match:
                # Preserve only explicitly sourced metadata; refresh volatile price/time.
                match.update({k:item[k] for k in ("price","checkedAt","localProofUrl","stock","payment","pickup","condition","city","sourceType","note")})
                match["url"] = item["url"]
            else:
                existing.append(item)
        data["observedOffers"] = existing
        data["observedOffersUpdated"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
        CATALOG.write_text(json.dumps(data, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print("Price.ru local observations:", len(found))
    return 0

if __name__ == "__main__": sys.exit(main())
