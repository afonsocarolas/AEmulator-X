# AEmulator X modification notice

**AEmulator X** is a modified fork of
[somerandomusernam2/AEmulator-Plus](https://github.com/somerandomusernam2/AEmulator-Plus) `0.0.0.2.13`,
still licensed under the GNU General Public License, version 3; see [LICENSE](LICENSE).
Files modified for X carry a dated "Modified for AEmulator X" (or "Added for AEmulator X") notice.
The Plus notice below, with its credits and history, is kept unchanged.

- **2026-10-15**: AEmulator X.
  - Rebrand: app name, package `app.aemu.x` (installs next to Plus), version `0.0.0.2.13-x1`; the
    updater and GitHub link follow `aemuX.repo` in `gradle.properties` (`app/build.gradle.kts`,
    `AppUpdateManager.kt`, `UpdateAssetPolicy.kt`, `AppSettingsActivity.kt`, `ExperimentalFeatures.kt`).
  - Cooler UI: violet/magenta/cyan palette (`Theme.kt`, `colors.xml`), animated aurora background,
    gradient wordmark, glass firmware cards with gradient badges and Start buttons
    (`app/src/main/java/app/aemu/ui/XStyle.kt`, `MainActivity.kt`).
  - Screen recording: guest screen (H.264) and guest sound (AAC) to
    `/sdcard/Movies/AEmulator/<timestamp>-<VM id>.mp4` (`app/src/main/java/app/aemu/core/ScreenRecorder.kt`;
    audio tap in `AudioOut.kt`; shared pixel decoding in `Screenshot.kt`; menu item and REC indicator in
    `VmActivity.kt`; `strings_x.xml`).
- **2026-10-15**: AEmulator X2 (`0.0.0.2.13-x2`).
  - Boot sound picker: per-VM `bootSound` setting (`GuestImage.kt`), playback and guest muting while booting
    (`GuestVm.kt`), synthesised built-in sounds, firmware sound scan and custom files
    (`app/src/main/java/app/aemu/core/BootSound.kt`), picker dialog (`app/src/main/java/app/aemu/ui/BootSoundPicker.kt`,
    `SettingsSheet.kt`), strings in `strings_x.xml`; tests in `BootSoundTest.kt`.
  - Updater: `-x<N>` versions and bare `x<N>` tags compare by X release (`AppUpdateManager.kt`, `AppVersionCompareTest.kt`).
- **2026-10-15**: AEmulator X3 (`0.0.0.2.13-x3`): Android 1.x–2.1.
  - Version detection for 1.0–2.1 firmware without `ro.build.version.sdk` (was guessed as API 19) and for trees
    without a `build.prop`; profile version 16 re-analyses earlier imports (`Analyzer.kt`, `EffectiveApiTest.kt`).
  - Framebuffer fallback: firmware that composites into fb0 and never draws through the GPU bridge (T-Mobile G1
    1.6) is shown from fb0 once boot completes (`GuestVm.kt` `fbDrawnAt`, `VmActivity.kt`).
- **2026-10-16**: AEmulator X4 (`0.0.0.2.13-x4`).
  - Host chip: MediaTek Helio G81 Ultra (MT6769 platform) recognised (`SocCompatibility.kt`, `SocCompatibilityTest.kt`).
  - Square grid library: `libraryLayout` app setting (List / Square grid / 3D, replaces the 3D switch; `AppPrefs.kt`,
    `AppSettingsActivity.kt`), square tiles (`app/src/main/java/app/aemu/ui/LibraryGrid.kt`, `MainActivity.kt`; the
    thumbnail helpers in `Library3D.kt` are now shared), strings in `strings_x.xml`.
- **2026-10-16**: AEmulator X5 (`0.0.0.2.13-x5`): experimental 64-bit probe for arm64 firmware
  (`app/src/main/java/app/aemu/core/Arm64Probe.kt`, `app/src/main/java/app/aemu/ui/Arm64ProbeSection.kt`,
  `SettingsSheet.kt`, `strings_x.xml`; tests in `Arm64ProbeTest.kt`). Runs the firmware's shell natively in five ways
  (host or guest linker and libraries, a PT_INTERP-rewritten copy started by the kernel, the firmware's toybox first on
  PATH) and reports the result; it never writes into the firmware tree.
- **2026-10-16**: AEmulator X6 (`0.0.0.2.13-x6`).
  - Game controllers: host gamepad buttons, D-pad, sticks, hat and triggers to guest Linux key codes, gamepad or
    classic phone-key mapping per VM (`app/src/main/java/app/aemu/core/GamepadMap.kt`, `VmActivity.kt`,
    `GuestImage.kt` `controllerMode`, `SettingsSheet.kt`; tests in `GamepadMapTest.kt`).
  - Custom tile pictures for the square grid (`app/src/main/java/app/aemu/core/TileCover.kt`, `LibraryGrid.kt`).
  - AEmulator X themes (System, Violet, Ocean, Sunset, Emerald, Rose, Mono) for the scheme and brand gradient
    (`XStyle.kt` `XThemes`, `Theme.kt`, `AppPrefs.kt`, `AppSettingsActivity.kt`).
  - Start on 64-bit firmware shows an explanation instead of running the 32-bit engine (`VmActivity.kt`).
  - Host chip: HiSilicon Kirin 980 recognised (`SocCompatibility.kt`, `SocCompatibilityTest.kt`).
- **2026-10-16**: AEmulator X7 (`0.0.0.2.13-x7`).
  - Play time per firmware (`app/src/main/java/app/aemu/core/PlayStats.kt`, `VmActivity.kt`).
  - Favourites, search and sort for the library (`app/src/main/java/app/aemu/ui/LibrarySort.kt`, `MainActivity.kt`,
    `LibraryGrid.kt`, `AppPrefs.kt`; tests in `LibrarySortTest.kt`).
  - Home screen shortcuts (`app/src/main/java/app/aemu/core/HomeShortcut.kt`).
  - Screenshots and recordings gallery (`app/src/main/java/app/aemu/ui/GalleryActivity.kt`, `AndroidManifest.xml`,
    `res/xml/file_paths.xml`).
  - On-screen game buttons (`app/src/main/java/app/aemu/ui/GamePadOverlay.kt`, `VmActivity.kt`).
- **2026-10-17**: AEmulator X8 (`0.0.0.2.13-x8`): the VNC server below (first pushed under x7), picture-in-picture,
  Send to emulated phone and the home screen widget.
  - VNC server: RFB 3.3–3.8 with VNC authentication (own DES), changed-tile Raw updates, DesktopSize, any true-colour
    pixel format; pointer/keyboard into the guest (`app/src/main/java/app/aemu/core/VncServer.kt`, `VncDes.kt`,
    `VncKeys.kt`, `VmActivity.kt`; tests in `VncServerTest.kt`).
- **2026-10-17**: picture-in-picture for the VM screen (`AndroidManifest.xml`, `VmActivity.kt`, `AppPrefs.kt`,
  `AppSettingsActivity.kt`); "Send to emulated phone" share target (`app/src/main/java/app/aemu/ui/ShareActivity.kt`,
  `VmActivity.kt` `receiveFiles`); quick-start home screen widget (`app/src/main/java/app/aemu/ui/RecentWidget.kt`,
  `res/layout/widget_recent.xml`, `res/xml/widget_recent_info.xml`, `res/values/widget_styles.xml`,
  `res/drawable/widget_*.xml`; tests in `RecentWidgetTest.kt`).
- **2026-10-17**: Holo style for the app (`XStyle.kt` `XTheme.holo`, `XSectionHeader`, `Theme.kt` HoloShapes,
  `MainActivity.kt`); custom boot animation (`app/src/main/java/app/aemu/core/BootAnimation.kt`, `GuestVm.kt`,
  `GuestImage.kt` `bootAnim`, `app/src/main/java/app/aemu/ui/BootAnimSection.kt`, `SettingsSheet.kt`); built-in
  device skins drawn in code, original artwork without photos or logos (`app/src/main/java/app/aemu/core/SkinArt.kt`,
  `VmTheme.kt`; tests in `SkinArtTest.kt`).
- **2026-10-17**: full screen mode for the VM screen (`VmActivity.kt`: layout without bar, frame or skin; fit or
  stretch; Home/Recents menu items; remembered per firmware).

---

# AEmulator Plus modification notice

**AEmulator Plus** is a modified fork of
[drel4/AEmulator-Sunset](https://github.com/drel4/AEmulator-Sunset), which is a modified fork of [uxazu/AEmulator](https://github.com/uxazu/AEmulator). It remains licensed
under the GNU General Public License, version 3; see [LICENSE](LICENSE).

The upstream project grew from the HTC Desire HD and HTC One M7 emulators by
the [original engine author](https://t.me/istratiit_ech). Those credits and the
upstream Git history are intentionally preserved.

**Modification statement:** AEmulator Plus was derived from AEmulator Sunset
`0.0.0.3-sunset.30`. Modifications by somerandomusernam2 (GitHub display name
somerandomusername2) are dated from 2026-10-04; the individual changes are recorded in this
repository's Git history. Files modified for Plus carry a dated
"Modified for AEmulator Plus" notice.

This work has been
modified by somerandomusernam2 and contributors to AEmulator Sunset. Original copyright,
license notices, credits and Git history are retained; no upstream authorship
is claimed for Plus's additions.

Source code for AEmulator Plus releases, including the exact tagged revision
used to build each APK, is published at:

<https://github.com/somerandomusernam2/AEmulator-Plus>

The Plus and Sunset application and fork-owned modifications are released under GPL-3.0.
This does not relicense independently licensed dependencies or user firmware.
See [build/source instructions](docs/build-source.md) and the
[source/licensing audit](docs/license-audit.md).

## Dated modifications

- **2026-10-04**: fork of Sunset `0.0.0.3-sunset.30`; Plus application changes
  (settings, importers, UI, helper bridges) from this date onward.
- **2026-10-11**: host microphone input. Added the host-side bridge
  (`app/src/main/java/app/aemu/core/MicBridge.kt`, `MicProtocol.kt`), the
  guest-side channel in `native/audiohal/audio_hal.c` and rebuilt the six
  `app/src/main/assets/engines/kk/audio.primary.*.so` libraries from that
  source with `native/audiohal/build-zig.sh` (LLVM/`zig cc`, no NDK). These six
  binaries are now reproducible from source tracked in this repository;
  `audio.primary.default.so` (the inherited HTC-layout HAL for Sense ROMs) is
  unchanged and still has no source mapping here.
- **2026-10-12**: merged from the AEmulator Super Plus fork (itself forked from Plus `0.0.0.2.9`;
  GPL-3.0), by manual diff against the fork point. Only these
  features were taken: European Portuguese (`pt-PT`: `app/l10n/langs/pt-PT.py`,
  `app/src/main/res/values-pt-rPT/strings.xml`, language list and `locales_config.xml`); Wi-Fi signal
  strength and nearby (locked) networks (`WifiEmu.kt`, `app/guest-wifi/**` and the rebuilt
  `aemu-wifi.jar`); configurable fake SIM cards (`SimCard.kt`, `SimSettings.kt`, `LegacySimIo.kt`, the
  SIM handling in `RilStub` in `Stubs.kt`, and the matching `VmSettings` fields and settings UI).
  Not taken: the 3D firmware library and the Android 8.x / Treble work.
- **2026-10-12** (second merge from the same fork): the VM screen theme editor. Added
  `app/src/main/java/app/aemu/core/VmTheme.kt` and `app/src/main/java/app/aemu/ui/ThemeSettings.kt`; the glyph
  sets, colours and press feedback in `HoloNavigation.kt`; the theme hooks in `VmActivity.kt`
  (bar, frame picture, rounded screen corners) and `AppSettingsActivity.kt` (the theme row replaces the navbar
  style switch); the `holoNavbar` flag in `AppPrefs.kt`; the `th_*` and `as_navbar_holo` strings
  (en, pt-BR, pt-PT). Not taken: the 3D firmware library and the other Super Plus changes.

- **2026-10-14**: raw NAND partition dumps (`mtd0.img`, `mtd1.img`, ... as found in HTC Sooner firmware). Added
  `app/src/main/java/app/aemu/importer/NandDump.kt`; `Importer.kt` (`importMtdDump`, hooks in `handle()` and the 7z
  loop); `Yaffs2Reader.kt` (the 2048+64 "sector row" spare layout); `BootImage.kt` (kernel with an appended gzip
  ramdisk, no `ANDROID!` header).

**Unresolved source-provenance gap:** this checkout contains inherited prebuilt
engine binaries for which matching source, patches and build instructions have
not been located or verified. The tagged checkout can rebuild the APK using
those prebuilts, but is not established as complete Corresponding Source for
every bundled component. This notice is not a claim of full GPL compliance;
publishing notices or hashes does not remedy missing required source.
