package dev.zooty.artcharts.services.site

import dev.zooty.artcharts.services.ChartType
import dev.zooty.artcharts.services.GraphLayout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChartViewServiceTest {
    private val service = ChartViewService()

    @Test
    fun `uses defaults for missing values`() {
        val model = service.createModel("artistDistribution", null, null, null, null, null, null)

        assertEquals(1800, model.width)
        assertEquals(900, model.height)
        assertEquals("/chart/artistDistribution?width=1800&height=900", model.chartUrl)
    }

    @Test
    fun `normalizes invalid dimensions`() {
        val model = service.createModel("artistDistribution", 0, 5001, null, null, null, null)

        assertEquals(1800, model.width)
        assertEquals(900, model.height)
    }

    @Test
    fun `builds species chart URL with type`() {
        val model = service.createModel("speciesDistribution", 800, 600, null, ChartType.TREEMAP, null, null)

        assertEquals("/chart/speciesDistribution?width=800&height=600&type=TREEMAP", model.chartUrl)
        assertEquals(ChartType.TREEMAP, model.selectedType)
    }

    @Test
    fun `builds currency chart URL with repeated filters and ignores unknown values`() {
        val model = service.createModel(
            "currencyDistribution", 800, 600, listOf("USD", "EUR", "UNKNOWN"), null, null, null
        )

        assertTrue(model.chartUrl.contains("filterList=USD"))
        assertTrue(model.chartUrl.contains("filterList=EUR"))
        assertTrue(!model.chartUrl.contains("UNKNOWN"))
        assertEquals(listOf("USD", "EUR"), model.filterList)
    }

    @Test
    fun `builds character graph URL with layout and self flag`() {
        val model = service.createModel(
            "characterGraph", 700, 500, null, null, GraphLayout.CIRCLE, true
        )

        assertEquals(
            "/chart/characterGraph?width=700&height=500&layout=CIRCLE&selfIncluded=true",
            model.chartUrl,
        )
    }
}
