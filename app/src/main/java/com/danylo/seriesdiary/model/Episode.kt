package com.danylo.seriesdiary.model

class Episode(val seriesTitle: String, val seasonNumber: Int, val episodeNumber: Int) {

    constructor(seriesTitle: String) : this(seriesTitle, 1, 1)

    fun play() {
        println("Відтворення: $seriesTitle -  Сезон $seasonNumber, Епізод $episodeNumber")
    }

    fun play(quality: String = "1080p") {
        println("Відтворення: $seriesTitle - Сезон $seasonNumber, Епізод $episodeNumber [Якість: $quality]")
    }

}