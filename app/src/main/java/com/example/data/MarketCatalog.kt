package com.example.data

enum class MarketCategory(
    val id: String,
    val labelAr: String,
    val subtitleAr: String,
) {
    ALL("all", "الكل", "جميع الأقسام"),
    GAS("gas", "أسطوانات الغاز", "توصيل وتبديل فوري"),
    PRODUCE("produce", "خضار وفواكه", "طازجة من المزرعة"),
    BAKERY_DAIRY("bakery_dairy", "ألبان ومخبوزات", "خبز يومي وأجبان"),
    MEATS("meats", "لحوم ودواجن", "طازجة ومبردة"),
    PANTRY("pantry", "مونة ومنزل", "زيوت، حبوب ومناديل"),
}

enum class ProductVisualTone {
    GAS_BLUE,
    PRODUCE_GREEN,
    BAKERY_GOLD,
    MEAT_CRIMSON,
    PANTRY_AMBER,
}

data class MarketProduct(
    val id: String,
    val nameAr: String,
    val unitAr: String,
    val descriptionAr: String,
    val category: MarketCategory,
    val price: Double,
    val badgeAr: String? = null,
    val featured: Boolean = false,
    val visualTone: ProductVisualTone,
)

object MarketCatalog {
    const val FREE_DELIVERY_THRESHOLD = 20.0
    const val STANDARD_DELIVERY_FEE = 1.50

