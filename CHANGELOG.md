## 0.1.0

* First packaged release.
* `TreeSdk.start` presents the native `TreeRootView` over the Flutter view.
* Auth: `TreeTokenProvider` (host-supplied bearer token, no secret in the
  binary) or `TreeClientCredentials` (development only).
* `TreeSdk.onClose` and `TreeSdk.isPresented` report presentation state.
* Typed `TreeEnvironment` / `TreeModule` / `TreeScreen`, with config validated
  in Dart before it reaches the platform.
* iOS only. Other platforms throw `unsupported_platform`.
