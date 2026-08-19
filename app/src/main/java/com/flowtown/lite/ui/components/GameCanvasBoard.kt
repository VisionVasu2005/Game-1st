package com.flowtown.lite.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.flowtown.lite.engine.ActiveVehicleState
import com.flowtown.lite.engine.ParticleEvent
import com.flowtown.lite.engine.ParticleType
import com.flowtown.lite.model.Building
import com.flowtown.lite.model.BuildingType
import com.flowtown.lite.model.GameState
import com.flowtown.lite.model.LevelData
import com.flowtown.lite.model.RoadNode
import com.flowtown.lite.model.ServiceRequest
import com.flowtown.lite.model.Vehicle
import com.flowtown.lite.model.VehicleType
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GameCanvasBoard(
    levelData: LevelData,
    assignedRoutes: Map<String, List<String>>,
    selectedVehicleId: String?,
    gameState: GameState,
    activeVehicleStates: Map<String, ActiveVehicleState>,
    requests: List<ServiceRequest>,
    particles: List<ParticleEvent> = emptyList(),
    onNodeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeMap = remember(levelData) { levelData.nodes.associateBy { it.id } }
    val vehicleMap = remember(levelData) { levelData.vehicles.associateBy { it.id } }
    val buildingMap = remember(levelData) { levelData.buildings.associateBy { it.id } }

    // Pulsing animation for destination markers
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceOffset"
    )

    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashOffset"
    )

    val emojiPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 38f
        }
    }

    val badgePaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 22f
            isFakeBoldText = true
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gameState, levelData, selectedVehicleId) {
                if (gameState == GameState.PLANNING && selectedVehicleId != null) {
                    detectTapGestures { tapOffset ->
                        val tappedNode = findNearestNode(
                            tapOffset.x,
                            tapOffset.y,
                            size.width.toFloat(),
                            size.height.toFloat(),
                            levelData.nodes
                        )
                        tappedNode?.let { onNodeSelected(it.id) }
                    }
                }
            }
            .pointerInput(gameState, levelData, selectedVehicleId) {
                if (gameState == GameState.PLANNING && selectedVehicleId != null) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val tappedNode = findNearestNode(
                                startOffset.x,
                                startOffset.y,
                                size.width.toFloat(),
                                size.height.toFloat(),
                                levelData.nodes
                            )
                            tappedNode?.let { onNodeSelected(it.id) }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val draggedNode = findNearestNode(
                                change.position.x,
                                change.position.y,
                                size.width.toFloat(),
                                size.height.toFloat(),
                                levelData.nodes
                            )
                            draggedNode?.let { onNodeSelected(it.id) }
                        }
                    )
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // 1. Town District Meadow Backdrop
        drawDistrictBackdrop(width, height)

        // 2. Road Network (Curbs, Asphalt, Dashed Lane Lines)
        drawRoads(levelData, nodeMap, width, height)

        // 3. Tire Skid Marks from Drifts
        drawSkidMarks(particles, width, height)

        // 4. Planned Routes with Directional Flow Animation
        drawPlannedRoutes(
            assignedRoutes,
            nodeMap,
            vehicleMap,
            selectedVehicleId,
            dashOffset,
            width,
            height
        )

        // 5. Road Junction Nodes
        drawRoadNodes(levelData.nodes, width, height)

        // 6. Destination Pulse Targets & Doorstep Halos
        drawDestinationPulses(requests, buildingMap, nodeMap, pulseScale, width, height)

        // 7. Rich 2D Illustrated Buildings (Homes, Bakeries, Hospitals, etc.)
        drawIllustratedBuildings(
            levelData.buildings,
            requests,
            nodeMap,
            bounceOffset,
            width,
            height,
            emojiPaint,
            badgePaint
        )

        // 8. High-Detail 2D Vehicles with Dynamic Drift Oversteer Angle
        drawVehicles(
            levelData.vehicles,
            assignedRoutes,
            nodeMap,
            activeVehicleStates,
            gameState,
            pulseScale,
            width,
            height
        )

        // 9. Particle Spark & Star Bursts
        drawDynamicParticles(particles, width, height)
    }
}

