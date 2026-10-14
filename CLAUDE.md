# Umbral World — notes for Claude

Fabric mod for **Minecraft Java 26.3**. Mod id `umbral_world`, package `io.github.thedarkhorse99.umbralworld`.
Repo: `thedarkhorse99/Shadestone-Mod`, default branch `26.3`. Owner: Mac (GitHub TheDarkHorse99).

## Who you're working with

Mac has **no prior coding background** and is learning as the mod grows. He makes all the mod's art himself.
Act as a veteran modder: rigorous, honest feedback, no reflexive praise. Point out design problems.

## Standing rules (always follow)

1. **Ask before naming anything.** For any new block, item, mob, effect, etc., ask Mac what it should be called *before* writing code.
2. **Ask about art first.** Before implementing, ask which textures are needed and what to name them. Mac draws them; only make or edit textures when he asks.
3. **Explain everything, step by step.** Say what each piece does and *why*, in plain language. Keep the explanations even when you make the edits yourself.
4. **Always give full paths** for every package, file, and folder you create or reference (e.g. `src/main/java/io/github/thedarkhorse99/umbralworld/block/ModBlocks.java`).
5. **Verify against 26.3, never write from memory.** Minecraft changes names and formats often. Check classes, method signatures, and data formats against the real 26.3 jars before using them. Loom keeps them in the Gradle cache (search `%USERPROFILE%\.gradle\caches` for `minecraft-common-*-26_3.jar` / `minecraft-client-*-26_3.jar`); compare data files with vanilla's own JSON in those jars.
6. **Vanilla flavor.** Content and art should feel like Mojang could have made it. Item art is sized like diamonds/emeralds.
7. **Balance:** basic Umbral gear is diamond-tier, but it's the dimension's "stone tier": cheap and short-lived. The dimension's terrain is the Shadestone block itself, so shards are effectively unlimited once you're there; that's intended. Shadestone gear keeps low durability on purpose (Blade: 150). Better ores, tools and armor come later. The free Shadestone frame the game builds when you arrive somewhere new is also intended (like vanilla's obsidian portals). Mac decided all of this on 2026-10-14; Umbral mobs are scaled so diamond gear is the baseline (as dangerous to a diamond player as a zombie/skeleton is to an iron-ish one).
8. **Mac tests in-game himself.** End each change with a short numbered test list. Compile with `.\gradlew.bat build` (Windows) before handing over.
9. **Keep this file current.** Whenever Mac decides something new about the mod (a to-do item, a design decision, or a new rule), update CLAUDE.md in the same session and tell him exactly what changed (which section, what was added, edited, or removed).

## Workflow

- **Commit and push straight to `26.3`.** No feature branches or pull requests; Mac doesn't want to approve each change. Because nothing is reviewed before it lands, only push after `.\gradlew.bat build` passes, keep each commit to one feature or fix with a clear message, and never commit `.mcp.json`.
- **Mac often works from his phone** through Remote Control of a Claude Code session running in IntelliJ's terminal on his PC (the PC stays on). He can't easily reach the PC mid-session, so finish work end to end (code, build, commit, push) without needing him at the keyboard. In-game testing waits until he's back at the PC.
- **In-game tests (do this before pushing gameplay changes):** `.\gradlew.bat runClientGameTest` (about 1.5 minutes; opens a game window, so it works while Mac is away from the PC). It runs `src/gametest/java/.../test/UmbralWorldClientTests.java` in a fresh flat world: places blocks, spawns mobs, presses keys, waits, and checks results. Results: `build/run/clientGameTest/umbral_test_report.txt` (PASS/FAIL per check, with measured numbers); screenshots: `build/run/clientGameTest/screenshots/` (look at them with Read). Add checks for every new feature. Tests can't judge Mac's art, how things feel, or long-term spawn rates, so Mac's in-game test list still covers those.
- **GitHub access:** the GitHub CLI is installed at `C:\Program Files\GitHub CLI\gh.exe` (may not be on PATH), logged in as TheDarkHorse99, and set up as git's credential helper, so `git push` works without prompts.

## Project layout

