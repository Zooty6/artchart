package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.dto.CreateArtistRequest
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.services.api.ArtistService
import dev.zooty.artcharts.dto.UpdateArtistRequest
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
@RequestMapping("/site/artists")
class SiteArtistController(
    private val artistRepository: ArtistRepository,
    private val artistService: ArtistService,
) {
    companion object {
        private const val VIEW_ARTIST_FORM = "site/artists/new"
        private const val VIEW_ARTIST_SUGGESTIONS = "site/fragments/artist-suggestions"
        private const val REDIRECT_SITE = "redirect:/site"
    }

    @GetMapping("/new")
    fun newArtist(model: Model): String {
        model.addAttribute("artist", CreateArtistRequest())
        return VIEW_ARTIST_FORM
    }

    @GetMapping("/edit")
    fun editArtist(model: Model): String {
        model.addAttribute("artist", UpdateArtistRequest())
        return "site/artists/edit"
    }

    @PostMapping
    fun createArtist(
        @Valid @ModelAttribute("artist") artist: CreateArtistRequest,
        bindingResult: BindingResult,
    ): String {
        if (bindingResult.hasErrors()) return VIEW_ARTIST_FORM
        artistService.create(artist)
        return REDIRECT_SITE
    }

    @PostMapping("/{id}")
    fun updateArtist(
        @PathVariable id: Long,
        @Valid @ModelAttribute("artist") artist: UpdateArtistRequest,
        bindingResult: BindingResult,
    ): String {
        if (bindingResult.hasErrors()) return "site/artists/edit"
        artistService.update(id, artist)
        return REDIRECT_SITE
    }

    @GetMapping("/search")
    fun searchArtists(
        @RequestParam("artistName") artistName: String,
        model: Model,
    ): String {
        model.addAttribute("artists", artistRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(artistName))
        return VIEW_ARTIST_SUGGESTIONS
    }
}