private fun findNearestNode(
    touchX: Float,
    touchY: Float,
    width: Float,
    height: Float,
    nodes: List<RoadNode>
): RoadNode? {
    val hitRadiusPx = width * 0.16f // Generous touch tolerance for seamless single-finger route drawing
    var nearestNode: RoadNode? = null
    var minDistance = Float.MAX_VALUE

    for (node in nodes) {
        val nx = node.x * width
        val ny = node.y * height
        val dist = sqrt((touchX - nx) * (touchX - nx) + (touchY - ny) * (touchY - ny))
        if (dist <= hitRadiusPx && dist < minDistance) {
            minDistance = dist
            nearestNode = node
        }
    }
    return nearestNode
}

private fun DrawScope.drawDistrictBackdrop(width: Float, height: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
        )
    )

    val lotPadding = width * 0.035f

    // Soft Town Ground Plot
    drawRoundRect(
        color = Color(0xFFDCEDC8).copy(alpha = 0.6f),
        topLeft = Offset(lotPadding, lotPadding),
        size = Size(width - lotPadding * 2, height - lotPadding * 2),
        cornerRadius = CornerRadius(28f, 28f)
    )

    // Subtle isometric road grid guides
    val gridColor = Color(0x12000000)
    val step = width * 0.14f
    var x = step
    while (x < width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
        x += step
    }
}

private fun DrawScope.drawRoads(
    levelData: LevelData,
    nodeMap: Map<String, RoadNode>,
    width: Float,
    height: Float
) {
    val curbWidth = width * 0.095f
    val roadWidth = width * 0.082f

    // 1. Concrete Curbs
    for (edge in levelData.edges) {
        val n1 = nodeMap[edge.fromNodeId] ?: continue
        val n2 = nodeMap[edge.toNodeId] ?: continue
        val start = Offset(n1.x * width, n1.y * height)
        val end = Offset(n2.x * width, n2.y * height)

        drawLine(
            color = if (edge.isBlocked) Color(0xFFB0BEC5) else Color(0xFF263238),
            start = start,
            end = end,
            strokeWidth = curbWidth,
            cap = StrokeCap.Round
        )
    }

    // 2. Asphalt Road Surfaces
    for (edge in levelData.edges) {
        val n1 = nodeMap[edge.fromNodeId] ?: continue
        val n2 = nodeMap[edge.toNodeId] ?: continue
        val start = Offset(n1.x * width, n1.y * height)
        val end = Offset(n2.x * width, n2.y * height)

        val asphaltColor = if (edge.isBlocked) {
            Color(0xFFCFD8DC)
        } else if (edge.speedModifier > 1.1f) {
            Color(0xFF37474F)
        } else {
            Color(0xFF455A64)
        }

        drawLine(
            color = asphaltColor,
            start = start,
            end = end,
            strokeWidth = roadWidth,
            cap = StrokeCap.Round
        )

        // Center dashed lane stripes
        if (!edge.isBlocked) {
            val stripeColor = if (edge.speedModifier > 1.1f) Color(0xFFFFD54F) else Color(0xFFECEFF1)
            drawLine(
                color = stripeColor.copy(alpha = 0.85f),
                start = start,
                end = end,
                strokeWidth = 2.8f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )
        }
    }
}

