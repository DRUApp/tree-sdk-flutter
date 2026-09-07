## 0.8.3

* Requires `TreeSDK` 0.7.4 on iOS — no Dart API change:
  * **The medicine alert's three buttons are visible by default now.** iOS only shows a
    notification's actions in the *expanded* banner, so while the app is in the foreground the
    SDK suppresses the system banner and draws its own card — *i took it* / *snooze* /
    *cancel* on screen from the first frame, to the design's spec. A backgrounded or locked
    app still gets iOS's banner, with the buttons behind a long-press; that is the OS's own
    presentation and cannot be changed.

## 0.8.2

* Requires `TreeSDK` 0.7.3 on iOS — no Dart API change:
  * **The medicine notification's three buttons never appeared, because the notification was
    never scheduled.** Only opening the Medications screen ever armed a medicine's reminders,
    so anyone who reinstalled — or whose medicines were added on another device — had the
    Reminders screen listing times with nothing pending behind them. The Reminders screen
    arms them now, from the medicine list it already fetches.
  * **A snooze was cancelled by reopening the app.** Re-arming a medicine swept every pending
    request that was not in its plan, and a snooze deliberately is not — so the reminder
    pushed ten minutes out never came back.

## 0.8.1

* Requires `TreeSDK` 0.7.1 on iOS — no Dart API change:
  * **Interactive medicine notifications.** A medicine reminder carries three buttons —
    *i took it* / *snooze* / *cancel*. "i took it" logs the dose timestamped at the
    notification's own fire time, snooze re-fires the same notification (with the same
    buttons) after 10 minutes, cancel clears the alarm and logs nothing. Only medicines
    declare the actions; a reminder to go and measure something keeps none. Answering from
    the notification no longer also opens the app behind it.
  * HbA1c's reminder row showed no date — `start_date` has no time part, which both ISO8601
    formatters reject, so it decoded as nil and the detail screen fell back to today.
  * The Explore carousel only swiped from the far edge of the card, and a summary sparkline
    with a single reading drew a lone dot in an otherwise empty card.

## 0.8.0

* Requires `TreeSDK` 0.7.0 on iOS — a QA round, no Dart API change:
  * **Arabic-Indic digits are accepted everywhere.** `Int("٦")` and `Double("٦")` are both
    nil, so every numeric field silently dropped what the Arabic keyboard typed and kept its
    previous value. Fixed at all seven numeric-input sites, not just the reported one.
  * **Blood pressure charts as a pair.** The metric chart plotted systolic alone and dropped
    every diastolic reading; both lines now draw, and "time in range" requires both halves
    in range rather than passing 201/80 because 80 is fine.
  * **Explore cards show the real latest reading.** They read the shared 25-entry first page,
    so a metric whose last reading was older — typically weight and waist — showed
    "No readings yet" until you logged again. Swiping the carousel is cheaper too.
  * Reminder rows show the next time due rather than the earliest; the medication delete
    prompt no longer freezes in the language the host first started in; HbA1c opens its
    ring at 0, matching dru.
* Android still requires `android-tree-sdk` 0.4.0 and is unported for all of the above.

## 0.7.0

* **The iOS floor drops to 15.6.** Requires `TreeSDK` 0.6.0, which back-deploys off every
  iOS 16 API it used. A host can now set `IPHONEOS_DEPLOYMENT_TARGET = 15.6`; one already
  on 16.0 or higher needs no change. On 15.x three things degrade cosmetically and nothing
  breaks: sheets are full height rather than half, `List` rows keep the system separator
  inset, and the chat/notes fields are single-line rather than growing.
* **CocoaPods is supported alongside SPM.** `tree_sdk_flutter.podspec` now declares
  `s.dependency 'TreeSDK'`, so a host that cannot enable Swift Package Manager names the
  source in its own `Podfile` and builds. SPM stays the preferred route and is unchanged.
* `pubspec.yaml`'s Dart constraint widens to `>=3.0.0 <4.0.0`; it pinned `^3.12.2`, which
  turned away hosts on an older-but-compatible SDK for no reason.
* No Dart API change. Android still requires `android-tree-sdk` 0.4.0.

## 0.6.6

* Requires `TreeSDK` 0.5.6 on iOS — the diary performance fix. `DiaryEntry.recordedAt` was
  a computed property that built an `ISO8601DateFormatter` and re-parsed the wire string on
  every read, and it backs every sort, grouping and chart comparison; it is parsed once at
  decode now. Same name and type, so nothing changes for a host. Also fixes entries whose
  timestamps carry fractional seconds, which the old parser rejected outright.

