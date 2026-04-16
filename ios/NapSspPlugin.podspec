Pod::Spec.new do |s|
  s.name         = 'NapSspPlugin'
  s.version      = '0.1.0'
  s.summary      = 'React Native plugin placeholder for NapSsp'
  s.platform     = :ios, '13.0'
  s.source       = { :git => 'https://github.com/glenn-yu/NapSspReactNativePluginSample.git', :tag => s.version }
  s.source_files = 'ios/**/*.{swift,h,m}'
  s.dependency 'React-Core'
end
