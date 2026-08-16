package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.dto.CreateArtRequest
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.api.ArtCreationService
import dev.zooty.artcharts.services.api.ArtService
import dev.zooty.artcharts.services.site.SiteQueryService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@WebMvcTest(SiteArtController::class)
class SiteArtControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var siteQueryService: SiteQueryService

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
        `when`(artCreationService.create(request))
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

        verify(artCreationService).create(request)
    }
}
