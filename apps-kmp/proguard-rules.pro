# ProGuard rules for FairAir Android app

# Keep Kotlin metadata for serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclasseswithmembers class **$$serializer {
    <fields>;
}

-keep class kotlinx.serialization.**

# Ktor client
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**

# Koin
-keepclassmembers class * { public <init>(...); }
-keep class org.koin.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Voyager
-keep class cafe.adriel.voyager.** { *; }

# Keep contract model classes (shared DTOs)
-keep class com.fairair.contract.** { *; }

# Keep app API classes
-keep class com.fairair.app.api.** { *; }

# OkHttp (used by Ktor on Android)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Standard Android
-keepattributes Signature
-keepattributes Exceptions
