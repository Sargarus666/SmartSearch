# SmartScan automation

## What runs automatically
- `.github/workflows/quality.yml`: validates catalog structure, the 15,000–20,000 RUB selection range, minimum 8 GB physical RAM, HTML and JavaScript syntax.
- `.github/workflows/deploy-pages.yml`: publishes the static catalog to GitHub Pages when the website or catalog changes.
- `.github/workflows/codeql.yml`: static security analysis.
- `.github/dependabot.yml`: weekly dependency update checks.
- `.github/workflows/ai-audit.yml`: weekly read-only catalog audit. It does not call an AI service and requires no API key.

## Prices
The site does not scrape prices, call Firecrawl, read merchant feeds, or claim live stock. It displays approximate Russian-market price ranges stored in `data/phones.json`. These are comparison estimates only, not current store offers.

## Product details
Selecting a phone card opens its own detail view inside the site, with the product photo, specifications, estimated price and search links for DNS, Ситилинк, Ozon, МегаФон, Билайн and МТС.

## Secrets and permissions
No Firecrawl, Gemini, Google or merchant API key is needed. Do not add credentials to the repository or the website. Quality and audit workflows remain read-only; the Pages workflow only publishes the static site.
