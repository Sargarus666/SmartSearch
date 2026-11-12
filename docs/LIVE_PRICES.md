# SmartScan: live prices for Naberezhnye Chelny

## Fixed selection criteria
- Price: **15,000–20,000 RUB inclusive**.
- Physical RAM: **at least 8 GB**. 4 GB and 6 GB variants are never eligible; virtual RAM expansion does not count.
- Device condition: **new only**.
- Local offer: in stock in Naberezhnye Chelny, with a valid product HTTPS URL and an explicit warranty.
- Freshness: merchant offer must have been checked within the last 24 hours.
- The feed variant's physical RAM and storage must exactly match the corresponding catalog variant.

The catalog currently contains 16 candidate models and all entries declare at least 8 GB physical RAM. This is a model shortlist, not proof that each model is currently in stock or priced in range.

## Important truth about prices
Old manually entered prices and store addresses are historical catalog fields. Search-result URLs are not proof of stock or price. The website does not show these legacy values as current offers.

## Automated refresh
GitHub Actions runs every 6 hours and on manual dispatch. It reads repository variable `SMARTSCAN_FEED_URLS`, a comma-separated list of HTTPS JSON feed URLs. Each feed must return:

```json
{
  "offers": [{
    "brand": "Example",
    "model": "Example 8/128",
    "ramGb": 8,
    "storageGb": 128,
    "price": 17990,
    "shop": "Merchant",
    "url": "https://merchant.example/product",
    "city": "Набережные Челны",
    "stock": "in_stock",
    "condition": "new",
    "warrantyMonths": 12,
    "checkedAt": "2026-11-12T12:00:00+03:00",
    "paymentOnDelivery": true
  }]
}
```

Only records meeting every selection criterion above are written to `verifiedOffers`. Invalid, stale, out-of-range, used, low-RAM, mismatched, or unverified offers are rejected.

## Enable
1. In GitHub repository Settings → Secrets and variables → Actions → Variables, add `SMARTSCAN_FEED_URLS`.
2. Set it to one or more authorized retailer feed endpoints separated by commas.
3. Run Actions → Refresh Chelny smartphone prices → Run workflow.

Do not point this variable at search pages or scrape pages without permission. Retailer feeds/API access must be authorized and must provide local delivery/stock data. Without configured feeds, the workflow deliberately leaves the catalog unchanged and does not invent prices.

## Current limitation
The ingestion pipeline enforces the requirements but does not itself grant access to retailer stock systems. Until authorized merchant feeds are configured, there are no verified live prices.