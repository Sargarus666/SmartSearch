package ru.chelny.smartscan

data class NewsItem(
    val id: Int,
    val title: String,
    val text: String,
    val date: String,
    val tag: String,
    val colorStart: Int,
    val colorEnd: Int,
    val imageRes: String,
    val url: String
)

object NewsRepo {
    val news = listOf(
        NewsItem(1, "POCO M8 Pro вылетел из бюджета: 25–28 тыс. в октябре",
            "Ещё летом стоил 18 900 ₽, в октябре 2026 — от 25 990 ₽. Из подборки исключён, вместо него — POCO M7 8/256 за 17 990 ₽.",
            "14.10.2026", "Проверка цен", 0xFFE11D2E.toInt(), 0xFF7B1FA2.toInt(),
            "phone_7", "https://hi-tech.mail.ru/news/153818-10-populyarnyh-smartfonov-do-20-000-rublej/"),
        NewsItem(2, "Redmi Note 15 8/256: реально 19 899 ₽, а не 17 900",
            "Официальный магазин просит 21 990 ₽, RU-версия у розницы — 19 899 ₽. Старые цены из нейросетей устарели.",
            "14.10.2026", "Проверка цен", 0xFF1E88E5.toInt(), 0xFF00ACC1.toInt(),
            "phone_1", "https://mi-shop.com/catalog/smartphones/redmi-note-15/"),
        NewsItem(3, "Что значит «физические 8 ГБ»? Не ведитесь на 8+8",
            "Маркетплейсы пишут «16 ГБ (8+8)», но это 8 ГБ физической + 8 виртуальной из накопителя. В подборку попадают только модели с реальной RAM ≥ 8 ГБ.",
            "12.10.2026", "Гайд", 0xFF43A047.toInt(), 0xFF009688.toInt(),
            "phone_4", "https://www.mvideo.ru/blog/podborki/luchshie-smartfoni-stoimostyu-do-20-000-rublej"),
        NewsItem(4, "OZON vs Wildberries: где дешевле забирать в Челнах?",
            "OZON с картой дешевле в 6 случаях из 10 (минус 300–500 ₽), WB-кошелёк даёт −3% почти везде. Пункты выдачи на Сююмбике 61 и Набережночелнинском 41.",
            "11.10.2026", "Сравнение", 0xFFFF8F00.toInt(), 0xFFE11D2E.toInt(),
            "phone_14", "https://www.ozon.ru/category/mobilnye-telefony-15574/"),
        NewsItem(5, "Эльдорадо: акции и рассрочка 0-0-12 в Челнах",
            "ТЦ Омега, пр. Московский 140: трейд-ин, рассрочка и подарки к POCO, Infinix и TECNO. Все текущие акции — по ссылке.",
            "15.10.2026", "Акция", 0xFF6C4DFF.toInt(), 0xFFEC407A.toInt(),
            "phone_3", "https://www.eldorado.ru/actions/"),
        NewsItem(6, "DNS Челны: скидки недели на TECNO и Samsung",
            "Московский 128А, Набережночелнинский 10Б, Сююмбике 67В: ценопады до −23% на Spark 40 Pro и Galaxy A16. Все акции — по ссылке.",
            "15.10.2026", "Акция", 0xFF00897B.toInt(), 0xFF1E88E5.toInt(),
            "phone_2", "https://www.dns-shop.ru/actions/"),
        NewsItem(7, "М.Видео: промо и бонусы М.Клуб в Торговом квартале",
            "пр. Мира 49А: до 10% бонусами на realme и Samsung, рассрочка 0-0-12. Все промо — по ссылке.",
            "15.10.2026", "Акция", 0xFFE11D2E.toInt(), 0xFFEC407A.toInt(),
            "phone_9", "https://www.mvideo.ru/promo"),
        NewsItem(8, "В каталоге уже 16 моделей: что добавилось",
            "Galaxy A16, POCO C85, HONOR X7d, Hot 50 Pro+, realme C75 и Camon 40 Pro 5G — все с физической RAM 8 ГБ и ценами 14–19 тыс ₽. Полный топ — у Т-Ж.",
            "15.10.2026", "Каталог", 0xFF1E88E5.toInt(), 0xFF7B1FA2.toInt(),
            "phone_11", "https://t-j.ru/best-smartphones-below-20k/"),
        NewsItem(9, "Батареи-гиганты до 20к: 7000 мА·ч — норма",
            "Pova 7 Neo, realme C85 Pro и POCO M7 — по 7000 мА·ч, два дня без розетки. Зарядка 33–45 Вт возвращает 50% за ~25 минут.",
            "15.10.2026", "Железо", 0xFF43A047.toInt(), 0xFF009688.toInt(),
            "phone_5", "https://ichip.ru/podborki/smartfony-planshety/smartfony-do-20000-rublej-shest-luchshih-modelej-774632"),
        NewsItem(10, "Как выжать −15% сверху: бонусы, трейд-ин, кэшбэк",
            "Бонусы магазинов 5–10%, трейд-ин −3000 ₽, кэшбэк картой «Электроника» до 5%. Пошаговый разбор — по ссылке.",
            "15.10.2026", "Гайд", 0xFFFF8F00.toInt(), 0xFF6C4DFF.toInt(),
            "phone_8", "https://rev.games/blog/kak-poluchit-skidku-techniku")
    )
}
