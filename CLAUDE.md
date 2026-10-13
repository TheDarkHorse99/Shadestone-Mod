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
7. **Balance:** basic Umbral gear is diamond-tier; Umbral mobs are scaled so diamond gear is the baseline (as dangerous to a diamond player as a zombie/skeleton is to an iron-ish one).
8. **Mac tests in-game himself.** End each change with a short numbered test list. Compile with `.\gradlew.bat build` (Windows) before handing over.
9. **Keep this file current.** Whenever Mac decides something new about the mod (a to-do item, a design decision, or a new rule), update CLAUDE.md in the same session and tell him exactly what changed (which section, what was added, edited, or removed).

## Workflow

- **Commit and push straight to `26.3`.** No feature branches or pull requests; Mac doesn't want to approve each change. Because nothing is reviewed before it lands, only push after `.\gradlew.bat build` passes, keep each commit to one feature or fix with a clear message, and never commit `.mcp.json`.
- **Mac often works from his phone** through Remote Control of a Claude Code session running in IntelliJ's terminal on his PC (the PC stays on). He can't easily reach the PC mid-session, so finish work end to end (code, build, commit, push) without needing him at the keyboard. In-game testing waits until he's back at the PC.
- **GitHub access:** the GitHub CLI is installed at `C:\Program Files\GitHub CLI\gh.exe` (may not be on PATH), logged in as TheDarkHorse99, and set up as git's credential helper, so `git push` works without prompts.

## Project layout

- `src/main/java/.../umbralworld/` — common code
  - `UmbralWorld.java` — main entrypoint (`onInitialize`), `UmbralWorld.id("name")` helper
  - `block/` — `ModBlocks`, Shadestone blocks, portal (`UmbralPortalBlock`, `UmbralPortalShape`), Dark Furnace, Grave plants
  - `block/entity/` — `ModBlockEntities`, `DarkFurnaceBlockEntity`
  - `menu/` — Dark Furnace menu and result slot
  - `item/` — `ModItems`, `ModCreativeTabs` ("Umbral World" tab)
  - `effect/` — Deep Blindness, Nightstalker (`NightstalkerEffect`), `ModPotions`
  - `mixin/` — common mixins (`umbral_world.mixins.json`): `LivingEntityMixin` (Nightstalker visibility)
  - `entity/` — `ModEntities`, `Wight`, `Ghoul`
  - `world/` — dimension keys and helpers
- `src/client/java/.../umbralworld/client/` — client-only code: `UmbralWorldClient`, renderers and models (`GhoulModel`), `ModRenderPipelines` (black sun, fog-proof glowing eyes), `NightstalkerVision`, `mixin/` (fog, sky, Nightstalker screen filter and outlines)
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
- A short `.\gradlew.bat runClient` (killed after ~4 min) is a safe smoke test that mixins and registries load; it needs no EULA.

## Current state

Done and tested: Shadestone ore/shard/block, Shadestone Blade with Deep Blindness, Barrow Dirt, the Umbral World dimension and Grave biome, the darkness-activated portal (light level 4 or below), the Dark Furnace (darkness-powered, gives XP), the creative tab, Withered Tufts and Grave Thorns.

Written, awaiting Mac's in-game test: **the Wight** (Enderman body, skeleton-style texture, 30 HP / 8 attack / 4 armor / speed 0.27 / follow range 35, thorn-immune, undead, drops bones, spawns in the Grave). Needs Mac's spawn egg art at `textures/item/wight_spawn_egg.png`.

Written, awaiting Mac's in-game test: **Miasma stage 1**. The dimension now uses a `multi_noise` biome source: the Grave and Miasma are split 50/50 by the `vegetation` (humidity) slot, driven by `worldgen/noise/biome_layout.json`. Temperature is reserved for the Chasm. The Miasma has Effluvium on top, Barrow Dirt underneath, blue and red spore particles, and purple fog/water. No plants or mobs yet. Needs Mac's art: `textures/block/effluvium_top.png`, `effluvium_side.png`, `effluvium_bottom.png`.

Written, awaiting Mac's in-game test: **Miasma stage 2, the Ghost Tree**. `GhostLogBlock` (a `PipeBlock`, 6 px thick), `GhostPlanksBlock` (a `TransparentBlock`), `GhostVaporBlock` (a `LeavesBlock` with an invisible render shape, empty shapes, `END_ROD` particles for now), `GhostSaplingBlock` (custom `TreeGrower`, skips the light check). `world/GhostTreeFeature.java` grows the tree; in 26.3 a `Feature` is a record registered as a `MapCodec` in `BuiltInRegistries.FEATURE_TYPE`. 1–3 trees per chunk. Needs Mac's art: `textures/block/ghost_log.png`, `ghost_planks.png`, `ghost_sapling.png`.

