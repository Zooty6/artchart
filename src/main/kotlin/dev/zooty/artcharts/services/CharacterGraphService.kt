package dev.zooty.artcharts.services

import com.mxgraph.layout.mxCircleLayout
import com.mxgraph.layout.mxFastOrganicLayout
import com.mxgraph.layout.mxOrganicLayout
import dev.zooty.artcharts.persistence.ArtRepository
import org.jgrapht.Graph
import org.jgrapht.ext.JGraphXAdapter
import org.jgrapht.graph.SimpleWeightedGraph
import org.springframework.stereotype.Service

@Service
class CharacterGraphService(
    private val svgService: SvgConverterService,
    private val artRepository: ArtRepository,
) {

    fun characterGraph(graphLayout: GraphLayout, isSelfIncluded: Boolean): String {
        val graph = createCharacterGraph(isSelfIncluded)
        return exportGraphToSvg(graph, graphLayout)
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
        val charactersWithZooty = (if (selfIncluded) characters + "Zooty" else characters).sorted()
        return charactersWithZooty
            .indices
            .flatMap { i ->
                (i + 1 until charactersWithZooty.size).map { j ->
                    Pair(charactersWithZooty[i], charactersWithZooty[j])
                }
            }.toList()
    }

    private fun renderGraphAsListSvg(graph: Graph<String, VisibleWeightedEdge>): String {
        TODO("Not yet implemented")
    }

    private fun exportGraphToSvg(graph: Graph<String, VisibleWeightedEdge>, graphLayout: GraphLayout): String {
        return when (graphLayout) {
            GraphLayout.CIRCLE -> svgService.mxSvgExport(mxCircleLayout(JGraphXAdapter(graph)))
            GraphLayout.ORGANIC -> svgService.mxSvgExport(mxOrganicLayout(JGraphXAdapter(graph)))
            GraphLayout.FAST_ORGANIC -> svgService.mxSvgExport(mxFastOrganicLayout(JGraphXAdapter(graph)))
            GraphLayout.LIST -> renderGraphAsListSvg(graph)
        }
    }
}