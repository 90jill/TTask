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
#-renamesourcefileattribute SoopenurceFile

# public 以外を難読化
-keep public class cutboss.support.util.** { public *; }

# 依存関係
# ./gradlew assembleRelease --stacktrace
# Reading library jar [/Users/***/Library/Android/sdk/platforms/android-27/optional/org.apache.http.legacy.jar]
# Note: duplicate definition of library class [org.apache.http.params.HttpConnectionParams]
# ./gradlew tktask:dependencies | grep "org.apache.httpcomponents:httpcore"
#        |    |    |    +--- org.apache.httpcomponents:httpcore:4.2.5
#        |    |         +--- org.apache.httpcomponents:httpcore:4.1 -> 4.2.5
-dontnote android.net.http.**
-dontnote org.apache.http.**

# Warning: jp.co.lib.tkato.tktask.Task: can't find referenced class java.lang.invoke.LambdaMetafactory
-dontwarn java.lang.invoke.**
