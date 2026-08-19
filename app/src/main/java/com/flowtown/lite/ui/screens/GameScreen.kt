package com.flowtown.lite.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtown.lite.model.AppScreen
import com.flowtown.lite.model.GameState
import com.flowtown.lite.ui.GameUiState
import com.flowtown.lite.ui.GameViewModel
import com.flowtown.lite.ui.components.GameCanvasBoard

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = Color(0xFFF1F8E9)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Top Header: Back button, Level Title, District & Controls
                GameTopHeader(
                    uiState = uiState,
                    onBackToHome = { viewModel.navigateTo(AppScreen.HOME_ROADMAP) },
                    onPauseToggle = {
                        if (uiState.gameState == GameState.SIMULATING) {
                            viewModel.pauseSimulation()
                        } else if (uiState.gameState == GameState.PAUSED) {
                            viewModel.resumeSimulation()
                        }
                    },
                    onRetry = { viewModel.retryLevel() }
                )

                // 2. Main Game Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFE8F5E9))
                        .border(2.dp, Color(0xFFC8E6C9), RoundedCornerShape(22.dp))
                ) {
                    GameCanvasBoard(
                        levelData = uiState.levelData,
                        assignedRoutes = uiState.assignedRoutes,
                        selectedVehicleId = uiState.selectedVehicleId,
                        gameState = uiState.gameState,
                        activeVehicleStates = uiState.activeVehicleStates,
                        requests = uiState.requests,
                        onNodeSelected = { nodeId ->
                            viewModel.onNodeInput(nodeId)
                        }
                    )
                }

                // 3. Bottom Control Bar: Responsive, no text clipping, minimum 48dp touch targets
                GameBottomControls(
                    uiState = uiState,
                    onSelectVehicle = { viewModel.selectVehicle(it) },
                    onUndo = { viewModel.undoRoute() },
                    onClear = { viewModel.clearRoute() },
                    onStartSimulation = { viewModel.startSimulation() }
                )
            }

            // 4. Result Overlay Dialog (Success / Failure)
            if (uiState.gameState == GameState.SUCCESS || uiState.gameState == GameState.FAILURE) {
                ResultOverlay(
                    uiState = uiState,
                    onRetry = { viewModel.retryLevel() },
                    onNextLevel = { viewModel.nextLevel() },
                    onHome = { viewModel.navigateTo(AppScreen.HOME_ROADMAP) }
                )
            }
        }
    }
}

@Composable
private fun GameTopHeader(
    uiState: GameUiState,
    onBackToHome: () -> Unit,
    onPauseToggle: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button + Level Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToHome,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("back_to_roadmap_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Roadmap",
                            tint = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = "Level ${uiState.levelData.levelNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = uiState.levelData.districtName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF558B2F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls & Timer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    uiState.levelData.timeLimitSeconds?.let { limit ->
                        val remaining = (limit - uiState.elapsedTimeSeconds).coerceAtLeast(0f)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (remaining < 5f) Color(0xFFFFEBEE) else Color(0xFFFFF3E0),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = if (remaining < 5f) Color(0xFFD32F2F) else Color(0xFFE65100),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${remaining.toInt()}s",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (remaining < 5f) Color(0xFFD32F2F) else Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    if (uiState.gameState == GameState.SIMULATING || uiState.gameState == GameState.PAUSED) {
                        IconButton(
                            onClick = onPauseToggle,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("pause_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.gameState == GameState.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = Color(0xFF37474F)
                            )
                        }
                    }

                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("retry_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = Color(0xFF37474F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Objective description pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF1F8E9)
            ) {
                Text(
                    text = uiState.levelData.objectiveDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF33691E),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun GameBottomControls(
    uiState: GameUiState,
    onSelectVehicle: (String) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onStartSimulation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Vehicle Selection Chips (Horizontally scrollable for safety)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vehicle:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF455A64)
                )

                uiState.levelData.vehicles.forEach { vehicle ->
                    val isSelected = vehicle.id == uiState.selectedVehicleId
                    val routePoints = uiState.assignedRoutes[vehicle.id]?.size ?: 1
                    val badgeColor = Color(vehicle.colorHex)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) badgeColor.copy(alpha = 0.18f) else Color(0xFFF5F5F5),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, badgeColor) else null,
                        modifier = Modifier
                            .clickable(enabled = uiState.gameState == GameState.PLANNING) {
                                onSelectVehicle(vehicle.id)
                            }
                            .testTag("vehicle_selector_${vehicle.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${vehicle.type.displayName} ($routePoints)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color(0xFF263238)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Undo, Clear, Start Simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onUndo,
                    enabled = uiState.gameState == GameState.PLANNING,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("undo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Undo,
                        contentDescription = "Undo",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Undo", fontSize = 13.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onClear,
                    enabled = uiState.gameState == GameState.PLANNING,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 13.sp, maxLines = 1)
                }

                Button(
                    onClick = onStartSimulation,
                    enabled = uiState.gameState == GameState.PLANNING,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("start_simulation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start Simulation",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                }
            }

            // Status message
            uiState.feedbackMessage?.let { msg ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF546E7A),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ResultOverlay(
    uiState: GameUiState,
    onRetry: () -> Unit,
    onNextLevel: () -> Unit,
    onHome: () -> Unit
) {
    val isSuccess = uiState.gameState == GameState.SUCCESS

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSuccess) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        repeat(3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Text(
                        text = "Town Delivered! 🎉",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2E7D32)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "All service deliveries reached on time without traffic crashes!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF558B2F)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onNextLevel,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("next_level_button")
                    ) {
                        Text("Next Level", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onHome,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Roadmap", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text(
                        text = uiState.failureReason?.title ?: "💥 Delivery Failed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFC62828)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = uiState.failureReason?.message ?: "Please adjust vehicle routes and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF616161)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("retry_result_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onHome,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Roadmap", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
