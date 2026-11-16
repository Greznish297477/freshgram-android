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
- Version: `13.1.1` (code `7237`)
- Upstream version reported to Telegram: `13.1.1`

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
adb install -r freshGram-13.1.1-7237-full-universal.apk
```

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
