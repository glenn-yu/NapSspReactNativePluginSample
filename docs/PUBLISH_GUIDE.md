# NPM 플러그인 배포 가이드 (Publishing Guide)

이 문서는 `react-native-nap-ssp` 플러그인을 NPM 레지스트리에 배포하기 위한 절차와 체크리스트를 정의합니다.

---

## 1. 배포 전 체크리스트 (Pre-publish Checklist)

배포 명령을 실행하기 전에 아래 항목을 반드시 확인하십시오.

### 1.1 빌드 및 검증
- [ ] **Type Check**: `npm run typecheck` 실행 후 에러가 없는지 확인.
- [ ] **Build**: `npm run build`를 통해 `lib/` 디렉토리가 정상적으로 생성되는지 확인.
- [ ] **Smoke Test**: `npm run smoke:test`를 통해 공개 API의 기본 동작 확인.
- [ ] **Integration Test**: `integration-test-app`에서 Android/iOS 실기기 빌드 및 광고 노출 최종 확인.

### 1.2 버전 및 문서
- [ ] **Version**: `package.json`의 `version` 필드가 Semantic Versioning(vX.Y.Z)에 맞게 업데이트되었는가?
- [ ] **Changelog**: `CHANGELOG.md`에 이번 버전의 변경 사항이 기록되었는가?
- [ ] **Native Files**: `NapSspPlugin.podspec`의 버전이 `package.json`과 일치하는가?

---

## 2. 배포 절차 (Step-by-Step)

### Step 1. 로컬 클린업
기존 빌드 결과물과 `node_modules`를 정리하고 새로 설치합니다.
```bash
rm -rf node_modules lib
npm install
npm run build
```

### Step 2. NPM 로그인
배포 권한이 있는 계정으로 로그인합니다.
```bash
npm login
```

### Step 3. 배포 실행
`package.json`의 `files` 필드에 정의된 파일들(`lib/`, `android/`, `ios/`, `src/` 등)이 포함됩니다.
```bash
# 드라이 런 (실제 배포 전에 포함될 파일 목록 확인)
npm publish --dry-run

# 실제 배포
npm publish --access public
```

---

## 3. 사후 작업 (Post-publish)

### 3.1 Git 태그 생성
배포된 버전과 코드 시점을 일치시키기 위해 Git 태그를 생성하고 푸시합니다.
```bash
git tag v0.1.8
git push origin v0.1.8
```

### 3.2 샘플 앱 확인
새 버전 배포 후 `example/` 또는 다른 프로젝트에서 새 버전을 설치하여 정상 동작하는지 최종 확인합니다.
```bash
npm install react-native-nap-ssp@latest
```

---

## 4. 주의 사항

- **Secrets**: `.env` 파일이나 API Key 등 민감한 정보가 배포 파일에 포함되지 않도록 `.npmignore`를 확인하십시오.
- **Peer Dependencies**: `react` 및 `react-native` 버전 호환성을 `package.json`의 `peerDependencies`에 명시하십시오.
- **Native Linking**: 자동 링크(Autolinking)가 정상 동작하도록 `react-native.config.js` 설정이 올바른지 확인하십시오.
