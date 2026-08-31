package dev.zooty.artcharts.persistence

import dev.zooty.artcharts.persistence.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TagRepository : JpaRepository<Tag, String> {
    fun findByName(name: String): Optional<Tag>
    fun findTop20ByNameContainingIgnoreCaseOrderByNameAsc(name: String): List<Tag>
}
