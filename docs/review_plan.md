nap ssp React Native 플러그인 전수 조사 및 검증 계획서

본 문서는 개발된 react-native-nap-ssp 플러그인과 테스트 앱이 초기 설계(Plan v1.0) 및 최신 네이티브 SDK 가이드(Android v1.0.21 / iOS v2.2.1)를 모두 준수하고 있는지 전수 조사하기 위한 체크리스트이자 가이드라인입니다.

1. 검증 개요

검증 대상: react-native-nap-ssp 패키지 소스, example 앱, 네이티브 브릿지 코드

기준 문서:

nap_ssp_react_native_plan.docx (설계서)

nap-ssp-android-sdk-native.md (Android 최신 가이드)

nap-ssp-ios-sdk-native.md (iOS 최신 가이드)

2. 전수 조사 핵심 체크리스트

2.1 SDK 초기화 및 공통 설정 (Core)

[ ] Initialization: mediaKey와 adUnitIds 목록이 네이티브 SDK로 정상 전달되는가?

[ ] Log Level: setDebugEnabled (iOS) 및 AdMixerLog.setLogLevel (Android)이 JS 환경 설정에 따라 연동되는가?

[ ] Mediation Adapters: registerAdapter (Android) 로직이 초기화 시점에 포함되어 있는가? (Google, AdFit, Pangle, AppLovin, Unity)

[ ] Pangle/AppLovin Key: 초기화 시 appId 및 sdkKey가 각 네트워크 SDK 설정에 반영되는가?

2.2 광고 포맷별 구현 (Ad Formats)

배너 광고 (BannerAd)

[ ] Lifecycle: onResume, onPause, onDestroy (Android) 및 stop (iOS) 호출이 RN 생명주기에 매핑되었는가?

[ ] Events: onSuccess, onFail, onTap 이벤트가 JS 콜백으로 전달되는가?

[ ] Size Mapping: 설계서에 정의된 320x50, 300x250 등의 사이즈 상수가 정의되었는가?

전면 광고 (InterstitialAd)

[ ] Config: basic, popup, countDown 3종 옵션(특히 countDownTime, popupOption)이 네이티브로 전달되는가?

[ ] Method: load()와 show()가 비동기(Promise)로 분리되어 있는가?

[ ] Resource Recovery: 광고 닫힘 시 stop() 또는 nil 처리가 수행되는가?

네이티브 광고 (NativeAd)

[ ] Asset Mapping: icon, headline, advertiser, description, media, cta 6종 에셋이 Native View Manager를 통해 렌더링되는가?

[ ] v2.2.1 대응 (중요): iOS loadAD() 시 기존 뷰를 제거하는 removeView 로직이 반영되었는가?

[ ] Layout: Android에서 RelativeLayout 기반의 컨테이너 구조를 유지하는가?

리워드 동영상 (RewardedAd)

[ ] Reward Callback: Android의 EARNEDREWARD와 iOS의 onRewardVideoEarned가 동일한 JS 이벤트(onRewarded)로 통합되었는가?

[ ] Custom Params: customParam (iOS) 및 setCustomParams (Android) 딕셔너리 전달 기능이 포함되었는가?

2.3 네이티브 플랫폼 특이사항 (Platform Specifics)

[ ] Android Gradle: build.gradle에 필수 의존성 및 미디에이션 repositories (Kakao, Pangle)가 포함되었는가?

[ ] Android Proguard: consumer-rules.pro에 공식 가이드의 -keep 규칙 6종이 포함되었는가?

[ ] iOS Info.plist: GADApplicationIdentifier 및 SKAdNetwork ID 가이드라인이 README에 명시되었는가?

[ ] ATT Handling: iOS 13.0+ 대응을 위한 requestTrackingAuthorization 유틸리티가 제공되는가?

2.4 아키텍처 및 New Architecture (Fabric/JSI)

[ ] TurboModules: NapSspModule이 New Architecture 환경에서 JSI를 통해 동작하는가?

[ ] Fabric: 배너 및 네이티브 뷰 컴포넌트가 Fabric Renderer를 지원하도록 구현되었는가?

[ ] Compatibility: RN 0.72~0.74+ 버전 범위에서 하위 호환성을 유지하는가?

3. 공식 가이드 대비 누락 사항 분석 (Gap Analysis)

항목

공식 가이드 내용 (Android/iOS)

RN 플러그인 구현 여부

비고

에러 코드

iOS 7종 (0~6) 에러 매핑

[ ]

JS 에러 객체에 에러코드 포함 필요

비즈보드

Kakao AdFit 비즈보드 단독 지면 정책

[ ]

미디에이션 불가 설정 확인

음소거

리워드 광고 setMute(true) 옵션

[ ]



타임아웃

전면 광고 interstitialTimeout (Android)

[ ]



클릭 영역

iOS 전면 광고 closeButtonTouchAreaRatio

[ ]

0.2~1.0 범위 설정

4. 테스트 앱 (Example App) 검증

[ ] Integration Test: 모든 광고 포맷(배너, 전면, 네이티브, 리워드)이 단일 앱에서 테스트 가능한가?

[ ] Mediation Test: 실제 AdUnit ID를 입력하여 각 네트워크별 광고 호출을 시뮬레이션할 수 있는가?

[ ] Memory Leak: 광고 반복 로드 및 화면 전환 시 메모리 증가 폭이 정상 범위인가? (Xcode/Android Studio Profiler)

5. 향후 조치 계획

AI 전수 조사: Gemini CLI/Claude Code를 통해 위 체크리스트 기반 정적 분석 수행.

누락 보완: 분석 결과 발견된 미구현 옵션(예: 비즈보드, 상세 Config 등) 추가 개발.

QA 리포트: 실제 기기 테스트 결과를 포함한 최종 QA 결과서 작성.