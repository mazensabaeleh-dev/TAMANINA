package com.example.data.model

data class SchoolInitiative(
    val id: String,
    val title: String,
    val category: String,
    val triggerReason: String,
    val objective: String,
    val targetAudience: String,
    val duration: String,
    val executionSteps: List<String>,
    val expectedImpact: String,
    val priorityLevel: String // "أولوية قصوى", "مهم وموصى به", "تعزيزي مستمر"
)

object InitiativeAnalyzer {

    fun generateInitiatives(
        checkIns: List<CheckInRecord>,
        periodTitle: String = "الفترة المحددة"
    ): Pair<EmotionType?, List<SchoolInitiative>> {
        if (checkIns.isEmpty()) {
            return Pair(null, getDefaultGeneralInitiatives())
        }

        // Calculate emotion frequencies
        val counts = mutableMapOf<EmotionType, Int>()
        for (item in checkIns) {
            try {
                val emotion = EmotionType.valueOf(item.emotion)
                counts[emotion] = (counts[emotion] ?: 0) + 1
            } catch (_: Exception) {}
        }

        val total = checkIns.size
        // Find dominant emotion
        val dominantEntry = counts.maxByOrNull { it.value }
        val dominantEmotion = dominantEntry?.key ?: EmotionType.CALM
        val dominantPercent = if (total > 0 && dominantEntry != null) (dominantEntry.value * 100) / total else 0

        val initiatives = mutableListOf<SchoolInitiative>()

        when (dominantEmotion) {
            EmotionType.ANXIOUS, EmotionType.EXHAUSTED -> {
                initiatives.add(
                    SchoolInitiative(
                        id = "INIT-01",
                        title = "مبادرة «واحة الهدوء» لإدارة قلق الاختبارات والواجبات",
                        category = "إرشاد أكاديمي ونفسي",
                        triggerReason = "رصد ارتفاع مشاعر ${dominantEmotion.arabicTitle} بنسبة ($dominantPercent%) خلال $periodTitle",
                        objective = "تزويد الطلاب باستراتيجيات علمية لتنظيم المذاكرة، والتخلص من التوتر المفرط قبل التقييمات.",
                        targetAudience = "جميع الفصول الدراسية والطلاب المعرضين لضغوط التقييم",
                        duration = "أسبوعين مكثفين",
                        executionSteps = listOf(
                            "عقد ورش إرشادية صغيرة (20 دقيقة) خلال حصص النشاط حول تقنيات البومودورو وجدولة المواد.",
                            "توفير ركن استرخاء هادئ في غرفة التوجيه الطلابي يحتوي على تمارين تنفس وإرشادات مطبوعة.",
                            "إرسال نشرة توعوية رقمية لأولياء الأمور عبر منصة مدرستي لتخفيف الضغوط المنزلية."
                        ),
                        expectedImpact = "انخفاض مؤشر القلق بنسبة 35% وزيادة الاستقرار الذهني أثناء الاختبارات.",
                        priorityLevel = "أولوية قصوى"
                    )
                )
                initiatives.add(
                    SchoolInitiative(
                        id = "INIT-02",
                        title = "برنامج «دقيقة استرخاء ذهني» بين الحصص",
                        category = "صحة نفسية يومية",
                        triggerReason = "تخفيف الإرهاق التراكمي للطلاب على مدار اليوم المدرسي",
                        objective = "إعادة شحن تركيز الطلاب وخفض التشتت الذهني بين الحصة والأخرى.",
                        targetAudience = "الصفوف الأكثر كثافة ونشاطاً",
                        duration = "طوال الشهر الجاري",
                        executionSteps = listOf(
                            "تدريب معلمي الحصص على تمرين تنفس مهدئ لمدة 60 ثانية في بداية الحصة الرابعة.",
                            "تشغيل مقاطع صوتية لطبيعة هادئة عبر الإذاعة المدرسية في فترات التبديل.",
                            "تخصيص ملصقات تذكيرية بتمارين التمدد والاسترخاء في الفصول."
                        ),
                        expectedImpact = "تحسين انتباه الطلاب وتراجع معدلات التعب والإجهاد بعد منتصف اليوم.",
                        priorityLevel = "مهم وموصى به"
                    )
                )
            }
            EmotionType.SAD, EmotionType.BORED, EmotionType.FEAR -> {
                initiatives.add(
                    SchoolInitiative(
                        id = "INIT-03",
                        title = "مبادرة «رفيق الإيجابية ودائرة الدعم النظير»",
                        category = "اندماج اجتماعي ومساندة",
                        triggerReason = "ظهور مشاعر ${dominantEmotion.arabicTitle} بنسبة ($dominantPercent%) خلال $periodTitle",
                        objective = "تعزيز الشعور بالانتماء، وكسر العزلة الاجتماعية للطلاب، ومكافحة الشعور بالوحدة.",
                        targetAudience = "الطلاب الذين يشعرون بالانعزال أو الملل المستمر",
                        duration = "شهر كامل",
                        executionSteps = listOf(
                            "تشكيل مجموعات نشاط تعاونية غير أكاديمية في الفسحة والمكتبة المدرسية.",
                            "إطلاق تحديات مدرسية تفاعلية أسبوعية (تحدي القراءة، الشطرنج، الألعاب الذهنية).",
                            "جلسات استماع فردية سرية مع الأخصائي النفسي للطلاب الأكثر احتياجاً."
                        ),
                        expectedImpact = "رفع معنويات الطلاب وتعزيز الصداقات الإيجابية ومعدلات الرضا المدرسي.",
                        priorityLevel = "أولوية قصوى"
                    )
                )
                initiatives.add(
                    SchoolInitiative(
                        id = "INIT-04",
                        title = "أسبوع «صوتك يهمنا» للتعبير والمشاركة الإبداعية",
                        category = "أنشطة لا صفية ومواهب",
                        triggerReason = "تحويل الطاقة المنخفضة ومشاعر الحزن إلى إبداع وإنتاجية",
                        objective = "منح الطلاب مساحة حرة للتعبير عن مواهبهم وتطلعاتهم بدون أحكام مسبقة.",
                        targetAudience = "جميع طلاب المدرسة",
                        duration = "5 أيام دراسية",
                        executionSteps = listOf(
                            "معرض فني للتعبير بالرسم والكتابة الأدبية الحرة في ساحة المدرسة.",
                            "صندوق مقترحات إيجابي لتطوير مرافق الفسحة والأنشطة المدرسية.",
                            "تكريم المساهمات الطلابية الإبداعية في الطابور الصباحي."
                        ),
                        expectedImpact = "زيادة الحيوية والدافعية الدراسية بنسبة تتجاوز 40%.",
                        priorityLevel = "مهم وموصى به"
                    )
                )
            }
            EmotionType.ANGRY -> {
                initiatives.add(
                    SchoolInitiative(
                        id = "INIT-05",
                        title = "مبادرة «الفسحة النشطة والتفريغ الإيجابي»",
                        category = "أنشطة رياضية وتفريغ انفعالي",
                        triggerReason = "رصد مشاعر توتر وانفعال (${dominantEmotion.arabicTitle}) لدى بعض الطلاب",
                        objective = "تفريغ الطاقات الزائدة والانفعالات عبر أنشطة بدنية صحية ومنظمة.",
                        targetAudience = "طلاب المراحل المتوسطة والثانوية",
                        duration = "3 أسابيع",
                        executionSteps = listOf(
                            "تنظيم دوريات كرة قدم وطائرة وكرة سلة سريعة في وقت الفسحة بإشراف معلم الرياضة.",
                            "توفير طاولات تنس طاولة وأنشطة حركية في الساحات المظللة.",
                            "ورشة حول «فن إدارة الغضب وحل النزاعات بذكاء» مع المرشد الطلابي."
                        ),
                        expectedImpact = "انخفاض المشاحنات الطلابية بنسبة 60% وزيادة الروح الرياضية الإيجابية.",
                        priorityLevel = "أولوية قصوى"
                    )
                )
            }
            EmotionType.JOY, EmotionType.CALM -> {
                initiatives.add(
                    SchoolInitiative(
                        id = "INIT-06",
                        title = "مبادرة «سفراء الطمأنينة والإلهام الطلابي»",
                        category = "تمكين وقيادة طلابية",
                        triggerReason = "سيادة مشاعر الاستقرار والإيجابية (${dominantEmotion.arabicTitle}) بنسبة ممتازة ($dominantPercent%)",
                        objective = "استثمار المناخ النفسي الإيجابي لتمكين الطلاب المتميزين في دعم زملائهم ونشر ثقافة الامتنان.",
                        targetAudience = "قادة الفصول والطلاب المتطوعون",
                        duration = "طوال الفصل الدراسي",
                        executionSteps = listOf(
                            "اختيار ممثلين للطمأنينة من كل فصل ليكونوا حلقة وصل دافئة مع المرشد.",
                            "إطلاق حملة «رسائل الامتنان والتقدير» المتبادلة بين المعلمين والطلاب.",
                            "توثيق الممارسات المدرسية المتميزة في تعزيز جودة الحياة المدرسية."
                        ),
                        expectedImpact = "ترسيخ بيئة مدرسية جاذبة ومستدامة تحتذي بها المدارس الأخرى.",
                        priorityLevel = "تعزيزي مستمر"
                    )
                )
                initiatives.add(
                    SchoolInitiative(
                        id = "INIT-07",
                        title = "يوم «الفرح والإنجاز المدرسي»",
                        category = "احتفاء وتحفيز",
                        triggerReason = "الاحتفاء بنجاح المجتمع المدرسي في الحفاظ على بيئة مطمئنة",
                        objective = "مكافأة الطلاب وتقدير جهود المعلمين والكادر الإرشادي.",
                        targetAudience = "كامل منسوبي المدرسة",
                        duration = "يوم دراسي كامل",
                        executionSteps = listOf(
                            "فعاليات ترفيهية ومسابقات ثقافية خفيفة في الساحة المدرسية.",
                            "منصات تصوير وتكريم للجهود الفردية والجماعية.",
                            "إعلان تقرير مؤشر الطمأنينة السنوي بفخر واعتزاز."
                        ),
                        expectedImpact = "مضاعفة الولاء المدرسي والشعور بالفخر والسكينة.",
                        priorityLevel = "مهم وموصى به"
                    )
                )
            }
        }

        return Pair(dominantEmotion, initiatives)
    }

    private fun getDefaultGeneralInitiatives(): List<SchoolInitiative> {
        return listOf(
            SchoolInitiative(
                id = "INIT-GEN-01",
                title = "برنامج «نبض المدرسة الدوري» للتوجيه الوقائي",
                category = "إرشاد وقائي عام",
                triggerReason = "متابعة دورية مستمرة لبناء قاعدة بيانات دقيقة للاستقرار النفسي",
                objective = "تشجيع الطلاب على التعبير المستمر واكتشاف التحديات قبل تفاقمها.",
                targetAudience = "جميع الطلاب",
                duration = "مستمر طوال العام",
                executionSteps = listOf(
                    "تذكير الطلاب يومياً بمسح رمز الباركود والاطمئنان على مشاعرهم.",
                    "متابعة دورية من المرشد الطلابي للتقارير الأسبوعية.",
                    "تخصيص حصص إرشاد جمعي للتعامل مع المتغيرات الدراسية."
                ),
                expectedImpact = "وقاية مبكرة ورفع مستوى الاستقرار النفسي العام.",
                priorityLevel = "مهم وموصى به"
            )
        )
    }
}
