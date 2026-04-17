# Keep the plugin bridge, modules, and placeholder view classes available to host apps.
-keep class com.gwangy.** { *; }
-dontwarn com.gwangy.**

# React Native bridge classes are referenced from JavaScript and the package registry.
-keep class com.facebook.react.** { *; }
-dontwarn com.facebook.react.**

# Vendor SDK class names are intentionally omitted until the official Android API
# surface is wired. Add concrete keep rules here once the real nap ssp symbols are linked.
