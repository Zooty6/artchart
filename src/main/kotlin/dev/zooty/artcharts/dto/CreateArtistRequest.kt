package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Artist
import jakarta.validation.constraints.NotBlank

data class CreateArtistRequest(
    @field:NotBlank val name: String = "",
    val furaffinity: String? = null,
    val twitter: String? = null,
    val discord: String? = null,
    val deviantart: String? = null,
    val note: String? = null,
    val paypalEmail: String? = null,
    val site: String? = null,
    val boosty: String? = null,
    val telegram: String? = null,
    val facebook: String? = null,
    val vgen: String? = null,
) {
    fun toEntity() = Artist(
        0L, name.trim(), furaffinity, twitter, discord, deviantart, note,
        paypalEmail, site, boosty, telegram, facebook, vgen
    )
}
