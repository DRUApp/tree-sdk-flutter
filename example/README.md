# tree_sdk_flutter_example

A minimal harness for the `tree_sdk_flutter` plugin, preset with the dev B2B
credentials:

- Backend base URL: `https://dev-b2b-api.dru-app.org`
- Tree user ID: `EG_PATIENT_5555`
- Client ID: `65768202`
- Client secret: (see `lib/main.dart`)
- Language: `en`

It uses `TreeClientCredentials` (dev-only auth — the secret ships in the
binary). Swap for `TreeTokenProvider` before shipping this to anyone real.

## Before your first build: publish the native SDK locally (no JitPack token)

`tree_sdk_flutter`'s Android side depends on
`com.github.DRUApp.android-tree-sdk:treesdk`, which normally comes from JitPack
and needs a token because that GitHub repo is private. This app skips JitPack
entirely by resolving the same coordinates from your **local Maven repo**
instead — `android-tree-sdk`'s `treesdk` module already publishes itself under
exactly `com.github.DRUApp.android-tree-sdk:treesdk:main-SNAPSHOT` for this
purpose.

Run once (and again whenever you change the native SDK):

```bash
./tools/publish_local_sdk.sh
# same thing by hand:
cd ../../android-tree-sdk && ./gradlew :treesdk:publishToMavenLocal
```

That builds the SDK from your local checkout and drops the AAR + POM in
`~/.m2/repository/com/github/DRUApp/android-tree-sdk/treesdk/main-SNAPSHOT/`.
`android/build.gradle.kts` lists `mavenLocal()` ahead of the plugin's JitPack
entry, so the local artifact wins and JitPack is never contacted.

If a build ever fails with a JitPack 401/404 for `treesdk`, that means the local
publish is missing or stale — re-run the command above.

## Run it

```bash
flutter pub get
flutter run            # needs a device/emulator (API 26+)
```

## Build a distributable APK (BetaDrop etc.)

```bash
./tools/publish_local_sdk.sh     # only if the native SDK changed since last time
flutter build apk --release --target-platform android-arm,android-arm64
```

Output: `build/app/outputs/flutter-apk/app-release.apk` — one APK carrying both
ABIs real phones use, so testers still get a single file to install. Don't use
`--split-per-abi` for BetaDrop; that produces several per-architecture APKs and
testers would have to pick.

Notes:

- **Why the `--target-platform` flag.** Without it Flutter also packs
  `x86_64`, which only an emulator can load: 17 MB of the APK that no tester
  will ever execute. Add `,android-x64` back when you need to hand the same
  file to somebody running an emulator.
- **Shrinking is ON** (no `--no-shrink`, and `isMinifyEnabled = true` in
  `android/app/build.gradle.kts`). Unshrunk, the dex came to 55 MB of a 109 MB
  APK. The Tree SDK decodes JSON reflectively (Moshi
  `KotlinJsonAdapterFactory`) and its `consumer-rules.pro` now covers that
  surface — wire models, `@Json` fields anywhere, wire enums, `kotlin.Metadata`,
  `Continuation`. Anything those rules still miss is stripped and blows up at
  runtime *only in release*, so **smoke-test login, a diary save and Nawaf on
  the release build before uploading it**. If something breaks, add
  `--no-shrink` and set both flags back to `false` to unblock the upload, then
  fix the keep rule.
- **Signing.** With no `android/key.properties`, the APK is signed with your
  local debug key. It installs fine, but debug keys differ per machine, so a
  build from another laptop can't update it. For a stable distribution track,
  create a keystore and `key.properties` — see the comment at the top of
  `android/app/build.gradle.kts`.
- **Bump the version for each upload.** `version: 1.0.0+1` in `pubspec.yaml`
  feeds `versionName`/`versionCode`; increment the `+N` build number or
  distribution tools will treat re-uploads as the same build.
- **The client secret is inside this APK.** `TreeClientCredentials` embeds it in
  the binary, and anyone who downloads the APK can unzip and read it. Fine for a
  dev backend and a known tester list; move to `TreeTokenProvider` before this
  goes anywhere wider.
