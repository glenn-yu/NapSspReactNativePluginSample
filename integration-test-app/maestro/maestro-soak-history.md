# Maestro Soak Test History

This file accumulates Maestro soak-test results across runs.

## 2026-04-25 Android soak run
- Started: 2026-04-25 18:28:35 (Asia/Seoul)
- Status at log capture: in progress
- Completed successful iterations so far: 17
- Failed iterations so far: 0
- Last recorded successful iteration: 17 at 2026-04-25 18:52:06
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `integration-test-app/maestro/results/soak-30m-20260425-182835`
- Artifacts:
  - `integration-test-app/maestro/results/soak-30m-20260425-182835/summary.txt`
  - `integration-test-app/maestro/results/soak-30m-20260425-182835/run-*.log`
  - `integration-test-app/maestro/results/soak-30m-20260425-182835/adb-status.txt`
- Notes:
  - Android Maestro flow remained stable through at least 17 consecutive passes.
  - Metro-backed React Native debug runtime stayed healthy during the observed soak window.
  - No fail artifacts were created up to this capture point.

## 2026-04-25 21:27:21 Android soak run
- Started: 2026-04-25 21:27:21 (Asia/Seoul)
- Ended: 2026-04-25 21:28:20 (Asia/Seoul)
- Total iterations: 3
- Pass: 0
- Fail: 3
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-soak-60m-20260425-212721`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-soak-60m-20260425-212721/summary.txt`
- Stop reason: `repeat_failure_threshold:maestro_android_transport`
- Failure artifacts present: yes

## 2026-04-25 22:31:47 Android soak run
- Started: 2026-04-25 22:31:47 (Asia/Seoul)
- Ended: 2026-04-25 22:32:51 (Asia/Seoul)
- Total iterations: 3
- Pass: 0
- Fail: 3
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-soak-60m-20260425-223147`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-soak-60m-20260425-223147/summary.txt`
- Stop reason: `repeat_failure_threshold:maestro_android_transport`
- Failure artifacts present: yes

## 2026-04-26 21:53:18 iOS soak run
- Started: 2026-04-26 21:53:18
- Ended: 2026-04-26 22:28:17
- Total iterations: 68
- Pass: 0
- Fail: 68
- Flow: `integration-test-app/maestro/ios-ad-validation.yaml`

## 2026-04-28 15:23:03 Android adb soak run
- Started: 2026-04-28 15:23:03 (Asia/Seoul)
- Ended: 2026-04-28 15:24:20 (Asia/Seoul)
- Total iterations: 4
- Pass: 1
- Fail: 3
- Mode: `adb + uiautomator dump + logcat`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-152303`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-152303/summary.txt`
- Stop reason: `repeat_failure_threshold:android_missing_init_button`

## 2026-04-28 15:41:42 Android adb soak run
- Started: 2026-04-28 15:41:42 (Asia/Seoul)
- Ended: 2026-04-28 15:46:16 (Asia/Seoul)
- Total iterations: 3
- Pass: 0
- Fail: 3
- Mode: `adb + uiautomator dump + logcat`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-154142`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-154142/summary.txt`
- Stop reason: `repeat_failure_threshold:android_init_not_success`

## 2026-04-28 17:19:08 Android adb soak run
- Started: 2026-04-28 17:19:08 (Asia/Seoul)
- Ended: 2026-04-28 17:21:30 (Asia/Seoul)
- Total iterations: 4
- Pass: 0
- Fail: 4
- Mode: `adb + uiautomator dump + logcat`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-171907`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-171907/summary.txt`
- Stop reason: `repeat_failure_threshold:android_missing_init_button`

## 2026-04-28 17:23:27 Android adb soak run
- Started: 2026-04-28 17:23:27 (Asia/Seoul)
- Ended: 2026-04-28 17:24:17 (Asia/Seoul)
- Total iterations: 3
- Pass: 0
- Fail: 3
- Mode: `adb + uiautomator dump + logcat`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-172327`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-172327/summary.txt`
- Stop reason: `repeat_failure_threshold:android_missing_init_button`

## 2026-04-28 17:24:39 Android adb soak run
- Started: 2026-04-28 17:24:39 (Asia/Seoul)
- Ended: 2026-04-28 17:27:36 (Asia/Seoul)
- Total iterations: 5
- Pass: 2
- Fail: 3
- Mode: `adb + uiautomator dump + logcat`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-172439`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/android-adb-soak-60m-20260428-172439/summary.txt`
- Stop reason: `repeat_failure_threshold:android_init_not_success`

## 2026-04-26 21:53:18 Android soak run
- Started: 2026-04-26 21:53:18 (Asia/Seoul)
- Ended: 2026-04-28 20:23:23 (Asia/Seoul)
- Total iterations: 1
- Pass: 0
- Fail: 1
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260426-215318`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260426-215318/summary.txt`
- Failure artifacts present: yes

