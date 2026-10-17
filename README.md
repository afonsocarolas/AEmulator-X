<div align="center">

<img src="docs/assets/logo.png" width="128" alt="AEmulator X logo"/>

# AEmulator X

**A fork of [AEmulator Plus](https://github.com/somerandomusernam2/AEmulator-Plus) with a cooler UI and screen recording.**

</div>

---

AEmulator X boots a real Android 2.3–7.x system image straight from a firmware file you already have, exactly like AEmulator Plus, and adds:

## ✨ New in X

- **🎬 Screen recording**: ⋮ menu → **Record screen**. Records the guest screen as H.264 video with the guest's own sound (AAC), 30 fps, sharp even on old 320×480 screens (scaled to 640–1280 px). A pulsing **REC** pill shows the time; tap it (or ⋮ → **Stop recording**) to save to `Movies/AEmulator/<time>-<VM>.mp4`. Works in both GPU-bridge and framebuffer modes, and in recovery. Only the guest screen is recorded, not the host's bars or menus.
- **🌌 Cooler UI**: an electric violet / magenta / cyan palette with a deep near-black dark mode, an animated aurora background behind the library, a glowing gradient **X** wordmark, and glass firmware cards with gradient release badges and Start buttons.
- **🔔 Boot sound picker** (X2): VM settings → **Boot sound**. Choose the firmware's own sound, silence, one of three original AEmulator X sounds (X Chime, Retro 8-bit, Rise), any sound file found inside the firmware (PowerOn.wav, UI/notification/ringtone sounds…), or your own audio file. Every option has a ▶ preview. A chosen sound replaces the firmware's boot sound: the system stays muted until it has booted.
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
