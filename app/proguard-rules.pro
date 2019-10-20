# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/franco/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn org.jaudiotagger.**
-dontwarn com.google.android.gms.**
-dontwarn com.crashlytics.**

-keep class com.gracenote.** { *; }
-keep class org.jaudiotagger.** { *; }
-keep class androidx.appcompat.widget.SearchView { *; }
-keep class com.google.** { *; }
-keep class com.android.** { *; }
#Fabric uses annotations internally
-keepattributes *Annotation*
#In order to provide the most meaningful crash reports
-keepattributes SourceFile,LineNumberTable
#Crashlytics will still function without this rule, but your crash reports will not include proper file names or line numbers.
#if using custom exceptions:
-keep public class * extends java.lang.Exception
#To skip running ProGuard on Crashlytics
-keep class com.crashlytics.** { *; }
#Exclude R from ProGuard to enable the font addon auto detection
-keep class .R
-keep class **.R$* {
    <fields>;
}