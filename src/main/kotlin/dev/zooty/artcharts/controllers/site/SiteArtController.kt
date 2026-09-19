package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.api.ArtCreationService
import dev.zooty.artcharts.services.api.ArtService
import dev.zooty.artcharts.services.site.SiteQueryService
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.data.domain.PageRequest
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/site/arts")
class SiteArtController(
    private val siteQueryService: SiteQueryService,
    private val artRepository: ArtRepository,
    private val artCreationService: ArtCreationService,
    private val artService: ArtService,
) {
    companion object {
        private const val VIEW_ARTS_LIST = "site/arts/list"
        private const val VIEW_ART_BROWSER = "site/fragments/art-browser"
        private const val VIEW_ART_MODE_UPDATE = "site/fragments/art-mode-update"
        private const val VIEW_SEARCH_ERROR = "site/fragments/search-error"
        private const val VIEW_ART_DETAIL = "site/arts/detail"
        private const val VIEW_TAG_LIST = "site/fragments/tag-list"
        private const val VIEW_ART_FORM = "site/arts/form"
        private const val REDIRECT_ART = "redirect:/site/arts/"
    }

    @GetMapping
    fun arts(
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false, defaultValue = "YEAR") searchMode: SearchMode,
        @RequestParam(required = false) searchParams: String?,
        @RequestParam(required = false, defaultValue = "true") hideNsfw: Boolean,
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        try {
            addSearchModelAttributes(year, searchMode, searchParams, hideNsfw, model)
        } catch (exception: IllegalArgumentException) {
            model.addAttribute("searchError", exception.message ?: "Invalid search filter")
            model.addAttribute("arts", emptyList<Any>())
            if (request.getHeader("HX-Request") == "true") {
                response.setHeader("HX-Retarget", "#search-error")
                response.setHeader("HX-Reswap", "outerHTML")
                return VIEW_SEARCH_ERROR
            }
        }
        return VIEW_ARTS_LIST
    }

    @GetMapping("/list")
    fun artList(
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false, defaultValue = "YEAR") searchMode: SearchMode,
        @RequestParam(required = false) searchParams: String?,
        @RequestParam(required = false, defaultValue = "false") hideNsfw: Boolean,
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        try {
           addSearchModelAttributes(year, searchMode, searchParams, hideNsfw, model) 
        } catch (exception: IllegalArgumentException) {
            model.addAttribute("searchError", exception.message ?: "Invalid search filter")
            response.setHeader("HX-Retarget", "#search-error")
            response.setHeader("HX-Reswap", "outerHTML")
            return VIEW_SEARCH_ERROR
        }
        if (request.getHeader("HX-Target") == "year-navigation-content") {
            return VIEW_ART_MODE_UPDATE
        }
        return VIEW_ART_BROWSER
    }
    
    private fun addSearchModelAttributes(year: Int?, searchMode: SearchMode, searchParams: String?, hideNsfw: Boolean, model: Model) {
        val years = if (searchMode == SearchMode.GENERAL) emptyList() else siteQueryService.years()
        val selectedYear = year ?: years.firstOrNull()
        model.addAttribute("years", years)
        model.addAttribute("selectedYear", selectedYear)
        model.addAttribute("hideNsfw", hideNsfw)
        model.addAttribute("searchMode", searchMode)
        model.addAttribute("generalSearch", searchMode == SearchMode.GENERAL)
        model.addAttribute("otherSearchMode", searchMode.toggled())
        model.addAttribute("searchParams", searchParams.orEmpty())
        model.addAttribute(
            "arts",
            if (searchMode == SearchMode.GENERAL) siteQueryService.artsForSearch(searchParams.orEmpty(), hideNsfw)
            else siteQueryService.artsForYear(selectedYear, hideNsfw),
        )
    }

    @GetMapping("/suggestions/{field}")
    fun fieldSuggestions(
        @PathVariable field: String,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) species: String?,
        @RequestParam(required = false) quality: String?,
        model: Model,
    ): String {
        val limit = PageRequest.of(0, 20)
        val values = when (field) {
            "type" -> artRepository.findTypes(type.orEmpty(), limit)
            "species" -> artRepository.findSpecies(species.orEmpty(), limit)
            "quality" -> artRepository.findQualities(quality.orEmpty(), limit)
            else -> emptyList()
        }
        model.addAttribute("values", values)
        return "site/fragments/value-suggestions"
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
