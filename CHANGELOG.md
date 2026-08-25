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
