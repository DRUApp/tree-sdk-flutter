## 0.14.0

* Requires `TreeSDK` 0.13.0 on iOS. Additive Swift surface only
  (`ExploreView.init` gains a defaulted `focusMetricID`, plus the
  `ExploreView.medicineFocus` constant); no Dart API change.
  * **The carousel lands on the metric you just logged.** Saving from Explore
    returns to Explore, but the view is torn down while the Diary carries the
    form, so the pager came back on card 0 and the new reading was several
    swipes away. A logged dose does the same through the same field — the
    medications screen has no subcategory id, so the host names the Medicine
    card and Explore resolves it from the catalogue.
  * **Nawaf's card is one height across both states** and eases between them,
    rather than jumping when the conversation starts.
  * Air above the card on a Dynamic Island device, and its top corners go square
    on *both* sides while the history drawer is open — the trailing one used to
    read as a stray rounded fragment against the green behind it.
  * **The weight drum keeps its columns left-to-right in Arabic.** SwiftUI hands
    a representable an RTL host, so the components came out mirrored and the
    grams sat where the kilos belong.
  * The empty state's Arabic button is the imperative form.

## 0.13.1

* Requires `TreeSDK` 0.12.1 on iOS. No API change on either side — a design round.
  * **One empty state everywhere.** The All tab drops its own illustration and its
    two private sentences for the shared skeleton the metric tabs already had, and
    the subtitle is the same line on every screen that draws it. The block sits a
    fixed 32pt under the chip row rather than centring in whatever space the info
    card leaves, so it no longer moves with the card's height.
  * **Nawaf's hero panel matches the frame**: the gradient runs top to bottom from
    `#00B57F` to `#00FFB3`, 24pt corners at the top and 80 at the bottom. Its
    header controls are 32pt on a 20% white fill, and the send button is 40pt
    rather than 32.
  * **The picker wheels lose the grey selection band.** SwiftUI's `.wheel` paints
    one behind the centre row with no way to turn it off, and the frames have
    none; the selection is marked by the centre row's own weight and colour
    instead. The weight drum is drawn by hand for the same reason — a 40pt centre
    against 24pt neighbours, with kilos, point and grams kept tight together.
  * **The steps pill drops "AVG" on a single day.** Selecting a bar on the Week
    chart or a date on the Month calendar collapses the period to that day, so
    the pill was calling one day's steps an average. It stays an average for a
    whole week or month.
  * The reminders hub's medicine rows show On/Off like the Health and Lifestyle
    rows, reading the medicine's reminder switch rather than its soft-delete flag.

## 0.13.0

* Requires `TreeSDK` 0.12.0 on iOS. One additive Swift property
  (`UserMedicine.remindersEnabled`); no Dart API change.
  * **A reminder switch per medicine.** Its reminder page carries a toggle beside
    the name: off stops the notifications and keeps the times, the frequency and
    the course, so turning it back on resumes them. Distinct from deleting the
    medicine, and distinct from clearing its times — which is the thing the
    switch exists to avoid.
  * **Needs a backend that has `reminders_enabled`.** It is on the b2b's `dev`
    branch. On an environment without it the failure is *silent*: `whitelist:
    true` strips the field from the PATCH and the GET has nothing to return, so
    the app saves, reads back nothing, and shows the switch on again with no
    error. Probe with a PATCH carrying `{"reminders_enabled": true}` and see
    whether the response echoes it.
  * A medicine row written before the field existed reads as **on**, matching the
    backend default — no migration, and no device loses its reminders.
  * Blood sugar, medication and weight take their own Explore carousel glyphs,
    and a workout whose type the build doesn't recognise draws the running
    figure rather than the generic one.

## 0.12.0