    val products: List<MarketProduct> = listOf(
        // Cooking Gas Cylinders
        MarketProduct(
            id = "gas_12kg",
            nameAr = "أسطوانة غاز منزلية كبيرة",
            unitAr = "12 كغ (تبديل)",
            descriptionAr = "أسطوانة غاز طهي منزلية مفحوصة ومختومة مع توصيل سريع وتركيب آمن.",
            category = MarketCategory.GAS,
            price = 7.00,
            badgeAr = "الأكثر طلباً",
            featured = true,
            visualTone = ProductVisualTone.GAS_BLUE,
        ),
        MarketProduct(
            id = "gas_5kg",
            nameAr = "أسطوانة غاز صغيرة",
            unitAr = "5 كغ",
            descriptionAr = "مثالية للرحلات والتخييم والمطابخ الصغيرة بوزن خفيف وأمان عالٍ.",
            category = MarketCategory.GAS,
            price = 4.25,
            badgeAr = "توصيل فوري",
            featured = true,
            visualTone = ProductVisualTone.GAS_BLUE,
        ),
        MarketProduct(
            id = "gas_regulator",
            nameAr = "منظم غاز أمان مع خرطوم",
            unitAr = "طقم كامل",
            descriptionAr = "منظم ضغط عالي الجودة مزود بصمام أمان وخرطوم معتمد بطول 1.5 متر.",
            category = MarketCategory.GAS,
            price = 5.50,
            featured = false,
            visualTone = ProductVisualTone.GAS_BLUE,
        ),

        // Fresh Produce
        MarketProduct(
            id = "tomato_1kg",
            nameAr = "طماطم بلدية حمراء",
            unitAr = "1 كغ",
            descriptionAr = "طماطم ناضجة مقطوفة حديثاً غنية بالنكهة للسلطات والطبخ اليومي.",
            category = MarketCategory.PRODUCE,
            price = 0.85,
            badgeAr = "طازج اليوم",
            featured = true,
            visualTone = ProductVisualTone.PRODUCE_GREEN,
        ),
        MarketProduct(
            id = "lettuce_head",
            nameAr = "خس أخضر طازج",
            unitAr = "حبة كاملة",
            descriptionAr = "أوراق خس مقرمشة ومغسولة بعناية لتحضير أشهى السلطات.",
            category = MarketCategory.PRODUCE,
            price = 0.70,
            featured = false,
            visualTone = ProductVisualTone.PRODUCE_GREEN,
        ),
        MarketProduct(
            id = "yellow_pepper",
            nameAr = "فلفل أصفر حلو",
            unitAr = "500 غ",
            descriptionAr = "فلفل رومي أصفر مقرمش وغني بالفيتامينات للشواء والسلطات.",
            category = MarketCategory.PRODUCE,
            price = 1.25,
            featured = false,
            visualTone = ProductVisualTone.PRODUCE_GREEN,
        ),
        MarketProduct(
            id = "broccoli_fresh",
            nameAr = "بروكلي طازج",
            unitAr = "1 حبة (~450 غ)",
            descriptionAr = "زهرات بروكلي خضراء طازجة مثالية للطبخ الصحي والشوربات.",
            category = MarketCategory.PRODUCE,
            price = 1.15,
            featured = false,
            visualTone = ProductVisualTone.PRODUCE_GREEN,
        ),
        MarketProduct(
            id = "cucumber_1kg",
            nameAr = "خيار بلدي مقرمش",
            unitAr = "1 كغ",
            descriptionAr = "خيار طازج صغير الحجم بطعم بلدي أصيل.",
            category = MarketCategory.PRODUCE,
            price = 0.90,
            featured = false,
            visualTone = ProductVisualTone.PRODUCE_GREEN,
        ),

        // Bakery & Dairy
        MarketProduct(
            id = "baguette_fresh",
            nameAr = "خبز باجيت فرنسي",
            unitAr = "رغيف طازج",
            descriptionAr = "مخبوز صباحاً بقشرة ذهبية مقرمشة ولب طري دافئ.",
            category = MarketCategory.BAKERY_DAIRY,
            price = 0.65,
            badgeAr = "مخبوز الآن",
            featured = true,
            visualTone = ProductVisualTone.BAKERY_GOLD,
        ),
        MarketProduct(
            id = "milk_bottle_1l",
            nameAr = "حليب طازج كامل الدسم",
            unitAr = "1 لتر زجاجي",
            descriptionAr = "حليب بقري طبيعي مبستر معبأ في عبوة زجاجية تحفظ الطعم الطازج.",
            category = MarketCategory.BAKERY_DAIRY,
            price = 1.60,
            badgeAr = "مزارع طبيعية",
            featured = true,
            visualTone = ProductVisualTone.BAKERY_GOLD,
        ),
        MarketProduct(
            id = "cheese_aged_250g",
            nameAr = "جبنة صفراء شيدر فاخرة",
            unitAr = "250 غ",
            descriptionAr = "جبنة غنية معتقة مثالية للسندويشات والباستا والأطباق الساخنة.",
            category = MarketCategory.BAKERY_DAIRY,
            price = 2.95,
            featured = false,
            visualTone = ProductVisualTone.BAKERY_GOLD,
        ),

        // Fresh Meats
        MarketProduct(
            id = "beef_steak_500g",
            nameAr = "لحم بقري طازج (ستيك)",
            unitAr = "500 غ",
            descriptionAr = "قطع لحم بقري طازج مختارة بعناية من أجود الذبائح اليومية.",
            category = MarketCategory.MEATS,
            price = 5.90,
            badgeAr = "ذبح اليوم",
            featured = true,
            visualTone = ProductVisualTone.MEAT_CRIMSON,
        ),
        MarketProduct(
            id = "minced_beef_500g",
            nameAr = "لحم مفروم طازج",
            unitAr = "500 غ",
            descriptionAr = "لحم بقري مفروم طازج بنسبة دهن متوازنة لجميع الوصفات.",
            category = MarketCategory.MEATS,
            price = 4.75,
            featured = false,
            visualTone = ProductVisualTone.MEAT_CRIMSON,
        ),
        MarketProduct(
            id = "chicken_breast_1kg",
            nameAr = "صدور دجاج طازجة مسحبة",
            unitAr = "1 كغ",
            descriptionAr = "صدور دجاج مبردة ونظيفة خالية من العظم والجلد.",
            category = MarketCategory.MEATS,
            price = 3.80,
            featured = false,
            visualTone = ProductVisualTone.MEAT_CRIMSON,
        ),

        // Pantry & Household
        MarketProduct(
            id = "cooking_oil_18l",
            nameAr = "زيت طهي نباتي نقي",
            unitAr = "1.8 لتر",
            descriptionAr = "زيت ذرة ودوار شمس خفيف مثالي للقلي والطبخ اليومي.",
            category = MarketCategory.PANTRY,
            price = 3.40,
            featured = false,
            visualTone = ProductVisualTone.PANTRY_AMBER,
        ),
        MarketProduct(
            id = "pickles_jar_650g",
            nameAr = "مخلل خيار مقرمش",
            unitAr = "برطمان 650 غ",
            descriptionAr = "مخلل خيار محضر بالطريقة البيتية التقليدية بنكهة الثوم والأعشاب.",
            category = MarketCategory.PANTRY,
            price = 1.75,
            featured = false,
            visualTone = ProductVisualTone.PANTRY_AMBER,
        ),
        MarketProduct(
            id = "flour_bag_1kg",
            nameAr = "دقيق أبيض فاخر للمخبوزات",
            unitAr = "1 كغ",
            descriptionAr = "دقيق قمح ناعم متعدد الاستخدامات للمعجنات والحلويات.",
            category = MarketCategory.PANTRY,
            price = 1.10,
            featured = false,
            visualTone = ProductVisualTone.PANTRY_AMBER,
        ),
        MarketProduct(
            id = "tissue_pack_6",
            nameAr = "مناديل ورقية منزلية فاخرة",
            unitAr = "عبوة 6 رولات",
            descriptionAr = "مناديل مطبخ وحمام ناعمة وعالية الامتصاص بطبقتين.",
            category = MarketCategory.PANTRY,
            price = 2.60,
            featured = false,
            visualTone = ProductVisualTone.PANTRY_AMBER,
        ),
    )

    private val byId = products.associateBy { it.id }

    fun findById(productId: String): MarketProduct? = byId[productId]
}
