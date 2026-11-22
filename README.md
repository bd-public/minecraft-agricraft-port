# AgriCraft for Minecraft 26.1.2

This repository hosts an unofficial port of [AgriCraft](https://github.com/AgriCraft/AgriCraft)
for Minecraft Java Edition 26.1.2 only. The original project and its source
are maintained in the upstream AgriCraft repository. This port is based on
the [upstream `1.21` branch at `b142a775`](https://github.com/AgriCraft/AgriCraft/commit/b142a775418cc7e95cc8e4fc93a49b1941842bd1).
See upstream for the official releases and support for other Minecraft versions.

## Installation

Use Minecraft Java Edition 26.1.2, Java 25, and NeoForge 26.1.2.129 or later
for Minecraft 26.1.2. Download the AgriCraft JAR from
[GitHub Releases](https://github.com/bd-public/minecraft-agricraft-port/releases)
and place it in the NeoForge instance's `mods` folder. For a dedicated server,
install the same mod versions on the server and clients.

## Crop sticks and Mystical Agriculture

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

## Building and testing

Build with `./gradlew build` (`.\gradlew.bat build` on Windows). The JAR is
written to `build/libs`.

Development commands on Windows:

```powershell
.\gradlew.bat runClient -PwithMysticalAgriculture
.\gradlew.bat runGameTestServer -PgameTests
.\gradlew.bat runGameTestServer -PgameTests -PwithMysticalAgriculture
```

## Port status

The build includes the upstream 1.21.1 generated crop data, textures and
recipes converted to the 26.1.2 pack formats. The legacy 1.21.1 datagen and
its Java integrations for EMI, Jade, JEI, WTHIT, Serene Seasons and Farmer's
Delight are outside this port build. Existing AgriCraft worlds from 1.20.1
have not been tested for upgrade compatibility.

