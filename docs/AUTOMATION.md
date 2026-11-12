# SmartScan automation

## Automated checks

- `.github/workflows/quality.yml`: validates catalog invariants, **15,000–20,000 RUB** budget, minimum **8 GB physical RAM**, new-device and fresh-offer rules, HTML IDs and Python syntax.
- `.github/workflows/codeql.yml`: scheduled CodeQL static security analysis for JavaScript, Python and Kotlin/Java.
- `.github/dependabot.yml`: weekly update PRs for GitHub Actions.
- `.github/workflows/refresh-prices.yml`: scheduled merchant-feed refresh. Only new, in-stock offers in Naberezhnye Chelny with at least 8 GB physical RAM, price 15,000–20,000 RUB, warranty and HTTPS product links are accepted.
- `.github/workflows/ai-audit.yml`: weekly deterministic catalog audit. Despite the historical filename, it no longer calls an AI service and requires no API key.

## AI configuration

No Gemini, OpenRouter, Groq or other AI API key is required. The audit uses Python rules, produces a downloadable report and has read-only permissions. It cannot independently confirm real-world prices, stock, warranty or product authenticity.

## Live merchant feeds

Set repository variable `SMARTSCAN_FEED_URLS` to comma-separated HTTPS JSON feed URLs from merchants or authorized data providers. Feed format and strict acceptance policy are documented in [LIVE_PRICES.md](LIVE_PRICES.md). Do not put API keys in this variable or in the repository; use GitHub Actions secrets for credentials and extend the importer to consume them securely.

## Permissions and safety

All quality and audit workflows are read-only. Only the dedicated price-refresh workflow has `contents: write`, and it can commit only `data/phones.json`. Do not grant the audit workflow write permissions.
