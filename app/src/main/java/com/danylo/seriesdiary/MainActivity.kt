package com.danylo.seriesdiary

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.danylo.seriesdiary.model.SeriesDataSource
import com.danylo.seriesdiary.model.TvSeries
import com.danylo.seriesdiary.ui.theme.SeriesDiaryTheme

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
            val savedName = entry.savedStateHandle.getStateFlow("userName", "").collectAsState()

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
                    rootNavController.previousBackStackEntry?.savedStateHandle?.set("userName", name)
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
fun OnboardingScreen(currentName: String, onEnterNameClick: () -> Unit, onStartClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Movie, contentDescription = "Logo", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Щоденник Серіалів", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onEnterNameClick) {
            Text(if (currentName.isEmpty()) "Ввести ім'я" else "Змінити ім'я ($currentName)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        val isStartEnabled = currentName.isNotBlank()
        Button(
            onClick = onStartClick,
            enabled = isStartEnabled
        ) {
            Text(if (isStartEnabled) "Привіт, $currentName! Розпочати" else "Розпочати")
        }
    }
}

@Composable
fun NameInputScreen(onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Ваше ім'я") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
            Text("Зберегти")
        }
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
                    label = { Text("Список") },
                    selected = currentTab == "tab_list",
                    onClick = {
                        currentTab = "tab_list"
                        bottomNavController.navigate("tab_list") { popUpTo(bottomNavController.graph.startDestinationId) { saveState = true } ; launchSingleTop = true; restoreState = true }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Плитка") },
                    label = { Text("Плитка") },
                    selected = currentTab == "tab_grid",
                    onClick = {
                        currentTab = "tab_grid"
                        bottomNavController.navigate("tab_grid") { popUpTo(bottomNavController.graph.startDestinationId) { saveState = true } ; launchSingleTop = true; restoreState = true }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Профіль") },
                    label = { Text("Профіль") },
                    selected = currentTab == "tab_profile",
                    onClick = {
                        currentTab = "tab_profile"
                        bottomNavController.navigate("tab_profile") { popUpTo(bottomNavController.graph.startDestinationId) { saveState = true } ; launchSingleTop = true; restoreState = true }
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
            composable("tab_profile") { ProfileTab(userName) }

            composable(
                route = "details/{seriesTitle}",
                arguments = listOf(navArgument("seriesTitle") { type = NavType.StringType })
            ) { entry ->
                val title = entry.arguments?.getString("seriesTitle") ?: ""
                val series = SeriesDataSource.seriesList.find { it.title == title }
                DetailsScreen(series, onBack = { bottomNavController.popBackStack() })
            }
        }
    }
}


@Composable
fun ListTab(navController: NavHostController) {
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val favorites = listOf("Breaking Bad", "The Boys")

    val filteredList by remember(showOnlyFavorites) {
        derivedStateOf {
            if (showOnlyFavorites) {
                SeriesDataSource.seriesList.filter { favorites.contains(it.title) }
            } else {
                SeriesDataSource.seriesList
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = showOnlyFavorites, onCheckedChange = { showOnlyFavorites = it })
            Spacer(modifier = Modifier.width(8.dp))
            Text("Показувати лише улюблені")
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(filteredList) { series ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        navController.navigate("details/${series.title}")
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(series.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Рік: ${series.releaseYear}", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun GridTab(navController: NavHostController) {
    var sortByRating by remember { mutableStateOf(false) }

    val sortedList by remember(sortByRating) {
        derivedStateOf {
            if (sortByRating) {
                SeriesDataSource.seriesList.sortedByDescending { it.rating }
            } else {
                SeriesDataSource.seriesList.sortedBy { it.title }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Сортувати за:")
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = !sortByRating,
                onClick = { sortByRating = false },
                label = { Text("Алфавітом") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = sortByRating,
                onClick = { sortByRating = true },
                label = { Text("Рейтингом") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(2)) {
            items(sortedList) { series ->
                Card(
                    modifier = Modifier.padding(4.dp).aspectRatio(1f).clickable {
                        navController.navigate("details/${series.title}")
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(series.title, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("Рейтинг: ${series.rating}", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsScreen(series: TvSeries?, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Назад") }
        Spacer(modifier = Modifier.height(16.dp))

        if (series != null) {
            Text(series.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Рік випуску: ${series.releaseYear}")
            Text("Статус: ${series.status.description}")
            Text("Рейтинг: ${series.rating}")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Опис: Чудовий серіал, який варто подивитись кожному. Відстежуйте свої епізоди тут!")
        } else {
            Text("Серіал не знайдено")
        }
    }
}


@Composable
fun ProfileTab(initialUserName: String) {
    var editableName by remember { mutableStateOf(initialUserName) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Профіль", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Інформація про додаток", fontWeight = FontWeight.Bold)
                Text("Назва: Series Diary")
                Text("Версія: 1.0.0")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = editableName,
            onValueChange = { editableName = it },
            label = { Text("Ваше ім'я (можна редагувати)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}