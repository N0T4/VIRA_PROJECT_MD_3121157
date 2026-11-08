# Асети та підготовка моделей
## Прийнято
Моделі, інтерфейс і анімації можна оновлювати окремо.
Концепт-картинка не є glTF, rig, texture atlas чи готовою анімацією.

## Місця
`assets/source/` — вихідні ручні файли; `assets/exports/` — погоджені експорти;
`assets/placeholders/` — явні тимчасові заміни; `assets/manifest.json` — статус і відповідальний.
Великі вихідні файли ігноруються Git за замовчуванням; зберігати приватно/LFS після окремого налаштування.
Ніколи не замінювати ручний оригінал згенерованим файлом із тим самим ім'ям без дозволу.

## Контракт експорту (пропозиція до технічного spike)
Одиниці: метри. Вказувати pivot, orientation, dimensions та scene placement.
Для unfolding card: root + явний hinge pivot + окремі leaves; області UI й collider описуються окремо.
Для аватарів/маскота: rig, animation clip names та neutral pose документуються до інтеграції.
glTF/GLB pipeline перевірити в обраному pinned sample; не вважати довільний Blender export сумісним автоматично.
Ресурсні бюджети triangle/material/texture визначити за вимірюванням, не за вигаданим «максимумом Quest».

## Реєстр для кожного ресурсу
ID, роль, автор/джерело, license/permission, version, checksum,
working/export paths, scale/pivot, dependencies, replaceability, acceptance status.
Невідомий license позначити UNKNOWN, не переліцензовувати весь набір під MIT.

## Замінність
UI layouts і motion parameters не захардкоджувати в моделях.
Анімації звертаються до стабільних semantic anchors, не до випадкових індексів об'єктів.
Після заміни моделі перевірити hinge, hit areas, depth occlusion, hidden collision і frame cost.
