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
- **2026-10-28**: AEmulator X38 (`0.0.0.2.13-x38`): new green app icon, artwork by Chris (donkris01) (`docs/assets/icon-x.png`,
  `docs/assets/logo.png`, `docs/assets/make-icon.sh`, `res/mipmap-*/ic_launcher_*.png`, `res/drawable-nodpi/logo.png`,
  `res/values/colors.xml` `ic_bg`, `app/src/main/java/app/aemu/ui/AppIcons.kt`).
- **2026-10-24**: AEmulator X37 (`0.0.0.2.13-x37`): the empty library's heading follows the theme's text colour, so it is readable
  over live backgrounds (`app/src/main/java/app/aemu/ui/MainActivity.kt`).
- **2026-10-24**: AEmulator X36 (`0.0.0.2.13-x36`): dark mode while a live background is on, so text is readable over the dark
  scenes (`app/src/main/java/app/aemu/ui/Theme.kt`).
- **2026-10-24**: AEmulator X35 (`0.0.0.2.13-x35`): live backgrounds (`app/src/main/java/app/aemu/core/LiveScenes.kt`,
  `ui/LiveBackground.kt`, `ui/XStyle.kt`, `ui/AppSettingsActivity.kt`), penalty shootout (`core/Penalty.kt`, `ui/PenaltyActivity.kt`,
  whistle and crowd sounds in `core/UiSounds.kt`, Penalty king badge in `core/Achievements.kt`); tests in `X35LiveScenesTest.kt`,
  `X27PenaltyTest.kt`.
- **2026-10-24**: AEmulator X34 (`0.0.0.2.13-x34`): the Mono icon style was named ic_launcher_mono, the name of the main icon's
  monochrome layer, so the main adaptive icon referred back to an adaptive icon and launchers showed the default icon; it is now
  ic_launcher_bw (`res/mipmap-anydpi-v26/ic_launcher_bw.xml`, `res/mipmap-*/ic_launcher_bw_fg.png`, `AndroidManifest.xml`,
  `res/values/colors.xml`, `app/src/main/java/app/aemu/ui/AppIcons.kt`, `docs/assets/make-icon-variants.sh`); tests in `X34IconTest.kt`.
- **2026-10-23**: AEmulator X33 (`0.0.0.2.13-x33`): new coral app icon, artwork by Chris (donkris01) (`docs/assets/icon-x.png`,
  `docs/assets/logo.png`, `docs/assets/make-icon.sh`, `docs/assets/make-icon-variants.sh`, `res/mipmap-*/ic_launcher_*.png`,
  `res/drawable-nodpi/logo.png`, `res/values/colors.xml` `ic_bg`, `app/src/main/java/app/aemu/ui/AppIcons.kt`).
- **2026-10-23**: AEmulator X32 (`0.0.0.2.13-x32`): firmware badges (`app/src/main/java/app/aemu/core/FirmwareTags.kt`,
  `ui/FirmwareTagChips.kt`, `MainActivity.kt`, `LibraryGrid.kt`, `Library3D.kt`), slideshow (`core/SlideOrder.kt`,
  `ui/SlideshowActivity.kt`); tests in `X32FeaturesTest.kt`.
- **2026-10-22**: AEmulator X31 (`0.0.0.2.13-x31`): screenshot stickers (`app/src/main/java/app/aemu/core/Stickers.kt`,
  `ui/StickerEditor.kt`, `ui/GalleryActivity.kt`); tests in `X31StickersTest.kt`.
- **2026-10-22**: AEmulator X30 (`0.0.0.2.13-x30`): Hebrew AEmulator X texts (`res/values-iw/strings_x.xml`) and voice command
  words (`core/VoiceCommands.kt`); classic wallpaper gallery reading the wallpapers of the user's own firmwares, nothing bundled
  (`app/src/main/java/app/aemu/core/FirmwareWallpapers.kt`, `ui/WallpaperGalleryActivity.kt`, `ui/LibraryWallpaper.kt`,
  SET_WALLPAPER in `AndroidManifest.xml`); tests in `X29FeaturesTest.kt`, `X30WallpaperTest.kt`.
- **2026-10-22**: AEmulator X29 (`0.0.0.2.13-x29`): boot logo creator (`app/src/main/java/app/aemu/core/BootLogoMaker.kt`,
  `ui/BootLogoCreator.kt`, `ui/BootAnimSection.kt`), Polish, French and German AEmulator X texts (`res/values-pl/strings_x.xml`,
  `res/values-fr/strings_x.xml`, `res/values-de/strings_x.xml`), voice command words in those languages (`core/VoiceCommands.kt`);
  tests in `X29FeaturesTest.kt`.
- **2026-10-22**: AEmulator X28 (`0.0.0.2.13-x28`): app icons from Chris's artwork (`docs/assets/make-icon-variants.sh`,
  `res/mipmap-*/ic_launcher_*_fg.png`, `res/mipmap-anydpi-v26/ic_launcher_*.xml`, `res/values/colors.xml`, launcher
  activity-aliases in `AndroidManifest.xml`, `app/src/main/java/app/aemu/ui/AppIcons.kt`), play timer (`core/PlayTimer.kt`,
  `VmActivity.kt`), firmware info card (`ui/FirmwareInfo.kt`, `MainActivity.kt`, `LibraryGrid.kt`, `Library3D.kt`); tests in
  `X28FeaturesTest.kt`.
