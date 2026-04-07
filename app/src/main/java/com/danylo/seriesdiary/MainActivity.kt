package com.danylo.seriesdiary

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danylo.seriesdiary.ui.theme.SeriesDiaryTheme
import com.danylo.seriesdiary.viewmodel.*

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

    NavHost(navController = rootNavController, startDestination = "onboarding") {
        composable("onboarding") { entry ->
            val savedName = entry.savedStateHandle
                .getStateFlow("userName", "")
                .collectAsStateWithLifecycle()
            OnboardingScreen(
                currentName = savedName.value,
                onEnterNameClick = { rootNavController.navigate("name_input") },
                onStartClick = {
                    rootNavController.navigate("main/${savedName.value}") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("name_input") {
            NameInputScreen(
                onSave = { name ->
                    rootNavController.previousBackStackEntry
                        ?.savedStateHandle?.set("userName", name)
                    rootNavController.popBackStack()
                }
            )
        }

        composable(
            route = "main/{userName}",
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { entry ->
            val userName = entry.arguments?.getString("userName") ?: "Користувач"
            MainScreenWithTabs(userName = userName)
        }
    }
}


@Composable
fun OnboardingScreen(
    currentName: String,
    onEnterNameClick: () -> Unit,
    onStartClick: () -> Unit
) {
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

        Button(onClick = onEnterNameClick) {
            Text(
                text = if (currentName.isEmpty()) "Ввести ім'я" else "Змінити ім'я ($currentName)",
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        val isStartEnabled = currentName.isNotBlank()
        Button(onClick = onStartClick, enabled = isStartEnabled) {
            Text(
                text = if (isStartEnabled) "Привіт, $currentName! Розпочати" else "Розпочати",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Preview(name = "Onboarding – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Onboarding – Dark",  uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun OnboardingScreenPreview() {
    SeriesDiaryTheme {
        OnboardingScreen(
            currentName = "Данило",
            onEnterNameClick = {},
            onStartClick = {}
        )
    }
}


@Composable
fun NameInputScreen(onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
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
        Button(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
            Text(text = "Зберегти", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "NameInput – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "NameInput – Dark",  uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun NameInputScreenPreview() {
    SeriesDiaryTheme {
        NameInputScreen(onSave = {})
    }
}


@Composable
fun MainScreenWithTabs(userName: String) {
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
                    icon = { Icon(Icons.Default.Person, contentDescription = "Профіль") },
                    label = { Text("Профіль", style = MaterialTheme.typography.labelSmall) },
                    selected = currentTab == "tab_profile",
                    onClick = {
                        currentTab = "tab_profile"
                        bottomNavController.navigate("tab_profile") {
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
            composable("tab_list")    { ListTab(bottomNavController) }
            composable("tab_grid")    { GridTab(bottomNavController) }
            composable("tab_profile") { ProfileTab(userName) }

            composable(
                route = "details/{seriesTitle}",
                arguments = listOf(navArgument("seriesTitle") { type = NavType.StringType })
            ) { entry ->
                val title = entry.arguments?.getString("seriesTitle") ?: ""
                DetailsScreen(title, onBack = { bottomNavController.popBackStack() })
            }
        }
    }
}


@Composable
fun ListTab(navController: NavHostController, viewModel: ListViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()

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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ListUiState.Success -> {
                LazyColumn {
                    items(state.series) { series ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { navController.navigate("details/${series.title}") }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                        }
                    }
                }
            }
            is ListUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(name = "ListTab – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "ListTab – Dark",  uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun ListTabPreview() {
    SeriesDiaryTheme {
        ListTab(navController = rememberNavController())
    }
}


@Composable
fun GridTab(navController: NavHostController, viewModel: GridViewModel = viewModel()) {
    val sortedList by viewModel.series.collectAsStateWithLifecycle()
    val sortByRating by viewModel.sortByRating.collectAsStateWithLifecycle()

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

        LazyVerticalGrid(columns = GridCells.Fixed(2)) {
            items(sortedList) { series ->
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clickable { navController.navigate("details/${series.title}") }
                ) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "GridTab – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "GridTab – Dark",  uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun GridTabPreview() {
    SeriesDiaryTheme {
        GridTab(navController = rememberNavController())
    }
}


@Composable
fun DetailsScreen(seriesTitle: String, onBack: () -> Unit) {
    val factory = remember { DetailsViewModel.Factory(seriesTitle) }
    val viewModel: DetailsViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                    text = "Статус: ${series.status.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Рейтинг: ${series.rating}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(name = "Details – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Details – Dark",  uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun DetailsScreenPreview() {
    SeriesDiaryTheme {
        DetailsScreen(seriesTitle = "Breaking Bad", onBack = {})
    }
}


@Composable
fun ProfileTab(initialUserName: String) {
    val factory = remember { ProfileViewModel.Factory(initialUserName) }
    val viewModel: ProfileViewModel = viewModel(factory = factory)
    val editableName by viewModel.userName.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Профіль",
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
            value = editableName,
            onValueChange = { viewModel.updateName(it) },
            label = {
                Text(
                    text = "Ваше ім'я (можна редагувати)",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Preview(name = "Profile – Light", uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "Profile – Dark",  uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = false)
@Composable
private fun ProfileTabPreview() {
    SeriesDiaryTheme {
        ProfileTab(initialUserName = "Данило")
    }
}