Written, awaiting Mac's in-game test: **Miasma stage 3, the aura plants**. Frail Bramble and Pale Blossoms are `MiasmaPlantBlock`; Torpor Sprout is `TorporSproutBlock` (a `FlowerBedBlock`). Both implement `AuraPlant`. `MiasmaPlantAuras` checks blocks within 2 sideways / 1 up-down of each player every 10 ticks (Fabric `ServerTickEvents.END_LEVEL_TICK`) and gives level I of the effect for 4 s, topped up while they stay near. Needs Mac's art: `textures/block/frail_bramble.png`, `pale_blossoms.png`, `torpor_sprout.png`, `torpor_sprout_stem.png`, and `textures/item/torpor_sprout.png`.

Written, awaiting Mac's in-game test: **Miasma stage 4, Ectoplasm**. The Phantom Tree (`GhostTreeFeature` now reads its log/vapor blocks from JSON; `worldgen/feature/phantom_tree.json`, 1 in 10 chunks), `SpileBlock`, `EctoplasmCauldronBlock`, the Ectoplasm Bottle, the Nightstalker effect and potions, 10 brewing recipes in `data/umbral_world/recipe/brewing/`. A dev-client launch loaded with no errors. Needs Mac's art: `textures/block/phantom_log.png`, `phantom_planks.png`, `phantom_sapling.png`, `spile.png`, `ectoplasm.png` (the liquid in the cauldron), `textures/item/spile.png`, `ectoplasm_bottle.png`.

Written, awaiting Mac's in-game test: **the Ghoul and Corpse Candles**. `entity/Ghoul.java` (a `PathfinderMob` in the CREATURE category, hearing check every 5 ticks, `RunAwayGoal`), `block/CorpseCandleBlock.java` (no model; flame particles from `block/entity/CorpseCandleBlockEntity`), client `GhoulModel` (zombie parts bent forward, zombie 64x64 texture layout), `GhoulRenderer` with an `EyesLayer` drawn through `ModRenderPipelines.GLOWING_EYES` and `shaders/core/glowing_eyes.fsh` (vanilla's entity shader without fog). Spawns in the Grave (creature list). Needs Mac's art: `textures/entity/ghoul/ghoul.png`, `textures/entity/ghoul/ghoul_eyes.png`, `textures/item/ghoul_spawn_egg.png`, `textures/item/corpse_candle.png`.

## To-do

- Test the blade's Deep Blindness on a second player over LAN
- Draw the Dark Furnace GUI art (background, darkness icon, progress arrow)
- Random/mirrored orientations for Shadestone blocks
- Grave plants on vanilla dirt transform: Withered Tufts → rose bush, Grave Thorns → lilac
- A future custom drop for Withered Tufts
- Known issues: infinite shards from terrain, blade durability (150) is low, portal frame duplication exploit

## Planned

Shadestone tools and armor · animated portal · twin black suns with a purple-blue/dark-red sky cycle · the Wight's Shroud, Corpse Candles and the Ghoul (see Grave biome below) · snuffing vanilla candles/campfires · Grave ruins with a spire · the Miasma and Chasm biomes (see below) · a see-in-the-dark item and shadow powers · falling into the void as a way in · license, icon, version 0.1.0

## Biome designs (decided, partly built)

The dimension will have three biomes, roughly equal in size: the Grave, the Miasma and the Chasm. Terrain shape is shared by all biomes, so biome placement and terrain must be driven by the same noise for the Chasm's flat land and fissures to line up with the biome.

