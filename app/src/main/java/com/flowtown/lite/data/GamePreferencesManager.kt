package com.flowtown.lite.data

import android.content.Context
import android.content.SharedPreferences
import com.flowtown.lite.model.PlayerSettings

class GamePreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("flowtown_prefs", Context.MODE_PRIVATE)

    fun getCompletedLevels(): Set<Int> {
        val raw = prefs.getStringSet("completed_levels", emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun markLevelCompleted(levelNumber: Int, stars: Int) {
        val completed = getCompletedLevels().toMutableSet()
        completed.add(levelNumber)
        prefs.edit()
            .putStringSet("completed_levels", completed.map { it.toString() }.toSet())
            .apply()

        val currentBest = getStarsForLevel(levelNumber)
        if (stars > currentBest) {
            prefs.edit().putInt("stars_lvl_$levelNumber", stars).apply()
        }
    }

    fun getStarsForLevel(levelNumber: Int): Int {
        return prefs.getInt("stars_lvl_$levelNumber", 0)
    }

    fun getTotalStars(): Int {
        var total = 0
        for (i in 1..100) {
            total += getStarsForLevel(i)
        }
        return total
    }

    fun getUnlockedCars(): Set<String> {
        val defaultUnlocked = setOf("car_classic_van")
        val saved = prefs.getStringSet("unlocked_cars", defaultUnlocked) ?: defaultUnlocked
        return (defaultUnlocked + saved)
    }

    fun unlockCar(carId: String) {
        val cars = getUnlockedCars().toMutableSet()
        cars.add(carId)
        prefs.edit().putStringSet("unlocked_cars", cars).apply()
    }

    fun getSelectedCar(): String {
        return prefs.getString("selected_car", "car_classic_van") ?: "car_classic_van"
    }

    fun setSelectedCar(carId: String) {
        prefs.edit().putString("selected_car", carId).apply()
    }

    fun getSettings(): PlayerSettings {
        return PlayerSettings(
            soundEnabled = prefs.getBoolean("setting_sound", true),
            vibrationEnabled = prefs.getBoolean("setting_vibration", true),
            reducedAnimations = prefs.getBoolean("setting_reduced_anim", false)
        )
    }

    fun saveSettings(settings: PlayerSettings) {
        prefs.edit()
            .putBoolean("setting_sound", settings.soundEnabled)
            .putBoolean("setting_vibration", settings.vibrationEnabled)
            .putBoolean("setting_reduced_anim", settings.reducedAnimations)
            .apply()
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
    }
}
