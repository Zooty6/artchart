package dev.zooty.artcharts.services.api

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.persistence.ArtRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
class ArtSearchServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository

    @Test
    fun `search applies all filters with AND semantics`() {
        val matching = TestFixtures.art(id = 1, year = 2024, artist = TestFixtures.artist("Fox Artist"))
        val wrongArtist = TestFixtures.art(id = 2, year = 2024, artist = TestFixtures.artist("Cat Artist"))
        val wrongYear = TestFixtures.art(id = 3, year = 2023, artist = TestFixtures.artist("Fox Artist"))
        `when`(artRepository.findAllBy()).thenReturn(Stream.of(matching, wrongArtist, wrongYear))

        val result = ArtSearchService(artRepository).searchArts("artist:fox deliveredDate:2024")

        assertEquals(listOf(matching), result)
    }

    @Test
    fun `search returns repository results for an empty query`() {
        val arts = listOf(TestFixtures.art(id = 1), TestFixtures.art(id = 2))
        `when`(artRepository.findAllBy()).thenReturn(arts.stream())

        assertEquals(arts, ArtSearchService(artRepository).searchArts(""))
    }
}
