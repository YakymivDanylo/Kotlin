package com.danylo.seriesdiary.model

fun runSeriesDiary(){
    println("Щоденник серіалів".toAppHeader())

    val breakingBad = TvSeries("Breaking Bad", 2008, SeriesStatus.ENDED, 9.5).apply{
        totalEpisodes = 62
        watchedEpisodes = 60
    }

    val fallout = TvSeries("Fallout", 2024, SeriesStatus.CONTINUING, 8.3)

    val upcomingHit = TvSeries.createTrendingSeries()

    val danya = User("Danya")
    danya.addFavorite(breakingBad)
    danya.addFavorite(fallout)

    val pilotEpisode = Episode(fallout.title)
    val finalEpisode = Episode(breakingBad.title, 5, 16)

    println("\n--- Процес перегляду ---")

    danya.watchEpisode(pilotEpisode,fallout)
    finalEpisode.play("4K")

    println("\n--- Статуси серіалів ---")
    println("${breakingBad.title}: ${breakingBad.getProgress()}")
    println("${fallout.title}: ${fallout.getProgress()}")

    println("\n--- Робота з Nullable змінними ---")
    danya.lastWatchedEpisode?.let { episode ->
        println("Останній переглянутий епізод був із серіалу: ${episode.seriesTitle}")
    }

    println("\n--- Робота з лямбдами та колекціями ---")
    val highlyRatedSeries = danya.favoriteSeries
        .filter { series -> series.rating >= 9.0 }
        .sortedByDescending { it.rating }

    println("Високооцінені улюблені серіали (рейтинг >= 9.0:")
    highlyRatedSeries.forEach { println("- ${it.title} (${it.rating})") }

    println("\n--- Використання Extension функцій ---")
    if (breakingBad.isMasterpiece()) {
        println("${breakingBad.title} - це справжній шедевр!")
    }
    danya.showStats()

    println("\n--- Сповіщення (Singleton) ---")
    NotificationService.notifyNewRelease(fallout.title)

}