package dev.zooty.artcharts.services

import dev.zooty.artcharts.persistence.ArtRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ArtistDistributionServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository

    @Test
    fun `artistDistribution excludes unknown and counts known artists`() {
        `when`(artRepository.findAllGroupByArtist()).thenReturn(
            listOf(arrayOf("known", 2L), arrayOf("unknown", 9L))
        )
        val service = ArtistDistributionService(SvgConverterService(), artRepository)

        val svg = service.artistDistribution(800, 600)

        assertTrue(svg.startsWith("<svg"))
    }
}
