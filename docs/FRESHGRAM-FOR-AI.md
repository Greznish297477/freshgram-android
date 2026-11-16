# freshGram Android — полное описание проекта (для другой нейросети/разработчика)

Этот документ описывает, что представляет собой приложение, какие в нём есть функции,
как оно собирается и какие технические решения/подводные камни в нём есть.
Пиши по-русски простыми словами, всё в этом проекте делалось без интернета и без
доступа к исходникам freshGram Desktop — фичи портировались «по смыслу».

---

## 1. Что это за проект

`freshGram Android` — неофициальный клиент Telegram на базе **Nekogram** (форк Telegram
Android). Пакет приложения: `app.freshgram.android`. Название: **freshGram**.

Цель: перенести «уникальные» фичи **freshGram Desktop** и **AyuGram / Materialgram**
(которых нет ни в официальном Telegram, ни в Nekogram) в Android-клиент, плюс
собственный зелёный брендинг.

Исходники: `Snowy-Fluffy/freshGram` (Desktop) — референс для идей, не для кода.
База этого Android-проекта — Nekogram (Telegram Android fork), уже предварительно
переименованный под freshGram.

### На чём именно базируется форк (цепочка)
```
Telegram Android (официальный клиент, github.com/DrKLO/Telegram, форк TDesktop-команды)
        │
        ▼
Nekogram (github.com/Nekogram/Nekogram) — форк Telegram Android с «неко»-фичами:
        • встроенные настройки «Настройки Neko» (NekoConfig, tw.nekomimi.nekogram.*)
        • анти-удаление, режим призрака, оставлять удалённые чаты, стример-режим
        • безлимитный выбор (>100), кастомные шрифты, свои иконки лаунчера
        • переключение языка, блокировка sponsored, экспорт настроек и т.п.
        │
        ▼
freshGram Android (ЭТОТ проект, app.freshgram.android)
        • переименование и брендинг поверх Nekogram
        • порт уникальных фич из freshGram Desktop (Snowy-Fluffy/freshGram)
        • порт уникальных фич из AyuGram / Materialgram (Desktop-клиенты на базе TDesktop)
```

Ключевые внешние источники идей (не код, а поведение):
- **Nekogram** — база и «скелет» настроек (уже содержит часть AyuGram-подобных фич:
  анти-удаление, ghost, keep-deleted-chats, unlimited selection, custom fonts).
- **freshGram Desktop** (`Snowy-Fluffy/freshGram`) — целевой продукт, чьи фичи портируем.
- **AyuGram** (`AyuGram/AyuGramDesktop`) и **Materialgram** (`levlam`/`3ok`-family,
  Desktop-форки TDesktop) — источники фич: «удалить у всех, о себе оставить»,
  история правок/удалённых, peek last seen, перестановка вкладок drag-and-drop,
  сплэш-анимация, Material-оформление и др.
- Официальный **Telegram Android / TDesktop** — источник оригинального кода
  (Nekogram является его форком), API (`org.telegram.tgnet.tl.TL_account` и т.д.).

Важно: **Materialgram — Desktop-only**, поэтому он не является базой Android-проекта,
а только источником идей. Базой остаётся Nekogram.

Путь проекта на диске:
```
/home/gryaz2/Загрузки/freshGram-android-full-20261101/freshgram-android
```

---

## 2. Брендинг и внешний вид

### 2.1 Зелёная тема
- `TMessagesProj/src/main/assets/day.attheme` и `night.attheme` — перекрашены в зелёный.
- `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java` — цвета по умолчанию.
- Основной акцент: зелёный `#40D245` (в темах ~`FF40D245`).
- Служебный чат Telegram (id **777000**) рисуется зелёным кружком с белым логотипом-самолётиком
  вместо стандартного синего фото-логотипа:
  - `AvatarDrawable.setInfo()` — ветка `if (id == 777000)` ставит `color/color2` зелёные
    и подставляет `customIconDrawable = ic_telegram_plane`, `scaleSize = 1.0f`.
  - `AvatarDrawable` **обязательно** сбрасывает `customIconDrawable = null` в начале
    `setInfo()`, иначе самолётик «протекает» на обычные буквенные аватарки
    (был реальный баг: у текстовых аватарок вместо буквы/цифры рисовалась зелёная иконка TG).
  - `ImageReceiver.setForUserOrChat()` — для 777000 игнорирует серверное фото и рисует
    буквенный/иконочный аватар.
  - `TMessagesProj/src/main/res/drawable/ic_telegram_plane.xml` — **векторная иконка
    самолётика**. Путь взят из официального лого, viewport 120×120, но path лежит
    в координатах ~97..206 по X и ~120..210 по Y. В `<group>`:
    `pivotX=151.94 pivotY=165.44 scaleX/Y=0.72 translateX=-91.94 translateY=-105.44`.
    Масштаб 0.72 — чтобы самолётик **не выходил за границы круга** (иначе кончик
    вылезает). Если правишь path — пересчитай центр и подгони scale.
