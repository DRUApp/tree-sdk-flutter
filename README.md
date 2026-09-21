# tree_sdk_flutter

Flutter bridge to the **Tree SDK**: Nawaf AI chat, health diary, medications and
reminders, backed by the Dru B2B API, in English and Arabic.

The plugin is the single entry point for both native SDKs: **iOS and
Android**. On any other platform `TreeSdk.start` throws
`TreeSdkException('unsupported_platform')`. One call site, either platform —
see [Android notes](#android-notes) below for the one behavioral difference
today (`TreeScreen.chat` as an `initial` screen).

## Install

```yaml
dependencies:
  tree_sdk_flutter:
    git:
      url: https://github.com/DRUApp/tree-sdk-flutter.git
      ref: v0.17.5
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

## Appearance

The SDK renders **light only**. It ships a single palette with no dark variant,
so the plugin pins both the SwiftUI colour scheme and the hosting controller's
`overrideUserInterfaceStyle`. A host in dark mode gets the SDK in light
regardless — nothing to configure, and no half-dark screens.

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

### iOS

- **iOS 15.6 minimum.** Set `IPHONEOS_DEPLOYMENT_TARGET = 15.6` (or higher) in
  your Runner project. Below that, Swift Package resolution fails with an opaque
  error. On 15.x three things degrade cosmetically and nothing breaks: sheets are
  full height rather than half (no `presentationDetents`), `List` rows keep the
  system separator inset, and the chat/notes fields are single-line rather than
  growing.
- **Swift Package Manager** (`flutter config --enable-swift-package-manager`) is
  the preferred route: `Package.swift` resolves `TreeSDK` from its tag for you,
  and it coexists with CocoaPods, so your other pods keep working.
- **CocoaPods** works too. `TreeSDK` is not on the CocoaPods trunk, so name its
  source in your `Podfile` — the plugin's podspec only pins the version:

  ```ruby
  pod 'TreeSDK', :git => 'https://github.com/DRUApp/ios-tree-sdk.git', :tag => 'v0.18.5'
  ```

  Keep that tag in step with the plugin version you are on.
- **Repository access.** Both this repo and `DRUApp/ios-tree-sdk` are private, so
  the host's machine and CI need credentials for each.
- **HealthKit, photos and notifications are not configured by this plugin.**
  Steps sync needs the HealthKit entitlement and `NSHealthShareUsageDescription`;
  reminder notifications need the SDK's permission prompt, which it currently
  requests on its own schedule. Without those, both features are inert but
  harmless.
- **The HbA1c photo sources follow the host's usage strings.** "upload Photo"
  offers Camera and Photos. `NSPhotoLibraryUsageDescription` turns the Photos row
  into the native Allow Full / Limited / Don't Allow flow; without it the SDK
  falls back to `PHPickerViewController`, which asks for nothing (requesting
  access with no usage description is a hard crash, so the SDK will not do it).
  `NSCameraUsageDescription` is what makes the Camera row appear at all, for the
  same reason.

### Android

- **`minSdkVersion 26` minimum** in your app's `android/app/build.gradle` (the
  `treesdk` library itself declares `minSdk = 26`).
- **JitPack, with credentials.** `android/build.gradle.kts` pulls
  `com.github.DRUApp.android-tree-sdk:treesdk:$treeSdkVersion` from
  `https://jitpack.io` — the Android equivalent of the iOS side resolving
  `TreeSDK` from a git tag via SPM, since `android-tree-sdk` has no Maven
  publishing step of its own. `DRUApp/android-tree-sdk` is private, so set a
  JitPack token in the consuming app's `~/.gradle/gradle.properties` as
  `jitpackToken=...`, or export `JITPACK_TOKEN` — the same "host's machine and
  CI need credentials" requirement as the iOS side, for the other repo.
- **Pin `treeSdkVersion`.** It defaults to `main-SNAPSHOT` (JitPack's
  build-the-tip-of-a-branch mode), which is not reproducible across builds. Set
  a real tag or commit SHA once one exists on GitHub, e.g. in your app's
  `android/gradle.properties`: `treeSdkVersion=1.2.3`.
- **Notifications are not configured by this plugin.** Reminder notifications
  need the SDK's own permission prompt, requested on its own schedule, same as
  iOS.
- **Tapping a reminder notification needs one addition to your `MainActivity`.**
  A reminder's `PendingIntent` targets your app's launcher Activity (the SDK
  has no Activity of its own to route to), so your `MainActivity` needs to
  hand that intent to the plugin before it can turn into a call to `start()`:

  ```kotlin
  import android.content.Intent
  import android.os.Bundle
  import com.dru.tree_sdk_flutter.TreeSdkFlutterPlugin
  import io.flutter.embedding.android.FlutterActivity

  class MainActivity : FlutterActivity() {
      override fun onCreate(savedInstanceState: Bundle?) {
          // Before super.onCreate() — that's what creates the Flutter engine
          // and attaches the plugin.
          TreeSdkFlutterPlugin.notePendingReminderIntent(intent)
          super.onCreate(savedInstanceState)
      }

      override fun onNewIntent(intent: Intent) {
          super.onNewIntent(intent)
          TreeSdkFlutterPlugin.notePendingReminderIntent(intent)
      }
  }
  ```

  On the Dart side, listen to `TreeSdk.onReminderTapped` early (e.g. your root
  widget's `initState`) and respond by calling `start()` again with your real
  config — same as any other entry point into the SDK. The native side reads
  which reminder was tapped from its own intent and opens straight to that
  reminder's screen; Dart never needs to know which one it was. See
  `flutter_application_1`'s `MainActivity.kt` and `main.dart` for a complete
  example. Unimplemented on iOS so far — `onReminderTapped` never fires there.

### Android notes

- Android's `TreeSDKConfig` has no unified full-screen chat route yet — Nawaf
  is a floating button ([`NawafFloatingButton`]) rather than a dedicated
  screen. `start(..., initial: TreeScreen.chat)` throws
  `TreeSdkException('unsupported_platform')` on Android specifically (checked
  in Dart, before anything reaches the platform); every other `TreeScreen`
  works the same as iOS.
- `userName` is a single string in this Dart API (matching iOS), but Android's
  native `TreeSDKConfig` models `firstName`/`lastName` separately. The plugin
  splits it on the first space — good enough for most names, but a multi-part
  family name may not split how you'd want. `gender` passes straight through
  as a free-form string (`"male"` / `"female"`, matching what the SDK already
  reads back from the user profile) — used only as a synchronous fallback
  before the real profile loads.

## Debugging

The bridge logs to the unified log on subsystem `com.dru.tree_sdk_flutter` —
config, initialize outcome, presentation, every `getToken`, and close:

```bash
xcrun simctl spawn booted log stream --level debug --style compact \
  --predicate 'subsystem == "com.dru.tree_sdk_flutter"'
```

Use `log stream --device` for a physical device. Tokens are logged **by length
only**, never by value.

On Android, the same events go to `Log.d`/`Log.i`/`Log.e` under tag
`TreeSdkFlutter`:

```bash
adb logcat -s TreeSdkFlutter:V
```

Note `print`/`debugPrint` from Dart does not reach the unified log and is not
visible to `simctl launch --console`, which is why the native side does the
logging.

Expect one `getToken` per backend request — nine or so to render Explore. If
your provider hits the network every time, that's nine calls to your auth
endpoint per screen. Cache.

## Wire contract

Four enums cross the channel as strings and **must** match the native side on
whichever platform is running: `TreeModule`, `TreeScreen`, `TreeEnvironment`,
and the failure codes above. `test/tree_sdk_flutter_test.dart` pins the Dart
side; `TreeRootViewTests.testScreenWireValuesAreStable` pins the Swift side.
On Android, `TreeSdkFlutterPlugin.moduleFromWire`/`screenFrom` do the mapping
by hand (no wire-string enum on that side yet) — a rename on either side has
to break a test here, same reasoning as the Swift pin.

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
