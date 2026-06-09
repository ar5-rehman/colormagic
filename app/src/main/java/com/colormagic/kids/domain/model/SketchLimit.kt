package com.colormagic.kids.domain.model

// Parent-controlled cap on how many sketches the kid can generate in one day.
// Independent of the Firebase credit quota — even a Pro account with 50
// monthly sketches can be locally throttled by the parent.
//
// [perDay] == null means Unlimited (the local throttle is off; the Firebase
// quota is still the upper bound). A non-null value is the exact daily cap,
// which can be one of the presets OR any custom number the parent enters.
data class SketchLimit(val perDay: Int?) {
    /** Chip label — the number, or "∞" for unlimited. */
    val label: String get() = perDay?.toString() ?: "∞"

    companion object {
        val Unlimited = SketchLimit(null)

        /** Quick-pick options shown in the parent picker (besides Custom). */
        val presets: List<SketchLimit> = listOf(
            SketchLimit(1),
            SketchLimit(3),
            SketchLimit(5),
            SketchLimit(7),
            SketchLimit(10),
            Unlimited
        )

        /** Sensible bounds for a custom value. */
        const val MIN_CUSTOM = 1
        const val MAX_CUSTOM = 99
    }
}
