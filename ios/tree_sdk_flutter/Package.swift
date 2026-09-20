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
        // Identity is the repo name ("ios-tree-sdk"), which is why the product
        // below says `package: "ios-tree-sdk"` and not `"TreeSDK"`.
        .package(url: "https://github.com/DRUApp/ios-tree-sdk.git", from: "0.17.0"),
    ],
    targets: [
        .target(
            name: "tree_sdk_flutter",
            dependencies: [
                .product(name: "FlutterFramework", package: "FlutterFramework"),
                .product(name: "TreeSDK", package: "ios-tree-sdk"),
            ]
        )
    ]
)
