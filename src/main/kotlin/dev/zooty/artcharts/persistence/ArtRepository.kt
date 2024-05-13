package dev.zooty.artcharts.persistence

import dev.zooty.artcharts.persistence.model.Art
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ArtRepository : JpaRepository<Art, Long> {
    @Query("SELECT a.artist.name, COUNT(a) c FROM Art a GROUP BY a.artist.name ORDER BY c DESC")
    fun findAllGroupByArtist(): List<Array<Any>>
}