# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# flurry
-dontwarn com.flurry.**

# parse
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**
-dontwarn com.facebook.**

# pushwoosh
-keep class com.pushwoosh.** { *; }
-keep class com.arellomobile.** { *; }
-dontwarn com.pushwoosh.**
-dontwarn com.arellomobile.**

# appsFlyer
-keep class com.appsflyer.** { *; }
-dontwarn com.appsflyer.**
-dontwarn com.android.installreferrer

# myTarget
# Remove this dontwarn when MyTarget is updated to 4.6.15
-dontwarn com.my.target.nativeads.mediation.**
# Remove this dontwarn when MyTarget is updated to 4.6.16
-dontwarn com.my.target.core.net.cookie.**
-dontwarn com.mopub.**

-dontoptimize
-keepattributes **

# Gson support
-keep class com.mapswithme.util.Gsonable
-keep class * implements com.mapswithme.util.Gsonable

-keepclassmembernames class * implements com.mapswithme.util.Gsonable {
  !transient <fields>;
}

-keepnames class * implements com.mapswithme.util.Gsonable {
  !transient <fields>;
}

-keepclassmembers class * implements com.mapswithme.util.Gsonable {
  <init>(...);
}