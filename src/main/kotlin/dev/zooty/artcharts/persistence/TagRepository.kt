package dev.zooty.artcharts.persistence

import dev.zooty.artcharts.persistence.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.*

interface TagRepository : JpaRepository<Tag, String> {
    fun findByName(name: String): Optional<Tag>
    fun findTop20ByNameContainingIgnoreCaseOrderByNameAsc(name: String): List<Tag>
    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Tag>

    @Query("select distinct t.category from Tag t order by t.category")
    fun findDistinctCategories(): List<String>
}