- «Избранное» (`AVATAR_TYPE_SAVED`) — зелёная закладка: в `setAvatarType()`
  отдельная ветка `color=0xFF0F9200`, `color2=0xFF94F389` (вместо стандартного
  голубого). Значок закладки остаётся штатный (`Theme.avatarDrawables[0]`).
- Заголовок «Настройки freshGram» — вместо «Настройки Neko». Иконка строки —
  `ic_telegram_plane` на зелёном градиенте (`SettingsActivity`, `SettingCell.Factory.of(50, 0xFFBFF7B4, 0xFF52C93D, R.drawable.ic_telegram_plane, ...)`).

### 2.2 Иконки приложения (16 вариантов)
- `TMessagesProj/src/main/java/org/telegram/ui/LauncherIconController.java` —
  enum `LauncherIcon` со списком вариантов: DEFAULT, AQUA, PREMIUM, TURBO, NOX,
  MERIO, RAINBOW, SCHOOL, MUSHEEN, SPACE, CLOUD, NEON, MATERIAL, WHEELOLD, WIN95,
  MATERIALDESIGN (новая зелёная Material-иконка).
- Каждому соответствует `activity-alias` в `TMessagesProj/src/main/AndroidManifest.xml`
  (компонент `org.telegram.messenger.<Key>`, напр. `MaterialDesignIcon`), а также адаптивные
  иконки `mipmap-anydpi-v26/icon_N_launcher.xml` + `drawable-*dpi/icon_N_fg.webp`.
  Для Material-иконки — `icon_17_launcher.xml` + `icon_17_fg.webp` (5 плотностей,
  сгенерированы PIL: зелёный градиент + белый «send»-самолётик Material Design).
- Выборщик иконок в UI (`AppIconsSelectorCell`, настройки → Оформление) показывает
  превью из `icon_N_fg`.
- **Важно (сделано в этой сессии):** выбранная иконка сохраняется в SharedPreferences
  (`mainconfig`, ключ `freshgram_launcher_icon` = имя enum) — см. `saveIcon()`,
  `getSavedIcon()`. При старте `tryFixLauncherIconIfNeeded()` (вызывается из
  `ApplicationLoader`, после `SharedConfig.loadConfig()`) — если ни один alias не
  включён (после обновления APK состояние компонентов может сброситься),
  восстанавливается сохранённая иконка, а не DEFAULT. Это фикс «при обновлении
  приложения сбрасывается выставленная иконка».
- `getSelectedIcon()` — возвращает текущую включённую иконку (используется сплэшем).

### 2.3 Сплэш-анимация (AyuGram-style)
- `TMessagesProj/src/main/java/org/telegram/ui/Components/SplashOverlay.java` —
  кастомный оверлей: круг с **иконкой, которая следует за выбранной в
  LauncherIconController** (`IconView` внутри читает `getSelectedIcon()`, рисует
  `icon.background` + `icon.foreground`, обрезая по кругу), плюс подпись «freshGram».
  Анимация: иконка появляется с масштабом 0.7→1.0 + поворот −12°→0° (420 мс),
  затем текст (fade), потом весь оверлей плавно исчезает (~900 мс + 300 мс fade).
- **Тема-зависимость:** конструктор `SplashOverlay(Context, boolean isDark)`.
  `isDark` берётся в `LaunchActivity` через `Theme.isCurrentThemeDark()` (в try/catch,
  fallback — системный `uiMode`). Светлая тема → белый фон + тёмный текст,
  тёмная → `#1F2732` + белый текст.
- Вставка: `LaunchActivity.onCreate()`, сразу после `setupActionBarLayout()`,
  только при `savedInstanceState == null` (чтобы не показывать при повороте экрана).
- На Android 12+ система сама рисует splash (`values-v31/styles.xml`,
  `android:windowSplashScreenAnimatedIcon = @drawable/ic_launcher_animated`,
  длительность `@integer/splash_screen_duration = 450`), поверх него идёт наш оверлей.

---

## 3. Стартовый экран (IntroActivity)

