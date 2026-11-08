# Відкриті рішення
Не вимагати відповіді на всі питання перед greybox. Не вигадувати їх як затверджені.

| ID | Питання | Що можна робити до відповіді |
|---|---|---|
| O01 | Точний summon gesture і захист від випадкового спрацювання | Input abstraction + явна тимчасова кнопка; gesture spike |
| O02 | Кількість стулок, шарнір і спосіб unfold | Простий двостулковий greybox, позначений proposal |
| O03 | Docking side, відстані, seated/standing | Recenter/configuration; тест у Quest |
| O04 | Вибір SDK baseline після smoke test | Перевірити pinned candidate, не autoupgrade |
| O05 | Netflix/Crunchyroll access, playback/DRM та дозволені APIs | Capability research і demo player |
| O06 | Формат custom provider config | Схема без виконання стороннього коду; mock adapter |
| O08 | Представлення гостей, голос, ліміт кімнати | Avatar placeholders; не плутати рисунок 2/4 з вимогою |
| O09 | Роль маскота / персоналізація | Модельна специфікація; визначити поведінку маскота |
| O10 | Coursework layouts/sensors/intents acceptance | Вимоги окремо від продуктового UX |
| O11 | Назва нового repo та package ID | Локальний pack; не створювати repo без рішення |
| O12 | Точні design tokens і бюджети продуктивності | Запропоновані числа + вимірювання, не hard limits |

O07 закрито рішенням D013. [Реєстр рішень](../decisions/DECISIONS.md).

T070 уточнив O05/O06, але не закрив їх. Власник підтвердив `https://animego.org/` та `https://animeua.club/`. Публічний HTML метаданих прочитано; погоджений автоматичний доступ, права повторного використання, повні списки серій і playback залишаються відкритими. Документований шлях Netflix і підтримка DRM не підтверджують доступу VIRA до сервісних API. [Матриця T070](16_PROVIDER_CAPABILITIES.md).
