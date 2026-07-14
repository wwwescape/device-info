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

# Keep stack traces readable in crash reports.
-keepattributes SourceFile,LineNumberTable

# Glance widgets dispatch clicks and updates by reconstructing classes from a stored name
# (ActionCallback implementations, the GlanceAppWidgetReceivers themselves) outside our own
# call graph, so R8 can't trace those references — keep the whole package intact rather than
# risk a silent ClassNotFoundException the next time a widget is tapped or refreshed.
-keep class com.wwwescape.deviceinfo.widget.** { *; }
