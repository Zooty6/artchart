package dev.zooty.artcharts.persistence

import dev.zooty.artcharts.persistence.entity.Art
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
interface ArtRepository : JpaRepository<Art, Long> {
    @Query("SELECT a.artist.name, COUNT(a) c FROM Art a GROUP BY a.artist.name ORDER BY c DESC")
    fun findAllGroupByArtist(): List<Array<Any>>

    @Query("SELECT DISTINCT a.type FROM Art a WHERE LOWER(a.type) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY a.type")
    fun findTypes(@Param("query") query: String, pageable: Pageable): List<String>

    @Query("SELECT DISTINCT a.species FROM Art a WHERE LOWER(a.species) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY a.species")
    fun findSpecies(@Param("query") query: String, pageable: Pageable): List<String>

    @Query("SELECT DISTINCT a.quality FROM Art a WHERE a.quality IS NOT NULL AND LOWER(a.quality) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY a.quality")
    fun findQualities(@Param("query") query: String, pageable: Pageable): List<String>
}
