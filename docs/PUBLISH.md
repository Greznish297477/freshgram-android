# Публикация freshGram на GitHub — full и lite через GitHub Actions

Workflow `.github/workflows/release-full-lite.yml` собирает **обе** версии
(full и lite) и публикует их отдельными APK в GitHub Release при пуше тега `v*`.

## 1. Создать свой репозиторий

Сейчас remote указывает на апстрим `Nekogram/Nekogram.git`. Переключите на свой:

```bash
# создать пустой репозиторий (один раз)
gh repo create <логин>/freshgram-android --private --source=. --remote=origin --push
# или вручную:
git remote set-url origin https://github.com/<логин>/freshgram-android.git
git push -u origin main
```

> Подмодули (`TMessagesProj/jni/td`, `tgcalls`, `third_party/*`) в CI клонируются
> рекурсивно. Если они указывают на оригинальные URL — всё ок, они публичные.

## 2. Добавить секреты репозитория

`Settings → Secrets and variables → Actions → New repository secret`:

| Секрет | Что это |
|---|---|
| `API_ID` / `API_HASH` | с my.telegram.org |
| `KEYSTORE_JKS` | `base64 -w0 freshgram-release.jks` |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | алиас ключа (`freshgram`) |
| `KEY_PASSWORD` | пароль ключа |
| `GOOGLE_SERVICES_JSON` | (опц.) `base64 -w0 google-services.json` |

Через CLI:

```bash
base64 -w0 ~/freshgram-release.jks > ks.b64
gh secret set API_ID --body "12345"
gh secret set API_HASH --body "0123456789abcdef0123456789abcdef"
gh secret set KEYSTORE_JKS < ks.b64
gh secret set KEYSTORE_PASSWORD --body "***"
gh secret set KEY_ALIAS --body "freshgram"
gh secret set KEY_PASSWORD --body "***"
```

> **Важно:** подпись привязана к анти-тамперу
> `TMessagesProj/jni/colorado/colorado.h`. Workflow намеренно падает, если
> `KEYSTORE_JKS` не задан, — debug-подпись привела бы к SIGKILL при старте.

## 3. Залить workflow и выпустить релиз

```bash
git add .github/workflows/release-full-lite.yml .gitignore
git commit -m "ci: build full + lite releases"
git push

git tag v13.1.1
git push origin v13.1.1
```

Пуш тега запускает сборку. Через ~20–40 минут в `Releases` появятся файлы:

```
freshGram-13.1.1-7237-full-universal.apk
freshGram-13.1.1-7237-full-arm64-v8a.apk
freshGram-13.1.1-7237-full-armeabi-v7a.apk
freshGram-13.1.1-7237-lite-universal.apk
freshGram-13.1.1-7237-lite-arm64-v8a.apk
freshGram-13.1.1-7237-lite-armeabi-v7a.apk
```

Локальная альтернатива (уже собранные APK из `dist/`):

```bash
gh release create v13.1.1 dist/*.apk \
  --title "freshGram 13.1.1 (full + lite)" \
  --notes-file dist/README.md
```

## Что изменилось в репозитории

- `release-full-lite.yml` — новый workflow (full + lite + Release).
- Старые `release.yml`, `build.yml`, `freshgram.yml` переименованы в `*.yml.bak`
  и больше не запускаются (они тянули Blacksmith-раннеры и лишние секреты).

## Ограничение

full и lite имеют **один** `applicationId` (`app.freshgram.android`).
Поэтому пользователь ставит **одну** из версий; рядом они не встанут,
и в Google Play можно загрузить только одну.

## Автообновление

Приложение само проверяет GitHub Releases и скачивает APK под свой ABI и вариант
(full/lite) — см. `TMessagesProj/src/main/java/tw/nekomimi/nekogram/helpers/FreshgramUpdater.java`.
От вас требуется только публиковать релизы с APK-файлами (workflow это делает).

Если репозиторий переименован/перенесён — обновите `REPO_OWNER` и `REPO_NAME`
в этом файле. Владелец по умолчанию: `Greznish297477/freshgram-android`.