## 0.6.5

* Requires `TreeSDK` 0.5.5 on iOS — a discard confirmation on the workout log form, plus
  header tidying across medication, reminders and the workout tab. No public API change
  on either side; `TreeEnvironment` and the token store are untouched and
  `android-tree-sdk` stays at v0.4.0.

## 0.6.4

* Requires `TreeSDK` 0.5.4 on iOS. Fixes a blood-pressure classification bug: a reading
  above every `risk_level` band was unclassified, so 207/80 fell back to diastolic's
  yellow and read "Elevated" where dru says "Very High". A value above the highest band
  now takes that band, matching dru's unbounded `systolic >= 140` branch. Not symmetric
  at the bottom on purpose — clamping downwards would paint a hypoglycaemic reading
  green. Also medication list/tile controls. No public API change on either side.

## 0.6.3

* Requires `TreeSDK` 0.5.3 on iOS — metric detail chart fixes, workout log form, reminder
  detail and Arabic copy, plus `TreeLocale` adjustments. No public API change on either
  side; `TreeEnvironment` and the token store are untouched and `android-tree-sdk` stays
  at v0.4.0.

## 0.6.2

* Requires `TreeSDK` 0.5.2 on iOS — Steps chart icons, and medication search/custom-form
  fixes. No public API change on either side, so nothing changes for a host;
  `TreeEnvironment` and the token store are untouched and `android-tree-sdk` stays at
  v0.4.0.

## 0.6.1

* Requires `TreeSDK` 0.5.1 on iOS — design matching for the reminder detail screen,
  the diary/steps charts and Arabic copy, plus tab memory. No public API change on
  either side, so nothing changes for a host: `TreeEnvironment` and the token store
  are untouched and the two SDKs still agree. `android-tree-sdk` stays at v0.4.0.

## 0.6.0

* Requires `TreeSDK` 0.5.0 on iOS. That release adds `MeasurementDefinition.healthyRange`
  — the green `risk_level` rather than `allowed_range` — which fixes "Hypoglycaemia
  events" reading 0% and "Time in range" 100% forever, plus the chart's healthy band.
  Also: blood-pressure insertion matched to the old app, the Explore header and Arabic
  date/time, and the carousel status indicator.
* No plugin API change, and no change on Android: `android-tree-sdk` is untouched by this,
  and `TreeEnvironment` and the token store are identical on both sides, so the two SDKs
  still agree.

## 0.5.1

* Requires `TreeSDK` 0.4.1 on iOS — the design alignment pass (icons, colours, type
  sizes and diary chrome ported from `android-tree-sdk`, which is the source of truth
  for all four). No plugin API change, and no change on Android: `android-tree-sdk`
  stays at v0.4.0 because nothing in it changed.

## 0.5.0

* Requires `TreeSDK` 0.4.0 (iOS) and `android-tree-sdk` v0.4.0 — **the Android
  SDK's first tag**, so the Gradle dependency is a reproducible version instead
  of `main-SNAPSHOT`. Bump the two together from here on.
* **`TreeEnvironment` now means the same thing on both platforms**, and all
  three cases resolve the b2b: `production` → `b2b-api.dru-app.org`, `staging`
  → `stag-b2b-api.dru-app.org`, `development` → `dev-b2b-api.dru-app.org`.
  Previously `production`/`staging` pointed at the legacy Dru API on both, and
  Android's `development` was `http://localhost:3002` — unreachable from a
  phone. A host that worked around this with `TreeEnvironment.custom` plus an
  explicit `baseUrl` can drop back to the named case.
* **Fixes a token cache that ignored which backend minted it.** The stored
  bundle was keyed by a constant on both platforms, and the SDK trusts any
  unexpired one, so switching environment — or rotating a client id — silently
  reused a foreign token and reported a working session against a backend that
  rejects those credentials. Requests then 401 behind the UI, which looks like
  a user with no data. The store is now namespaced by base URL and client id.
  Existing installs re-authenticate once.
* `mavenLocal()` now precedes JitPack, so `publishToMavenLocal` in
  `android-tree-sdk` is picked up for local SDK work.

## 0.4.0

* Requires `TreeSDK` 0.3.0 (iOS). The SDK now ships GT Planar as its typeface,
  registered at runtime from the package bundle — **hosts need no font entry in
  Info.plist**. Arabic keeps the system font, which GT Planar has no glyphs for.
