package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.services.api.MediaFile
import dev.zooty.artcharts.services.api.MediaFileService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SiteMediaController::class)
class SiteMediaControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var mediaFileService: MediaFileService

    @Test
    fun `media endpoint returns inline media`() {
        `when`(mediaFileService.resolveForArt(1L)).thenReturn(
            listOf(MediaFile(ByteArrayResource("image".toByteArray()), MediaType.IMAGE_PNG, "image.png"))
        )

        mockMvc.perform(get("/site/media/1"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(header().string("Content-Disposition", "inline"))
            .andExpect(header().string("Cache-Control", "public, max-age=31536000, immutable"))
    }

    @Test
    fun `indexed media endpoint returns selected media`() {
        `when`(mediaFileService.resolveForArt(1L)).thenReturn(
            listOf(
                MediaFile(ByteArrayResource("first".toByteArray()), MediaType.IMAGE_PNG, "01.png"),
                MediaFile(ByteArrayResource("second".toByteArray()), MediaType.IMAGE_JPEG, "02.jpg"),
            )
        )

        mockMvc.perform(get("/site/media/1/2"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "image/jpeg"))
    }

    @Test
    fun `indexed media endpoint returns not found for invalid index`() {
        `when`(mediaFileService.resolveForArt(1L)).thenReturn(
            listOf(MediaFile(ByteArrayResource("image".toByteArray()), MediaType.IMAGE_PNG, "image.png"))
        )

        mockMvc.perform(get("/site/media/1/2"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `manifest endpoint returns indexed media metadata`() {
        `when`(mediaFileService.resolveForArt(1L)).thenReturn(
            listOf(
                MediaFile(ByteArrayResource("image".toByteArray()), MediaType.IMAGE_PNG, "01.png"),
                MediaFile(ByteArrayResource("video".toByteArray()), MediaType.valueOf("video/mp4"), "02.mp4"),
            )
        )

        mockMvc.perform(get("/site/media/1/manifest"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.count").value(2))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].url").value("/site/media/1/1"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[1].kind").value("VIDEO"))
    }

    @Test
    fun `missing media returns not found`() {
        `when`(mediaFileService.resolveForArt(99L))
            .thenThrow(ResourceNotFoundException("missing"))

        mockMvc.perform(get("/site/media/99"))
            .andExpect(status().isNotFound)
    }
}
