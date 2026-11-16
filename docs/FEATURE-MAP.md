# freshGram Android — карта переноса фич

База: Nekogram (форк официального Telegram Android, GPLv2 + доп. условия Telegram).
Источник фич: Snowy-Fluffy/freshGram (Telegram Desktop, слой `Telegram/SourceFiles/ayu`, ~100k строк C++).

Статусы: `есть` — уже в базе, `порт` — переносим, `N/A` — смысла на Android нет.

## Из AyuGram

| Фича | Статус | Куда/как |
|---|---|---|
| Режим призрака (гибкий) | порт | `GhostMode`, перехват read/typing/online в `MessagesController`, `SendMessagesHelper`, `MediaController` |
| История: правки и удалённые (SQLite + бэкапы) | порт | новый пакет `fgram/history`: SQLite-схема, hooks в `MessagesController.processUpdate*`, `MessageObject` diff |
| Anti-recall | порт | перехват `TLRPC.TL_updateDeleteMessages` / `updateDeleteScheduledMessages` |
| Peek last seen | порт | `PrivacySettings`/`account.updateStatus`, временное исключение, восстановление |
| Секретные чаты в списке | есть | в Android секретные чаты уже в списке; собственная крипто-схема freshGram не нужна |
| Сохранять системные сообщения | порт | `MessageObject` + локальная БД истории |
| «Удалённые» чаты остаются в списке | порт | хранимый список диалогов + hooks `deleteDialog` |
| Настройка шрифтов | порт | выбор семейства (Google Sans / Vazirmatn) через `Theme`/`Typeface` |
| Режим стримера | порт | спойлер-обёртки в `ChatMessageCell`, `ProfileActivity`, `DialogCell` |
| Локальный Premium | есть | Nekogram уже подменяет premium |
| Переводчик | есть | `tw/nekomimi/nekogram/translator` |
| Force-click превью | N/A | macOS |
| Улучшенное оформление | порт | тема/иконки |
| Выбор иконки приложения | есть | 28 activity-alias (`DefaultIcon`, `AquaIcon`, …) уже в Nekogram |

## Из materialgram

| Фича | Статус | Куда/как |
|---|---|---|
| Material You темы (Google Day/Dark) | есть | Monet уже в Nekogram: `MonetHelper` + темы `monet_light/dark.attheme` |
| Google Sans везде | порт | `assets/fonts` + `Theme.createTypeface` |
| Material-иконки | порт | `res/drawable` из `Resources/fa/micons` |
| Скругления медиа, сообщения без хвостов | порт | `ChatMessageCell`, `PhotoViewer` |
| Цветной фон ответов | порт | `ChatMessageCell.drawReplyBackground` |
| Без обводки у больших эмодзи | порт | `AnimatedEmojiDrawable` |
| Меньше CAPS в UI | порт | строки/`TextView` uppercase |
| Перемотка кружков | порт | `RoundVideoView` |
| Удаление >100 сообщений | порт | лимиты `MessagesController.deleteMessages` |
| Копирование ID автора стикерсета/инкремента | порт | меню стикерсета |
| Упоминание нескольких пользователей | порт | `MentionsContainer`/меню |
| Кнопки меню админов и журнала | порт | `ChatActivity` меню |
| Копирование @username | есть | `ChatActivity`/`ProfileActivity` уже копируют с `@` |
| Фото из @gamee в профиле | порт | `ProfileActivity` (опция) |
| Без задержки записи голосовых | порт | `MediaController`/`ChatActivity` |
| Web-app платформа «android» | N/A | уже android |
| Заменённые звуки | порт | `res/raw` |
| Меньшее сжатие JPEG | готово | фото 94% в `SendMessagesHelper.sendPhoto` |
| Мин. размер окна / толщина кисти | N/A/порт | окно — N/A, кисть — `PhotoPaintView` |
| Меньше задержек, быстрее загрузка | порт | точечно |
| Анимация спойлера | порт | `SpoilerEffect` |
| Улучшения панели стикеров | порт | `EmojiView` |
| Экспорт чатов до 10000 сообщений | порт | `ExportChatting` |
| Битрейт голосовых | порт | `MediaController` Opus |
| Скрытие номера | есть | Nekogram: `HidePhone` |
| Больше недавних стикеров | частично | лимит хранилища 200 (`MessagesController`); показ всё ещё режет `NekoConfig.maxRecentStickers` (20) |
| Дата создания аккаунта и ДЦ в профиле | порт | `ProfileActivity` |
| ДЦ и исходная дата фото | порт | `PhotoViewer`/`ImageReceiver` |
| Платформа отправки фото | порт | `PhotoViewer` |
| Детали уникальных подарков | порт | `ProfileActivity` |

## Порядок работ

1. Каркас: брендинг, сборка, подпись, CI.
2. Волна 1 (UI): Material You, Google Sans, иконки, скругления/хвосты, звуки, лимиты.
3. Волна 2 (данные): SQLite-история, anti-recall, удалённые чаты.
4. Волна 3 (privacy): ghost mode, peek last seen, streamer mode.
5. Волна 4: остальное (стикеры, экспорт, профиль, подарки, переводчик-опции).
