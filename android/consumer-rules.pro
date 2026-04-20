# Keep the plugin bridge, modules, and placeholder view classes available to host apps.
-keep class com.gwangy.** { *; }
-dontwarn com.gwangy.**

# React Native bridge classes are referenced from JavaScript and the package registry.
-keep class com.facebook.react.** { *; }
-dontwarn com.facebook.react.**

# Nap SSP vendor SDK and mediation adapter keep rules (required for release builds).
-keep class com.nasmedia.admixerssp.** { *; }
-dontwarn com.nasmedia.admixerssp.**
-keep class com.nasmedia.admanager.** { *; }
-dontwarn com.nasmedia.admanager.**
-keep class com.nasmedia.adfit.** { *; }
-dontwarn com.nasmedia.adfit.**
-keep class com.pangle.** { *; }
-dontwarn com.pangle.**
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**
-keep interface com.nasmedia.** { *; }
