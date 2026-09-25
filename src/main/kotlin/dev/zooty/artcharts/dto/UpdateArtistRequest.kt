package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Artist
import jakarta.validation.constraints.NotBlank

data class UpdateArtistRequest(
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
    fun applyTo(artist: Artist) {
        artist.name = name.trim()
        artist.furaffinity = furaffinity.nullIfBlank()
        artist.twitter = twitter.nullIfBlank()
        artist.discord = discord.nullIfBlank()
        artist.deviantart = deviantart.nullIfBlank()
        artist.note = note.nullIfBlank()
        artist.paypalEmail = paypalEmail.nullIfBlank()
        artist.site = site.nullIfBlank()
        artist.boosty = boosty.nullIfBlank()
        artist.telegram = telegram.nullIfBlank()
        artist.facebook = facebook.nullIfBlank()
        artist.vgen = vgen.nullIfBlank()
    }
}
