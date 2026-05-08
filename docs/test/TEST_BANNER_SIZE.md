# 동적 배너 사이즈 지원 검증 가이드

**패키지**: `react-native-nap-ssp` v0.1.7  
**작성일**: 2026-05-08  
**대상**: QA 엔지니어, 통합 테스트 담당자

---

## 1. 배경 및 변경 내용

### 1.1 무엇이 바뀌었는가

v0.1.7 이전에는 배너 사이즈가 코드에 **하드코딩**된 상수 목록에만 의존했다. 서버(SSP 플랫폼)가 `BANNER_360x230`처럼 목록에 없는 사이즈를 내려줄 경우 뷰가 0×0 또는 기본값(320×50)으로 깨지는 문제가 있었다.

v0.1.7부터는 **`BANNER_WxH` 패턴을 동적으로 파싱**하는 방식으로 전환되었다.

| 레이어 | 변경 위치 | 핵심 로직 |
|---|---|---|
| React Native (JS/TS) | `src/BannerAd.tsx` — `resolveBannerDimensions()` | `size.match(/(\d+)[xX](\d+)/)` 로 너비·높이 추출 후 컨테이너 `width`/`height` 설정 |
| Android (Kotlin) | `NapSspBannerView.kt` — `isSupportedSize()` | `Regex("BANNER_\\d+[xX]\\d+")` 로 동적 허용 |
| iOS (Swift) | `NapSspSupport.swift` — `NapSspBannerSize.parse()` | `"_"` 로 분리 후 `"X"` 기준 파싱, `CGFloat` 변환 |

### 1.2 폴백(Fallback) 규칙

다음 경우에는 자동으로 **320×50** 으로 대체된다.

- `BANNER_0x0` 처럼 너비 또는 높이가 0인 경우 — **JS 레이어에서 그대로 통과되므로 뷰 크기가 0**이 될 수 있다. 플랫폼 네이티브 레이어가 0을 어떻게 처리하는지 별도 확인 필요.
- `"INVALID_SIZE"` 처럼 `WxH` 패턴이 없는 문자열 — JS `resolveBannerDimensions()` 가 기본값(320×50) 반환.
- `size` prop 미지정 — `props.size ?? 'BANNER_320x50'` 기본값 적용.

### 1.3 어댑터별 사이즈 맥락

| 사이즈 | 관련 어댑터 / 용도 |
|---|---|
| `BANNER_360x230` | NaverAdManager / AdManager 어댑터 |
| `BANNER_360x210` | AdFit(Kakao) 어댑터 |
| `BANNER_728x90` | 태블릿/가로 화면, 서버 임의 전송 시나리오 |

---

## 2. 사전 준비

### 2.1 환경 설정

- [ ] `react-native-nap-ssp` 버전이 **0.1.7** 이상인지 확인  
  ```sh
  cat node_modules/react-native-nap-ssp/package.json | grep '"version"'
  ```
- [ ] Android: 에뮬레이터 또는 실기기 연결 확인 (`adb devices`)
- [ ] iOS: Simulator 또는 실기기 연결 확인 (`xcrun simctl list devices | grep Booted`)
- [ ] Metro 번들러 실행 중 (`npx react-native start`)
- [ ] React Native Inspector 활성화 준비 — 디바이스 흔들기 → **Inspector** 메뉴 또는 `cmd+D` / `cmd+M`

### 2.2 테스트 화면 생성

아래 코드를 프로젝트의 임의 화면(예: `TestBannerSizeScreen.tsx`)에 붙여넣고 앱 내비게이션에 연결한다.

