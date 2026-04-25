# Maestro Stabilization Plan

## Goal
Stabilize `integration-test-app` on both Android and iOS so the app runs without runtime errors, Maestro flows pass reliably, and each platform can complete 10 repeated automated validation runs before final reporting.

## Scope
- Platform targets: Android and iOS
- App target: `integration-test-app`
- Automation target: Maestro flows under `integration-test-app/maestro`
- Model target for ongoing work: OpenAI Codex GPT-5.4

## Operating Rules
- Before making more fixes, keep the plan/TODO/worklog files updated and use them as the source of truth.
- Do not skip steps silently. If a phase changes, reflect it in the checklist and work log.
- Commit meaningful milestones continuously so progress is not lost.
- Record durable process lessons in memory so this workflow is not forgotten.
- Follow a Claude Code style flow: plan first, step-by-step TODO tracking, running work log, then execution.

## Execution Plan

### 1. Reconfirm current failure state
- Re-run Android and iOS smoke validation
- Separate failures into:
  - app build/runtime issues
  - Metro/bundle connectivity issues
  - Maestro selector/timing issues

### 2. Remove runtime errors first
- Fix test app code/config so Android and iOS launch without runtime errors
- Fix Metro readiness and device connectivity issues before Maestro assertions
- Rebuild and relaunch after each meaningful fix

### 3. Stabilize Maestro flows
- Make Android and iOS YAML selectors resilient to real screen state
- Reduce brittle assertions and add targeted waits/scrolls only where necessary
- Keep YAML updates saved in-repo and commit meaningful changes incrementally

### 4. Achieve single-pass success on both platforms
- Android single full pass
- iOS single full pass
- If either fails, loop back to runtime/app/flow fixes

### 5. Run repeated automated validation
- Run Android Maestro flow 10 times
- Run iOS Maestro flow 10 times
- Capture pass/fail counts and recurring failure points

### 6. Finalize and report
- Write an honest validation report with:
  - what passed
  - what remains flaky
  - whether results prove smoke only or real runtime readiness
- Commit remaining meaningful changes
- Notify the user with the final outcome

## Success Criteria
- Android app launches without runtime bundle errors
- iOS app launches without native/runtime crashes
- Android Maestro flow completes successfully
- iOS Maestro flow completes successfully
- 10 repeated Maestro runs are completed for each platform
- Final report is written and shared
