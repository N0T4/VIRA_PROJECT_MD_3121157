# Екрани та компоненти
Статус: специфікація для реалізації, не готові UI-файли.

| Компонент | Призначення | Обов'язкові стани |
|---|---|---|
| MainScreen | Logo / video; фіксоване місце | entry, ready, buffering, playing, paused, unavailable, error |
| SummonNavigation | Discover / Library / Social, пошук | hidden, revealing, ready, focus, disabled |
| FeaturedDiscover | Великий тайтл + постерні tiles | loading, ready, missing artwork, in library |
| SearchGrid | Пошук, фільтри, пагінація | typing, loading, results, empty, offline, error, next-page error |
| UnfoldingCard | Постер, дані, серії, оцінка, статус | folded, opening, open, held, docked, hidden, recovered |
| EpisodeList | Сезон/серія/джерело | available, selected, unavailable, loading, failed |
| LibraryCategory | Planning / Watching / Completed | selected, empty, loaded, saving, error |
| LibraryDisplay | Обмежена видима підмножина колекції | page, filter, empty, no poster |
| DropTarget | Локальне додавання або перенесення | hidden, available, active, invalid, saving, success, error |
| ProgressRating | Прогрес і персональна оцінка | unknown total, zero, valid, validation error, saving |
| PlaybackControls | Play/pause/seek/volume/subtitles | ready, playing, paused, buffering, unsupported |
| SocialPanel | Присутність і запрошення | signed out, connecting, joined, reconnecting, error |
| ProviderSettings | Список сервісів та підключень | unconfigured, validating, connected, expired, unsupported |

## Правила даних
Каталог тайтлів не гарантує playable video. Відсутній постер має neutral placeholder.
Персональна оцінка — цілий бал від 1 до 10 або порожнє значення. Середній рейтинг каталогу зберігається окремо. Повна серія автоматично зараховується після завершення. [D013](../decisions/DECISIONS.md).
Episode ID — ключ провайдера/матеріалу, а не тільки «номер 1». Різні джерела можуть мати різні списки.
Каталог використовує окремі демонстраційні дані.

## Текст і доступність
Пропозиція: робоча мова англійська; японські підписи тільки перевірені й необов'язкові для розуміння.
Ключові стани позначати не лише кольором: label/icon/outline. Disabled не маскувати під активну кнопку.
Документальна serif/display графіка не переноситься в довгий опис або дрібні кнопки.
Після кожного дизайну потрібен screenshot на пристрої з реальними довгими назвами.
