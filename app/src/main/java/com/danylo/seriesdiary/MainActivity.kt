package com.danylo.seriesdiary

import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeriesDiaryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RootNavigation()
                }
            }
        }
    }
}

@Composable
fun RootNavigation() {
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
            MainScreenWithTabs()
        }
    }
}

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
            label = {
                Text(
                    text = "Ваше ім'я",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onStartClick(name) },
            enabled = name.isNotBlank()
        ) {
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
    SeriesDiaryTheme {
        OnboardingScreen(onStartClick = {})
    }
}

@Composable
fun MainScreenWithTabs() {
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
                            launchSingleTop = true
                            restoreState = true
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
                            launchSingleTop = true
                            restoreState = true
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
                            launchSingleTop = true
                            restoreState = true
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
            composable("tab_list") { ListTab(bottomNavController) }
            composable("tab_grid") { GridTab(bottomNavController) }
            composable("tab_settings") { SettingsTab() }

            composable("add_series") {
                AddSeriesScreen(onDone = { bottomNavController.popBackStack() })
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

// ─── Offline banner ───

@Composable
private fun OfflineBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = "Офлайн",
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Офлайн-режим: показано кешовані дані",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

// ─── Error view ───

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Помилка",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Повторити", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ─── Empty view ───

@Composable
private fun EmptyView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = "Порожньо",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── List Tab ───

@Composable
fun ListTab(navController: NavHostController, viewModel: ListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState is ListUiState.Success && (uiState as ListUiState.Success).isFromCache) {
                OfflineBanner()
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = showOnlyFavorites,
                        onCheckedChange = { viewModel.toggleFavorites(it) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Показувати лише улюблені",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (val state = uiState) {
                    is ListUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is ListUiState.Success -> {
                        if (state.series.isEmpty()) {
                            Box(modifier = Modifier.weight(1f)) {
                                EmptyView("Немає серіалів. Додайте перший через кнопку +")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(state.series, key = { it.id }) { series ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable(enabled = !isMutating) {
                                                navController.navigate("details/${series.id}")
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = series.title,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Рік: ${series.releaseYear}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.toggleFavorite(series) },
                                                enabled = !isMutating
                                            ) {
                                                Icon(
                                                    imageVector = if (series.isFavorite) Icons.Default.Star
                                                    else Icons.Default.StarBorder,
                                                    contentDescription = "Улюблене",
                                                    tint = if (series.isFavorite) RatingHigh
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteSeries(series.id) },
                                                enabled = !isMutating
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Видалити",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is ListUiState.Error -> {
                        Box(modifier = Modifier.weight(1f)) {
                            ErrorView(state.message, onRetry = { viewModel.refresh() })
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { navController.navigate("add_series") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Додати серіал")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(name = "ListTab – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "ListTab – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun ListTabPreview() {
    SeriesDiaryTheme {
        ListTab(navController = rememberNavController())
    }
}

// ─── Grid Tab ───

@Composable
fun GridTab(navController: NavHostController, viewModel: GridViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortByRating by viewModel.sortByRating.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState is GridUiState.Success && (uiState as GridUiState.Success).isFromCache) {
            OfflineBanner()
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Сортувати за:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = !sortByRating,
                    onClick = { viewModel.setSortByRating(false) },
                    label = { Text("Алфавітом", style = MaterialTheme.typography.labelMedium) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = sortByRating,
                    onClick = { viewModel.setSortByRating(true) },
                    label = { Text("Рейтингом", style = MaterialTheme.typography.labelMedium) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is GridUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GridUiState.Success -> {
                    if (state.series.isEmpty()) {
                        EmptyView("Немає серіалів")
                    } else {
                        LazyVerticalGrid(columns = GridCells.Fixed(2)) {
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
                                        Text(
                                            text = series.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Рейтинг: ${series.rating}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ratingColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is GridUiState.Error -> {
                    ErrorView(state.message, onRetry = { viewModel.refresh() })
                }
            }
        }
    }
}

@Preview(name = "GridTab – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "GridTab – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun GridTabPreview() {
    SeriesDiaryTheme {
        GridTab(navController = rememberNavController())
    }
}

// ─── Details Screen ───

@Composable
fun DetailsScreen(seriesId: String, onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val factory = remember { DetailsViewModel.Factory(application, seriesId) }
    val viewModel: DetailsViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState is DetailsUiState.Success && (uiState as DetailsUiState.Success).isFromCache) {
            OfflineBanner()
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = onBack) {
                Text(text = "Назад", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is DetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
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

                    Text(
                        text = series.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Рік випуску: ${series.releaseYear}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Статус: ${seriesStatus.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                    Text(
                        text = "Рейтинг: ${series.rating}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ratingColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Експертна думка: ${state.extraInfo}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Опис: Чудовий серіал, який варто подивитись кожному. Відстежуйте свої епізоди тут!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                is DetailsUiState.Error -> {
                    ErrorView(state.message, onRetry = { viewModel.loadDetails() })
                }
            }
        }
    }
}

@Preview(name = "Details – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Details – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun DetailsScreenPreview() {
    SeriesDiaryTheme {
        DetailsScreen(seriesId = "1", onBack = {})
    }
}

// ─── Add Series Screen ───

@Composable
fun AddSeriesScreen(
    viewModel: AddSeriesViewModel = viewModel(),
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(SeriesStatus.CONTINUING) }
    var rating by remember { mutableStateOf("") }
    var statusMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AddSeriesUiState.Saved) {
            viewModel.reset()
            onDone()
        }
    }

    val isSaving = uiState is AddSeriesUiState.Saving
    val yearInt = year.toIntOrNull()
    val ratingDouble = rating.replace(',', '.').toDoubleOrNull()
    val isValid = title.isNotBlank() &&
            yearInt != null && yearInt in 1900..2100 &&
            ratingDouble != null && ratingDouble in 0.0..10.0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone, enabled = !isSaving) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Новий серіал",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Назва") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = year,
            onValueChange = { year = it.filter { ch -> ch.isDigit() } },
            label = { Text("Рік випуску") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = rating,
            onValueChange = { rating = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
            label = { Text("Рейтинг (0–10)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Статус",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { if (!isSaving) statusMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(status.description, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = statusMenuExpanded,
                onDismissRequest = { statusMenuExpanded = false }
            ) {
                SeriesStatus.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.description) },
                        onClick = {
                            status = option
                            statusMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.save(
                    title = title.trim(),
                    releaseYear = yearInt ?: 0,
                    status = status.name,
                    rating = ratingDouble ?: 0.0
                )
            },
            enabled = isValid && !isSaving,
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
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(name = "Add – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Add – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun AddSeriesScreenPreview() {
    SeriesDiaryTheme {
        AddSeriesScreen(onDone = {})
    }
}

// ─── Settings Tab ───

@Composable
fun SettingsTab(viewModel: SettingsViewModel = viewModel()) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val sortByRating by viewModel.defaultSortByRating.collectAsStateWithLifecycle()
    val showEnded by viewModel.showEndedSeries.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Налаштування",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Інформація про додаток",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Назва: Series Diary",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Версія: 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { viewModel.updateName(it) },
            label = {
                Text(
                    text = "Ваше ім'я",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Параметри відображення",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Сортування за рейтингом",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = sortByRating,
                onCheckedChange = { viewModel.setDefaultSortByRating(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Показувати завершені серіали",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = showEnded,
                onCheckedChange = { viewModel.setShowEndedSeries(it) }
            )
        }
    }
}

@Preview(name = "Settings – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Settings – Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun SettingsTabPreview() {
    SeriesDiaryTheme {
        SettingsTab()
    }
}
