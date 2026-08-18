package dev.zooty.artcharts.dto

fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