- **2026-10-22**: AEmulator X27 (`0.0.0.2.13-x27`): collections (`app/src/main/java/app/aemu/core/Collections.kt`,
  `ui/CollectionChips.kt`, `MainActivity.kt`), AEmulator Wrapped (`core/WrappedStats.kt`, `ui/WrappedActivity.kt`); tests in
  `X27FeaturesTest.kt`.
- **2026-10-21**: AEmulator X26 (`0.0.0.2.13-x26`): the in-app logo (Settings › About) now uses the AEmulator X icon by
  Chris (donkris01) instead of the AEmulator Plus one (`res/drawable-nodpi/logo.png`, made by `docs/assets/make-icon.sh`).
- **2026-10-21**: AEmulator X25 (`0.0.0.2.13-x25`): gamepad editor (`app/src/main/java/app/aemu/core/PadLayout.kt`,
  `ui/GamePadOverlay.kt`, `VmActivity.kt`), device museum (`core/DeviceMuseum.kt`, `ui/MuseumActivity.kt`); tests in
  `X25FeaturesTest.kt`.
- **2026-10-21**: AEmulator X24 (`0.0.0.2.13-x24`): screenshot frames (`app/src/main/java/app/aemu/core/ScreenshotFrame.kt`,
  `core/Screenshot.kt`, `VmActivity.kt`), firmware notes (`core/FirmwareNotes.kt`, `MainActivity.kt`, `LibraryGrid.kt`, `Home.kt`);
  tests in `X24FeaturesTest.kt`.
- **2026-10-21**: AEmulator X23 (`0.0.0.2.13-x23`): daily challenges (`app/src/main/java/app/aemu/core/DailyChallenge.kt`), Android
  birthdays (`core/AndroidBirthdays.kt`), sound effects synthesised in code (`core/UiSounds.kt`), library cards and confetti
  (`ui/DailyCards.kt`); wiring in `core/Achievements.kt`, `core/GuestVm.kt`, `MainActivity.kt`, `VmActivity.kt`,
  `SnakeActivity.kt`, `AppSettingsActivity.kt`; tests in `X23FeaturesTest.kt`.
- **2026-10-20**: AEmulator X22 (`0.0.0.2.13-x22`): theme maker (`app/src/main/java/app/aemu/core/CustomTheme.kt`, `ui/ThemeMaker.kt`),
  instant replay (`core/ReplayRing.kt`, `ui/ReplayDialog.kt`), voice commands (`core/VoiceCommands.kt`), gesture shortcuts
  (`core/GestureShapes.kt`, `ui/GesturePad.kt`), music player (`ui/HomeMusic.kt`), firmware of the day (`core/DailyPick.kt`,
  `ui/FirmwareOfTheDay.kt`); wiring in `VmActivity.kt`, `MainActivity.kt`, `AppSettingsActivity.kt`, `XStyle.kt`, `Theme.kt`,
  `core/GifRecorder.kt`; tests in `X22FeaturesTest.kt`.
- **2026-10-20**: AEmulator X21 (`0.0.0.2.13-x21`): hidden Dessert Snake game (`app/src/main/java/app/aemu/core/SnakeGame.kt`,
  `ui/SnakeActivity.kt`), opened from the About logo in `AppSettingsActivity.kt`; Dessert lover badge in `Achievements.kt`; tests in `X21SnakeTest.kt`.
- **2026-10-20**: AEmulator X20 (`0.0.0.2.13-x20`): key mapping (`app/src/main/java/app/aemu/core/KeyMap.kt`,
  `app/src/main/java/app/aemu/ui/KeymapOverlayView.kt`), external display (`app/src/main/java/app/aemu/ui/ExternalScreen.kt`),
  `VmActivity.kt`; tests in `X20FeaturesTest.kt`.
- **2026-10-19**: AEmulator X19 (`0.0.0.2.13-x19`): statistics (`app/src/main/java/app/aemu/ui/StatsDialog.kt`,
  `app/src/main/java/app/aemu/core/PlayDays.kt`, `PlayStats.kt`), library wallpaper (`app/src/main/java/app/aemu/ui/LibraryWallpaper.kt`,
  `XStyle.kt`, `AppSettingsActivity.kt`), screen filters (`CrtOverlay.kt`), touch macros (`app/src/main/java/app/aemu/core/TouchMacro.kt`,
  `InputService.kt`), GIF recording (`app/src/main/java/app/aemu/core/GifRecorder.kt`, using Square gifencoder 0.10.1, Apache License 2.0;
  `Screenshot.kt`), clipboard from the guest (`app/src/main/java/app/aemu/core/GuestClipboard.kt`), `VmActivity.kt`, `MainActivity.kt`;
  tests in `X19FeaturesTest.kt`.
