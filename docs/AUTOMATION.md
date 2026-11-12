# SmartScan automation

## Automated checks

- `.github/workflows/quality.yml`: validates catalog invariants, verified-offer rules, HTML IDs and Python syntax on pushes and pull requests.
- `.github/workflows/codeql.yml`: scheduled CodeQL static security analysis for JavaScript, Python and Kotlin/Java.
- `.github/dependabot.yml`: weekly update PRs for GitHub Actions.
- `.github/workflows/refresh-prices.yml`: scheduled merchant-feed refresh. Only explicit local stock, physical RAM, warranty and HTTPS product links are accepted.
- `.github/workflows/ai-audit.yml`: optional weekly read-only Gemini audit. It creates a downloadable report and never changes catalog data.

## Optional AI configuration

Add repository secret `GEMINI_API_KEY` to enable the AI audit. Optionally set repository variable `GEMINI_MODEL` to a model available to that API key. Without the secret the job exits safely without calling an AI service.

The AI report is advisory, not a source of truth. The model cannot independently confirm a merchant's real-world price, stock, warranty, or product authenticity. It has no write permission and cannot update catalog files.

## Live merchant feeds

Set repository variable `SMARTSCAN_FEED_URLS` to comma-separated HTTPS JSON feed URLs from merchants or authorized data providers. Feed format and strict acceptance policy are documented in [LIVE_PRICES.md](LIVE_PRICES.md). Do not put API keys in this variable or in the repository; use GitHub Actions secrets for credentials and extend the importer to consume them securely.

## Permissions and safety

All quality and AI audit workflows are read-only. Only the dedicated price-refresh workflow has `contents: write`, and it can commit only `data/phones.json`. Do not grant the AI audit workflow write permissions.