`TMessagesProj/src/main/java/org/telegram/ui/IntroActivity.java`:
- Заголовок freshGram.
- **Переключатель языков English / Русский** — слева, на одной высоте с кнопкой
  смены темы (справа). Смена языка пересоздаёт интро (`reloadInterface`).
- Дисклеймер: «freshGram — неофициальный форк Telegram/Nekogram…» + ссылка на
  `github.com/Snowy-Fluffy/freshGram`.
- Строки лежат в `values/strings_neko.xml` (en) и `values-ru/strings_neko.xml` (ru).

---

## 4. Настройки freshGram (бывшие «Настройки Neko»)

Строка «Настройки freshGram» вынесена **в самый верх списка настроек**.

### 4.1 Раздел «Чат» — новые тумблеры (все в `NekoConfig`)
| Тумблер | Флаг | Что делает |
|---|---|---|
| Анти-удаление | `antiRecall` | удалённые у других сообщения остаются видимыми и не удаляются из локальной БД |
| Режим призрака | `ghostMode` | не отправляются: прочтение, «печатает», онлайн-статус |
| Оставлять удалённые чаты | `keepDeletedChats` | чат не пропадает из списка при удалении |
| Режим стримера | `streamerMode` | скрывает номер телефона в настройках/профиле |
| Выбор без ограничений | `unlimitedSelection` | снимает лимит 100 при массовом выборе; запросы режутся на пачки по 100 ID |
| История редактирования | `keepEditHistory` | сохраняет все версии отредактированных сообщений в `fgram_edits.db` |
| Скрывать спонсорские | `hideSponsoredMessages` | прячет sponsored-сообщения в каналах |
| Сохранять удалённые | `keepDeleteHistory` | пишет тексты удалённых сообщений в `fgram_deleted.db` |
| Скрывать кнопку «Переслать» | `hideForwardButton` | убирает «Переслать» из меню |
| Peek last seen | `peekLastSeen` | при открытии чужого профиля временно выходит в онлайн, чтобы получить «был(а) недавно», затем снова скрывается |
| Копировать ID сообщения | (пункт меню) | копирует числовой ID сообщения |
| Удалённые сообщения | (пункт меню ⋮) | просмотр сохранённых удалённых (отправитель, дата, текст) |
| Удалить последние N сообщений | (пункт меню ⋮) | удаляет последние N сообщений; есть галочка «Удалить у всех (о себе оставить)» |

### 4.2 Раздел «Оформление»
- Семейство шрифтов: Системный / Google Sans / Vazirmatn.
- **Перестановка вкладок нижней панели** — long-press + drag, порядок сохраняется
  в `NekoConfig.mainTabsOrder`, применяется при старте. Тумблер `allowTabsReorder`
  (включён по умолчанию) — можно отключить перетаскивание.
- **Material-пузыри** (`NekoConfig.materialBubbles`) — Materialgram-стиль: увеличенный
  радиус пузырей (24dp) с одинаковыми углами (без «хвостика»). Реализация —
  `NekoConfig.applyMaterialBubblesRadius()` меняет сам `SharedConfig.bubbleRadius`
  (чтобы его подхватили ВСЕ места отрисовки: `ChatMessageCell`, `RichMessageLayout`,
  `MessageDrawable`), плюс хелперы `getBubbleRadius()` / `getNearBubbleRadius()`.
  **Важно:** правка только `MessageDrawable` не даёт видимого эффекта — радиус
  должен меняться в `SharedConfig.bubbleRadius`, иначе разница 17→20dp незаметна.
- Выбор иконки приложения (16 вариантов, включая новый **Material**) — см. п. 2.2.

### 4.3 Lite-версия (для Google Play)
- Собирается с флагом `-PfreshgramLite=true` (buildConfigField `FRESHGRAM_LITE`).
- `BuildVars.LITE` → в `NekoConfig.loadConfig()` принудительно отключаются и в UI
  скрываются приватные фичи: анти-удаление, режим призрака, оставлять удалённые,
  режим стримера, история редактирования, сохранять удалённые, скрывать спонсорские,
  выбор без ограничений, peek last seen.
- Всё остальное (тема, иконки, Material-пузыри, счётчик устройств, порядок вкладок,
  шрифты, языки, сплэш, системный DNS) — как в full.
- Скрипт сборки обеих версий: `./build-versions.sh` → APK в `./dist/`.
- **Lite дополнительно скрывает** «Удалить у всех (о себе оставить)» (и в обычном
  удалении, и в диалоге «Удалить последние N») — через `!BuildVars.LITE`, т.к.
  это тоже обходной хранения-фича.
