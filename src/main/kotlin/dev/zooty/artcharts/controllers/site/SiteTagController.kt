package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.persistence.TagRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/site/tags")
class SiteTagController(
    private val tagRepository: TagRepository,
) {
    @GetMapping("/search")
    fun searchTags(
        @RequestParam name: String,
        model: Model,
    ): String {
        model.addAttribute("tags", tagRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(name))
        return "site/fragments/tag-suggestions"
    }
}
