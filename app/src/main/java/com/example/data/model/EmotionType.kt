package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class EmotionType(
    val titleArabic: String,
    val descriptionArabic: String,
    val supportMessageArabic: String,
    val primaryColor: Color,
    val backgroundColor: Color,
    val isPositiveOrStable: Boolean,
    val needsAttention: Boolean
) {
    JOY(
        titleArabic = "الفرح",
        descriptionArabic = "طاقة إيجابية وشعور بالإنجاز والرضا",
        supportMessageArabic = "رائع جداً! استمتع بلحظتك الإيجابية وشارك هذا النور مع من حولك. طاقتك مصدر إلهام لزملائك.",
        primaryColor = EmotionJoy,
        backgroundColor = EmotionJoyBg,
        isPositiveOrStable = true,
        needsAttention = false
    ),
    CALM(
        titleArabic = "الهدوء",
        descriptionArabic = "سكينة وسلام داخلي وارتياح عميق",
        supportMessageArabic = "الهدوء قوة ثمينة. هذا التوازن يساعدك على التركيز والإبداع وتجاوز يومك بنقاء ويسر.",
        primaryColor = EmotionCalm,
        backgroundColor = EmotionCalmBg,
        isPositiveOrStable = true,
        needsAttention = false
    ),
    SAD(
        titleArabic = "الحزن",
        descriptionArabic = "مشاعر هبوط، ضيق أو فقدان الحماس",
        supportMessageArabic = "من الطبيعي جداً أن نشعر بالحزن أحياناً. أنت لست وحدك، ومشاعرك محترمة ولها مساحة آمنة لتمر بسلام.",
        primaryColor = EmotionSad,
        backgroundColor = EmotionSadBg,
        isPositiveOrStable = false,
        needsAttention = true
    ),
    ANXIOUS(
        titleArabic = "القلق",
        descriptionArabic = "تسارع أفكار، توتر وتوجس من القادم",
        supportMessageArabic = "توقف لبرهة وتنفس بعمق.. معظم ما نخشاه يمر بسلام. جرب تمرين التنفس المهدئ الآن لاستعادة ثباتك.",
        primaryColor = EmotionAnxious,
        backgroundColor = EmotionAnxiousBg,
        isPositiveOrStable = false,
        needsAttention = true
    ),
    EXHAUSTED(
        titleArabic = "الإرهاق",
        descriptionArabic = "تعب جسدي أو ذهني واستنزاف للطاقة",
        supportMessageArabic = "جسدك وعقلك يطلبان استراحة مستحقة. امنح نفسك قسطاً من الراحة، فالإنجاز الحقيقي يبدأ بصحتك.",
        primaryColor = EmotionExhausted,
        backgroundColor = EmotionExhaustedBg,
        isPositiveOrStable = false,
        needsAttention = false
    ),
    BORED(
        titleArabic = "الملل",
        descriptionArabic = "فقدان الشغف والاهتمام بالروتين اليومي",
        supportMessageArabic = "الملل أحياناً بداية الشرارة لاكتشاف شيء جديد وشغف مختلف. ابحث عن فكرة صغيرة تكسر الروتين.",
        primaryColor = EmotionBored,
        backgroundColor = EmotionBoredBg,
        isPositiveOrStable = true,
        needsAttention = false
    ),
    ANGRY(
        titleArabic = "الغضب",
        descriptionArabic = "انفعال حاد، استياء أو إحساس بعدم الإنصاف",
        supportMessageArabic = "غضبك مؤشر على أنك تهتم أو تشعر بالظلم. خذ نفساً عميقاً حتى تهدأ العاصفة، لتتخذ القرار الأفضل بحكمة.",
        primaryColor = EmotionAngry,
        backgroundColor = EmotionAngryBg,
        isPositiveOrStable = false,
        needsAttention = true
    ),
    FEAR(
        titleArabic = "الخوف",
        descriptionArabic = "رهبة أو تخوف من موقف أو مواجهة قادمة",
        supportMessageArabic = "الشجاعة ليست غياب الخوف، بل مواجهته بلطف. المدرسة بيئة أمان، ومرشدوك هنا لمساندتك دائماً.",
        primaryColor = EmotionFear,
        backgroundColor = EmotionFearBg,
        isPositiveOrStable = false,
        needsAttention = true
    );

    val emoji: String
        get() = when (this) {
            JOY -> "😊"
            CALM -> "🌿"
            SAD -> "🌧️"
            ANXIOUS -> "⚡"
            EXHAUSTED -> "🔋"
            BORED -> "⏳"
            ANGRY -> "🔥"
            FEAR -> "🛡️"
        }

    val arabicTitle: String
        get() = titleArabic
}

val ALL_REASON_CATEGORIES = listOf(
    "الدراسة والاختبارات",
    "الأصدقاء والعلاقات",
    "المنزل والأسرة",
    "الضغوطات والمسؤوليات",
    "التنمر أو المضايقة",
    "الصحة والنوم والنشاط",
    "شعور عام بدون سبب محدد"
)
