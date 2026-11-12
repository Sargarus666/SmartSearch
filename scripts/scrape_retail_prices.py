#!/usr/bin/env python3
"""Scrape public retailer product pages into non-verified price observations."""
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
        self.buf = []
    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == "meta":
            key = a.get("property") or a.get("name") or a.get("itemprop")
            if key and a.get("content"): self.meta[key.casefold()] = a["content"]
        if tag == "title": self.in_title = True
        if tag == "script" and a.get("type","").casefold() == "application/ld+json":
            self.in_jsonld, self.buf = True, []
    def handle_endtag(self, tag):
        if tag == "title": self.in_title = False
        if tag == "script" and self.in_jsonld:
            if self.buf: self.jsonld.append("".join(self.buf))
            self.in_jsonld = False
    def handle_data(self, data):
        if self.in_title: self.title += data
        if self.in_jsonld: self.buf.append(data)

def walk(x):
    if isinstance(x, dict):
        yield x
        for v in x.values(): yield from walk(v)
    elif isinstance(x, list):
        for v in x: yield from walk(v)

def scrape(source):
    url = source["url"]
    if urlsplit(url).scheme != "https": raise ValueError("HTTPS required")
    req = Request(url, headers={"User-Agent":"Mozilla/5.0 (compatible; ChelnySmartScan/1.0)", "Accept-Language":"ru-RU,ru;q=0.9"})
    with urlopen(req, timeout=25) as response: raw = response.read(8*1024*1024+1)
    if len(raw) > 8*1024*1024: raise ValueError("page too large")
    page = Page(); page.feed(raw.decode("utf-8", errors="replace"))
    title = page.title.strip()
    price = page.meta.get("product:price:amount") or page.meta.get("og:price:amount")
    ram = storage = None
    stock = "unknown"
    for raw_ld in page.jsonld:
        try: payload = json.loads(raw_ld)
        except Exception: continue
        for item in walk(payload):
            if item.get("@type") == "Product":
                title = item.get("name") or title
                offers = item.get("offers") or {}
                if isinstance(offers, list): offers = offers[0] if offers else {}
                if isinstance(offers, dict):
                    price = price or offers.get("price") or offers.get("lowPrice")
                    av = str(offers.get("availability","")).lower()
                    if "instock" in av: stock = "in_stock"
                    elif "outofstock" in av: stock = "out_of_stock"
                txt = json.dumps(item, ensure_ascii=False)
                m = re.search(r"(?:оперативная|ram)[^0-9]{0,24}(8|12|16)\s*(?:гб|gb)", txt, re.I)
                s = re.search(r"(?:встроенная|storage)[^0-9]{0,24}(128|256|512)\s*(?:гб|gb)", txt, re.I)
                if m: ram = int(m.group(1))
                if s: storage = int(s.group(1))
    if price is not None:
        try: price = int(float(str(price).replace(" ","").replace(",", ".")))
        except Exception: price = None
    if price is None:
        text = re.sub(r"<[^>]+>", " ", raw.decode("utf-8", errors="replace"))
        m = re.search(r"(?<!\d)(1[5-9]\s?\d{3}|20\s?000)\s*(?:₽|руб)", text, re.I)
        if m: price = int(re.sub(r"\s","",m.group(1)))
    if not price or not MIN_PRICE <= price <= MAX_PRICE: raise ValueError("price not found/in budget")
    expected = source["expected"]
    norm = re.sub(r"[^a-zа-я0-9/]+"," ",title.casefold())
    if not all(term.casefold() in norm for term in expected["matchTerms"]):
        raise ValueError("product title does not match expected model/variant: "+title[:120])
    variant = re.search(r"(8|12|16)\s*/\s*(128|256|512)", title)
    if variant: ram, storage = int(variant.group(1)), int(variant.group(2))
    if ram != expected["ramGb"] or storage != expected["storageGb"]:
        raise ValueError("exact physical RAM/storage not independently confirmed")
    return {"brand":expected["brand"],"model":expected["model"],"ramGb":ram,"storageGb":storage,
      "price":price,"shop":source["shop"],"url":url,"checkedAt":datetime.now(timezone.utc).isoformat(timespec="seconds"),
      "sourceType":"retailer_product_page","condition":"new","stock":stock,"city":"not_confirmed",
      "warrantyMonths":None,"note":"Цена извлечена со страницы магазина; наличие, доставка в Набережные Челны и гарантия отдельно не подтверждены."}

def main():
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    sources = json.loads(SOURCES.read_text(encoding="utf-8"))
    found = []
    for source in sources:
        try:
            item = scrape(source); found.append(item)
            print("FOUND", item["shop"], item["model"], item["price"])
        except Exception as exc: print("SKIP", source.get("shop"), str(exc))
    if found:
        # Merge observations instead of deleting locally enriched Price.ru delivery/warranty evidence
        # when a retailer blocks automated requests or omits local availability metadata.
        existing = data.get("observedOffers", [])
        for item in found:
            match = next((old for old in existing if
                str(old.get("brand","")).casefold() == item["brand"].casefold() and
                str(old.get("model","")).casefold() == item["model"].casefold() and
                str(old.get("shop","")).casefold() == item["shop"].casefold() and
                old.get("url") == item["url"]), None)
            if match:
                match["price"] = item["price"]
                match["checkedAt"] = item["checkedAt"]
                match["sourceType"] = item["sourceType"]
            else:
                existing.append(item)
        data["observedOffers"] = existing
        data["observedOffersUpdated"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
        CATALOG.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print("Retailer observations:",len(found))
    return 0

if __name__ == "__main__": sys.exit(main())
