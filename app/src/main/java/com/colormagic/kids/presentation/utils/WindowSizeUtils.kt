package com.colormagic.kids.presentation.utils

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

enum class DeviceType { PHONE, TABLET }

// Returns TABLET for medium (600dp+) and expanded (840dp+) widths.
// Phone layouts are used on compact widths (< 600dp).
fun WindowSizeClass.toDeviceType(): DeviceType =
    if (widthSizeClass == WindowWidthSizeClass.Compact) DeviceType.PHONE else DeviceType.TABLET

fun WindowSizeClass.isTablet(): Boolean = toDeviceType() == DeviceType.TABLET
