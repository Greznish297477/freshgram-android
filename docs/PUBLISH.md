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

`Settings → Secrets and variables → Actions → New repository secret`
(имя — ровно как в таблице, значение — без кавычек и без пробелов по краям).

| Секрет | Что это |
|---|---|
| `API_ID` | число из my.telegram.org |
| `API_HASH` | 32 hex-символа из my.telegram.org |
| `KEYSTORE_JKS` | base64 от `.jks` **одной строкой** |
| `KEYSTORE_PASSWORD` | пароль хранилища ключей |
| `KEY_ALIAS` | алиас ключа, для freshGram — `freshgram` |
| `KEY_PASSWORD` | пароль самого ключа |
| `GOOGLE_SERVICES_JSON` | (опц.) base64 от `google-services.json` |

### 2.1. API_ID и API_HASH

1. Откройте <https://my.telegram.org> и войдите по номеру телефона
   (код придёт в Telegram).
2. **API development tools** → заполните форму: название приложения
   (например `freshGram`), short name (`freshgram`), platform — **Android**,
   URL и описание — можно любые.
3. На странице появятся **App api_id** (число) и **App api_hash**
   (32 символа). Это и есть `API_ID` и `API_HASH`.

Нюансы:

- Пара `api_id`/`api_hash` — «личность» приложения для серверов Telegram.
  Не публикуйте её; при утере можно создать новое приложение.
- Лучше взять **те же** значения, с которыми собирались APK на машине сборки
  (они лежат в её `local.properties` в строках `apiId` / `apiHash`).
  С другим `api_id` приложение тоже работает, но это уже другая «личность»,
  и пользователям, возможно, придётся войти заново.
- У одного аккаунта лимит на количество приложений; удалять старые можно там же.

### 2.2. Ключ подписи (`.jks`)

Готовые APK в `dist/` подписаны ключом, под который уже настроен анти-тампер.
Его сертификат:

```
Owner:  CN=freshGram, OU=freshGram, O=freshGram, L=Local, ST=Local, C=US
Serial: c57b671652e35232
SHA256: 27:16:BC:1C:DB:AA:D0:D2:FB:9E:7D:FE:21:87:70:9C:36:5F:2C:6F:4B:CF:36:44:EE:D0:BB:C7:F0:51:9E:5B
```

**Ищите существующий `.jks`** на машине, где собирались релизы
(по `HANDOFF.md` — `~/freshgram-release.jks`, alias `freshgram`).
Пароли — в её `local.properties` (`storePassword`, `keyPassword`).

Проверка ключа на Windows (размер и CRC32 сертификата должны совпасть с
`CERT_SIZE 0x57c` / `CERT_HASH 0xcc88ad67` в
`TMessagesProj/jni/colorado/colorado.h`):

```powershell
.\Tools\keystore-check.ps1 -Keystore C:\keys\freshgram-release.jks -Alias freshgram -CopyToClipboard
```

Если PowerShell блокирует скрипты («выполнение сценариев отключено»), используйте
обёртку — она запускает то же самое с `-ExecutionPolicy Bypass`:

```powershell
.\Tools\keystore-check.cmd -Keystore C:\keys\freshgram-release.jks -Alias freshgram -CopyToClipboard
```

либо разрешите локальные скрипты один раз: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

Скрипт печатает сертификат, сверяет его с `colorado.h` и **кладёт base64
в буфер обмена** — остаётся вставить в поле `KEYSTORE_JKS`.

base64 вручную:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\keys\freshgram-release.jks")) | Set-Clipboard
```

**Если ключ потерян**, его нельзя восстановить (приватный ключ нигде больше
не хранится). Тогда:

```powershell
keytool -genkeypair -v -keystore freshgram-release.jks -alias freshgram `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -dname "CN=freshGram, OU=freshGram, O=freshGram, L=Local, ST=Local, C=US"
```

и **обязательно** пересчитать константы — скрипт выше покажет готовые строки:

```c
#define CERT_HASH 0x........
#define CERT_SIZE 0x...
```

Последствия нового ключа: уже установленное приложение нельзя обновить
поверх (Android отклонит APK с другой подписью) — нужна переустановка;
пользователи обновятся, только если удалят старую версию.

### 2.3. Пароли и alias

- `KEY_ALIAS` — имя ключа внутри хранилища. Для freshGram — `freshgram`
  (посмотреть: `keytool -list -keystore file.jks`).
- `KEYSTORE_PASSWORD` — пароль файла `.jks`
  (`keytool -list` спрашивает именно его).
- `KEY_PASSWORD` — пароль приватного ключа; при `-genkeypair` часто совпадает
  с паролем хранилища, но это не обязательно.

Через GitHub CLI:

```powershell
gh secret set API_ID --body "12345"
gh secret set API_HASH --body "0123456789abcdef0123456789abcdef"
Get-Content ks.b64 | gh secret set KEYSTORE_JKS
gh secret set KEYSTORE_PASSWORD --body "***"
gh secret set KEY_ALIAS --body "freshgram"
gh secret set KEY_PASSWORD --body "***"
```

> **Важно:** подпись привязана к анти-тамперу
> `TMessagesProj/jni/colorado/colorado.h`. Workflow намеренно падает, если
> `KEYSTORE_JKS` не задан, — debug-подпись привела бы к SIGKILL при старте.
> Секреты после сохранения прочитать нельзя, можно только заменить.

## 3. Залить workflow и выпустить релиз

```bash
git add .github/workflows/release-full-lite.yml .gitignore
git commit -m "ci: build full + lite releases"
git push

git tag v13.1.4
git push origin v13.1.4
```

Пуш тега запускает сборку. Через ~20–40 минут в `Releases` появятся файлы:

```
freshGram-13.1.4-7240-full-universal.apk
freshGram-13.1.4-7240-full-arm64-v8a.apk
freshGram-13.1.4-7240-full-armeabi-v7a.apk
freshGram-13.1.4-7240-full-x86.apk
freshGram-13.1.4-7240-full-x86_64.apk
freshGram-13.1.4-7240-lite-universal.apk
freshGram-13.1.4-7240-lite-arm64-v8a.apk
freshGram-13.1.4-7240-lite-armeabi-v7a.apk
freshGram-13.1.4-7240-lite-x86.apk
freshGram-13.1.4-7240-lite-x86_64.apk
```

Локальная альтернатива (уже собранные APK из `dist/`):

```bash
gh release create v13.1.4 dist/*.apk \
  --title "freshGram 13.1.4 (full + lite)" \
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
