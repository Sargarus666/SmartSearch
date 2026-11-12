#!/usr/bin/env python3
"""Refresh SmartScan observations through Firecrawl's hosted scrape API.

All extracted offers remain observations, never verifiedOffers. The local
Price.ru pages are used because they are scoped to Naberezhnye Chelny.
"""
import json
import os
import sys
from datetime import datetime, timezone, timedelta
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]
SOURCES = ROOT / "scripts" / "retailer_sources.json"
CATALOG = ROOT / "data" / "phones.json"
API_URL = "https://api.firecrawl.dev/v2/scrape"
MIN_PRICE, MAX_PRICE = 15000, 20000
LOCAL_TZ = timezone(timedelta(hours=3))

SCHEMA = {
    "type": "object",
    "properties": {
        "brand": {"type": ["string", "null"]},
        "model": {"type": ["string", "null"]},
        "physical_ram_gb": {"type": ["integer", "null"]},
        "storage_gb": {"type": ["integer", "null"]},
        "price_rub": {"type": ["integer", "null"]},
        "seller": {"type": ["string", "null"]},
        "condition": {"type": ["string", "null"]},
        "stock_status": {"type": ["string", "null"]},
        "delivery_chelny": {"type": ["string", "null"]},
        "pickup": {"type": ["string", "null"]},
        "payment": {"type": ["string", "null"]},
        "warranty": {"type": ["string", "null"]},
        "updated_at": {"type": ["string", "null"]}
    },
    "required": [
        "brand", "model", "physical_ram_gb", "storage_gb", "price_rub",
        "seller", "condition", "stock_status", "delivery_chelny",
        "pickup", "payment", "warranty", "updated_at"
    ],
    "additionalProperties": False
}


def firecrawl_extract(api_key, source):
    expected = source["expected"]
    prompt = (
        "Extract the offer for the exact product variant from this page. "
        "This is a city-specific Price.ru product page for Naberezhnye Chelny. "
        f"Expected product: {expected['brand']} {expected['model']}; "
        f"physical RAM exactly {expected['ramGb']} GB; storage exactly "
        f"{expected['storageGb']} GB; expected seller {expected['seller']}. "
        "Return the exact price of that seller's offer, not the page-wide "
        "minimum/maximum unless it is explicitly that seller's price. "
        "Physical RAM excludes virtual/extended RAM. Do not mix another "
        "seller or memory variant. Condition, delivery, pickup, payment and "
        "warranty must be copied only if explicitly stated. Delivery is "
        "relevant only when the page states delivery to Naberezhnye Chelny. "
        "If a field is absent or ambiguous, return null. Return price as "
        "integer RUB without currency symbols."
    )
    payload = {
        "url": source["url"],
        "formats": [{
            "type": "json",
            "prompt": prompt,
            "schema": SCHEMA
        }],
        "onlyMainContent": True,
        "maxAge": 0,
        "proxy": "stealth",
        "waitFor": 2500
    }
    req = Request(
        API_URL,
        data=json.dumps(payload).encode("utf-8"),
        headers={
            "Authorization": "Bearer " + api_key,
            "Content-Type": "application/json",
            "Accept": "application/json"
        },
        method="POST"
    )
    try:
        with urlopen(req, timeout=120) as response:
            result = json.loads(response.read().decode("utf-8"))
    except HTTPError as exc:
        detail = exc.read(2000).decode("utf-8", errors="replace")
        raise RuntimeError(f"Firecrawl HTTP {exc.code}: {detail}") from exc
    except (URLError, TimeoutError) as exc:
        raise RuntimeError(f"Firecrawl request failed: {exc}") from exc

    if not result.get("success"):
        raise RuntimeError("Firecrawl API returned success=false: " + str(result.get("error")))
    page = result.get("data") or {}
    status = (page.get("metadata") or {}).get("statusCode")
    if status is not None and not (200 <= int(status) < 300 or int(status) == 304):
        raise RuntimeError(f"Source page returned HTTP {status}")
    extracted = page.get("json") or {}
    if not isinstance(extracted, dict):
        raise RuntimeError("Firecrawl returned no structured JSON object")
    return extracted


