# freshGram Android — состояние проекта

## Что это
Форк Nekogram (Telegram Android) с брендингом freshGram. Цель — перенести фичи
Snowy-Fluffy/freshGram (Telegram Desktop) на Android. Карта фич: `docs/FEATURE-MAP.md`.

## Текущий статус: приложение собирается, запускается, в него можно войти

### Сборка
```bash
JAVA_HOME=$HOME/jdk25 ANDROID_HOME=$HOME/Android/Sdk ./gradlew :TMessagesProj_App:assembleRelease \
  -Pandroid.injected.testOnly=false --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC" -Dorg.gradle.parallel=true
```
Без `-Pandroid.injected.build.abi` собирается универсальный APK (все ABI) +
сплиты по ABI в `TMessagesProj_App/build/outputs/apk/release/`.

Для установки на телефон (APK помечен как не-testOnly, но `-t` не повредит):
```bash
adb install -r -t TMessagesProj_App/build/outputs/apk/release/freshGram-*-universal.apk
```

Ключ подписи: `~/freshgram-release.jks` (alias `freshgram`), пароли в `local.properties`.

## Что уже сделано (проверено на устройстве)

### Каркас / брендинг
- Пакет `app.freshgram.android`, имя приложения freshGram.
- **Исправлен мгновенный SIGKILL при старте.** Причина: анти-тампер проверка подписи
  в нативном слое (`TMessagesProj/jni/colorado/`) была захардкожена под официальный
  Nekogram (пакет `tw.nekomimi.nekogram` + его сертификат) и вызывала
  `kill(getpid(), SIGKILL)` при несовпадении. Константы в `colorado/colorado.h`
  пересчитаны под наш пакет и наш сертификат (CERT_SIZE 0x57c, CERT_HASH 0xcc88ad67).
  **Важно:** если менять ключ подписи — нужно пересчитать эти константы, иначе
  приложение снова будет себя убивать. Debug-сборка (debug-ключ) тоже будет падать.
- Иконка приложения — из freshGram Desktop (`art/ayu/default/app.png`),
  adaptive-icon + legacy mipmap'ы.
- **14 вариантов иконок** из `art/ayu/*` подключены к activity-alias
  (plane, alt, bard, chibi, chibi2, discord, extera, extera2, nothing, segment,
  spotify, yaplus, wheelold, win95).
  Встроенный выборщик иконок (`LauncherIconController` + `ThemeActivity`) теперь тоже показывает
  эти арты (preview через `icon_N_fg`), названия вариантов переименованы под Desktop-имена.
- Зелёная тема вместо синей: `assets/day.attheme`, `assets/night.attheme`,
  `ThemeColors.java` (`TELEGRAM_COLOR = 0xFF40D245` и ~280 цветов).
- Cronet из GMS отключён (`CronetHelper.ENABLED = false`) — на Huawei ронял процесс.
- Analytics выключена по умолчанию; Firebase/Sentry обёрнуты в try/catch.

### Стартовый экран (IntroActivity)
- Заголовок freshGram, текст-строка обновлена (en + ru).
- **Переключатель языков English / Русский** слева, на уровне кнопки смены темы
  (справа). Смена языка пересоздаёт интро (`reloadInterface` → новый `IntroActivity`).
- Дисклеймер «freshGram — неофициальный форк…» + ссылка на `Snowy-Fluffy/freshGram`.

### Настройки: «Настройки Neko» → «Чат»
- **Анти-удаление** — удалённые сообщения остаются видимыми и в БД.
- **Режим призрака** — не отправляются: отметки о прочтении (`completeReadTask`),
  статус набора (`sendTyping`), онлайн-статус (`updateTimerProc`).
- **Оставлять удалённые чаты** — чат не исчезает из списка (`deleteDialog`).
- **Режим стримера** — скрывает номер телефона в настройках и профиле.
- **Выбор без ограничений** — снимает лимит в 100 сообщений при массовом выборе;
  запрос на удаление автоматически режется на пачки по 100 ID
  (`DeleteMessagesBottomSheet`), чтобы сервер принял его.
