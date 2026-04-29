# ProGuard rules for Smart Voice Assistant

# Keep MediaPipe classes
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Keep Room entities and DAOs
-keep class com.smartvoice.assistant.data.local.** { *; }

# Keep model classes (used by Room and serialization)
-keep class com.smartvoice.assistant.data.model.** { *; }

# Keep accessibility service
-keep class com.smartvoice.assistant.service.accessibility.VoiceAccessibilityService { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# AndroidX Lifecycle
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }

# General Android rules
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes InnerClasses,EnclosingMethod

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
