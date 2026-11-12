# ExampleHostApp

This React Native 0.81.6 host exercises all six ad formats and displays their events. It depends on the local plugin via `file:../..`. Use Node 20.19.4 or newer and set up the native React Native toolchain for your platform.

From the repository root, build the plugin first, then install the example dependencies:

```sh
npm install
npm run build
cd example/ExampleHostApp
npm install
```

Review `adConfig.ts` and replace the media key and ad unit IDs with numeric values provisioned for your own app. Check the platform configuration and adapter requirements in [Setup](../../docs/SETUP.md). Android currently selects `admanager,adfit` in `android/gradle.properties`; iOS selects GAM and AdFit in `ios/Podfile` before autolinking. Select the adapters for your integration; see [Setup](../../docs/SETUP.md#4-1-cocoapods).

Start Metro from `example/ExampleHostApp`:

```sh
npm start
```

In another terminal in the same directory, run `npm run android` with an emulator or device available. On macOS, install iOS pods first and then run the app:

```sh
cd ios
bundle install
bundle exec pod install
cd ..
npm run ios
```

After changing plugin source, rebuild it with `npm run build` at the repository root and refresh the example's local dependency if necessary. Reinstall pods after native dependency changes and rebuild the native app. SDK initialization and ad delivery require valid configuration and network access; a no-fill response is possible. The 0.6.0 native builds and device flows have not been verified in this update.
