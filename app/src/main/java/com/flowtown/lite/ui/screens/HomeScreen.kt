package com.flowtown.lite.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowtown.lite.data.LevelRepository
import com.flowtown.lite.model.AppScreen
import com.flowtown.lite.model.LevelData
import com.flowtown.lite.ui.GameUiState
import com.flowtown.lite.ui.GameViewModel
import com.flowtown.lite.ui.dialogs.GarageRewardsDialog
import com.flowtown.lite.ui.dialogs.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showGarageDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val districtNames = listOf(
        "Maple Grove",
        "Riverbend",
        "Downtown",
        "Industrial",
        "Sunset Coast"
    )

    val currentDistrictLevels = LevelRepository.getLevelsForDistrict(uiState.selectedDistrictIndex)
    val completedInDistrict = currentDistrictLevels.count { uiState.completedLevels.contains(it.levelNumber) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = Color(0xFFF1F8E9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar
            HomeTopBar(
                totalStars = uiState.totalStars,
                onGarageClick = { showGarageDialog = true },
                onSettingsClick = { showSettingsDialog = true }
            )

            // District Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedDistrictIndex,
                containerColor = Color(0xFFF1F8E9),
                contentColor = Color(0xFF2E7D32),
                edgePadding = 16.dp,
                divider = {}
            ) {
                districtNames.forEachIndexed { index, name ->
                    Tab(
                        selected = uiState.selectedDistrictIndex == index,
                        onClick = { viewModel.selectDistrict(index) },
                        text = {
                            Text(
                                text = "D${index + 1}: $name",
                                fontWeight = if (uiState.selectedDistrictIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // District Banner Card
            DistrictSummaryCard(
                districtIndex = uiState.selectedDistrictIndex,
                districtName = districtNames[uiState.selectedDistrictIndex],
                completed = completedInDistrict,
                total = currentDistrictLevels.size
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 100 Levels Grid (20 per district)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(currentDistrictLevels) { level ->
                    LevelRoadmapNode(
                        level = level,
                        isUnlocked = level.levelNumber <= uiState.highestUnlockedLevel,
                        isCompleted = uiState.completedLevels.contains(level.levelNumber),
                        stars = uiState.starsPerLevel[level.levelNumber] ?: 0,
                        isCurrent = level.levelNumber == uiState.highestUnlockedLevel,
                        onClick = {
                            viewModel.startLevel(level.levelNumber - 1)
                        }
                    )
                }
            }

            // Bottom Quick Play Button
            val nextLevelToPlay = uiState.highestUnlockedLevel.coerceIn(1, 100)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { viewModel.startLevel(nextLevelToPlay - 1) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("play_next_level_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play Level $nextLevelToPlay",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showGarageDialog) {
        GarageRewardsDialog(
            rewards = uiState.carRewards,
            totalStars = uiState.totalStars,
            onSelectCar = { viewModel.selectCarReward(it) },
            onDismiss = { showGarageDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = uiState.settings,
            onUpdateSettings = { viewModel.updateSettings(it) },
            onResetProgress = {
                viewModel.resetAllData()
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun HomeTopBar(
    totalStars: Int,
    onGarageClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "FlowTown Lite",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1B5E20)
            )
            Text(
                text = "Route Puzzle Adventure • 100 Levels",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF558B2F)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Star Counter Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFFF9C4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Stars",
                        tint = Color(0xFFFF8F00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$totalStars ⭐",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFE65100)
                    )
                }
            }

            // Garage Button
            IconButton(
                onClick = onGarageClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2F1))
                    .testTag("garage_button")
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = "Garage Rewards",
                    tint = Color(0xFF00796B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE7F6))
                    .testTag("settings_button")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Game Settings",
                    tint = Color(0xFF512DA8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DistrictSummaryCard(
    districtIndex: Int,
    districtName: String,
    completed: Int,
    total: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "District ${districtIndex + 1}: $districtName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Levels ${(districtIndex * 20) + 1} - ${((districtIndex + 1) * 20)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                }

                Text(
                    text = "$completed / $total",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (completed.toFloat() / total.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE8F5E9)
            )
        }
    }
}

@Composable
private fun LevelRoadmapNode(
    level: LevelData,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    stars: Int,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isCompleted -> Color(0xFFC8E6C9)
        isCurrent -> Color(0xFF81C784)
        isUnlocked -> Color.White
        else -> Color(0xFFE0E0E0)
    }

    val borderColor = when {
        isCurrent -> Color(0xFF1B5E20)
        isCompleted -> Color(0xFF4CAF50)
        isUnlocked -> Color(0xFF81C784)
        else -> Color(0xFFBDBDBD)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_node_${level.levelNumber}")
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${level.levelNumber}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = if (isCurrent) Color(0xFF1B5E20) else Color(0xFF263238)
                    )

                    if (isCompleted) {
                        Row(modifier = Modifier.padding(top = 1.dp)) {
                            repeat(stars.coerceAtLeast(1)) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "L${level.levelNumber}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isUnlocked) Color(0xFF424242) else Color(0xFF9E9E9E),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