```tsx
// TestBannerSizeScreen.tsx
import React, { useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  Text,
  TouchableOpacity,
  View,
  StyleSheet,
} from 'react-native';
import { BannerAd } from 'react-native-nap-ssp';

const AD_UNIT_ID = 'YOUR_TEST_AD_UNIT_ID'; // 실제 테스트 adUnitId 로 교체

const SIZES = [
  'BANNER_320x50',
  'BANNER_320x100',
  'BANNER_300x250',
  'BANNER_360x230',
  'BANNER_360x210',
  'BANNER_728x90',
  'BANNER_999x999',
  'BANNER_0x0',
  'INVALID_SIZE',
  'LARGE_BANNER',
  'MEDIUM_RECTANGLE',
  'SMART_BANNER',
] as const;

export default function TestBannerSizeScreen() {
  const [selectedSize, setSelectedSize] = useState<string>('BANNER_320x50');
  const [log, setLog] = useState<string[]>([]);

  const addLog = (msg: string) =>
    setLog((prev) => [`[${new Date().toISOString().slice(11, 19)}] ${msg}`, ...prev.slice(0, 19)]);

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView>
        {/* 사이즈 선택 버튼 */}
        <View style={styles.grid}>
          {SIZES.map((s) => (
            <TouchableOpacity
              key={s}
              style={[styles.chip, selectedSize === s && styles.chipActive]}
              onPress={() => {
                setSelectedSize(s);
                addLog(`size 변경: ${s}`);
              }}
            >
              <Text style={[styles.chipText, selectedSize === s && styles.chipTextActive]}>
                {s}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* 배너 렌더링 영역 */}
        <Text style={styles.label}>현재 size prop: {selectedSize}</Text>
        <View style={styles.bannerWrapper}>
          <BannerAd
            adUnitId={AD_UNIT_ID}
            size={selectedSize as any}
            onAdLoaded={() => addLog(`onAdLoaded — size=${selectedSize}`)}
            onAdFailedToLoad={(e) => addLog(`onAdFailedToLoad — ${e.code}: ${e.message}`)}
            onAdImpression={() => addLog('onAdImpression')}
          />
        </View>

        {/* 이벤트 로그 */}
        <Text style={styles.label}>이벤트 로그</Text>
        {log.map((line, i) => (
          <Text key={i} style={styles.logLine}>{line}</Text>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#F8FAFC' },
  grid: { flexDirection: 'row', flexWrap: 'wrap', padding: 8, gap: 6 },
  chip: {
    paddingHorizontal: 10, paddingVertical: 6, borderRadius: 6,
    backgroundColor: '#E2E8F0',
  },
  chipActive: { backgroundColor: '#3B82F6' },
  chipText: { fontSize: 11, color: '#334155' },
  chipTextActive: { color: '#FFFFFF', fontWeight: '600' },
  label: { fontSize: 13, fontWeight: '600', color: '#475569', marginLeft: 12, marginTop: 12 },
  bannerWrapper: {
    alignItems: 'center', backgroundColor: '#F1F5F9',
    borderWidth: 1, borderColor: '#CBD5E1', borderRadius: 8,
    margin: 12, padding: 2,
  },
  logLine: { fontSize: 11, color: '#475569', fontFamily: 'monospace', marginHorizontal: 12 },
});
```

### 2.3 단일 사이즈 스니펫 (빠른 확인용)

개별 사이즈를 빠르게 확인할 때는 아래 패턴을 사용한다.

```tsx
import { BannerAd } from 'react-native-nap-ssp';

// 원하는 size 값으로 교체해 테스트
<BannerAd
  adUnitId="YOUR_TEST_AD_UNIT_ID"
  size="BANNER_360x230"
  onAdLoaded={() => console.log('[TEST] 광고 로드 성공')}
  onAdFailedToLoad={(e) => console.warn('[TEST] 로드 실패', e)}
/>
```

### 2.4 치수 확인 방법

**방법 A — React Native Inspector**  
디바이스 개발자 메뉴 → **Show Element Inspector** → 배너 뷰를 탭 → 오른쪽 패널에서 `width` / `height` 확인

**방법 B — 레이아웃 테두리 (권장)**

```tsx
// 배너를 감싸는 뷰에 borderWidth 추가
<View style={{ borderWidth: 2, borderColor: 'red', alignSelf: 'flex-start' }}>
  <BannerAd adUnitId="..." size="BANNER_360x230" />
</View>
```

빨간 테두리가 뷰의 실제 렌더링 크기를 시각적으로 나타낸다.

**방법 C — onLayout 콜백**

```tsx
<BannerAd
  adUnitId="..."
  size="BANNER_360x230"
  style={{
    // onLayout 은 BannerAd 의 외부 래퍼 View 에 붙여야 한다
  }}
/>
// BannerAd 를 View 로 감싸고 onLayout 사용:
<View
  onLayout={(e) => {
    const { width, height } = e.nativeEvent.layout;
    console.log(`실측 크기: ${width}×${height}`);
  }}
>
  <BannerAd adUnitId="..." size="BANNER_360x230" />
</View>
```

**방법 D — Android 로그캣**

```sh
adb logcat -s NapSspBanner:V ReactNativeJS:V
```