- **Пакет у lite тот же** (`app.freshgram.android`) — потому что анти-тампер в
  `colorado.h` привязан к пакету+сертификату; менять applicationId нельзя без
  пересчёта констант.

### 4.3 Сеть / VPN
- Тумблер **«Системный DNS (для VPN)»** (`NekoConfig.useSystemDns`, по умолчанию выключен)
  + `useIpv4Fetch`. Реализация: `ConnectionsManager.ResolveHostByNameTask` — при
  `useSystemDns=true` резолв идёт через `InetAddress.getAllByName` (системный резолвер),
  а не через Google DoH. Это чинит «бесконечно обновляется» при включённом VPN
  (Google DNS заблокирован/медленный).

### 4.4 «Устройства» — счётчик подключённых устройств
- Строка «Устройства» в настройках теперь показывает **число активных авторизаций справа**.
- Реализация:
  - `MessagesController`: поле `volatile int deviceSessionsCount`, метод
    `loadDeviceSessionsCount()` — шлёт `TL_account.getAuthorizations`, считает
    `res.authorizations.size()`, сохраняет и постит `NotificationCenter.updateInterfaces`.
    Есть защита от параллельных запросов (`deviceSessionsRequestId`).
  - `SettingsActivity`: строка `SettingCell.Factory.of(8, …, getString(R.string.SettingsDevices), getString(R.string.SettingsDevicesInfo), devicesValue)`,
    плюс вызов `loadDeviceSessionsCount()`; по `updateInterfaces` список
    перестраивается (`listView.adapter.update(true)`), чтобы значение обновилось.
  - `SessionsActivity.loadSessions()` — при загрузке авторизаций пишет точное число
    в `deviceSessionsCount`.

---

## 5. Уникальные фичи AyuGram / freshGram Desktop (реализовано)

### 5.1 «Удалить у всех, а у себя оставить»
Доступно двумя путями:
1. **Обычное удаление сообщения** (меню сообщения → «Удалить»): в диалоге удаления
   появился второй чекбокс `DeleteForAllKeepLocal` («Удалить у всех (о себе оставить)»)
   под чекбоксом «Удалить для всех». Реализовано в
   `AlertsCreator.createDeleteMessagesAlert` (обе ветки: каналы/мегагруппы и личные чаты):
   добавлен `LinearLayout` с двумя `CheckBoxCell`; в `deleteAction` при включённом
   флаге вызывается `deleteMessagesKeepLocally`.
2. **Диалог «Удалить последние N сообщений»** (меню чата ⋮) — там тоже есть этот чекбокс.
- `MessagesController.deleteMessagesKeepLocally(ArrayList<Integer> messages, long dialogId, int topicId, boolean forAll)`
  — отправляет на сервер запрос удаления с `revoke = forAll` (в каналах
  `TL_channels_deleteMessages`), но **локально ничего не удаляет**: сообщения
  остаются в чате и БД. Ответ обрабатывается через `processNewDifferenceParams` /
  `processNewChannelDifferenceParams`.
- Вызов из `ChatActivity.deleteLastNMessages(n, keepLocally)`.

### 5.2 «Удалить последние N сообщений»
- `ChatActivity.showDeleteLastNMessagesDialog()` + `deleteLastNMessages(int n, boolean keepLocally)`.
- Собирает ID последних N сообщений из `messages`, пропуская `isDateObject`,
  `id <= 0`, sponsored. **Служебные сообщения (action != null) НЕ пропускаются** —
  иначе в служебном чате Telegram (777000) удаление «не работало».
- Обычный режим: `MessagesController.deleteMessages(ids, null, null, dialog_id, threadMessageId, false, chatMode)`.

### 5.3 Анти-удаление / сохранение удалённых / история правок
- `MessagesStorage.markMessagesAsDeletedInternal` — хук на удаление: пишет тексты
  в `fgram_deleted.db` (`DeleteHistoryHelper`) **напрямую**, а не через storage-очередь.
- `MessagesStorage.getMessageAsync` — новый асинхронный геттер для истории правок
  (`EditHistoryHelper`, БД `fgram_edits.db`), чтобы не было дедлока.
- **Критично:** нельзя вызывать `MessagesStorage.getMessage()` внутри
  `getStorageQueue().postRunnable` — это вешало очередь навсегда
  (симптом «бесконечно обновляется» при запуске). Такой код был и удалён.
