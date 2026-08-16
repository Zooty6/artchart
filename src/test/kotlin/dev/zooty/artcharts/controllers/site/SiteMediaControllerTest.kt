package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.services.site.MediaFile
import dev.zooty.artcharts.services.site.MediaFileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`

@WebMvcTest(SiteMediaController::class)
class SiteMediaControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var mediaFileService: MediaFileService

    @Test
    fun `media endpoint returns inline media`() {
        `when`(mediaFileService.resolveForArt(1L)).thenReturn(
            MediaFile(ByteArrayResource("image".toByteArray()), MediaType.IMAGE_PNG)
        )

        mockMvc.perform(get("/site/media/1"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(header().string("Content-Disposition", "inline"))
    }

    @Test
    fun `missing media returns not found`() {
        `when`(mediaFileService.resolveForArt(99L))
            .thenThrow(ResourceNotFoundException("missing"))

        mockMvc.perform(get("/site/media/99"))
            .andExpect(status().isNotFound)
    }
}
