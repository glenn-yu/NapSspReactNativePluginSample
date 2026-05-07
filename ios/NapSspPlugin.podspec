Pod::Spec.new do |s|
  s.name         = 'NapSspPlugin'
  s.version      = '0.1.4'
  s.summary      = 'React Native scaffold for KT Nasmedia nap ssp SDK'
  s.homepage     = 'https://github.com/glenn-yu/react-native-nap-ssp'
  s.license      = { :type => 'MIT', :file => 'LICENSE' }
  s.author       = { 'gwangy' => 'gwangy@example.com' }
  s.platform     = :ios, '14.0'
  s.static_framework = true
  s.source       = { :path => '.' }
  s.preserve_paths = '*', 'README.md'
  s.source_files = '*.{h,m,swift}'
  s.resources = '*.{xib}'
  s.exclude_files = 'Package.swift'
  s.swift_version = '5.0'
  s.frameworks = 'Foundation', 'UIKit', 'AdSupport', 'StoreKit'
  s.weak_frameworks = 'AppTrackingTransparency'
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'CLANG_ENABLE_MODULES' => 'YES',
    'APPLICATION_EXTENSION_API_ONLY' => 'NO',
    'OTHER_SWIFT_FLAGS' => '$(inherited) -D COCOAPODS'
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
end
