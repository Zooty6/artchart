package dev.zooty.artcharts.controllers

import dev.zooty.artcharts.services.chart.ArtistDistributionService
import dev.zooty.artcharts.services.chart.CharacterGraphService
import dev.zooty.artcharts.services.chart.DeliveryTimeService
import dev.zooty.artcharts.services.chart.NsfwService
import dev.zooty.artcharts.services.chart.SpeciesDistributionService
import dev.zooty.artcharts.services.chart.SpendingService
import dev.zooty.artcharts.services.chart.TagDistributionService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@ExtendWith(MockitoExtension::class)
class ChartControllerTest {
    @Mock
    lateinit var deliveryTimeService: DeliveryTimeService

    private lateinit var mockMvc: MockMvc
    private val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>"

    @BeforeEach
    fun setUp() {
        val controller = ChartController(
            mock(ArtistDistributionService::class.java),
            mock(SpendingService::class.java),
            mock(NsfwService::class.java),
            mock(SpeciesDistributionService::class.java),
            mock(CharacterGraphService::class.java),
            mock(TagDistributionService::class.java),
            deliveryTimeService,
        )
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `delivery endpoint returns SVG with default dimensions and no year filter`() {
        `when`(deliveryTimeService.deliveryTimeGraph(1800, 900, null)).thenReturn(svg)

        mockMvc.perform(get("/chart/delivery"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
            .andExpect(content().string(svg))
    }

    @Test
    fun `delivery endpoint forwards dimensions and order year filter`() {
        `when`(deliveryTimeService.deliveryTimeGraph(800, 600, "2024")).thenReturn(svg)

        mockMvc.perform(get("/chart/delivery").param("width", "800").param("height", "600").param("year", "2024"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
            .andExpect(content().string(svg))
    }
}
