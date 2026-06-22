# 네이티브 광고 에셋 & 레이아웃 가이드 (Native Assets & Layout Guide)

> KO: `NativeAd`와 인라인 `VideoAd`는 매체사 앱의 디자인에 맞게 커스텀 레이아웃을 구성해야 합니다. 이 문서는 v2(Android `admixer-ssp` 2.0.0 / iOS `AdMixerMediation` 2.3.7) 기준 플랫폼별 네이티브 레이아웃 구성 방법을 안내합니다.
> EN: `NativeAd` and the inline `VideoAd` require a custom layout that matches your app's design. This document explains how to build native layouts per platform for v2 (Android `admixer-ssp` 2.0.0 / iOS `AdMixerMediation` 2.3.7).

---

## 1. 개요 (Overview)

> KO: 나스미디어 SDK는 네이티브 광고의 각 요소(제목, 아이콘, 미디어 뷰 등)를 매체사가 직접 정의한 뷰에 바인딩하는 방식을 사용합니다. v2부터 Android는 `NativeAdViewBinder`로만 바인딩하며 구버전의 `setViewIds()` 호출은 제거되었습니다.
> EN: The Nasmedia SDK binds each native ad element (title, icon, media view, etc.) to views you define yourself. From v2, Android binds exclusively through `NativeAdViewBinder`; the legacy `setViewIds()` call has been removed.

> KO: **이 RN 플러그인은 위 네이티브 바인딩(NativeAdViewBinder 생성·View 매핑)을 내부적으로 처리합니다.** 따라서 앱(JS) 개발자가 직접 해야 하는 일은 **올바른 View ID/프로퍼티 이름을 가진 레이아웃을 제공**하는 것뿐입니다.
> EN: **This RN plugin performs the native binding (creating the `NativeAdViewBinder` and mapping views) internally.** The only thing app (JS) developers must do is **provide a layout whose View IDs / property names match the expected names below.**

---

## 2. Android (XML Layout)

> KO: `res/layout/` 디렉토리에 레이아웃 파일을 생성하십시오.
> EN: Create a layout file under the `res/layout/` directory.

### 2.1 필수 구성 요소 (Required Views / View IDs)

> KO: 플러그인이 `NativeAdViewBinder`로 인지할 수 있도록 아래 ID와 클래스를 정확히 사용하십시오. **v2부터 모든 View ID에 `nap_mx_` 접두사가 적용됩니다.**
> EN: Use exactly the IDs and classes below so the plugin can wire them via `NativeAdViewBinder`. **From v2, every View ID uses the `nap_mx_` prefix.**

| 요소 (Element) | View ID | 뷰 타입 (View Type) |
| :--- | :--- | :--- |
| Icon | `nap_mx_native_icon` | `ImageView` |
| Title | `nap_mx_native_title` | `TextView` |
| Advertiser | `nap_mx_native_adv` | `TextView` |
| Body | `nap_mx_native_desc` | `TextView` |
| Media (이미지/동영상, image/video) | `nap_mx_native_main` | `com.nasmedia.admixerssp.common.nativeads.NativeMainAdView` |
| CTA | `nap_mx_native_cta` | `Button` |

