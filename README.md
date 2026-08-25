# tree_sdk_flutter

Flutter bridge to the **Tree SDK**: Nawaf AI chat, health diary, medications and
reminders, backed by the Dru B2B API, in English and Arabic.

The plugin is the single entry point for both native SDKs. **Only iOS is
released today** — on any other platform `TreeSdk.start` throws
`TreeSdkException('unsupported_platform')`, so an Android build compiles and
runs, it just can't open the SDK yet. When the Android SDK lands, hosts get it by
bumping this package; no call-site change.

## Install

```yaml
dependencies:
  tree_sdk_flutter:
    git:
      url: https://github.com/DRUApp/tree-sdk-flutter.git
      ref: v0.1.2
```

## Use

```dart
import 'package:tree_sdk_flutter/tree_sdk_flutter.dart';

await TreeSdk.start(
  auth: TreeTokenProvider(() => myBackend.fetchTreeToken()),
  treeUserId: 'EG_PATIENT_5496',
  environment: TreeEnvironment.production,
  languageCode: 'ar',              // 'en' | 'ar' — 'ar' also flips the SDK to RTL
  initial: TreeScreen.explore,     // or .diary / .medications / .reminders / .chat
  modules: {TreeModule.diary, TreeModule.medications},
);
```

The SDK takes the whole screen and presents itself over your Flutter view.
`start` completes once it is **on screen**, not when the user leaves it:

```dart
TreeSdk.onClose.listen((_) => refreshMyHomeScreen());
final open = await TreeSdk.isPresented;
```

Calling `start` while it's already open is a no-op.

## Modules

`modules` is a licence, not a hint. Leaving one out hides its entry points and
blocks navigation into it:

| omitted | what disappears |
|---|---|
| `TreeModule.diary` | the metric carousel, "Add New Reading", the My Diary card, the Diary screen |
| `TreeModule.medications` | the Medications card, the Diary's Medication tab, the Medications screen |
| `TreeModule.nawafAi` | the floating chat bubble and the chat screen |

`TreeScreen.reminders` is not a module of its own — it needs `diary` or
`medications`. `TreeScreen.explore` is always reachable; its cards are gated
individually. Passing an `initial` screen that `modules` does not permit is a
`configuration` error, so check with `TreeScreen.diary.isAllowedBy(modules)` if
you build the set dynamically.

## Auth

Two options; pick at the call site.

```dart
// Production. Your server mints the token; no secret is in the app binary.
TreeTokenProvider(() => myBackend.fetchTreeToken())

// Development only. Puts the partner secret in the binary, where anyone who
// unzips the build can read it.
TreeClientCredentials(clientId: '…', clientSecret: '…')
```

`getToken` is called on **every backend request**, so return a cached token and
refresh only when it's near expiry. Throwing from it fails that request.

## Errors

Everything throws `TreeSdkException(code, message)`. `code` is the native
failure case — `unauthorized`, `network`, `timeout`, `forbidden`, `not_found`,
`validation`, `server`, `cancelled`, `configuration`, `unknown` — plus the
plugin's own `unsupported_platform` and `no_window`. Configuration mistakes
(empty user id, `custom` without a `baseUrl`, an unsupported language) are caught
in Dart before anything reaches the platform.

## Host requirements

- **iOS 16.0 minimum.** Set `IPHONEOS_DEPLOYMENT_TARGET = 16.0` in your Runner
  project. Below that, Swift Package resolution fails with an opaque error.
- **Swift Package Manager**, i.e. `flutter config --enable-swift-package-manager`.
  CocoaPods cannot carry this plugin: it depends on `TreeSDK`, which ships as an
  SPM package with no podspec. A CocoaPods host must add `TreeSDK` to its own
  target manually.
- **Repository access.** Both this repo and `DRUApp/ios-tree-sdk` are private, so
  the host's machine and CI need credentials for each.
- **HealthKit and notifications are not configured by this plugin.** Steps sync
  needs the HealthKit entitlement and `NSHealthShareUsageDescription`; reminder
  notifications need the SDK's permission prompt, which it currently requests on
  its own schedule. Without those, both features are inert but harmless.

## Debugging

The bridge logs to the unified log on subsystem `com.dru.tree_sdk_flutter` —
config, initialize outcome, presentation, every `getToken`, and close:

```bash
xcrun simctl spawn booted log stream --level debug --style compact \
  --predicate 'subsystem == "com.dru.tree_sdk_flutter"'
```

Use `log stream --device` for a physical device. Tokens are logged **by length
only**, never by value.

Note `print`/`debugPrint` from Dart does not reach the unified log and is not
visible to `simctl launch --console`, which is why the native side does the
logging.

Expect one `getToken` per backend request — nine or so to render Explore. If
your provider hits the network every time, that's nine calls to your auth
endpoint per screen. Cache.

## Wire contract

Four enums cross the channel as strings and **must** match the Swift raw values:
`TreeModule`, `TreeScreen`, `TreeEnvironment`, and the failure codes above.
`test/tree_sdk_flutter_test.dart` pins the Dart side;
`TreeRootViewTests.testScreenWireValuesAreStable` pins the Swift side.

## Development

```bash
flutter test      # 15 tests, no simulator needed
```

`ios/tree_sdk_flutter/Package.swift` depends on `TreeSDK` by version. To develop
against a local checkout instead, point it at a path — but note that Flutter
symlinks this package into `<host>/ios/Flutter/ephemeral/Packages/.packages/`
and SPM resolves relative paths against *that*, so a literal `../..` depends on
where the host app sits. Derive it from `#filePath` + `.resolvingSymlinksInPath()`.

The reference host is `tree/` in the SDK working repo — one Start button. Since
touch injection isn't available in the simulator there, it reaches the SDK with
`flutter build ios --simulator --debug --dart-define=TREE_AUTOSTART=true`.
