#!/usr/bin/env python3
"""Fail-fast validation for SmartScan's public catalog and static site."""
import json
import re
import sys
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class SiteParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
        self.duplicates = []
        self.has_viewport = False
        self.has_catalog_root = False
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        value = attrs.get("id")
        if value:
            if value in self.ids:
                self.duplicates.append(value)
            self.ids.add(value)
        if tag == "meta" and attrs.get("name") == "viewport":
            self.has_viewport = True
        if attrs.get("id") == "cards":
            self.has_catalog_root = True

def main():
    data = json.loads((ROOT / "data/phones.json").read_text(encoding="utf-8"))
    phones = data.get("phones")
    if not isinstance(phones, list) or not phones:
        raise SystemExit("Catalog must contain a non-empty phones array")
    ids = [p.get("id") for p in phones]
    if len(ids) != len(set(ids)):
        raise SystemExit("Phone IDs must be unique")
    for p in phones:
        if not p.get("brand") or not p.get("model"):
            raise SystemExit(f"Phone {p.get('id')} is missing brand/model")
        if int(p.get("ramGb", 0)) < 8:
            raise SystemExit(f"Phone {p.get('id')} does not meet 8 GB physical RAM threshold")
        if int(p.get("storageGb", 0)) < 1:
            raise SystemExit(f"Phone {p.get('id')} has invalid storage")
    offers = data.get("verifiedOffers", [])
    if not isinstance(offers, list):
        raise SystemExit("verifiedOffers must be an array")
    for offer in offers:
        if offer.get("verified") is not True:
            raise SystemExit("Every verifiedOffers item must have verified=true")
        if offer.get("city") != "Набережные Челны":
            raise SystemExit("Verified offer has wrong city")
        if offer.get("stock") != "in_stock":
            raise SystemExit("Verified offer is not explicitly in stock")
        if int(offer.get("ramGb", 0)) < 8:
            raise SystemExit("Verified offer has less than 8 GB physical RAM")
        if not 10000 <= int(offer.get("price", 0)) <= 20000:
            raise SystemExit("Verified offer is outside 10,000–20,000 RUB")
        if not re.match(r"^https://", str(offer.get("url", ""))):
            raise SystemExit("Verified offer must use HTTPS")
        if not offer.get("shop") or int(offer.get("warrantyMonths", 0)) < 1:
            raise SystemExit("Verified offer must include shop and warranty")
    page = SiteParser()
    page.feed((ROOT / "web/index.html").read_text(encoding="utf-8"))
    if not page.has_viewport or not page.has_catalog_root:
        raise SystemExit("Web shell is missing viewport metadata or #cards catalog root")
    if page.duplicates:
        raise SystemExit("Duplicate HTML IDs: " + ", ".join(page.duplicates))
    print(f"OK: {len(phones)} catalog models, {len(offers)} verified offers, web shell valid")

if __name__ == "__main__":
    main()
