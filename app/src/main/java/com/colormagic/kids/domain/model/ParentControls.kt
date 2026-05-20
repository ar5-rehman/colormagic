package com.colormagic.kids.domain.model

// On-device parent-control toggles. Persisted via ParentControlsStore.
//
// Note: this lives on the device only, not in Firestore — these are gentle
// guardrails on how the kid interacts with the app, not a security boundary.
// Firebase credits + prompt safety + App Check are the real lines of defence.
data class ParentControls(
    /** When false, the prompt input is read-only — the kid can only generate
     *  by tapping a category chip (random idea from that category). */
    val allowFreeText: Boolean = true,
    /** Per-day local cap. Independent of (and lower than) the Firebase quota. */
    val dailyLimit: SketchLimit = SketchLimit.Unlimited,
    /** How many sketches the kid has finished today. */
    val sketchesToday: Int = 0,
    /** Day stamp the counter belongs to. Days since 1970-01-01 in the device's
     *  local timezone — a new day's first sketch resets the counter. */
    val dayStamp: Long = 0L
) {
    /** True when the kid has hit today's local cap. Unlimited never blocks. */
    val dailyLimitReached: Boolean
        get() = dailyLimit.perDay?.let { sketchesToday >= it } ?: false
}
