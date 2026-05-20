package com.colormagic.kids.domain.model

// Parent-controlled cap on how many sketches the kid can generate in one day.
// Independent of the Firebase credit quota — even a Pro account with 50
// monthly sketches can be locally throttled to 5/day by the parent.
//
// `Unlimited` disables the local throttle (Firebase quota is still the
// upper bound).
enum class SketchLimit(val label: String, val perDay: Int?) {
    Five("5", 5),
    Ten("10", 10),
    Unlimited("∞", null)
}
