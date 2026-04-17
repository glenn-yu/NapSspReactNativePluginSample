-keep class com.napsspplugin.** { *; }
-dontwarn com.napsspplugin.**

# React Native bridge classes are referenced from JavaScript and the package registry.
-keep class com.facebook.react.** { *; }
-dontwarn com.facebook.react.**

# The official nap ssp / mediation SDK packages are intentionally resolved at build time.
# Keep warnings quiet until the concrete SDK APIs are wired.
-dontwarn io.github.nasmedia-tech.**
-dontwarn com.google.android.gms.**
