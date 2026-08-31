package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.persistence.TagRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@WebMvcTest(SiteTagController::class)
class SiteTagControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var tagRepository: TagRepository

    @Test
    fun `tag search renders suggestions fragment`() {
        `when`(tagRepository.findTop20ByNameContainingIgnoreCaseOrderByNameAsc("fur"))
            .thenReturn(emptyList())

        mockMvc.perform(get("/site/tags/search").param("name", "fur"))
            .andExpect(status().isOk)
            .andExpect(view().name("site/fragments/tag-suggestions"))
    }
}