**방법 E — iOS 콘솔**

Xcode → **Debug** → **Attach to Process** → Metro 앱 선택 후 콘솔 필터: `NapSsp`

---

## 3. 테스트 매트릭스

각 행에 대해 Android / iOS 각각 체크박스를 완료한다.  
**판정 기준**: 통과(PASS) / 실패(FAIL) / 해당없음(N/A)

---

### TC-01 `BANNER_320x50` — 표준 배너 (기존 동작 유지 확인)

**목적**: 가장 많이 사용되는 표준 사이즈가 기존과 동일하게 동작하는지 회귀 확인

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="BANNER_320x50" />
```

**기대 동작**:
- `resolveBannerDimensions('BANNER_320x50')` → `KNOWN_DIMENSIONS` 에서 직접 반환 (동적 파싱 경로 미사용)
- 컨테이너 뷰: **width=320, height=50** (DP 단위)
- `onAdLoaded` 이벤트 발생
- 뷰가 화면에 가로로 꽉 차거나 320dp 너비로 표시됨

**체크리스트**:
- [ ] (Android) Inspector 또는 onLayout 으로 width=320, height=50 확인
- [ ] (Android) `onAdLoaded` 콜백 수신 확인
- [ ] (Android) 로그캣에서 `NAP_SSP_INVALID_BANNER_SIZE` 오류 없음 확인
- [ ] (iOS) Inspector 또는 onLayout 으로 width=320, height=50 확인
- [ ] (iOS) `onAdLoaded` 콜백 수신 확인
- [ ] (iOS) Xcode 콘솔에서 파싱 오류 없음 확인

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-02 `BANNER_320x100` — 표준 대형 배너

**목적**: `KNOWN_DIMENSIONS` 정적 경로로 처리되는 320×100 사이즈 확인

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="BANNER_320x100" />
```

**기대 동작**:
- 컨테이너 뷰: **width=320, height=100**
- `KNOWN_DIMENSIONS` 에 정의되어 있으므로 동적 파싱 불필요

**체크리스트**:
- [ ] (Android) width=320, height=100 확인
- [ ] (Android) `onAdLoaded` 콜백 수신
- [ ] (iOS) width=320, height=100 확인
- [ ] (iOS) `onAdLoaded` 콜백 수신

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-03 `BANNER_300x250` — 중형 직사각형 (MREC)

**목적**: 가장 많이 쓰이는 인터스티셜형 배너 크기 확인

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="BANNER_300x250" />
```

**기대 동작**:
- 컨테이너 뷰: **width=300, height=250**
- iOS `NapSspBannerSize.parse()` 에서 `"BANNER_300X250"` case 로 처리

**체크리스트**:
- [ ] (Android) width=300, height=250 확인
- [ ] (Android) `onAdLoaded` 콜백 수신
- [ ] (iOS) width=300, height=250 확인
- [ ] (iOS) `onAdLoaded` 콜백 수신

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-04 `BANNER_360x230` — NaverAdManager / AdManager 어댑터 전용

**목적**: `KNOWN_DIMENSIONS` 에 없는 사이즈를 동적 파싱으로 처리하는지 확인 (핵심 신규 기능)

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="BANNER_360x230" />
```

**기대 동작**:
- JS: `KNOWN_DIMENSIONS` 에 없으므로 `size.match(/(\d+)[xX](\d+)/)` 실행 → `{width:360, height:230}`
- Android: `isSupportedSize()` 에서 `Regex("BANNER_\\d+[xX]\\d+")` 매칭 성공 → 광고 요청 진행
- iOS: `parse()` 에서 `"_"` 분리 후 `"360"`, `"230"` 파싱 → `NapSspBannerSize(width:360, height:230)`
- 컨테이너 뷰: **width=360, height=230**

**체크리스트**:
- [ ] (Android) width=360, height=230 확인
- [ ] (Android) `onAdLoaded` 콜백 수신 (또는 SDK 미연결 시 placeholder 로드)
- [ ] (Android) `NAP_SSP_INVALID_BANNER_SIZE` 오류 **미발생** 확인
- [ ] (iOS) width=360, height=230 확인
- [ ] (iOS) `onAdLoaded` 콜백 수신
- [ ] (iOS) Swift 파싱 경로가 `default:` 분기를 탄다는 것을 로그로 확인 (선택)

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-05 `BANNER_360x210` — AdFit(Kakao) 어댑터 전용

