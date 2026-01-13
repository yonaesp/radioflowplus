# Radio Android ProGuard Rules
# Optimized for release builds following Android 2024/2025 best practices

# ============================================
# KOTLIN SPECIFIC RULES
# ============================================

# Keep Kotlin metadata for reflection
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# Keep kotlin.Metadata for proper Kotlin reflection
-keep class kotlin.Metadata { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================
# MEDIA3 / EXOPLAYER RULES
# ============================================

# Keep Media3 session classes for media browser service
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.** { *; }

# Keep ExoPlayer internal classes needed for playback
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.exoplayer.dash.** { *; }

# Keep MediaItem and MediaMetadata
-keep class androidx.media3.common.MediaItem { *; }
-keep class androidx.media3.common.MediaItem$Builder { *; }
-keep class androidx.media3.common.MediaMetadata { *; }
-keep class androidx.media3.common.MediaMetadata$Builder { *; }

# Keep Player.Listener implementations
-keep class * implements androidx.media3.common.Player$Listener { *; }

# ============================================
# COMPOSE RULES  
# ============================================

# Keep Compose Preview annotations
-keep @androidx.compose.ui.tooling.preview.Preview class *

# Don't warn about Compose internal APIs
-dontwarn androidx.compose.**

# ============================================
# DATA CLASSES AND MODELS
# ============================================

# Keep RadioStation data class
-keep class com.radioandroid.data.RadioStation { *; }
-keep class com.radioandroid.data.RadioStations { *; }

# Keep sealed classes for state management
-keep class com.radioandroid.player.PlaybackState { *; }
-keep class com.radioandroid.player.PlaybackState$* { *; }

# ============================================
# SERVICE CLASSES
# ============================================

# Keep service classes that are referenced in AndroidManifest
-keep class com.radioandroid.service.RadioMediaService { *; }
-keep class com.radioandroid.timer.AlarmReceiver { *; }

# ============================================
# OPTIMIZATION SETTINGS
# ============================================

# Optimize more aggressively
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove Kotlin assertions in release
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNull(...);
    static void checkNotNullParameter(...);
    static void checkParameterIsNotNull(...);
    static void checkNotNullExpressionValue(...);
    static void throwUninitializedPropertyAccessException(...);
}

# ============================================
# DEBUG INFORMATION
# ============================================

# Keep source file and line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# Rename source file to reduce APK size
-renamesourcefileattribute SourceFile
