package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.services.api.ArtistCreationService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@WebMvcTest(SiteArtistController::class)
class SiteArtistControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var artistRepository: ArtistRepository

    @MockitoBean
    lateinit var artistCreationService: ArtistCreationService

    @Test
    fun `new artist page renders form`() {
        mockMvc.perform(get("/site/artists/new"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/artists/form"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("New artist")))
    }

    @Test
    fun `artist search renders suggestions fragment`() {
        `when`(artistRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc("Fox"))
            .thenReturn(emptyList())

        mockMvc.perform(get("/site/artists/search").param("artistName", "Fox"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/fragments/artist-suggestions"))
    }
}
