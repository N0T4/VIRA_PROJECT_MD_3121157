# T070 — можливості відеоджерел на Quest

Перевірено 2026-11-05. Власник підтвердив **https://animego.org/** та **https://animeua.club/**. T070 завершено як обмежене дослідження; інтеграції ще не реалізовано. Мета D011 зберігається: офіційні сервіси та користувацькі API.. Підтвердження документацією, читання сторінки й фактичне відтворення мають окремі статуси.

## AnimeGO та Anime UA — поточний пріоритет

Підтверджений `animego.org` перенаправляє на `animego.me`; AnimeUA лишається `animeua.club`. Раніше знайдений `aniua.top` не обрано. Зміна домену під час майбутнього завантаження має перевірятися явно. [AnimeGO](https://animego.org/), [AnimeUA](https://animeua.club/).

Результати трьох окремих перевірок:

1. **Каталог — HTML_OBSERVED.** Отримано публічні сторінки з назвами, описами, жанрами, роками, постерами й кандидатами ID. Повне перебирання каталогу не перевірене. Обидва robots задають обмеження маршрутів; для автоматичного збору потрібен узгоджений спосіб доступу, а права описів/постерів перевіряються окремо. [Каталог AnimeGO](https://animego.me/anime), [деталі AnimeUA](https://animeua.club/8404-shkilnyi-gul.html).
2. **Серії та озвучення — PARTIAL_METADATA.** Видно загальні лічильники, команди й частину розкладу. Повний список відтворюваних серій зі сталими ID для конкретного озвучення не отримано. AnimeGO підвантажує плеєр/серії окремо; у перевіреному HTML AnimeUA немає такого списку. Не вмикати Play з дати ефіру чи загальної кількості. [AnimeGO title](https://animego.me/anime/i-vse-taki-mir-prekrasen-4445), [AnimeUA title](https://animeua.club/8404-shkilnyi-gul.html).
3. **Відтворення — NOT_CONFIRMED / NOT_RUN.** В AnimeGO початковий HTML містить JavaScript loader; в AnimeUA знайдено lazy iframe стороннього плеєра на `ashdi.vip` і повідомлення про доступність лише з України. Публічний iframe не є native MP4/HLS descriptor, дозволом embed чи перевіркою Quest. Потоки й захист не обходили; iframe/відео не завантажували.

Обидва [robots AnimeGO](https://animego.me/robots.txt), [robots AnimeUA](https://animeua.club/robots.txt) та [умови AnimeGO](https://animego.me/terms) отримано HTTP 200. Вони не підтверджують для VIRA ліцензію повторного використання чи контракт playback. Публічний API не підтверджено в оглянутих сторінках; це не доказ його відсутності. Один AnimeGO search GET виконався до прочитання robots; після виявлення обмеження `/search/` пошукові запити припинено. Парсер у застосунку, приватні endpoints, player-loader, cookies та media extraction не використовувалися.

## Матриця інших перевірених можливостей

| Джерело / шлях | Підтверджено | Статус у поточній VIRA |
|---|---|---|
| Оригінальний локальний preview T050 | Власний файл, стабільний ID E1, відтворення на головному екрані та керування підтверджені попередніми Quest-перевірками | Є у T052. Це 16-секундний preview, не повна серія. Новий тест T070 — NOT_RUN. |
| Netflix → Meta Quest Browser | Netflix документує Quest 2/Pro/3/3S і до 1080p у своєму браузерному плеєрі | Кандидат зовнішнього відкриття. Кнопку VIRA, вхід, відтворення й повернення ще не перевірено. [Netflix Quest](https://help.netflix.com/en/node/110502) |
| Netflix → власний плеєр / WebView / головний екран VIRA | У переглянутих відкритих офіційних матеріалах не підтверджено доступний цьому проєкту consumer playback/embed API, контракт каталогу й дозвіл на інтеграцію | ACCESS_NOT_CONFIRMED. Підтримка Quest Browser не підтверджує сторонній WebView або Surface. |
| Crunchyroll → Quest-застосунок | Сторінка Meta Store показує **Coming soon / Winter 2026** | Анонс, а не підтверджений реліз чи встановлення. Перевірити статус повторно перед будь-яким тестом. [Meta Store](https://www.meta.com/experiences/crunchyroll-anime-streaming/1069089185792025/) |
| Crunchyroll → офіційний сайт / власний плеєр | Quest Browser і придатний для VIRA публічний каталог/playback API не підтверджені перевіреними джерелами | Зовнішній офіційний URL — лише кандидат. Native/WebView, DRM-доступ і дозволи — NOT_CONFIRMED. |
| Власне або належно ліцензоване HTTPS MP4 | Android MediaPlayer підтримує мережеві потоки | Найменший кандидат для внутрішнього плеєра. Нинішній код приймає тільки raw resource; мережева версія ще не реалізована. [Android](https://developer.android.com/media/platform/mediaplayer) |
| Дозволений HLS / DASH | Media3 документує обидва протоколи, MP4 та відповідні обмеження форматів/кодеків | Технічний кандидат. Media3/HLS/DASH у VIRA не додано, Quest playback — NOT_RUN. [Формати](https://developer.android.com/media/media3/exoplayer/supported-formats) |
| Особистий Jellyfin | Є офіційний API/Kotlin SDK та документована авторизація | Можливий custom provider. Сервер і контент власника не обрані й не перевірені. Ліцензія SDK та права на медіа перевіряються окремо. [Jellyfin SDK](https://kotlin-sdk.jellyfin.org/), [авторизація](https://kotlin-sdk.jellyfin.org/guide/authentication.html) |

Статуси NOT_CONFIRMED не означають, що приватних партнерських API не існує або що провайдер відмовив проєкту. Вони означають відсутність підтвердженого контракту й доступу для VIRA в межах цього дослідження.

## Три способи інтеграції

**Зовнішнє відкриття.** Користувач явно відкриває офіційну сторінку й входить у середовищі сервісу. Android документує `ACTION_VIEW` для HTTPS URL, але конкретний обробник Quest і повернення до VIRA ще потребують перевірки. Не закладати невідомі package IDs, custom URI чи відповідність ID демотайтлу реальному епізоду. [Android intents](https://developer.android.com/guide/components/intents-common#Browser).

Зовнішній плеєр не є відео на фіксованому екрані VIRA. Без офіційних callback/API не обіцяємо керування ним із картки, час перегляду, автоматичне зарахування повної серії або co-watch. Повернення з браузера саме по собі не зараховує епізод. Це обмеження запропонованого адаптера, а не нова поведінка вже встановленого APK.

**Внутрішній дозволений потік.** Власний або ліцензований MP4/HLS/DASH надходить через відомий адаптер на головний екран. Спершу потрібні джерело, підтверджені права й стабільний ID епізоду. Рекомендовано почати з одного HTTPS MP4; Android радить Media3 для нового playback-коду, проте міграція нинішнього MediaPlayer й версія залежності є окремою технічною зміною. Жодну залежність T070 не оновлює.

**Партнерський захищений потік.** Потрібні офіційний контракт авторизації, entitlement, manifest/license endpoints, умови використання й захищений шлях виводу. Самої підписки чи підтримки Widevine недостатньо як доказу дозволу на стороннє вбудовування. Перевірені [Netflix Terms](https://help.netflix.com/legal/termsofuse) передбачають особисте некомерційне використання й обмеження обходу захисту та автоматизованого доступу. Повний актуальний текст англійських умов Crunchyroll інструмент не отримав, тому категоричного висновку про дозвіл на native embedding не робимо.

## DRM: можливість SDK і доступ до сервісу

Spatial SDK підтримує захищені панелі. Meta документує secure layers для DRM. Android Media3 підтримує Widevine, але схема шифрування, контейнер, decoder і secure output мають бути сумісними. Це не надає доступу до каталогу конкретного сервісу. [Meta DRM](https://developers.meta.com/vr/documentation/spatial-sdk/spatial-sdk-2dpanel-drm/), [Android DRM](https://developer.android.com/media/media3/exoplayer/drm), [secure output](https://developer.android.com/media/media3/ui/surface), [Widevine](https://developers.google.com/widevine/drm/overview).

На нашому точному коміті `822f4d1ec967d4975ae8db386502b60cda5218c0` офіційний `PremiumMediaSample` використовує direct-to-surface з `VideoSurfacePanelRegistration` та `MediaPanelRenderOptions(isDRM = …)` за наявності `drmLicenseUrl`. Це доказ шляху SDK 0.15.0, а не перевірка Netflix/Crunchyroll у VIRA. [ExoVideoEntity.kt, рядки 197–243](https://github.com/meta-quest/Meta-Spatial-SDK-Samples/blob/822f4d1ec967d4975ae8db386502b60cda5218c0/PremiumMediaSample/app/src/main/java/com/meta/spatial/samples/premiummediasample/entities/ExoVideoEntity.kt#L197).

Sample декларує Media3 1.4.1 і videoextensions 0.2.2. Це зафіксовані upstream-версії для довідки, а не вибрана нова залежність VIRA. HLS-модуль у перевіреному переліку sample не заявлено. [Залежності, рядки 90–96](https://github.com/meta-quest/Meta-Spatial-SDK-Samples/blob/822f4d1ec967d4975ae8db386502b60cda5218c0/PremiumMediaSample/app/build.gradle.kts#L90).

Не копіювати sample без адаптації. `ExoPlayerExt.kt:167` розпізнає DASH за закінченням URI, а `183–188` журналює media/license URL. Для VIRA потрібні явний MIME/тип джерела, коректна обробка signed URL та редагування секретів у логах. [Точний файл](https://github.com/meta-quest/Meta-Spatial-SDK-Samples/blob/822f4d1ec967d4975ae8db386502b60cda5218c0/PremiumMediaSample/app/src/main/java/com/meta/spatial/samples/premiummediasample/entities/ExoPlayerExt.kt#L161).

Поточна VIRA використовує raw-only `MediaPlayer` та звичайні налаштування video panel без явно ввімкненого DRM. Її secure playback, Widevine security level, output protection і сумісність із конкретним ліцензійним сервером не перевірені. Чорний кадр у screenshot для захищеного шару не є сам по собі відмовою відтворення й не дає підстав вимикати захист.

## Контракт custom provider — пропозиція

Це вимоги до майбутнього адаптера, а не вигадані SDK-класи чи вже наявний endpoint.

- Окремі capabilities каталогу, списку епізодів, доступності, external open, internal playback, DRM, seek, subtitles, completion та co-watch. Наявність URL не робить усі можливості доступними.
- Стабільні provider/title/episode IDs, account scope, територіальні/часові обмеження та перевірка entitlement перед відтворенням.
- Playback descriptor із mode, явним MIME, URI, expiry, відомими заголовками та ознакою preview/full episode. Повна серія зараховується за T052 лише з власного достовірного playback lifecycle.
- HTTPS за замовчуванням. Приватний сервер користувача налаштовується явно. Обмежити redirects, час і розмір відповідей; не пересилати Authorization на інший origin чи при downgrade. Перевіряти також URL вкладених manifests/segments/keys/license, якщо їх завантажує адаптер.
- Авторизація лише через документований механізм сервера. Токени зберігати захищено; URL із секретами, cookies і заголовки не потрапляють у логи, docs чи snapshots. Remote Kotlin/JS не виконується.
- Стани `unconfigured`, `auth_required`, `expired`, `unavailable`, `unsupported`, `offline`, `rate_limited` показують реальну причину без неперевіреного fallback на чужий потік.

## Наступний етап після підтвердження джерел

Запропоновано T071: погоджений доступ до метаданих і невеликий адаптер вибраних тайтлів для Discover/Search та Anime Card, зі збереженням деталей старих записів Library. Заміна демокаталогу сама по собі створила б синтетичні серії через `demoEpisodesFor` і могла б втратити відображення старих записів. Потрібні окремі provider IDs, реальне джерело серій або стан відсутності даних, фонове завантаження й кеш. Читання HTML не називати офіційним API. Озвучення й відео мають окремі можливості..

Для ізольованої перевірки внутрішнього мережевого плеєра можливий власний/ліцензований **HTTPS MP4**, без DRM. Це резервний технічний тест, а не заміна обраних власником джерел. Оригінальний ролик T050 можна розмістити лише на явно погодженому endpoint; T070 нічого не розміщує й не публікує.

| Тест | Необхідний доказ |
|---|---|
| Право на матеріал і доступ | Власне/погоджене джерело, стабільний ID, відомий preview/full статус; секрети поза звітом |
| HTTPS MP4 на Quest | Prepare, перший кадр і звук, play/pause/seek, buffering, network loss/recovery, завершення й повернення картки; фіксований екран збережено |
| Expiry/401/403 і відсутній матеріал | Описаний стан без витоку URL credentials і без автоматичного обходу авторизації |
| HLS/DASH, tracks | Окрема перевірка manifests, адаптації, кодеків, аудіо й субтитрів; не переносити результат MP4 автоматично |
| Дозволений DRM asset | Погоджений тестовий контент і license flow, secure layer/decoder, user-visible кадр, pause/seek/lifecycle; без підміни на readable texture |
| Netflix external | Офіційний Quest Browser, самостійний вхід власника, доступний матеріал, явне повернення до VIRA; без автоматичного прогресу |
| Crunchyroll external/app | Повторно перевірений реліз/сумісність і доступ власника, далі окремий Quest-тест |

Co-watch T080 залишається відкладеним: немає реалізованої акаунтної синхронізації та підтвердженого live provider playback. Кожен учасник має власний доступ до матеріалу; синхронізація команд не надає права ретранслювати відео.
