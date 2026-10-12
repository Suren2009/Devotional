# Pradarshana — Temple Pradakshina Counter

Pradarshana helps devotees count the rounds (Pradakshinas) they walk around a temple. Tap **+**
once per round on the phone or on a Wear OS watch, see how many rounds remain, and keep a history
of completed sessions. No account, internet connection, ads or runtime permissions are needed.

## Features

**Phone**
- Home screen with *Start Pradarshana*, *Pradarshana History*, and a *Resume* card while a session is in progress.
- Target entry with validation (must be a whole number from 1 to 100,000). Common counts start with the latest target, then counts used more than once, then 3, 9, 18, 54 and 108. Each number appears once.
- An optional title, filled in with the title of the latest session. A session with no title is shown as **Untitled**.
- The place where the session started, when location is allowed. History and the session details show it, with a map preview and **Show on map**. Counting still works if location is declined.
- Counting screen: large completed count, target, remaining, percentage and progress ring, a large **+** button,
  Pause/Resume, and End Session with a confirmation dialog. Subtle haptic tick on every round, and a gentle
  three-second vibration when the target is reached. The screen stays on while a session is active.
- Automatic completion screen ("Pradarshana Completed!") with final count, target, duration and a gentle radiance animation.
- History, newest first, with date/time, target, completed rounds, duration and status (Completed / Incomplete),
  a detail view per session, delete for one session, and *Clear history*. Both ask for confirmation.
  History is never deleted automatically.
- Settings for a light theme, a dark theme, or the phone’s system theme, in a saffron, cream and muted gold palette.

**Wear OS watch**
- Home: app name, *Start Pradarshana*, and *Resume* when a session exists.
- Target picker with the same common counts as the phone, plus a custom value (on-screen stepper or rotating crown).
- Counting screen for round displays: large count, target and remaining, progress arc, a large **+** button and
  Pause/Resume. *End session* is only offered while paused and asks for confirmation, so a stray touch mid-walk
  cannot end the session.
- Completion screen with a gentle three-second vibration.
- Works stand-alone; it synchronises with the phone whenever they are connected.

Every count is saved immediately, so an active session survives the app being closed or the process being killed
and is reopened automatically on the next launch.

## Project structure

| Module | Contents |
| --- | --- |
| `core:model` | Pure Kotlin. Session model, counter rules and validation, and the conflict-free merge used for sync. |
| `core:data` | Android library shared by both apps. Room database, `SessionRepository`, `SyncManager`, the Wear Data Layer transport and listener service, and the shared `CounterViewModel`. |
| `app` | Phone app (Jetpack Compose, Material 3, Navigation). |
| `wear` | Wear OS app (Compose for Wear OS). |

The architecture is MVVM with a repository layer. Dependencies are wired by a small manual container
(`PradarshanaContainer`) instead of Hilt; with only a handful of objects this avoids annotation processing in both apps.
Each device has its own Room database. Nothing is shared implicitly; all exchange goes through the Data Layer.

## How counting and sync stay correct

- **Transactions.** Every increment, pause, resume, end and completion runs in a Room transaction behind a mutex.
  An increment is rejected if the session is paused, already finished or at its target, so the count can never go
  above the target. The tap that reaches the target marks the session `COMPLETED` and sets its end time in the same
  transaction. This happens only once, and the UI receives a single completion event.
- **Per-device counters (G-Counter CRDT).** A session's count is not stored as one number that devices overwrite.
  Each device keeps its own count (`session_device_counts`), and every local tap is logged with a unique operation
  id `sessionId:deviceId:sequence` (`counter_operations`). The total is the sum of the per-device counts, capped at
  the target. Replicas merge by taking the maximum of each device's entry. As a result:
  - receiving the same update twice, or an old one late, never adds rounds (the merge is idempotent);
  - taps made on the phone and the watch at the same time are both kept (the merge is commutative);
  - receiving a sync message never increments anything by itself.
- **Status.** Pause and resume use last-writer-wins on `(statusUpdatedAt, deviceId)`. Ended and completed are final.
  Reaching the target on the merged count always means Completed; for example, if both devices count the last
  round while offline, the merged session completes once, at the target.
- **One session in progress.** Starting a new session ends the previous one as incomplete. If both devices start a
  session while disconnected, the one started most recently wins on both devices after they reconnect.
- **Transport.** Each device publishes the full state of a session as a `DataItem` at `/pradarshana/session/<id>`.
  The Wear OS `DataClient` stores the item and delivers the latest version when the Bluetooth (or cloud) link
  comes back, which provides the offline queue. A local `local_version` / `synced_version` pair records changes that
  could not even be handed to the Data Layer (for example, Play services unavailable); these are retried on app start
  and resume. `PradarshanaWearableListenerService` receives updates while the app is closed. A full reconcile
  (`getDataItems` plus a push of pending changes) runs on every app start and resume.
- **Clearing history** removes finished sessions from that device and its own published items, and records the time
  of the clear so that older finished sessions still held by the other device are not imported again.

## Requirements

