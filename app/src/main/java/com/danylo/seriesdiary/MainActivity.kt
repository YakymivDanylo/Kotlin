package com.danylo.seriesdiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.danylo.seriesdiary.model.runSeriesDiary
import com.danylo.seriesdiary.ui.theme.SeriesDiaryTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.danylo.seriesdiary.model.Episode
import com.danylo.seriesdiary.model.SeriesDataSource
import com.danylo.seriesdiary.model.TvSeries


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeriesDiaryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        Text(
                            text = "Топові серіали (Список)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        TopRatedSeriesList(
                            series = SeriesDataSource.topRatedSeries,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "Епізоди (Сітка)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        EpisodesGrid(
                            episodes = SeriesDataSource.episodesSet.toList(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        runSeriesDiary()
    }
}


@Composable
fun SeriesListItem(series: TvSeries) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = series.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                text = "Рік: ${series.releaseYear} | Статус: ${series.status.description}",
                color = Color.Gray
            )
        }
        Text(text = "⭐ ${series.rating}", color = Color(0xFFFBC02D), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SeriesGridItem(episode: Episode) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE3F2FD))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = episode.seriesTitle,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "S${episode.seasonNumber} E${episode.episodeNumber}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun TopRatedSeriesList(series: List<TvSeries>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
    ) {
        items(series) { item ->
            SeriesListItem(item)
        }
    }
}

@Composable
fun EpisodesGrid(episodes: List<Episode>, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        items(episodes) { episode ->
            SeriesGridItem(episode)
        }
    }
}