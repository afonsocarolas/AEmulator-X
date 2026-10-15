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
- A recent Snapdragon / Dimensity / Tensor chip is recommended

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
