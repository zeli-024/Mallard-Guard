# Particle effects — current behavior

Flying Sparks, Spark Debris and Spark Tracers share a parry origin, an uneven upward-favored spherical launch distribution and one bounded head budget. Shield Forward Bias optionally favors defender yaw while retaining side/rear directions. Fall parries use a horizontal ring with slight vertical variation instead. Each family retains its own motion, drag, gravity, collisions and rendering.

Base lifetimes default to 20 ticks for Flying Sparks, 30 ticks for Spark Debris, and 12 ticks for Spark Tracers. Flying Sparks normally live for 50–100% of that value; 12% sample a longer 110–135% lifetime. This lifetime behavior is unchanged. Flying Size Variance now defaults to 50% while retaining the selected 0.55–1.60 spawn-size range. Base intensity defaults to 5%, perfect adds 50% of that base, damage adds 5% per damage point. Base intensity 0 disables the family. Tracer tails keep their independent 1–4-tick length setting and tick-stepped squares.

Center Impact is a stationary, camera-facing six-frame animation: six 64×64 white artwork PNGs and six matching tint-mask PNGs. Defaults: Size 250%, Spin Speed 45 degrees/second, Opacity 100%, Duration 8 ticks. Starting rotation and spin direction are sampled once per spawn. Drawn expansion and outward dissipation live in the PNG sequence; runtime rotates it and fades its final frame interval without resizing it over time. Mask alpha controls color coverage with no separate tint-strength slider.

All effect geometry shares the existing particle-atlas batch. Active lists are bounded and clear on world/session changes. Death clears player-specific HUD and feedback without deleting other players' world effects. One shared monitor protects custom rendering, particle ticks/history, registry publication/pruning, palette application, removal and collision-profiler windows. It supports overlapping tick/render scheduling without copying lists or trails every frame. Only Mallard Guard particle work is serialized; other mods' particle settings are not changed. Runtime compatibility still needs an AsyncParticles test.

Mob tracers and their Center Impact use the server-selected mob Start/Middle/End palette captured with the burst. Player palettes remain local. The palette is captured before the first tick; separate bursts never share mutable color state. Flying Sparks and Spark Debris retain their usual local colors.

Default player tracer/impact palette: #FFEBD7 → #E9B692 → #FFFFFF. Default mob palette: #FFE7E3 → #F20900 → #B9B9B9. Flying Sparks and Spark Debris remain white. Chromatic Aberration defaults to 6 ticks.

## Multiplayer palettes and effect toggles

Player colors (Flying Sparks, Spark Debris and the three Spark Tracer colors) are shared on joining and applying a changed palette. The server stores one palette per connected player; bursts reference its small numeric ID. A late join receives the current palettes. Respawns and dimension changes retain the palette; logout and server shutdown remove it. Each received burst captures immutable colors before any hitlag delay, and each spawned particle retains that selection. Only colors are shared; the viewer keeps their local intensity, size, lifetime and enable/disable preferences unless the server enforces that category.

Server enforcement overrides shared player palettes; disabling enforcement restores their submitted colors. Mob tracer colors and matching impact colors always come from server settings. Server config reloads redistribute the policy and cached palettes.

Each spark family and the Center Impact has its own On/Off switch in its existing divider. Off stops spawning that family without changing its saved sliders. The shield forward-bias option now sits inside Spark Tracers and defaults On; it still affects all three spark families. Fall parries retain their horizontal ring spread.

Perfect parry hitlag defaults to 6 frames; regular parry hitlag has a separate 0–10-frame slider and defaults to 0 (Off). Each frame is 1/60 second. Old settings with Perfect Only Hitlag disabled migrate their former shared duration to the regular slider. Server retaliation timing continues to use the perfect-parry duration.
