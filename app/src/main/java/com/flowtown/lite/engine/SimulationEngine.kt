package com.flowtown.lite.engine

import com.flowtown.lite.model.FailureReason
import com.flowtown.lite.model.GameState
import com.flowtown.lite.model.LevelData
import com.flowtown.lite.model.RoadNode
import com.flowtown.lite.model.ServiceRequest
import com.flowtown.lite.model.Vehicle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ParticleType {
    SPARK,
    STAR,
    SKID_MARK,
    SMOKE
}

data class ParticleEvent(
    val x: Float,
    val y: Float,
    val type: ParticleType,
    val colorHex: Long = 0xFFFFD700,
    val angle: Float = 0f,
    val lifetime: Float = 1.0f
)

data class ActiveVehicleState(
    val vehicleId: String,
    val currentX: Float,
    val currentY: Float,
    val angleDegrees: Float,
    val segmentIndex: Int,
    val progressInSegment: Float,
    val isFinished: Boolean,
    val isDrifting: Boolean = false,
    val driftIntensity: Float = 0f,
    val driftVisualAngle: Float = angleDegrees
)

data class SimulationFrame(
    val gameState: GameState,
    val vehicleStates: Map<String, ActiveVehicleState>,
    val requests: List<ServiceRequest>,
    val failureReason: FailureReason?,
    val elapsedTimeSeconds: Float,
    val newParticles: List<ParticleEvent> = emptyList(),
    val triggeredDriftSound: Boolean = false,
    val triggeredSuccessSound: Boolean = false,
    val triggeredCrashSound: Boolean = false
)

