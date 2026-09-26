# Mallard Guard — Immersive Parrying

Stage 1 source for Minecraft 1.21.1 / NeoForge 21.1.248. Requires Java 21 to build.

On Linux: `chmod +x gradlew && ./gradlew build`. The mod JAR is in `build/libs/`.
Alternatively, upload this folder to a GitHub repository and download the artifact from Actions > Build.

Press the normal Use Item binding while holding an item with a positive attack damage modifier. Parry windows progress from perfect to regular, then to a held guard if Use Item stays down. The crosshair shield visualizes the window and recovery. Doors and other normal right-click interactions still run. `/mgc` or `/mallardguardconfig` opens the settings screen; operators can save server gameplay settings, while anyone can save local HUD settings.

This stage provides input, state, GUI, and HUD for testing. It does not yet intercept attacks, reduce damage, play block animations, or create hit effects. These are later stages.
