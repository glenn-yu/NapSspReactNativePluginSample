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
  s.source_files = 'ios/**/*.{h,m,swift}'
  s.resources    = 'ios/**/*.xib'
  s.exclude_files = 'ios/Package.swift'
  s.swift_version = '5.9'
  s.frameworks = 'Foundation', 'UIKit', 'AdSupport', 'StoreKit'
  s.weak_frameworks = 'AppTrackingTransparency'
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'CLANG_ENABLE_MODULES' => 'YES',
    'APPLICATION_EXTENSION_API_ONLY' => 'NO'
  }

  s.dependency 'React-Core'
  # Core SDK — unpinned so `pod update` picks up the latest 2.x. Verified against 2.5.0.
  s.dependency 'AdMixerMediation'

  # Mediation adapters are opt-in. Add the matching subspec to your Podfile, e.g.
  #   pod 'NapSspPlugin/GAM'
  # https://napmx.github.io/#/ios/native/getting-started
  s.subspec 'GAM' do |ss|
    ss.dependency 'AdMixerMediationGAM'
  end

  s.subspec 'AdFit' do |ss|
    ss.dependency 'AdMixerMediationAdFit'
  end

  s.subspec 'Pangle' do |ss|
    ss.dependency 'AdMixerMediationPangle'
  end

  s.subspec 'AppLovin' do |ss|
    ss.dependency 'AdMixerMediationAppLovin'
  end

  s.subspec 'UnityAds' do |ss|
    ss.dependency 'AdMixerMediationUnityAds'
  end

  # Naver Ad Manager
  s.subspec 'NAM' do |ss|
    ss.dependency 'AdMixerMediationNAM'
  end

  # Teads — requires TeadsSDK 6.2+ and Xcode 26 or newer.
  s.subspec 'Teads' do |ss|
    ss.dependency 'AdMixerMediationTeads'
  end
end
