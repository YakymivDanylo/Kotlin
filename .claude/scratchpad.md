## Feature: Лабораторна робота №10 — Адаптивні екрани + форма з валідацією

## Branch: production

## Status: complete

## Plan

### Wave 1
- [x] Slice 1: WindowSizeClass dep + adaptive ListTab (two-pane на Expanded)
- [x] Slice 2: Adaptive GridTab (2/3/4 колонки за Compact/Medium/Expanded)
- [x] Slice 3: Розширена форма — 5 полів різних типів (TextField, Number, ExposedDropdown, Switch, Slider), секції
- [x] Slice 4: Валідація on-blur, повідомлення під полем, кнопка disabled (derivedStateOf)
- [x] Slice 5: FocusRequester Next/Done, imePadding, keyboard dismiss via pointerInput
- [x] Slice 6: Adaptive форма — max 600dp centered на планшеті

## Completed
- Усі 6 слайсів реалізовано в одному наборі файлів
- build.gradle.kts: додано material3-window-size-class
- MainActivity.kt: повна переробка з адаптивністю і формою
- ViewModels.kt: AddSeriesViewModel.save() отримав isFavorite
- SeriesRepository.kt: createSeries() зберігає isFavorite локально

## Context
- Поля форми: Назва (text+min2), Рік (number+range), Статус (ExposedDropdownMenuBox), Рейтинг (Slider 0-10), Кількість сезонів (number+range), Улюблений (Switch), IMDb URL (regex), Коментар (multiline)
- Regex: IMDB_URL_REGEX для https://www.imdb.com/title/ttXXXXXXX
- Валідація: on-blur через onFocusChanged, кнопка disabled поки isFormValid=false
- Focus chain: title→year→seasons→imdb→comment(Done)
- Tablet: two-pane ListTab, 4 стовпці GridTab, форма 600dp centered
- Previews: phone/dark + tablet для кожного екрану

## Blockers
- Компіляція через gradlew в WSL неможлива (Windows-бінарники SDK)
- Тестувати через Android Studio безпосередньо на Windows
