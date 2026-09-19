package dev.zooty.artcharts.controllers.site

enum class SearchMode {
    YEAR,
    GENERAL;

    fun toggled(): SearchMode = if (this == YEAR) GENERAL else YEAR
}
