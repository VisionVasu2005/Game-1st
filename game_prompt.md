# Project: FlowTown Lite — Native Android 2D Route Puzzle Game

Act as a senior Android game architect and Kotlin developer. Plan and build a lightweight portrait-oriented 2D puzzle game called **FlowTown Lite**.

Follow this specification strictly. Do not introduce Unity, third-party game engines, 3D graphics, physics engines or heavy rendering systems.

---

## 1. Product Goal

Create a lightweight native Android game with:

* Simple one-finger gameplay
* Smooth performance on low-end Android devices
* Short puzzle levels
* Long-term town restoration progression
* Strong daily retention potential
* Broad global audience appeal
* Low language dependency
* Offline-first gameplay
* Ad and IAP readiness

The main player loop is:

1. A building or resident needs a service.
2. The player draws a route on fixed roads.
3. The player presses Start.
4. Vehicles follow the planned routes.
5. The player avoids collisions and completes requests.
6. The player earns stars and construction materials.
7. Rewards restore buildings in the town.
8. Restored buildings unlock new puzzle mechanics.

---

## 2. Strict Technical Constraints

The project must use:

* Kotlin
* Native Android SDK
* Jetpack Compose for regular application UI
* A custom Android `View` with `Canvas` for active gameplay
* MVVM or unidirectional state architecture
* Coroutines and StateFlow
* Room for persistent player progress
* DataStore for preferences
* Hilt or clean Constructor Injection
* Gradle Kotlin DSL
* Material 3 for menus and dialogs

Do not use:

* Unity, Unreal, Godot, LibGDX, Cocos2d
* OpenGL or Vulkan directly
* 3D models or physics engines
* WebView gameplay

The game must be entirely portrait-oriented.

---

## 3. Game Concept & Board Architecture

Graph-based road network with normalized coordinates (`0f` to `1f`).
Components:
- `RoadNode`: ID, x, y, type, connected road IDs
- `RoadEdge`: ID, fromNodeId, toNodeId, bidirectional / one-way, travel cost, blocked state
- `Building`: ID, buildingType, attachedNodeId, name, service provided
- `Vehicle`: ID, vehicleType, startingNodeId, color, speed
- `ServiceRequest`: ID, originBuildingId, destinationBuildingId, requiredService, timeLimit, reward

---

## 4. Simulation & Route Rules

- Touch and drag over connected road nodes with hit-radius snap
- Reversing over latest segment removes it (undo)
- Deterministic fixed-timestep simulation (20-30 updates/sec) with interpolated rendering
- Logical collision detection on segments & intersections
- Success when all requests fulfilled, failure on collision / timeout / unfulfilled
