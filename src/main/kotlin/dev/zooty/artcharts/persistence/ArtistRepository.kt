package dev.zooty.artcharts.persistence

import dev.zooty.artcharts.persistence.entity.Artist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtistRepository : JpaRepository<Artist, Long>