* Requires `TreeSDK` 0.11.0 on iOS. One additive Swift method
  (`MedicationReminderScheduling.cancelOrphans(keeping:)`, defaulted, so existing
  conformances compile unchanged); no Dart API change.
  * **A reminder fired for a medicine that was no longer on the list.** Nothing
    swept a medicine's pending notifications when the *medicine* went away —
    deleted on another device, deleted while the app was closed, a course that
    ended, or a different user on the same install. Deleting in the app was
    always fine, which is why it only showed up on a second device. The two
    screens that fetch the whole list now cancel what is left over.
  * **The Nawaf orb covered the chat's own send button.** Its window draws above
    everything, so at its default corner the mascot sat on the button and ate its
    taps — which is why the welcome screen looked like it had no send button. The
    orb hides itself while the chat is up.
  * **Hours and minutes loop on the time wheel** — 12 rolls into 1, 59 into 00,
    rather than stopping dead. The date and AM/PM columns deliberately do not.
  * **The Weight / Water / Carbs period picker is a bottom sheet**, not a system
    menu, with the selection mark at the row's trailing edge.
  * **The metric empty state names the metric** in English ("You haven't logged
    any Weight readings yet!") and stays one sentence in Arabic; the "+" hides
    while a metric tab is empty, since that state already carries its own Log
    button.
  * Nawaf's welcome art, avatar and floating orb are the delivered assets now, and
    the chat card's margins are measured from the safe area rather than the screen.

## 0.11.3

* Requires `TreeSDK` 0.10.3 on iOS. No API change on either side.
  * **The risk indicator is now entirely the backend's.** The SDK used to decide
    both the wording and the ranking of a reading's band from its colour — four
    colour names mapped to three hardcoded words. It renders the band's own
    `recommendation_message` instead, which the API localizes off the `lang`
    header, so a band edited in the catalogue changes the app with no release.
    Ported from the Android SDK rule for rule, so the two stay in step.
  * Severity — which half of a blood-pressure reading speaks for it, and which
    way the arrow points — is measured as distance from the *normal* band,
    found by colour or by wording. Position cannot stand in for it: none of the
    three vitals defines a band below normal, so their normal band is the first
    of three or four, and treating the middle one as normal made 120/80 read
    "low".
  * A risk band with no `min_value` or no `max_value` now decodes as
    open-ended rather than 0. The outermost bands are written that way, and
    reading an absent minimum as zero moved a band's floor — invisible on a
    metric that cannot go negative, but it put the band in the wrong place in
    the sort severity depends on.
  * Nothing is drawn for a value no band covers, or for a band carrying no
    message: where the backend says nothing, the reading reads plain.

## 0.11.2

* Requires `TreeSDK` 0.10.2 on iOS. No API change on either side.
  * **The Explore carousel's status indicator went missing** against a backend
    whose catalogue names its risk bands with hex codes rather than colour
    names. The reading kept its colour and lost the word beside it — Blood
    Sugar drew an orange `185 mg/dL` with nothing next to the time. `riskColor`
    always accepted both formats (the API documents hex; the seeded data sends
    names), but the label and severity lookups took only the names. One rule
    resolves a band's colour now, by hue rather than a table of hex values, so
    a shade the backend tweaks later still lands on the right band. A colour
    with no meaningful hue stays unrecognised — an unclassified reading must
    never read as "Normal".
  * Its quieter half: with every band ranking equal, the worst-band summary
    could describe a `258/80` blood-pressure reading by its diastolic rather
    than its systolic.

## 0.11.1

* Requires `TreeSDK` 0.10.1 on iOS. No API change on either side.
  * **The time wheel read backwards in Arabic.** A system `DatePicker` mirrors
    every one of its columns under RTL, so Arabic showed `ص | 00 | 8` — minutes
    before hours — and pinning the picker LTR only traded that for ص/م on the
    wrong side, there being nothing in between. `TreeTimeWheel` is dru's own
    hand-rolled three-column picker: the view stays RTL and the hour and minute
    columns are *swapped* in Arabic, so the mirroring puts them back in reading
    order. Used by the diary's "Date & time" sheet (with a leading day column),
    the medication custom form and the reminder detail screen.

## 0.11.0

* **New: `TreeSdk.hasPendingReminderTap`.** `onReminderTapped` is a push, so a
  **cold** start misses it — the native side fires it while Dart is still building
  its first widget and nothing is subscribed, leaving a tapped reminder stranded on
  the host's connect screen. Ask this as your app starts and call `start` when it
  answers true; it does not consume the deep link, which `start` still delivers.
  The example host shows the pattern, and guards against the double `start` a cold
  tap would otherwise cause (the second launch had no deep link left to consume, so
  the log screen ended up buried under a second Explore).
* Android: the SDK activity is `singleTop` and re-launched with
  `FLAG_ACTIVITY_SINGLE_TOP`, so a second launch reaches the instance on screen
  instead of stacking another copy; `start` now also guards the window *before*
  the session is established, which two calls could both pass. Kotlin 2.2.20 and
  `compileSdk` 36 / `minSdk` 28 across the plugin and the example, matching the
  partner compatibility matrix as closely as Flutter 3.38.9 allows — its Gradle
  plugin hard-fails below Kotlin 2.2.20, so the matrix's 2.2.0 is unreachable from
  a Flutter host.
* Requires `TreeSDK` 0.10.0 on iOS. The Swift additions are defaulted
  (`MedicationListView.init` gains `onLogged`, `DiaryViewModel` gains
  `loadSubcategoriesIfNeeded()`), so existing callers compile unchanged. A Diary
  and Explore QA round:
  * **A save now returns where the logging started.** A dose or reading logged
    from Explore ends on Explore, and one logged from a Diary tab ends on that
    tab — both used to unwind to the medication list or the metric's tab. A host
    that opens either screen as a destination (Explore's My Health card,
    Reminders, a notification tap) keeps the old unwind.
  * **"View all" on the save toast** brings the Diary forward when the save
    returned to Explore, and lands on All rather than the metric's tab.
  * **Opening a log form from Explore no longer flashes the Diary first** — the
    form is pushed as soon as the subcategories are in, without animation,
    instead of waiting on the entries page and the dose logs.
  * Weight, Water and Carbs share one period dropdown — today / last week /
    last month / last year — and "View chart" is gone from Weight's.
  * HbA1c and blood pressure get a hero card, but still no chart button.
  * The message under the metric chart shows on an empty window too, unbolded.
  * The Reminders medication row is an add action: a plus and its label in
    `accentGreen`, "Add medication".

## 0.10.0

* Requires `TreeSDK` 0.9.0 on iOS. No Dart API change; one additive Swift parameter
  (`MedicationLogsView.init` gains `isVisible`, defaulted, so existing callers compile
  unchanged). A QA round:
  * **The keyboard stayed up** over the chat transcript and behind the history drawer.
    Tapping the body drops it, and both hamburgers dismiss before animating the drawer.
  * **The chat's send button vanished past ~3 wrapped lines** — the input pill was a
    `Capsule`, whose corner radius is half its height, so the growing field's end-caps
    clipped the 40pt icon off the trailing edge. Now a fixed-radius rounded rectangle:
    identical while short, stable while tall.
  * **The Diary's "+" sat under the Nawaf orb**, which draws in its own window over the whole
    app and took the tap. The button is raised clear (112pt, derived from the orb's own
    geometry rather than eyeballed).
  * The target-weight sheet printed a bare `kg` in Arabic — `WeightWheel`'s default was a
    literal, so the sheet bypassed the translated unit. Now **كجم**.
  * Plus the toast, medicine-log and chart-line items from the same round.
* Android still requires `android-tree-sdk` 0.4.0 and is unported for all of it.

## 0.9.0

* Requires `TreeSDK` 0.8.0 on iOS. No Dart API change; two additive Swift members:
  * **A Nawaf answer is no longer lost when the app leaves mid-generation** (QA `Kpco47`).
    Backgrounding or killing the app kills the in-flight `POST /ai-companion/ask`, and the
    client showed Retry for a turn the server had already finished — both messages are
    inserted before that call returns, so the answer was sitting in the session unasked for.
    `recoverPendingReply()` fetches it after a failed send, on the chat appearing, and on
    return to the foreground; it only adopts a transcript that contains the lost question and
    ends on a model message, so a half-written turn still keeps Retry.
  * **The "Log new" sheet draws the design's own order** and no longer follows the home
    arrangement — one property served both, so dragging a row elsewhere re-shuffled which
    metric got the double-height featured card. `defaultOrderSubcategories` is the unarranged
    list the sheet reads.
* Android still requires `android-tree-sdk` 0.4.0 and is unported for both.

## 0.8.8

* Requires `TreeSDK` 0.7.9 on iOS. No Dart or public Swift API change:
  * **HbA1c and Blood Pressure get the hero summary card.** Both tabs used to open straight
    onto the readings list, matching Android and the frames; QA asked for the card, so
    `showsSummary` is now true for every metric. The default card handles both without extra
    work — BP joins its measurements and reads `117/80 mmHg`, HbA1c reads `6 %`.
  * **Neither gets a chart button**, because neither has an insights screen — dru has no chart
    for either metric, and that button is the only route into it. Kept as a separate rule from
    the summary gate, since the two now disagree.
* Android still requires `android-tree-sdk` 0.4.0 and is unported.

## 0.8.7

* Requires `TreeSDK` 0.7.8 on iOS. No Dart or public Swift API change:
  * **The Explore Steps card carries its progress ring**, the old app's
    `confingureStepsCell` — the card already showed the goal on its subtitle line, and this
    is the ring that was noted as unported beside it.
  * The message under the metric chart is no longer bold, matching dru.
* Android still requires `android-tree-sdk` 0.4.0 and is unported.

## 0.8.6

* Requires `TreeSDK` 0.7.7 on iOS. No Dart API change, and no public Swift API change either —
  a QA round of display fixes:
  * **Workout type and its icon.** `WorkoutType.from` used to answer "Walking" for a value it
    did not recognise, so any unknown type was mislabelled; it returns nil now and the metric's
    own name and glyph stand in. The five figure glyphs are the designed assets at all four
    sites, and the All tab titles a workout row by its type rather than "Workout".
  * **"Other" is a workout type**, and the chips are built from the backend's `allowed_values`
    so no build can offer a value the b2b would reject. The catalogue was updated on dev and
    staging; **production still rejects it**.
  * **The orange band reads "Elevated", not "High"** — dru's own vocabulary has no "high" band.
  * **Blood pressure's diastolic markers are tappable**, and the tooltip no longer truncates
    `201/80 mmHg`.
  * The message under the chart is dru's own text, drawn once rather than twice; the Diary's
    tab icons keep their own colours; Carbs, Steps and All use their delivered glyphs; the new
    weight log says "Save" rather than "Update".
* Android still requires `android-tree-sdk` 0.4.0 and is unported for all of it.

## 0.8.5

* Requires `TreeSDK` 0.7.6 on iOS:
  * `cancel` is marked destructive, so it draws red in the OS's action list.
  * `Examples/MedicineNotificationExtension` in the SDK repo is a copy-in notification content
    extension, ported from the old app: it draws the design's card inside the **expanded**
    notification. A Flutter host adds the target to its own `ios/Runner` project — a plugin
    cannot ship one. Note it does **not** put the buttons on a collapsed banner; nothing can.
    Mind the README's `UserNotificationsUI` linkage trap.

## 0.8.4

* Requires `TreeSDK` 0.7.5 on iOS: the medicine alert's three actions take equal thirds, as
  the design draws them.

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