**Grave biome** (built: Barrow Dirt, ash, Withered Tufts, Grave Thorns, the Wight, the Ghoul, Corpse Candles). Still to build: the Wight's Shroud.
- **Ghoul** (built): a hunched, zombie-like creature, very dark except for bright fiery glowing eyes that show through the fog even when its body doesn't. A passive "creature" that never attacks. It shuffles slowly and runs away when it hears a player: sprinting is heard within 16 blocks, walking within 8, sneaking or standing still not at all; being hit always scares it. When fleeing it is just slower than a sprinting player (about 96%), so chasing works but takes a while and sneaking up is the better way. 10 HP (1-2 diamond sword hits). Undead. Drops 0-2 Corpse Candles (+0-1 per Looting level)
- **Corpse Candle** (built): a floating orb of fire just above the ground, made only of vanilla flame particles (no model; Mac didn't want it to look like a mini fire). A tiny block entity makes the particles every tick, because a plain block's display ticks are too rare to keep the orb solid. Placed only on the ground (solid top face), no collision, breaks instantly, doesn't burn. Light level 4, bright orange-red like normal fire. Only for light for now. Farmable: a candle on Effluvium slowly spreads (about every 5 minutes) to a nearby Effluvium block (1 sideways, up to 1 up/down) that has nothing on top, and each time the Effluvium under the old candle turns into Barrow Dirt. It can't spread from anything but Effluvium
- **Wight's Shroud**: a 1 in 50 drop from the Wight. Worn in the chest slot (so no chestplate). Makes the wearer invisible while standing in light level 5 or higher; like a player, it can still be seen in the dark

**Miasma biome** (being built in stages: soil/particles/colours → tree → 3 bushes → sap/potion → mobs): a pink/purple/blue "sinister fairy forest". It should feel slow, drowsy and sickly, but more colourful and pretty than the other biomes.
- Its own custom soil, **Effluvium**, coloured like the flora, with separate top/side/bottom textures like grass
- An ambient particle effect in the air, like the Grave's ash. For now it uses vanilla's blue (warped) and red (crimson) spores
- The **Ghost Tree**: a skinny, branching tree shaped like a chorus plant (thin connecting **Ghost Logs**, not block-sized logs), but breaking a piece doesn't collapse the rest. Gives its own wood: Ghost Logs → **Ghost Planks** (1 log → 2 planks, because the logs are thin; Mac confirmed 2, not 4; Phantom Logs too). Just logs, planks and "leaves" for now; more wood blocks later. Logs are slightly see-through, planks even more faintly. The "leaves" are **Ghost Vapor**: invisible, untouchable, unbreakable, faint light (level 3), only particles; it fades like leaves when the tree is cut and sometimes drops a **Ghost Sapling**. Saplings grow without light. The tree's sap, **Ectoplasm**, is a valuable potion ingredient (stage 4):
  - The rare **Phantom Tree** (Phantom Log, Phantom Planks, Phantom Vapor, Phantom Sapling): the same shape as the Ghost Tree but purple instead of pale white-blue. About 1 tree in 20. It is the only tree that gives sap
  - A **Spile** (crafted from 3 iron ingots: `## ` over `  #`) is placed on the side of a log. On a Phantom Log it drips like pointed dripstone (same rate as water dripstone) into a cauldron up to 11 blocks below, making an **Ectoplasm Cauldron** (3 levels). A glass bottle takes one level as an **Ectoplasm Bottle**
  - Ectoplasm Bottle + Awkward Potion → **Potion of the Nightstalker** (3:00; redstone → 8:00; no Strong version). Effect **Nightstalker**: "Frodo's ring" vision (washed-out purple screen, hostile mobs you can see are outlined, not through walls), +2 attack damage, +15% speed, but hostile mobs notice you from 1.5× as far away
  - Rejected: phasing through blocks (buggy and exploitable); invisible-in-the-dark overlaps the Wight's Shroud
- Three plants that give an effect to players standing near them, shown by coloured specks: **Frail Bramble** (thorny bush, Weakness, purple), **Pale Blossoms** (single white flower, Nausea, white; rarest because nausea is harshest), **Torpor Sprout** (ground cover like Pink Petals, 1–4 per block, Slowness, red)
- Two mobs (drops not decided yet):
  - **Cursed Soul** (hostile): a black/dark wisp of light with a trail (give it a coloured glow so it can be seen in the dark). Flies around fast, through walls like a Vex, and attacks players. Spawns only within a few blocks of Ghost Vapor
  - **Crawler**: the top half of a zombie, crawling on its arms. Drawn to the aura plants as if addicted to their effects. Attacks you if you step too close to the plants it's near; crawls away when hit. Being next to the plants buffs it (more speed, strength and vision) instead of giving it their effects

**Chasm biome**: flat land cut by very deep fissures, too dark to see the bottom. Mostly thin fissures, sometimes large gorges.
- A third custom block makes the surface and the first few layers down; below that it's Shadestone
- Relatively shallow water at the bottom of the chasms. This needs the dimension's `sea_level` (currently 63) lowered to near the world floor, or every chasm floods up to y=63. Landing in water cancels fall damage, so jumping in is survivable
- The only plant is a climbable vine-like plant growing on the chasm walls
- A couple of hostile aquatic mobs living in the bottom water
- The heat ore, the rare resource, is found at the bottom
