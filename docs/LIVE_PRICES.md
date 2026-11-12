# SmartScan: live prices for Naberezhnye Chelny

## Important truth about prices
The old catalog contains manually entered sample prices and store addresses. Search-result URLs are not proof of stock or price. Do not present these entries as live/confirmed offers.

## Automated refresh
GitHub Actions runs every 6 hours and on manual dispatch. It reads the repository variable `SMARTSCAN_FEED_URLS`, a comma-separated list of HTTPS JSON feed URLs. Each feed must return:

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
    "warrantyMonths": 12,
    "checkedAt": "2026-11-12T12:00:00+03:00"
  }]
}
```

Only records with explicit city, in-stock status, physical RAM >= 8 GB, price 10,000–20,000 RUB, HTTPS product URL, and a warranty period are included in `verifiedOffers`.

## Enable
1. In GitHub repository Settings → Secrets and variables → Actions → Variables, add `SMARTSCAN_FEED_URLS`.
2. Set it to one or more feed endpoints separated by commas.
3. Run Actions → Refresh Chelny smartphone prices → Run workflow.

Do not point this variable at search pages or scrape pages without permission. Retailer feeds/API access must be authorized and must provide local delivery/stock data. Without configured feeds, the workflow deliberately leaves the catalog unchanged and reports that no live source is configured. It never invents prices.

## Current limitation
This first implementation establishes a safe ingestion pipeline. It does not magically grant access to DNS, M.Video, marketplaces or their private stock systems. Until authorized merchant feeds are configured, existing `phones` data remains legacy catalog content, not live verified offers. The app UI must be updated to display `verifiedOffers` and visibly label legacy entries as unverified before those old prices are shown as current.
