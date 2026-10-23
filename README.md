<div align="center">

<img src="docs/assets/logo.png" width="128" alt="AEmulator X logo"/>

# AEmulator X

**A fork of [AEmulator Plus](https://github.com/somerandomusernam2/AEmulator-Plus) with a cooler UI and screen recording.**

<sub>App icon designed by Chris (donkris01) 💜</sub>

</div>

---

AEmulator X boots a real Android 2.3–7.x system image straight from a firmware file you already have, exactly like AEmulator Plus, and adds:

## ✨ New in X

- **🎬 Screen recording**: ⋮ menu → **Record screen**. Records the guest screen as H.264 video with the guest's own sound (AAC), 30 fps, sharp even on old 320×480 screens (scaled to 640–1280 px). A pulsing **REC** pill shows the time; tap it (or ⋮ → **Stop recording**) to save to `Movies/AEmulator/<time>-<VM>.mp4`. Works in both GPU-bridge and framebuffer modes, and in recovery. Only the guest screen is recorded, not the host's bars or menus.
- **🌌 Cooler UI**: an electric violet / magenta / cyan palette with a deep near-black dark mode, an animated aurora background behind the library, a glowing gradient **X** wordmark, and glass firmware cards with gradient release badges and Start buttons.
- **🔔 Boot sound picker** (X2): VM settings → **Boot sound**. Choose the firmware's own sound, silence, one of three original AEmulator X sounds (X Chime, Retro 8-bit, Rise), any sound file found inside the firmware (PowerOn.wav, UI/notification/ringtone sounds…), or your own audio file. Every option has a ▶ preview. A chosen sound replaces the firmware's boot sound: the system stays muted until it has booted.
- **🏷️ Firmware badges** (X32): little tags on tiles and cards: ✨ New, 💤 Not booted, 🥇 Top (most played), 💪 64-bit, ⚡ Fast, 🏛️ Classic, 🧩 Container.
- **🎞️ Slideshow** (X32): the library's ⋮ › **Slideshow**: a calm screensaver of your screenshots, covers and firmware wallpapers, slowly zooming, with each firmware's name and year.
- **✨ Screenshot stickers** (X31): long-press a screenshot in **Screenshots & recordings** › **Decorate**: emoji stickers and text, drag to move, pinch to resize and turn; save as a new copy or share.
- **🖼️ Classic wallpapers** (X30): the library's ⋮ › **Classic wallpapers**: the original wallpapers found inside your own firmwares, by firmware; use one behind the library, set it on your device or save it.
- **🇮🇱 Hebrew** (X30): every AEmulator X text and voice command in עברית, right to left.
- **🎬 Boot logo creator** (X29): a firmware's settings › **Boot animation** › **Create your own…**: your word, a line under it, colours, background, entrance (zoom, fade, slide, typewriter) and your own picture, with a live preview.
- **🌍 Polish, French and German** (X29): every AEmulator X text and voice command in Polski, Français and Deutsch.
- **🎨 App icons** (X28): Settings › Appearance › **App icon**: Classic, Neon, Gold, Retro, Christmas or Mono versions of Chris's icon on your home screen.
- **⏰ Play timer** (X28): a running system's ⋮ › **Play timer…**: 15 to 90 minutes, a small countdown, a heads-up 5 minutes before the end and a friendly "time's up" (it never closes anything).
- **🔍 Firmware info** (X28): ⓘ on a card or **Info** in a tile's menu: Android version, 32/64-bit, screen, size, boots, play time, collections, notes and the famous phone with the same Android.
- **📁 Collections** (X27): chips at the top of the library: **+ New collection** to make folders like "Games" or "Old ones", tap one to show only it, long-press to edit or delete it.
- **🎁 AEmulator Wrapped** (X27): the library's ⋮ › **Your Wrapped**: a colourful story of your play time, most played firmware, favourite Android era, boots, screenshots, streaks and badges, with a Share button.
- **🎮 Gamepad editor** (X25): a running system's ⋮ › **Edit gamepad…**: drag the D-pad, A/B/X/Y and Select/Start anywhere, resize each group, set how see-through they are, add L/R buttons; saved per firmware.
- **🏛️ Device museum** (X25): the library's ⋮ › **Device museum**: 15 famous Android phones and tablets (T-Mobile G1 to Pixel 3), drawn in code, with year, maker, Android version and a fun fact; boot a matching firmware from there.
- **🖼️ Screenshot frames** (X24): a running system's ⋮ › **Screenshot frame…**: screenshots inside a phone, or as a Polaroid photo with the firmware name and date.
- **📝 Firmware notes** (X24): long-press a tile (or ✏️ on a card) › **Name and notes** to write your own notes; they show on the tile.
- **🏆 Daily challenges** (X23): a card in the library with one small goal a day (screenshots, play time, booting a certain Android version, Dessert Snake…), a progress bar, confetti when it is done and a 🔥 streak.
- **🎂 Android birthdays** (X23): on the release day of an Android version the library throws a party (confetti, a tune, a button to boot it if you have it); the challenge card shows the next birthday.
- **🔊 Sound effects** (X23): small sounds made in code for starting a firmware, screenshots, badges, challenges, birthdays and Dessert Snake; off with Settings › Appearance › **Sound effects**.
- **🎨 Theme maker** (X22): Settings › Appearance › **My theme**: pick the main colour and three glow colours with a live preview; **Share** sends the theme as a short code, **Paste code** uses a friend's.
- **⏪ Instant replay** (X22): a running system's ⋮ › **Instant replay** plays back the last 15 seconds of the screen (pause, scrub, save as GIF). Only the picture goes back; the running system itself cannot be rewound.
- **🎙️ Voice commands** (X22): ⋮ › **Voice command**, then say "screenshot", "home", "back", "recents", "volume up/down", "replay" or "gif" (English, Russian, Portuguese, Spanish).
- **✍️ Gesture shortcuts** (X22): ⋮ › **Draw a shortcut**: O Home, < Back, ^ Recents, S Screenshot, ✓ Instant replay, Z GIF.
- **🎵 Music player** (X22): the library's ⋮ › **Music player**: your own songs with a mini player bar; it pauses when a system starts.
- **📅 Firmware of the day** (X22): a card at the top of the library suggests one of your firmwares each day, with a fun fact about its Android version.
- **🐍 Dessert Snake** (X21): a secret game: tap the logo in Settings › About 7 times. Eat every Android dessert from Cupcake to Pie; swipe, arrow keys, WASD or a controller. Eat all 14 in one game for the 🐍 Dessert lover badge.
- **⌨️ Key mapping** (X20): a running system's ⋮ › **Key mapping…**: tap a spot and press a key (keyboard or controller) to make that key tap there, or place a WASD / arrow-key joystick; per firmware, switchable with ⋮ › **Use key mapping**.
- **🖥️ External display** (X20): with a TV or monitor connected (HDMI, USB-C, wireless display), ⋮ › **External display** shows the running system full-size there while the phone stays the controller.
- **📊 Statistics** (X19): library ⋮ › **Statistics**: total play time, days in a row, best streak, the last 7 days, most played firmwares and play time per Android version.
- **🖼️ Library wallpaper** (X19): App settings › Appearance › **Library wallpaper**: your own picture behind the library.
- **🎨 Screen filters** (X19): a running system's ⋮ › **Screen filter**: Retro CRT, Game Boy green, Sepia, Warm night or Black & white (Phone GPU off), per firmware.
- **🖐️ Touch macros** (X19): ⋮ › **Record touch macro**, then **Play macro** once or in a loop.
- **🎞️ GIF recording** (X19): ⋮ › **Record GIF (8 s)** saves a short looping GIF next to the screenshots.
- **📋 Copy from emulated phone** (X19): ⋮ › **Copy from emulated phone** puts the guest's clipboard on your device (best effort on very new Android versions).
- **🏆 Achievements** (X18): the trophy button in the library opens 13 badges (First boot, Time traveler, Every era, 64-bit power, Marathon…) with progress bars; new ones are announced when you come back to the library.
- **📱 QR code for browser play** (X18): the Play in a browser dialog shows a QR code of the address to scan with another phone.
- **🕹️ Game mode** (X18): a running system's ⋮ › **Game mode** turns on full screen, the game buttons and keep-screen-on in one go, and puts everything back when you leave.
- **📺 Retro screen** (X17): a running system's ⋮ › **Retro screen** draws CRT scanlines, a faint shadow mask and dark corners over the screen; on by default for Android 1.x–2.x, remembered per firmware.
- **📳 Shake for screenshot** (X17): ⋮ › **Shake for screenshot**, then two quick shakes save a screenshot.
- **🎮 Controller rumble** (X17): when the guest vibrates, connected game controllers with a motor rumble too.
- **🕰️ Android time machine** (X17): App settings › Appearance › library layout › **Timeline** shows the firmwares on a line from 2008 to today, by release year and Android name.
- **🌐 Play in a browser** (X17): ⋮ › **Play in a browser** gives an address (with an access code) that any browser on the same Wi-Fi opens to watch and control the running system: touch or mouse, keyboard (Esc = Back), Back / Home / Recents.
- **🛠️ Android 1.x–2.x fixed again** (X16): since X14 they no longer booted (an Android 2.3 camera fix from AEmulator 0.0.0.3 also hit 1.x, and three engine libraries had been replaced); 32-bit firmware now runs exactly as in X13, and firmware that got the wrong camera library has its own one put back.
- **💬 Hardware 32-bit execution, explained** (X16): the setting now tests the chip with a tiny 32-bit program, so it unlocks on phones whose Android is 64-bit-only but whose CPU can still run 32-bit code, and says plainly when the chip has no 32-bit hardware (e.g. Snapdragon 8s Gen 3).
- **🎄 Christmas theme** (X15): App settings › Appearance › **Christmas** (red, green and gold, snow falling behind the library); **Snow in December** lets it snow all December with any theme, in Sleep mode's sky too.
- **📊 Performance overlay** (X15): a running system's ⋮ › **Performance overlay** shows FPS, CPU and RAM in a corner (green / orange / red); tap it to hide.
- **🖼️ Automatic tile pictures** (X15): about 15 s after each boot the home screen becomes the firmware's library picture (your own picture still wins).
- **🚀 64-bit Android** (X14): AEmulator X now includes the native arm64 engine of [AEmulator 0.0.0.3](https://github.com/uxazu/AEmulator/releases/tag/0.0.0.3): 64-bit firmware of Android 5.0–17 and GSIs (8–17) run directly on the phone, their 32-bit apps through qemu-arm (tested: Android 10 boots to the Pixel launcher). New import formats for them: super.img, EROFS, LZ4. 32-bit firmware boots exactly as before.
- **☀️ Time mode** (X13): App settings › Appearance › theme › **Time**: the app is light in the day (7:00–19:00) and dark at night, switching by itself with the clock; the library greets you with a sun or a moon (Good morning / afternoon / evening / night).
- **🌙 Sleep mode** (X12): library ⋮ › **Sleep mode**: a dimmed night sky in warm colours with a quiet clock, 4-7-8 breathing (in 4 s, hold 7 s, out 8 s), generated sleep sounds (rain, ocean, soft noise) and a 15/30/45/60-minute timer that fades the sound out over its last two minutes, then lets the screen turn off.
- **🌊 Relax mode** (X10): library ⋮ › **Relax mode**: slow calm colours, a breathing guide (in 4 s, hold 4 s, out 6 s), optional generated ocean sound and a 1/3/5-minute timer.
- **🔷 Holo style** (X9): App settings › Appearance › AEmulator X theme › **Holo** gives AEmulator X itself the Android 4.0 look: black, Holo blue, flat panels with thin blue lines, square corners and upper-case blue section titles.
- **🎞️ Custom boot animation** (X9): a firmware's settings › **Boot animation**: the firmware's own, AEmulator X's (a glowing X, generated for the screen size) or any `bootanimation.zip` you pick. Firmwares with their own boot screen format (Samsung .qmg, some HTC) keep theirs.
- **📱 Built-in device skins** (X9): App settings › Appearance › VM screen theme › **Pebble White**, **Slate Black**, **Slider QWERTY** or **Tablet**: original drawings of 2.x–4.x era phones and a tablet around the screen; their drawn keys work. Slider and Tablet look best with a landscape resolution (480×320, 800×600).
- **⛶ Full screen mode** (X9): ⋮ › **Full screen** hides AEmulator's button bar and device frame and makes the screen as big as possible; **Stretch to fill** covers the whole phone screen. Home and Recent apps move into the ⋮ menu; Back is your phone's back gesture. Remembered per firmware.
- **📺 Picture-in-picture** (X8): leave a running system and it keeps going in a small floating window (App settings › Appearance to turn it off).
- **📤 Send to emulated phone** (X8): share files from any app to AEmulator X; they land in the running system's Download folder, and APKs are installed.
- **🧩 Home screen widget** (X8): AEmulator X's widget starts your favourite and recently played systems with one tap.
- **🖥️ VNC server** (X8): ⋮ › **VNC server** lets any VNC viewer on the same Wi-Fi (RealVNC Viewer, TigerVNC, bVNC…) see and control the running firmware: click = tap, right-click = Back, keyboard types. A new random password every session; only changed parts of the screen are sent.
- **🕹️ On-screen game buttons** (X7): ⋮ › **Game buttons** puts a translucent D-pad, A/B/X/Y and Select/Start over the screen (multi-touch), mapped like a real controller.
- **🔍 Search, sort & favourites** (X7): search the library by name, version, skin or model; sort by name, Android version, recently played or recently added; ⭐ favourites always come first.
- **⏱️ Play time** (X7): each firmware shows how long you've played it and when you last did.
- **📱 Home screen shortcuts** (X7): ➕ / ⋮ › **Add to home screen** starts a firmware straight from your launcher.
- **🖼️ Screenshots & recordings gallery** (X7): the gallery button in the library shows everything you captured, to open, share or delete.
- **🎮 Game controllers** (X6): Bluetooth and USB gamepads work in the firmware: buttons, D-pad, sticks and triggers. *Gamepad buttons* for Android 4.0+ games, *classic phone keys* for 2.x games (A = select, B = Back, Start = Enter, Select = Menu); automatic by Android version, changeable in the VM settings › Controls › Game controller.
- **🖼️ Custom tile pictures** (X6): in the square grid, ⋮ › **Choose picture** gives a firmware's tile your own image instead of its last screen.
- **🎨 AEmulator X themes** (X6): App settings › Appearance › **AEmulator X theme**: System, Violet, Ocean, Sunset, Emerald, Rose or Mono recolour the whole app, gradient X and aurora included.
- **🧪 Experimental 64-bit probe** (X5): 64-bit firmware still cannot boot, but its VM settings now have **Experimental: 64-bit probe**. It runs the firmware's own shell and commands natively on your phone and shows a report you can copy. It changes and boots nothing. Proven so far on Android 8 (Oreo) firmware with an Android 16 phone: the firmware's shell, loader, libraries and toybox commands all run natively, with no translator. Still missing for a 64-bit boot: a file view (paths like `/system` pointing into the firmware), a 64-bit binder, system properties and a 64-bit graphics bridge. Please share your probe reports in the issues.
- **🔲 Square grid library** (X4): App settings › Appearance › **Library layout** → List, **Square grid** or 3D. Each firmware is a square glass tile showing its last screen, a gradient release badge, name and version, and a dot while it runs. Tap to start; long-press or ⋮ for settings, rename, delete and low-power start.
- **📱 Android 1.x–2.1** (X3): 1.0–2.1 firmware is now recognised correctly, and firmware that draws its screen in software (e.g. the T-Mobile G1 on 1.6) is shown instead of a black screen. Tested: T-Mobile G1, Android 1.6 (DMD64).
- Package `app.aemu.x`: installs **next to** AEmulator Plus, so you can keep both.

## ✨ Everything from Plus

- Import almost any firmware format: CWM/TWRP ZIP, Samsung Odin `.tar.md5`, Google factory `.tgz`, `system.img`, OTA `system.new.dat.br`, Motorola `.sbf`, sparse images, EDL images, Spreadtrum `.pac`, HTC `.nbh`, HTC `RUU.exe`
- Vendor skins work as shipped: HTC Sense, Samsung TouchWiz, MIUI, AOSP
- Hardware graphics through the GL bridge, sound, touch and multitouch, network with a modern TLS proxy
- Shared memory-card folder for APKs, music and photos
- VM screen themes and theme editor, Material 3 Expressive interface in 19 languages
- Free and open source (GPL-3.0); see [NOTICE.md](NOTICE.md) for what X changed

## 🚀 Quick start

1. Download the APK from this repository's Releases page and install it.
2. Open AEmulator X → **Add firmware** and pick the file. Import takes a few minutes.
3. Press **Start**. The first boot is slower: the system optimises its apps.
4. Use the ⋮ menu for volume, power button and logs; the ⚙️ button opens app settings and language.

## 📋 Requirements

- Android 8.0+ on a 64-bit ARM phone (arm64-v8a)
- About 1–3 GB of free space per firmware
- A recent Snapdragon / Dimensity / Tensor chip is recommended; AEmulator X also supports the MediaTek Helio G81 Ultra (MT6769 platform, Mali-G52 GPU; tested on a real device) and the HiSilicon Kirin 980 (Huawei P30 / P30 Pro, Mate 20; Mali-G76 GPU; tested on a real P30 Pro)

## ⚙️ How it works

Each guest process runs under a patched user-mode QEMU. A binder daemon replaces the kernel driver, a GL bridge forwards OpenGL ES calls to the phone’s GPU, and small guest libraries (audio HAL, audio policy wrapper, LD_PRELOAD shim) adapt vendor code to the emulator. The importer reads the firmware, finds the init scripts in its boot image and builds a start plan for system services.

## 🛠️ Build from source

The tested app release build uses JDK 21 and Android SDK 36. See [build/source instructions](docs/build-source.md), including native toolchain details and signing. **Inherited engine prebuilts do not yet have verified complete source/build provenance**; see [the audit](docs/license-audit.md).

```bash
git clone https://github.com/somerandomusernam2/AEmulator-Plus.git
cd AEmulator-Plus
./gradlew copyReleaseApks
```

## 🙏 Credits

AEmulator Plus grew out of the HTC Desire HD and HTC One M7 emulators by [the original author](https://t.me/istratiit_ech) — their engine made this project possible.

This is a modified fork of [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset), which is a modified fork of [uxazu/AEmulator](https://github.com/uxazu/AEmulator).

## 🔗 Links

- My fork: [somerandomusernam2/AEmulator-Plus](https://github.com/somerandomusernam2/AEmulator-Plus)
- Upstream: [drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset)
- Original: [uxazu/AEmulator](https://github.com/uxazu/AEmulator)
- Author: [somerandomusername2](https://github.com/somerandomusernam2)
- Original author: [t.me/istratiit_ech](https://t.me/istratiit_ech)

## 📄 License

GPL-3.0. Android, trademarks and firmware belong to their owners.

See [dated modification notices](NOTICE.md) and the [outstanding source/licensing audit](docs/license-audit.md). The GPL label is not a certification that every bundled prebuilt has complete matching source.
