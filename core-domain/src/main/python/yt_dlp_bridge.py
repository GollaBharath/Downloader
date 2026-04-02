import json
from yt_dlp import YoutubeDL


def fetch_info(url):
    options = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "extract_flat": "in_playlist",
    }
    with YoutubeDL(options) as ydl:
        info = ydl.extract_info(url, download=False)
        sanitized = ydl.sanitize_info(info)
        return json.dumps(sanitized)
