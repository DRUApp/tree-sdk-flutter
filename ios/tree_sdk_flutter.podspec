#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint tree_sdk_flutter.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'tree_sdk_flutter'
  s.version          = '0.20.0'
  s.summary          = 'Flutter bridge to the native Tree SDK.'
  s.description      = <<-DESC
Presents the native Tree SDK (Nawaf AI chat, health diary, medications,
reminders) from a Flutter host.
                       DESC
  s.homepage         = 'https://github.com/DRUApp/tree-sdk-flutter'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'DRU' => 'aelgohary@dru-app.com' }
  s.source           = { :path => '.' }
  s.source_files = 'tree_sdk_flutter/Sources/tree_sdk_flutter/**/*'
  s.dependency 'Flutter'
  s.vendored_frameworks = 'tree_sdk_flutter/TreeSDK.xcframework'
  s.platform = :ios, '15.6'

  # TreeSDK is vendored as a compiled framework rather than pulled from a second
  # repository, so a CocoaPods host adds nothing to its Podfile and the SDK's
  # source never leaves DRUApp/ios-tree-sdk. Swift Package Manager hosts get the
  # same binary through tree_sdk_flutter/Package.swift's .binaryTarget.
  # Tools/build-xcframework.sh in the SDK repo rebuilds and copies it in.

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'

  # If your plugin requires a privacy manifest, for example if it uses any
  # required reason APIs, update the PrivacyInfo.xcprivacy file to describe your
  # plugin's privacy impact, and then uncomment this line. For more information,
  # see https://developer.apple.com/documentation/bundleresources/privacy_manifest_files
  # s.resource_bundles = {'tree_sdk_flutter_privacy' => ['tree_sdk_flutter/Sources/tree_sdk_flutter/PrivacyInfo.xcprivacy']}
end