- JDK 17 or newer (built and tested with JDK 21)
- Android SDK with platform 35 and build-tools 35
- Android Studio Ladybug (2024.2) or newer, or the Gradle wrapper on the command line

Versions: AGP 8.7.3, Gradle 8.11.1, Kotlin 2.0.21, Compose BOM 2024.12.01, Wear Compose 1.4.0, Room 2.6.1,
play-services-wearable 18.2.0. Phone `minSdk` 26, watch `minSdk` 30 (Wear OS 3+).

## Build

```bash
# Point Gradle at your SDK if ANDROID_HOME is not set
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew :app:assembleDebug     # phone APK  -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :wear:assembleDebug    # watch APK  -> wear/build/outputs/apk/debug/wear-debug.apk
```

## Install

```bash
adb devices                                    # list phone and watch serials
adb -s <phone-serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <watch-serial> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

To install on a physical watch, enable *Developer options → ADB debugging* and *Debug over Wi-Fi* on the watch,
then run `adb connect <watch-ip>:5555` (or use *Wireless debugging* pairing on Wear OS 4+).

## Pairing the phone and watch

1. Pair the watch with the phone using the **Wear OS** / **Pixel Watch** / **Galaxy Wearable** companion app.
2. Install **both** APKs from the same build machine. The Data Layer only connects apps that share the same
   application id (`com.pradarshana.app`) **and** the same signing certificate. Debug builds made on one machine
   share its debug keystore. For release builds, sign both APKs with the same key.
3. Open Pradarshana on either device. Sessions and counts sync automatically whenever the devices are connected.

**Emulators.** Create a phone AVD with a *Google Play* system image and a Wear OS AVD. In Android Studio, use
*Device Manager → Pair Wearable*. On the command line, install the Wear OS companion app on the phone emulator,
then run `adb -s <phone-serial> forward tcp:5601 tcp:5601` and pair from the companion app.

## Tests

```bash
./gradlew test                    # all JVM tests: core:model, core:data, app and wear (Robolectric)
./gradlew connectedAndroidTest    # instrumentation tests on a connected device or emulator
./gradlew :app:testDebugUnitTest :wear:testDebugUnitTest -Pscreenshots
                                  # also render the Compose screens to PNG under build/screenshots
```

| Area | Tests |
| --- | --- |
| Counter rules: valid targets; rejecting zero, negative, empty and non-numeric targets; remaining and progress; never exceeding the target | `core/model/.../CounterRulesTest` |
| Sync merge: idempotent, order-independent, concurrent increments, stale updates, overshoot capping, single completion, sticky cancellation | `core/model/.../SessionMergerTest` |
| Repository: start, increment, concurrent taps, completion once, pause, end, history order, clear history, restore after process death, persisted history | `core/data/.../SessionRepositoryTest` |
| Phone and watch sync over a simulated Data Layer: phone→watch and watch→phone updates, duplicate deliveries, alternating taps, offline counting and catch-up, failed publishes retried, concurrent offline edits, overshoot while disconnected, a session counted entirely on the watch, ending on one device, two sessions started offline, cleared history not re-imported | `core/data/.../sync/PhoneWatchSyncTest` |
| Data Layer encoding | `core/data/.../sync/SnapshotCodecTest` |
| Phone UI (Compose on Robolectric): counter, remaining and progress, pause, completion navigation happening once, end confirmation, target validation, resume card, history list and clear confirmation | `app/src/test/...` |
| Watch UI (Compose on Robolectric, round 227dp): counting, completion entirely on the watch, pause and end confirmation, resume, completion summary | `wear/src/test/...` |
| Instrumentation: full phone flow (start, count, complete, history) and Room schema validation | `app/src/androidTest`, `core/data/src/androidTest` |

The sync tests use `FakeDataLayer`, which behaves like `DataClient`: it keeps the latest item per session per node,
holds items while the link is down, and delivers the newest version on reconnect.

## What was verified here

`./gradlew test` passes for every module (domain rules, repository, simulated phone–watch sync, and Robolectric Compose
UI tests for the phone and a 227dp round watch).

On an Android 14 phone emulator, `connectedDebugAndroidTest` passed for:

- `DatabaseSchemaTest` — the exported Room schema opens and keeps a completed session.
- `PradarshanaFlowTest` — start a session with target 3, tap **+** three times, land on **Pradarshana Completed!**, then open history and see the completed session.

KVM is present on this machine but creating a virtual CPU faults (`kvm_spurious_fault`), so the emulator ran with
software CPU emulation. That is slow enough that the system UI itself shows "isn't responding" dialogs, which blocked
an interactive tap-through after the tests. The instrumentation test still drove the real app end to end.

## Not verified on real hardware

- Bluetooth Data Layer delivery between a paired phone and watch, including a real disconnect and reconnect. The sync
  rules are covered by `PhoneWatchSyncTest` against a store-and-forward fake of `DataClient`, but Google Play services
  was not used as the transport.
- A Wear OS emulator or physical watch. The watch UI was rendered and clicked under Robolectric on a round 227dp display.
- How the haptic feedback feels, and readability in sunlight.
- Small (around 192dp) round watches and square watches.
