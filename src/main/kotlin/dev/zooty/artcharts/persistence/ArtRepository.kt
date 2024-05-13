package dev.zooty.artcharts.persistence

import dev.zooty.artcharts.persistence.model.Art
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ArtRepository : JpaRepository<Art, Long>