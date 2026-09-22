package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.services.api.ArtCreationService
import dev.zooty.artcharts.services.api.ArtService
import dev.zooty.artcharts.services.site.SiteQueryService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import java.util.Optional

@WebMvcTest(SiteArtController::class)
class SiteArtControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var siteQueryService: SiteQueryService

    @MockitoBean
    lateinit var artRepository: ArtRepository

    @MockitoBean
    lateinit var artCreationService: ArtCreationService

    @MockitoBean
    lateinit var artService: ArtService

    @Test
    fun `new art page renders form and currency options`() {
        mockMvc.perform(get("/site/arts/new"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/arts/form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Artist name")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("USD")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("enctype=\"multipart/form-data\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"file\"")))
    }

    @Test
    fun `field suggestions render value fragment`() {
        `when`(artRepository.findSpecies("cat", PageRequest.of(0, 20)))
            .thenReturn(listOf("cat"))

        mockMvc.perform(get("/site/arts/suggestions/species").param("species", "cat"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/fragments/value-suggestions"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"cat\"")))
    }

    @Test
    fun `invalid new art form is rendered again`() {
        mockMvc.perform(
            post("/site/arts")
                .param("type", "")
                .param("species", "")
                .param("deliveredDate", "")
                .param("fileName", "")
                .param("artistName", "")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("site/arts/form"))
        verifyNoInteractions(artCreationService)
    }

    @Test
    fun `valid new art form redirects to the created art`() {
        val createdArt = TestFixtures.art(id = 42L)
        val request = CreateArtRequest(
            type = "commission",
            species = "cat",
            deliveredDate = "2024-01-01",
            fileName = "art.png",
            currency = Currency.USD,
            amount = 10.0,
            artistName = "Artist",
        )
        `when`(artCreationService.create(request, Optional.empty()))
            .thenReturn(createdArt)

        mockMvc.perform(
            post("/site/arts")
                .param("type", "commission")
                .param("species", "cat")
                .param("deliveredDate", "2024-01-01")
                .param("fileName", "art.png")
                .param("currency", "USD")
                .param("amount", "10.0")
                .param("artistName", "Artist")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(view().name("redirect:/site/arts/42"))

        verify(artCreationService).create(request, Optional.empty())
    }

    @Test
    fun `valid new art form forwards uploaded file`() {
        val createdArt = TestFixtures.art(id = 42L)
        val request = CreateArtRequest(
            type = "commission",
            species = "cat",
            deliveredDate = "2024-01-01",
            fileName = "art.png",
            currency = Currency.USD,
            amount = 10.0,
            artistName = "Artist",
        )
        val file = MockMultipartFile("file", "art.png", "image/png", "image".toByteArray())
        `when`(artCreationService.create(request, Optional.of(file))).thenReturn(createdArt)

        mockMvc.perform(
            multipart("/site/arts")
                .file(file)
                .param("type", "commission")
                .param("species", "cat")
                .param("deliveredDate", "2024-01-01")
                .param("fileName", "art.png")
                .param("currency", "USD")
                .param("amount", "10.0")
                .param("artistName", "Artist")
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(view().name("redirect:/site/arts/42"))

        verify(artCreationService).create(request, Optional.of(file))
    }

    @Test
    fun `search mode renders filtered arts without year navigation`() {
        val art = TestFixtures.art(id = 42L)
        `when`(siteQueryService.years()).thenReturn(listOf(2024, 2023))
        `when`(siteQueryService.artsForSearch("artist:fox", true)).thenReturn(listOf(art))

        mockMvc.perform(
            get("/site/arts")
                .param("searchMode", "GENERAL")
                .param("searchParams", "artist:fox")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("site/arts/list"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Search results")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("href=\"/site/arts(year=2024"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"artist:fox\"")))

        verify(siteQueryService).artsForSearch("artist:fox", true)
    }

    @Test
    fun `mode toggle response replaces navigation without loading the art list`() {
        `when`(siteQueryService.years()).thenReturn(listOf(2024))

        mockMvc.perform(
            get("/site/arts/list")
                .param("searchMode", "GENERAL")
                .header("HX-Target", "year-navigation-content")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("site/fragments/art-mode-update"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"year-navigation-content\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"search-mode-toggle\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("hx-swap-oob=\"outerHTML\"")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("id=\"art-list\""))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Year view"))))

        verifyNoInteractions(siteQueryService)
    }

    @Test
    fun `invalid search targets the persistent error element`() {
        `when`(siteQueryService.years()).thenReturn(emptyList())
        `when`(siteQueryService.artsForSearch("wrongsearch", true))
            .thenThrow(IllegalArgumentException("Invalid filter string: wrongsearch"))

        mockMvc.perform(
            get("/site/arts/list")
                .param("searchMode", "GENERAL")
                .param("searchParams", "wrongsearch")
                .param("hideNsfw", "true")
                .header("HX-Target", "art-browser")
        )
            .andExpect(status().isOk)
            .andExpect(view().name("site/fragments/search-error"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid filter string: wrongsearch")))
            .andExpect(header().string("HX-Retarget", "#search-error"))
    }
}
