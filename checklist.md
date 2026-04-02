# Downloader App Build Checklist (Claude Execution Guide)

Use this as the single source of truth while implementing the Android APK.
Complete items in order. Do not skip validation gates.

## 0) Guardrails Before Coding

- [x] Project type is Android app only (Kotlin + Jetpack Compose), no server code.
- [x] Build target is APK-first.
- [ ] App works fully offline except network access to target media URLs.
- [ ] All download execution is on-device via bundled binaries.
- [x] Keep code modular and testable; avoid putting logic directly in UI screens.

Exit criteria:

- [ ] Architecture and constraints are documented in README section "Implementation Rules".

## 1) Project Bootstrap

- [x] Create Android project with Kotlin and Compose enabled.
- [x] Set minSdk >= 26.
- [x] Configure Gradle Kotlin DSL and stable dependency versions.
- [ ] Add modules:
  - [x] app
  - [x] core-domain
  - [x] core-data
  - [x] downloader-engine
- [x] Add DI framework (Hilt preferred).
- [ ] Add basic logging strategy (tagged logs + release-safe behavior).

Exit criteria:

- [x] Clean build succeeds.
- [x] App launches to a placeholder Home screen.

## 2) Domain Models and Contracts

- [ ] Define domain models:
  - [x] DownloadJob
  - [ ] DownloadItem
  - [x] DownloadProgress
  - [x] AppSetting
- [x] Define enums/states:
  - [x] queued
  - [x] running
  - [x] succeeded
  - [x] failed
  - [x] cancelled
- [x] Define repository interfaces in core-domain.
- [x] Define use cases for:
  - [x] enqueue download
  - [x] start next queued
  - [x] cancel active download
  - [x] retry failed download
  - [x] stream progress updates

Exit criteria:

- [ ] Domain module has no Android framework dependency.

## 3) Local Persistence (Room)

- [ ] Create Room entities and DAOs for jobs, items, and settings.
- [x] Add mappers between entity and domain model.
- [x] Implement repository interfaces in core-data.
- [ ] Add migration strategy from schema version 1 onward.
- [ ] Add tests for DAO operations and state transitions.

Exit criteria:

- [ ] Can create, update, query, and observe job lifecycle in local DB.

## 4) Bundle yt-dlp and ffmpeg

- [x] Decide binary packaging layout per ABI (arm64-v8a mandatory).
- [x] Add binary files to project in appropriate locations.
- [ ] On first app run:
  - [x] Copy binaries to app-internal executable directory.
  - [x] Mark binaries executable.
  - [ ] Verify presence + checksum/version.
- [ ] Persist installed binary metadata (version/hash/date).

Exit criteria:

- [ ] App can execute `yt-dlp --version` and `ffmpeg -version` successfully on-device.

## 5) Execution Engine

- [x] Build YtDlpCommandBuilder:
  - [x] URL input
  - [x] output template
  - [x] selected format/audio/video options
  - [x] subtitle/thumbnail/postprocess flags
- [x] Build ProcessRunner:
  - [x] launch process
  - [x] stream stdout/stderr lines
  - [x] surface exit code and errors
  - [x] support cancellation/kill
- [x] Implement parser for yt-dlp progress lines:
  - [x] percent
  - [x] speed
  - [x] ETA
  - [x] phase (downloading/postprocessing/merging)

Exit criteria:

- [ ] A single command run can be started, observed, cancelled, and finalized reliably.

## 6) Download Orchestration Runtime

- [x] Implement queue policy: one active, N pending.
- [x] Use Foreground Service for active work.
- [ ] Use WorkManager for scheduling/retry coordination.
- [x] Wire orchestration to Room-backed state machine.
- [ ] Ensure process resume behavior after app process death (recover queued/running state safely).

Exit criteria:

- [ ] Jobs move through valid state transitions only.
- [ ] Active job continues in background with foreground notification.

## 7) Notification Layer

- [x] Add persistent foreground notification for active download.
- [ ] Show title, progress percent, speed/ETA if available.
- [x] Add cancel action button in notification.
- [ ] Tap action opens app to Downloads screen.

Exit criteria:

- [ ] Notification updates in near-real-time and cancellation works from notification.

## 8) Storage and SAF

- [x] Implement folder picker using SAF tree URI.
- [x] Persist URI permissions.
- [ ] Write downloaded output only under granted tree.
- [ ] Handle filename sanitization and collision policy.
- [ ] Preflight free-space check before starting download.

Exit criteria:

- [ ] User-selected folder persists across restarts and files are written correctly.

## 9) UI Screens (Compose)

### 9.1 Home

- [x] URL input field
- [x] Paste from clipboard action
- [x] Receive share intent URL
- [x] Validate URL and show immediate errors

### 9.2 Media Info

- [x] Fetch metadata via yt-dlp (title, duration, thumbnail)
- [x] Display metadata loading/error/success states

### 9.3 Format Picker

- [ ] Video/audio tab split
- [ ] Show quality, codec, estimated size
- [ ] Select one target format profile

### 9.4 Downloads Queue

- [x] Show queued/running jobs
- [ ] Live progress rows
- [x] Cancel and retry actions

### 9.5 History/Files

- [ ] Show succeeded/failed entries
- [ ] Open file/share file actions

### 9.6 Settings

- [ ] Download folder chooser
- [ ] Default mode toggles (audio-only, subtitles, filename template)

Exit criteria:

- [ ] End-to-end flow works: paste URL -> choose format -> enqueue -> track -> complete.

## 10) Error Handling and Recovery

- [ ] Normalize common failures into user-friendly messages:
  - [ ] network unavailable
  - [ ] unsupported URL/extractor issue
  - [ ] permission denied
  - [ ] no storage space
  - [ ] ffmpeg merge failure
  - [ ] binary missing/corrupt
- [ ] Store detailed raw errors in logs for debugging.
- [ ] Provide retry where appropriate.

Exit criteria:

- [ ] No silent failures; each failed job has clear reason and next action.

## 11) Quality Gates (Must Pass)

- [ ] Unit tests for command builder and progress parser.
- [ ] Unit tests for state machine transitions.
- [ ] Instrumented tests for critical user flows.
- [ ] Manual test matrix includes:
  - [ ] app foreground/background transitions
  - [ ] cancel during active download
  - [ ] network drop during download
  - [ ] process death and recovery
  - [ ] long file names and unusual characters
- [ ] Validate performance on low-memory device profile.

Exit criteria:

- [ ] Critical flow pass rate is 100% on test matrix.

## 12) Packaging and Release APK

- [x] Configure release build type.
- [x] Enable minification/obfuscation for app code only.
- [ ] Ensure bundled binaries remain executable and not stripped incorrectly.
- [ ] Add in-app About page with license attributions.
- [ ] Generate signed release APK.

Exit criteria:

- [ ] Release APK installs and executes full download flow successfully.

## 13) Definition of Done (Global)

- [ ] User can paste/share URL and fetch metadata.
- [ ] User can choose format and enqueue download.
- [ ] Progress, speed, and ETA update while app is backgrounded.
- [x] User can cancel and retry.
- [ ] Output reliably saves to chosen SAF folder.
- [ ] History persists across app restarts.
- [ ] No backend dependency exists.

## 14) Optional Post-MVP (Do Not Block MVP)

- [ ] Parallel downloads with configurable limit.
- [ ] Playlist batch queue controls.
- [ ] SponsorBlock/chapter options.
- [ ] In-app binary update mechanism.
- [ ] Play Store compliance mode (future profile/flags).
