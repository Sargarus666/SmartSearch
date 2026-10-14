package ru.chelny.smartscan

data class StoreOffer(
    val shop: String,
    val address: String,
    val price: Int,
    val note: String,
    val url: String,
    val online: Boolean
)

data class Phone(
    val id: Int,
    val brand: String,
    val model: String,
    val ramGb: Int,          // физическая RAM
    val storageGb: Int,
    val price: Int,          // минимальная цена 10-20к
    val oldPrice: Int,
    val promo: String,       // акция
    val rating: Double,
    val screen: String,
    val battery: String,
    val camera: String,
    val cpu: String,
    val colorStart: Int,
    val colorEnd: Int,
    val photoUrl: String,
    val photoRes: String,
    val accent: String,      // цвет корпуса текстом
    val offline: List<StoreOffer>,
    val online: List<StoreOffer>
) {
    val discountPct: Int get() =
        if (oldPrice > price) ((oldPrice - price) * 100 / oldPrice) else 0
}

object Catalog {
    // СВЕЖЕЕ: топ-пересечение рейтингов осени-2026 (МТС 10/2026, Т-Ж 09/2026, iChip 08/2026,
    // Price.ru 06/2026, kod.ru, M.Video топ-15, Hi-Tech Mail). Проверено 14.10.2026.
    // Только модели из 2+ свежих топов, только физическая RAM 8 ГБ, только 10 000–20 000 ₽.
    // POCO M8 Pro исключён: в октябре 2026 стоит 25–28 тыс. — вылетел из бюджета.
    val phones: List<Phone> = listOf(
        Phone(
            id = 1, brand = "Xiaomi", model = "Redmi Note 15 8/256",
            ramGb = 8, storageGb = 256, price = 19899, oldPrice = 21990,
            promo = "Проверено 14.10.2026 • −9% • камера 108 Мп, хит топов",
            rating = 4.9, screen = "6.77\" AMOLED 120 Гц, 2392×1080",
            battery = "6000 мА·ч, 45 Вт", camera = "108+2 Мп, OIS + 32 Мп фронт",
            cpu = "Helio G100 Ultra", colorStart = 0xFF1A2A6C.toInt(), colorEnd = 0xFF2EBF91.toInt(),
            accent = "Midnight Black",
            photoUrl = "https://www.mobiledokan.com/media/xiaomi-redmi-note-15-4g-forest-green-official-image.webp",
            photoRes = "phone_1",
            offline = listOf(
                StoreOffer("DNS", "Московский пр-т, 128А, Челны", 19899, "RU-версия, самовывоз сегодня", "https://www.dns-shop.ru", false),
                StoreOffer("М.Видео", "пр. Мира, 49А, ТРК Торговый квартал", 20490, "Рассрочка 0-0-12", "https://www.mvideo.ru", false),
                StoreOffer("Эльдорадо", "пр. Московский, 140, ТЦ Омега", 20690, "Трейд-ин −3000 ₽", "https://www.eldorado.ru", false)
            ),
            online = listOf(
                StoreOffer("Яндекс Маркет", "Доставка в Челны 1–2 дня", 20500, "С картой Пэй дешевле", "https://market.yandex.ru", true),
                StoreOffer("OZON", "Пункт выдачи: Челны, пр. Сююмбике 61", 19990, "Купон продавца", "https://www.ozon.ru", true),
                StoreOffer("Wildberries", "ПВЗ Челны, Набережночелнинский 41", 20290, "WB-кошелёк −3%", "https://www.wildberries.ru", true)
            )
        ),
        Phone(
            id = 2, brand = "TECNO", model = "Spark 40 Pro 8/256",
            ramGb = 8, storageGb = 256, price = 14999, oldPrice = 17990,
            promo = "Проверено 14.10.2026 • −17% • топ МТС и M.Video",
            rating = 4.8, screen = "6.78\" AMOLED 1.5K, 144 Гц, 4500 нит",
            battery = "5200 мА·ч, 45 Вт", camera = "50 Мп + 13 Мп фронт",
            cpu = "Helio G100 Ultimate", colorStart = 0xFF141E30.toInt(), colorEnd = 0xFF4B6CB7.toInt(),
            accent = "Moon Titanium",
            photoUrl = "https://fdn2.gsmarena.com/vv/pics/tecno/techno-spark-40-pro-1.jpg",
            photoRes = "phone_2",
            offline = listOf(
                StoreOffer("DNS", "Набережночелнинский пр-т, 10Б", 14999, "Хит, витрина + склад", "https://www.dns-shop.ru", false),
                StoreOffer("МТС", "пр. Хасана Туфана, 12, Челны", 15490, "Скидка при переносе номера", "https://shop.mts.ru", false),
                StoreOffer("Билайн", "пр. Сююмбике, 2/19, Челны", 15990, "Пакет + связь 3 мес.", "https://beeline.ru", false)
            ),
            online = listOf(
                StoreOffer("OZON", "Пункт выдачи Челны, Московский 140", 14490, "Минус 500 ₽ по карте", "https://www.ozon.ru", true),
                StoreOffer("DNS-Shop онлайн", "Самовывоз Сююмбике 67В", 14999, "Онлайн-бронь −2%", "https://www.dns-shop.ru", true),
                StoreOffer("Ситилинк", "Доставка в Челны", 15290, "Бонусы Ситиклаб", "https://www.citilink.ru", true)
            )
        ),
        Phone(
            id = 3, brand = "Infinix", model = "Note Edge 8/256",
            ramGb = 8, storageGb = 256, price = 19490, oldPrice = 21990,
            promo = "Проверено 14.10.2026 • −11% • Dimensity 7100, лучший для игр",
            rating = 4.8, screen = "6.78\" AMOLED 120 Гц, Gorilla 7i",
            battery = "6500 мА·ч, 45 Вт", camera = "50 Мп + 13 Мп фронт",
            cpu = "Dimensity 7100", colorStart = 0xFF3A1C71.toInt(), colorEnd = 0xFFD76D77.toInt(),
            accent = "Shadow Black",
            photoUrl = "https://www.mobilemaya.com/public/media/infinix-note-edge-5g-official-image.webp",
            photoRes = "phone_3",
            offline = listOf(
                StoreOffer("Эльдорадо", "пр. Московский, 140, ТЦ Омега", 19490, "Ростест, гарантия 12 мес.", "https://www.eldorado.ru", false),
                StoreOffer("DNS", "пр-т Сююмбике, 67В, Челны", 19990, "В наличии, 5G-версия", "https://www.dns-shop.ru", false),
                StoreOffer("МегаФон", "Набережночелнинский пр-т, 41", 20490, "Тариф в подарок", "https://megafon.ru", false)
            ),
            online = listOf(
                StoreOffer("Wildberries", "ПВЗ Челны, пр. Мира 52", 19690, "Оплата WB-кошельком −3%", "https://www.wildberries.ru", true),
                StoreOffer("Яндекс Маркет", "Доставка в Челны за день", 19890, "Промокод на первый заказ", "https://market.yandex.ru", true),
                StoreOffer("OZON", "Пункт выдачи Челны, Татарстан 25", 19790, "Рассрочка 0-0-6", "https://www.ozon.ru", true)
            )
        ),
        Phone(
            id = 4, brand = "TECNO", model = "Pova 7 Neo 8/256",
            ramGb = 8, storageGb = 256, price = 15490, oldPrice = 17490,
            promo = "Проверено 14.10.2026 • −11% • 7000 мА·ч, рекорд автономности",
            rating = 4.7, screen = "6.78\" AMOLED 120 Гц, 2460×1080",
            battery = "7000 мА·ч, 45 Вт + реверс", camera = "108+2 Мп + 8 Мп фронт",
            cpu = "Helio G100 Ultimate", colorStart = 0xFF0F2027.toInt(), colorEnd = 0xFF2C5364.toInt(),
            accent = "Neon Black",
            photoUrl = "https://hamariweb.com/mobiles/LargeImages/6933_01.jpg",
            photoRes = "phone_4",
            offline = listOf(
                StoreOffer("DNS", "Московский пр-т, 128А, Челны", 15490, "Топ iChip и Т-Ж", "https://www.dns-shop.ru", false),
                StoreOffer("М.Видео", "пр. Мира, 49А, Торговый квартал", 15990, "Бонусы М.Клуб 10%", "https://www.mvideo.ru", false)
            ),
            online = listOf(
                StoreOffer("OZON", "ПВЗ Челны, пр. Чулман 23", 15190, "Цена с Ozon-картой", "https://www.ozon.ru", true),
                StoreOffer("Яндекс Маркет", "Доставка в Челны 1–3 дня", 15490, "Кэшбэк баллами", "https://market.yandex.ru", true)
            )
        ),
        Phone(
            id = 5, brand = "realme", model = "C85 Pro 8/256",
            ramGb = 8, storageGb = 256, price = 16599, oldPrice = 18999,
            promo = "Проверено 14.10.2026 • −13% • IP69 + 7000 мА·ч",
            rating = 4.7, screen = "6.8\" AMOLED 120 Гц",
            battery = "7000 мА·ч, 45 Вт", camera = "50 Мп + 8 Мп фронт",
            cpu = "Snapdragon 685", colorStart = 0xFFF7971E.toInt(), colorEnd = 0xFFFFD200.toInt(),
            accent = "Peacock Green",
            photoUrl = "https://mobiledevices.com.pk/images/realme/realme-c85-pro-peacock-green.webp",
            photoRes = "phone_5",
            offline = listOf(
                StoreOffer("М.Видео", "пр. Мира, 49А, Торговый квартал", 16599, "Официальная гарантия", "https://www.mvideo.ru", false),
                StoreOffer("DNS", "Набережночелнинский пр-т, 10Б", 16990, "Самовывоз сегодня", "https://www.dns-shop.ru", false),
                StoreOffer("Эльдорадо", "пр. Московский, 140", 17490, "Рассрочка 0-0-10", "https://www.eldorado.ru", false)
            ),
            online = listOf(
                StoreOffer("Яндекс Маркет", "Доставка в Челны", 16590, "Сплит без переплат", "https://market.yandex.ru", true),
                StoreOffer("OZON", "ПВЗ Челны", 16790, "Цена с картой Ozon", "https://www.ozon.ru", true)
            )
        ),
        Phone(
            id = 6, brand = "Samsung", model = "Galaxy A17 8/256",
            ramGb = 8, storageGb = 256, price = 19099, oldPrice = 21499,
            promo = "Проверено 14.10.2026 • −11% • 6 лет обновлений Samsung",
            rating = 4.6, screen = "6.7\" Super AMOLED 90 Гц",
            battery = "5000 мА·ч, 25 Вт", camera = "50+5+2 Мп, 13 Мп фронт",
            cpu = "Exynos 1330", colorStart = 0xFF000428.toInt(), colorEnd = 0xFF004E92.toInt(),
            accent = "Blue Black",
            photoUrl = "https://clickbuy.com.vn/uploads/news/anh-render-galaxy-a17-4g-8584470723638779187.jpg",
            photoRes = "phone_6",
            offline = listOf(
                StoreOffer("МТС", "пр. Хасана Туфана, 12", 19099, "Шесть лет обновлений", "https://shop.mts.ru", false),
                StoreOffer("DNS", "пр-т Сююмбике, 67В", 19490, "Витрина + склад", "https://www.dns-shop.ru", false),
                StoreOffer("М.Видео", "пр. Мира, 49А", 19690, "Рассрочка 0-0-10", "https://www.mvideo.ru", false)
            ),
            online = listOf(
                StoreOffer("М.Видео онлайн", "Доставка в Челны 2 дня", 19099, "Онлайн-эксклюзив", "https://www.mvideo.ru", true),
                StoreOffer("Wildberries", "ПВЗ Челны", 19290, "Быстрая выдача", "https://www.wildberries.ru", true)
            )
        ),
        Phone(
            id = 7, brand = "POCO", model = "M7 8/256",
            ramGb = 8, storageGb = 256, price = 17990, oldPrice = 19990,
            promo = "Проверено 14.10.2026 • −10% • 7000 мА·ч, топ Geekchoice",
            rating = 4.6, screen = "6.9\" IPS 144 Гц",
            battery = "7000 мА·ч, 33 Вт", camera = "50 Мп + 8 Мп фронт",
            cpu = "Snapdragon 685", colorStart = 0xFFCB0000.toInt(), colorEnd = 0xFF3A0000.toInt(),
            accent = "Power Black",
            photoUrl = "https://cdn.movertix.com/media/catalog/product/cache/image/1200x/x/i/xiaomi-poco-m7-dual-sim-silver-256gb-and-8gb-ram_2.jpg",
            photoRes = "phone_7",
            offline = listOf(
                StoreOffer("DNS", "Московский пр-т, 128А", 17990, "Фирменный стенд POCO", "https://www.dns-shop.ru", false),
                StoreOffer("Эльдорадо", "пр. Московский, 140", 18490, "Подарок: наушники", "https://www.eldorado.ru", false)
            ),
            online = listOf(
                StoreOffer("OZON", "ПВЗ Челны", 17590, "Купон продавца −400 ₽", "https://www.ozon.ru", true),
                StoreOffer("Яндекс Маркет", "Доставка в Челны", 17990, "Баллы Маркета", "https://market.yandex.ru", true)
            )
        ),
        Phone(
            id = 8, brand = "HONOR", model = "X8c 8/256",
            ramGb = 8, storageGb = 256, price = 16999, oldPrice = 19999,
            promo = "Проверено 14.10.2026 • −15% • AMOLED + 108 Мп OIS",
            rating = 4.6, screen = "6.7\" AMOLED 120 Гц",
            battery = "5000 мА·ч, 35 Вт", camera = "108 Мп OIS + 50 Мп фронт",
            cpu = "Snapdragon 685", colorStart = 0xFF134E5E.toInt(), colorEnd = 0xFF71B280.toInt(),
            accent = "Emerald Lake",
            photoUrl = "https://cdn.beebom.com/mobile/honor-x8c-front-and-back.png",
            photoRes = "phone_8",
            offline = listOf(
                StoreOffer("DNS", "Набережночелнинский пр-т, 10Б", 16999, "Топ Hi-Tech Mail", "https://www.dns-shop.ru", false),
                StoreOffer("Билайн", "пр. Сююмбике, 2/19", 17490, "Комплект со связью", "https://beeline.ru", false)
            ),
            online = listOf(
                StoreOffer("Wildberries", "ПВЗ Челны", 16790, "Скидка WB-кошелька", "https://www.wildberries.ru", true),
                StoreOffer("OZON", "ПВЗ Челны", 16990, "Рассрочка", "https://www.ozon.ru", true)
            )
        ),
        Phone(
            id = 9, brand = "realme", model = "14 5G 8/256",
            ramGb = 8, storageGb = 256, price = 17599, oldPrice = 19999,
            promo = "Проверено 14.10.2026 • −12% • Snapdragon 6 Gen 4 + 6000 мА·ч",
            rating = 4.7, screen = "6.67\" AMOLED 120 Гц",
            battery = "6000 мА·ч, 45 Вт", camera = "50+2 Мп + 16 Мп фронт",
            cpu = "Snapdragon 6 Gen 4", colorStart = 0xFF8E2DE2.toInt(), colorEnd = 0xFF4A00E0.toInt(),
            accent = "Mecha Silver",
            photoUrl = "https://image01.realme.net/general/20250403/174367344340048fc865b1978495aaf45046bd9a508b3.png",
            photoRes = "phone_9",
            offline = listOf(
                StoreOffer("М.Видео", "пр. Мира, 49А", 17599, "Топ Price.ru 06/2026", "https://www.mvideo.ru", false),
                StoreOffer("DNS", "пр-т Сююмбике, 67В", 17990, "Самовывоз", "https://www.dns-shop.ru", false)
            ),
            online = listOf(
                StoreOffer("Яндекс Маркет", "Доставка в Челны", 17599, "5G-версия, IP69", "https://market.yandex.ru", true),
                StoreOffer("OZON", "ПВЗ Челны", 17790, "Быстрая доставка", "https://www.ozon.ru", true)
            )
        ),
        Phone(
            id = 10, brand = "TECNO", model = "Camon 40 8/256",
            ramGb = 8, storageGb = 256, price = 17199, oldPrice = 18999,
            promo = "Проверено 14.10.2026 • −9% • выбор Т-Ж + ИИ Ella",
            rating = 4.6, screen = "6.78\" AMOLED 120 Гц",
            battery = "5200 мА·ч, 45 Вт", camera = "50 Мп OIS + 32 Мп фронт",
            cpu = "Helio G100", colorStart = 0xFF232526.toInt(), colorEnd = 0xFF414345.toInt(),
            accent = "Emerald Lake Green",
            photoUrl = "https://cdn.kalvo.com/uploads/img/gallery/69477-tecno-camon-40-6.jpg",
            photoRes = "phone_10",
            offline = listOf(
                StoreOffer("Эльдорадо", "пр. Московский, 140", 17199, "Рассрочка 0-0-12", "https://www.eldorado.ru", false),
                StoreOffer("DNS", "Московский пр-т, 128А", 17490, "В наличии", "https://www.dns-shop.ru", false)
            ),
            online = listOf(
                StoreOffer("OZON", "ПВЗ Челны", 16890, "Цена с картой Ozon", "https://www.ozon.ru", true),
                StoreOffer("Ситилинк", "Доставка в Челны", 17190, "Клубные бонусы", "https://www.citilink.ru", true)
            )
        )
    )
}
