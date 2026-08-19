package com.flowtown.lite.model

enum class AppScreen {
    HOME_ROADMAP,
    GAMEPLAY,
    GARAGE_REWARDS,
    SETTINGS
}

enum class BuildingType {
    BAKERY,
    HOUSE,
    HOSPITAL,
    APARTMENT,
    DELIVERY_STATION,
    TAXI_STATION,
    ICE_CREAM_PARLOR,
    FIRE_STATION
}

enum class VehicleType(val displayName: String, val speed: Float, val iconEmoji: String) {
    DELIVERY_VAN("Delivery Van", 0.35f, "🚐"),
    TAXI("Taxi Cab", 0.45f, "🚖"),
    AMBULANCE("Ambulance", 0.55f, "🚑"),
    ICE_CREAM_TRUCK("Ice Cream Truck", 0.38f, "🍦"),
    CYBER_VAN("Cyber Cruiser", 0.50f, "⚡"),
    GOLDEN_TAXI("Golden Luxury", 0.48f, "👑")
}

enum class ServiceType(val displayName: String, val iconEmoji: String) {
    BREAD_DELIVERY("Fresh Bread", "🍞"),
    PARCEL_DELIVERY("Parcel Package", "📦"),
    PASSENGER_PICKUP("Taxi Pickup", "🚖"),
    EMERGENCY("Medical Emergency", "🚨"),
    ICE_CREAM("Sweet Treats", "🍦")
}

enum class GameState {
    PLANNING,
    SIMULATING,
    PAUSED,
    SUCCESS,
    FAILURE
}

enum class FailureReason(val title: String, val message: String) {
    COLLISION("Traffic Collision!", "Two vehicles crossed the same road space at the same moment. Plan alternate routes or adjust distances!"),
    TIMEOUT("Time Expired!", "Urgent service was not delivered before the countdown ran out."),
    WRONG_DESTINATION("Wrong Destination!", "Vehicle arrived at an unassigned building."),
    UNSERVED_DESTINATION("Incomplete Delivery!", "Some requested buildings were not reached by your route plan."),
    ROUTE_INCOMPLETE("No Route Defined!", "Please draw a route from the building to your destination.")
}

data class RoadNode(
    val id: String,
    val x: Float, // Normalized 0.0f .. 1.0f
    val y: Float, // Normalized 0.0f .. 1.0f
    val isIntersection: Boolean = false,
    val connectedRoadIds: List<String> = emptyList()
)

data class RoadEdge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val isBidirectional: Boolean = true,
    val isBlocked: Boolean = false,
    val speedModifier: Float = 1.0f
)

data class Building(
    val id: String,
    val type: BuildingType,
    val name: String,
    val attachedNodeId: String,
    val serviceProvided: ServiceType? = null
)

data class Vehicle(
    val id: String,
    val type: VehicleType,
    val startingNodeId: String,
    val colorHex: Long,
    val priority: Int = 1,
    val customName: String? = null
)

data class ServiceRequest(
    val id: String,
    val originBuildingId: String,
    val destinationBuildingId: String,
    val serviceType: ServiceType,
    val timeLimitSeconds: Float? = null,
    val rewardStars: Int = 3,
    val isCompleted: Boolean = false
)

data class LevelData(
    val id: String,
    val districtName: String,
    val levelNumber: Int,
    val objectiveDescription: String,
    val nodes: List<RoadNode>,
    val edges: List<RoadEdge>,
    val buildings: List<Building>,
    val vehicles: List<Vehicle>,
    val requests: List<ServiceRequest>,
    val timeLimitSeconds: Float? = null,
    val difficulty: String = "Normal"
)

data class LevelProgress(
    val levelNumber: Int,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val starsEarned: Int = 0,
    val bestTimeSeconds: Float = 0f
)

data class CarReward(
    val id: String,
    val name: String,
    val type: VehicleType,
    val description: String,
    val requiredStars: Int,
    val isLimitedTime: Boolean = false,
    val remainingTimeFormatted: String? = null,
    val colorHex: Long,
    val speedBonus: String,
    val isUnlocked: Boolean = false,
    val isSelected: Boolean = false
)

data class PlayerSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val reducedAnimations: Boolean = false
)
