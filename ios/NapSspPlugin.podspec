Pod::Spec.new do |s|
  s.name         = 'NapSspPlugin'
  s.version      = '0.1.0'
  s.summary      = 'React Native plugin placeholder for NapSsp'
  s.homepage     = 'https://github.com/glenn-yu/NapSspReactNativePluginSample'
  s.license      = { :type => 'MIT' }
  s.author       = { 'glenn-yu' => 'gwangy.claw@example.com' }
  s.platform     = :ios, '13.0'
  s.static_framework = true
  s.source       = { :git => 'https://github.com/glenn-yu/NapSspReactNativePluginSample.git', :tag => s.version.to_s }
  s.source_files = 'ios/**/*.{swift,m}'
  s.swift_version = '5.0'
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'CLANG_ENABLE_MODULES' => 'YES',
    'APPLICATION_EXTENSION_API_ONLY' => 'NO'
  }
  s.dependency 'React-Core'
end
