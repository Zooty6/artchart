package dev.zooty.artcharts.controllers

import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.dto.CreateArtistRequest
import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.api.ArtCreationService
import dev.zooty.artcharts.services.api.ArtService
import dev.zooty.artcharts.services.api.ArtistCreationService
import dev.zooty.artcharts.services.site.MediaFileService
import dev.zooty.artcharts.services.site.SiteQueryService
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import jakarta.validation.Valid
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class SiteController(
    private val siteQueryService: SiteQueryService,
    private val artistRepository: ArtistRepository,
    private val artCreationService: ArtCreationService,
    private val artistCreationService: ArtistCreationService,
    private val artService: ArtService,
    private val mediaFileService: MediaFileService,
) {
    companion object {
        private const val VIEW_SITE_INDEX = "site/index"
        private const val VIEW_ARTS_LIST = "site/arts/list"
        private const val VIEW_ART_BROWSER = "site/fragments/art-browser"
        private const val VIEW_ART_DETAIL = "site/arts/detail"
        private const val VIEW_TAG_LIST = "site/fragments/tag-list"
        private const val VIEW_ARTIST_FORM = "site/artists/form"
        private const val VIEW_ARTIST_SUGGESTIONS = "site/fragments/artist-suggestions"
        private const val VIEW_ART_FORM = "site/arts/form"
        private const val VIEW_CHART_INDEX = "site/charts/index"
        private const val VIEW_CHART_BROWSER = "site/fragments/chart-browser"
        private const val REDIRECT_SITE = "redirect:/site"
        private const val REDIRECT_ART = "redirect:/site/arts/"
    }

    private val chartOptions = listOf(
        "artistDistribution",
        "currencyDistribution",
        "spendOverTime",
        "nsfwRatio",
        "speciesDistribution",
        "characterGraph",
    )

    @GetMapping("/", "/site", "/site/")
    fun site(): String = VIEW_SITE_INDEX

    @GetMapping("/site/arts")
    fun arts(
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false, defaultValue = "false") hideNsfw: Boolean,
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

    @GetMapping("/site/arts/list")
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

    @GetMapping("/site/arts/{id}")
    fun artDetail(@PathVariable id: Long, model: Model): String {
        model.addAttribute("art", siteQueryService.art(id))
        return VIEW_ART_DETAIL
    }

    @PostMapping("/site/arts/{id}/tags")
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

    @PostMapping("/site/arts/{id}/tags/{tagName}/delete")
    fun removeTag(@PathVariable id: Long, @PathVariable tagName: String, model: Model): String {
        artService.removeTag(id, tagName)
        model.addAttribute("art", siteQueryService.art(id))
        return VIEW_TAG_LIST
    }

    @GetMapping("/site/artists/new")
    fun newArtist(model: Model): String {
        model.addAttribute("artist", CreateArtistRequest())
        return VIEW_ARTIST_FORM
    }

    @PostMapping("/site/artists")
    fun createArtist(
        @Valid @ModelAttribute("artist") artist: CreateArtistRequest,
        bindingResult: BindingResult,
    ): String {
        if (bindingResult.hasErrors()) return VIEW_ARTIST_FORM
        artistCreationService.create(artist)
        return REDIRECT_SITE
    }

    @GetMapping("/site/artists/search")
    fun searchArtists(
        @RequestParam("artistName") artistName: String,
        model: Model,
    ): String {
        model.addAttribute("artists", artistRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(artistName))
        return VIEW_ARTIST_SUGGESTIONS
    }

    @GetMapping("/site/arts/new")
    fun newArt(model: Model): String {
        model.addAttribute("art", CreateArtRequest())
        model.addAttribute("currencies", Currency.entries)
        return VIEW_ART_FORM
    }

    @PostMapping("/site/arts")
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

    @GetMapping("/site/charts")
    fun charts(
        @RequestParam(required = false, defaultValue = "artistDistribution") chart: String,
        model: Model
    ): String {
        model.addAttribute("chart", chart)
        model.addAttribute("chartOptions", chartOptions)
        return VIEW_CHART_INDEX
    }

    @GetMapping("/site/charts/view")
    fun chartView(@RequestParam chart: String, model: Model): String {
        model.addAttribute("chart", chart)
        model.addAttribute("chartOptions", chartOptions)
        return VIEW_CHART_BROWSER
    }

    @GetMapping("/site/media/{id}")
    fun media(@PathVariable id: Long): ResponseEntity<Resource> {
        val media = mediaFileService.resolveForArt(id)
        val headers = HttpHeaders()
        headers.contentType = media.mediaType
        headers.contentDisposition = ContentDisposition.inline().build()
        return ResponseEntity.ok().headers(headers).body(media.resource)
    }

    @ExceptionHandler(dev.zooty.artcharts.exceptions.ResourceNotFoundException::class)
    fun notFound(): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.NOT_FOUND).build()
}
