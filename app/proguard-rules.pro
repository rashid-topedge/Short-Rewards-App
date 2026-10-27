# ─────────────────────────────────────────────────────────────────────────────
# R8 rules for Short Rewards (release builds).
#
# Most libraries ship their own rules (Compose, Room, OkHttp, Media3, Coil,
# Firebase, coroutines). These cover what's specific to this app.
# ─────────────────────────────────────────────────────────────────────────────

# Readable stack traces in crash reports (mapping.txt de-obfuscates names).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── kotlinx.serialization ────────────────────────────────────────────────────
# Ktor looks serializers up by type at runtime (`body<T>()` / `setBody(x)`), via
# the generated `Companion.serializer()`. Keep those for the app's models.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.dramflix.android.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class com.dramflix.android.**
-keepclassmembers class <1>$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.dramflix.android.**$$serializer { *; }

# ── Ktor ─────────────────────────────────────────────────────────────────────
# Optional JVM-only references Ktor checks for at runtime; absent on Android.
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.util.debug.**

# ── Credential Manager (Sign in with Google) ─────────────────────────────────
# The Play Services provider is loaded by reflection.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
    *;
}
