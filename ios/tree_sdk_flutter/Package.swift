// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "tree_sdk_flutter",
    platforms: [
        // TreeSDK's floor. The host app's IPHONEOS_DEPLOYMENT_TARGET must match
        // or exceed this, or SPM refuses to resolve.
        .iOS("15.6")
    ],
    products: [
        .library(name: "tree-sdk-flutter", targets: ["tree_sdk_flutter"])
    ],
    dependencies: [
        .package(name: "FlutterFramework", path: "../FlutterFramework"),
    ],
    targets: [
        .target(
            name: "tree_sdk_flutter",
            dependencies: [
                .product(name: "FlutterFramework", package: "FlutterFramework"),
                "TreeSDK",
            ]
        ),
        // The SDK ships as a compiled framework that lives in this package, so a
        // host resolves one repository and never sees the SDK's source. Rebuild
        // it from the source repo with Tools/build-xcframework.sh, which copies
        // it here. The path is inside the package, so it survives the symlink
        // Flutter makes into the host's ios/Flutter/ephemeral/.
        .binaryTarget(name: "TreeSDK", path: "TreeSDK.xcframework"),
    ]
)
