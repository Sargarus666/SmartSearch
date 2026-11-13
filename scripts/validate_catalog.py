#!/usr/bin/env python3
"""Fail-fast validation for SmartScan's catalog and static site."""
import json
from datetime import datetime, timezone
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
CITY = "Набережные Челны"
MIN_PRICE, MAX_PRICE, MIN_RAM = 15000, 20000, 8


class SiteParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids, self.duplicates = set(), []
        self.has_viewport = self.has_catalog_root = False

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        value = attrs.get("id")
        if value:
            if value in self.ids:
                self.duplicates.append(value)
            self.ids.add(value)
        if tag == "meta" and attrs.get("name") == "viewport":
            self.has_viewport = True
        if attrs.get("id") in {"cards", "grid"}:
            self.has_catalog_root = True


def main():
    data = json.loads((ROOT / "data/phones.json").read_text(encoding="utf-8"))
    phones = data.get("phones")
    if not isinstance(phones, list) or not phones:
        raise SystemExit("Catalog must contain a non-empty phones array")
    if data.get("city") != CITY:
        raise SystemExit("Catalog city must be Naberezhnye Chelny")
    if int(data.get("minPrice", 0)) != MIN_PRICE or int(data.get("maxPrice", 0)) != MAX_PRICE:
        raise SystemExit("Catalog budget must be 15,000–20,000 RUB")
    if int(data.get("minRamGb", 0)) != MIN_RAM:
        raise SystemExit("Catalog must require at least 8 GB physical RAM")
    ids = [p.get("id") for p in phones]
    if len(ids) != len(set(ids)):
        raise SystemExit("Phone IDs must be unique")
    known = {}
    for phone in phones:
        if not phone.get("brand") or not phone.get("model"):
            raise SystemExit(f"Phone {phone.get('id')} is missing brand/model")
        if int(phone.get("ramGb", 0)) < MIN_RAM:
            raise SystemExit(f"Phone {phone.get('id')} does not meet 8 GB physical RAM threshold")
        if int(phone.get("storageGb", 0)) < 1:
            raise SystemExit(f"Phone {phone.get('id')} has invalid storage")
        if not phone.get("cpu") or not phone.get("battery") or not phone.get("screen"):
            raise SystemExit(f"Phone {phone.get('id')} is missing core specifications")
        low, high = int(phone.get("priceFrom", 0)), int(phone.get("priceTo", 0))
        if low <= 0 or high < low:
            raise SystemExit(f"Phone {phone.get('id')} has an invalid estimated price range")
        if not phone.get("priceBasis"):
            raise SystemExit(f"Phone {phone.get('id')} must explain that its price is an estimate")
        photo = str(phone.get("photoUrl", ""))
        if photo.startswith("./assets/"):
            local_photo = ROOT / "web" / photo.removeprefix("./")
            if not local_photo.is_file():
                raise SystemExit(f"Phone {phone.get('id')} local photo is missing: {photo}")
        else:
            parsed_photo = urlsplit(photo)
            if parsed_photo.scheme != "https" or not parsed_photo.hostname:
                raise SystemExit(f"Phone {phone.get('id')} must have a valid HTTPS photo URL")
        gallery = phone.get("galleryUrls", [])
        if not isinstance(gallery, list) or len(gallery) > 12 or len(gallery) != len(set(gallery)):
            raise SystemExit(f"Phone {phone.get('id')} has an invalid or duplicate photo gallery")
        for image_url in gallery:
            parsed_image = urlsplit(str(image_url))
            if parsed_image.scheme != "https" or not parsed_image.hostname:
                raise SystemExit(f"Phone {phone.get('id')} gallery contains a non-HTTPS image")
        key = (phone["brand"].casefold(), phone["model"].casefold())
        if key in known:
            raise SystemExit(f"Duplicate brand/model: {phone['brand']} {phone['model']}")
        known[key] = phone
    offers = data.get("verifiedOffers", [])
    if not isinstance(offers, list):
        raise SystemExit("verifiedOffers must be an array")
    for offer in offers:
        if offer.get("verified") is not True:
            raise SystemExit("Every verifiedOffers item must have verified=true")
        if offer.get("city") != CITY or offer.get("stock") != "in_stock":
            raise SystemExit("Verified offer must be in stock in Naberezhnye Chelny")
        if offer.get("condition") != "new":
            raise SystemExit("Verified offer must be explicitly new")
        key = (str(offer.get("brand", "")).casefold(), str(offer.get("model", "")).casefold())
        phone = known.get(key)
        if not phone:
            raise SystemExit("Verified offer must match an exact catalog model")
        if int(offer.get("ramGb", 0)) != int(phone["ramGb"]) or int(offer.get("storageGb", 0)) != int(phone["storageGb"]):
            raise SystemExit("Verified offer RAM/storage must match the exact catalog variant")
        if int(offer.get("ramGb", 0)) < MIN_RAM:
            raise SystemExit("Verified offer has less than 8 GB physical RAM")
        if not MIN_PRICE <= int(offer.get("price", 0)) <= MAX_PRICE:
            raise SystemExit("Verified offer is outside 15,000–20,000 RUB")
        parsed = urlsplit(str(offer.get("url", "")))
        if parsed.scheme != "https" or not parsed.hostname:
            raise SystemExit("Verified offer must use a valid HTTPS product URL")
        if not offer.get("shop") or int(offer.get("warrantyMonths", 0)) < 1:
            raise SystemExit("Verified offer must include shop and warranty")
        checked = datetime.fromisoformat(str(offer.get("checkedAt", "")).replace("Z", "+00:00"))
        if checked.tzinfo is None:
            raise SystemExit("Verified offer checkedAt must include a timezone")
        age = datetime.now(timezone.utc) - checked.astimezone(timezone.utc)
        if age.total_seconds() > 24 * 60 * 60 or age.total_seconds() < -300:
            raise SystemExit("Verified offer must have been checked within the last 24 hours")
        if "paymentOnDelivery" in offer and not isinstance(offer["paymentOnDelivery"], bool):
            raise SystemExit("paymentOnDelivery must be boolean when supplied")
    page = SiteParser()
    page.feed((ROOT / "web/index.html").read_text(encoding="utf-8"))
    if not page.has_viewport or not page.has_catalog_root:
        raise SystemExit("Web shell is missing viewport metadata or catalog root (#grid/#cards)")
    if page.duplicates:
        raise SystemExit("Duplicate HTML IDs: " + ", ".join(page.duplicates))
    print(f"OK: {len(phones)} catalog models, {len(offers)} verified offers, web shell valid")


if __name__ == "__main__":
    main()
