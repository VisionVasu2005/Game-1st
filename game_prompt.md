{\rtf1\ansi\ansicpg1252\cocoartf2639
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fswiss\fcharset0 Helvetica;\f1\fnil\fcharset0 LucidaGrande;}
{\colortbl;\red255\green255\blue255;}
{\*\expandedcolortbl;;}
\paperw11900\paperh16840\margl1440\margr1440\vieww11520\viewh8400\viewkind0
\pard\tx566\tx1133\tx1700\tx2267\tx2834\tx3401\tx3968\tx4535\tx5102\tx5669\tx6236\tx6803\pardirnatural\partightenfactor0

\f0\fs24 \cf0 # Project: FlowTown Lite \'97 Native Android 2D Route Puzzle Game\
\
Act as a senior Android game architect and Kotlin developer. Plan and build a lightweight portrait-oriented 2D puzzle game called **FlowTown Lite**.\
\
Follow this specification strictly. Do not introduce Unity, third-party game engines, 3D graphics, physics engines or heavy rendering systems.\
\
---\
\
## 1. Product Goal\
\
Create a lightweight native Android game with:\
\
* Simple one-finger gameplay\
* Smooth performance on low-end Android devices\
* Short puzzle levels\
* Long-term town restoration progression\
* Strong daily retention potential\
* Broad global audience appeal\
* Low language dependency\
* Offline-first gameplay\
* Ad and IAP readiness\
\
The main player loop is:\
\
1. A building or resident needs a service.\
2. The player draws a route on fixed roads.\
3. The player presses Start.\
4. Vehicles follow the planned routes.\
5. The player avoids collisions and completes requests.\
6. The player earns stars and construction materials.\
7. Rewards restore buildings in the town.\
8. Restored buildings unlock new puzzle mechanics.\
\
---\
\
## 2. Strict Technical Constraints\
\
The project must use:\
\
* Kotlin\
* Native Android SDK\
* Jetpack Compose for regular application UI\
* A custom Android `View` with `Canvas` for active gameplay\
* MVVM or unidirectional state architecture\
* Coroutines and StateFlow\
* Room for persistent player progress\
* DataStore for preferences\
* Hilt for dependency injection\
* Gradle Kotlin DSL\
* Material 3 for menus and dialogs\
\
Do not use:\
\
* Unity\
* Unreal Engine\
* Godot\
* LibGDX\
* Cocos2d\
* OpenGL directly\
* Vulkan directly\
* 3D models\
* Physics engines\
* WebView gameplay\
* Video backgrounds\
* Large Lottie animations\
* Heavy particle systems\
* Real-time multiplayer\
* Continuous background services\
\
The game must be entirely portrait-oriented.\
\
---\
\
## 3. Target Devices\
\
Optimize for:\
\
* Android 8.0 and newer\
* Minimum SDK 26\
* Target current stable Android SDK\
* Devices with 3 GB RAM\
* Budget and mid-range Android devices\
* Different portrait aspect ratios\
* 60 FPS on capable devices\
* Graceful 30 FPS mode on weaker devices\
* Offline gameplay after installation\
\
Initial performance targets:\
\
* App download size below 60 MB\
* Gameplay RAM target below 180 MB\
* Maximum 12 visible vehicles\
* Maximum 20 visible buildings\
* Maximum 50 road nodes\
* Maximum 8 simultaneous requests\
* Maximum 20 lightweight particles\
* No unnecessary bitmap allocation during gameplay\
\
Treat these as profiling targets, not guaranteed values.\
\
---\
\
## 4. Game Concept\
\
The player manages a small illustrated 2D town.\
\
Buildings generate service requests such as:\
\
* Bread delivery\
* Parcel delivery\
* Taxi pickup\
* Ambulance emergency\
* School bus pickup\
* Fire response\
* Garbage collection\
\
The player connects service buildings to destinations by drawing routes over a fixed road network.\
\
Example:\
\
```text\
Bakery 
\f1 \uc0\u8594 
\f0  Junction A 
\f1 \uc0\u8594 
\f0  Junction B 
\f1 \uc0\u8594 
\f0  House\
```\
\
When the player presses Start, all assigned vehicles move simultaneously.\
\
The player succeeds when:\
\
* Every required destination is served\
* Vehicles do not collide\
* Priority requests are completed on time\
* Vehicles follow valid road connections\
\
The player fails when:\
\
* Two vehicles collide\
* A timed request expires\
* A vehicle reaches an incorrect destination\
* A required destination is not served\
* The move or route limit is exceeded\
\
---\
\
## 5. Visual Direction\
\
Use a clean, friendly, flat 2D style.\
\
The game board should feel like a polished illustrated board game, not a realistic simulator.\
\
Use:\
\
* Flat top-down map\
* Soft but limited colour palette\
* Simple road shapes\
* Small 2D vehicle sprites\
* Flat building illustrations\
* Clear visual request icons\
* Lightweight shadows baked into assets\
* Small scale, opacity and translation animations\
* Minimal completion particles\
\
Do not use:\
\
* Isometric 3D\
* Perspective camera\
* Real-time shadows\
* Complex lighting\
* Large weather effects\
* Hundreds of animated residents\
* Excessive glow\
* Large transparent bitmap layers\
\
Use WebP assets wherever appropriate.\
\
---\
\
## 6. Screen Structure\
\
Create the following screens:\
\
### Splash Screen\
\
* Small app logo\
* Lightweight static background\
* Fast transition to the home screen\
\
### Home/Town Screen\
\
* Restored town illustration\
* Player stars and materials\
* Current district\
* Continue button\
* Daily challenge access\
* Building upgrade indicators\
* Settings button\
\
### Level Selection Screen\
\
* District progress\
* Completed levels\
* Locked levels\
* Star ratings\
* Current level highlight\
\
### Gameplay Screen\
\
Portrait composition:\
\
* Top area: level objective, timer, moves and pause\
* Middle area: interactive town puzzle board\
* Bottom area: available vehicles, route tools, undo and Start\
* All important controls should be reachable with one hand\
\
### Level Result Screen\
\
* Success or failure result\
* Stars earned\
* Materials earned\
* Perfect City bonus\
* Retry button\
* Continue button\
* Optional rewarded bonus placeholder\
\
### Restoration Screen\
\
* Damaged building\
* Upgrade requirements\
* Restore action\
* Lightweight before/after animation\
* Newly unlocked mechanic\
\
### Daily Challenge Screen\
\
* Daily puzzle\
* One main reward\
* Completion state\
* Daily streak\
\
### Settings Screen\
\
* Music\
* Sound\
* Vibration\
* Reduced animation mode\
* Language-ready structure\
* Privacy and consent placeholders\
\
---\
\
## 7. Game Board Architecture\
\
Represent every road network as a graph.\
\
### RoadNode\
\
Each road node should contain:\
\
* Unique ID\
* Normalized X coordinate\
* Normalized Y coordinate\
* Node type\
* Connected road IDs\
* Optional intersection metadata\
\
Use normalized coordinates from `0f` to `1f` so the same level scales correctly across different screen sizes.\
\
### RoadEdge\
\
Each road edge should contain:\
\
* Unique ID\
* Starting node ID\
* Ending node ID\
* Road direction\
* Travel cost\
* Speed modifier\
* Blocked state\
* One-way state\
* Intersection occupancy rules\
\
### Building\
\
Each building should contain:\
\
* Unique ID\
* Building type\
* Attached road node\
* Current visual state\
* Service provided\
* Request compatibility\
* Upgrade level\
\
### Vehicle\
\
Each vehicle should contain:\
\
* Unique ID\
* Vehicle type\
* Starting node\
* Assigned route\
* Current route index\
* Current segment progress\
* Movement speed\
* Capacity\
* Current state\
* Priority level\
\
### ServiceRequest\
\
Each request should contain:\
\
* Unique ID\
* Origin building\
* Destination building\
* Required service type\
* Optional time limit\
* Reward value\
* Completion state\
* Priority level\
\
---\
\
## 8. Player Route Input\
\
The player must draw only on valid connected roads.\
\
Recommended input flow:\
\
1. Player selects a vehicle.\
2. Player touches its starting node.\
3. Player drags across connected road nodes.\
4. Valid road segments become highlighted.\
5. Invalid movement is rejected visually.\
6. Reversing over the latest segment removes it.\
7. Releasing the finger confirms the route.\
8. The route can be edited before simulation starts.\
\
Do not use pixel-perfect freehand pathfinding.\
\
Convert touch positions into the nearest valid road node using a configurable hit radius.\
\
Keep input and rendering code separate from puzzle rules.\
\
---\
\
## 9. Simulation Rules\
\
Use a deterministic fixed-timestep simulation.\
\
Recommended logic update rate:\
\
* 20\'9630 updates per second\
\
Rendering can interpolate vehicle positions for smooth movement.\
\
The simulation should:\
\
* Advance each vehicle along its assigned route\
* Track current and next road nodes\
* Reserve intersections when required\
* Detect vehicle conflicts\
* Complete matching service requests\
* Process request timers\
* Trigger success or failure\
* Pause and resume safely\
\
Do not execute pathfinding every frame.\
\
Routes are prepared before simulation and stored as lists of node IDs.\
\
Collision detection should be logical rather than physics-based.\
\
Example:\
\
* Two vehicles reserve the same intersection during overlapping time windows.\
* Vehicles occupy incompatible positions on the same road segment.\
* Opposite-direction vehicles enter a single-lane road simultaneously.\
\
The simulation must produce identical results for identical routes and starting conditions.\
\
---\
\
## 10. Level Data\
\
Levels must be data-driven and loaded from local JSON files.\
\
Do not hardcode individual levels inside Kotlin classes.\
\
Each level should define:\
\
* Level ID\
* District ID\
* Objective\
* Road nodes\
* Road edges\
* Buildings\
* Vehicles\
* Requests\
* Obstacles\
* Timers\
* Move limits\
* Available tools\
* Star conditions\
* Reward values\
* Tutorial instructions\
\
Create a JSON parser and level validator.\
\
The validator must detect:\
\
* Duplicate IDs\
* Missing node references\
* Disconnected required destinations\
* Invalid building attachments\
* Unsupported vehicle types\
* Impossible empty routes\
* Invalid reward values\
\
---\
\
## 11. MVP Content\
\
Build the architecture for expansion, but implement only the following MVP content:\
\
### District\
\
* One small neighbourhood district\
\
### Services\
\
* Delivery van\
* Taxi\
* Ambulance\
\
### Buildings\
\
* Bakery\
* Delivery station\
* Taxi station\
* Hospital\
* House\
* Apartment\
\
### Road mechanics\
\
* Basic two-way road\
* One-way road\
* Intersection\
* Blocked road\
* Priority route\
\
### Levels\
\
Create 20 test levels:\
\
* Levels 1\'963: single vehicle tutorial\
* Levels 4\'966: multiple destinations\
* Levels 7\'9610: two vehicles\
* Levels 11\'9614: collision management\
* Levels 15\'9617: ambulance timer\
* Levels 18\'9620: combined mechanics\
\
Difficulty must increase gradually.\
\
---\
\
## 12. Town Restoration\
\
The town is not a fully simulated open world.\
\
Use fixed clickable building slots.\
\
Each building has three visual states:\
\
```text\
Damaged 
\f1 \uc0\u8594 
\f0  Restored 
\f1 \uc0\u8594 
\f0  Upgraded\
```\
\
The player spends stars and materials to change a building\'92s state.\
\
Restoring buildings should unlock content:\
\
* Bakery unlocks delivery puzzles\
* Taxi station unlocks taxi puzzles\
* Hospital unlocks ambulance puzzles\
* Apartment unlocks additional residents\
* Upgraded buildings increase reward opportunities\
\
Store all restoration progress locally.\
\
---\
\
## 13. Progression System\
\
Track:\
\
* Current district\
* Highest unlocked level\
* Level completion\
* Best star rating\
* Total stars\
* Construction materials\
* Restored buildings\
* Daily challenge state\
* Daily streak\
* Tutorial completion\
* Player settings\
\
Make progression rules configurable rather than scattered across UI code.\
\
Do not add complicated currencies during the MVP.\
\
MVP currencies:\
\
* Stars\
* Construction materials\
\
---\
\
## 14. Retention Foundation\
\
Prepare architecture for:\
\
* Daily challenge\
* Daily streak\
* Three daily objectives\
* Resident gifts\
* Weekly events\
* Endless mode\
* Building collections\
* Vehicle cosmetics\
* Seasonal districts\
\
For the MVP, implement only:\
\
* Daily challenge framework\
* Daily streak\
* Three simple daily objectives\
* Local reward tracking\
\
Do not implement multiplayer or server-authoritative events.\
\
Use interfaces so remote event support can be added later.\
\
---\
\
## 15. Monetization Readiness\
\
Prepare abstractions for:\
\
* Rewarded ads\
* Interstitial ads\
* No-ads purchase\
* Starter pack\
* Cosmetic purchases\
* Season pass\
\
Do not tightly couple AdMob or Billing code to gameplay.\
\
Create interfaces such as:\
\
```kotlin\
interface RewardedAdManager\
interface InterstitialAdManager\
interface PurchaseManager\
```\
\
For the initial development build, use fake implementations.\
\
Expected rewarded placements:\
\
* Route hint\
* Retry after failure\
* Double construction material\
* Extra daily challenge attempt\
\
Never interrupt an active simulation with an advertisement.\
\
---\
\
## 16. Recommended Module Structure\
\
Use a modular or cleanly separated package structure:\
\
```text\
app\
core-model\
core-database\
core-preferences\
core-analytics\
core-designsystem\
game-engine\
game-renderer\
game-levels\
feature-home\
feature-levelselect\
feature-gameplay\
feature-restoration\
feature-dailychallenge\
feature-settings\
feature-result\
```\
\
If a full multi-module project creates unnecessary initial complexity, begin with one app module but preserve these package boundaries.\
\
Explain the selected approach before implementation.\
\
---\
\
## 17. State Architecture\
\
Use immutable UI state.\
\
Gameplay should have clearly defined states:\
\
```text\
Loading\
Planning\
Simulating\
Paused\
Success\
Failure\
Error\
```\
\
Use explicit player actions:\
\
```text\
SelectVehicle\
StartRoute\
ExtendRoute\
RemoveLastSegment\
ConfirmRoute\
ClearRoute\
StartSimulation\
PauseSimulation\
ResumeSimulation\
RetryLevel\
ExitLevel\
```\
\
Do not allow composables or Canvas drawing code to directly modify game state.\
\
The game engine must be unit-testable without Android UI dependencies.\
\
---\
\
## 18. Performance Requirements\
\
Apply these rules:\
\
* Preload only assets needed for the active level\
* Reuse Paint objects\
* Avoid allocating objects inside the draw loop\
* Cache scaled static bitmaps\
* Recycle or release unused bitmap references\
* Pause simulation when the app enters the background\
* Avoid recomposing the entire screen for every vehicle position\
* Keep high-frequency rendering state outside normal Compose recomposition\
* Draw the active board using one custom Canvas view\
* Use frame timing measurements\
* Support reduced animation mode\
* Avoid unnecessary transparency and overdraw\
* Avoid unbounded coroutines\
* Never write to Room on every simulation frame\
\
Static layers may be cached into an off-screen bitmap where profiling shows a benefit.\
\
Do not optimize blindly; measure using Android Studio Profiler and frame metrics.\
\
---\
\
## 19. Save and Resume Behaviour\
\
Persist progress after meaningful events:\
\
* Level completion\
* Building restoration\
* Currency change\
* Daily reward collection\
* Settings change\
\
If the app closes during a puzzle:\
\
* Save the level ID\
* Save planning-stage routes if simulation has not started\
* Do not resume a partially running simulation\
* Return the player to planning state\
* Clearly explain that the attempt was restored\
\
Room database migrations must be supported from the beginning.\
\
---\
\
## 20. Analytics Plan\
\
Create an analytics abstraction.\
\
Track events such as:\
\
```text\
game_opened\
tutorial_started\
tutorial_completed\
level_started\
route_created\
route_changed\
simulation_started\
level_failed\
failure_reason\
level_completed\
level_retried\
hint_requested\
rewarded_ad_offered\
rewarded_ad_completed\
building_restored\
daily_challenge_started\
daily_challenge_completed\
session_ended\
```\
\
Useful properties:\
\
* Level ID\
* District ID\
* Attempt number\
* Level duration\
* Planning duration\
* Simulation duration\
* Failure reason\
* Routes created\
* Vehicles used\
* Stars earned\
* Player progression stage\
\
Do not collect personally identifiable information.\
\
---\
\
## 21. Testing Requirements\
\
Create unit tests for:\
\
* Graph connections\
* Route validation\
* One-way road rules\
* Route reversal\
* Vehicle movement\
* Intersection reservation\
* Logical collision detection\
* Request completion\
* Timer expiration\
* Success conditions\
* Failure conditions\
* Star calculation\
* Level JSON validation\
* Progression unlocking\
* Daily streak calculation\
\
Create UI or instrumentation tests for:\
\
* Selecting a vehicle\
* Drawing a valid route\
* Rejecting an invalid route\
* Starting simulation\
* Pausing and resuming\
* Completing the tutorial\
* Restoring a building\
* Saving and restoring progress\
\
Add at least one deterministic simulation test where the same input is executed repeatedly and always produces the same result.\
\
---\
\
## 22. Accessibility\
\
Support:\
\
* Large touch targets\
* Colour-independent route identification\
* Icons plus colours\
* Sound toggle\
* Vibration toggle\
* Reduced animation mode\
* Adequate contrast\
* Screen-reader labels for menus\
* No essential information communicated only through animation\
\
The Canvas gameplay itself may use limited accessibility semantics, but all controls and objectives must have accessible labels.\
\
---\
\
## 23. Implementation Phases\
\
Do not attempt the entire product in one step.\
\
### Phase 1: Technical Prototype\
\
Build:\
\
* Empty Android project\
* Portrait orientation\
* Compose navigation\
* Custom gameplay Canvas\
* Fixed road graph\
* One vehicle\
* Route drawing\
* Start simulation\
* Vehicle movement\
* Success condition\
\
Do not add restoration, ads or daily systems yet.\
\
### Phase 2: Core Puzzle Engine\
\
Add:\
\
* Multiple vehicles\
* Multiple requests\
* Collision logic\
* Timers\
* One-way roads\
* Obstacles\
* Pause and retry\
* JSON level loading\
* Unit tests\
\
### Phase 3: MVP User Experience\
\
Add:\
\
* Home screen\
* Level selection\
* Result screen\
* Tutorial\
* Audio and vibration\
* Initial 20 levels\
* Basic visual polish\
\
### Phase 4: Progression\
\
Add:\
\
* Room database\
* Stars\
* Construction materials\
* Restoration screen\
* Building states\
* Content unlocking\
* Save and resume\
\
### Phase 5: Retention Systems\
\
Add:\
\
* Daily challenge\
* Daily streak\
* Daily objectives\
* Local notifications only if explicitly approved\
* Analytics abstraction\
\
### Phase 6: Monetization Preparation\
\
Add:\
\
* Fake ad managers\
* Fake purchase manager\
* Monetization placements\
* Consent-ready interfaces\
\
Do not add production ad SDKs until the core game retention has been tested.\
\
### Phase 7: Optimization and Release Preparation\
\
Perform:\
\
* Low-end device testing\
* Memory profiling\
* Frame-time profiling\
* APK/AAB size review\
* Accessibility review\
* Crash testing\
* Database migration testing\
* Release build verification\
\
---\
\
## 24. AI Agent Working Rules\
\
Before writing code:\
\
1. Inspect the existing repository.\
2. Identify the current project state.\
3. Check Gradle and Android SDK configuration.\
4. Present a short implementation plan.\
5. Identify assumptions and risks.\
6. Start only with the next incomplete phase.\
\
While working:\
\
* Do not rewrite unrelated existing code.\
* Do not replace working architecture without justification.\
* Keep commits or changes logically separated.\
* Prefer small testable components.\
* Run compilation after meaningful changes.\
* Run relevant tests.\
* Fix compilation errors before adding more features.\
* Avoid placeholder architecture that cannot be executed.\
* Do not create unnecessary abstraction layers.\
* Do not silently change the game rules.\
* Document important technical decisions.\
* Keep the project runnable after each phase.\
\
If information is missing, make the safest lightweight assumption and document it.\
\
Ask for clarification only when the decision would significantly change gameplay, architecture, cost or project scope.\
\
---\
\
## 25. First Required Deliverable\
\
For the first development cycle, implement only a playable technical prototype containing:\
\
* Portrait Android application\
* One gameplay screen\
* Simple flat 2D board\
* 10\'9615 fixed road nodes\
* One bakery\
* One house\
* One delivery van\
* Route drawing between connected nodes\
* Clear route display\
* Undo route action\
* Start button\
* Smooth vehicle movement\
* Delivery completion\
* Basic success result\
* Retry button\
* One unit test for route validation\
* One deterministic simulation test\
\
Use temporary programmatic shapes where final artwork is unavailable.\
\
Do not build the restoration system during the first cycle.\
\
---\
\
## 26. Prototype Acceptance Criteria\
\
The prototype is accepted only when:\
\
* It builds successfully.\
* It launches without crashing.\
* It remains locked to portrait orientation.\
* The player can draw only across connected road nodes.\
* Invalid connections are rejected.\
* The route can be undone or cleared.\
* The vehicle follows the selected route smoothly.\
* The simulation result is deterministic.\
* Reaching the correct house completes the request.\
* Retry fully resets the level.\
* Gameplay logic is separated from Canvas rendering.\
* No Unity or external game engine is present.\
* No 3D or physics system is present.\
* No unnecessary bitmap allocation occurs inside the draw loop.\
* Automated tests pass.\
\
---\
\
## 27. Response Format\
\
Respond initially with:\
\
1. Repository assessment\
2. Selected architecture\
3. Package or module structure\
4. Phase 1 implementation plan\
5. Files that will be created or modified\
6. Performance risks\
7. Testing plan\
8. Any blocking questions\
\
After that, implement Phase 1 and report:\
\
* What was implemented\
* Build result\
* Test result\
* Remaining limitations\
* Recommended next step\
\
Do not claim success without compiling and testing the project.\
}