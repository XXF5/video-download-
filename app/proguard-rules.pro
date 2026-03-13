# Add project specific ProGuard rules here.

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Data Models
-keep class com.videodownloader.data.models.** { *; }

# JSoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# General
-keep public class * {
    public protected *;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Video URLs
-keep class * {
    @com.videodownloader.core.extractor.** *;
}
