/**
 * React Native autolinking configuration.
 *
 * The Gradle `namespace` is `com.nasmedia.admixerssp.reactnative` so the generated `R` and
 * `BuildConfig` do not collide with the nap mx SDK, which owns `com.nasmedia.admixerssp`. The
 * Kotlin sources stay in `com.nasmedia.admixerssp`, so autolinking is told the real import path
 * instead of deriving it from the namespace.
 */
module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: 'android',
        packageImportPath: 'import com.nasmedia.admixerssp.NapSspPackage;',
        packageInstance: 'new NapSspPackage()',
      },
      // iOS is discovered from NapSspPlugin.podspec.
      ios: {},
    },
  },
};
