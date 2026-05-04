@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)

package com.danylo.seriesdiary

import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.danylo.seriesdiary.data.TvSeriesEntity
import com.danylo.seriesdiary.model.SeriesStatus
import com.danylo.seriesdiary.ui.theme.SeriesDiaryTheme
import com.danylo.seriesdiary.viewmodel.*
import com.example.compose.*

// ─── IMDb URL validation regex ───
private val IMDB_URL_REGEX = Regex("""^https?://www\.imdb\.com/title/tt\d{7,8}/?$""")

// ─── Validation helpers ───

private fun validateTitle(title: String): String? = when {
    title.isBlank() -> "Назва не може бути порожньою"
    title.trim().length < 2 -> "Назва має містити щонайменше 2 символи"
    else -> null
}

private fun validateYear(year: String): String? {
    val y = year.toIntOrNull()
    return when {
        year.isBlank() -> "Рік випуску є обов'язковим"
        y == null -> "Введіть коректний рік"
        y < 1900 || y > 2100 -> "Рік має бути від 1900 до 2100"
        else -> null
    }
}

private fun validateStatus(status: SeriesStatus?): String? =
    if (status == null) "Виберіть статус серіалу" else null

private fun validateSeasons(seasons: String): String? {
    val s = seasons.toIntOrNull()
    return when {
        seasons.isBlank() -> "Кількість сезонів є обов'язковою"
        s == null -> "Введіть ціле число"
        s < 1 || s > 50 -> "Кількість сезонів: від 1 до 50"
        else -> null
    }
}

private fun validateImdbUrl(url: String): String? {
    if (url.isBlank()) return null
    return if (IMDB_URL_REGEX.matches(url)) null
    else "Формат: https://www.imdb.com/title/ttXXXXXXX"
}

// ─── Activity ───

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            SeriesDiaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootNavigation(widthSizeClass = windowSizeClass.widthSizeClass)
                }
            }
        }
    }
}

// ─── Navigation ───

