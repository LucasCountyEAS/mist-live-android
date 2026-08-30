# Mist TV — Android TV Streaming Guide

A polished Android TV app built with Kotlin + Leanback + ExoPlayer/Media3.
Displays your channels as a TV Guide grid with logos, names, and descriptions.
Selecting a channel launches a fullscreen RTMP/HLS/DASH player.

---

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (bundled with Android Studio)
- Android TV device or emulator (API 21+)

---

## Quick Start

1. **Open** the project in Android Studio:
   `File → Open → select the MistTV folder`

2. **Add your channels** in:
   `app/src/main/java/com/mist/streaming/data/ChannelRepository.kt`

   Each channel needs:
   ```kotlin
   Channel(
       id           = "unique_id",
       name         = "Channel Name",
       description  = "Short description shown on the card",
       thumbnailUrl = "https://yourcdn.com/thumbnail.png", // Image shown in the guide grid
       logoUrl      = "https://yourcdn.com/logo.png",      // Logo shown in the player overlay
       streamUrl    = "rtmp://your-server/live/key",       // RTMP, HLS (.m3u8), or DASH (.mpd)
       category     = "News",                               // Groups channels into rows
       isLive       = true                                  // Shows red LIVE badge in player
   )
   ```

3. **Build the APK**:
   `Build → Build Bundle(s) / APK(s) → Build APK(s)`

4. **Install on your TV** via ADB:
   ```bash
   adb connect <tv-ip-address>
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Stream URL Formats

| Format | Example URL                                      |
|--------|--------------------------------------------------|
| RTMP   | `rtmp://your-server/live/stream-key`             |
| HLS    | `https://your-server/hls/stream.m3u8`            |
| DASH   | `https://your-server/dash/manifest.mpd`          |

> **Note**: The app requires `android:usesCleartextTraffic="true"` for plain `rtmp://`
> and `http://` URLs. For production, use TLS (`rtmps://` or `https://`).

---

## Project Structure

```
app/src/main/
├── java/com/mist/streaming/
│   ├── MainActivity.kt           — Entry point, hosts BrowseFragment
│   ├── BrowseFragment.kt         — TV Guide grid (Leanback BrowseSupportFragment)
│   ├── PlaybackActivity.kt       — Fullscreen player with channel overlay
│   ├── data/
│   │   ├── Channel.kt            — Channel data model
│   │   └── ChannelRepository.kt  — Your channel list (edit this!)
│   └── ui/
│       └── ChannelCardPresenter.kt — Leanback card renderer with Glide thumbnails
├── res/
│   ├── drawable/
│   │   ├── mist_banner.png        — 320×180 TV launcher banner
│   │   ├── placeholder_channel.png — Fallback logo
│   │   ├── overlay_gradient.xml   — Player info bar gradient
│   │   └── live_badge_bg.xml      — Red LIVE badge shape
│   ├── layout/
│   │   ├── activity_main.xml      — Main container
│   │   └── activity_playback.xml  — Player + overlay layout
│   └── values/
│       ├── colors.xml             — Mist dark blue color palette
│       ├── strings.xml            — App strings
│       └── themes.xml             — Leanback-based TV themes
└── AndroidManifest.xml
```

---

## Customization

### Branding
- Replace `res/drawable/mist_banner.png` with your own 320×180 banner
- Replace `res/mipmap-hdpi/ic_launcher.png` with your own icon
- Adjust colors in `res/values/colors.xml`

### Channel Logo Hosting
- Use any CDN (Cloudflare R2, AWS S3, Bunny CDN, etc.)
- Recommended size: **300×300px PNG** with transparent or dark background
- Local drawables also work: `thumbnailUrl = ""` + load from `R.drawable.your_logo` in the presenter

### Adding Features
- **EPG/Schedule**: Fetch JSON from your API and add a "Now On" field to `Channel`
- **Favorites**: Use `SharedPreferences` or Room DB to save favorite channel IDs
- **Channel Search**: Add `SearchSupportFragment` (Leanback has this built-in)
- **Authentication**: Add a login screen before `MainActivity` for gated content

---

## D-Pad Controls

| Key         | Action                          |
|-------------|---------------------------------|
| D-pad       | Navigate channel guide          |
| Select/OK   | Play selected channel / toggle overlay |
| Back        | Return to guide from player     |
| Left arrow  | Switch between categories       |

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Stream won't play | Check `streamUrl` is reachable from the TV's network |
| Logos not loading | Verify `thumbnailUrl` and `logoUrl` are publicly accessible HTTPS URLs |
| App not on TV home | Ensure `LEANBACK_LAUNCHER` intent-filter is in manifest |
| Cleartext error | Add `android:usesCleartextTraffic="true"` to `<application>` (already set) |
| RTMP buffering | Increase ExoPlayer buffer settings or use HLS instead |
