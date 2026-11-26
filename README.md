# freshGram Android

A third-party Telegram client for Android that ports the most useful features of
**freshGram Desktop**, **AyuGram** and **Materialgram** on top of **Nekogram**.

freshGram Android is an unofficial fork. It is not affiliated with, endorsed by,
or supported by Telegram, Nekogram or the freshGram Desktop project.

## What it is

freshGram Desktop is a fork of Telegram Desktop focused on privacy and usability
extras (ghost mode, message history, anti-recall, streamer mode, theming). This
project brings that idea to Android, keeping the green freshGram branding and
adding select features from the AyuGram and Materialgram ecosystems.

## What it is based on

| Layer | Source | License |
|---|---|---|
| Android client | Official Telegram for Android | GPL-2.0 (+ Telegram additional terms) |
| Immediate base | [Nekogram](https://github.com/Nekogram/Nekogram) | GPL-2.0 |
| Feature reference | [freshGram Desktop](https://github.com/Snowy-Fluffy/freshGram) | GPL-2.0 |
| Feature reference | AyuGram, Materialgram | GPL-2.0 |

- Package: `app.freshgram.android`
- Version: `13.1.5` (code `7241`)
- Upstream version reported to Telegram: `13.1.5`

Because it is built on Telegram for Android (which carries the Telegram
additional terms on top of GPL-2.0), freshGram Android is distributed under the
same terms. See [`LICENSE`](LICENSE).

## Full and lite builds

Every release ships two variants as separate APKs:

| Variant | Description |
|---|---|
| **full** | Everything, including privacy extras. Recommended for personal use. |
| **lite** | Private extras disabled, built with `-PfreshgramLite=true`, intended to be safe for Google Play policy. |

Both variants share one package name (`app.freshgram.android`), so install
**only one** of them, and only one can be published to Google Play.

Private features that lite disables: anti-recall, ghost mode, keeping deleted
dialogs, streamer mode, edit history, saving deleted messages, sponsor-message
hiding and unlimited (>100) selection.

Everything else is identical in both builds: green theme and branding, launcher
icon picker, tab order, fonts, language switch and the intro screen.

## Download

APKs are attached to [GitHub Releases](../../releases):

```
freshGram-<version>-<code>-full-universal.apk      # all ABIs
freshGram-<version>-<code>-full-arm64-v8a.apk      # modern phones
freshGram-<version>-<code>-full-armeabi-v7a.apk    # older 32-bit phones
freshGram-<version>-<code>-lite-universal.apk
freshGram-<version>-<code>-lite-arm64-v8a.apk
freshGram-<version>-<code>-lite-armeabi-v7a.apk
```

Install with:

```bash
adb install -r freshGram-13.1.5-7241-full-universal.apk
```

## Automatic updates from GitHub

freshGram checks its own GitHub Releases for updates, so you don't need the Play
Store or a new APK download by hand:

- The app queries
  `https://api.github.com/repos/Greznish297477/freshgram-android/releases/latest`.
- It compares the latest release version with the installed one and picks the
  asset that matches your CPU ABI and build variant (**full** or **lite**).
- When a newer release is available it shows a dialog with the release notes;
  **Download Now** fetches the APK inside the app and hands it to the system
  installer (`REQUEST_INSTALL_PACKAGES` is already declared).
- You can trigger a manual check from **freshGram Settings → Check for update**
  (and also from **Settings → Experimental → Check for update**); the automatic
  check runs at most once a day.

> If you rename the repository or move the project to another owner, update
> `REPO_OWNER` / `REPO_NAME` in
> `TMessagesProj/src/main/java/tw/nekomimi/nekogram/helpers/FreshgramUpdater.java`.
> Updates only work for releases that contain the `freshGram-*-<variant>-<abi>.apk`
> assets, which the release workflow produces automatically.

## Local pins (more than 5 pinned chats)

Telegram limits pinned chats on the server (5 per folder for free accounts). freshGram
adds *local pins* — pins that exist only on this device and are never sent to Telegram:

- Just tap **Pin** as usual. When Telegram's server limit is reached, freshGram
  keeps the extra pin on this device instead of showing a "limit reached" error —
  visually it is indistinguishable from a normal pin and the menu shows **Unpin**.
- You can also use the explicit **⋮ → Pin locally** item (or select several chats
  → **⋮ → Pin locally**) to pin locally right away.
- A locally pinned chat gets the same pin badge and sits in the pinned block, right
  after the real server pins. There is no limit on how many you can add.
- The list is stored in `nekoconfig` preferences (`localPins`), ordered by when you
  added the pin. It is not synced, not visible on other devices and not shown in
  other Telegram clients.
- Unpin the same way: **⋮ → Unpin locally**.

This does not spoof Telegram Premium and does not change server state, so it cannot
get an account limited or banned. Real pin limits still apply to normal pins (the
ones synced across devices).

## Building

### Requirements

- JDK 25
- Android SDK (build-tools, NDK, CMake — see the CI setup)
- `local.properties` with API credentials and signing config

### Local builds

```bash
# full + lite, output to ./dist
./build-versions.sh
./build-versions.sh full
./build-versions.sh lite
```

Or the plain Gradle task:

```bash
./gradlew :TMessagesProj_App:assembleRelease
```

On Windows use the helper, which finds JDK 25 and the Android SDK by itself:

```powershell
.\Tools\build-local.ps1                              # full release
.\Tools\build-local.ps1 -Lite                        # lite release
.\Tools\build-local.ps1 -Task compileDebugJavaWithJavac   # quick Java-only check
```

> **Windows requirements for local builds**
> - The project path must contain **only ASCII characters** — AGP refuses
>   non-ASCII paths (`Your project path contains non-ASCII characters`).
> - `git config --global core.longpaths true` and
>   `LongPathsEnabled=1` are required for the `TMessagesProj_Modules/media` submodule.
> - JDK 25 + NDK 27.3.13750724 + CMake 3.22.1 must be inside the SDK
>   (`Sdk\ndk\27.3.13750724`, `Sdk\cmake\3.22.1`).

### CI builds

The workflow [`.github/workflows/release-full-lite.yml`](.github/workflows/release-full-lite.yml)
builds both variants and publishes a GitHub Release on every `v*` tag push.
See [`docs/PUBLISH.md`](docs/PUBLISH.md) for the required secrets and steps.

### Signing note

The native anti-tamper check in `TMessagesProj/jni/colorado/colorado.h` is bound
to a specific package name and signing certificate. If you change the signing
key you **must** recompute those constants, otherwise the app kills itself
(`SIGKILL`) on launch. Debug keys trigger the same behaviour.

## Project status and docs

- [`HANDOFF.md`](HANDOFF.md) — current project state (Russian)
- [`docs/FEATURE-MAP.md`](docs/FEATURE-MAP.md) — feature port map (Russian)
- [`docs/PORT-MATRIX.md`](docs/PORT-MATRIX.md) — port status matrix (Russian)
- [`docs/PUBLISH.md`](docs/PUBLISH.md) — release and publishing guide (Russian)
- [`docs/README.ru.md`](docs/README.ru.md) — full project documentation (Russian)

## API and protocol documentation

- Telegram API: https://core.telegram.org/api
- MTProto: https://core.telegram.org/mtproto

## License

GNU General Public License v2.0, with the additional terms that apply to the
underlying Telegram for Android source. See [`LICENSE`](LICENSE).

## Disclaimer

freshGram Android is an unofficial fork of Telegram and Nekogram. It is not
supported by the Telegram team. Use it at your own risk.