- **История редактирования** — прежние версии отредактированных сообщений
  сохраняются в отдельной БД `fgram_edits.db` (`EditHistoryHelper`).
  Пишется и при своей правке (`SendMessagesHelper.editMessage`), и при чужой
  (`MessagesController.processUpdateArray` → `updateEditMessage`).
  В меню сообщения появляется «История правок» (bottom sheet со списком версий).
- **Копировать ID сообщения** — пункт в контекстном меню сообщения
  копирует его числовой ID (AyuGram-стиль).
- **Сохранять удалённые сообщения** — при удалении сообщения сервером его текст
  сохраняется в отдельной БД `fgram_deleted.db` (`DeleteHistoryHelper`), даже если
  анти-удаление выключено. Хук в `MessagesController.processUpdateArray` (сбор
  `deleteHistoryIds`), запись — на storage-очереди до фактического удаления.
  Просмотр: меню чата (⋮) → «Удалённые сообщения» — bottom sheet со списком
  сохранённых удалённых (отправитель, дата, текст).
- **Peek last seen** — при включении тумблера `peekLastSeen` открытие чужого профиля
  временно выводит аккаунт в онлайн (на ~20 сек), чтобы получить точное «last seen»,
  затем снова скрывает (`MessagesController.peekLastSeenFor` + `updateTimerProc`).
  и использует системный резолвер, что позволяет Telegram подключаться через VPN,
  где Google DNS заблокирован или медленный (`ConnectionsManager.ResolveHostByNameTask`
  при `useSystemDns=true` сразу идёт через `InetAddress.getAllByName`).
- **Исправлен дедлок storage-очереди** (симптом «бесконечно обновляется»): история
  удалённых вызывала `MessagesStorage.getMessage()` внутри `getStorageQueue().postRunnable`,
  что блокировало очередь навсегда. Теперь удалённые пишутся прямо в
  `MessagesStorage.markMessagesAsDeletedInternal`, а история правок — через новый
  асинхронный `MessagesStorage.getMessageAsync` (без блокировки).

### Брендинг раздела «Настройки Neko»
- Заголовок — «Настройки freshGram».
- В блоке «О проекте» ссылка на канал `@nekoupdates` заменена на
  `freshGram Desktop` (github.com/Snowy-Fluffy/freshGram); сайт `nekogram.app`
  убран; «Исходный код» ведёт на базовый Nekogram.

### Настройки: «Настройки Neko» → «Оформление»
- **Семейство шрифтов**: Системный / Google Sans / Vazirmatn.

### Прочее
- Зелёная аватарка чата Telegram (id 777000) — `AvatarDrawable.setInfo` (проверка 777000
  поставлена выше peer-цвета) + `ImageReceiver.setForUserOrChat` принудительно рисует
  буквенный аватар вместо синего фото-логотипа Telegram для 777000.
- **Порядок вкладок нижней панели**: перетаскивание вкладки с зажатием (long-press + drag)
  переставляет её на новое место с сохранением последовательности остальных
  (`MainTabsLayout.moveChild`), порядок хранится в `NekoConfig.mainTabsOrder` и
  применяется при запуске (`MainTabsActivity.addTabsInOrder`).

## Что НЕ сделано (остальные волны)
- История правок и удалённых сообщений (SQLite + бэкапы) — крупная фича.
- Vault / зашифрованные чаты / менеджер секретов.
- Плагины (desktop-плагинная система).
- Peek last seen (частично закрыт режимом призрака).
- Материал-иконки, хвосты/скругления сообщений, звуки, лимиты (>100 удаление, экспорт 10k).
- `HidePhone` (строка есть, код не подключён).

## Как продолжить
1. Разобраться с оставшимися фичами из `docs/FEATURE-MAP.md` — по одной за раз.
2. После каждой фичи: собрать APK, установить, проверить (без проверки регрессии не поймать).
3. Помнить про привязку проверки подписи в `colorado.h` к ключу.
