# Maestro Results Policy

## Default policy
- Commit and push **summarized test results only**.
- Do **not** push raw runtime artifacts by default.

## Raw artifacts not pushed by default
Examples:
- `summary.txt` from timestamped run directories
- `run-*.log`
- `fail-*.png`
- `logcat-fail-*.txt`
- temporary simulator screenshots

These remain local for debugging unless explicitly requested.

## What should be pushed
Push a concise repository-tracked summary document that captures:
- test window and platform
- pass/fail counts
- whether the run stopped early
- stop reason if repeated failures triggered a stop
- key observed failure classes
- links or paths to local artifact directories when needed for later retrieval

## Repeat-failure safeguard
- If the same failure class occurs 3 times in a row, the soak runner stops early.
- The stop reason is recorded in the run summary and cumulative history.

## Current intent
Use this policy for ongoing Maestro soak validation of `integration-test-app` unless the user explicitly asks to version raw artifacts.
