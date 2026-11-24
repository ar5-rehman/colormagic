package com.colormagic.kids.domain.model

import java.util.Calendar

/**
 * Seasonal content switch. Everything Christmas-related (category, idea
 * cards, daily idea, stickers) appears automatically from Nov 20 to Jan 6 on
 * the device's local date and hides itself afterwards, so no app update is
 * needed to turn it off. (Calendar, not java.time: minSdk is 24.)
 */
object Season {

    /** True from Nov 20 through Jan 6 (inclusive), across the new year. */
    fun isChristmas(now: Calendar = Calendar.getInstance()): Boolean {
        val month = now.get(Calendar.MONTH) // 0-based
        val day = now.get(Calendar.DAY_OF_MONTH)
        return (month == Calendar.NOVEMBER && day >= 20) ||
            month == Calendar.DECEMBER ||
            (month == Calendar.JANUARY && day <= 6)
    }
}
