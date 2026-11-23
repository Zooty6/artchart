package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.dto.CreateWipRequest
import dev.zooty.artcharts.dto.DeliverArtRequest
import dev.zooty.artcharts.dto.UpdateWipRequest
import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.services.api.WipService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@WebMvcTest(SiteWipController::class)
class SiteWipControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var wipService: WipService

    @Test
    fun `new wip page renders form and navigation`() {
        val html = mockMvc.perform(get("/site/wips/new"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/wips/new"))
            .andExpect(content().string(containsString("action=\"/site/wips\"")))
            .andExpect(content().string(containsString("name=\"deadlineDate\"")))
            .andExpect(content().string(containsString("value=\"USD\"")))
            .andReturn().response.contentAsString

        val newWip = html.indexOf("href=\"/site/wips/new\">New wip")
        assertTrue(newWip in 0 until html.indexOf("href=\"/site/arts/new\">New art"))
    }

    @Test
    fun `valid form submits wip data and redirects with confirmation`() {
        val saved = mockMvc.perform(validForm())
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/site/wips"))
            .andExpect(flash().attribute("wipSaved", true))
            .andReturn()

        mockMvc.perform(get("/site/wips").flashAttrs(saved.flashMap))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("WIP saved.")))

        val captor = ArgumentCaptor.forClass(CreateWipRequest::class.java)
        verify(wipService).create(captor.capture() ?: CreateWipRequest())
        val request = captor.value
        assertEquals("Artist", request.artistName)
        assertEquals("commission", request.type)
        assertEquals("cat", request.species)
        assertEquals("2024-03-01", request.deadlineDate)
        assertEquals(Currency.USD, request.currency)
        assertEquals(25.0, request.amount)
        assertTrue(request.isNsfw)
    }

    @Test
    fun `invalid form shows field errors`() {
        mockMvc.perform(post("/site/wips").param("artistName", "").param("type", "").param("species", ""))
            .andExpect(status().isOk)
            .andExpect(view().name("site/wips/new"))
            .andExpect(model().attributeHasFieldErrors("wip", "artistName", "type", "species"))
        verifyNoInteractions(wipService)
    }

    @Test
    fun `unknown artist shows an error and keeps submitted values`() {
        doThrow(ResourceNotFoundException("Missing artist"))
            .`when`(wipService).create(any(CreateWipRequest::class.java) ?: CreateWipRequest())

        mockMvc.perform(validForm())
            .andExpect(status().isOk)
            .andExpect(view().name("site/wips/new"))
            .andExpect(model().attributeHasFieldErrors("wip", "artistName"))
            .andExpect(content().string(containsString("No artist with this name exists")))
            .andExpect(content().string(containsString("value=\"2024-03-01\"")))
    }

    @Test
    fun `wips page shows stored values and row actions`() {
        `when`(wipService.getWips()).thenReturn(listOf(TestFixtures.wip()))

        mockMvc.perform(get("/site/wips"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/wips/list"))
            .andExpect(content().string(containsString("<td>1</td>")))
            .andExpect(content().string(containsString("<td>Artist</td>")))
            .andExpect(content().string(containsString("<td>cat</td>")))
            .andExpect(content().string(containsString("<td>2024-01-01</td>")))
            .andExpect(content().string(containsString("<td>2024-01-02</td>")))
            .andExpect(content().string(containsString("<td>2024-02-02</td>")))
            .andExpect(content().string(containsString("<td>USD 10.0</td>")))
            .andExpect(content().string(containsString("href=\"/site/wips/1/edit\"")))
            .andExpect(content().string(containsString("href=\"/site/wips/1/deliver\"")))
    }

    @Test
    fun `wips page renders when no wip is overdue`() {
        val wip = TestFixtures.wip(deadlineDate = LocalDate.now().plusDays(1).toString())
        `when`(wipService.getWips()).thenReturn(listOf(wip))

        val html = mockMvc.perform(get("/site/wips"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("<td>Artist</td>")))
            .andReturn().response.contentAsString

        assertFalse(html.contains("class=\"overdue\""))
    }

    @Test
    fun `wips are overdue after their deadline or five months after ordering`() {
        val today = LocalDate.now()
        val wips = listOf(
            TestFixtures.wip(id = 1L, deadlineDate = today.minusDays(1).toString()),
            TestFixtures.wip(id = 2L, deadlineDate = today.toString()),
            TestFixtures.wip(id = 3L, deadlineDate = null).apply {
                orderedDate = today.minusMonths(6).toString()
            },
            TestFixtures.wip(id = 4L, deadlineDate = "").apply {
                orderedDate = today.minusMonths(4).toString()
            },
            TestFixtures.wip(id = 5L, deadlineDate = null).apply { orderedDate = null },
        )
        `when`(wipService.getWips()).thenReturn(wips)

        mockMvc.perform(get("/site/wips"))
            .andExpect(status().isOk)
            .andExpect(model().attribute("overdueWipIds", setOf(1L, 3L)))
            .andExpect(content().string(containsString("<tr class=\"overdue\">")))
    }

    @Test
    fun `edit form shows stored values and submits the updated deadline`() {
        `when`(wipService.getWip(1L)).thenReturn(TestFixtures.wip())

        mockMvc.perform(get("/site/wips/1/edit"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/wips/new"))
            .andExpect(content().string(containsString("action=\"/site/wips/1/edit\"")))
            .andExpect(content().string(containsString("value=\"2024-02-02\"")))

        mockMvc.perform(post("/site/wips/1/edit")
            .param("type", "commission").param("species", "cat")
            .param("deadlineDate", "2024-04-01"))
            .andExpect(redirectedUrl("/site/wips"))

        val captor = ArgumentCaptor.forClass(UpdateWipRequest::class.java)
        verify(wipService).update(eq(1L), captor.capture() ?: UpdateWipRequest())
        assertEquals("2024-04-01", captor.value.deadlineDate)
    }

    @Test
    fun `delivery form shows inherited values and requires delivery details`() {
        `when`(wipService.getWip(1L)).thenReturn(TestFixtures.wip(isNsfw = true))

        mockMvc.perform(get("/site/wips/1/deliver"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/arts/new"))
            .andExpect(content().string(containsString("action=\"/site/wips/1/deliver\"")))
            .andExpect(content().string(containsString("value=\"commission\"")))
            .andExpect(content().string(containsString("value=\"Artist\"")))
            .andExpect(content().string(containsString("value=\"2024-01-01\"")))

        mockMvc.perform(post("/site/wips/1/deliver"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/arts/new"))
            .andExpect(model().attributeHasFieldErrors("art", "deliveredDate", "fileName"))
    }

    @Test
    fun `delivery forwards files and redirects to the created art`() {
        val file = MockMultipartFile("file", "art.png", "image/png", byteArrayOf(1, 2, 3))
        val art = TestFixtures.art(id = 42L)
        doAnswer { invocation ->
            val request = invocation.getArgument<DeliverArtRequest>(1)
            assertEquals("2024-03-05", request.deliveredDate)
            assertEquals("art.png", request.fileName)
            assertEquals("fox", request.species)
            assertFalse(request.isNsfw ?: true)
            art
        }.`when`(wipService).deliverArt(eq(1L), any(DeliverArtRequest::class.java) ?: DeliverArtRequest(), eq(listOf(file)) ?: emptyList())

        mockMvc.perform(multipart("/site/wips/1/deliver").file(file)
            .param("deliveredDate", "2024-03-05").param("fileName", "art.png")
            .param("species", "fox").param("_isNsfw", "on"))
            .andExpect(redirectedUrl("/site/arts/42"))
    }

    private fun validForm() = post("/site/wips")
        .param("artistName", "Artist")
        .param("type", "commission")
        .param("species", "cat")
        .param("deadlineDate", "2024-03-01")
        .param("currency", "USD")
        .param("amount", "25.0")
        .param("isNsfw", "true")
}