private fun DrawScope.drawSkidMarks(particles: List<ParticleEvent>, width: Float, height: Float) {
    for (p in particles) {
        if (p.type == ParticleType.SKID_MARK) {
            val cx = p.x * width
            val cy = p.y * height
            rotate(degrees = p.angle, pivot = Offset(cx, cy)) {
                // Double tire rubber tracks
                drawLine(
                    color = Color(0x66212121),
                    start = Offset(cx - 10f, cy - 8f),
                    end = Offset(cx + 10f, cy - 8f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0x66212121),
                    start = Offset(cx - 10f, cy + 8f),
                    end = Offset(cx + 10f, cy + 8f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawPlannedRoutes(
    assignedRoutes: Map<String, List<String>>,
    nodeMap: Map<String, RoadNode>,
    vehicleMap: Map<String, Vehicle>,
    selectedVehicleId: String?,
    dashOffset: Float,
    width: Float,
    height: Float
) {
    for ((vehicleId, route) in assignedRoutes) {
        if (route.size < 2) continue
        val vehicle = vehicleMap[vehicleId] ?: continue
        val isSelected = vehicleId == selectedVehicleId
        val baseColor = Color(vehicle.colorHex)

        for (i in 0 until route.size - 1) {
            val n1 = nodeMap[route[i]] ?: continue
            val n2 = nodeMap[route[i + 1]] ?: continue
            val start = Offset(n1.x * width, n1.y * height)
            val end = Offset(n2.x * width, n2.y * height)

            // Outer vibrant glow
            if (isSelected) {
                drawLine(
                    color = baseColor.copy(alpha = 0.42f),
                    start = start,
                    end = end,
                    strokeWidth = width * 0.075f,
                    cap = StrokeCap.Round
                )
            }

            // Core neon route line
            drawLine(
                color = baseColor,
                start = start,
                end = end,
                strokeWidth = if (isSelected) width * 0.038f else width * 0.022f,
                cap = StrokeCap.Round
            )

            // Animated directional flow dots
            drawLine(
                color = Color.White,
                start = start,
                end = end,
                strokeWidth = 4f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 24f), dashOffset)
            )
        }
    }
}

private fun DrawScope.drawRoadNodes(
    nodes: List<RoadNode>,
    width: Float,
    height: Float
) {
    for (node in nodes) {
        val center = Offset(node.x * width, node.y * height)

        // Outer rim
        drawCircle(
            color = Color(0xFF212121),
            radius = width * 0.032f,
            center = center
        )
        // Inner junction disc
        drawCircle(
            color = if (node.isIntersection) Color(0xFFFFB300) else Color(0xFFFAFAFA),
            radius = width * 0.022f,
            center = center
        )
    }
}

private fun DrawScope.drawDestinationPulses(
    requests: List<ServiceRequest>,
    buildingMap: Map<String, Building>,
    nodeMap: Map<String, RoadNode>,
    pulseScale: Float,
    width: Float,
    height: Float
) {
    for (req in requests) {
        if (req.isCompleted) continue
        val destBuilding = buildingMap[req.destinationBuildingId] ?: continue
        val node = nodeMap[destBuilding.attachedNodeId] ?: continue
        val center = Offset(node.x * width, node.y * height)

        // Pulsing radar ring on the doorstep node
        drawCircle(
            color = Color(0xFFFF5722).copy(alpha = (1.3f - pulseScale * 0.8f).coerceIn(0.1f, 0.5f)),
            radius = width * 0.075f * pulseScale,
            center = center,
            style = Stroke(width = 3.5f)
        )
    }
}

private fun DrawScope.drawIllustratedBuildings(
    buildings: List<Building>,
    requests: List<ServiceRequest>,
    nodeMap: Map<String, RoadNode>,
    bounceOffset: Float,
    width: Float,
    height: Float,
    emojiPaint: Paint,
    badgePaint: Paint
) {
    val bWidth = width * 0.18f
    val bHeight = width * 0.18f

    for (b in buildings) {
        val node = nodeMap[b.attachedNodeId] ?: continue
        val cx = node.x * width
        val cy = node.y * height

        val offsetX = if (node.x > 0.5f) cx + width * 0.055f else cx - width * 0.055f - bWidth
        val offsetY = if (node.y > 0.5f) cy + width * 0.04f else cy - width * 0.04f - bHeight

        val bx = offsetX.coerceIn(8f, width - bWidth - 8f)
        val by = offsetY.coerceIn(8f, height - bHeight - 8f)

        // 1. Building Soft Drop Shadow
        drawRoundRect(
            color = Color(0x38000000),
            topLeft = Offset(bx + 4f, by + 6f),
            size = Size(bWidth, bHeight),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // 2. Render Distinct Rich 2D Architecture based on BuildingType
        when (b.type) {
            BuildingType.HOUSE -> {
                // Cottage Base Wall (Warm Terracotta Brick)
                drawRoundRect(
                    color = Color(0xFFFFCCBC),
                    topLeft = Offset(bx, by + bHeight * 0.35f),
                    size = Size(bWidth, bHeight * 0.65f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                // Pitched Gable Slate Roof
                val roofPath = Path().apply {
                    moveTo(bx - 3f, by + bHeight * 0.38f)
                    lineTo(bx + bWidth / 2f, by)
                    lineTo(bx + bWidth + 3f, by + bHeight * 0.38f)
                    close()
                }
                drawPath(roofPath, color = Color(0xFFD84315))

                // Brick Chimney
                drawRoundRect(
                    color = Color(0xFFBF360C),
                    topLeft = Offset(bx + bWidth * 0.70f, by + bHeight * 0.05f),
                    size = Size(bWidth * 0.18f, bHeight * 0.28f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Chimney Cozy Smoke Puff
                drawCircle(
                    color = Color(0x77ECEFF1),
                    radius = 5f,
                    center = Offset(bx + bWidth * 0.79f, by - 2f)
                )

                // Cute Wooden Front Door
                drawRoundRect(
                    color = Color(0xFF6D4C41),
                    topLeft = Offset(bx + bWidth * 0.38f, by + bHeight * 0.65f),
                    size = Size(bWidth * 0.24f, bHeight * 0.35f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Golden Doorknob
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = 2f,
                    center = Offset(bx + bWidth * 0.56f, by + bHeight * 0.82f)
                )

                // Lighted Windows with warm glow
                drawRoundRect(
                    color = Color(0xFFFFF9C4),
                    topLeft = Offset(bx + bWidth * 0.12f, by + bHeight * 0.50f),
                    size = Size(bWidth * 0.20f, bHeight * 0.20f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = Color(0xFFFFF9C4),
                    topLeft = Offset(bx + bWidth * 0.68f, by + bHeight * 0.50f),
                    size = Size(bWidth * 0.20f, bHeight * 0.20f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
            }

            BuildingType.BAKERY -> {
                // Bakery Warm Cream Base
                drawRoundRect(
                    color = Color(0xFFFFE0B2),
                    topLeft = Offset(bx, by),
                    size = Size(bWidth, bHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Striped Awning (Orange & White)
                drawRoundRect(
                    color = Color(0xFFFF9800),
                    topLeft = Offset(bx, by),
                    size = Size(bWidth, bHeight * 0.34f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                for (s in 1..3) {
                    drawRect(
                        color = Color(0xFFFFF3E0),
                        topLeft = Offset(bx + s * (bWidth / 4f) - 6f, by),
                        size = Size(12f, bHeight * 0.34f)
                    )
                }

                // Bakery Display Window
                drawRoundRect(
                    color = Color(0xFFFFF8E1),
                    topLeft = Offset(bx + bWidth * 0.15f, by + bHeight * 0.44f),
                    size = Size(bWidth * 0.70f, bHeight * 0.46f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawContext.canvas.nativeCanvas.drawText("🍞", bx + bWidth / 2f, by + bHeight * 0.80f, emojiPaint)
            }

            BuildingType.HOSPITAL -> {
                // Hospital Clean Facade
                drawRoundRect(
                    color = Color(0xFFFAFAFA),
                    topLeft = Offset(bx, by),
                    size = Size(bWidth, bHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = Color(0xFFE53935),
                    topLeft = Offset(bx, by),
                    size = Size(bWidth, bHeight * 0.28f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // Red Cross Emblem
                val hcx = bx + bWidth / 2f
                val hcy = by + bHeight * 0.60f
                drawRect(Color(0xFFE53935), topLeft = Offset(hcx - 5f, hcy - 14f), size = Size(10f, 28f))
                drawRect(Color(0xFFE53935), topLeft = Offset(hcx - 14f, hcy - 5f), size = Size(28f, 10f))
            }

            BuildingType.APARTMENT -> {
                // Modern Multi-Floor Apartment
                drawRoundRect(
                    color = Color(0xFFB2DFDB),
                    topLeft = Offset(bx, by),
                    size = Size(bWidth, bHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = Color(0xFF00897B),
                    topLeft = Offset(bx, by),
                    size = Size(bWidth, bHeight * 0.24f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                // 4 Window Panes
                drawRect(Color(0xFFE0F2F1), topLeft = Offset(bx + bWidth * 0.18f, by + bHeight * 0.36f), size = Size(bWidth * 0.24f, bHeight * 0.20f))
                drawRect(Color(0xFFE0F2F1), topLeft = Offset(bx + bWidth * 0.58f, by + bHeight * 0.36f), size = Size(bWidth * 0.24f, bHeight * 0.20f))
                drawRect(Color(0xFFE0F2F1), topLeft = Offset(bx + bWidth * 0.18f, by + bHeight * 0.64f), size = Size(bWidth * 0.24f, bHeight * 0.20f))
                drawRect(Color(0xFFE0F2F1), topLeft = Offset(bx + bWidth * 0.58f, by + bHeight * 0.64f), size = Size(bWidth * 0.24f, bHeight * 0.20f))
            }

            else -> {
                // Default Building Frame
                drawRoundRect(
                    color = Color(0xFFE1BEE7),
                    topLeft = Offset(bx, by),
                    size = Size(bWidth, bHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawContext.canvas.nativeCanvas.drawText("🏢", bx + bWidth / 2f, by + bHeight * 0.72f, emojiPaint)
            }
        }

        // Crisp Outer Building Border
        drawRoundRect(
            color = Color(0xFF37474F),
            topLeft = Offset(bx, by),
            size = Size(bWidth, bHeight),
            cornerRadius = CornerRadius(14f, 14f),
            style = Stroke(width = 2.8f)
        )

        // 3. Floating Animated Bouncy Request Callout Pin
        val pendingReq = requests.firstOrNull { it.destinationBuildingId == b.id && !it.isCompleted }
        val completedReq = requests.firstOrNull { it.destinationBuildingId == b.id && it.isCompleted }

        if (pendingReq != null) {
            val pinX = bx + bWidth / 2f
            val pinY = by - 12f + bounceOffset

            // Pin Shadow
            drawCircle(Color(0x33000000), radius = 16f, center = Offset(pinX + 2f, pinY + 3f))
            // Pin Body
            drawCircle(Color(0xFFFF5722), radius = 17f, center = Offset(pinX, pinY))
            drawCircle(Color.White, radius = 17f, center = Offset(pinX, pinY), style = Stroke(width = 2.5f))

            badgePaint.color = android.graphics.Color.WHITE
            drawContext.canvas.nativeCanvas.drawText(
                pendingReq.serviceType.iconEmoji,
                pinX,
                pinY + 8f,
                badgePaint
            )
        } else if (completedReq != null) {
            val pinX = bx + bWidth / 2f
            val pinY = by - 10f

            drawCircle(Color(0xFF4CAF50), radius = 16f, center = Offset(pinX, pinY))
            badgePaint.color = android.graphics.Color.WHITE
            drawContext.canvas.nativeCanvas.drawText("✓", pinX, pinY + 6f, badgePaint)
        }
    }
}

private fun DrawScope.drawVehicles(
    vehicles: List<Vehicle>,
    assignedRoutes: Map<String, List<String>>,
    nodeMap: Map<String, RoadNode>,
    activeStates: Map<String, ActiveVehicleState>,
    gameState: GameState,
    pulseScale: Float,
    width: Float,
    height: Float
) {
    for (v in vehicles) {
        val (vx, vy, angle) = if (gameState == GameState.SIMULATING || gameState == GameState.PAUSED || gameState == GameState.SUCCESS || gameState == GameState.FAILURE) {
            val state = activeStates[v.id]
            if (state != null) {
                Triple(state.currentX * width, state.currentY * height, state.driftVisualAngle)
            } else {
                val startNode = nodeMap[v.startingNodeId]
                Triple(startNode?.let { it.x * width } ?: (width / 2f), startNode?.let { it.y * height } ?: (height / 2f), 0f)
            }
        } else {
            val startNode = nodeMap[v.startingNodeId]
            Triple(startNode?.let { it.x * width } ?: (width / 2f), startNode?.let { it.y * height } ?: (height / 2f), 0f)
        }

        rotate(degrees = angle, pivot = Offset(vx, vy)) {
            val vLength = width * 0.088f
            val vWidth = width * 0.054f
            val vColor = Color(v.colorHex)

            // 1. Vehicle Drop Shadow
            drawRoundRect(
                color = Color(0x4D000000),
                topLeft = Offset(vx - vLength / 2f + 3f, vy - vWidth / 2f + 3.5f),
                size = Size(vLength, vWidth),
                cornerRadius = CornerRadius(9f, 9f)
            )

            // 2. Main Chassis Body
            drawRoundRect(
                color = vColor,
                topLeft = Offset(vx - vLength / 2f, vy - vWidth / 2f),
                size = Size(vLength, vWidth),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // 3. Windshield & Windows
            drawRoundRect(
                color = Color(0xFF1E272E),
                topLeft = Offset(vx + vLength * 0.08f, vy - vWidth * 0.36f),
                size = Size(vLength * 0.34f, vWidth * 0.72f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Rear Window
            drawRoundRect(
                color = Color(0xFF1E272E),
                topLeft = Offset(vx - vLength * 0.40f, vy - vWidth * 0.30f),
                size = Size(vLength * 0.16f, vWidth * 0.60f),
                cornerRadius = CornerRadius(3f, 3f)
            )

            // 4. Vehicle Type Special Accents
            when (v.type) {
                VehicleType.TAXI, VehicleType.GOLDEN_TAXI -> {
                    // Taxi Roof Sign
                    drawRoundRect(
                        color = Color(0xFFFFD600),
                        topLeft = Offset(vx - vLength * 0.08f, vy - vWidth * 0.20f),
                        size = Size(vLength * 0.22f, vWidth * 0.40f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
                VehicleType.AMBULANCE -> {
                    // Flashing Strobe Beacon
                    drawCircle(
                        color = if (pulseScale > 1.05f) Color(0xFFFF1744) else Color(0xFF2979FF),
                        radius = 4f,
                        center = Offset(vx - vLength * 0.05f, vy)
                    )
                }
                VehicleType.ICE_CREAM_TRUCK -> {
                    drawCircle(
                        color = Color(0xFFF06292),
                        radius = 4.5f,
                        center = Offset(vx - vLength * 0.08f, vy)
                    )
                }
                VehicleType.CYBER_VAN -> {
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(vx - vLength * 0.35f, vy),
                        end = Offset(vx + vLength * 0.10f, vy),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                else -> {
                    // Delivery Package Box on Roof
                    drawRoundRect(
                        color = Color(0xFF8D6E63),
                        topLeft = Offset(vx - vLength * 0.28f, vy - vWidth * 0.22f),
                        size = Size(vLength * 0.24f, vWidth * 0.44f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }

            // 5. Headlights
            drawCircle(
                color = Color(0xFFFFF9C4),
                radius = 3.5f,
                center = Offset(vx + vLength / 2f - 1f, vy - vWidth * 0.32f)
            )
            drawCircle(
                color = Color(0xFFFFF9C4),
                radius = 3.5f,
                center = Offset(vx + vLength / 2f - 1f, vy + vWidth * 0.32f)
            )

            // 6. Outer Outline
            drawRoundRect(
                color = Color(0xFF212121),
                topLeft = Offset(vx - vLength / 2f, vy - vWidth / 2f),
                size = Size(vLength, vWidth),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2.4f)
            )
        }
    }
}

private fun DrawScope.drawDynamicParticles(particles: List<ParticleEvent>, width: Float, height: Float) {
    for (p in particles) {
        val px = p.x * width
        val py = p.y * height
        when (p.type) {
            ParticleType.STAR -> {
                drawCircle(color = Color(p.colorHex), radius = 6f, center = Offset(px, py))
            }
            ParticleType.SPARK -> {
                drawCircle(color = Color(p.colorHex), radius = 5f, center = Offset(px, py))
            }
            ParticleType.SMOKE -> {
                drawCircle(color = Color(p.colorHex), radius = 7f, center = Offset(px, py))
            }
            else -> {}
        }
    }
}
