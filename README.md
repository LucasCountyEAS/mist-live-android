# Mist Live - Android TV Client

An unofficial Android TV client for [Mist Live](https://mistlive.tv/), letting viewers browse and watch live streams directly on their TV.

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/3fc1f3fb-1ae7-4162-bee9-b2cf4d7b3434" />


## Installing Mist Live on Your Android TV

Since this isn't on the Google Play Store, you'll need to install it manually. This takes a few minutes and only needs to be done once. **Updates will also have to be installed manually.**

### Step 1: Enable Unknown Sources

1. On your Android TV, go to **Settings → Device Preferences → Security & Restrictions** (the exact path varies slightly by device/launcher).
2. Enable **Unknown Sources** (sometimes called **Install unknown apps**) for whichever app you'll use to open the APK — for example, a file manager or Downloader app.

> Some Android TV boxes may already allow this, or ask you to confirm the first time you try installing an APK.

### Step 2: Get the app file

Download the latest `MistLive.apk` from the [Releases](../../releases) page.

### Step 3: Install it on your TV

There are a couple of ways to get the APK onto your device:

**Option A: Sideload with a file manager app**
1. Install a file manager app from the Play Store on your Android TV.
2. Use it to download `MistLive.apk` directly, or transfer it via USB drive.
3. Open the file and select **Install**.

**Option B: Install via ADB**
1. On your Android TV, enable **Developer Options** (Settings → Device Preferences → About → click **Build** 7 times) and turn on **USB Debugging** / **Network Debugging**.
2. Find your TV's IP address under **Settings → Network**.
3. From a computer on the same Wi-Fi network:
   ```bash
   adb connect <tv-ip-address>
   adb install MistLive.apk
   ```

Either way, once installed, Mist Live will show up on your Android TV home screen (or in your apps list) ready to launch.

### If something goes wrong

- **Can't install / "app not installed" error:** Double check Unknown Sources is enabled, and that you downloaded the correct APK for your device's architecture if prompted.
- **Can't connect via ADB:** Make sure your computer and TV are on the same Wi-Fi network, and that Network Debugging is turned on.
- **Install fails or app crashes:** Open an issue on this repo (or contact me directly) with a description of what happened. A screenshot or photo of the error helps.

## Development

This app is built with Kotlin, the AndroidX Leanback library, and ExoPlayer/Media3 for playback. To work on it yourself:

1. Clone this repo
2. Open the `MistTV` folder in Android Studio
3. Let Gradle sync, then run the app on an Android TV emulator or a real device in Developer Mode

Channels are fetched live from the Mist Live API rather than hardcoded, so no channel-list editing is needed to get started.

## License

See [LICENSE](./LICENSE) for details.

## Disclaimer

This is an independent, unofficial project. It is not affiliated with or endorsed by Google or Android TV. Streams and channel content are provided by Mist Weather Media; this project only provides the Android TV client for browsing and viewing them.

## Contact

Questions or issues:
[Rukasu Development Server](https://discord.gg/3KvxYqpYdF) on Discord or
[lucascountyeas+mist@gmail.com](mailto:lucascountyeas+mist@gmail.com)
