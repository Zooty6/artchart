package dev.zooty.artcharts.persistence.entity

import jakarta.persistence.*

@Entity
@Table(name = "artists")
@Suppress("unused")
class Artist(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long,
    var name: String,
    var furaffinity: String?,
    var twitter: String?,
    var discord: String?,
    var deviantart: String?,
    var note: String?,
    var paypalEmail: String?,
    var site: String?,
    var boosty: String?,
    var telegram: String?,
    var facebook: String?,
    var vgen: String?
)