- `src/main/java/.../umbralworld/` — common code
  - `UmbralWorld.java` — main entrypoint (`onInitialize`), `UmbralWorld.id("name")` helper
  - `block/` — `ModBlocks`, Shadestone blocks, portal (`UmbralPortalBlock`, `UmbralPortalShape`), Dark Furnace, Grave plants
  - `block/entity/` — `ModBlockEntities`, `DarkFurnaceBlockEntity`
  - `menu/` — Dark Furnace menu and result slot
  - `item/` — `ModItems`, `ModCreativeTabs` ("Umbral World" tab), `WightsShroud` (light check + invisibility)
  - `effect/` — Deep Blindness, Nightstalker (`NightstalkerEffect`), `ModPotions`
  - `mixin/` — common mixins (`umbral_world.mixins.json`): `LivingEntityMixin` (Nightstalker visibility)
  - `entity/` — `ModEntities`, `Wight`, `Ghoul`, `Crawler`, `CursedSoul`
  - `particle/` — `ModParticles` (the Corpse Candle's flame)
  - `world/` — dimension keys and helpers
- `src/client/java/.../umbralworld/client/` — client-only code: `UmbralWorldClient`, renderers and models (`GhoulModel`, `CrawlerModel`, `CursedSoulRenderer` billboard), `ModRenderPipelines` (black suns, fog-proof glowing eyes), `NightstalkerVision`, `mixin/` (fog, sky, Nightstalker screen filter and outlines, hiding the Shroud on invisible players)
- `src/gametest/` — the in-game tests (own mod id `umbral_world_test`, set up by `fabricApi.configureTests` in `build.gradle`; not part of the finished mod)
- `src/main/resources/assets/umbral_world/` — blockstates, `items/` + `models/` (every item needs both), textures, `lang/en_us.json`
- `src/main/resources/data/umbral_world/` — dimension, dimension_type, worldgen (biome, noise, features), loot tables, recipes, damage types, tags
- `src/main/resources/data/minecraft/tags/` — additions to vanilla tags (`"replace": false`)

## 26.3 facts already verified (don't relearn the hard way)

- Mojang mappings. `Identifier`, not `ResourceLocation`.
- Block render layer is chosen automatically from texture transparency; no `BlockRenderLayerMap` needed.
- Plants: extend `VegetationBlock`, override `mayPlaceOn`; `cross` block model.
- There is **no** `#minecraft:shears` item tag. Shears-only drops use `"condition": "minecraft:tool/can_shear"`.
- Biome `features` has 11 generation steps; vegetation is the 10th list (index 9).
- Mob spawning lives in the biome attribute `minecraft:gameplay/natural_mob_spawns` → `spawns_by_category`.
- "Undead" is the entity-type tag `minecraft:undead` (poison immunity, Smite, etc.).
- Spawn eggs: `new Item.Properties().spawnEgg(type)` with `SpawnEggItem::new`; each egg has its own texture (no tinting).
- `EntityRenderers.register` and `SpawnPlacements.register` are public — no Fabric helper needed.
- Custom damage: `data/umbral_world/damage_type/*.json`, death message key `death.attack.<message_id>`.
- Worldgen: there are no "configured features" any more. `Feature` is an interface; each feature is a record with a `MapCodec`, registered in `BuiltInRegistries.FEATURE_TYPE`; JSON in `worldgen/feature/` uses its fields directly. Noise JSON uses `base_octave` / `base_amplitude` / `octave_count` / `amplitude_modifiers`.
- `new TreeGrower(name, trees, megaTrees, flowerTrees, shortestTree)` takes `WeightedList<ResourceKey<Feature>>`s. Saplings need light 9+ unless `randomTick` is overridden.
- `PushReaction` has no `DESTROY`; it's `POPPED`. No sources jar: inspect classes with `javap` from the Adoptium JDK on the Loom jars.
- Don't edit Java files with PowerShell `Set-Content`: it adds a BOM that breaks compilation.
- Brewing is data-driven: every mix is its own JSON in `recipe/brewing/` (`"type": "minecraft:brewing"`, input/reagent/output), including each splash/lingering/redstone/gunpowder/dragon's breath step. Potions themselves are registered in Java (`BuiltInRegistries.POTION`); names come from `item.minecraft.potion.effect.<name>` etc.
- `CauldronInteraction.Dispatcher.put` is package-private: custom cauldrons override `useItemOn` instead.
- Screen filters: `GameRenderer.getRequestedPostEffects()` is rebuilt every frame in `update()`; add an id there to show `assets/<ns>/post_effect/<name>.json`. Post effects may only use the `minecraft:main` target.
- Entity outlines: `Minecraft.shouldEntityAppearGlowing(Entity)` (client). Mob detection range = follow range × `LivingEntity.getVisibilityPercent`.
- Custom shaders: `assets/umbral_world/shaders/core/*.fsh`, used by a `RenderPipeline` built in `ModRenderPipelines` from vanilla's snippets (`RenderPipelines.*_SNIPPET`, made public by Fabric). `RenderType.create(name, RenderSetup.builder(pipeline)...)` is also public. Pipelines are compiled the first time they draw, not at startup, so a runClient smoke test doesn't check them.
- Mob models: build a `LayerDefinition` in Java and register it with Fabric's `ModelLayerRegistry.registerModelLayer`. `Model.setupAnim` starts with `resetPose()`, so add animation on top of the default pose with `+=`.
- Mob speed: a mob's ground speed goes with the square of `MOVEMENT_SPEED` × goal speed (`Mob.setSpeed` also sets its forward input). A player's is about 0.098 walking and 0.127 sprinting on that scale (zombie 0.23 → 0.053).
- Players' movement on the server: `player.getKnownMovement()`; sneaking is `isShiftKeyDown()`.
- Wearable items: `Item.Properties().component(DataComponents.EQUIPPABLE, Equippable.builder(slot).setAsset(key)...)`, key = `ResourceKey.create(EquipmentAssets.ROOT_ID, id)`. The look is `assets/<ns>/equipment/<name>.json` (`layers.humanoid[].texture`), texture at `textures/entity/equipment/humanoid/<name>.png` (chestplate layout). `HumanoidArmorLayer` still draws armor on invisible players.
- Flat camera-facing entities (billboards): copy `DragonFireballRenderer` (`submit` → `poseStack.rotate(camera.orientation)` → `collector.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> ...)`).
- A Vex subclass gets wall-flying for free (`Vex.tick` sets `noPhysics`); override `populateDefaultEquipmentSlots` or it spawns with an iron sword.
- Client game tests (`fabric-client-gametest`): `context.worldBuilder().create()` makes a flat survival world with time, weather and mob spawning frozen. Use `computeOnServer` for server work and `getInput().holdKey(o -> o.keyUp)` for real movement. In commands use `@a`, not `@p` (`@p` only finds players in the dimension the command runs in). `/fill` fails in areas that haven't loaded; place blocks in code instead (`ServerLevel.setBlock` loads the chunk). Every monster gets a small random bonus (±) to follow range when it spawns.
- Custom particles: register a `FabricParticleTypes.simple()` in `BuiltInRegistries.PARTICLE_TYPE`, add `assets/<ns>/particles/<name>.json` (`textures` list, may point at vanilla sprites), and on the client `ParticleProviderRegistry.getInstance().register(type, Provider::new)` with a `SingleQuadParticle`. Vanilla's flame sprite has its colour baked in; `minecraft:glow` is white and can be tinted with `setColor`.
- Day cycles are data: `world_clock/<name>.json` (`{}`) plus `timeline/<name>.json` (`clock`, `period_ticks`, `time_markers`, `tracks` of keyframes for attributes like `minecraft:visual/sun_angle`, `moon_angle`, `sky_color`). The dimension type points at them with `default_clock` and `timelines`. Copy vanilla's `timeline/day.json` for the sun/moon paths. `has_fixed_time` now only decides whether the dimension counts as bright or dark outside. `SkyRenderer.renderMoon` is called with the sky already turned to the moon's angle.
- A short `.\gradlew.bat runClient` (killed after ~4 min) is a safe smoke test that mixins and registries load; it needs no EULA.
- Density functions were rewritten (package `levelgen.densityfunction`). Types: `noise`, `gradient` (`axis`, `from_coordinate`/`from_value`/`to_coordinate`/`to_value`, clamped), `clamp` (`input`/`min`/`max`), `add`/`sub`/`mul`/`div`/`min`/`max` (`left`/`right`, a plain number is allowed), `abs`/`square`/`negate`..., `range_choice`, `interpolated`, `cache`, `lerp`, `spline`. Reusable pieces go in `worldgen/density_function/` and are referenced by id. y-independent parts are optimised automatically; no `flat_cache` needed. Surface rules are "material rules" (`worldgen/material_rule/`, conditions `stone_depth`, `y_above`, `biome`...), same meaning as the old surface rules.
- **Height maps go by a block tag**: a solid block only counts as ground for mob spawning, rain etc. if it's in `#minecraft:blocks_motion_no_leaves` (our additions: `data/minecraft/tags/block/blocks_motion_no_leaves.json`). Add every new solid block there, or the game thinks the ground is lower than it is.
- Game tests can read terrain without building chunks: `ServerLevel.getUncachedNoiseBiome(x>>2, y>>2, z>>2)` and `getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, getChunkSource().randomState())`.

## Current state

Done and tested: Shadestone ore/shard/block, Shadestone Blade with Deep Blindness, Barrow Dirt, the Umbral World dimension and Grave biome, the darkness-activated portal (light level 4 or below), the Dark Furnace (darkness-powered, gives XP), the creative tab, Withered Tufts and Grave Thorns.

Written, awaiting Mac's in-game test: **the Wight** (Enderman body, skeleton-style texture, 30 HP / 8 attack / 4 armor / speed 0.27 / follow range 35, thorn-immune, undead, drops bones, spawns in the Grave). Needs Mac's spawn egg art at `textures/item/wight_spawn_egg.png`.

Written, awaiting Mac's in-game test: **Miasma stage 1**. The dimension now uses a `multi_noise` biome source: the Grave and Miasma are split 50/50 by the `vegetation` (humidity) slot, driven by `worldgen/noise/biome_layout.json`. Temperature is reserved for the Chasm. The Miasma has Effluvium on top, Barrow Dirt underneath, blue and red spore particles, and purple fog/water. No plants or mobs yet. Art done.

Written, awaiting Mac's in-game test: **Miasma stage 2, the Ghost Tree**. `GhostLogBlock` (a `PipeBlock`, 6 px thick), `GhostPlanksBlock` (a `TransparentBlock`), `GhostVaporBlock` (a `LeavesBlock` with an invisible render shape, empty shapes, `END_ROD` particles for now), `GhostSaplingBlock` (custom `TreeGrower`, skips the light check). `world/GhostTreeFeature.java` grows the tree; in 26.3 a `Feature` is a record registered as a `MapCodec` in `BuiltInRegistries.FEATURE_TYPE`. 1–3 trees per chunk. Art done.

Written, awaiting Mac's in-game test: **Miasma stage 3, the aura plants**. Frail Bramble and Pale Blossoms are `MiasmaPlantBlock`; Torpor Sprout is `TorporSproutBlock` (a `FlowerBedBlock`). Both implement `AuraPlant`. `MiasmaPlantAuras` checks blocks within 2 sideways / 1 up-down of each player every 10 ticks (Fabric `ServerTickEvents.END_LEVEL_TICK`) and gives level I of the effect for 4 s, topped up while they stay near. Art done.

Written, awaiting Mac's in-game test: **Miasma stage 4, Ectoplasm**. The Phantom Tree (`GhostTreeFeature` now reads its log/vapor blocks from JSON; `worldgen/feature/phantom_tree.json`, 1 in 10 chunks), `SpileBlock`, `EctoplasmCauldronBlock`, the Ectoplasm Bottle, the Nightstalker effect and potions, 10 brewing recipes in `data/umbral_world/recipe/brewing/`. A dev-client launch loaded with no errors. Phantom Tree art done. Still needs Mac's art: `textures/block/spile.png`, `ectoplasm.png` (the liquid in the cauldron), `textures/item/spile.png`, `ectoplasm_bottle.png`, and the effect icon `textures/mob_effect/nightstalker.png` (18x18, like `deep_blindness.png`).

Written, awaiting Mac's in-game test: **the Ghoul and Corpse Candles**. `entity/Ghoul.java` (a `PathfinderMob` in the CREATURE category, hearing check every 5 ticks, `RunAwayGoal`), `block/CorpseCandleBlock.java` (no model; flame particles from `block/entity/CorpseCandleBlockEntity`), client `GhoulModel` (zombie parts bent forward, zombie 64x64 texture layout), `GhoulRenderer` with an `EyesLayer` drawn through `ModRenderPipelines.GLOWING_EYES` and `shaders/core/glowing_eyes.fsh` (vanilla's entity shader without fog). Spawns in the Grave (creature list). Needs Mac's art: `textures/entity/ghoul/ghoul.png`, `textures/entity/ghoul/ghoul_eyes.png`, `textures/item/ghoul_spawn_egg.png`, `textures/item/corpse_candle.png`.