**목적**: TC-04 와 유사하나 높이가 다른 동적 사이즈 확인

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="BANNER_360x210" />
```

**기대 동작**:
- 컨테이너 뷰: **width=360, height=210**
- Android/iOS 모두 동적 파싱 경로 사용

**체크리스트**:
- [ ] (Android) width=360, height=210 확인
- [ ] (Android) `onAdLoaded` 콜백 수신
- [ ] (Android) `NAP_SSP_INVALID_BANNER_SIZE` 오류 미발생
- [ ] (iOS) width=360, height=210 확인
- [ ] (iOS) `onAdLoaded` 콜백 수신

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-06 `BANNER_728x90` — 임의 서버 전송 사이즈 (태블릿/리더보드)

**목적**: 서버가 코드에 없는 사이즈를 내려줬을 때 코드 수정 없이 동작하는지 확인

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="BANNER_728x90" />
```

**기대 동작**:
- 컨테이너 뷰: **width=728, height=90**
- 화면 너비를 초과할 경우 스크롤 또는 잘림이 발생할 수 있으나, 뷰 자체는 정확한 크기로 생성

**체크리스트**:
- [ ] (Android) width=728, height=90 확인
- [ ] (Android) `onAdLoaded` 콜백 수신 또는 placeholder 표시 확인
- [ ] (Android) 오류 없이 뷰 생성됨 확인
- [ ] (iOS) width=728, height=90 확인
- [ ] (iOS) `onAdLoaded` 콜백 수신 또는 placeholder 표시 확인

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-07 `BANNER_999x999` — 엣지 케이스: 매우 큰 사이즈

**목적**: 극단적으로 큰 사이즈가 크래시 없이 처리되는지 확인

**샘플 코드**:
```tsx
<View onLayout={(e) => console.log('실측:', e.nativeEvent.layout)}>
  <BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="BANNER_999x999" />
</View>
```

**기대 동작**:
- JS 레이어: 동적 파싱 성공 → `{width:999, height:999}`
- 컨테이너 뷰 크기 요청: **width=999, height=999**
- 화면 밖으로 나가거나 잘릴 수 있으나 크래시 없음
- Android: `isSupportedSize()` 통과 → 광고 요청 또는 placeholder
- iOS: `parse()` 통과 → `NapSspBannerSize(width:999, height:999)`

**체크리스트**:
- [ ] (Android) 앱 크래시 없음 확인
- [ ] (Android) width=999, height=999 요청됨 확인 (실제 화면 렌더링은 잘릴 수 있음)
- [ ] (Android) `onAdLoaded` 또는 `onAdFailedToLoad` 중 하나 수신 (어느 쪽이든 크래시는 없어야 함)
- [ ] (iOS) 앱 크래시 없음 확인
- [ ] (iOS) width=999, height=999 요청됨 확인
- [ ] (iOS) `onAdLoaded` 또는 `onAdFailedToLoad` 중 하나 수신

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-08 `BANNER_0x0` — 엣지 케이스: 0 크기

**목적**: 너비 또는 높이가 0인 경우 동작 확인 (폴백 적용 여부)

**샘플 코드**:
```tsx
<View onLayout={(e) => console.log('실측:', e.nativeEvent.layout)}>
  <BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="BANNER_0x0" />
</View>
```

**기대 동작 (구현 분석 기반)**:
- JS `resolveBannerDimensions('BANNER_0x0')`: `match(/(\d+)[xX](\d+)/)` 에서 `"0"`, `"0"` 추출 → `{width:0, height:0}` 반환 (폴백 없이 그대로 사용)
- 컨테이너 뷰 크기: **width=0, height=0** — 뷰가 화면에 표시되지 않을 수 있음
- Android: `isSupportedSize("BANNER_0x0")` → regex 매칭 성공 → 광고 요청 가능하나 0dp 뷰
- iOS: `parse()` 에서 `CGFloat("0")` 성공 → `NapSspBannerSize(width:0, height:0)`

> **주의**: 현재 구현에서 0×0 은 명시적 폴백 없이 그대로 통과된다. 이 동작이 의도적인지 확인 필요. 의도치 않다면 별도 이슈로 등록할 것.