class SimulationEngine(
    private val levelData: LevelData,
    private val assignedRoutes: Map<String, List<String>>
) {
    private val nodeMap: Map<String, RoadNode> = levelData.nodes.associateBy { it.id }
    private val vehicleMap: Map<String, Vehicle> = levelData.vehicles.associateBy { it.id }

    private var activeStates: MutableMap<String, ActiveVehicleState> = mutableMapOf()
    private var requestsState: MutableList<ServiceRequest> = levelData.requests.toMutableList()
    private var currentGameState: GameState = GameState.SIMULATING
    private var failureReason: FailureReason? = null
    private var totalElapsedSeconds: Float = 0f

    // Track previous angles to compute drift oversteer
    private val previousAngles: MutableMap<String, Float> = mutableMapOf()
    private var lastDriftSoundTime: Float = -1f

    init {
        // Initialize vehicle positions at their first assigned route node
        levelData.vehicles.forEach { vehicle ->
            val route = assignedRoutes[vehicle.id] ?: listOf(vehicle.startingNodeId)
            val firstNode = nodeMap[route.firstOrNull() ?: vehicle.startingNodeId]
            val secondNode = if (route.size > 1) nodeMap[route[1]] else null

            val initialAngle = if (firstNode != null && secondNode != null) {
                calculateAngle(firstNode.x, firstNode.y, secondNode.x, secondNode.y)
            } else 0f

            val initialX = firstNode?.x ?: 0.5f
            val initialY = firstNode?.y ?: 0.5f

            activeStates[vehicle.id] = ActiveVehicleState(
                vehicleId = vehicle.id,
                currentX = initialX,
                currentY = initialY,
                angleDegrees = initialAngle,
                segmentIndex = 0,
                progressInSegment = 0f,
                isFinished = false,
                driftVisualAngle = initialAngle
            )
            previousAngles[vehicle.id] = initialAngle
        }
    }

    fun step(dt: Float): SimulationFrame {
        if (currentGameState != GameState.SIMULATING) {
            return SimulationFrame(
                gameState = currentGameState,
                vehicleStates = activeStates,
                requests = requestsState,
                failureReason = failureReason,
                elapsedTimeSeconds = totalElapsedSeconds
            )
        }

        totalElapsedSeconds += dt
        val particles = mutableListOf<ParticleEvent>()
        var playDrift = false
        var playSuccess = false
        var playCrash = false

        // 1. Check Level Time Limit
        levelData.timeLimitSeconds?.let { limit ->
            if (totalElapsedSeconds >= limit) {
                currentGameState = GameState.FAILURE
                failureReason = FailureReason.TIMEOUT
                return SimulationFrame(
                    gameState = currentGameState,
                    vehicleStates = activeStates,
                    requests = requestsState,
                    failureReason = failureReason,
                    elapsedTimeSeconds = totalElapsedSeconds,
                    triggeredCrashSound = true
                )
            }
        }

        // 2. Advance Each Vehicle Along Its Route with Smooth Drift Dynamics
        val updatedStates = mutableMapOf<String, ActiveVehicleState>()

        for (vehicle in levelData.vehicles) {
            val state = activeStates[vehicle.id] ?: continue
            val route = assignedRoutes[vehicle.id] ?: listOf(vehicle.startingNodeId)

            if (state.isFinished || route.size < 2 || state.segmentIndex >= route.size - 1) {
                updatedStates[vehicle.id] = state.copy(isFinished = true, isDrifting = false)
                continue
            }

            val fromNode = nodeMap[route[state.segmentIndex]]
            val toNode = nodeMap[route[state.segmentIndex + 1]]

            if (fromNode == null || toNode == null) {
                updatedStates[vehicle.id] = state.copy(isFinished = true)
                continue
            }

            val segmentLength = calculateDistance(fromNode.x, fromNode.y, toNode.x, toNode.y).coerceAtLeast(0.001f)
            // Faster base speed (0.75f) for snappy, fun movement
            val baseSpeed = vehicle.type.speed * 2.1f
            val progressDelta = (baseSpeed * dt) / segmentLength

            var newProgress = state.progressInSegment + progressDelta
            var newSegmentIndex = state.segmentIndex
            var isFinished = false

            // Target straight-line heading
            val targetAngle = calculateAngle(fromNode.x, fromNode.y, toNode.x, toNode.y)

            // Drift detection when transitioning between segments
            var isDrifting = false
            var driftIntensity = 0f
            var visualAngle = targetAngle

            if (newProgress >= 1.0f) {
                newProgress = 0f
                newSegmentIndex++
                if (newSegmentIndex >= route.size - 1) {
                    isFinished = true
                    newProgress = 1.0f
                } else {
                    // Check turn angle into next segment
                    val nextFrom = nodeMap[route[newSegmentIndex]]
                    val nextTo = nodeMap[route[newSegmentIndex + 1]]
                    if (nextFrom != null && nextTo != null) {
                        val nextAngle = calculateAngle(nextFrom.x, nextFrom.y, nextTo.x, nextTo.y)
                        val angleDiff = Math.abs(normalizeAngle(nextAngle - targetAngle))
                        if (angleDiff > 28f) {
                            isDrifting = true
                            driftIntensity = (angleDiff / 90f).coerceIn(0.3f, 1.0f)
                            if (totalElapsedSeconds - lastDriftSoundTime > 0.4f) {
                                playDrift = true
                                lastDriftSoundTime = totalElapsedSeconds
                            }
                        }
                    }
                }
            }

            // Smooth Interpolated Position
            val currFrom = nodeMap[route[newSegmentIndex]] ?: fromNode
            val currTo = if (newSegmentIndex + 1 < route.size) nodeMap[route[newSegmentIndex + 1]] ?: toNode else toNode

            var posX = currFrom.x + (currTo.x - currFrom.x) * newProgress
            var posY = currFrom.y + (currTo.y - currFrom.y) * newProgress

            val currentHeading = calculateAngle(currFrom.x, currFrom.y, currTo.x, currTo.y)

            // Dynamic Drift Oversteer and Tire Skid Marks
            if (isDrifting || (state.isDrifting && state.progressInSegment < 0.45f)) {
                isDrifting = true
                driftIntensity = 0.8f
                // Oversteer tilt: vehicle rotates slightly inward to the turn
                val oversteer = (if (normalizeAngle(currentHeading - state.angleDegrees) > 0) 18f else -18f)
                visualAngle = currentHeading + oversteer

                // Emit Skid Mark & Smoke Particle
                particles.add(
                    ParticleEvent(
                        x = posX,
                        y = posY,
                        type = ParticleType.SKID_MARK,
                        colorHex = 0x88263238,
                        angle = currentHeading
                    )
                )
                particles.add(
                    ParticleEvent(
                        x = posX,
                        y = posY,
                        type = ParticleType.SMOKE,
                        colorHex = 0x55ECEFF1,
                        angle = currentHeading
                    )
                )
            } else {
                // Smooth rotational interpolation towards heading
                val prevAngle = previousAngles[vehicle.id] ?: currentHeading
                visualAngle = interpolateAngle(prevAngle, currentHeading, (dt * 18f).coerceAtMost(1f))
            }
            previousAngles[vehicle.id] = visualAngle

            updatedStates[vehicle.id] = ActiveVehicleState(
                vehicleId = vehicle.id,
                currentX = posX,
                currentY = posY,
                angleDegrees = currentHeading,
                segmentIndex = newSegmentIndex,
                progressInSegment = newProgress,
                isFinished = isFinished,
                isDrifting = isDrifting,
                driftIntensity = driftIntensity,
                driftVisualAngle = visualAngle
            )
        }

        activeStates = updatedStates

        // 3. Collision Detection (Vehicle-Vehicle proximity)
        val vehicleList = activeStates.values.toList()
        for (i in 0 until vehicleList.size) {
            for (j in i + 1 until vehicleList.size) {
                val v1 = vehicleList[i]
                val v2 = vehicleList[j]
                val dist = calculateDistance(v1.currentX, v1.currentY, v2.currentX, v2.currentY)
                val collisionRadius = 0.052f

                if (dist < collisionRadius) {
                    currentGameState = GameState.FAILURE
                    failureReason = FailureReason.COLLISION
                    playCrash = true

                    // Spark particles
                    particles.add(ParticleEvent((v1.currentX + v2.currentX) / 2f, (v1.currentY + v2.currentY) / 2f, ParticleType.SPARK, 0xFFFF3D00))
                    particles.add(ParticleEvent((v1.currentX + v2.currentX) / 2f, (v1.currentY + v2.currentY) / 2f, ParticleType.SPARK, 0xFFFFEA00))

                    return SimulationFrame(
                        gameState = currentGameState,
                        vehicleStates = activeStates,
                        requests = requestsState,
                        failureReason = failureReason,
                        elapsedTimeSeconds = totalElapsedSeconds,
                        newParticles = particles,
                        triggeredCrashSound = true
                    )
                }
            }
        }

        // 4. Destination Arrival & Service Delivery Check
        val buildingMap = levelData.buildings.associateBy { it.id }
        for (reqIndex in requestsState.indices) {
            val req = requestsState[reqIndex]
            if (req.isCompleted) continue

            val destBuilding = buildingMap[req.destinationBuildingId] ?: continue
            val destNode = nodeMap[destBuilding.attachedNodeId] ?: continue

            for (vehicle in levelData.vehicles) {
                val state = activeStates[vehicle.id] ?: continue
                val distToDest = calculateDistance(state.currentX, state.currentY, destNode.x, destNode.y)

                if (distToDest < 0.065f) {
                    requestsState[reqIndex] = req.copy(isCompleted = true)
                    playSuccess = true

                    // Star celebration particles
                    particles.add(ParticleEvent(destNode.x, destNode.y, ParticleType.STAR, 0xFFFFD700))
                    particles.add(ParticleEvent(destNode.x, destNode.y, ParticleType.STAR, 0xFFFFC107))
                }
            }
        }

        // 5. Check Completion / Success Conditions
        val allFinished = activeStates.values.all { it.isFinished }
        val allRequestsFulfilled = requestsState.all { it.isCompleted }

        if (allRequestsFulfilled) {
            currentGameState = GameState.SUCCESS
            playSuccess = true
        } else if (allFinished) {
            currentGameState = GameState.FAILURE
            failureReason = FailureReason.UNSERVED_DESTINATION
        }

        return SimulationFrame(
            gameState = currentGameState,
            vehicleStates = activeStates,
            requests = requestsState,
            failureReason = failureReason,
            elapsedTimeSeconds = totalElapsedSeconds,
            newParticles = particles,
            triggeredDriftSound = playDrift,
            triggeredSuccessSound = playSuccess,
            triggeredCrashSound = playCrash
        )
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val rad = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
        return (rad * 180.0 / PI).toFloat()
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    private fun interpolateAngle(from: Float, to: Float, factor: Float): Float {
        val diff = normalizeAngle(to - from)
        return from + diff * factor
    }
}
