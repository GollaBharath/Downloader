import json
from yt_dlp import YoutubeDL


def fetch_info(url):
    # Try playlist-aware extraction first and gracefully tolerate unavailable items.
    strategies = [
        {
            "quiet": True,
            "no_warnings": True,
            "skip_download": True,
            "extract_flat": "in_playlist",
            "ignoreerrors": True,
            "lazy_playlist": True,
        },
        {
            "quiet": True,
            "no_warnings": True,
            "skip_download": True,
            "ignoreerrors": True,
        },
    ]

    last_error = None
    for options in strategies:
        try:
            with YoutubeDL(options) as ydl:
                info = ydl.extract_info(url, download=False)
                sanitized = ydl.sanitize_info(info)
                return json.dumps(sanitized)
        except Exception as exc:
            last_error = exc

    raise RuntimeError(f"Unable to fetch media info: {last_error}")


def download_media(url, output_dir, audio_only=False, format_id=None):
    outtmpl = f"{output_dir}/%(title)s.%(ext)s"
    options = {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "outtmpl": outtmpl,
        "restrictfilenames": True,
    }

    if format_id:
        options["format"] = format_id
    elif audio_only:
        # Keep original audio container to avoid requiring ffmpeg conversion.
        options["format"] = "bestaudio[ext=m4a]/bestaudio"
    else:
        # Prefer progressive formats (audio+video in one file) to avoid ffmpeg merge.
        options["format"] = "best[ext=mp4][acodec!=none][vcodec!=none]/best[acodec!=none][vcodec!=none]/best"

    with YoutubeDL(options) as ydl:
        info = ydl.extract_info(url, download=True)
        result = {
            "title": info.get("title"),
            "filepath": ydl.prepare_filename(info),
            "ext": info.get("ext"),
        }
        return json.dumps(result)
