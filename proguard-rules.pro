# General ProGuard rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AppsFlyer rules
-keep class com.appsflyer.** { *; }
-dontwarn com.appsflyer.**

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**

# Android X
-keep class androidx.** { *; }
-dontwarn androidx.**
