# Технічна база

Android-проєкт у [app/](../app/) має один модуль `:app`. Він використовує Meta Spatial SDK 0.15.0, Gradle 9.4.1, AGP 8.11.1, Kotlin 2.1.0, JDK 17 та Android API 34. Точний upstream-коміт записаний у [lock-файлі](../config/upstream.lock.json).

## Межі компонентів
UI → ViewModel/state → domain command → repository → Room/network → оновлений стан.
Spatial event перекладається в ту саму команду, що й кнопка; Room не залежить від collider чи mesh.
Fold/unfold/docking — transient spatial state; Library status/rating/progress — persisted user data.
Poster loading/cache — окремий concern; image failures не блокують CRUD.
Provider adapter — окремий від AniList/catalog і окремий від account sync.

## Як використовувати samples
StarterSample — launch/scene foundation. Object3DSampleIsdk — вивчити взаємодії.
MediaPlayerSample / PremiumMediaSample — дослідити video/DRM capabilities, не вважати готовим Netflix integration.
AnimationsSample — шаблони анімації. PhysicsSample — тільки коли потрібні gravity/collisions.
Факт наявності прикладу не доводить сумісність конкретної інтеграції з усіма версіями SDK.

## Відкладено
Backend, auth provider, provider APIs, schema migrations, refresh rate та resource budgets
визначаються окремими рішеннями. Supabase був попереднім напрямом, але не є вже налаштованим сервісом.
