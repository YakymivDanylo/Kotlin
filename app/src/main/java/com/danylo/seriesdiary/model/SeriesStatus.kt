package com.danylo.seriesdiary.model

enum class SeriesStatus(val description: String) {
    CONTINUING("Виходить"),
    ENDED("Завершено"),
    UPCOMING("Очікується"),
    UNKNOWN("Невідомо")
}