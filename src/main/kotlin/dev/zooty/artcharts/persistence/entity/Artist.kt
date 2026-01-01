package dev.zooty.artcharts.persistence.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import lombok.NoArgsConstructor

@Entity
@NoArgsConstructor
@Table(name = "artists")
@Suppress("unused", "JpaDataSourceORMInspection")
class Artist(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    val name: String,
    val furaffinity: String?,
    val twitter: String?,
    val discord: String?,
    val deviantart: String?,
    val note: String?,
    val paypalEmail: String?,
    val site: String?,
    val boosty: String?,
    val telegram: String?,
    val facebook: String?
)