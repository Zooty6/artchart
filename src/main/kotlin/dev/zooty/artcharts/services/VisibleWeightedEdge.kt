package dev.zooty.artcharts.services

import org.jgrapht.graph.DefaultWeightedEdge

class VisibleWeightedEdge : DefaultWeightedEdge() {
    override fun toString(): String {
        return weight.toInt().toString()
    }
}
