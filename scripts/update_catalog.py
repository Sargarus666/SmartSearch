#!/usr/bin/env python3
"""Merge verified merchant JSON feeds into the SmartScan catalog.

Feed contract: {"offers":[{"brand":"...","model":"...","ramGb":8,"storageGb":128,
"price":17990,"shop":"DNS","url":"https://...","city":"Набережные Челны",
"stock":"in_stock","warrantyMonths":12,"checkedAt":"2026-11-12T12:00:00+03:00"}]}
Only offers with explicit physical RAM, city, stock, warranty and a product URL are accepted.
"""
import json, os, sys
from datetime import datetime, timezone
from urllib.request import Request, urlopen

CITY = "Набережные Челны"
MIN_PRICE, MAX_PRICE, MIN_RAM = 10000, 20000, 8
CATALOG = "data/phones.json"

def fetch(url):
    req = Request(url, headers={"User-Agent": "ChelnySmartScan/2.1"})
    with urlopen(req, timeout=20) as response:
        if response.status != 200:
            raise RuntimeError(f"Feed HTTP {response.status}: {url}")
        return json.load(response)

def main():
    urls = [u.strip() for u in os.getenv("SMARTSCAN_FEED_URLS", "").split(",") if u.strip()]
    if not urls:
        print("No merchant feeds configured. Keeping catalog unchanged; no prices were fabricated.")
        return 0
    offers = []
    errors = []
    for url in urls:
        try:
            payload = fetch(url)
            for item in payload.get("offers", []):
                try:
                    if item.get("city") != CITY: continue
                    if item.get("stock") != "in_stock": continue
                    if int(item.get("ramGb", 0)) < MIN_RAM: continue
                    price = int(item.get("price", 0))
                    if not MIN_PRICE <= price <= MAX_PRICE: continue
                    if not item.get("url", "").startswith("https://"): continue
                    if not item.get("shop") or not item.get("warrantyMonths"): continue
                    item["price"] = price
                    item["verified"] = True
                    offers.append(item)
                except (TypeError, ValueError):
                    continue
        except Exception as exc:
            errors.append(f"{url}: {exc}")
    path = CATALOG
    with open(path, encoding="utf-8") as f: catalog = json.load(f)
    catalog["city"] = CITY
    catalog["updated"] = datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")
    catalog["pricePolicy"] = "Only merchant-feed offers with explicit local stock, physical RAM and warranty are verified."
    catalog["verifiedOffers"] = sorted(offers, key=lambda x: x["price"])
    catalog["feedErrors"] = errors
    with open(path, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"Saved {len(offers)} verified offers; feed errors: {len(errors)}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