**체크리스트**:
- [ ] (Android) 앱 크래시 없음 확인
- [ ] (Android) 뷰 크기가 0×0 또는 폴백 320×50 으로 표시되는지 기록
- [ ] (Android) 로그캣에서 오류 메시지 또는 경고 수집
- [ ] (iOS) 앱 크래시 없음 확인
- [ ] (iOS) 뷰 크기가 0×0 또는 폴백 320×50 으로 표시되는지 기록
- [ ] 실제 동작이 기대와 다를 경우 이슈 번호 기록: ___________

**판정**: Android [ PASS / FAIL / 추가조사 ] &nbsp; iOS [ PASS / FAIL / 추가조사 ]

---

### TC-09 `"INVALID_SIZE"` — WxH 패턴 없는 문자열

**목적**: 패턴이 없는 임의 문자열 전달 시 폴백(320×50)이 적용되는지 확인

**샘플 코드**:
```tsx
<BannerAd
  adUnitId="YOUR_TEST_AD_UNIT_ID"
  size={"INVALID_SIZE" as any}
  onAdFailedToLoad={(e) => console.warn('실패:', e.code, e.message)}
/>
```

**기대 동작**:
- JS `resolveBannerDimensions('INVALID_SIZE')`:
  - `KNOWN_DIMENSIONS['INVALID_SIZE']` → `undefined`
  - `'INVALID_SIZE'.match(/(\d+)[xX](\d+)/)` → `null`
  - 폴백 반환: `{width:320, height:50}`
- 컨테이너 뷰: **width=320, height=50** (폴백)
- Android: `isSupportedSize("INVALID_SIZE")` → regex 실패 → `NAP_SSP_INVALID_BANNER_SIZE` 오류 발생 → `onAdFailedToLoad` 호출
- iOS: `parse()` 에서 `"_"` 분리 후 `"INVALID"`, `"SIZE"` → `CGFloat("INVALID")` 실패 → `.banner` 폴백 반환

**체크리스트**:
- [ ] (Android) JS 컨테이너 뷰 크기 = 320×50 확인
- [ ] (Android) `onAdFailedToLoad` 수신 확인
- [ ] (Android) 오류 코드 `NAP_SSP_INVALID_BANNER_SIZE` 확인
- [ ] (Android) 로그캣 메시지: `"Unsupported banner size: INVALID_SIZE"` 확인
- [ ] (iOS) JS 컨테이너 뷰 크기 = 320×50 확인
- [ ] (iOS) `onAdLoaded` 또는 `onAdFailedToLoad` 중 하나 수신 확인
- [ ] (iOS) 크래시 없음 확인

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-10 `LARGE_BANNER` — 명칭 별칭 (Named Alias)

**목적**: 문자열 별칭이 기존과 동일하게 동작하는지 회귀 확인

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="LARGE_BANNER" />
```

**기대 동작**:
- JS `KNOWN_DIMENSIONS['LARGE_BANNER']` = `{width:320, height:100}` 직접 반환
- Android: `isSupportedSize("LARGE_BANNER")` → `when` 분기에서 `true` 반환
- iOS: 직접 파싱 case 없으나 `parse()` default 분기에서 `"_"` 로 분리 시 `["LARGE", "BANNER"]` → `dims` 파싱 실패 → `.banner` 폴백 (320×50)

> **주의**: iOS 에서 `LARGE_BANNER` 는 `NapSspBannerSize.parse()` 의 switch 에 명시적 case 가 없으므로 폴백(320×50)이 적용될 수 있다. JS 레이어에서는 이미 `width=320, height=100` 으로 컨테이너 크기를 설정하므로 뷰 크기는 정상이다.

**체크리스트**:
- [ ] (Android) JS 컨테이너 뷰: width=320, height=100 확인
- [ ] (Android) `onAdLoaded` 수신
- [ ] (Android) 로그캣 오류 없음
- [ ] (iOS) JS 컨테이너 뷰: width=320, height=100 확인 (JS 레이어에서 크기 결정)
- [ ] (iOS) `onAdLoaded` 수신
- [ ] (iOS) 네이티브 레이어의 실제 처리 크기 기록 (320×50 vs 320×100)

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-11 `MEDIUM_RECTANGLE` — 명칭 별칭

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="MEDIUM_RECTANGLE" />
```

**기대 동작**:
- JS `KNOWN_DIMENSIONS['MEDIUM_RECTANGLE']` = `{width:300, height:250}`
- iOS `parse()`: `"MEDIUM_RECTANGLE"` case 에서 `.mediumRectangle` 반환 (300×250) — 명시적 처리

