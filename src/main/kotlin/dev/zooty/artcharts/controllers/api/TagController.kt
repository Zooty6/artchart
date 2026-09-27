package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.services.api.TagService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TagController(private val tagService: TagService) {
    @GetMapping("/api/tag/categories")
    fun getCategories(): List<String> = tagService.findAllDistinctCategories()
}