Written, awaiting Mac's in-game test: **the Wight's Shroud**. `item/WightsShroud.java` checks every 5 ticks; a wearer in light 5+ gets vanilla Invisibility in short hidden bursts (ambient, no particles, no icon), removed at once in the dark. Client `HumanoidArmorLayerMixin` hides the shroud while its wearer is invisible. Needs Mac's art: `textures/item/wights_shroud.png` and the worn look `textures/entity/equipment/humanoid/wights_shroud.png` (64x32 chestplate layout; transparent where it shouldn't cover).

Written, awaiting Mac's in-game test: **the Crawler**. `entity/Crawler.java` (a `Monster`; plant check, buff and guarding every 10 ticks in `customServerAiStep`; `CrawlAwayGoal`, `SeekPlantGoal`), client `CrawlerModel` (zombie head/body/arms lying face down, zombie 64x64 layout, legs unused) and `CrawlerRenderer`. Needs Mac's art: `textures/entity/crawler/crawler.png`, `textures/item/crawler_spawn_egg.png`. Drops still to decide.

Written, awaiting Mac's in-game test: **the Cursed Soul**. `entity/CursedSoul.java` (extends `Vex`, `DartAroundGoal`, `checkCursedSoulSpawnRules` looks for vapor), client `CursedSoulRenderer`. Spawns in the Miasma's monster list. Needs Mac's art: `textures/entity/cursed_soul/cursed_soul.png` (one square picture, e.g. 16x16 or 32x32; see-through edges allowed for a soft glow), `textures/item/cursed_soul_spawn_egg.png`. Drops still to decide.

