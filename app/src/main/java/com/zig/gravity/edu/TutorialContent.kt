package com.zig.gravity.edu

/**
 * First-launch tutorial content.
 *
 * Deliberately a plain Kotlin object with no Android and no Compose in it, so the content — the
 * part that actually has to be *correct* — can be unit-tested on the JVM.
 *
 * ### Every claim here was checked against the code, not remembered
 *
 * The whole risk with onboarding is that it describes an app that does not exist. Each step below
 * corresponds to a control that is really implemented:
 *
 *  * camera gestures — one finger drags a body, two fingers pan/pinch/twist the table
 *    (`GravitySandboxRoot`'s single `awaitEachGesture` machine);
 *  * selection opens the inspector (`open_inspector` / `InspectorSheet`);
 *  * the add control is a wordless circular `+` (`hud_add`);
 *  * dragging commits position only — velocity and mass are restored bit-for-bit on release;
 *  * the speed rungs are read from [com.zig.gravity.physics.EngineConstants.SPEED_LABELS] at
 *    runtime rather than written out here, so this text can never drift from the real ladder.
 */
data class TutorialStep(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val bodyFa: String,
    val bodyEn: String,
    /** Which part of the screen this step is about, so the overlay can point at it. */
    val focus: TutorialFocus,
    /** The gesture animation to demonstrate, if any. */
    val gesture: TutorialGesture = TutorialGesture.NONE
)

/** Where on the screen the spotlight sits. Resolved to real coordinates by the overlay. */
enum class TutorialFocus { NONE, CANVAS, TOP_BAR, ADD_BUTTON, HUD_BAR }

/** The finger animation played inside the step's illustration. */
enum class TutorialGesture { NONE, DRAG, PINCH, TAP }

object TutorialContent {

    const val SKIP_FA = "رد کردن"
    const val SKIP_EN = "Skip"
    const val NEXT_FA = "بعدی"
    const val NEXT_EN = "Next"
    const val BACK_FA = "قبلی"
    const val BACK_EN = "Back"
    const val FINISH_FA = "شروع آزمایش"
    const val FINISH_EN = "Start experimenting"

    const val OPEN_HELP_FA = "باز کردن آموزش آزمایشگاه گرانش"
    const val OPEN_HELP_EN = "Open Gravity Sandbox tutorial"
    const val SKIP_A11Y_FA = "رد کردن آموزش"
    const val SKIP_A11Y_EN = "Skip tutorial"

    /**
     * The seven steps. Short on purpose: the target is under a minute, and everything else in the
     * sandbox is discoverable once these are understood.
     */
    val steps: List<TutorialStep> = listOf(
        TutorialStep(
            id = "welcome",
            titleFa = "به میز گرانش خوش آمدید",
            titleEn = "Welcome to the gravity table",
            bodyFa = "این یک میز آزمایش گرانش است. اجسام را بچینید و ببینید تغییرِ جرم، فاصله و سرعت، " +
                "چگونه سرنوشت سامانه را عوض می‌کند. هر چیزی که می‌بینید نتیجهٔ محاسبهٔ واقعی است، نه انیمیشن.",
            bodyEn = "This is a gravity workbench. Arrange bodies and watch how changing mass, distance " +
                "and speed changes what the system does. Everything you see is computed, not animated.",
            focus = TutorialFocus.NONE
        ),
        TutorialStep(
            id = "camera",
            titleFa = "حرکت در صحنه",
            titleEn = "Moving around",
            bodyFa = "با دو انگشت، میز را جابه‌جا کنید. برای بزرگ‌نمایی دو انگشت را باز یا جمع کنید و " +
                "با چرخاندن آن‌ها، میز را بچرخانید. زاویهٔ دید را هم می‌توانید از دکمهٔ دوربین تغییر دهید.",
            bodyEn = "Move the table with two fingers. Pinch to zoom, and twist to rotate it. " +
                "The camera button also lets you change the viewing angle.",
            focus = TutorialFocus.CANVAS,
            gesture = TutorialGesture.PINCH
        ),
        TutorialStep(
            id = "select",
            titleFa = "انتخاب و ویرایش",
            titleEn = "Select and edit",
            bodyFa = "روی هر جسم ضربه بزنید تا انتخاب شود. سپس می‌توانید جرم، سرعت، مکان و اندازهٔ آن را " +
                "تغییر دهید — و همان‌جا دکمهٔ «دنبال کن» هست تا دوربین آن جسم را دنبال کند.",
            bodyEn = "Tap any body to select it. You can then change its mass, speed, position and size — " +
                "and the same panel has a Follow button that makes the camera track it.",
            focus = TutorialFocus.CANVAS,
            gesture = TutorialGesture.TAP
        ),
        TutorialStep(
            id = "add",
            titleFa = "افزودن جسم",
            titleEn = "Adding bodies",
            bodyFa = "با دکمهٔ گردِ + جسم تازه اضافه کنید: خورشید، سیاره، ماه، سیارک، جسم آزمایشی، " +
                "سیاه‌چاله یا کرم‌چاله.",
            bodyEn = "The round + button adds a new body: a star, a planet, a moon, an asteroid, " +
                "a test object, a black hole or a wormhole.",
            focus = TutorialFocus.ADD_BUTTON,
            gesture = TutorialGesture.TAP
        ),
        TutorialStep(
            id = "drag",
            titleFa = "جابه‌جا کردن اجسام",
            titleEn = "Moving a body",
            bodyFa = "یک جسم را با یک انگشت بکشید تا مکانش عوض شود. جرم و سرعتش دست‌نخورده می‌ماند، " +
                "مگر خودتان تغییرشان دهید. هنگام کشیدن، مسیر پیش‌بینی‌شده را هم می‌بینید.",
            bodyEn = "Drag a body with one finger to move it. Its mass and velocity stay exactly as they " +
                "were unless you change them yourself. While you drag, you also see its predicted path.",
            focus = TutorialFocus.CANVAS,
            gesture = TutorialGesture.DRAG
        ),
        TutorialStep(
            id = "time",
            titleFa = "زمان و سرعت",
            titleEn = "Time and speed",
            // The speed rungs are injected at render time from EngineConstants, never hard-coded.
            bodyFa = "با دکمهٔ پخش/مکث شبیه‌سازی را متوقف یا اجرا کنید و با ضریب‌های سرعت، " +
                "گرانش را تندتر تماشا کنید. دکمهٔ بازنشانی، آزمایش و دوربین را به حالت اول برمی‌گرداند.",
            bodyEn = "Play and pause the simulation, and use the speed multipliers to watch gravity unfold " +
                "faster. Reset returns both the experiment and the camera to how they started.",
            focus = TutorialFocus.HUD_BAR
        ),
        TutorialStep(
            id = "discover",
            titleFa = "آزادانه آزمایش کنید",
            titleEn = "Experiment freely",
            bodyFa = "از «صحنه‌ها» یک آزمایش آماده بردارید، یا خودتان یکی بسازید. هر وقت اتفاق جالبی بیفتد، " +
                "کارت آموزشی توضیحش را نشان می‌دهد. این راهنما همیشه از دکمهٔ ؟ در دسترس است.",
            bodyEn = "Pick a ready-made experiment from Scenes, or build your own. When something interesting " +
                "happens, a teaching card explains it. This guide is always available from the ? button.",
            focus = TutorialFocus.TOP_BAR
        )
    )

    val stepCount: Int get() = steps.size
}
