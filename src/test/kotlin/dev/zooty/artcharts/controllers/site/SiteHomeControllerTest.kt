package dev.zooty.artcharts.controllers.site

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@WebMvcTest(SiteHomeController::class)
class SiteHomeControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `home page is available from all supported routes`() {
        listOf("/", "/site", "/site/").forEach { route ->
            mockMvc.perform(get(route))
                .andExpect(status().isOk)
                .andExpect(view().name("site/index"))
                .andExpect(content().string(containsString("API documentation")))
        }
    }
}
