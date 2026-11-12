#!/usr/bin/env python3
"""Merge fresh, explicitly verified merchant offers into the SmartScan catalog."""
import json
import os
import sys
from datetime import datetime, timedelta, timezone
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

CITY = "Набережные Челны"
MIN_PRICE, MAX_PRICE, MIN_RAM = 10000, 15000, 8
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
    req = Request(url, headers={
        "User-Agent": "ChelnySmartScan/2.2",
        "Accept": "application/json",
    })
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
    brand = str(item.get("brand", "")).strip()
    model = str(item.get("model", "")).strip()
    key = (brand.casefold(), model.casefold())
    expected = known_models.get(key)
    if not expected:
        return None
    if item.get("city") != CITY or item.get("stock") != "in_stock":
        return None
    if str(item.get("condition", "")).casefold() != "new":
        return None
    ram = int(item.get("ramGb", 0))
    storage = int(item.get("storageGb", 0))
    if ram < MIN_RAM or ram != expected["ramGb"] or storage != expected["storageGb"]:
        return None
    price = int(item.get("price", 0))
    if not MIN_PRICE <= price <= MAX_PRICE:
        return None
    shop = str(item.get("shop", "")).strip()
    product_url = str(item.get("url", "")).strip()
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
        "brand": expected["brand"],
        "model": expected["model"],
        "ramGb": ram,
        "storageGb": storage,
        "price": price,
        "shop": shop,
        "url": product_url,
        "city": CITY,
        "stock": "in_stock",
        "condition": "new",
        "warrantyMonths": warranty,
        "checkedAt": checked.isoformat(timespec="seconds"),
        "verified": True,
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
        for p in catalog.get("phones", [])
        if p.get("brand") and p.get("model")
    }

    now = datetime.now(timezone.utc)
    offers, errors = [], []
    for url in urls:
        try:
            payload = fetch(url)
            for item in payload["offers"]:
                try:
                    normalized = normalize_offer(item, known_models, now)
                    if normalized:
                        offers.append(normalized)
                except (TypeError, ValueError, OverflowError):
                    continue
        except Exception as exc:
            host = urlsplit(url).hostname or "invalid-feed-url"
            errors.append(f"{host}: {exc}")

    # Stable de-duplication: one current offer per exact product, shop and URL.
    unique = {}
    for offer in offers:
        key = (offer["brand"].casefold(), offer["model"].casefold(),
               offer["shop"].casefold(), offer["url"])
        current = unique.get(key)
        if current is None or offer["checkedAt"] > current["checkedAt"]:
            unique[key] = offer
    offers = sorted(unique.values(), key=lambda x: (x["price"], x["brand"], x["model"], x["shop"]))

    catalog["city"] = CITY
    catalog["minPrice"] = MIN_PRICE
    catalog["maxPrice"] = MAX_PRICE
    catalog["minRamGb"] = MIN_RAM
    catalog["pricePolicy"] = (
        "Only fresh (24h), new, in-stock merchant-feed offers for exact catalog variants "
        "with physical RAM, local city, HTTPS product URL and warranty are verified."
    )
    catalog["verifiedOffers"] = offers
    catalog["feedErrors"] = errors
    catalog["updated"] = now.isoformat(timespec="seconds")

    with open(CATALOG, "w", encoding="utf-8") as handle:
        json.dump(catalog, handle, ensure_ascii=False, indent=2)
        handle.write("\n")

    print(f"Saved {len(offers)} fresh verified offers; feed errors: {len(errors)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
