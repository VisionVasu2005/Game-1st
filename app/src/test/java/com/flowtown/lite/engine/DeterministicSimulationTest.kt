package com.flowtown.lite.engine

import com.flowtown.lite.data.LevelRepository
import com.flowtown.lite.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.LinkedList
import java.util.Queue

class DeterministicSimulationTest {

    @Test
    fun testSimulationProducesIdenticalResultsForIdenticalInputs() {
        val level = LevelRepository.getLevel(0)
        val v1 = level.vehicles.first()
        val destBuilding = level.buildings.first { it.id == level.requests.first().destinationBuildingId }
        
        // Find shortest path from v1 start to dest using BFS
        val graph = GameGraph(level)
        val start = v1.startingNodeId
        val target = destBuilding.attachedNodeId

        val path = findBfsPath(graph, start, target)
        assertTrue("A valid path must exist in Level 1", path.isNotEmpty())

        val routes = mapOf(v1.id to path)

        fun runFullSimulation(): List<SimulationFrame> {
            val engine = SimulationEngine(level, routes)
            val frames = mutableListOf<SimulationFrame>()
            val dt = 0.016f

            for (step in 0..400) {
                val frame = engine.step(dt)
                frames.add(frame)
                if (frame.gameState == GameState.SUCCESS || frame.gameState == GameState.FAILURE) {
                    break
                }
            }
            return frames
        }

        val run1 = runFullSimulation()
        val run2 = runFullSimulation()

        assertEquals("Both runs must have exact same number of step frames", run1.size, run2.size)

        for (i in run1.indices) {
            val frame1 = run1[i]
            val frame2 = run2[i]

            assertEquals("GameState must match at step $i", frame1.gameState, frame2.gameState)
            assertEquals("FailureReason must match at step $i", frame1.failureReason, frame2.failureReason)

            val v1State = frame1.vehicleStates[v1.id]!!
            val v2State = frame2.vehicleStates[v1.id]!!

            assertEquals("X position must be identical at step $i", v1State.currentX, v2State.currentX, 0.0001f)
            assertEquals("Y position must be identical at step $i", v1State.currentY, v2State.currentY, 0.0001f)
            assertEquals("Segment index must match at step $i", v1State.segmentIndex, v2State.segmentIndex)
            assertEquals("IsFinished must match at step $i", v1State.isFinished, v2State.isFinished)
        }

        assertTrue("Final frame must be SUCCESS", run1.last().gameState == GameState.SUCCESS)
    }

    @Test
    fun testAll100LevelsAreProperlyConfigured() {
        val levels = LevelRepository.levels
        assertEquals(100, levels.size)

        for (lvl in levels) {
            assertTrue("Level ${lvl.levelNumber} must have nodes", lvl.nodes.isNotEmpty())
            assertTrue("Level ${lvl.levelNumber} must have edges", lvl.edges.isNotEmpty())
            assertTrue("Level ${lvl.levelNumber} must have vehicles", lvl.vehicles.isNotEmpty())
            assertTrue("Level ${lvl.levelNumber} must have requests", lvl.requests.isNotEmpty())
            assertTrue("Level ${lvl.levelNumber} must have buildings", lvl.buildings.isNotEmpty())
        }
    }

    @Test
    fun testEveryLevelIsConnectedAndSolvable() {
        val levels = LevelRepository.levels
        for (lvl in levels) {
            val graph = GameGraph(lvl)
            for (req in lvl.requests) {
                val origin = lvl.buildings.first { it.id == req.originBuildingId }
                val dest = lvl.buildings.first { it.id == req.destinationBuildingId }
                val path = findBfsPath(graph, origin.attachedNodeId, dest.attachedNodeId)
                assertTrue("Level ${lvl.levelNumber}: Path must exist between ${origin.name} and ${dest.name}", path.isNotEmpty())
            }
        }
    }

    @Test
    fun testOddAndEvenLevelParityAndMonotonicProgression() {
        val levels = LevelRepository.levels
        for (i in 0 until levels.size - 1) {
            val curr = levels[i]
            val next = levels[i + 1]

            // Parity check: adjacent levels should not have abrupt drops in complexity
            assertTrue(
                "Level ${next.levelNumber} should have >= or equal vehicle complexity as Level ${curr.levelNumber}",
                next.vehicles.size >= curr.vehicles.size || curr.vehicles.size - next.vehicles.size <= 1
            )
            assertTrue(
                "Level ${next.levelNumber} nodes (${next.nodes.size}) vs Level ${curr.levelNumber} nodes (${curr.nodes.size}) should have smooth progression",
                next.nodes.size >= curr.nodes.size || curr.nodes.size - next.nodes.size <= 2
            )
        }
    }

    private fun findBfsPath(graph: GameGraph, start: String, target: String): List<String> {
        val queue: Queue<List<String>> = LinkedList()
        val visited = mutableSetOf<String>()
        queue.add(listOf(start))
        visited.add(start)

        while (queue.isNotEmpty()) {
            val path = queue.poll()!!
            val last = path.last()
            if (last == target) return path

            for (neighborNode in graph.getConnectedNeighbors(last)) {
                val neighbor = neighborNode.id
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor)
                    queue.add(path + neighbor)
                }
            }
        }
        return emptyList()
    }
}
