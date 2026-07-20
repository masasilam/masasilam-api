package com.naskah.app.service.film.video;

public enum VideoProviderType {
    YOUTUBE,        // youtube.com / youtu.be
    ARCHIVE_ORG,    // archive.org — domain publik, direct MP4
    VIMEO,          // vimeo.com — oEmbed
    DAILYMOTION,    // dailymotion.com — oEmbed
    WIKIMEDIA,      // commons.wikimedia.org / upload.wikimedia.org
    HLS,            // .m3u8 — adaptive bitrate via hls.js di frontend
    DASH,           // .mpd — adaptive bitrate via dash.js di frontend
    DIRECT_URL,     // MP4/WebM langsung tanpa provider khusus
    UNKNOWN         // fallback jika tidak ada provider yang cocok
}