def validate_offer(item, source):
    expected = source["expected"]
    for key, wanted in (
        ("physical_ram_gb", expected["ramGb"]),
        ("storage_gb", expected["storageGb"])
    ):
        if item.get(key) != wanted:
            raise ValueError(f"{key} mismatch or missing: {item.get(key)!r}")
    price = item.get("price_rub")
    if not isinstance(price, int) or not MIN_PRICE <= price <= MAX_PRICE:
        raise ValueError(f"seller price missing or outside 15,000–20,000 RUB: {price!r}")
    allowed_sellers = [expected["seller"], *expected.get("sellerAliases", [])]
    if (item.get("seller") or "").casefold() not in {name.casefold() for name in allowed_sellers}:
        raise ValueError(f"seller mismatch: {item.get('seller')!r}")
    if (item.get("condition") or "").casefold() not in ("new", "новый", "новое"):
        raise ValueError(f"new condition not confirmed: {item.get('condition')!r}")
    brand = (item.get("brand") or "").strip().casefold()
    if brand != str(expected["brand"]).strip().casefold():
        raise ValueError(f"brand mismatch: {item.get('brand')!r}")
    model = (item.get("model") or "").casefold()
    # matchTerms also records the expected brand and RAM/storage variant.
    # Brand is checked above; memory is validated by the exact numeric fields.
    # Compare only distinctive model tokens with the model field itself.
    model_terms = [
        term.casefold() for term in expected["matchTerms"]
        if term.casefold() != str(expected["brand"]).casefold() and "/" not in term
    ]
    if not model_terms or not all(term in model for term in model_terms):
        raise ValueError(f"model mismatch: {item.get('model')!r}")


def main():
    api_key = os.environ.get("FIRECRAWL_API_KEY", "").strip()
    if not api_key:
        print("FIRECRAWL_API_KEY is not configured; skipping hosted scraping.")
        return 0

    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    sources = json.loads(SOURCES.read_text(encoding="utf-8"))
    existing = data.setdefault("observedOffers", [])
    found = 0

    for source in sources:
        expected = source["expected"]
        try:
            extracted = firecrawl_extract(api_key, source)
            validate_offer(extracted, source)
            now = datetime.now(LOCAL_TZ).isoformat(timespec="seconds")
            delivery = extracted.get("delivery_chelny")
            pickup_text = (extracted.get("pickup") or "").casefold()
            pickup = "no" if any(x in pickup_text for x in ("нет", "недоступ", "отсутств")) else (
                "available" if any(x in pickup_text for x in ("есть", "доступ", "забрать")) else "unknown"
            )
            warranty_text = (extracted.get("warranty") or "").casefold()
            warranty_months = None
            import re
            warranty_match = re.search(r"(\d+)\s*(?:мес|месяц)", warranty_text)
            if warranty_match:
                warranty_months = int(warranty_match.group(1))
            elif "год" in warranty_text:
                year_match = re.search(r"(\d+)\s*год", warranty_text)
                warranty_months = int(year_match.group(1)) * 12 if year_match else 12
            offer = {
                "brand": expected["brand"],
                "model": expected["model"],
                "ramGb": expected["ramGb"],
                "storageGb": expected["storageGb"],
                "price": extracted["price_rub"],
                "shop": expected["seller"],
                "url": expected["retailerUrl"],
                "localProofUrl": source["url"],
                "checkedAt": now,
                "sourceType": "firecrawl_price_ru_observation",
                "condition": "new",
                "stock": "delivery_available" if delivery else "unknown",
                "city": "Набережные Челны",
                "warrantyMonths": warranty_months,
                "payment": extracted.get("payment") or "unknown",
                "pickup": pickup,
                "note": (
                    "Извлечено Firecrawl из городской карточки Price.ru. "
                    f"Доставка: {delivery or 'не подтверждена'}. "
                    f"Самовывоз: {extracted.get('pickup') or 'не подтверждён'}. "
                    f"Гарантия: {extracted.get('warranty') or 'не подтверждена'}. "
                    "Это наблюдение агрегатора, а не подтверждение остатка у продавца."
                )
            }
            match = next((old for old in existing if
                str(old.get("brand", "")).casefold() == offer["brand"].casefold() and
                str(old.get("model", "")).casefold() == offer["model"].casefold() and
                str(old.get("shop", "")).casefold() == offer["shop"].casefold()), None)
            if match:
                match.update(offer)
            else:
                existing.append(offer)
            found += 1
            print(f"FOUND {offer['shop']} {offer['model']} {offer['price']} RUB")
        except Exception as exc:
            print(f"SKIP {expected.get('model')}: {exc}")

    if found:
        data["observedOffersUpdated"] = datetime.now(LOCAL_TZ).isoformat(timespec="seconds")
        CATALOG.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Firecrawl local observations refreshed: {found}/{len(sources)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
