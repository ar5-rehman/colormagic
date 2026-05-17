# ── ColorMagic Kids — R8 / ProGuard rules ──────────────────────────────
#
# Hilt, Room, Compose, Navigation, Coroutines and Biometric all ship their
# own *consumer* rules inside their AARs, so the release build needs almost
# nothing project-specific. The rules below cover crash-report readability
# and a couple of defensive keeps.

# Keep source file + line numbers so release crash stack traces stay
# meaningful. mapping.txt under build/outputs/mapping/release/ de-obfuscates.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotation metadata Hilt / Room rely on at runtime.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# ── Domain & UI state models ───────────────────────────────────────────
# Not serialised today, but keeping their members is cheap insurance
# against R8 stripping fields a future backend (Room DTOs, JSON parsing)
# would reach via reflection.
-keep class com.colormagic.kids.domain.model.** { *; }

# ── Kotlin coroutines ──────────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**

# If WebView with a JS bridge is ever added, keep the interface here:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