Written, checked by the in-game tests (screenshots), awaiting Mac's in-game look: **the twin black suns**. The Umbral World has its own clock (`world_clock/umbral_world.json`) and timeline (`timeline/twin_suns.json`, 24000 ticks = 20 minutes). The purple black sun takes the vanilla sun's path (rises at tick 0, highest at 6000) and the red one the moon's path (rises at 12000, highest at 18000), so they take turns. The sky is dark purple-blue (`#1b1238`) under the purple sun and dark red (`#300a10`) under the red one, blending at sunrise. Only the look changes: light level and gameplay are the same (Mac's choice). `SkyRendererMixin` draws the suns in place of the vanilla sun and moon. `/time of umbral_world:umbral_world set umbral_world:red_noon` (also `purple_noon`, `purple_sunrise`, `red_sunrise`) jumps to a point. Mac renamed the textures: his existing sun is now `textures/environment/celestial/purple_black_sun.png`. `red_black_sun.png` is a recolour of it that Mac asked Claude to make (glow `#e63838`, rim `#ffd6d2`, same shape and transparency).

Written, checked by the in-game tests, awaiting Mac's in-game look: **Chasm stage 1, the terrain**. The Chasm biome (`worldgen/biome/chasm.json`: grey-blue fog `#0d1219`, no particles, cave mood sounds only, no mobs or features yet) is picked by the `temperature` slot (`density_function/chasm/layout.json`, noise `chasm_layout`): Chasm where it's above 0.2, measured at 32% of the world; Grave/Miasma split the rest by humidity as before. The same layout drives the terrain: `chasm/mask` goes 0 to 1 just inside the biome's edge and flattens the hills (land at about y=68). `chasm/fissure_cut` cuts fissures where `abs(chasm_fissures noise)` is under a width (0.015, plus up to 0.04 where `chasm_gorges` is high), straight down to `chasm/floor` (about y=-52). Measured: median fissure 5 blocks wide, gorges up to ~100, about 8% of the Chasm is fissure. `sea_level` is now -46 (was 63), so only the fissure bottoms hold water (4-8 deep); the Grave and Miasma's old low-lying ponds are gone. The surface is **Dead Sea Shale** (`dead_sea_shale`, tuff-like, pickaxe, drops itself), 5-8 layers deep above y=0, Shadestone below. Its model borrows vanilla's tuff texture until Mac draws `textures/block/dead_sea_shale.png` (then point `models/block/dead_sea_shale.json` at it). Fissure walls are perfectly smooth and vertical for now (a 3D wall-roughness noise made ugly spikes; `noise/chasm_walls.json` is kept for a later try).

