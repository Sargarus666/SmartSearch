#!/usr/bin/env python3
"""Merge fresh, explicitly verified merchant offers into the SmartScan catalog."""
import json
import os
import sys
from datetime import datetime, timedelta, timezone
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

CITY = "Набережные Челны"
MIN_PRICE, MAX_PRICE, MIN_RAM = 15000, 20000, 8
MAX_AGE = timedelta(hours=24)
MAX_FUTURE_SKEW = timedelta(minutes=5)
CATALOG = "data/phones.json"
MAX_FEED_BYTES = 5 * 1024 * 1024


def parse_timestamp(value):
    if not isinstance(value, str) or not value.strip():
        raise ValueError("missing checkedAt")
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if parsed.tzinfo is None:
        raise ValueError("checkedAt must include a timezone")
    return parsed.astimezone(timezone.utc)


def fetch(url):
    parsed = urlsplit(url)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password:
        raise ValueError("feed URL must be a plain HTTPS URL")
    req = Request(url, headers={"User-Agent": "ChelnySmartScan/2.2", "Accept": "application/json"})
    with urlopen(req, timeout=20) as response:
        if response.status != 200:
            raise RuntimeError(f"HTTP {response.status}")
        raw = response.read(MAX_FEED_BYTES + 1)
    if len(raw) > MAX_FEED_BYTES:
        raise ValueError("feed exceeds 5 MB")
    payload = json.loads(raw.decode("utf-8"))
    if not isinstance(payload, dict) or not isinstance(payload.get("offers"), list):
        raise ValueError("feed must contain an offers array")
    return payload


def normalize_offer(item, known_models, now):
    if not isinstance(item, dict):
        return None
    brand, model = str(item.get("brand", "")).strip(), str(item.get("model", "")).strip()
    expected = known_models.get((brand.casefold(), model.casefold()))
    if not expected or item.get("city") != CITY or item.get("stock") != "in_stock":
        return None
    if str(item.get("condition", "")).casefold() != "new":
        return None
    ram, storage, price = int(item.get("ramGb", 0)), int(item.get("storageGb", 0)), int(item.get("price", 0))
    if ram < MIN_RAM or ram != expected["ramGb"] or storage != expected["storageGb"]:
        return None
    if not MIN_PRICE <= price <= MAX_PRICE:
        return None
    shop, product_url = str(item.get("shop", "")).strip(), str(item.get("url", "")).strip()
    parsed_url = urlsplit(product_url)
    if not shop or parsed_url.scheme != "https" or not parsed_url.hostname:
        return None
    warranty = int(item.get("warrantyMonths", 0))
    if warranty < 1:
        return None
    checked = parse_timestamp(item.get("checkedAt"))
    age = now - checked
    if age > MAX_AGE or age < -MAX_FUTURE_SKEW:
        return None
    result = {
        "brand": expected["brand"], "model": expected["model"],
        "ramGb": ram, "storageGb": storage, "price": price,
        "shop": shop, "url": product_url, "city": CITY,
        "stock": "in_stock", "condition": "new", "warrantyMonths": warranty,
        "checkedAt": checked.isoformat(timespec="seconds"), "verified": True,
    }
    if isinstance(item.get("paymentOnDelivery"), bool):
        result["paymentOnDelivery"] = item["paymentOnDelivery"]
    return result


def main():
    urls = [u.strip() for u in os.getenv("SMARTSCAN_FEED_URLS", "").split(",") if u.strip()]
    if not urls:
        print("No merchant feeds configured. Keeping catalog unchanged; no prices were fabricated.")
        return 0
    with open(CATALOG, encoding="utf-8") as handle:
        catalog = json.load(handle)
    known_models = {
        (p["brand"].casefold(), p["model"].casefold()): p
        for p in catalog.get("phones", []) if p.get("brand") and p.get("model")
    }
    now, offers, errors = datetime.now(timezone.utc), [], []
    for url in urls:
        try:
            payload = fetch(url)
            for item in payload["offers"]:
                try:
                    offer = normalize_offer(item, known_models, now)
                    if offer:
                        offers.append(offer)
                except (TypeError, ValueError, OverflowError):
                    continue
        except Exception as exc:
            errors.append(f"{urlsplit(url).hostname or 'invalid-feed-url'}: {exc}")
    unique = {}
    for offer in offers:
        key = (offer["brand"].casefold(), offer["model"].casefold(), offer["shop"].casefold(), offer["url"])
        if key not in unique or offer["checkedAt"] > unique[key]["checkedAt"]:
            unique[key] = offer
    catalog.update({
        "city": CITY, "minPrice": MIN_PRICE, "maxPrice": MAX_PRICE, "minRamGb": MIN_RAM,
        "pricePolicy": "Only fresh (24h), new, in-stock merchant-feed offers priced 15,000–20,000 RUB for exact physical-RAM variants, local city, HTTPS product URL and warranty are verified.",
        "verifiedOffers": sorted(unique.values(), key=lambda x: (x["price"], x["brand"], x["model"], x["shop"])),
        "feedErrors": errors,
        "updated": now.isoformat(timespec="seconds"),
    })
    with open(CATALOG, "w", encoding="utf-8") as handle:
        json.dump(catalog, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(f"Saved {len(unique)} fresh verified offers; feed errors: {len(errors)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
