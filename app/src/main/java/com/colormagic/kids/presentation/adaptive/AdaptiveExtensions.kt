package com.colormagic.kids.presentation.adaptive

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass

// Width buckets — Compact (<600dp) / Medium (600-840dp) / Expanded (840dp+).
val WindowAdaptiveInfo.isCompactWidth: Boolean
    get() = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

val WindowAdaptiveInfo.isMediumWidth: Boolean
    get() = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
            !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

val WindowAdaptiveInfo.isExpandedWidth: Boolean
    get() = windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

// Height buckets — useful for landscape phone vs tablet decisions.
val WindowAdaptiveInfo.isCompactHeight: Boolean
    get() = !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
