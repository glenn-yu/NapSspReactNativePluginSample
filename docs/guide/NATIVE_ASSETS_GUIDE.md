# Native Assets & Layout Guide

`NativeAd` 및 `VideoAd`는 매체사 앱의 디자인에 맞게 커스텀 레이아웃을 구성해야 합니다. 이 문서는 플랫폼별 네이티브 레이아웃 구성 방법을 안내합니다.

---

## 1. 개요

나스미디어 SDK는 네이티브 광고의 각 요소(제목, 아이콘, 미디어 뷰 등)를 매체사가 직접 정의한 뷰에 바인딩하는 방식을 사용합니다.

---

## 2. Android (XML Layout)

`res/layout/` 디렉토리에 레이아웃 파일을 생성하십시오.

### 필수 구성 요소 (View ID)
SDK(`NativeAdViewBinder`)에서 인지할 수 있도록 아래 ID와 클래스를 정확히 사용하십시오.

| 요소 | View ID | 뷰 타입 |
| :--- | :--- | :--- |
| Icon | `nap_ssp_native_icon` | `ImageView` |
| Title | `nap_ssp_native_title` | `TextView` |
| Advertiser | `nap_ssp_native_adv` | `TextView` |
| Body | `nap_ssp_native_desc` | `TextView` |
| Media (이미지/동영상) | `nap_ssp_native_main` | `com.nasmedia.admixerssp.common.nativeads.NativeMainAdView` |
| CTA | `nap_ssp_native_cta` | `Button` |

### 레이아웃 예시

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="12dp">

    <ImageView
        android:id="@+id/nap_ssp_native_icon"
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:layout_alignParentStart="true"
        android:layout_alignParentTop="true"
        android:scaleType="centerCrop" />

    <TextView
        android:id="@+id/nap_ssp_native_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentTop="true"
        android:layout_marginStart="8dp"
        android:layout_toEndOf="@id/nap_ssp_native_icon"
        android:maxLines="2"
        android:textStyle="bold" />

    <TextView
        android:id="@+id/nap_ssp_native_adv"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_below="@id/nap_ssp_native_title"
        android:layout_marginStart="8dp"
        android:layout_toEndOf="@id/nap_ssp_native_icon" />

    <TextView
        android:id="@+id/nap_ssp_native_desc"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_below="@id/nap_ssp_native_icon"
        android:layout_marginTop="8dp"
        android:maxLines="3" />

    <com.nasmedia.admixerssp.common.nativeads.NativeMainAdView
        android:id="@+id/nap_ssp_native_main"
        android:layout_width="match_parent"
        android:layout_height="200dp"
        android:layout_below="@id/nap_ssp_native_desc"
        android:layout_marginTop="8dp">

        <ImageView
            android:id="@+id/nap_ssp_native_main_image"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:scaleType="fitXY" />

    </com.nasmedia.admixerssp.common.nativeads.NativeMainAdView>

    <Button
        android:id="@+id/nap_ssp_native_cta"
        android:layout_width="match_parent"
        android:layout_height="44dp"
        android:layout_below="@id/nap_ssp_native_main"
        android:layout_marginTop="8dp" />

</RelativeLayout>
```

---

## 3. iOS (XIB 구성)

Xcode에서 `.xib` 파일을 생성하고 뷰 클래스를 아래와 같이 매핑하십시오.

### 구성 절차
1. 플러그인에 기본 제공되는 **`AMMNativeAdView.xib`** 파일을 프로젝트에 추가합니다.
2. `UIView` 클래스를 `AMMNativeAdView`로 설정합니다.
3. 내부 구성 요소들을 아래 프로퍼티에 연결합니다. **프로퍼티 이름이 정확히 일치해야** 합니다.

| 프로퍼티 이름 | 뷰 타입 | 설명 |
| :--- | :--- | :--- |
| `iv_icon` | `UIImageView` | 아이콘 이미지 |
| `l_headline` | `UILabel` | 제목 |
| `l_advertiser` | `UILabel` | 광고주명 |
| `l_description` | `UILabel` | 본문 설명 |
| `media` | `AMMMediaView` | 메인 이미지/동영상 |
| `b_cta` | `UIButton` | CTA 버튼 |

---

## 4. 플러그인 연결

레이아웃이 준비되면 `NativeAd` 컴포넌트의 스타일과 `adUnitId`를 통해 연동됩니다. 실제 네이티브 바인딩 로직은 플러그인 내부의 `NapSspNativeAdView` 클래스에서 수행됩니다.

상세한 네이티브 SDK 가이드는 [Native SDK Guides](./native_guides/)를 참조하십시오.