@Composable
fun RootNavigation(widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact) {
    val rootNavController = rememberNavController()
    val onboardingVm: OnboardingViewModel = viewModel()
    val savedName by onboardingVm.savedName.collectAsStateWithLifecycle()
    val isLoading by onboardingVm.isLoading.collectAsStateWithLifecycle()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDest = if (savedName.isNotBlank()) "main" else "onboarding"

    NavHost(navController = rootNavController, startDestination = startDest) {
        composable("onboarding") {
            OnboardingScreen(
                onStartClick = { name ->
                    onboardingVm.saveName(name)
                    rootNavController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreenWithTabs(widthSizeClass = widthSizeClass)
        }
    }
}

// ─── Onboarding ───

@Composable
fun OnboardingScreen(onStartClick: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = "Logo",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Щоденник Серіалів",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Ваше ім'я", style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onStartClick(name) }, enabled = name.isNotBlank()) {
            Text(
                text = if (name.isNotBlank()) "Привіт, $name! Розпочати" else "Розпочати",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(name = "Onboarding – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Onboarding – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun OnboardingScreenPreview() {
    SeriesDiaryTheme { OnboardingScreen(onStartClick = {}) }
}

// ─── Main tabs scaffold ───

@Composable
fun MainScreenWithTabs(widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact) {
    val bottomNavController = rememberNavController()
    var currentTab by remember { mutableStateOf("tab_list") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Список") },
                    label = { Text("Список", style = MaterialTheme.typography.labelSmall) },
                    selected = currentTab == "tab_list",
                    onClick = {
                        currentTab = "tab_list"
                        bottomNavController.navigate("tab_list") {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Плитка") },
                    label = { Text("Плитка", style = MaterialTheme.typography.labelSmall) },
                    selected = currentTab == "tab_grid",
                    onClick = {
                        currentTab = "tab_grid"
                        bottomNavController.navigate("tab_grid") {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Налаштування") },
                    label = { Text("Налаштування", style = MaterialTheme.typography.labelSmall) },
                    selected = currentTab == "tab_settings",
                    onClick = {
                        currentTab = "tab_settings"
                        bottomNavController.navigate("tab_settings") {
                            popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "tab_list",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("tab_list") { ListTab(bottomNavController, widthSizeClass = widthSizeClass) }
            composable("tab_grid") { GridTab(bottomNavController, widthSizeClass = widthSizeClass) }
            composable("tab_settings") { SettingsTab() }
            composable("add_series") {
                AddSeriesScreen(
                    onDone = { bottomNavController.popBackStack() },
                    widthSizeClass = widthSizeClass
                )
            }
            composable(
                route = "details/{seriesId}",
                arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString("seriesId") ?: ""
                DetailsScreen(id, onBack = { bottomNavController.popBackStack() })
            }
        }
    }
}

// ─── Shared helper composables ───

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Офлайн-режим: показано кешовані дані",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Повторити", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun EmptyView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
}

// ─── Slice 1: Adaptive List Tab ───

@Composable
fun ListTab(
    navController: NavHostController,
    viewModel: ListViewModel = viewModel(),
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedSeriesId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState is ListUiState.Success && (uiState as ListUiState.Success).isFromCache) {
                OfflineBanner()
            }

            if (isExpanded) {
                // ── Two-pane layout for tablets ──
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

                    // Left pane — series list (40%)
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = showOnlyFavorites, onCheckedChange = { viewModel.toggleFavorites(it) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Лише улюблені", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        when (val state = uiState) {
                            is ListUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                            is ListUiState.Success -> {
                                if (state.series.isEmpty()) {
                                    EmptyView("Немає серіалів")
                                } else {
                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        items(state.series, key = { it.id }) { series ->
                                            val isSelected = series.id == selectedSeriesId
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .clickable(enabled = !isMutating) { selectedSeriesId = series.id },
                                                colors = if (isSelected)
                                                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                                else CardDefaults.cardColors()
                                            ) {
                                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(series.title, style = MaterialTheme.typography.titleMedium)
                                                        Text("Рік: ${series.releaseYear}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    IconButton(onClick = { viewModel.toggleFavorite(series) }, enabled = !isMutating) {
                                                        Icon(
                                                            if (series.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                            contentDescription = null,
                                                            tint = if (series.isFavorite) RatingHigh else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    IconButton(onClick = { viewModel.deleteSeries(series.id) }, enabled = !isMutating) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is ListUiState.Error -> ErrorView(state.message, onRetry = { viewModel.refresh() })
                        }
                    }

                    VerticalDivider()

                    // Right pane — details or placeholder (60%)
                    Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                        if (selectedSeriesId != null) {
                            key(selectedSeriesId) {
                                DetailsScreen(
                                    seriesId = selectedSeriesId!!,
                                    onBack = { selectedSeriesId = null }
                                )
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Виберіть серіал зі списку", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Single-pane for phones ──
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = showOnlyFavorites, onCheckedChange = { viewModel.toggleFavorites(it) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Показувати лише улюблені", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    when (val state = uiState) {
                        is ListUiState.Loading -> Box(Modifier.fillMaxSize().weight(1f), Alignment.Center) { CircularProgressIndicator() }
                        is ListUiState.Success -> {
                            if (state.series.isEmpty()) {
                                Box(modifier = Modifier.weight(1f)) { EmptyView("Немає серіалів. Додайте перший через кнопку +") }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(state.series, key = { it.id }) { series ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable(enabled = !isMutating) { navController.navigate("details/${series.id}") }
                                        ) {
                                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(series.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                                                    Text("Рік: ${series.releaseYear}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                IconButton(onClick = { viewModel.toggleFavorite(series) }, enabled = !isMutating) {
                                                    Icon(
                                                        if (series.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                        contentDescription = null,
                                                        tint = if (series.isFavorite) RatingHigh else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(onClick = { viewModel.deleteSeries(series.id) }, enabled = !isMutating) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is ListUiState.Error -> Box(modifier = Modifier.weight(1f)) { ErrorView(state.message, onRetry = { viewModel.refresh() }) }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("add_series") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Додати серіал")
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Preview(name = "ListTab – Phone", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "ListTab – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun ListTabPreview() {
    SeriesDiaryTheme { ListTab(navController = rememberNavController()) }
}

@Preview(name = "ListTab – Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun ListTabTabletPreview() {
    SeriesDiaryTheme { ListTab(navController = rememberNavController(), widthSizeClass = WindowWidthSizeClass.Expanded) }
}

// ─── Slice 2: Adaptive Grid Tab ───

@Composable
fun GridTab(
    navController: NavHostController,
    viewModel: GridViewModel = viewModel(),
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortByRating by viewModel.sortByRating.collectAsStateWithLifecycle()

    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 4
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState is GridUiState.Success && (uiState as GridUiState.Success).isFromCache) {
            OfflineBanner()
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Сортувати за:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = !sortByRating, onClick = { viewModel.setSortByRating(false) }, label = { Text("Алфавітом", style = MaterialTheme.typography.labelMedium) })
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(selected = sortByRating, onClick = { viewModel.setSortByRating(true) }, label = { Text("Рейтингом", style = MaterialTheme.typography.labelMedium) })
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is GridUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is GridUiState.Success -> {
                    if (state.series.isEmpty()) {
                        EmptyView("Немає серіалів")
                    } else {
                        LazyVerticalGrid(columns = GridCells.Fixed(columns)) {
                            items(state.series, key = { it.id }) { series ->
                                Card(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .aspectRatio(1f)
                                        .clickable { navController.navigate("details/${series.id}") }
                                ) {
                                    val ratingColor = when {
                                        series.rating >= 8.5 -> RatingHigh
                                        series.rating < 6.0 -> RatingLow
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(series.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                                        Text("Рейтинг: ${series.rating}", style = MaterialTheme.typography.bodySmall, color = ratingColor)
                                    }
                                }
                            }
                        }
                    }
                }
                is GridUiState.Error -> ErrorView(state.message, onRetry = { viewModel.refresh() })
            }
        }
    }
}

@Preview(name = "GridTab – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "GridTab – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun GridTabPreview() {
    SeriesDiaryTheme { GridTab(navController = rememberNavController()) }
}

@Preview(name = "GridTab – Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun GridTabTabletPreview() {
    SeriesDiaryTheme { GridTab(navController = rememberNavController(), widthSizeClass = WindowWidthSizeClass.Expanded) }
}

// ─── Details Screen ───

@Composable
fun DetailsScreen(seriesId: String, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val factory = remember(seriesId) { DetailsViewModel.Factory(application, seriesId) }
    val viewModel: DetailsViewModel = viewModel(key = seriesId, factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState is DetailsUiState.Success && (uiState as DetailsUiState.Success).isFromCache) {
            OfflineBanner()
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = onBack) { Text("Назад", style = MaterialTheme.typography.labelLarge) }
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is DetailsUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                is DetailsUiState.Success -> {
                    val series = state.series
                    val seriesStatus = series.toSeriesStatus()
                    val statusColor = when (seriesStatus) {
                        SeriesStatus.CONTINUING -> StatusWatching
                        SeriesStatus.ENDED -> StatusCompleted
                        SeriesStatus.UPCOMING -> StatusPlanned
                        SeriesStatus.UNKNOWN -> StatusDropped
                    }
                    val ratingColor = when {
                        series.rating >= 8.5 -> RatingHigh
                        series.rating < 6.0 -> RatingLow
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(series.title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Рік випуску: ${series.releaseYear}", style = MaterialTheme.typography.bodyMedium)
                    Text("Статус: ${seriesStatus.description}", style = MaterialTheme.typography.bodyMedium, color = statusColor)
                    Text("Рейтинг: ${series.rating}", style = MaterialTheme.typography.bodyMedium, color = ratingColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Експертна думка: ${state.extraInfo}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Опис: Чудовий серіал, який варто подивитись кожному. Відстежуйте свої епізоди тут!", style = MaterialTheme.typography.bodyLarge)
                }
                is DetailsUiState.Error -> ErrorView(state.message, onRetry = { viewModel.loadDetails() })
            }
        }
    }
}

@Preview(name = "Details – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Details – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun DetailsScreenPreview() {
    SeriesDiaryTheme { DetailsScreen(seriesId = "1", onBack = {}) }
}

// ─── Slices 3–6: Extended Add Series Form ───

@Composable
fun AddSeriesScreen(
    viewModel: AddSeriesViewModel = viewModel(),
    onDone: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val isSaving = uiState is AddSeriesUiState.Saving

    // ── Form state ──
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<SeriesStatus?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    var ratingSlider by remember { mutableStateOf(5.0f) }
    var numberOfSeasons by remember { mutableStateOf("") }
    var imdbUrl by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    // ── Touched state (triggers on-blur validation) ──
    var titleTouched by remember { mutableStateOf(false) }
    var yearTouched by remember { mutableStateOf(false) }
    var statusTouched by remember { mutableStateOf(false) }
    var seasonsTouched by remember { mutableStateOf(false) }
    var imdbUrlTouched by remember { mutableStateOf(false) }

    // ── Per-field error messages ──
    val titleError = if (titleTouched) validateTitle(title) else null
    val yearError = if (yearTouched) validateYear(year) else null
    val statusError = if (statusTouched) validateStatus(status) else null
    val seasonsError = if (seasonsTouched) validateSeasons(numberOfSeasons) else null
    val imdbUrlError = if (imdbUrlTouched) validateImdbUrl(imdbUrl) else null

    // ── Overall form validity (derivedStateOf avoids redundant recompositions) ──
    val isFormValid by remember {
        derivedStateOf {
            validateTitle(title) == null &&
            validateYear(year) == null &&
            validateStatus(status) == null &&
            validateSeasons(numberOfSeasons) == null &&
            validateImdbUrl(imdbUrl) == null
        }
    }

    // ── Focus management (Slice 5) ──
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleFocus = remember { FocusRequester() }
    val yearFocus = remember { FocusRequester() }
    val seasonsFocus = remember { FocusRequester() }
    val imdbFocus = remember { FocusRequester() }
    val commentFocus = remember { FocusRequester() }

    LaunchedEffect(uiState) {
        if (uiState is AddSeriesUiState.Saved) { viewModel.reset(); onDone() }
    }

    // ── Adaptive wrapper: centered max-width on tablet (Slice 6) ──
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (isExpanded) Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    else Modifier.fillMaxWidth()
                )
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onDone, enabled = !isSaving) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("Новий серіал", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            }

            // ── Section 1: Основна інформація ──
            SectionHeader("Основна інформація")

            // Назва (текстове поле, валідація: не порожнє + мін. 2 символи)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Назва серіалу *") },
                isError = titleError != null,
                supportingText = titleError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus)
                    .onFocusChanged { if (!it.isFocused) titleTouched = true },
                singleLine = true,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { yearFocus.requestFocus() })
            )
            Spacer(Modifier.height(8.dp))

            // Рік випуску (числове поле, валідація: діапазон 1900–2100)
            OutlinedTextField(
                value = year,
                onValueChange = { year = it.filter(Char::isDigit) },
                label = { Text("Рік випуску *") },
                isError = yearError != null,
                supportingText = yearError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(yearFocus)
                    .onFocusChanged { if (!it.isFocused) yearTouched = true },
                singleLine = true,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { seasonsFocus.requestFocus() })
            )
            Spacer(Modifier.height(8.dp))

            // Статус (випадаючий список, валідація: вибір зроблено)
            ExposedDropdownMenuBox(
                expanded = statusMenuExpanded,
                onExpandedChange = { if (!isSaving) statusMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = status?.description ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Статус *") },
                    placeholder = { Text("Виберіть статус") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusMenuExpanded) },
                    isError = statusError != null,
                    supportingText = statusError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .onFocusChanged { if (!it.isFocused) statusTouched = true },
                    enabled = !isSaving
                )
                ExposedDropdownMenu(
                    expanded = statusMenuExpanded,
                    onDismissRequest = { statusMenuExpanded = false }
                ) {
                    SeriesStatus.entries.filter { it != SeriesStatus.UNKNOWN }.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.description) },
                            onClick = {
                                status = option
                                statusMenuExpanded = false
                                statusTouched = true
                            }
                        )
                    }
                }
            }

            // ── Section 2: Оцінка та параметри ──
            SectionHeader("Оцінка та параметри")

            // Рейтинг (повзунок 0.0–10.0)
            Text(
                text = "Рейтинг: ${"%.1f".format(ratingSlider)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Slider(
                value = ratingSlider,
                onValueChange = { ratingSlider = it },
                valueRange = 0f..10f,
                steps = 19,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            // Кількість сезонів (числове поле, валідація: 1–50)
            OutlinedTextField(
                value = numberOfSeasons,
                onValueChange = { numberOfSeasons = it.filter(Char::isDigit) },
                label = { Text("Кількість сезонів *") },
                isError = seasonsError != null,
                supportingText = seasonsError?.let { err -> { Text(err, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(seasonsFocus)
                    .onFocusChanged { if (!it.isFocused) seasonsTouched = true },
                singleLine = true,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { imdbFocus.requestFocus() })
            )
            Spacer(Modifier.height(8.dp))

            // Улюблений (перемикач)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Додати до улюблених", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isFavorite, onCheckedChange = { isFavorite = it }, enabled = !isSaving)
            }

            // ── Section 3: Додаткові відомості ──
            SectionHeader("Додаткові відомості")

            // IMDb URL (валідація за регулярним виразом)
            OutlinedTextField(
                value = imdbUrl,
                onValueChange = { imdbUrl = it },
                label = { Text("IMDb посилання") },
                placeholder = { Text("https://www.imdb.com/title/ttXXXXXXX") },
                isError = imdbUrlError != null,
                supportingText = imdbUrlError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                } ?: { Text("Необов'язково", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(imdbFocus)
                    .onFocusChanged { if (!it.isFocused) imdbUrlTouched = true },
                singleLine = true,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { commentFocus.requestFocus() })
            )
            Spacer(Modifier.height(8.dp))

            // Коментар (текстове поле, багаторядковий)
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Коментар") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(commentFocus),
                minLines = 3,
                maxLines = 5,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                })
            )

            Spacer(Modifier.height(24.dp))

            // Кнопка збереження (заблокована, поки форма невалідна)
            Button(
                onClick = {
                    // позначаємо всі поля як торкнуті перед збереженням
                    titleTouched = true; yearTouched = true; statusTouched = true
                    seasonsTouched = true; imdbUrlTouched = true
                    if (isFormValid && !isSaving) {
                        viewModel.save(
                            title = title.trim(),
                            releaseYear = year.toIntOrNull() ?: 0,
                            status = status!!.name,
                            rating = ratingSlider.toDouble(),
                            isFavorite = isFavorite,
                            numberOfSeasons = numberOfSeasons.toIntOrNull() ?: 1,
                            imdbUrl = imdbUrl.trim(),
                            comment = comment.trim()
                        )
                    }
                },
                enabled = isFormValid && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Зберегти", style = MaterialTheme.typography.labelLarge)
            }

            (uiState as? AddSeriesUiState.Error)?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(error.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(name = "Add – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Add – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun AddSeriesScreenPreview() {
    SeriesDiaryTheme { AddSeriesScreen(onDone = {}) }
}

@Preview(name = "Add – Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun AddSeriesTabletPreview() {
    SeriesDiaryTheme { AddSeriesScreen(onDone = {}, widthSizeClass = WindowWidthSizeClass.Expanded) }
}

// ─── Settings Tab ───

@Composable
fun SettingsTab(viewModel: SettingsViewModel = viewModel()) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val sortByRating by viewModel.defaultSortByRating.collectAsStateWithLifecycle()
    val showEnded by viewModel.showEndedSeries.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Налаштування", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Інформація про додаток", style = MaterialTheme.typography.titleSmall)
                Text("Назва: Series Diary", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Версія: 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { viewModel.updateName(it) },
            label = { Text("Ваше ім'я", style = MaterialTheme.typography.bodyMedium) },
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Параметри відображення", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сортування за рейтингом", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = sortByRating, onCheckedChange = { viewModel.setDefaultSortByRating(it) })
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Показувати завершені серіали", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = showEnded, onCheckedChange = { viewModel.setShowEndedSeries(it) })
        }
    }
}

@Preview(name = "Settings – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Settings – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun SettingsTabPreview() {
    SeriesDiaryTheme { SettingsTab() }
}
