#!/usr/bin/env python3
"""Deterministic, read-only audit for SmartScan. No AI provider or API key required."""
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
CITY = "Набережные Челны"
MIN_PRICE, MAX_PRICE, MIN_RAM = 15000, 20000, 8
NOW = datetime.now(timezone.utc)


def main():
    catalog = json.loads((ROOT / "data/phones.json").read_text(encoding="utf-8"))
    phones, offers = catalog.get("phones", []), catalog.get("verifiedOffers", [])
    findings = []
    if not phones:
        findings.append(("ERROR", "Каталог пуст: нет массива phones."))
    if not offers:
        findings.append(("WARNING", "Нет подтверждённых предложений verifiedOffers. Старые цены не должны выдаваться за актуальные."))

    updated = catalog.get("updated")
    if updated:
        try:
            age = (NOW.date() - datetime.fromisoformat(str(updated).replace("Z", "+00:00")).date()).days
            if age > 7:
                findings.append(("WARNING", f"Каталог не обновлялся {age} дней (updated={updated})."))
        except ValueError:
            findings.append(("WARNING", f"Поле updated имеет непонятный формат: {updated!r}."))
    else:
        findings.append(("WARNING", "У каталога отсутствует дата updated."))

    known = {}
    for phone in phones:
        key = (str(phone.get("brand", "")).strip().casefold(), str(phone.get("model", "")).strip().casefold())
        if key in known:
            findings.append(("ERROR", f"Повтор модели: {phone.get('brand')} {phone.get('model')}."))
        known[key] = phone
        ram = int(phone.get("ramGb", 0) or 0)
        if ram < MIN_RAM:
            findings.append(("ERROR", f"{phone.get('brand')} {phone.get('model')}: физической RAM меньше 8 ГБ."))
        if not phone.get("photoUrl"):
            findings.append(("WARNING", f"{phone.get('brand')} {phone.get('model')}: нет photoUrl."))

    for offer in offers:
        label = f"{offer.get('shop', 'магазин')} / {offer.get('model', 'модель')}"
        if offer.get("verified") is not True:
            findings.append(("ERROR", f"{label}: предложение не помечено verified=true."))
        if offer.get("city") != CITY or offer.get("stock") != "in_stock":
            findings.append(("ERROR", f"{label}: нет подтверждённого наличия в Набережных Челнах."))
        if offer.get("condition") != "new":
            findings.append(("ERROR", f"{label}: состояние не подтверждено как новое."))
        if int(offer.get("ramGb", 0) or 0) < MIN_RAM:
            findings.append(("ERROR", f"{label}: менее 8 ГБ физической RAM."))
        price = int(offer.get("price", 0) or 0)
        if not MIN_PRICE <= price <= MAX_PRICE:
            findings.append(("ERROR", f"{label}: цена вне диапазона 15 000–20 000 ₽."))
        key = (str(offer.get("brand", "")).strip().casefold(), str(offer.get("model", "")).strip().casefold())
        phone = known.get(key)
        if not phone or int(offer.get("ramGb", 0) or 0) != int(phone.get("ramGb", 0) or 0) or int(offer.get("storageGb", 0) or 0) != int(phone.get("storageGb", 0) or 0):
            findings.append(("ERROR", f"{label}: RAM/накопитель не совпадают с точной версией модели в каталоге."))
        parsed = urlsplit(str(offer.get("url", "")))
        if parsed.scheme != "https" or not parsed.hostname:
            findings.append(("ERROR", f"{label}: ссылка должна быть HTTPS."))
        if int(offer.get("warrantyMonths", 0) or 0) < 1:
            findings.append(("ERROR", f"{label}: не указана гарантия."))
        try:
            checked = datetime.fromisoformat(str(offer.get("checkedAt", "")).replace("Z", "+00:00"))
            if checked.tzinfo is None or NOW - checked.astimezone(timezone.utc) > timedelta(hours=24) or checked.astimezone(timezone.utc) - NOW > timedelta(minutes=5):
                findings.append(("ERROR", f"{label}: отметка проверки старше 24 часов или некорректна."))
        except ValueError:
            findings.append(("ERROR", f"{label}: отсутствует корректная дата checkedAt."))

    errors = sum(level == "ERROR" for level, _ in findings)
    warnings = sum(level == "WARNING" for level, _ in findings)
    lines = [
        "# SmartScan — автоматический аудит каталога", "",
        f"- Дата проверки (UTC): {NOW.date().isoformat()}",
        f"- Моделей в каталоге: {len(phones)}",
        f"- Подтверждённых предложений: {len(offers)}",
        f"- Критерии: новые устройства, 15 000–20 000 RUB, физическая RAM не менее 8 ГБ",
        f"- Ошибок: {errors}", f"- Предупреждений: {warnings}", "",
        "> Проверка выполняется локальными правилами Python. Внешние цены, наличие и гарантия не подтверждаются без данных магазина.", "",
        "## Результаты", ""
    ]
    if findings:
        lines.extend(f"- **{level}:** {message}" for level, message in findings)
    else:
        lines.append("Критичных проблем по заданным правилам не найдено.")
    lines.extend(["", "## Ограничения", "", "Отчёт проверяет внутреннюю согласованность и свежесть отметок. Он не заменяет подтверждение магазина и не создаёт цены."])
    (ROOT / "catalog-audit.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Audit complete: {errors} errors, {warnings} warnings.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
