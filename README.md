# Mallard Guard — Immersive Parrying

Parrying and guarding for Minecraft 1.21.1 on NeoForge.

## 1.0.0

The new 32×32 mod icon is bundled. The Items tab in `/mgc` lets operators configure comma-separated item IDs and `#item` tags for inclusion or exclusion. Exclusions always win. Inclusions can make an item eligible even when it lacks attack damage. Other items with attack damage remain eligible by default. "Allow usable items" controls whether ordinary eligible items with a hold-to-use animation can start a guard; it is on by default to preserve earlier behavior. Explicit inclusions still take priority when this switch is off. The use key remains Minecraft's rebindable Use Item key.

The Hits page of `/mgc` adds server-side toggles for fall parrying, explosion parrying, perfect-only explosion parrying, explosion blocking, projectile parrying, projectile blocking, and a push to the player who parries. Fall parries cancel fall damage and launch the player in the direction they are looking; perfect fall parries launch twice as far. Falls greater than ten blocks also create a blast. Fall blast strength, terrain damage, launch power, and defender push strength are adjustable. Only operators may change these server options.

Projectile parries cancel the impact and keep the projectile in flight: regular parries send it in a random direction, while perfect parries aim it at its original owner. Held projectile and explosion blocks use the existing block damage reduction. The defender receives impact knockback on ordinary parries; fall parries use their own launch instead. Blocks now shake the camera more than perfect parries, and regular parries retain a smaller camera shake.

Open `/mgc` and use the Audio page for server-wide master, perfect parry, regular parry, and block volume (0–200%). These multiply together; the normal 100% values use normalized copies of the supplied recordings. Only an operator can change server settings. Minecraft's Players and master audio settings still apply.

The Effects page controls the white screen flash for regular and perfect parries, rendered behind the 18-pixel shield and its larger, less translucent perfect-parry echo. The perfect-parry shield also grows and shakes more. Camera shake has its own toggle and strength slider; it is strongest on blocks, moderate on perfect parries, and light on regular parries. Held blocks tint and shake the shield red and briefly shade the outer edge of the screen with a light vignette; they do not show a white screen flash or produce particles.

Parries spawn one spray using Particle Interactions' anvil spark sprites, flying-spark velocity spread, crossed four-face shape, motion-dependent stretching, gravity, bounce response, and glow progression. Perfect parries spawn twenty-two flying sparks and twelve short-lived star-shaped flashes; regular parries spawn seven flying sparks without stars. Both parries emit one firework-style white orb using the vanilla flash sprite: the regular orb is 35% of the vanilla size and the perfect orb is 58%, with higher opacity on both. The emission occurs once per parry, and streak lifetime is shortened to 14–22 ticks. Held blocks spawn none. The mod has no Particle Interactions runtime dependency. See `THIRD_PARTY_ASSETS.md` for credit and license details.

Version 0.5.4 replaces all three perfect parry sound variants with the newly supplied recordings, converted to mono OGG with peak limiting. The other sounds and effects remain the same.

Build with Java 21: `./gradlew clean build`. The jar is `build/libs/mallardguard-1.0.0.jar`. Compare the size and opacity of both firework-style orbs; perfect should be larger and brighter, but smaller than the original vanilla orb. Check the larger shield remains centered below the crosshair and the perfect echo expands and fades. Verify the brief edge shading on blocks is subtle and that blocks still produce no particles or white screen flash.

For the 1.0.0 item rules, try including `minecraft:stick` and excluding `minecraft:iron_sword`. Exclusion should win if an item appears on both lists. Check an item tag, the usable-item toggle, and that a non-operator can view but not change server settings. Check the new icon in the Mods list.
