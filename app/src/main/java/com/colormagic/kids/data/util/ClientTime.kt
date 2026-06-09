package com.colormagic.kids.data.util

import java.util.Calendar

/**
 * The device's current UTC offset in minutes — i.e. minutes to ADD to UTC to
 * reach local time (DST-aware). UTC+5 → 300, US-Pacific (PST) → -480.
 *
 * Sent to the backend so daily credits reset at the user's LOCAL midnight. The
 * server only uses this to position the day boundary; the actual "has a new day
 * started" check is anchored to the trusted server clock, so changing the
 * device clock can't farm extra credits.
 *
 * Uses Calendar (API 1+) — ZONE_OFFSET + DST_OFFSET, in millis — instead of
 * java.time.OffsetDateTime (API 26+), so it runs on every supported device.
 */
fun currentUtcOffsetMinutes(): Int {
    val cal = Calendar.getInstance()
    val offsetMillis = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
    return offsetMillis / 60_000
}
