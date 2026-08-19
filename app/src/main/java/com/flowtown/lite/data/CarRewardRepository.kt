package com.flowtown.lite.data

import com.flowtown.lite.model.CarReward
import com.flowtown.lite.model.VehicleType

object CarRewardRepository {

    val allRewards: List<CarReward> = listOf(
        CarReward(
            id = "car_classic_van",
            name = "Classic Baker Van",
            type = VehicleType.DELIVERY_VAN,
            description = "The reliable neighborhood workhorse. Smooth handling with high bakery capacity.",
            requiredStars = 0,
            colorHex = 0xFFFF9800,
            speedBonus = "Standard (1.0x)",
            isUnlocked = true,
            isSelected = true
        ),
        CarReward(
            id = "car_ice_cream",
            name = "Sweet Swirl Truck",
            type = VehicleType.ICE_CREAM_TRUCK,
            description = "Plays happy chimes wherever it travels! Brings smiles to every child and resident.",
            requiredStars = 10,
            colorHex = 0xFFEC407A,
            speedBonus = "+10% Speed",
            isUnlocked = false
        ),
        CarReward(
            id = "car_limited_solar",
            name = "⚡ Solar Sprint GT",
            type = VehicleType.CYBER_VAN,
            description = "LIMITED TIME EVENT: Ultra-aerodynamic solar battery vehicle with sleek neon cyan glow!",
            requiredStars = 25,
            isLimitedTime = true,
            remainingTimeFormatted = "2d 14h Left",
            colorHex = 0xFF00E5FF,
            speedBonus = "+25% Acceleration",
            isUnlocked = false
        ),
        CarReward(
            id = "car_golden_taxi",
            name = "Golden Crown Taxi",
            type = VehicleType.GOLDEN_TAXI,
            description = "VIP transportation crafted with gold plating. Earning 2x town prestige on every drop-off.",
            requiredStars = 50,
            colorHex = 0xFFFFD700,
            speedBonus = "+20% Speed",
            isUnlocked = false
        ),
        CarReward(
            id = "car_cyber_ambulance",
            name = "Apex Rescue Ambulance",
            type = VehicleType.AMBULANCE,
            description = "High-speed medical interceptor equipped with siren strobes and intersection bypass tech.",
            requiredStars = 75,
            colorHex = 0xFFE53935,
            speedBonus = "+35% Speed Boost",
            isUnlocked = false
        )
    )
}