## To-do

- Test the blade's Deep Blindness on a second player over LAN
- Draw the Dark Furnace GUI art (background, darkness icon, progress arrow)
- Random/mirrored orientations for Shadestone blocks
- Grave plants on vanilla dirt transform: Withered Tufts → rose bush, Grave Thorns → lilac
- A future custom drop for Withered Tufts

## Planned

Shadestone tools and armor · animated portal · snuffing vanilla candles/campfires · Grave ruins with a spire · the Miasma and Chasm biomes (see below) · a see-in-the-dark item and shadow powers · falling into the void as a way in · license, icon, version 0.1.0

## Biome designs (decided, partly built)

The dimension will have three biomes, roughly equal in size: the Grave, the Miasma and the Chasm. Terrain shape is shared by all biomes, so biome placement and terrain must be driven by the same noise for the Chasm's flat land and fissures to line up with the biome.

**Grave biome** (built: Barrow Dirt, ash, Withered Tufts, Grave Thorns, the Wight, the Ghoul, Corpse Candles, the Wight's Shroud).
- **Ghoul** (built): a hunched, zombie-like creature, very dark except for bright pale light blue glowing eyes (Mac changed them from fiery to match the Corpse Candles; the colour comes from his `ghoul_eyes.png`) that show through the fog even when its body doesn't. A passive "creature" that never attacks. It shuffles slowly and runs away when it hears a player: sprinting is heard within 16 blocks, walking within 8, sneaking or standing still not at all; being hit always scares it. When fleeing it is just slower than a sprinting player (about 96%), so chasing works but takes a while and sneaking up is the better way. 10 HP (1-2 diamond sword hits). Undead. Drops 0-2 Corpse Candles (+0-1 per Looting level)
- **Corpse Candle** (built): a floating orb of fire just above the ground, made only of particles (no model; Mac didn't want it to look like a mini fire). The particle is the mod's own `umbral_world:corpse_flame`: vanilla's white glow-squid sparkle tinted pale blue in code (`client/CorpseFlameParticle`), full-bright. Its picture is set in `particles/corpse_flame.json`, so Mac can swap in his own later. A tiny block entity makes the particles every tick, because a plain block's display ticks are too rare to keep the orb solid. Placed only on the ground (solid top face), no collision, breaks instantly, doesn't burn. Light level 4, pale light blue like the Ghost Tree (Mac changed it from orange-red). Only for light for now. Farmable: a candle on Effluvium slowly spreads (about every 5 minutes) to a nearby Effluvium block (1 sideways, up to 1 up/down) that has nothing on top, and each time the Effluvium under the old candle turns into Barrow Dirt. It can't spread from anything but Effluvium
- **Wight's Shroud** (built): a 1 in 50 drop from the Wight (killed by a player; +1% per Looting level). Worn in the chest slot (so no chestplate), no armor points, no durability. Shows on the player as a poncho-like garment, drawn by Mac to match how it looks on the Wight's own texture. Makes the wearer (and the shroud) invisible while standing in light level 5 or higher; in the dark both can be seen as normal

**Miasma biome** (being built in stages: soil/particles/colours → tree → 3 bushes → sap/potion → mobs): a pink/purple/blue "sinister fairy forest". It should feel slow, drowsy and sickly, but more colourful and pretty than the other biomes.
- Its own custom soil, **Effluvium**, coloured like the flora, with separate top/side/bottom textures like grass
- An ambient particle effect in the air, like the Grave's ash. For now it uses vanilla's blue (warped) and red (crimson) spores
- The **Ghost Tree**: a skinny, branching tree shaped like a chorus plant (thin connecting **Ghost Logs**, not block-sized logs), but breaking a piece doesn't collapse the rest. Gives its own wood: Ghost Logs → **Ghost Planks** (1 log → 2 planks, because the logs are thin; Mac confirmed 2, not 4; Phantom Logs too). Just logs, planks and "leaves" for now; more wood blocks later. Logs are slightly see-through, planks even more faintly. The "leaves" are **Ghost Vapor**: invisible, untouchable, unbreakable, faint light (level 3), only particles; it fades like leaves when the tree is cut and sometimes drops a **Ghost Sapling**. Saplings grow without light. The tree's sap, **Ectoplasm**, is a valuable potion ingredient (stage 4):
  - The rare **Phantom Tree** (Phantom Log, Phantom Planks, Phantom Vapor, Phantom Sapling): the same shape as the Ghost Tree but purple instead of pale white-blue. About 1 tree in 20. It is the only tree that gives sap
  - A **Spile** (crafted from 3 iron ingots: `## ` over `  #`) is placed on the side of a log. On a Phantom Log it drips like pointed dripstone (same rate as water dripstone) into a cauldron up to 11 blocks below, making an **Ectoplasm Cauldron** (3 levels). A glass bottle takes one level as an **Ectoplasm Bottle**
  - Ectoplasm Bottle + Awkward Potion → **Potion of the Nightstalker** (3:00; redstone → 8:00; no Strong version). Effect **Nightstalker**: "Frodo's ring" vision (washed-out purple screen, hostile mobs you can see are outlined, not through walls), +2 attack damage, +15% speed, but hostile mobs notice you from 1.5× as far away
  - Rejected: phasing through blocks (buggy and exploitable); invisible-in-the-dark overlaps the Wight's Shroud
- Three plants that give an effect to players standing near them, shown by coloured specks: **Frail Bramble** (thorny bush, Weakness, purple), **Pale Blossoms** (single white flower, Nausea, white; rarest because nausea is harshest), **Torpor Sprout** (ground cover like Pink Petals, 1–4 per block, Slowness, red)
- Two mobs (both built; drops not decided yet):
  - **Cursed Soul** (hostile, built): a black/dark wisp of light with a trail (give it a coloured glow so it can be seen in the dark). Flies around fast, through walls like a Vex, and attacks players. Spawns only within 4 blocks of Ghost or Phantom Vapor (and not where block light is above 3). It extends vanilla's `Vex` (no sword, no owner, no time limit, darts around faster when idle, but not mid-fight, so it keeps charging); 14 HP, 6 damage, 5 XP, undead. Drawn as one camera-facing picture at full brightness, with a dark purple dust trail
  - **Crawler** (built): the top half of a zombie, crawling on its arms. Drawn to the aura plants as if addicted to their effects: when not next to one (2 sideways / 1 up-down, like the aura), it looks for one within 10 blocks and crawls over; once next to one it stays put (it only wanders while it has no plant). Attacks a player who comes within 4 blocks of its plant; crawls away for 3 s when hit. Being next to the plants buffs it instead of giving it their effects: +50% speed, +3 damage, double follow range; the buff fades as soon as it leaves, so luring it away weakens it. 16 HP, 5 damage (8 buffed), 2 armor, speed 0.2 (about a walking player when buffed). Undead. Spawns in the Miasma like a monster (dark, solid ground), 1–2. No drops yet

**Chasm biome**: flat land cut by very deep fissures, too dark to see the bottom. Mostly thin fissures, sometimes large gorges.
- A third custom block, **Dead Sea Shale** (Mac's name; built), makes the surface and the first few layers down; below that it's Shadestone. The surface feels cold and empty: grey-blue fog, no particles (Mac's choice)
- Relatively shallow water at the bottom of the chasms (built: `sea_level` lowered to -46). Landing in water cancels fall damage, so jumping in is survivable
- The only plant is a climbable vine-like plant growing on the chasm walls
- A couple of hostile aquatic mobs living in the bottom water
- The heat ore, the rare resource, is found at the bottom
