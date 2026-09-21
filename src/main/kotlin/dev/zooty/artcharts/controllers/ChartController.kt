package dev.zooty.artcharts.controllers

import dev.zooty.artcharts.services.ArtistDistributionService
import dev.zooty.artcharts.services.CharacterGraphService
import dev.zooty.artcharts.services.ChartType
import dev.zooty.artcharts.services.GraphLayout
import dev.zooty.artcharts.services.NsfwService
import dev.zooty.artcharts.services.SpeciesDistributionService
import dev.zooty.artcharts.services.SpendingService
import dev.zooty.artcharts.services.TagDistributionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChartController(
    private val artistDistributionService: ArtistDistributionService,
    private val spendingService: SpendingService,
    private val nsfwService: NsfwService,
    private val speciesDistributionService: SpeciesDistributionService,
    private val characterGraphService: CharacterGraphService,
    private val tagDistributionService: TagDistributionService,
) {
    private val defaultWidth: Int = 1800
    private val defaultHeight: Int = 900

    @GetMapping("/chart/artistDistribution", produces = ["image/svg+xml"])
    fun artistDistribution(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String =
        artistDistributionService.artistDistribution(width ?: defaultWidth, height ?: defaultHeight)

    @GetMapping("/chart/currencyDistribution", produces = ["image/svg+xml"])
    fun currencyDistribution(
        @RequestParam("width") width: Int?, @RequestParam("height") height: Int?,
        @RequestParam("filterList") filterList: List<String>?
    ): String = spendingService.currencyDistribution(
        width ?: defaultWidth,
        height ?: defaultHeight,
        filterList ?: listOf()
    )

    @GetMapping("/chart/spendOverTime", produces = ["image/svg+xml"])
    fun spendOverTime(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String =
        spendingService.spendOverTime(width ?: defaultWidth, height ?: defaultHeight)

    @GetMapping("/chart/nsfwRatio", produces = ["image/svg+xml"])
    fun nsfwRatio(@RequestParam("width") width: Int?, @RequestParam("height") height: Int?): String =
        nsfwService.nsfwRatio(width ?: defaultWidth, height ?: defaultHeight)

    @GetMapping("/chart/speciesDistribution", produces = ["image/svg+xml"])
    fun speciesDistribution(
        @RequestParam("width") width: Int?,
        @RequestParam("height") height: Int?,
        @RequestParam("type") type: ChartType?
    ): String = speciesDistributionService.speciesDistribution(
        width ?: defaultWidth,
        height ?: defaultHeight,
        type ?: ChartType.PIE
    )

    @GetMapping("/chart/tagDistribution", produces = ["image/svg+xml"])
    fun tagDistribution(
        @RequestParam("width") width: Int?,
        @RequestParam("height") height: Int?,
        @RequestParam("type") type: ChartType?,
        @RequestParam("category") categoryFilter: String?
    ): String = tagDistributionService.tagDistribution(
        width ?: defaultWidth,
        height ?: defaultHeight,
        type ?: ChartType.PIE,
        categoryFilter
    )

    @GetMapping("/chart/characterGraph", produces = ["image/svg+xml"])
    fun characterGraph(
        @RequestParam("width") width: Int?,
        @RequestParam("height") height: Int?,
        @RequestParam("layout") graphLayout: GraphLayout?,
        @RequestParam("selfIncluded") isSelfIncluded: Boolean?
    ): String = characterGraphService.characterGraph(
        width ?: defaultWidth,
        height ?: defaultHeight,
        graphLayout ?: GraphLayout.ORGANIC,
        isSelfIncluded ?: false
    )
}
