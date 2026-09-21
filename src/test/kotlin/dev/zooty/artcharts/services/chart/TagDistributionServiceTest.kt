package dev.zooty.artcharts.services.chart

import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.entity.Tag
import dev.zooty.artcharts.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
class TagDistributionServiceTest {

    @Mock
    lateinit var artRepository: ArtRepository

    @Test
    fun `pie chart renders the tags matching the requested category`() {
        `when`(artRepository.findAllBy()).thenReturn(
            Stream.of(
                TestFixtures.art(tags = mutableSetOf(Tag("cute", "style"), Tag("blue", "color"))),
                TestFixtures.art(tags = mutableSetOf(Tag("cute", "style")))
            )
        )

        val svg = TagDistributionService(
            SvgConverterService(), artRepository, RecordingTreeMapRendererService()
        ).tagDistribution(500, 400, ChartType.PIE, "style")

        assertTrue(svg.startsWith("<svg"))
        assertTrue(svg.contains("cute"))
        assertTrue(!svg.contains("blue"))
    }

    @Test
    fun `treemap receives all tags when no category is selected`() {
        `when`(artRepository.findAllBy()).thenReturn(
            Stream.of(
                TestFixtures.art(tags = mutableSetOf(Tag("cute", "style"), Tag("blue", "color"))),
                TestFixtures.art(tags = mutableSetOf(Tag("cute", "style")))
            )
        )
        val renderer = RecordingTreeMapRendererService()

        val svg = TagDistributionService(
            SvgConverterService(), artRepository, renderer
        ).tagDistribution(500, 400, ChartType.TREEMAP, null)

        assertEquals("<svg></svg>", svg)
        assertEquals(2, renderer.dataset.itemCount)
        assertEquals(2L, renderer.dataset.getValue("cute"))
        assertEquals(1L, renderer.dataset.getValue("blue"))
    }

    private class RecordingTreeMapRendererService : TreeMapRendererService() {
        lateinit var dataset: org.jfree.data.general.PieDataset

        override fun renderTreemapSvg(
            width: Int,
            height: Int,
            dataset: org.jfree.data.general.PieDataset
        ): String {
            this.dataset = dataset
            return "<svg></svg>"
        }
    }
}
