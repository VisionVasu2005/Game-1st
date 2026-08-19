package com.flowtown.lite.engine

import com.flowtown.lite.model.LevelData
import com.flowtown.lite.model.RoadEdge
import com.flowtown.lite.model.RoadNode

class GameGraph(val levelData: LevelData) {
    private val nodeMap = levelData.nodes.associateBy { it.id }
    private val edges = levelData.edges

    fun getNode(id: String): RoadNode? = nodeMap[id]

    fun isConnectionValid(fromNodeId: String, toNodeId: String): Boolean {
        if (fromNodeId == toNodeId) return false
        val edge = edges.firstOrNull {
            (!it.isBlocked) && (
                (it.fromNodeId == fromNodeId && it.toNodeId == toNodeId) ||
                (it.isBidirectional && it.fromNodeId == toNodeId && it.toNodeId == fromNodeId)
            )
        }
        return edge != null
    }

    fun getConnectedNeighbors(nodeId: String): List<RoadNode> {
        val neighbors = mutableListOf<RoadNode>()
        for (edge in edges) {
            if (edge.isBlocked) continue
            if (edge.fromNodeId == nodeId) {
                nodeMap[edge.toNodeId]?.let { neighbors.add(it) }
            } else if (edge.isBidirectional && edge.toNodeId == nodeId) {
                nodeMap[edge.fromNodeId]?.let { neighbors.add(it) }
            }
        }
        return neighbors
    }

    fun getEdgeBetween(fromNodeId: String, toNodeId: String): RoadEdge? {
        return edges.firstOrNull {
            (!it.isBlocked) && (
                (it.fromNodeId == fromNodeId && it.toNodeId == toNodeId) ||
                (it.isBidirectional && it.fromNodeId == toNodeId && it.toNodeId == fromNodeId)
            )
        }
    }
}
