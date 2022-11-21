#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint flutter_naurt_sdk.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'flutter_naurt_sdk'
  s.version          = '0.0.1'
  s.summary          = 'Naurt\'s official flutter wrapper'
  s.description      = <<-DESC
  Naurt\'s official flutter wrapper, containing their Android & iOS SDKs
                       DESC
  s.homepage         = 'http://naurt.com'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'NaurtNickS' => 'support@naurt.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'Naurt.swift', '0.0.3'
  s.platform = :ios, '13.4'

  s.swift_version = '5.0'
end