- История правок пишется и при своей правке (`SendMessagesHelper.editMessage`),
  и при чужой (`MessagesController.processUpdateArray` → `updateEditMessage`).
- В меню сообщения → «История правок» (bottom sheet со списком версий).
- Меню чата (⋮) → «Удалённые сообщения» (bottom sheet).
- Автоудаление по таймеру (self-destruct) тоже учитывается в хуке.

### 5.4 Peek last seen
- `NekoConfig.peekLastSeen` + `MessagesController.peekLastSeenFor(int seconds)`
  (ставит `peekLastSeenUntil`), в `updateTimerProc` аккаунт держится онлайн до этого
  времени, затем снова скрывается.
- `ProfileActivity`: при открытии чужого профиля вызывает `peekLastSeenFor(30)` и через
  1200 мс повторно запрашивает полный профиль (`loadFullUser(user, classGuid, true)`)
  — чтобы точный last seen успел прийти.

### 5.5 Перестановка вкладок нижней панели
- `MainTabsActivity` + `MainTabsLayout`: drag через long-press (`moveChild`),
  порядок в `NekoConfig.mainTabsOrder` (строка-разделитель), применение
  `addTabsInOrder`, сохранение `persistTabsOrder`.
- Порядок отражается после перезапуска (пересоздание вьюхи).

---

## 6. Прочие расширения/фиксы

- **Копировать ID сообщения** — `OPTION_COPY_MESSAGE_ID` в меню сообщения.
- **Скрывать кнопку переслать** (`hideForwardButton`).
- **Выбор без ограничений** (`unlimitedSelection`) — батчинг по 100 в
  `DeleteMessagesBottomSheet`.
- **Analytics выключена** по умолчанию; Firebase/Sentry обёрнуты в try/catch.
- **Cronet из GMS отключён** (`CronetHelper.ENABLED = false`) — на Huawei ронял процесс.
- **Анти-тампер SIGKILL**: нативный код `TMessagesProj/jni/colorado/colorado.h`
  проверяет подпись. Константы (`CERT_SIZE 0x57c`, `CERT_HASH 0xcc88ad67`,
  пакет `app.freshgram.android`) пересчитаны под наш ключ. **Если сменить ключ
  подписи — приложение немедленно убивает себя (`kill(getpid(), SIGKILL)`) при старте.**
- Настройки: «О проекте» — ссылка на `github.com/Snowy-Fluffy/freshGram`, сайт
  nekogram.app убран, «Исходный код» ведёт на Nekogram.
- Убран суффикс `@RKBDI` в заголовке.

---

## 7. Как собирать и тестировать

### 7.1 Сборка
```bash
cd /home/gryaz2/Загрузки/freshGram-android-full-20261101/freshgram-android
JAVA_HOME=$HOME/jdk25 ANDROID_HOME=$HOME/Android/Sdk ./gradlew :TMessagesProj_App:assembleRelease \
  -Pandroid.injected.testOnly=false --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx8g -Dfile.encoding=UTF-8 -XX:+UseParallelGC" -Dorg.gradle.parallel=true
```
- ~6–10 минут, лучше запускать в фоне, лог в файл.
- Универсальный APK (все ABI):
  `TMessagesProj_App/build/outputs/apk/release/freshGram-13.1.1-7237-universal.apk`
- Только arm64 (быстрее): добавить `-Pandroid.injected.build.abi=arm64-v8a`,
  APK появится в `TMessagesProj_App/build/intermediates/apk/release/`.

### 7.2 Установка и проверка
```bash
adb install -r TMessagesProj_App/build/outputs/apk/release/freshGram-*-universal.apk
adb shell am force-stop app.freshgram.android
adb shell am start -n app.freshgram.android/org.telegram.messenger.DefaultIcon
adb logcat -c   # перед воспроизведением бага
```
- Телефон: Huawei POT-LX1, серийник `WQD4C19219004204`.
- Автотест UI: `adb shell uiautomator dump /sdcard/ui.xml` + `adb shell cat /sdcard/ui.xml`,
  парсить `bounds` в Python (см. историю чата).
- Тап по координатам: `adb shell input tap X Y`; длинный тап/драг:
  `adb shell input swipe X Y X Y 3000`.
- Русский текст через `adb shell input text` не вводится — использовать английские
  запросы или тапать по элементам.