- **2026-10-19**: AEmulator X18 (`0.0.0.2.13-x18`): achievements (`app/src/main/java/app/aemu/core/Achievements.kt`,
  `app/src/main/java/app/aemu/ui/AchievementsDialog.kt`, `MainActivity.kt`), QR code of the browser play address
  (`app/src/main/java/app/aemu/core/QrCode.kt`, using ZXing core 3.5.3, Apache License 2.0), Game mode (`VmActivity.kt`);
  tests in `X18FeaturesTest.kt`.
- **2026-10-19**: AEmulator X17 (`0.0.0.2.13-x17`): retro screen (`app/src/main/java/app/aemu/ui/CrtOverlay.kt`), shake for screenshot
  (`app/src/main/java/app/aemu/core/ShakeDetector.kt`), controller rumble (`VibrationBridge.kt`), Android time machine library
  (`app/src/main/java/app/aemu/ui/Timeline.kt`, `AppPrefs.kt` `LAYOUT_TIMELINE`, `MainActivity.kt`, `AppSettingsActivity.kt`), play in a
  browser (`app/src/main/java/app/aemu/core/WebPlayServer.kt`, `VmActivity.kt`); tests in `X17FeaturesTest.kt`.
- **2026-10-19**: AEmulator X16 (`0.0.0.2.13-x16`): Android 1.x–2.x boot fix after the 0.0.0.3 merge (`TreeFixer.kt` camera swap off for
  32-bit firmware and undone, X13's `engines/kk/libGLES.so`, `libGLES_split.so`, `libashmemshim.so` and `engines/gb/libashmemshim.so` back,
  0.0.0.3's GL builds kept as `engines/kk/*.n64.so` for the arm64 engine, `GuestRunner.kt`); AArch32 detection by a test program and a clear
  message (`CpuInfo.kt`, `SettingsSheet.kt`; tests in `CpuInfoProbeTest.kt`).
- **2026-10-19**: AEmulator X15 (`0.0.0.2.13-x15`): Christmas theme and December snow (`app/src/main/java/app/aemu/ui/Snow.kt`, `XStyle.kt`,
  `AppPrefs.kt`, `AppSettingsActivity.kt`, `SleepActivity.kt`), performance overlay (`app/src/main/java/app/aemu/core/PerfStats.kt`,
  `VmActivity.kt`), home-screen tile pictures (`Screenshot.kt` `HOME`, `Library3D.kt`, `VmActivity.kt`); tests in `PerfStatsTest.kt`, `SnowfallTest.kt`.
- **2026-10-18**: AEmulator X14 (`0.0.0.2.13-x14`): merged [uxazu/AEmulator](https://github.com/uxazu/AEmulator) `0.0.0.3` (GPL-3.0):
  its native arm64 engine (`Kernel64.kt`, `WebView64.kt`, `EngineAssets.kt`, `CameraFeed.kt`, `Lights.kt`, `Sensors.kt`, `AudioIn.kt`,
  `assets/engines/n64-*`, `jniLibs/arm64-v8a/libaemu64_*`, `libbinderd64_*`, `native/guest64`), import formats (`Formats.kt`,
  `ErofsImage.kt`) and fixes; for 32-bit firmware the X/Plus code is kept wherever the two clashed (merge commit 3665110).
- **2026-10-18**: AEmulator X13 (`0.0.0.2.13-x13`): Time mode, day and night interface (`app/src/main/java/app/aemu/ui/TimeMode.kt`, `Theme.kt`,
  `AppPrefs.kt` `THEME_TIME`, `AppSettingsActivity.kt`, `MainActivity.kt`, `res/values*/strings_x.xml`; tests in `TimeModeTest.kt`).
- **2026-10-18**: AEmulator X12 (`0.0.0.2.13-x12`): Sleep mode (`app/src/main/java/app/aemu/ui/SleepActivity.kt`, `app/src/main/java/app/aemu/core/SleepSound.kt`,
  `MainActivity.kt`, `AndroidManifest.xml`, `res/values*/strings_x.xml`; tests in `SleepModeTest.kt`).
- **2026-10-18**: AEmulator X11 (`0.0.0.2.13-x11`): new app icon, artwork by Chris (donkris01), a friend of the developer (`docs/assets/icon-x.png`, `docs/assets/make-icon.sh`,
  `docs/assets/logo.png`, `res/mipmap-*/ic_launcher_fg.png` and `ic_launcher_mono.png`, `res/mipmap-anydpi-v26/ic_launcher.xml`,
  `res/values/colors.xml` `ic_bg`).
- **2026-10-17**: AEmulator X10 (`0.0.0.2.13-x10`): Relax mode (`app/src/main/java/app/aemu/ui/RelaxActivity.kt`, `app/src/main/java/app/aemu/core/OceanSound.kt`,
  `MainActivity.kt`, `AndroidManifest.xml`; tests in `BreathingTest.kt`).
- **2026-10-17**: AEmulator X9 (`0.0.0.2.13-x9`): full screen mode, Holo style, custom boot animation, built-in device skins.
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
