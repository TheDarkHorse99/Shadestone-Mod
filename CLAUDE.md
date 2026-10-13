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

## Project layout

- `src/main/java/.../umbralworld/` — common code
  - `UmbralWorld.java` — main entrypoint (`onInitialize`), `UmbralWorld.id("name")` helper
  - `block/` — `ModBlocks`, Shadestone blocks, portal (`UmbralPortalBlock`, `UmbralPortalShape`), Dark Furnace, Grave plants
  - `block/entity/` — `ModBlockEntities`, `DarkFurnaceBlockEntity`
  - `menu/` — Dark Furnace menu and result slot
  - `item/` — `ModItems`, `ModCreativeTabs` ("Umbral World" tab)
  - `effect/` — Deep Blindness
  - `entity/` — `ModEntities`, `Wight`
  - `world/` — dimension keys and helpers
- `src/client/java/.../umbralworld/client/` — client-only code: `UmbralWorldClient`, renderers, `mixin/` (fog, sky)
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

## Current state

Done and tested: Shadestone ore/shard/block, Shadestone Blade with Deep Blindness, Barrow Dirt, the Umbral World dimension and Grave biome, the darkness-activated portal (light level 4 or below), the Dark Furnace (darkness-powered, gives XP), the creative tab, Withered Tufts and Grave Thorns.

Written, awaiting Mac's in-game test: **the Wight** (Enderman body, skeleton-style texture, 30 HP / 8 attack / 4 armor / speed 0.27 / follow range 35, thorn-immune, undead, drops bones, spawns in the Grave). Needs Mac's spawn egg art at `textures/item/wight_spawn_egg.png`.

## To-do

- Test the blade's Deep Blindness on a second player over LAN
- Draw the Dark Furnace GUI art (background, darkness icon, progress arrow)
- Random/mirrored orientations for Shadestone blocks
- Grave plants on vanilla dirt transform: Withered Tufts → rose bush, Grave Thorns → lilac
- A future custom drop for Withered Tufts
- Known issues: infinite shards from terrain, blade durability (150) is low, portal frame duplication exploit

## Planned

Shadestone tools and armor · animated portal · twin black suns with a purple-blue/dark-red sky cycle · the Wight's Shroud and the invisible-in-light system · Corpse Candles and the ghoul mob · snuffing vanilla candles/campfires · Grave ruins with a spire · Miasma and Canyons biomes · heat ore · a see-in-the-dark item and shadow powers · falling into the void as a way in · license, icon, version 0.1.0
