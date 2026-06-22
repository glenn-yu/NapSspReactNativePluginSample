# Plugin RN bridge/modules/view-managers AND nap mx (AdMixer SSP) vendor SDK
# 모두 com.nasmedia.admixerssp 패키지에 있으므로 한 규칙으로 보존된다.
# (플러그인 패키지는 com.gwangy → com.nasmedia.admixerssp 로 변경됨)
-keep class com.nasmedia.admixerssp.** { *; }
-dontwarn com.nasmedia.admixerssp.**
-keep interface com.nasmedia.admixerssp.** { *; }

# React Native bridge classes are referenced from JavaScript and the package registry.
-keep class com.facebook.react.** { *; }
-dontwarn com.facebook.react.**

# nap mx 미디에이션 어댑터 — 사용하는 네트워크만 포함됨(없으면 규칙은 무시됨).
# Mediation adapters — only those you actually include are present.
-keep class com.nasmedia.admanager.** { *; }
-keep class com.nasmedia.adfit.** { *; }
-keep class com.nasmedia.pangle.** { *; }
-keep class com.nasmedia.applovin.** { *; }
-keep class com.nasmedia.unity.** { *; }
-keep class com.nasmedia.naveradmanager.** { *; }
-keep class com.nasmedia.teads.** { *; }
-keep class com.nasmedia.mobwith.** { *; }
-dontwarn com.nasmedia.**

# ℹ️ 각 어댑터 AAR 은 자체 consumer-rules.pro 를 포함하므로 위 규칙은 추가 안전망입니다.
# Each adapter AAR ships its own consumer rules; the above are an extra safety net.
