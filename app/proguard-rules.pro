# Add project-specific ProGuard rules here.

# Kotlin metadata (suppress R8 warnings for newer Kotlin versions)
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations

# Google Play Services Auth
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google API Client
-keep class com.google.api.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.** { *; }
-dontwarn com.google.api.**

# Google HTTP Client
-keep class com.google.http.** { *; }
-dontwarn com.google.http.**

# Gson (used by Google API)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# OkHttp (for REST API calls)
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Apache HTTP Client (legacy, references unavailable Java SE classes)
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