* **New host requirement:** live workout distance is read from `CMPedometer`, so
  a host that runs the workout tracker must declare `NSMotionUsageDescription`.
  Without it the tracker reports unavailable, distance stays 0.00 and — since
  calories are now derived from measured pace — a live session saves 0 kcal.
* Fixes carried from the SDK: diary tabs keep their own scroll position, a dose
  row in the All list opens that dose to edit, swipe-to-delete on the All list,
  the blood-pressure slider spans 40–300, the workout duration dialog is the
  Figma card, medicine reminders are listed under Reminders, Water reminders no
  longer revert to 11:00, the medication delete confirmation is centred and
  names the medicine, Nawaf opens on the newest message, and Arabic gains
  ملم زئبقي / مجم/دل / العلامات.

## 0.3.1

* Requires `TreeSDK` 0.2.1 (iOS): the save/update toast gains the "View all"
  action that jumps to that metric's tab, medicine dose units read Unit / وحدة
  instead of "other" (and a dose is logged with the medicine's own unit), a
  second attempt at connecting Apple Health offers Settings instead of doing
  nothing, and Nawaf's requests get their own 120s timeout — a normal answer
  takes 21–30s and was being cut off by the 30s default, which surfaced as a
  Retry button.
* Also carries the Android work that landed on `main` after 0.3.0 (activity and
  plugin changes, plus a Dart addition) — not authored here, just released
  alongside the dependency bump.

## 0.3.0

* Requires `TreeSDK` 0.2.0 (iOS), which is a QA round across the native SDK:
  weight change measured from the metric's own history (and zero instead of a
  blank), tapping a day in the Steps Week/Month charts, a revoked Apple Health
  permission now disconnecting, Medicine in the Explore carousel and its arrange
  sheet, catalogue medicine photos surviving the save, a medicine reminder tap
  opening that medicine's log, the system swipe-to-delete on Water reminder
  times, Nawaf reopening the conversation you were just in, and the floating orb
  dragging correctly in Arabic.
* No plugin or Android change: the bump is the iOS dependency and this note.

## 0.2.1

* Requires `TreeSDK` 0.1.9 (iOS): the Steps screen uses the supplied design
  assets — the footprint glyph in the ring and the routing icon on Total
  Distance — and the diary chip bar gets its own steps icon, with the selected
  chip now scrolling into view. No plugin or Android change.

## 0.2.0

* **Android support.** `TreeSdk.start` now works on Android as well as iOS —
  same Dart API, no call-site change. Requires `android-tree-sdk`'s `treesdk`
  module, pulled via JitPack (see README's Android host requirements).
* Added `TreeRootView` (module gating + screen routing) and a `TreeScreen`
  enum to `android-tree-sdk` itself, plus a `gender` field on
  `TreeSDKConfig`, to bring the native Android SDK to parity with what this
  plugin already assumed from iOS.
* One platform difference: `initial: TreeScreen.chat` is iOS-only for now —
  Android has no dedicated full-screen chat route yet (Nawaf is a floating
  button there). Throws `TreeSdkException('unsupported_platform')` on
  Android, checked in Dart before anything reaches the platform.
* `userName` is split into `firstName`/`lastName` for Android's native config,
  which models them separately rather than as one string.
* Requires `TreeSDK` **0.1.8** on iOS, so this release also carries the two QA
  rounds below.

## 0.1.8

* Requires `TreeSDK` 0.1.8 — second QA round, no plugin API change: the Arabic
  weight picker matches the old app (unit beside the whole number, drums pinned
  LTR); reminder notification bodies are localized; presented sheets keep the
  SDK's RTL/locale, which fixes the medication time wheel in Arabic; saving or
  updating a log shows the old app's confirmation toast; an HbA1c photo is no
  longer cropped and opens full screen with pinch-zoom.

## 0.1.7

* Requires `TreeSDK` 0.1.7 — a round of QA fixes, none of which change the
  plugin API: the Nawaf bubble now lives in its own window so sheets and
  full-screen covers can't hide it; tapping a reminder notification opens the
  screen it reminded you about; medication doses appear in the diary's All list;
  medication reminders survive a reminders sync and re-arm on load; water logs
  keep the container the user picked; Arabic fixes on the weight wheel.
* Host apps that want the full HbA1c photo flow should declare
  `NSPhotoLibraryUsageDescription` and `NSCameraUsageDescription`, and Steps sync
  still needs the HealthKit entitlement plus `NSHealthShareUsageDescription`.
  See the README's requirements section.

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
