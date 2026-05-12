## Feature: Лабораторна робота №12 — Камера + Геолокація + Permissions

## Branch: production

## Status: complete

## Plan

### Wave 1 (sequential — кожен слайс залежить від попереднього)
- [x] Slice 1: Manifest permissions (CAMERA, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), FileProvider, file_paths.xml, залежності (play-services-location, coil-compose) в build.gradle.kts
- [x] Slice 2: БД міграція v3→v4 — додати photoPath:String? в TvSeriesEntity, DAO updatePhoto, repo updatePhoto + видалення файлу при deleteSeries, збереження photoPath між refresh-ами через previousPhotoPath
- [x] Slice 3: PermissionUtils — openAppSettings(), Composable PermissionGate (3 стани) + LocationPermissionGate для FINE/COARSE
- [x] Slice 4: Інтеграція камери в DetailsScreen — ScenePhotoSection з TakePicture через FileProvider, DetailsViewModel.setPhoto, AsyncImage (coil) + reactive observeLocalChanges
- [x] Slice 5: LocationViewModel — FusedLocationProviderClient.getCurrentLocation з cancellation, LocationUiState (Idle/Loading/Success/Error), HBO HQ (40.7575, -73.9850)
- [x] Slice 6: LocationTab з усіма полями (lat/lon/accuracy/timestamp/distance) + кнопка оновлення + 4та вкладка в BottomNav

## Completed
- AndroidManifest.xml: CAMERA, ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, FileProvider
- res/xml/file_paths.xml: files-path photos/
- build.gradle.kts: play-services-location:21.3.0, coil-compose:2.7.0
- TvSeriesEntity: + photoPath:String?
- AppDatabase: v4 (fallbackToDestructiveMigration)
- TvSeriesDao: updatePhoto(id, path)
- SeriesDto.toEntity: previousPhotoPath параметр
- SeriesRepository: updatePhoto + deletePhotoFile, видалення файлу в deleteSeries
- permissions/PermissionUtils.kt: PermissionGate, LocationPermissionGate, openAppSettings
- media/PhotoStorage.kt: createPhotoFile + uriFor
- location/LocationViewModel.kt: getCurrentLocation + ReferencePoint(HBO HQ)
- DetailsViewModel: setPhoto + observeLocalChanges (реактивне оновлення з Room)
- MainActivity: ScenePhotoSection в DetailsScreen, LocationTab + 4та вкладка в BottomNav

## Context
- Варіант 10 (парний) → Варіант А: камера
- Сценарій фото: скріншот сцени до серіалу (photoPath у TvSeriesEntity)
- Точка відліку для геолокації: HBO HQ, New York (40.7575° N, -73.9850° W)
- Локація — окрема (4-та) вкладка в BottomNav
- Існує MVVM з ЛР №6, Room v3 з ЛР №8 → bump до v4
- Зображення зберігається у filesDir/photos, у БД лише шлях
- Камера через ActivityResultContracts.TakePicture + FileProvider

## Blockers
- Компіляція через gradlew в WSL неможлива — тестувати через Android Studio

## Archive
### ЛР №10 — Адаптивні екрани + форма з валідацією (complete)
- 6 слайсів: WindowSizeClass, adaptive ListTab/GridTab, форма з 5 типами полів, on-blur валідація, FocusRequester chain, adaptive max-width
- Файли: MainActivity.kt, ViewModels.kt, SeriesRepository.kt, build.gradle.kts
