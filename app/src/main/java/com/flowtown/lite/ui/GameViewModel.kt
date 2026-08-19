package com.flowtown.lite.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flowtown.lite.audio.SoundManager
import com.flowtown.lite.data.CarRewardRepository
import com.flowtown.lite.data.GamePreferencesManager
import com.flowtown.lite.data.LevelRepository
import com.flowtown.lite.engine.ActiveVehicleState
import com.flowtown.lite.engine.GameGraph
import com.flowtown.lite.engine.ParticleEvent
import com.flowtown.lite.engine.RouteModificationResult
import com.flowtown.lite.engine.RouteValidator
import com.flowtown.lite.engine.SimulationEngine
import com.flowtown.lite.model.AppScreen
import com.flowtown.lite.model.CarReward
import com.flowtown.lite.model.FailureReason
import com.flowtown.lite.model.GameState
import com.flowtown.lite.model.LevelData
import com.flowtown.lite.model.PlayerSettings
import com.flowtown.lite.model.ServiceRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameUiState(
    val currentScreen: AppScreen = AppScreen.HOME_ROADMAP,
    val levelData: LevelData,
    val levelIndex: Int = 0,
    val selectedVehicleId: String? = null,
    val assignedRoutes: Map<String, List<String>> = emptyMap(),
    val gameState: GameState = GameState.PLANNING,
    val activeVehicleStates: Map<String, ActiveVehicleState> = emptyMap(),
    val requests: List<ServiceRequest> = emptyList(),
    val failureReason: FailureReason? = null,
    val elapsedTimeSeconds: Float = 0f,
    val particles: List<ParticleEvent> = emptyList(),
    val feedbackMessage: String? = null,
    // Progression & Roadmap
    val completedLevels: Set<Int> = emptySet(),
    val highestUnlockedLevel: Int = 1,
    val totalStars: Int = 0,
    val starsPerLevel: Map<Int, Int> = emptyMap(),
    val selectedDistrictIndex: Int = 0,
    // Rewards & Garage
    val carRewards: List<CarReward> = emptyList(),
    val selectedCarId: String = "car_classic_van",
    val newlyUnlockedCar: CarReward? = null,
    // Settings
    val settings: PlayerSettings = PlayerSettings()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = GamePreferencesManager(application)
    private var simulationEngine: SimulationEngine? = null
    private var simulationJob: Job? = null

    private val _uiState = MutableStateFlow(createInitialState(0))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
        val completed = prefs.getCompletedLevels()
        val totalStars = prefs.getTotalStars()
        val highest = (completed.maxOrNull() ?: 0) + 1
        val unlockedCars = prefs.getUnlockedCars()
        val selectedCar = prefs.getSelectedCar()
        val settings = prefs.getSettings()

        SoundManager.isSoundEnabled = settings.soundEnabled

        val starsMap = mutableMapOf<Int, Int>()
        for (i in 1..100) {
            val s = prefs.getStarsForLevel(i)
            if (s > 0) starsMap[i] = s
        }

        val updatedRewards = CarRewardRepository.allRewards.map { reward ->
            val isUnlocked = unlockedCars.contains(reward.id) || (reward.requiredStars <= totalStars)
            if (isUnlocked && !unlockedCars.contains(reward.id)) {
                prefs.unlockCar(reward.id)
            }
            reward.copy(
                isUnlocked = isUnlocked,
                isSelected = (reward.id == selectedCar)
            )
        }

        _uiState.update {
            it.copy(
                completedLevels = completed,
                highestUnlockedLevel = highest.coerceAtMost(100),
                totalStars = totalStars,
                starsPerLevel = starsMap,
                carRewards = updatedRewards,
                selectedCarId = selectedCar,
                settings = settings
            )
        }
    }

    private fun createInitialState(levelIndex: Int): GameUiState {
        val level = LevelRepository.getLevel(levelIndex)
        val initialRoutes = mutableMapOf<String, List<String>>()
        level.vehicles.forEach { v ->
            initialRoutes[v.id] = listOf(v.startingNodeId)
        }
        return GameUiState(
            currentScreen = AppScreen.HOME_ROADMAP,
            levelData = level,
            levelIndex = levelIndex,
            selectedVehicleId = level.vehicles.firstOrNull()?.id,
            assignedRoutes = initialRoutes,
            gameState = GameState.PLANNING,
            requests = level.requests,
            feedbackMessage = "Select a vehicle and drag along the road network!"
        )
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.HOME_ROADMAP) {
            simulationJob?.cancel()
            simulationEngine = null
        }
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun selectDistrict(districtIndex: Int) {
        _uiState.update { it.copy(selectedDistrictIndex = districtIndex.coerceIn(0, 4)) }
    }

    fun startLevel(levelIndex: Int) {
        val levelNum = levelIndex + 1
        if (levelNum > _uiState.value.highestUnlockedLevel && levelNum > 1) {
            _uiState.update { it.copy(feedbackMessage = "Complete previous levels to unlock Level $levelNum!") }
            return
        }
        simulationJob?.cancel()
        simulationEngine = null
        val level = LevelRepository.getLevel(levelIndex)
        val initialRoutes = mutableMapOf<String, List<String>>()
        level.vehicles.forEach { v ->
            initialRoutes[v.id] = listOf(v.startingNodeId)
        }
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.GAMEPLAY,
                levelData = level,
                levelIndex = levelIndex,
                selectedVehicleId = level.vehicles.firstOrNull()?.id,
                assignedRoutes = initialRoutes,
                gameState = GameState.PLANNING,
                requests = level.requests,
                elapsedTimeSeconds = 0f,
                particles = emptyList(),
                failureReason = null,
                feedbackMessage = "Drag along road lines to connect to destination!"
            )
        }
    }

    fun selectVehicle(vehicleId: String) {
        if (_uiState.value.gameState != GameState.PLANNING) return
        _uiState.update { it.copy(selectedVehicleId = vehicleId) }
    }

    fun onNodeInput(nodeId: String) {
        val currentState = _uiState.value
        if (currentState.gameState != GameState.PLANNING) return
        val activeVehicleId = currentState.selectedVehicleId ?: return
        val vehicle = currentState.levelData.vehicles.firstOrNull { it.id == activeVehicleId } ?: return

        val currentRoute = currentState.assignedRoutes[activeVehicleId] ?: listOf(vehicle.startingNodeId)
        val graph = GameGraph(currentState.levelData)
        val validator = RouteValidator(graph)

        when (val result = validator.processNodeInput(currentRoute, nodeId, vehicle.startingNodeId)) {
            is RouteModificationResult.Appended -> {
                val newRoutes = currentState.assignedRoutes.toMutableMap()
                newRoutes[activeVehicleId] = result.newRoute
                SoundManager.playNodeConnect()
                _uiState.update {
                    it.copy(
                        assignedRoutes = newRoutes,
                        feedbackMessage = "Road connected! Keep dragging to destination."
                    )
                }
            }
            is RouteModificationResult.Popped -> {
                val newRoutes = currentState.assignedRoutes.toMutableMap()
                newRoutes[activeVehicleId] = result.newRoute
                _uiState.update {
                    it.copy(
                        assignedRoutes = newRoutes,
                        feedbackMessage = "Previous segment undone"
                    )
                }
            }
            is RouteModificationResult.NoChange -> {
                // No-op
            }
            is RouteModificationResult.Rejected -> {
                _uiState.update {
                    it.copy(feedbackMessage = result.reason)
                }
            }
        }
    }

    fun undoRoute() {
        val currentState = _uiState.value
        if (currentState.gameState != GameState.PLANNING) return
        val activeVehicleId = currentState.selectedVehicleId ?: return
        val currentRoute = currentState.assignedRoutes[activeVehicleId] ?: return
        if (currentRoute.size > 1) {
            val newRoutes = currentState.assignedRoutes.toMutableMap()
            newRoutes[activeVehicleId] = currentRoute.dropLast(1)
            _uiState.update {
                it.copy(
                    assignedRoutes = newRoutes,
                    feedbackMessage = "Undid step"
                )
            }
        }
    }

    fun clearRoute() {
        val currentState = _uiState.value
        if (currentState.gameState != GameState.PLANNING) return
        val activeVehicleId = currentState.selectedVehicleId ?: return
        val vehicle = currentState.levelData.vehicles.firstOrNull { it.id == activeVehicleId } ?: return

        val newRoutes = currentState.assignedRoutes.toMutableMap()
        newRoutes[activeVehicleId] = listOf(vehicle.startingNodeId)
        _uiState.update {
            it.copy(
                assignedRoutes = newRoutes,
                feedbackMessage = "Route cleared"
            )
        }
    }

    fun startSimulation() {
        val currentState = _uiState.value
        if (currentState.gameState != GameState.PLANNING) return

        val hasAnyRoute = currentState.assignedRoutes.values.any { it.size >= 2 }
        if (!hasAnyRoute) {
            _uiState.update {
                it.copy(feedbackMessage = "Draw a route along the road before starting!")
            }
            return
        }

        SoundManager.playEngineRev()
        simulationEngine = SimulationEngine(currentState.levelData, currentState.assignedRoutes)
        _uiState.update {
            it.copy(
                gameState = GameState.SIMULATING,
                failureReason = null,
                elapsedTimeSeconds = 0f,
                particles = emptyList(),
                feedbackMessage = "Vehicles cruising & drifting…"
            )
        }

        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            // High-frequency 60 FPS update rate (16ms) for buttery smooth vehicle motion
            val dt = 0.016f
            while (isActive) {
                val engine = simulationEngine ?: break
                val frame = engine.step(dt)

                if (frame.triggeredDriftSound) {
                    SoundManager.playDriftSound()
                }
                if (frame.triggeredSuccessSound) {
                    SoundManager.playDeliverySuccess()
                }
                if (frame.triggeredCrashSound) {
                    SoundManager.playCrashSound()
                }

                _uiState.update { current ->
                    current.copy(
                        gameState = frame.gameState,
                        failureReason = frame.failureReason,
                        activeVehicleStates = frame.vehicleStates,
                        requests = frame.requests,
                        elapsedTimeSeconds = frame.elapsedTimeSeconds,
                        particles = if (frame.newParticles.isNotEmpty()) {
                            (current.particles + frame.newParticles).takeLast(24)
                        } else current.particles,
                        feedbackMessage = when (frame.gameState) {
                            GameState.SUCCESS -> "Delivered! ⭐⭐⭐"
                            GameState.FAILURE -> frame.failureReason?.message ?: "Delivery failed"
                            else -> current.feedbackMessage
                        }
                    )
                }

                if (frame.gameState == GameState.SUCCESS) {
                    onLevelSuccess()
                    break
                } else if (frame.gameState == GameState.FAILURE) {
                    break
                }

                delay(16L) // 60 FPS smooth stepping
            }
        }
    }

    private fun onLevelSuccess() {
        val levelNum = _uiState.value.levelData.levelNumber
        val stars = 3
        prefs.markLevelCompleted(levelNum, stars)
        loadPersistedState()
    }

    fun pauseSimulation() {
        if (_uiState.value.gameState == GameState.SIMULATING) {
            simulationJob?.cancel()
            _uiState.update { it.copy(gameState = GameState.PAUSED, feedbackMessage = "Simulation Paused") }
        }
    }

    fun resumeSimulation() {
        if (_uiState.value.gameState == GameState.PAUSED) {
            _uiState.update { it.copy(gameState = GameState.SIMULATING, feedbackMessage = "Simulation Resumed") }
            startSimulationLoop()
        }
    }

    fun retryLevel() {
        startLevel(_uiState.value.levelIndex)
    }

    fun nextLevel() {
        val nextIdx = (_uiState.value.levelIndex + 1).coerceAtMost(LevelRepository.levels.size - 1)
        startLevel(nextIdx)
    }

    fun selectCarReward(rewardId: String) {
        val reward = _uiState.value.carRewards.firstOrNull { it.id == rewardId } ?: return
        if (reward.isUnlocked) {
            prefs.setSelectedCar(rewardId)
            loadPersistedState()
        }
    }

    fun updateSettings(newSettings: PlayerSettings) {
        prefs.saveSettings(newSettings)
        SoundManager.isSoundEnabled = newSettings.soundEnabled
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun resetAllData() {
        prefs.resetAllProgress()
        loadPersistedState()
        startLevel(0)
        navigateTo(AppScreen.HOME_ROADMAP)
    }
}