### 2.2 레이아웃 예시 (Layout Example)

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="12dp">

    <ImageView
        android:id="@+id/nap_mx_native_icon"
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:layout_alignParentStart="true"
        android:layout_alignParentTop="true"
        android:scaleType="centerCrop" />

    <TextView
        android:id="@+id/nap_mx_native_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentTop="true"
        android:layout_marginStart="8dp"
        android:layout_toEndOf="@id/nap_mx_native_icon"
        android:maxLines="2"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/nap_mx_native_adv"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_below="@id/nap_mx_native_title"
        android:layout_marginStart="8dp"
        android:layout_toEndOf="@id/nap_mx_native_icon" />

    <TextView
        android:id="@+id/nap_mx_native_desc"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_below="@id/nap_mx_native_icon"
        android:layout_marginTop="8dp"
        android:maxLines="3" />

    <com.nasmedia.admixerssp.common.nativeads.NativeMainAdView
        android:id="@+id/nap_mx_native_main"
        android:layout_width="match_parent"
        android:layout_height="200dp"
        android:layout_below="@id/nap_mx_native_desc"
        android:layout_marginTop="8dp">

        <ImageView
            android:id="@+id/nap_mx_native_main_image"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scaleType="fitXY" />

    </com.nasmedia.admixerssp.common.nativeads.NativeMainAdView>

    <Button
        android:id="@+id/nap_mx_native_cta"
        android:layout_width="match_parent"
        android:layout_height="44dp"
        android:layout_below="@id/nap_mx_native_main"
        android:layout_marginTop="8dp" />

</RelativeLayout>
```

> KO: 참고(내부 동작) — v2에서 네이티브 광고 클래스는 `NativeAdView` → `AMMNativeAdView`로 변경되었고, 바인딩은 `NativeAdViewBinder`만 사용합니다. 플러그인이 이를 흡수하므로 앱 코드 변경은 필요 없습니다.
> EN: Note (internal) — in v2 the native ad class was renamed `NativeAdView` → `AMMNativeAdView`, and binding uses `NativeAdViewBinder` only. The plugin absorbs this, so no app code change is required.

---

## 3. iOS (XIB 구성, XIB Setup)

> KO: Xcode에서 `.xib` 파일을 생성하고 뷰 클래스를 아래와 같이 매핑하십시오.
> EN: Create a `.xib` file in Xcode and map the view classes as below.

### 구성 절차 (Steps)

> KO:
> 1. 플러그인에 기본 제공되는 **`AMMNativeAdView.xib`** 파일을 프로젝트에 추가합니다.
> 2. 루트 `UIView`의 클래스를 `AMMNativeAdView`로 설정합니다.
> 3. 내부 구성 요소들을 아래 프로퍼티에 연결합니다. **프로퍼티 이름이 정확히 일치해야** 합니다.
>
> EN:
> 1. Add the bundled **`AMMNativeAdView.xib`** file to your project.
> 2. Set the root `UIView`'s class to `AMMNativeAdView`.
> 3. Connect the inner subviews to the properties below. **Property names must match exactly.**

| 프로퍼티 이름 (Property) | 뷰 타입 (View Type) | 설명 (Description) |
| :--- | :--- | :--- |
| `iv_icon` | `UIImageView` | 아이콘 이미지 / Icon image |
| `l_headline` | `UILabel` | 제목 / Title |
| `l_advertiser` | `UILabel` | 광고주명 / Advertiser |
| `l_description` | `UILabel` | 본문 설명 / Body |
| `media` | `AMMMediaView` | 메인 이미지/동영상 / Main media |
| `b_cta` | `UIButton` | CTA 버튼 / CTA button |

> KO: iOS에는 Teads 어댑터가 없습니다(Teads는 Android 전용).
> EN: There is no Teads adapter on iOS (Teads is Android-only).

---

## 4. 플러그인 연결 (Wiring it Up)

> KO: 레이아웃이 준비되면 `NativeAd` 컴포넌트의 스타일과 `adUnitId`를 통해 연동됩니다. 실제 네이티브 바인딩 로직은 플러그인 내부(`NapSspNativeAdView`)에서 자동으로 수행되므로, 앱 개발자는 위 View ID/프로퍼티 이름만 맞추면 됩니다.
> EN: Once the layout is ready, it is connected through the `NativeAd` component's style and `adUnitId`. The actual native binding runs automatically inside the plugin (`NapSspNativeAdView`), so app developers only need to match the View IDs / property names above.

> KO: 상세한 네이티브 SDK 가이드는 공식 가이드 https://napmx.github.io/ 를 참조하십시오.
> EN: For detailed native SDK guidance, see the official guide at https://napmx.github.io/.
