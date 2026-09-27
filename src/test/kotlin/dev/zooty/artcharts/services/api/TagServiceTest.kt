package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.persistence.TagRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class TagServiceTest {
    private val tagRepository = mock(TagRepository::class.java)
    private val service = TagService(tagRepository)

    @Test
    fun `returns categories from repository`() {
        `when`(tagRepository.findDistinctCategories()).thenReturn(listOf("general", "style"))

        assertEquals(listOf("general", "style"), service.findAllDistinctCategories())
        verify(tagRepository).findDistinctCategories()
    }
}
