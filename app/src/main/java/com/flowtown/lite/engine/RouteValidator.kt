package com.flowtown.lite.engine

sealed class RouteModificationResult {
    data class Appended(val newRoute: List<String>) : RouteModificationResult()
    data class Popped(val newRoute: List<String>) : RouteModificationResult()
    data object NoChange : RouteModificationResult()
    data class Rejected(val reason: String) : RouteModificationResult()
}

class RouteValidator(private val graph: GameGraph) {

    fun processNodeInput(
        currentRoute: List<String>,
        candidateNodeId: String,
        startingNodeId: String
    ): RouteModificationResult {
        // If route is empty, the first node MUST be the vehicle's starting node
        if (currentRoute.isEmpty()) {
            return if (candidateNodeId == startingNodeId) {
                RouteModificationResult.Appended(listOf(candidateNodeId))
            } else {
                RouteModificationResult.Rejected("Route must begin at the vehicle's starting building node.")
            }
        }

        val lastNodeId = currentRoute.last()

        // Touching the current end node does nothing
        if (candidateNodeId == lastNodeId) {
            return RouteModificationResult.NoChange
        }

        // Reversing over the previous node removes the last segment (Undo behavior)
        if (currentRoute.size >= 2 && candidateNodeId == currentRoute[currentRoute.size - 2]) {
            val poppedRoute = currentRoute.dropLast(1)
            return RouteModificationResult.Popped(poppedRoute)
        }

        // Check if candidate node is connected to the last node
        if (graph.isConnectionValid(lastNodeId, candidateNodeId)) {
            val updatedRoute = currentRoute + candidateNodeId
            return RouteModificationResult.Appended(updatedRoute)
        }

        return RouteModificationResult.Rejected("No valid road connection between $lastNodeId and $candidateNodeId")
    }

    fun isRouteValid(route: List<String>, startingNodeId: String): Boolean {
        if (route.isEmpty() || route.first() != startingNodeId) return false
        for (i in 0 until route.size - 1) {
            if (!graph.isConnectionValid(route[i], route[i + 1])) {
                return false
            }
        }
        return true
    }
}
