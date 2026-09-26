# Mallard Guard — Immersive Parrying

Parrying and guarding for Minecraft 1.21.1 on NeoForge.

## 0.5.2 effects

Open `/mgc` and use the Audio page for server-wide master, perfect parry, regular parry, and block volume (0–200%). These multiply together; the normal 100% values use normalized copies of the supplied recordings. Only an operator can change server settings. Minecraft's Players and master audio settings still apply.

The Effects page controls the white screen flash and expanding gold rings, rendered behind the shield and its perfect-parry echo. The perfect shield pulse and echo are slightly larger and more visible.

Hit particles use the bundled anvil spark art from Particle Interactions. Mallard Guard registers and renders the effect itself, including sparks and an impact flash. No other mod is required on the server or clients. See `THIRD_PARTY_ASSETS.md` for credit and license details.

Build with Java 21: `./gradlew clean build`. The jar is `build/libs/mallardguard-0.5.2.jar`. Test a perfect parry, regular parry, and held block, including on a second nearby client without Particle Interactions installed. Check flash layering, spark visibility, slider extremes, and muted sound at 0%.
