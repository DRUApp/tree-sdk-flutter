## 0.1.6

* Requires `TreeSDK` 0.1.6: saving a diary reading now asks Nawaf to comment on
  it and shows the reply beside the floating button. Gated on `nawafAI` being
  enabled, and silent for device-synced entries. No plugin API change.

## 0.1.5

* Requires `TreeSDK` 0.1.5. Medication: a dose logged from "Log details" is
  written back to the medicine, and the screen reopens on the last log's dose,
  tags and notes instead of a bare 1. HbA1c: the lab photo no longer spins
  forever when it can't be fetched, and tapping "upload Photo" asks for photo
  access up front (native Full / Limited / Don't Allow) before opening the
  library picker.
* **Hosts should declare `NSPhotoLibraryUsageDescription`** in their app's
  Info.plist. Without it the SDK falls back to `PHPickerViewController`, which
  needs no permission — requesting access without the key would crash the host.

## 0.1.4

* Requires `TreeSDK` 0.1.4: the SDK is pinned to the light colour scheme. It has
  one palette and no dark variant, and on a dark device its diary rows rendered
  as black cards with dark text. The plugin also pins the hosting controller's
  `overrideUserInterfaceStyle`, so UIKit-backed pieces (keyboard, pickers) match
  instead of following a dark host.

## 0.1.3

* Requires `TreeSDK` 0.1.3, which carries a reminder-notification fix and a
  custom alert sound (`tree_reminder.caf`). No plugin API change.

## 0.1.2

* Requires `TreeSDK` 0.1.2: `modules` now actually gates the UI. Leaving out
  `TreeModule.diary` or `.medications` hides the corresponding Explore cards,
  the metric carousel, and the Diary's Medication tab, and blocks navigation
  into those screens — previously it gated only the Nawaf bubble.
* `TreeScreen.isAllowedBy` exposes the same rule, and `start` rejects an
  `initial` screen that `modules` does not permit instead of silently opening
  somewhere else.
* Note `TreeScreen.reminders` needs `diary` or `medications`: reminders is not a
  module of its own.

## 0.1.1

* Requires `TreeSDK` 0.1.1, which verifies host-supplied tokens on `start`
  instead of accepting any non-empty string — an invalid or expired token now
  fails loudly rather than presenting a session whose every request 401s.
* Added unified-log instrumentation on subsystem `com.dru.tree_sdk_flutter`:
  config, initialize outcome, presentation, every `getToken` (length only,
  never the token), and close. Read it with
  `xcrun simctl spawn booted log stream --predicate 'subsystem == "com.dru.tree_sdk_flutter"'`.
  `print`/`debugPrint` was unusable here — Flutter's stdout does not reach the
  unified log.

## 0.1.0

* First packaged release.
* `TreeSdk.start` presents the native `TreeRootView` over the Flutter view.
* Auth: `TreeTokenProvider` (host-supplied bearer token, no secret in the
  binary) or `TreeClientCredentials` (development only).
* `TreeSdk.onClose` and `TreeSdk.isPresented` report presentation state.
* Typed `TreeEnvironment` / `TreeModule` / `TreeScreen`, with config validated
  in Dart before it reaches the platform.
* iOS only. Other platforms throw `unsupported_platform`.
