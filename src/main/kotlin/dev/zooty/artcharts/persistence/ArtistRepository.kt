package dev.zooty.artcharts.persistence

import dev.zooty.artcharts.persistence.entity.Artist
import org.springframework.data.jpa.repository.JpaRepository

interface ArtistRepository : JpaRepository<Artist, Long>