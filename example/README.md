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

## Build a distributable APK

```bash
flutter build apk --release
```

The APK lands at `build/app/outputs/flutter-apk/app-release.apk` — install it
directly on any Android 8.0+ (API 26+) device, or upload it wherever you want
it reachable (Firebase App Distribution, a plain download link, etc.). It's
signed with the Flutter debug key by default (see `android/app/build.gradle.kts`)
so it installs out of the box; swap in your own signing config before
distributing it beyond your own testing.

Note that the release APK bundles the native SDK build that was in mavenLocal at
build time, so publish the SDK first if you've changed it.
