package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.api.ArtCreationService
import dev.zooty.artcharts.services.api.ArtService
import dev.zooty.artcharts.services.site.SiteQueryService
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/site/arts")
class SiteArtController(
    private val siteQueryService: SiteQueryService,
    private val artCreationService: ArtCreationService,
    private val artService: ArtService,
) {
    companion object {
        private const val VIEW_ARTS_LIST = "site/arts/list"
        private const val VIEW_ART_BROWSER = "site/fragments/art-browser"
        private const val VIEW_ART_DETAIL = "site/arts/detail"
        private const val VIEW_TAG_LIST = "site/fragments/tag-list"
        private const val VIEW_ART_FORM = "site/arts/form"
        private const val REDIRECT_ART = "redirect:/site/arts/"
    }

    @GetMapping
    fun arts(
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false, defaultValue = "true") hideNsfw: Boolean,
        model: Model,
    ): String {
        val years = siteQueryService.years()
        val selectedYear = year ?: years.firstOrNull()
        model.addAttribute("years", years)
        model.addAttribute("selectedYear", selectedYear)
        model.addAttribute("hideNsfw", hideNsfw)
        model.addAttribute("arts", siteQueryService.artsForYear(selectedYear, hideNsfw))
        return VIEW_ARTS_LIST
    }

    @GetMapping("/list")
    fun artList(
        @RequestParam year: Int,
        @RequestParam(required = false, defaultValue = "false") hideNsfw: Boolean,
        model: Model,
    ): String {
        model.addAttribute("years", siteQueryService.years())
        model.addAttribute("selectedYear", year)
        model.addAttribute("hideNsfw", hideNsfw)
        model.addAttribute("arts", siteQueryService.artsForYear(year, hideNsfw))
        return VIEW_ART_BROWSER
    }

    @GetMapping("/{id}")
    fun artDetail(@PathVariable id: Long, model: Model): String {
        model.addAttribute("art", siteQueryService.art(id))
        return VIEW_ART_DETAIL
    }

    @PostMapping("/{id}/tags")
    fun addTag(
        @PathVariable id: Long,
        @RequestParam name: String,
        @RequestParam category: String,
        model: Model,
    ): String {
        artService.addTag(id, TagDto(name.trim(), category.trim()))
        model.addAttribute("art", siteQueryService.art(id))
        return VIEW_TAG_LIST
    }

    @PostMapping("/{id}/tags/{tagName}/delete")
    fun removeTag(@PathVariable id: Long, @PathVariable tagName: String, model: Model): String {
        artService.removeTag(id, tagName)
        model.addAttribute("art", siteQueryService.art(id))
        return VIEW_TAG_LIST
    }

    @GetMapping("/new")
    fun newArt(model: Model): String {
        model.addAttribute("art", CreateArtRequest())
        model.addAttribute("currencies", Currency.entries)
        return VIEW_ART_FORM
    }

    @PostMapping
    fun createArt(
        @Valid @ModelAttribute("art") art: CreateArtRequest,
        bindingResult: BindingResult,
        model: Model,
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("currencies", Currency.entries)
            return VIEW_ART_FORM
        }
        return try {
            REDIRECT_ART + artCreationService.create(art).id
        } catch (_: ResourceNotFoundException) {
            bindingResult.rejectValue("artistName", "artist.notFound", "No artist with this name exists")
            model.addAttribute("currencies", Currency.entries)
            VIEW_ART_FORM
        }
    }
}