**체크리스트**:
- [ ] (Android) width=300, height=250 확인
- [ ] (Android) `onAdLoaded` 수신
- [ ] (iOS) width=300, height=250 확인
- [ ] (iOS) `onAdLoaded` 수신

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

### TC-12 `SMART_BANNER` — 명칭 별칭

**샘플 코드**:
```tsx
<BannerAd adUnitId="YOUR_TEST_AD_UNIT_ID" size="SMART_BANNER" />
```

**기대 동작**:
- JS `KNOWN_DIMENSIONS['SMART_BANNER']` = `{width:320, height:50}`
- Android: `isSupportedSize("SMART_BANNER")` → `when` 분기 `true`
- iOS: `"SMART_BANNER"` case 에서 `.smartBanner` 반환 (320×50)

**체크리스트**:
- [ ] (Android) width=320, height=50 확인
- [ ] (Android) `onAdLoaded` 수신
- [ ] (iOS) width=320, height=50 확인
- [ ] (iOS) `onAdLoaded` 수신

**판정**: Android [ PASS / FAIL ] &nbsp; iOS [ PASS / FAIL ]

---

## 4. 서버 전송 사이즈 시뮬레이션

실제 서버가 배너 사이즈를 동적으로 내려주는 시나리오를 로컬에서 시뮬레이션하는 방법이다.

### 4.1 상태(state)로 사이즈 동적 변경

```tsx
import React, { useEffect, useState } from 'react';
import { View } from 'react-native';
import { BannerAd } from 'react-native-nap-ssp';

// 서버 응답을 흉내낸 함수 (실제 환경에서는 fetch/API 호출로 대체)
async function fetchAdConfig(): Promise<{ size: string }> {
  // 2초 지연 후 서버가 BANNER_360x230 을 내려준다고 가정
  await new Promise((r) => setTimeout(r, 2000));
  return { size: 'BANNER_360x230' };
}

export default function ServerDrivenBannerTest() {
  const [bannerSize, setBannerSize] = useState<string | null>(null);

  useEffect(() => {
    fetchAdConfig().then(({ size }) => {
      console.log('[TEST] 서버에서 받은 사이즈:', size);
      setBannerSize(size);
    });
  }, []);

  if (!bannerSize) return null; // 서버 응답 전 렌더링 생략

  return (
    <View>
      <BannerAd
        adUnitId="YOUR_TEST_AD_UNIT_ID"
        size={bannerSize}
        onAdLoaded={() => console.log('[TEST] 서버 지정 사이즈 로드 성공:', bannerSize)}
        onAdFailedToLoad={(e) => console.warn('[TEST] 실패:', e)}
      />
    </View>
  );
}
```

**확인 포인트**:
- [ ] `bannerSize` state 가 `null` → 서버 응답값으로 변경될 때 배너가 올바른 크기로 렌더링되는지 확인
- [ ] 사이즈 변경 후 뷰가 리마운트(re-mount)되는지, 아니면 같은 인스턴스가 유지되는지 확인
- [ ] Android 에서 `size` prop 변경 시 `isSupportedSize()` 재평가 및 `maybeAutoLoad()` 재호출 여부 확인

### 4.2 여러 사이즈를 순서대로 변경 (스트레스 테스트)

```tsx
const SERVER_SEQUENCE = [
  'BANNER_320x50',
  'BANNER_360x230',
  'BANNER_300x250',
  'BANNER_728x90',
];

export function CycleBannerSizeTest() {
  const [idx, setIdx] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setIdx((prev) => (prev + 1) % SERVER_SEQUENCE.length);
    }, 3000); // 3초마다 사이즈 변경
    return () => clearInterval(timer);
  }, []);

  return (
    <BannerAd
      adUnitId="YOUR_TEST_AD_UNIT_ID"
      size={SERVER_SEQUENCE[idx] as any}
      onAdLoaded={() => console.log('[CYCLE] 로드:', SERVER_SEQUENCE[idx])}
    />
  );
}
```

**확인 포인트**:
- [ ] 각 사이즈 전환 시 크래시 없음
- [ ] 이전 사이즈 뷰가 정상적으로 정리(cleanup)되는지 확인
- [ ] 메모리 누수 없음 (Android Profiler 또는 Instruments 로 확인)

---

## 5. 통과/실패 기준 요약

