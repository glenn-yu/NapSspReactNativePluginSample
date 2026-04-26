# Native Assets & Layout Guide

`NativeAd` 및 `VideoAd`는 매체사 앱의 디자인에 맞게 커스텀 레이아웃을 구성해야 합니다. 이 문서는 플랫폼별 네이티브 레이아웃 구성 방법을 안내합니다.

---

## 1. 개요

나스미디어 SDK는 네이티브 광고의 각 요소(제목, 아이콘, 미디어 뷰 등)를 매체사가 직접 정의한 뷰에 바인딩하는 방식을 사용합니다.

---

## 2. Android (XML Layout)

`res/layout/` 디렉토리에 레이아웃 파일을 생성하십시오.

### 필수 구성 요소 (View ID)
SDK에서 인지할 수 있도록 아래 ID를 사용하여 뷰를 배치하십시오.
- **Icon**: `com_nasmedia_ad_icon` (ImageView)
- **Title**: `com_nasmedia_ad_headline` (TextView)
- **Body**: `com_nasmedia_ad_body` (TextView)
- **Media**: `com_nasmedia_ad_media` (com.nasmedia.admixerssp.AMMediaView)
- **CTA**: `com_nasmedia_ad_call_to_action` (Button)
- **Advertiser**: `com_nasmedia_ad_advertiser` (TextView)

---

## 3. iOS (XIB 구성)

Xcode에서 `.xib` 파일을 생성하고 뷰 클래스를 아래와 같이 매핑하십시오.

### 구성 절차
1. `UIView` 클래스를 `AMMNativeAdView`로 설정합니다.
2. 내부 구성 요소들을 아래 아웃렛(Outlet)에 연결합니다.
   - `headlineView`: (UILabel)
   - `mediaView`: (AMMMediaView)
   - `callToActionView`: (UIButton)
   - `iconView`: (UIImageView)
   - `bodyView`: (UILabel)
   - `advertiserView`: (UILabel)

---

## 4. 플러그인 연결

레이아웃이 준비되면 `NativeAd` 컴포넌트의 스타일과 `adUnitId`를 통해 연동됩니다. 실제 네이티브 바인딩 로직은 플러그인 내부의 `NapSspNativeAdView` 클래스에서 수행됩니다.

상세한 네이티브 SDK 가이드는 [Native SDK Guides](./native_guides/)를 참조하십시오.
