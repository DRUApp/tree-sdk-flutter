#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint tree_sdk_flutter.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'tree_sdk_flutter'
  s.version          = '0.7.0'
  s.summary          = 'Flutter bridge to the native Tree SDK.'
  s.description      = <<-DESC
Presents the native Tree SDK (Nawaf AI chat, health diary, medications,
reminders) from a Flutter host.
                       DESC
  s.homepage         = 'https://github.com/DRUApp/ios-tree-sdk'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'DRU' => 'aelgohary@dru-app.com' }
  s.source           = { :path => '.' }
  s.source_files = 'tree_sdk_flutter/Sources/tree_sdk_flutter/**/*'
  s.dependency 'Flutter'
  s.dependency 'TreeSDK', '>= 0.6.0'
  s.platform = :ios, '15.6'

  # TreeSDK is not on the CocoaPods trunk, so a CocoaPods host must name the
  # source itself — this dependency only pins the version:
  #
  #   pod 'TreeSDK', :git => 'https://github.com/DRUApp/ios-tree-sdk.git', :tag => 'v0.6.0'
  #
  # A Swift-Package-Manager host (flutter config --enable-swift-package-manager)
  # needs none of that — ../ios/tree_sdk_flutter/Package.swift resolves TreeSDK
  # from the same tag. Keep this version in lockstep with that Package.swift.

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'

  # If your plugin requires a privacy manifest, for example if it uses any
  # required reason APIs, update the PrivacyInfo.xcprivacy file to describe your
  # plugin's privacy impact, and then uncomment this line. For more information,
  # see https://developer.apple.com/documentation/bundleresources/privacy_manifest_files
  # s.resource_bundles = {'tree_sdk_flutter_privacy' => ['tree_sdk_flutter/Sources/tree_sdk_flutter/PrivacyInfo.xcprivacy']}
end
