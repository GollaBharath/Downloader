**Build Strategy**

1. Ship one Android app (Kotlin + Compose), no backend.
2. Bundle yt-dlp + ffmpeg binaries inside the APK (per CPU architecture).
3. Run downloads in a foreground service so jobs survive app backgrounding.
4. Store job state and history locally in Room.
5. Target reliability first, then polish.

**Phase 0: Decisions (1-2 days)**

1. Min SDK target (recommend 26+).
2. Supported ABIs for v1:
3. arm64-v8a required.
4. Optionally armeabi-v7a for older devices.
5. Output strategy:
6. Ask user to pick a root download folder via SAF.
7. App writes only under that granted tree.
8. v1 scope:
9. URL paste/share
10. metadata preview
11. format select
12. queue + progress + cancel
13. basic history

**Phase 1: App Skeleton (2-3 days)**

1. Create modules:
2. app (UI + DI + navigation)
3. core-domain (use cases + models)
4. core-data (Room + repositories)
5. downloader-engine (process exec, parsing, binary handling)
6. Add dependency injection (Hilt or Koin).
7. Set up Room entities:
8. DownloadJob
9. DownloadItem
10. AppSetting
11. Add app-wide logging and crash-safe error mapping.

**Phase 2: Bundle and Execute Binaries (4-6 days)**

1. Package yt-dlp and ffmpeg under native libs or assets.
2. On first run:
3. copy binaries to app-internal files dir
4. mark executable
5. verify version/checksum
6. Build a YtDlpCommandBuilder:
7. input URL
8. output template
9. selected format/audio options
10. subtitle/thumbnail flags
11. Create ProcessRunner:
12. starts process
13. streams stdout/stderr
14. supports cancel
15. Parse yt-dlp progress lines into structured events:
16. percent
17. speed
18. ETA
19. phase (downloading, postprocessing, merging)

**Phase 3: Download Runtime (4-5 days)**

1. ForegroundService controls active download workers.
2. WorkManager handles queued/retry behavior.
3. Implement queue policy:
4. v1 simplest: one active, N pending
5. Add notifications:
6. active progress
7. tap to open app
8. cancel action
9. Persist state transitions:
10. queued
11. running
12. succeeded
13. failed
14. cancelled

**Phase 4: UI Flow (5-7 days)**

1. Screen 1: Home
2. paste URL
3. quick actions from clipboard/share intent
4. Screen 2: Media info
5. title, duration, thumbnail
6. fetch formats
7. Screen 3: Format picker
8. video/audio tabs
9. size and codec display
10. Screen 4: Queue/Downloads
11. live progress rows
12. cancel/retry
13. Screen 5: History/Files
14. completed items
15. open location/share file
16. Settings:
17. download folder picker
18. defaults (audio-only, subtitle, naming template)

**Phase 5: Android Storage + Permissions Hardening (2-3 days)**

1. Use SAF tree URI and persisted permissions.
2. Handle Android 13+ media nuances.
3. Validate free-space checks before enqueue.
4. Handle filename sanitization and duplicates.

**Phase 6: Reliability and QA (4-6 days)**

1. Device matrix:
2. at least one low-end and one modern device
3. Test cases:
4. app background/foreground transitions
5. process kill + recovery
6. network drop/reconnect
7. long filename/unicode URL edge cases
8. ffmpeg merge failures
9. Add instrumentation for top 10 critical flows.

**Phase 7: APK Packaging and Release (2-3 days)**

1. Build release flavor with minified app code.
2. Keep native binaries intact and verified.
3. Add in-app About + licenses screen.
4. Sign and generate release APK.
5. Set up simple versioning and changelog discipline.

**MVP Success Criteria**

1. User can paste URL, pick format, download successfully on-device.
2. Progress and cancel are accurate.
3. File lands in user-selected folder reliably.
4. Failed jobs are understandable and retryable.
5. No server dependency.

**Post-MVP Enhancements**

1. Parallel downloads with bandwidth cap.
2. Playlist support with batch controls.
3. SponsorBlock/chapter options.
4. In-app binary update mechanism.
5. Play Store compliance mode toggle (future).

**Recommended 4-Week Sprint Outline**

1. Week 1: skeleton + binary execution proof.
2. Week 2: queue/service/progress pipeline.
3. Week 3: full UI flow + storage hardening.
4. Week 4: QA pass + release APK.