### 7.3 Проверки визуала (Python + Pillow)
Скриншот: `adb exec-out screencap -p > shot.png`. Затем анализ цветов:
```python
from PIL import Image
im = Image.open('shot.png').convert('RGB')
px = list(im.crop((x1,y1,x2,y2)).getdata())
green = sum(1 for r,g,b in px if g>110 and g>r+25 and g>b+25)
print(green/len(px))
```
Так проверялись: зелёная аватарка 777000 (green 83.7%, blue 0%) и «Избранное».

### 7.4 Ключ подписи
`~/freshgram-release.jks`, alias `freshgram`, пароли в `local.properties`.
**Менять ключ нельзя без пересчёта `colorado.h`** — иначе SIGKILL на старте.

---

## 8. Известные ограничения и что дальше

- Все изменения **не закоммичены** в git (190+ файлов); рабочая копия — источник истины.
- Vault / зашифрованные чаты, плагины Desktop, Material-иконки сообщений, звуки —
  не реализованы (вне задачи этой волны).
- Lite-версия для Google Play — отдельная задача (см. ниже).

### Lite-версия (для Google Play)
Идея: собрать вариант без спорных функций (anti-recall, удалённые сообщения со
хранением текста, peek last seen, скрытие sponsored и пр.), т.к. Google Play
ограничивает функциональность, нарушающую приватность/ToS Telegram.
Технически: продуктовый флейвор (`TMessagesProj_App/build.gradle`) с флагом в
`BuildVars`, которым в `NekoConfig` по умолчанию выключаются соответствующие
тумблеры и скрываются пункты меню. Проверить `TMessagesProj_App/build.gradle`
на наличие flavor-конфигураций и при отсутствии — добавить `productFlavors { full {} lite {} }`.

---

## 9. Ключевые файлы (карта)

| Файл | Что там |
|---|---|
| `.../ui/Components/AvatarDrawable.java` | зелёная аватарка 777000 + самолётик, сброс `customIconDrawable`, зелёное «Избранное» |
| `.../messenger/ImageReceiver.java` | нейтрализация фото 777000 |
| `.../res/drawable/ic_telegram_plane.xml` | вектор самолётика (scale 0.72) |
| `.../ui/Components/SplashOverlay.java` | сплэш-анимация с иконкой приложения |
| `.../ui/LaunchActivity.java` | точка вставки сплэша, выбор темы для сплэша |
| `.../ui/LauncherIconController.java` | 15 иконок, сохранение/восстановление выбранной |
| `.../ui/SettingsActivity.java` | строка «Настройки freshGram» (top), счётчик устройств |
| `.../ui/SessionsActivity.java` | загрузка авторизаций, запись `deviceSessionsCount` |
| `.../messenger/MessagesController.java` | `deleteMessagesKeepLocally`, `peekLastSeenFor`, `loadDeviceSessionsCount`, peek в `updateTimerProc`, анти-удаление |
| `.../messenger/MessagesStorage.java` | хук удаления (`markMessagesAsDeletedInternal`), `getMessageAsync` |
| `.../ui/ChatActivity.java` | «Удалить последние N» (+keep-locally), история правок/удалённых, копирование ID |
| `.../ui/MainTabsActivity.java`, `.../ui/MainTabsLayout.java` | порядок вкладок + drag |
| `.../tw/nekomimi/nekogram/NekoConfig.java` | все тумблеры + export/import |
| `.../ui/ProfileActivity.java` | peek last seen + повторная загрузка профиля |
| `.../ui/ConnectionsManager.java` (messenger) | `useSystemDns` / `useIpv4Fetch` в `ResolveHostByNameTask` |
| `TMessagesProj/src/main/AndroidManifest.xml` | 15 activity-alias иконок |
| `TMessagesProj/jni/colorado/colorado.h` | анти-тампер константы (привязаны к ключу!) |
| `docs/PORT-MATRIX.md` | матрица портированных фич |
| `HANDOFF.md` | краткая сводка состояния (устаревает) |

---

## 10. Правила работы над этим проектом

1. **Тестировать каждую фичу на реальном устройстве** — без этого ловятся регрессии
   (примеры: `action != null` ломал удаление в служебном чате; `getMessage()` в
   storage-очереди вешал приложение; `customIconDrawable` протекал на текстовые аватарки).
2. Собирать APK после каждой волны правок, устанавливать `adb install -r`
   (данные сохраняются).
3. Не менять ключ подписи без пересчёта `colorado.h`.
4. Не удалять пользовательские (не закоммиченные) изменения: в репозитории лежит
   много параллельных правок.
5. Не отправлять сообщения никому, кроме официальных чатов Telegram; не логировать
   `api_hash` и прочие секреты.
