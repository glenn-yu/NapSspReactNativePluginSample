require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = 'NapSspPlugin'
  s.version      = package['version']
  s.summary      = 'React Native bridge for the nap mx (AdMixer SSP) iOS SDK'
  s.description  = package['description']
  s.homepage     = package['homepage']
  s.license      = { :type => 'MIT', :file => 'LICENSE' }
  s.author       = package['author']
  # The core SDK supports iOS 13; the AdFit and Teads adapters require iOS 14.
  s.platform     = :ios, '14.0'
  s.static_framework = true
  s.source       = { :git => package['repository']['url'].sub('git+', ''), :tag => "v#{s.version}" }
  s.preserve_paths = 'ios/**/*', 'LICENSE', 'README.md'
  s.swift_version = '5.9'
  s.frameworks = 'Foundation', 'UIKit', 'AdSupport', 'StoreKit'
  s.weak_frameworks = 'AppTrackingTransparency'
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'CLANG_ENABLE_MODULES' => 'YES',
    'APPLICATION_EXTENSION_API_ONLY' => 'NO'
  }

  s.dependency 'React-Core'
  # Mediation 2.6.1 pulls in AdMixer Core >= 1.5.1.
  s.dependency 'AdMixerMediation', '>= 2.6.1', '< 3.0'

  # Mediation adapters are opt-in. Add the matching subspec to your Podfile, e.g.
  #   pod 'NapSspPlugin', :path => '../node_modules/react-native-nap-ssp', :subspecs => ['GAM']
  # Place this declaration before use_native_modules! in the target.
  # https://napmx.github.io/#/ios/native/getting-started
  # Keep bridge sources on an explicitly selected spec. Adapter-only selections must
  # also compile the bridge rather than producing a dependency-only pod target.
  s.default_subspecs = 'Core'
  s.subspec 'Core' do |ss|
    ss.source_files = 'ios/**/*.{h,m,swift}'
    ss.resources = 'ios/**/*.xib'
  end

  s.subspec 'GAM' do |ss|
    ss.dependency 'NapSspPlugin/Core'
    ss.dependency 'AdMixerMediationGAM'
  end

  s.subspec 'AdFit' do |ss|
    ss.dependency 'NapSspPlugin/Core'
    ss.dependency 'AdMixerMediationAdFit'
  end

  s.subspec 'Pangle' do |ss|
    ss.dependency 'NapSspPlugin/Core'
    ss.dependency 'AdMixerMediationPangle'
  end

  s.subspec 'AppLovin' do |ss|
    ss.dependency 'NapSspPlugin/Core'
    ss.dependency 'AdMixerMediationAppLovin'
  end

  s.subspec 'UnityAds' do |ss|
    ss.dependency 'NapSspPlugin/Core'
    ss.dependency 'AdMixerMediationUnityAds'
  end

  # Naver Ad Manager
  s.subspec 'NAM' do |ss|
    ss.dependency 'NapSspPlugin/Core'
    ss.dependency 'AdMixerMediationNAM'
  end

  # Teads — requires TeadsSDK 6.2+ and Xcode 26 or newer.
  s.subspec 'Teads' do |ss|
    ss.dependency 'NapSspPlugin/Core'
    ss.dependency 'AdMixerMediationTeads'
  end
end
