#!/usr/bin/env python3
"""Deterministic, read-only catalog audit. No AI provider or API key required."""
import json
from datetime import date, datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
TODAY = datetime.now(timezone.utc).date()

def main():
    catalog = json.loads((ROOT / "data/phones.json").read_text(encoding="utf-8"))
    phones = catalog.get("phones", [])
    offers = catalog.get("verifiedOffers", [])
    findings = []

    if not phones:
        findings.append(("ERROR", "Каталог пуст: нет массива phones."))
    if not offers:
        findings.append(("WARNING", "Нет подтверждённых предложений verifiedOffers. Сайт не должен выдавать старые цены за актуальные."))

    updated = catalog.get("updated")
    if updated:
        try:
            age = (TODAY - date.fromisoformat(str(updated)[:10])).days
            if age > 7:
                findings.append(("WARNING", f"Каталог не обновлялся {age} дней (updated={updated})."))
        except ValueError:
            findings.append(("WARNING", f"Поле updated имеет непонятный формат: {updated!r}."))
    else:
        findings.append(("WARNING", "У каталога отсутствует дата updated."))

    seen = set()
    for phone in phones:
        key = (str(phone.get("brand", "")).strip().casefold(), str(phone.get("model", "")).strip().casefold())
        if key in seen:
            findings.append(("ERROR", f"Повтор модели: {phone.get('brand')} {phone.get('model')}."))
        seen.add(key)
        if int(phone.get("ramGb", 0) or 0) < 8:
            findings.append(("ERROR", f"{phone.get('brand')} {phone.get('model')}: физической RAM меньше 8 ГБ."))
        if not phone.get("photoUrl"):
            findings.append(("WARNING", f"{phone.get('brand')} {phone.get('model')}: нет photoUrl."))

    for offer in offers:
        label = f"{offer.get('shop', 'магазин')} / {offer.get('model', 'модель')}"
        if offer.get("verified") is not True:
            findings.append(("ERROR", f"{label}: предложение не помечено verified=true."))
        if offer.get("city") != "Набережные Челны":
            findings.append(("ERROR", f"{label}: неверный город."))
        if offer.get("stock") != "in_stock":
            findings.append(("WARNING", f"{label}: наличие не подтверждено."))
        if not str(offer.get("url", "")).startswith("https://"):
            findings.append(("ERROR", f"{label}: ссылка не HTTPS."))
        if int(offer.get("warrantyMonths", 0) or 0) < 1:
            findings.append(("WARNING", f"{label}: гарантия не указана."))

    errors = sum(level == "ERROR" for level, _ in findings)
    warnings = sum(level == "WARNING" for level, _ in findings)
    lines = [
        "# SmartScan — автоматический аудит каталога",
        "",
        f"- Дата проверки (UTC): {TODAY.isoformat()}",
        f"- Моделей в каталоге: {len(phones)}",
        f"- Подтверждённых предложений: {len(offers)}",
        f"- Ошибок: {errors}",
        f"- Предупреждений: {warnings}",
        "",
        "> Проверка выполняется локальными правилами Python. Внешние цены, наличие и гарантия автоматически не подтверждаются.",
        "",
        "## Результаты",
        ""
    ]
    if findings:
        lines.extend(f"- **{level}:** {message}" for level, message in findings)
    else:
        lines.append("Критичных проблем по заданным правилам не найдено.")
    lines.extend(["", "## Ограничения", "", "Этот отчёт проверяет внутреннюю согласованность данных и свежесть отметок. Он не заменяет подтверждение магазина и не создаёт цены."])
    (ROOT / "catalog-audit.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Audit complete: {errors} errors, {warnings} warnings.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
