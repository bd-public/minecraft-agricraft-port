# AgriCraft

Agricultural farming extended.

Available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/agricraft) and [Modrinth](https://modrinth.com/mod/agricraft).

We also have a [Discord server](https://discord.gg/vyrdwWc) where you can ask for support/suggestions or just discuss with us.

## Available versions

| Version | Support           |
|---------|-------------------|
| 1.21    | Supported         |
| 1.20.4  | Supported         |
| 1.18    | Major issues only |
| 1.16    | Unsupported       |
| 1.12    | Unsupported       |
| 1.11    | Unsupported       |
| 1.10    | Unsupported       |
| 1.9     | Unsupported       |
| 1.8.9   | Unsupported       |
| 1.7.10  | Unsupported       |

# Minecraft 26.1.2 port

This development build targets Minecraft Java Edition 26.1.2, NeoForge
26.1.2.129 or newer for that Minecraft release, and Java 25. It is based on
the upstream AgriCraft `1.21` branch (`b142a775`).

Build the mod with `./gradlew build` (`.\gradlew.bat build` on Windows). Put
the JAR from `build/libs` in the NeoForge instance's `mods` folder. For a
dedicated server, install the same mod versions on the server and clients.

Crop sticks allow players to walk through them by default. Set
`crop_sticks_collide = true` under `[core]` in
`config/agricraft-common.toml` to enable collision. On a dedicated server,
set the same value on the server and clients.

Mystical Agriculture 9.0.11 with Cucumber Library 9.0.8 is optional. With
those mods loaded, AgriCraft reads enabled crops from the Mystical Agriculture
registry. Use the original seed on crop sticks, sneak-right-click a Seed
Analyzer to convert and analyze the seed, and harvest the crop for its native
essence. Growth respects the crop's biome, crux block, and configured essence
farmland requirements. The Harvester uses the AgriCraft harvest stages and
keeps the plant in its crop sticks.

Development runs and in-game tests:

```powershell
.\gradlew.bat runClient -PwithMysticalAgriculture
.\gradlew.bat runGameTestServer -PgameTests
.\gradlew.bat runGameTestServer -PgameTests -PwithMysticalAgriculture
```

The build includes the upstream 1.21.1 generated crop data, textures and
recipes converted to the 26.1.2 pack formats. The legacy 1.21.1 datagen and
its Java integrations for EMI, Jade, JEI, WTHIT, Serene Seasons and Farmer's
Delight are outside this port build. Existing AgriCraft worlds from 1.20.1
have not been tested for upgrade compatibility.

