# Work Status / Next Steps — 2026-04-30

## Current snapshot

This repository has already been updated and pushed through the earlier stabilization/docs commits up to `1dd3a4f`.

After that point, work continued mainly in two tracks:

1. **Make Maestro/ad-result interpretation easier to read in the integration test app**
2. **Classify each ad format as `SHOWN`, `NO_ADS`, or `ERROR` on Android/iOS**

---

## Completed in this phase

### 1) Integration test app result classification UI added
`integration-test-app/App.tsx` was updated so the app now exposes deterministic result lines for each format:

- `BANNER_RESULT:*`
- `NATIVE_RESULT:*`
- `VIDEO_RESULT:*`
- `INTER_RESULT:*`
- `REWARD_RESULT:*`
- `IV_RESULT:*`

Classification rules currently used:
- `SHOWN`
- `NO_ADS`
- `ERROR`
- `REQUESTING`
- `IDLE`
- `LOADED_NOT_SHOWN`

This makes follow-up Maestro/manual verification much easier than relying only on raw status strings.

---

### 2) Android/iOS ad-format classification was rechecked
#### Android
Confirmed/observed:
- **Banner**: `SHOWN`
- **Native**: `SHOWN`
- **Inline video**: `SHOWN`
- **Interstitial**: `SHOWN`
- **Rewarded**: `ERROR`
  - explicit message included `Unable to instantiate mediation adapter class.`
- **Interstitial video (IV)**: now treated as **`ERROR`**, not `NO_ADS`
  - focused follow-up showed Android-side runtime failure / crash behavior rather than a no-fill message
  - dependency/runtime evidence indicates a mediation compatibility problem rather than inventory shortage

#### iOS
Confirmed/observed:
- **Banner**: `SHOWN`
- **Native**: `SHOWN`
- **Inline video**: `ERROR`
  - visible result was `failed:unknown`
- **Interstitial**: `SHOWN`
- **Rewarded**: `NO_ADS`
  - explicit user-facing no-fill message: `현재 노출가능한 광고가 없습니다. 잠시 후 다시 광고 요청을 시도해 주세요.`
- **Interstitial video (IV)**: `ERROR`
  - explicit message: `Invalid Ad Unit or required info missing`

---

## Important technical finding from the latest Android IV investigation

The remaining Android IV ambiguity was narrowed down further and is now best understood as an **SDK/adapter compatibility issue**, not a `no ads` case.

Observed evidence from the latest focused debugging:
- Android IV flow could trigger app failure / app exit behavior instead of a clean no-fill result
- dependency graph inspection showed:
  - app/plugin currently resolve **`admixer-ssp:1.0.23`**
  - `admixer-adfit:1.0.12_beta` still declares dependency on **`admixer-ssp:1.0.21_alpha`**
- this strongly suggests a mediation/runtime mismatch in the Android stack

Practical meaning:
- current “latest version bump” is **not yet a clean commercial-ready Android mediation state** for every fullscreen path
- Android mediation version alignment still needs one more pass before calling the newest Android set fully hardened

---

## What still needs to be done

### Highest priority
1. **Resolve Android mediation compatibility mismatch**
   - verify which Android mediation artifacts are actually compatible with `admixer-ssp:1.0.23`
   - likely options:
     - downgrade core to the mediation-compatible line, or
     - upgrade mediation artifacts to a version published against `1.0.23`

2. **Re-run Android fullscreen verification after dependency alignment**
   - especially:
     - rewarded
     - interstitial video (IV)

3. **Refresh readiness guidance after Android compatibility fix**
   - update `docs/MAESTRO_READINESS_2026-04-30.md` once Android fullscreen mediation is revalidated

### Secondary cleanup
4. Decide whether to keep or refine the temporary Maestro helper YAML files created during focused debugging
5. Remove or archive scratch screenshots that were generated only for local diagnosis
6. Continue excluding `integration-test-app/maestro/maestro-soak-history.md` from intentional commits because it is generated history noise

---

## Recommended next working sequence

1. Lock or adjust Android mediation/core versions to a known-compatible set
2. Build/install Android integration test app again
3. Run Android validate + focused fullscreen checks
4. Confirm whether rewarded and IV move from `ERROR` to `SHOWN` or `NO_ADS`
5. Update readiness/status docs with the new final verdict

---

## Files intentionally relevant to this phase

### Changed source
- `integration-test-app/App.tsx`

### New documentation
- `docs/WORK_STATUS_2026-04-30.md`

### Local scratch/debug artifacts created during investigation
These were useful for diagnosis but are not intended as clean source-of-truth deliverables unless explicitly curated later:
- temporary Maestro YAML helpers under `integration-test-app/maestro/`
- local screenshots used to inspect failure states

---

## Bottom line

As of this document:
- **iOS classification is clear enough to distinguish `SHOWN` / `NO_ADS` / `ERROR` per format**
- **Android is mostly stable for banner/native/inline/interstitial**
- **Android fullscreen mediation still needs dependency alignment work before final sign-off on the latest SDK combination**
