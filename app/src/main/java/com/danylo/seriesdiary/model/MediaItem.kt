package com.danylo.seriesdiary.model

open class MediaItem(val title: String, val releaseYear: Int) {
    open fun printInfo() {
        println("Медіа: $title (Рік випуску: $releaseYear)")
    }
}