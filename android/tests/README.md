# SDK listener JVM regression test

Run from the repository root on Windows with Java on `PATH`:

```powershell
powershell -ExecutionPolicy Bypass -File android/tests/run-listener-test.ps1
```

The runner compiles the actual production `NapAdListener.kt` against the real SDK's
`classes.jar`, then checks callback forwarding and stale callback suppression,
including a callback queued before teardown. No Android device or React Native AAR
is needed. This is a focused listener test, not a replacement for the Android library
build or device lifecycle tests.

All dependencies must already exist in the local Gradle cache: AdMixer SSP 2.5.1,
Kotlin compiler/stdlib/reflect/script-runtime 2.1.20, Trove, Kotlin coroutines, and
JetBrains annotations. The runner performs no downloads and fails if anything is
missing. Use `-GradleUserHome`, `-KotlinVersion`, or `-SdkVersion` to override those
locations/versions; the selected Kotlin version must have all four artifacts cached.
JARs and compiled test output are extracted into a unique temporary directory whose
path is printed. No SDK binaries are stored in the repository.
