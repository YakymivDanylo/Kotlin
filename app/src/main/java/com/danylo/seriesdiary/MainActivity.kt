package com.danylo.seriesdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danylo.seriesdiary.model.SeriesDataSource
import com.danylo.seriesdiary.model.SeriesStatus
import com.danylo.seriesdiary.model.TvSeries
import com.danylo.seriesdiary.ui.theme.SeriesDiaryTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeriesDiaryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SeriesDiaryScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

// Завдання 3: Виділення стану в окремий компонент (State Hoisting)
@Composable
fun InputPanel(inputText: String, onTextChange: (String) -> Unit, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            label = { Text("Новий серіал") },
            singleLine = true
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onAddClick) {
            Text("Додати")
        }
    }
}

@Composable
fun SeriesDiaryScreen(modifier: Modifier = Modifier) {
    // Завдання 5: Стан завантаження (mutableStateOf)
    var isLoading by remember { mutableStateOf(true) }

    // Завдання 1: Реактивний стан списку (mutableStateListOf)
    val seriesList = remember { mutableStateListOf<TvSeries>() }
    // Стан для зберігання улюблених серіалів
    val favoriteSeriesTitles = remember { mutableStateListOf<String>() }

    // Завдання 4: Стан для фільтрації
    var showOnlyFavorites by remember { mutableStateOf(false) }

    // Завдання 3: Стан для текстового поля зберігається у батьківському компоненті
    var inputText by remember { mutableStateOf("") }

    // Завдання 5: Імітація асинхронного завантаження даних
    LaunchedEffect(Unit) {
        delay(1500) // Затримка 1.5 секунди
        seriesList.addAll(SeriesDataSource.seriesList)
        favoriteSeriesTitles.add("Breaking Bad") // Демо-дані для улюблених
        favoriteSeriesTitles.add("The Boys")
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

            // Ввід нового серіалу
            InputPanel(
                inputText = inputText,
                onTextChange = { inputText = it },
                onAddClick = {
                    if (inputText.isNotBlank()) {
                        seriesList.add(TvSeries(inputText, 2024, SeriesStatus.UNKNOWN, 0.0))
                        inputText = "" // Очищення поля після додавання
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Завдання 2: Лічильник та візуальне попередження (Badge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "У списку: ${seriesList.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (seriesList.size > 5) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("Більше 5 серіалів!", modifier = Modifier.padding(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Завдання 4: Елемент керування фільтрацією
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = showOnlyFavorites,
                    onCheckedChange = { showOnlyFavorites = it }
                )
                Text("Показувати лише улюблені")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Завдання 4: Фільтрація списку з використанням derivedStateOf
            val filteredList by remember(seriesList.toList(), showOnlyFavorites, favoriteSeriesTitles.toList()) {
                derivedStateOf {
                    if (showOnlyFavorites) {
                        seriesList.filter { favoriteSeriesTitles.contains(it.title) }
                    } else {
                        seriesList
                    }
                }
            }

            // Завдання 2: Обробка порожнього стану списку
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Список порожній. Додайте перший серіал.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredList, key = { it.title + it.hashCode() }) { series ->
                        val isFav = favoriteSeriesTitles.contains(series.title)
                        SeriesListItemEditable(
                            series = series,
                            isFavorite = isFav,
                            onDelete = {
                                seriesList.remove(series)
                                favoriteSeriesTitles.remove(series.title)
                            },
                            onToggleFavorite = {
                                if (isFav) favoriteSeriesTitles.remove(series.title)
                                else favoriteSeriesTitles.add(series.title)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Завдання 2: Елемент списку з можливістю видалення та додавання в улюблені
@Composable
fun SeriesListItemEditable(
    series: TvSeries,
    isFavorite: Boolean,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = series.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = "Рік: ${series.releaseYear} | Статус: ${series.status.description}",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color.Red else Color.Gray
            )
        }
        IconButton(onClick = onDelete) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
        }
    }
}