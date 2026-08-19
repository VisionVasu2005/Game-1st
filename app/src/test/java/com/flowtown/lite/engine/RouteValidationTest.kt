package com.flowtown.lite.engine

import com.flowtown.lite.data.LevelRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteValidationTest {

    private lateinit var graph: GameGraph
    private lateinit var validator: RouteValidator
    private lateinit var startNodeId: String
    private lateinit var connectedNeighborId: String
    private lateinit var nonNeighborId: String

    @Before
    fun setUp() {
        val level1 = LevelRepository.getLevel(0)
        graph = GameGraph(level1)
        validator = RouteValidator(graph)
        startNodeId = level1.vehicles.first().startingNodeId
        
        val neighbors = graph.getConnectedNeighbors(startNodeId)
        connectedNeighborId = neighbors.first().id

        // Find a node that is NOT connected directly to startNodeId
        val nonNeighbors = level1.nodes.filter { node ->
            node.id != startNodeId && neighbors.none { it.id == node.id }
        }
        nonNeighborId = nonNeighbors.firstOrNull()?.id ?: "non_neighbor"
    }

    @Test
    fun testValidRouteStepAccepted() {
        val initialRoute = listOf(startNodeId)
        val result = validator.processNodeInput(initialRoute, connectedNeighborId, startNodeId)

        assertTrue(result is RouteModificationResult.Appended)
        val appended = result as RouteModificationResult.Appended
        assertEquals(listOf(startNodeId, connectedNeighborId), appended.newRoute)
    }

    @Test
    fun testInvalidJumpRejected() {
        val initialRoute = listOf(startNodeId)
        val result = validator.processNodeInput(initialRoute, nonNeighborId, startNodeId)

        assertTrue(result is RouteModificationResult.Rejected)
    }

    @Test
    fun testReversingOverPreviousSegmentPopsLastNode() {
        // If route is start -> connectedNeighbor, and player touches start, it should pop connectedNeighbor
        val currentRoute = listOf(startNodeId, connectedNeighborId)
        val result = validator.processNodeInput(currentRoute, startNodeId, startNodeId)

        assertTrue(result is RouteModificationResult.Popped)
        val popped = result as RouteModificationResult.Popped
        assertEquals(listOf(startNodeId), popped.newRoute)
    }

    @Test
    fun testRouteMustBeginAtStartingNode() {
        val emptyRoute = emptyList<String>()
        val invalidStart = validator.processNodeInput(emptyRoute, connectedNeighborId, startNodeId)
        assertTrue(invalidStart is RouteModificationResult.Rejected)

        val validStart = validator.processNodeInput(emptyRoute, startNodeId, startNodeId)
        assertTrue(validStart is RouteModificationResult.Appended)
        assertEquals(listOf(startNodeId), (validStart as RouteModificationResult.Appended).newRoute)
    }
}