## 2026-04-29 09:20:36 Android soak run
- Started: 2026-04-29 09:20:36 (Asia/Seoul)
- Ended: 2026-04-29 09:50:37 (Asia/Seoul)
- Total iterations: 29
- Pass: 0
- Fail: 29
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-092036`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-092036/summary.txt`
- Failure artifacts present: yes

## 2026-04-29 15:31:51 iOS soak run
- Started: 2026-04-29 15:31:51
- Ended: 2026-04-29 16:02:30
- Total iterations: 30
- Pass: 30
- Fail: 0
- Flow: `integration-test-app/maestro/ios-ad-validation.yaml`

## 2026-04-29 18:37:35 Android soak run
- Started: 2026-04-29 18:37:35 (Asia/Seoul)
- Ended: 2026-04-29 18:40:25 (Asia/Seoul)
- Total iterations: 1
- Pass: 1
- Fail: 0
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-183734`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-183734/summary.txt`
- Failure artifacts present: no

## 2026-04-29 18:46:34 iOS soak run
- Started: 2026-04-29 18:46:34
- Ended: 2026-04-29 19:16:34
- Total iterations: 29
- Pass: 29
- Fail: 0
- Flow: `integration-test-app/maestro/ios-ad-validation.yaml`

## 2026-04-29 18:46:33 Android soak run
- Started: 2026-04-29 18:46:33 (Asia/Seoul)
- Ended: 2026-04-29 19:17:36 (Asia/Seoul)
- Total iterations: 12
- Pass: 9
- Fail: 3
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-184633`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-184633/summary.txt`
- Failure artifacts present: yes

## 2026-04-29 21:04:32 Android soak run
- Started: 2026-04-29 21:04:32 (Asia/Seoul)
- Ended: 2026-04-29 21:09:58 (Asia/Seoul)
- Total iterations: 2
- Pass: 2
- Fail: 0
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-210432`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-210432/summary.txt`
- Failure artifacts present: no

## 2026-04-29 21:10:07 Android soak run
- Started: 2026-04-29 21:10:07 (Asia/Seoul)
- Ended: 2026-04-29 21:41:22 (Asia/Seoul)
- Total iterations: 12
- Pass: 11
- Fail: 1
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-211007`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-211007/summary.txt`
- Failure artifacts present: yes

## 2026-04-29 23:19:41 Android soak run
- Started: 2026-04-29 23:19:41 (Asia/Seoul)
- Ended: 2026-04-29 23:25:04 (Asia/Seoul)
- Total iterations: 2
- Pass: 2
- Fail: 0
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-231941`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-231941/summary.txt`
- Failure artifacts present: no

## 2026-04-29 23:25:55 iOS soak run
- Started: 2026-04-29 23:25:55
- Ended: 2026-04-29 23:56:04
- Total iterations: 32
- Pass: 20
- Fail: 12
- Flow: `integration-test-app/maestro/ios-ad-validation.yaml`

## 2026-04-29 23:25:12 Android soak run
- Started: 2026-04-29 23:25:12 (Asia/Seoul)
- Ended: 2026-04-29 23:57:16 (Asia/Seoul)
- Total iterations: 13
- Pass: 9
- Fail: 4
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-232512`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260429-232512/summary.txt`
- Failure artifacts present: yes

## 2026-04-30 07:35:36 iOS soak run
- Started: 2026-04-30 07:35:36
- Ended: 2026-04-30 07:40:45
- Total iterations: 5
- Pass: 5
- Fail: 0
- Flow: `integration-test-app/maestro/ios-ad-validation.yaml`

## 2026-04-30 07:35:33 Android soak run
- Started: 2026-04-30 07:35:33 (Asia/Seoul)
- Ended: 2026-04-30 07:41:01 (Asia/Seoul)
- Total iterations: 2
- Pass: 2
- Fail: 0
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260430-073533`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260430-073533/summary.txt`
- Failure artifacts present: no

## 2026-04-30 08:20:47 iOS soak run
- Started: 2026-04-30 08:20:47
- Ended: 2026-04-30 08:51:27
- Total iterations: 30
- Pass: 30
- Fail: 0
- Flow: `integration-test-app/maestro/ios-ad-validation.yaml`

## 2026-04-30 09:48:14 Android soak run
- Started: 2026-04-30 09:48:14 (Asia/Seoul)
- Ended: 2026-04-30 09:56:13 (Asia/Seoul)
- Total iterations: 3
- Pass: 3
- Fail: 0
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260430-094814`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260430-094814/summary.txt`
- Failure artifacts present: no

## 2026-04-30 09:56:32 Android soak run
- Started: 2026-04-30 09:56:32 (Asia/Seoul)
- Ended: 2026-04-30 10:27:28 (Asia/Seoul)
- Total iterations: 12
- Pass: 11
- Fail: 1
- Flow: `integration-test-app/maestro/android-ad-validation.yaml`
- Output directory: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260430-095632`
- Summary: `/Users/gwangy.claw/.openclaw/workspace/NapSspReactNativePluginSample/integration-test-app/maestro/results/soak-30m-20260430-095632/summary.txt`
- Failure artifacts present: yes
