# Перевірки

З каталогу `app/`:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Модульні тести перевіряють каталог, епізоди, шарнір, drop-зону й зарахування прогресу. Тести Room на пристрої запускати через `:app:connectedDebugAndroidTest` після підключення авторизованого Quest.

## Майбутні рівні
Unit: прогрес/статус/rating, duplicate add, event ownership, state transitions, stale network response.
Integration: Room reopen, migrations, provider parsing/expiry, cache behavior, auth isolation.
Device: launch, render, input, reading comfort, hands/controllers, video lifecycle, resource measurements.
Co-watch: два незалежні пристрої, join/seek/reconnect/host leave, різні доступи до джерел.

## Регресійний набір із попередньої ітерації
R01 Duplicate Add не скидає статус/прогрес.
R02 Poke/point-pinch/controller не генерують подвійний click.
R03 Інша рука не завершує touch/grab першої.
R04 Tracking loss/app pause не активує кнопку й не виконує drop.
R05 Card body/leaves/UI рухаються узгоджено, без text-through-back.
R06 Unfold не перекриває дії collider-ом.
R07 Hidden/docked UI не ловить input.
R08 Повторне opening/closing не залишає observer/entities.
R09 Search response старого запиту не замінює новий.
R10 Failed next-page request не стирає вже показані результати.
R11 Reopen DB підтверджує CRUD після restart.
R12 Playback recall не перезапускає відео; episode switch зберігає стару позицію.

## Формат доказу
Дата/commit; команда; environment; exit code; pass/fail/blocked/not_run; log path;
який сценарій підтверджено та що не перевірено. Скриншот без device/build ID не заміняє журнал тесту.
Збірка test APK ≠ execution instrumentation. UI render ≠ touch verification. Demo video ≠ Netflix support.

## Coursework gate
Compose layouts, explicit/implicit intents і sensor use потребують зіставлення з реальною рубрикою.
Відповідь викладача не надана; не писати «вимога виконана» лише тому, що працює headset tracking.