| # | 테스트 케이스 | 통과 조건 | 실패 조건 |
|---|---|---|---|
| TC-01 | BANNER_320x50 | 뷰 320×50, onAdLoaded 수신 | 크기 불일치, 크래시, 이벤트 미수신 |
| TC-02 | BANNER_320x100 | 뷰 320×100, onAdLoaded 수신 | 크기 불일치, 크래시 |
| TC-03 | BANNER_300x250 | 뷰 300×250, onAdLoaded 수신 | 크기 불일치, 크래시 |
| TC-04 | BANNER_360x230 | 뷰 360×230, 오류 없음 | `NAP_SSP_INVALID_BANNER_SIZE` 발생, 크기 불일치 |
| TC-05 | BANNER_360x210 | 뷰 360×210, 오류 없음 | `NAP_SSP_INVALID_BANNER_SIZE` 발생, 크기 불일치 |
| TC-06 | BANNER_728x90 | 뷰 728×90, 크래시 없음 | 크래시, 크기 불일치 |
| TC-07 | BANNER_999x999 | 크래시 없음, 뷰 생성됨 | 크래시, 무한루프 |
| TC-08 | BANNER_0x0 | 크래시 없음, 동작 기록 | 크래시, 예외 처리 안됨 |
| TC-09 | INVALID_SIZE | JS 폴백 320×50, Android onAdFailedToLoad 수신 | 크래시, 폴백 미적용 |
| TC-10 | LARGE_BANNER | JS 뷰 320×100, onAdLoaded 수신 | 크기 불일치, 이벤트 미수신 |
| TC-11 | MEDIUM_RECTANGLE | 뷰 300×250, onAdLoaded 수신 | 크기 불일치, 크래시 |
| TC-12 | SMART_BANNER | 뷰 320×50, onAdLoaded 수신 | 크기 불일치, 크래시 |

---

## 6. 알려진 제한 사항 및 주의 사항

1. **`BANNER_0x0` 폴백 없음**: 현재 JS 레이어(`resolveBannerDimensions`)는 `0×0` 을 유효한 치수로 처리하므로 뷰가 0dp 크기로 렌더링될 수 있다. 이 동작이 문제라면 `resolveBannerDimensions` 에서 `width === 0 || height === 0` 조건을 추가 검토해야 한다.

2. **iOS `LARGE_BANNER` / `SMART_BANNER` 네이티브 레이어**: `NapSspBannerSize.parse()` 의 `switch` 에 명시적 case 가 없어 `default:` 분기로 진입, 파싱 실패 후 `.banner`(320×50)를 반환한다. JS 레이어에서 이미 올바른 크기를 설정하므로 컨테이너 뷰에는 영향 없지만, 네이티브 SDK 에 전달되는 크기가 다를 수 있다.

3. **`size` prop 변경 시 Android 재로드**: `NapSspBannerView.kt` 에서 `size` setter 는 `maybeAutoLoad()` 를 호출하지만, `currentState == LOADED` 이면 재로드를 건너뛴다. `reload()` 를 명시적으로 호출해야 할 수 있다.

4. **플레이스홀더 모드**: `BuildConfig.NAP_SSP_VENDOR_SDK_ENABLED` 가 `false` 이면 실제 SDK 대신 placeholder 가 사용된다. CI/개발 빌드에서는 이 모드로 동작하므로 네이티브 광고 렌더링 테스트는 **프로덕션 빌드 또는 SDK 연결 환경**에서 별도 수행해야 한다.

---

## 7. 결과 기록 양식

테스트 완료 후 아래 표를 채워 PR 리뷰 또는 이슈에 첨부한다.

```
테스트 일자:
테스터:
디바이스 (Android): 기종 / OS 버전
디바이스 (iOS):    기종 / OS 버전
빌드 구성:         Debug / Release
SDK 연결 여부:     Yes / No (placeholder 모드)

| TC   | Android | iOS    | 비고 |
|------|---------|--------|------|
| TC-01 |        |        |      |
| TC-02 |        |        |      |
| TC-03 |        |        |      |
| TC-04 |        |        |      |
| TC-05 |        |        |      |
| TC-06 |        |        |      |
| TC-07 |        |        |      |
| TC-08 |        |        |      |
| TC-09 |        |        |      |
| TC-10 |        |        |      |
| TC-11 |        |        |      |
| TC-12 |        |        |      |
```
