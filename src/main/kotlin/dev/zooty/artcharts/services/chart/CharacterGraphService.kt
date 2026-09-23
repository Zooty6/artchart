package dev.zooty.artcharts.services.chart

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.layout.mxFastOrganicLayout
import com.mxgraph.layout.mxOrganicLayout
import dev.zooty.artcharts.persistence.ArtRepository
import org.jfree.graphics2d.svg.SVGGraphics2D
import org.jgrapht.Graph
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.SimpleWeightedGraph
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints

@Service
class CharacterGraphService(
    private val svgService: SvgConverterService,
    private val artRepository: ArtRepository
) {
    @Value($$"${artcharts.self.name}")
    private lateinit var selfName: String

    fun characterGraph(graphLayout: GraphLayout, isSelfIncluded: Boolean): String =
        characterGraph(DEFAULT_WIDTH, DEFAULT_HEIGHT, graphLayout, isSelfIncluded)

    fun characterGraph(width: Int, height: Int, graphLayout: GraphLayout, isSelfIncluded: Boolean): String {
        val graph = createCharacterGraph(isSelfIncluded)
        return exportGraphToSvg(graph, width, height, graphLayout)
    }

    private fun createCharacterGraph(isSelfIncluded: Boolean): Graph<String, VisibleWeightedEdge> {
        val graph = SimpleWeightedGraph<String, VisibleWeightedEdge>(VisibleWeightedEdge::class.java)
        artRepository.findAll()
            .filter { it.otherCharacters?.isNotBlank() ?: false }
            .map { it.otherCharacters!!.split(", ") }
            .flatMap { createConnections(it, isSelfIncluded) }
            .groupingBy { it }
            .eachCount()
            .forEach {
                graph.addVertex(it.key.first)
                graph.addVertex(it.key.second)
                val edge = graph.addEdge(it.key.first, it.key.second)
                graph.setEdgeWeight(edge, it.value.toDouble())
            }

        return graph
    }

    private fun createConnections(characters: List<String>, selfIncluded: Boolean): List<Pair<String, String>> {
        val charactersWithZooty = (if (selfIncluded) characters + selfName else characters).sorted()
        return charactersWithZooty
            .indices
            .flatMap { i ->
                (i + 1 until charactersWithZooty.size).map { j ->
                    Pair(charactersWithZooty[i], charactersWithZooty[j])
                }
            }.toList()
    }

    private fun renderGraphAsListSvg(graph: Graph<String, VisibleWeightedEdge>, width: Int, height: Int): String {
        val svgGraphics2D = SVGGraphics2D(width, height)
        svgGraphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        svgGraphics2D.color = Color.WHITE
        svgGraphics2D.fillRect(0, 0, width, height)

        val connections = graphConnections(graph)
        if (connections.isEmpty()) return svgGraphics2D.svgElement

        val padding = 10
        val lineHeight = ((height - 2 * padding).toDouble() / connections.size)
            .coerceAtMost(24.0)
            .coerceAtLeast(1.0)
        val fontSize = (lineHeight * 0.7).toInt().coerceAtLeast(1)
        svgGraphics2D.color = Color.BLACK
        svgGraphics2D.font = Font("Monospaced", Font.PLAIN, fontSize)

        formatConnections(connections).forEachIndexed { index, label ->
            val baseline = padding + (index + 1) * lineHeight
            svgGraphics2D.drawString(label, padding, baseline.toInt())
        }

        return svgGraphics2D.svgElement
    }

    private fun graphConnections(graph: Graph<String, VisibleWeightedEdge>): List<GraphConnection> =
        graph.edgeSet()
            .map { edge ->
                GraphConnection(
                    source = graph.getEdgeSource(edge),
                    target = graph.getEdgeTarget(edge),
                    weight = edge.toString(),
                )
            }
            .sortedWith(compareBy({ it.source }, { it.target }))

    private fun formatConnections(connections: List<GraphConnection>): List<String> {
        val sourceColumnWidth = connections.maxOf { it.source.length } + MIN_DASHES
        val weightColumnWidth = connections.maxOf { it.weight.length }
        val rightCharacterColumn = sourceColumnWidth + weightColumnWidth + MIN_DASHES

        return connections.map { connection ->
            val dashesBeforeWeight = "-".repeat(sourceColumnWidth - connection.source.length)
            val dashesAfterWeight = "-".repeat(rightCharacterColumn - sourceColumnWidth - connection.weight.length)
            connection.source + dashesBeforeWeight + connection.weight + dashesAfterWeight + connection.target
        }
    }

    private data class GraphConnection(
        val source: String,
        val target: String,
        val weight: String,
    )

    private fun exportGraphToSvg(
        graph: Graph<String, VisibleWeightedEdge>,
        width: Int,
        height: Int,
        graphLayout: GraphLayout,
    ): String {
        return when (graphLayout) {
            GraphLayout.CIRCLE -> svgService.mxSvgExport(mxCircleLayout(JGraphXAdapter(graph)))
            GraphLayout.ORGANIC -> svgService.mxSvgExport(mxOrganicLayout(JGraphXAdapter(graph)))
            GraphLayout.FAST_ORGANIC -> svgService.mxSvgExport(mxFastOrganicLayout(JGraphXAdapter(graph)))
            GraphLayout.LIST -> renderGraphAsListSvg(graph, width, height)
        }
    }

    companion object {
        private const val DEFAULT_WIDTH = 1800
        private const val DEFAULT_HEIGHT = 900
        private const val MIN_DASHES = 3
    }
}
