Pod::Spec.new do |s|
  s.name         = 'NapSspPlugin'
  s.version      = '0.2.0'
  s.summary      = 'React Native scaffold for KT Nasmedia nap ssp SDK'
  s.homepage     = 'https://github.com/glenn-yu/react-native-nap-ssp'
  s.license      = { :type => 'MIT', :file => 'LICENSE' }
  s.author       = { 'gwangy' => 'gwangy@example.com' }
  s.platform     = :ios, '14.0'
  s.static_framework = true
  s.source       = { :path => '.' }
  s.preserve_paths = 'ios/**/*', 'LICENSE', 'README.md'
  s.source_files = 'ios/**/*.{h,m,swift}'
  s.resources = 'ios/**/*.{xib}'
  s.exclude_files = 'ios/Package.swift'
  s.swift_version = '5.0'
  s.frameworks = 'Foundation', 'UIKit', 'AdSupport', 'StoreKit'
  s.weak_frameworks = 'AppTrackingTransparency'
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'CLANG_ENABLE_MODULES' => 'YES',
    'APPLICATION_EXTENSION_API_ONLY' => 'NO'
  }

  s.dependency 'React-Core'
  s.dependency 'AdMixerMediation'

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

  # Naver Ad Manager — v2.3.7 가이드에서 추가된 어댑터 / adapter added in the v2.3.7 guide
  s.subspec 'NAM' do |ss|
    ss.dependency 'AdMixerMediationNAM'
  end
end
