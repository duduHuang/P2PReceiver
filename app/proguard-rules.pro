# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\NedHuang\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
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

-keep class com.via.p2p.VIAManager {
    public *;
}

-keep class com.via.p2p.DefaultSetting {
    public final static *;
}

-keep class com.via.p2preceiver.libnice {
    public *;
    private *;
    protected *;
}

-keep class com.via.p2preceiver.libnice$* {
    public *;
}

#####################################################################
# libnice
-dontwarn **.JSONArray
-dontwarn **.libnice.**

-keep interface com.via.p2preceiver.libnice$* { *; }

-keepclassmembers class com.via.p2preceiver.libnice { *; }
-keepclassmembers class com.via.p2preceiver.libnice$ReceiveCallback { *; }
-keepclassmembers class com.via.p2preceiver.libnice$StateObserver { *; }

-keep class com.via.p2preceiver.libnice.** { *; }

-dontwarn com.via.p2preceiver.libnice.**
#####################################################################

-optimizationpasses 5

-keepattributes InnerClasses

-keepattributes Exceptions