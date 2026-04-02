# Playlist Auto-Detection & Download Feature

This implementation plan details exactly how we will update the internal engine to automatically detect and parse playlists, and update the UI/ViewModel to batch queue all videos found instead of just extracting a single item or failing.

## Proposed Changes

### Background Engine Updates

#### [MODIFY] `app/src/main/python/yt_dlp_bridge.py`

- Remove the `"noplaylist": True` hardcoded constraint in `fetch_info(url)`.
- Introduce `"extract_flat": "in_playlist"` to allow fast fetching of all playlist metadata without heavily delaying the UI thread by extracting hundreds of individual detailed formats immediately.

#### [MODIFY] `core-domain/src/main/java/com/downloader/core/domain/model/Models.kt`

- Add `isPlaylist: Boolean = false` and `playlistEntries: List<MediaInfo>? = null` to the `MediaInfo` model so it can store a recursive representation of queued items.

#### [MODIFY] `core-data/src/main/java/com/downloader/core/data/repository/YtDlpServiceImpl.kt`

- Update the JSON parsing block for `MediaInfo` to check if `_type == "playlist"`.
- If true, parse the `"entries"` JSON Array into a `List<MediaInfo>` and attach it to the parent `MediaInfo`.

### View Model and Queue Flow

#### [MODIFY] `app/src/main/java/com/downloader/app/ui/MainViewModel.kt`

- Overhaul `enqueueDownload(audioOnly: Boolean)`.
- If the current `mediaInfo` is a playlist containing valid `playlistEntries`, the method will loop over all entries.
- For each entry, create a separate `DownloadJob` using its standalone URL and Title, assigning a fresh randomized UUID `jobId` for each.
- Inform the user that `N` items were queued.

### UI Enhancements

#### [MODIFY] `app/src/main/java/com/downloader/app/ui/screens/HomeScreen.kt`

- In the `MediaInfo` Card, display an extra label if a playlist is detected: e.g., "Playlist Detected: X Videos".
- The buttons "Download Audio" and "Download Video" remain the same but will naturally trigger the new batched workflow internally.

## Open Questions

> [!IMPORTANT]
> Because playlists can contain huge numbers of videos, the fast extraction (`extract_flat`) doesn't return the exact final duration/format options until each individual download starts processing inside the engine. Therefore, the "Total Size" or "Total Duration" might not be populated instantly on the Home Screen. Is this acceptable?

> yes

## Verification Plan

1. Paste a single regular YouTube link -> verify we extract exactly 1 item and create 1 job.
2. Paste a YouTube Playlist link -> verify `yt-dlp` returns a playlist object, UI updates to notify size, and hitting Download pushes `N` separate jobs into the `JobsScreen`.
