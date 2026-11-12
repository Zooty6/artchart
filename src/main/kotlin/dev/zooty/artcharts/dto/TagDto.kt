package dev.zooty.artcharts.dto

import dev.zooty.artcharts.persistence.entity.Tag
import java.io.Serializable

/**
 * DTO for [dev.zooty.artcharts.persistence.entity.Tag]
 */
data class TagDto(val name: String, val category: String) : Serializable {
    fun toEntity(): Tag {
        return Tag(name, category)
    }
}