package com.danylo.seriesdiary.model

class User(val username: String) {
    val favoriteSeries = mutableListOf<TvSeries>()
    var lastWatchedEpisode: Episode? = null

    fun addFavorite(series: TvSeries){
        favoriteSeries.add(series)
        println("Серіал '${series.title}' додано до улюблених користувача $username")
    }

    fun watchEpisode(episode: Episode, series: TvSeries){
        lastWatchedEpisode = episode
        series.watchedEpisodes ++
        episode.play()
    }
}

object NotificationService{
    fun notifyNewRelease(seriesTitle: String){
        println("СПОВІЩЕННЯ: Вийшов новий епізод вашого улюбленого среіалу '$seriesTitle'!")
    }
}

fun String.toAppHeader(): String{
    return "=== $this ==="
}

fun TvSeries.isMasterpiece(): Boolean{
    return this.rating >= 9.0
}

fun User.showStats(){
    println("Статистика користувача $username: улюбених серіалів - ${favoriteSeries.size}")
}