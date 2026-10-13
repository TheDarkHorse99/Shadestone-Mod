package io.github.thedarkhorse99.umbralworld.test;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.block.ModBlocks;
import io.github.thedarkhorse99.umbralworld.block.entity.CorpseCandleBlockEntity;
import io.github.thedarkhorse99.umbralworld.entity.Crawler;
import io.github.thedarkhorse99.umbralworld.entity.CursedSoul;
import io.github.thedarkhorse99.umbralworld.entity.Ghoul;
import io.github.thedarkhorse99.umbralworld.entity.ModEntities;
import io.github.thedarkhorse99.umbralworld.item.ModItems;
import io.github.thedarkhorse99.umbralworld.world.ModDimensions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Umbral World's in-game tests. Run with .\gradlew.bat runClientGameTest
 *
 * Opens the game, makes a fresh flat world, and checks each feature by placing blocks,
 * spawning mobs, moving the player with real key presses and waiting. Each check is
 * written to build/run/clientGameTest/umbral_test_report.txt as PASS or FAIL, and
 * screenshots go to build/run/clientGameTest/screenshots/ so looks can be checked by eye.
 * One failing check doesn't stop the others; the run fails at the end if any did.
 *
 * The flat world: grass on top at y = -61, so the player stands at y = -60.
 * Each feature gets its own patch of ground, far enough apart not to disturb each other.
 */
public class UmbralWorldClientTests implements FabricClientGameTest {
	private static final int GROUND = -61;
	private static final int FEET = -60;

	private final List<String> report = new ArrayList<>();
	private int failures;
	private ClientGameTestContext context;
	private TestSingleplayerContext world;

	@Override
	public void runTest(ClientGameTestContext context) {
		this.context = context;
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			this.world = world;
			world.getConnection().waitForChunksRender();
			// A survival player (mobs only react to survival players) that can't die or starve
			command("difficulty normal");
			command("gamemode survival @a");
			command("effect give @a minecraft:resistance infinite 3 true");
			command("effect give @a minecraft:regeneration infinite 2 true");
			command("effect give @a minecraft:saturation infinite 0 true");

			section("Corpse Candle", this::testCorpseCandle);
			section("Wight's Shroud", this::testShroud);
			section("Loot tables", this::testLoot);
			section("Ghoul", this::testGhoul);
			section("Crawler", this::testCrawler);
			section("Cursed Soul", this::testCursedSoul);
		}
		writeReport();
		if (failures > 0) {
			throw new AssertionError(failures + " Umbral World check(s) failed; see umbral_test_report.txt");
		}
	}

	// =====================================================================================
	// Corpse Candle
	// =====================================================================================

	private void testCorpseCandle() {
		BlockPos candle = new BlockPos(0, FEET, 5);
		boolean placed = onServer(server -> {
			ServerLevel level = server.overworld();
			level.setBlockAndUpdate(candle, ModBlocks.CORPSE_CANDLE.defaultBlockState());
			return level.getBlockState(candle).is(ModBlocks.CORPSE_CANDLE)
					&& level.getBlockEntity(candle) instanceof CorpseCandleBlockEntity;
		});
		check("candle places on grass and has its particle block entity", placed);

		boolean floatsOnAir = onServer(server -> ModBlocks.CORPSE_CANDLE.defaultBlockState()
				.canSurvive(server.overworld(), new BlockPos(0, FEET + 5, 5)));
		check("candle can't be placed in mid-air", !floatsOnAir);

		boolean pops = onServer(server -> {
			ServerLevel level = server.overworld();
			BlockPos test = new BlockPos(3, FEET, 5);
			level.setBlockAndUpdate(test, ModBlocks.CORPSE_CANDLE.defaultBlockState());
			level.setBlockAndUpdate(test.below(), Blocks.AIR.defaultBlockState());
			return level.getBlockState(test).isAir();
		});
		check("candle pops off when the ground under it is removed", pops);

		// Screenshots at night: close up, and from 10 blocks
		command("time set midnight");
		teleport(0.5, FEET, 2.5, 0, 15);
		context.waitTicks(60);
		context.takeScreenshot("corpse_candle_3_blocks");
		teleport(0.5, FEET, -5, 0, 5);
		context.waitTicks(60);
		context.takeScreenshot("corpse_candle_10_blocks");

		// Spreading: a candle in the middle of a 5x5 patch of Effluvium
		String spread = onServer(server -> {
			ServerLevel level = server.overworld();
			BlockPos center = new BlockPos(20, FEET, 0);
			fill(level, center.offset(-2, -1, -2), center.offset(2, -1, 2), ModBlocks.EFFLUVIUM.defaultBlockState());
			level.setBlockAndUpdate(center, ModBlocks.CORPSE_CANDLE.defaultBlockState());
			RandomSource random = RandomSource.create(1);
			for (int i = 0; i < 2000; i++) {
				level.getBlockState(center).randomTick(level, center, random);
			}
			int candles = count(level, center.offset(-2, 0, -2), center.offset(2, 0, 2), ModBlocks.CORPSE_CANDLE.defaultBlockState());
			boolean deadGround = level.getBlockState(center.below()).is(ModBlocks.BARROW_DIRT);
			return candles + " candles, ground under the first one is " + (deadGround ? "Barrow Dirt" : "still Effluvium");
		});
		check("candle on Effluvium spreads once and turns its ground to Barrow Dirt (" + spread + ")",
				spread.startsWith("2 candles") && spread.endsWith("Barrow Dirt"));

		// Blocked: every spot around it has a block on top
		String blocked = onServer(server -> {
			ServerLevel level = server.overworld();
			BlockPos center = new BlockPos(40, FEET, 0);
			fill(level, center.offset(-1, -1, -1), center.offset(1, -1, 1), ModBlocks.EFFLUVIUM.defaultBlockState());
			fill(level, center.offset(-1, 0, -1), center.offset(1, 0, 1), Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(center, ModBlocks.CORPSE_CANDLE.defaultBlockState());
			RandomSource random = RandomSource.create(2);
			for (int i = 0; i < 2000; i++) {
				level.getBlockState(center).randomTick(level, center, random);
			}
			return level.getBlockState(center.below()).is(ModBlocks.EFFLUVIUM) ? "didn't spread" : "spread anyway";
		});
		check("candle doesn't spread when all the Effluvium around it is covered (" + blocked + ")", blocked.equals("didn't spread"));

		// Not from other ground: a candle on grass never spreads
		String onGrass = onServer(server -> {
			ServerLevel level = server.overworld();
			BlockPos center = new BlockPos(60, FEET, 0);
			level.setBlockAndUpdate(center.offset(1, -1, 0), ModBlocks.EFFLUVIUM.defaultBlockState());
			level.setBlockAndUpdate(center, ModBlocks.CORPSE_CANDLE.defaultBlockState());
			RandomSource random = RandomSource.create(3);
			for (int i = 0; i < 2000; i++) {
				level.getBlockState(center).randomTick(level, center, random);
			}
			return level.getBlockState(center.offset(1, 0, 0)).isAir() ? "didn't spread" : "spread";
		});
		check("candle on grass doesn't spread (" + onGrass + ")", onGrass.equals("didn't spread"));
	}

	// =====================================================================================
	// Wight's Shroud
	// =====================================================================================

	private void testShroud() {
		teleport(0.5, FEET, -30.5, 0, 0);
		command("time set noon");
		onServer(server -> {
			player(server).setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.WIGHTS_SHROUD));
			return null;
		});
		context.waitTicks(20);
		check("wearing the Shroud in daylight makes you invisible (server)", onServer(server -> player(server).hasEffect(MobEffects.INVISIBILITY)));
		check("... and the game screen agrees (client)", context.computeOnClient(client -> client.player != null && client.player.isInvisible()));
		check("... with no effect icon", onServer(server -> {
			MobEffectInstance effect = player(server).getEffect(MobEffects.INVISIBILITY);
			return effect != null && !effect.showIcon();
		}));

		// Look at yourself from the front: you (and the shroud) should be gone
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(10);
		context.takeScreenshot("shroud_daylight_should_be_invisible");

		command("time set midnight");
		context.waitTicks(20);
		check("in the dark (light 4) you're visible again", onServer(server -> !player(server).hasEffect(MobEffects.INVISIBILITY)));
		context.takeScreenshot("shroud_night_should_be_visible");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

		// A torch next to you brings it back, even at night
		onServer(server -> {
			server.overworld().setBlockAndUpdate(new BlockPos(1, FEET, -31), Blocks.TORCH.defaultBlockState());
			return null;
		});
		context.waitTicks(20);
		check("at night next to a torch you're invisible", onServer(server -> player(server).hasEffect(MobEffects.INVISIBILITY)));
		onServer(server -> {
			server.overworld().setBlockAndUpdate(new BlockPos(1, FEET, -31), Blocks.AIR.defaultBlockState());
			return null;
		});
		context.waitTicks(20);

		// A real potion in the dark isn't cancelled by the shroud
		onServer(server -> player(server).addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600)));
		context.waitTicks(20);
		check("a real Invisibility potion isn't removed by the Shroud in the dark", onServer(server -> {
			MobEffectInstance effect = player(server).getEffect(MobEffects.INVISIBILITY);
			return effect != null && effect.getDuration() > 500;
		}));
		onServer(server -> player(server).removeEffect(MobEffects.INVISIBILITY));

		// Taking it off in the light ends it
		command("time set noon");
		context.waitTicks(20);
		onServer(server -> {
			player(server).setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			return null;
		});
		context.waitTicks(20);
		check("taking the Shroud off in daylight ends the invisibility", onServer(server -> !player(server).hasEffect(MobEffects.INVISIBILITY)));
	}

	// =====================================================================================
	// Loot: the Shroud's drop rate and the Ghoul's candles
	// =====================================================================================

	private void testLoot() {
		int rolls = 20000;
		int shrouds = onServer(server -> rollLoot(server, ModEntities.WIGHT, "entities/wight", rolls, ModItems.WIGHTS_SHROUD.getDefaultInstance()));
		double percent = 100.0 * shrouds / rolls;
		check(String.format("Wight drops the Shroud about 1 in 50 (%d in %d kills = %.2f%%, expected 2%%)", shrouds, rolls, percent),
				percent > 1.5 && percent < 2.5);

		int candleRolls = 2000;
		int candles = onServer(server -> rollLoot(server, ModEntities.GHOUL, "entities/ghoul", candleRolls, ModBlocks.CORPSE_CANDLE.asItem().getDefaultInstance()));
		double average = (double) candles / candleRolls;
		check(String.format("Ghoul drops 0-2 Corpse Candles (average %.2f per kill, expected about 1)", average), average > 0.85 && average < 1.15);
	}

	/** Kills the mob {@code rolls} times (only in the loot table) and counts how many of one item drop. */
	private static int rollLoot(MinecraftServer server, net.minecraft.world.entity.EntityType<? extends Mob> type, String table, int rolls, ItemStack item) {
		ServerLevel level = server.overworld();
		ServerPlayer player = player(server);
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		LootTable loot = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, UmbralWorld.id(table)));
		LootParams params = new LootParams.Builder(level)
				.withParameter(LootContextParams.THIS_ENTITY, mob)
				.withParameter(LootContextParams.ORIGIN, player.position())
				.withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player))
				.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
				.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
				.withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
				.create(LootContextParamSets.ENTITY);
		int found = 0;
		for (int i = 0; i < rolls; i++) {
			for (ItemStack stack : loot.getRandomItems(params)) {
				if (ItemStack.isSameItem(stack, item)) {
					found += stack.getCount();
				}
			}
		}
		return found;
	}

	// =====================================================================================
	// Ghoul
	// =====================================================================================

	private void testGhoul() {
		command("time set noon");
		// Ghoul 10 blocks south of the player, who faces it
		BlockPos ghoulAt = new BlockPos(100, FEET, 10);
		UUID ghoul = spawn(ModEntities.GHOUL, ghoulAt);
		teleport(100.5, FEET, 0.5, 0, 0);
		context.waitTicks(60);
		check("standing still 10 blocks away doesn't scare it", !ghoulScared(ghoul));

		// Sneak towards it for 3 seconds (about 4 blocks)
		facePosition(ghoul);
		context.getInput().holdKey(options -> options.keyShift);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(60);
		context.getInput().releaseKey(options -> options.keyUp);
		double sneakDistance = distanceToPlayer(ghoul);
		context.getInput().releaseKey(options -> options.keyShift);
		context.waitTicks(5);
		check(String.format("sneaking up to %.1f blocks away doesn't scare it", sneakDistance), !ghoulScared(ghoul));

		// Walk towards it until it notices (or 3 seconds)
		facePosition(ghoul);
		context.getInput().holdKey(options -> options.keyUp);
		int walked = 0;
		while (walked < 60 && !ghoulScared(ghoul)) {
			context.waitTick();
			walked++;
		}
		double heardAt = distanceToPlayer(ghoul);
		context.getInput().releaseKey(options -> options.keyUp);
		check(String.format("walking up scares it (heard you at %.1f blocks, expected about 8 or less)", heardAt),
				ghoulScared(ghoul) && heardAt < 9.0);

		// How fast does it run away?
		Vec3 start = entityPosition(ghoul);
		context.waitTicks(40);
		double fleeSpeed = entityPosition(ghoul).subtract(start).horizontalDistance() / 2.0;
		check(String.format("it runs away (%.1f blocks/second over 2 seconds)", fleeSpeed), fleeSpeed > 2.0);

		// Stand still: it calms down 5 seconds after it last heard you
		context.waitTicks(120);
		check("it calms down after 5 seconds of quiet", !ghoulScared(ghoul));

		// Sprint at it from 14 blocks: heard straight away. Also measures the player's sprint speed.
		BlockPos ghoulNow = BlockPos.containing(entityPosition(ghoul));
		teleport(ghoulNow.getX() + 0.5, FEET, ghoulNow.getZ() - 13.5, 0, 0);
		facePosition(ghoul);
		context.waitTicks(10);
		context.getInput().holdKey(options -> options.keySprint);
		context.getInput().holdKey(options -> options.keyUp);
		context.waitTicks(10);
		Vec3 playerStart = playerPosition();
		Vec3 ghoulStart = entityPosition(ghoul);
		context.waitTicks(40);
		double sprintSpeed = playerPosition().subtract(playerStart).horizontalDistance() / 2.0;
		double ghoulSprintFlee = entityPosition(ghoul).subtract(ghoulStart).horizontalDistance() / 2.0;
		context.getInput().releaseKey(options -> options.keyUp);
		context.getInput().releaseKey(options -> options.keySprint);
		check("sprinting from 14 blocks away scares it", ghoulScared(ghoul));
		check(String.format("fleeing speed vs a sprinting player: Ghoul %.2f, player %.2f blocks/second (%.0f%%, aim was about 96%%)",
				ghoulSprintFlee, sprintSpeed, 100 * ghoulSprintFlee / Math.max(sprintSpeed, 0.01)), ghoulSprintFlee < sprintSpeed);

		// Hitting it always scares it, even a calm one
		BlockPos calmAt = new BlockPos(130, FEET, 40);
		UUID calm = spawn(ModEntities.GHOUL, calmAt);
		onServer(server -> {
			Entity entity = server.overworld().getEntity(calm);
			ServerPlayer player = player(server);
			if (entity instanceof Ghoul g) {
				g.hurtServer(server.overworld(), server.overworld().damageSources().playerAttack(player), 1.0f);
			}
			return null;
		});
		check("hitting it scares it", ghoulScared(calm));

		// How it looks: a frozen Ghoul 4 blocks in front of you, by day and by night
		command("summon umbral_world:ghoul 150.5 -60 4.5 {NoAI:1b,Rotation:[180f,0f]}");
		teleport(150.5, FEET, 0.5, 0, 10);
		command("time set noon");
		context.waitTicks(40);
		context.takeScreenshot("ghoul_overworld_day_4_blocks");
		command("time set midnight");
		context.waitTicks(40);
		context.takeScreenshot("ghoul_overworld_night_4_blocks");

		// The eyes through the fog: on a platform high in the Umbral World, Ghouls 12 to 30 blocks away (frozen in place).
		// Built in code, which loads the area first (a /fill command fails on areas that haven't loaded yet).
		onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			fill(umbral, new BlockPos(-12, 199, -3), new BlockPos(12, 199, 40), Blocks.STONE.defaultBlockState());
			// Side by side so none hides another: 12, 18, 24 and 30 blocks away (the fog ends at 24)
			double[][] spots = {{-4.5, 12.5}, {-1.5, 18.5}, {1.5, 24.5}, {4.5, 30.5}};
			for (double[] spot : spots) {
				Ghoul g = ModEntities.GHOUL.create(umbral, EntitySpawnReason.COMMAND);
				g.snapTo(spot[0], 200, spot[1], 180.0f, 0.0f);
				g.setNoAi(true);
				umbral.addFreshEntity(g);
			}
			return null;
		});
		command("execute in umbral_world:umbral_world run tp @a 0.5 200 0.5 0 0");
		world.getConnection().waitForChunksRender();
		context.waitTicks(60);
		String where = onServer(server -> {
			ServerPlayer player = player(server);
			int ghouls = player.level().getEntitiesOfClass(Ghoul.class, player.getBoundingBox().inflate(40)).size();
			return player.level().dimension().identifier() + " at " + player.blockPosition().toShortString() + ", " + ghouls + " Ghouls within 40 blocks";
		});
		int clientGhouls = context.computeOnClient(client -> client.level == null || client.player == null ? -1
				: client.level.getEntitiesOfClass(Ghoul.class, client.player.getBoundingBox().inflate(40)).size());
		report.add("INFO  fog test: player in " + where + "; the game screen knows of " + clientGhouls + " Ghouls");
		context.takeScreenshot("ghoul_eyes_umbral_fog_12_18_24_30_blocks");
		// The same with a light next to the near one, to show where the bodies are
		command("execute in umbral_world:umbral_world run setblock 2 200 5 minecraft:glowstone");
		context.waitTicks(20);
		context.takeScreenshot("ghoul_umbral_with_glowstone");
		command("execute in minecraft:overworld run tp @a 0.5 -60 0.5");
		world.getConnection().waitForChunksRender();
	}

	private boolean ghoulScared(UUID id) {
		return onServer(server -> server.overworld().getEntity(id) instanceof Ghoul ghoul && ghoul.isScared());
	}

	// =====================================================================================
	// Crawler
	// =====================================================================================

	private void testCrawler() {
		command("time set noon");
		BlockPos plant = new BlockPos(200, FEET, 10);
		onServer(server -> {
			ServerLevel level = server.overworld();
			fill(level, plant.offset(-1, -1, -1), plant.offset(1, -1, 1), ModBlocks.EFFLUVIUM.defaultBlockState());
			level.setBlockAndUpdate(plant, ModBlocks.FRAIL_BRAMBLE.defaultBlockState());
			return null;
		});
		teleport(200.5, FEET, -20.5, 0, 0); // far from the plant
		UUID crawler = spawn(ModEntities.CRAWLER, new BlockPos(200, FEET, 3));

		// It should crawl over to the plant and get its buff
		int waited = 0;
		while (waited < 400 && !crawlerBuffed(crawler)) {
			context.waitTicks(5);
			waited += 5;
		}
		check(String.format("it crawls to a plant 7 blocks away and gets buffed (took %.1f seconds)", waited / 20.0), crawlerBuffed(crawler));
		Identifier buff = UmbralWorld.id("crawler_plant_buff");
		String stats = onServer(server -> server.overworld().getEntity(crawler) instanceof Crawler c
				? String.format(java.util.Locale.ROOT, "speed %.2f, damage %.1f, follow range %.1f; buff on speed/damage/range: %b/%b/%b",
						c.getAttributeValue(Attributes.MOVEMENT_SPEED), c.getAttributeValue(Attributes.ATTACK_DAMAGE),
						c.getAttributeValue(Attributes.FOLLOW_RANGE),
						c.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(buff), c.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(buff),
						c.getAttribute(Attributes.FOLLOW_RANGE).hasModifier(buff))
				: "missing");
		check("the buff: speed 0.20 -> 0.30, damage 5 -> 8, follow range doubled (" + stats + ")",
				stats.startsWith("speed 0.30, damage 8.0") && stats.endsWith("true/true/true"));

		// Screenshot (third person, from the side)
		teleport(204.5, FEET, 6.5, 45, 25);
		context.waitTicks(40);
		context.takeScreenshot("crawler_by_its_plant");

		// Step close to the plant: it attacks
		teleport(200.5, FEET, 7.5, 0, 0);
		int noticed = 0;
		while (noticed < 60 && !crawlerTargetsPlayer(crawler)) {
			context.waitTick();
			noticed++;
		}
		check("stepping within 4 blocks of its plant makes it attack (" + describeCrawler(crawler) + ")", crawlerTargetsPlayer(crawler));

		// Hit it: it gives up the attack and crawls away
		double before = distanceToPlayer(crawler);
		onServer(server -> {
			if (server.overworld().getEntity(crawler) instanceof Crawler c) {
				c.hurtServer(server.overworld(), server.overworld().damageSources().playerAttack(player(server)), 1.0f);
			}
			return null;
		});
		check("after being hit it stops attacking and flees", onServer(server ->
				server.overworld().getEntity(crawler) instanceof Crawler c && c.isFleeing() && c.getTarget() == null));
		context.waitTicks(40);
		double after = distanceToPlayer(crawler);
		check(String.format("it crawls away (%.1f -> %.1f blocks from you)", before, after), after > before + 1.0);

		// Out of plant range: the buff wears off (and it can't sense a plant 15 blocks away)
		teleport(200.5, FEET, -20.5, 0, 0);
		onServer(server -> {
			Entity entity = server.overworld().getEntity(crawler);
			if (entity instanceof Crawler c) {
				c.setTarget(null);
				c.snapTo(215.5, FEET, 10.5);
			}
			return null;
		});
		context.waitTicks(25);
		check("15 blocks from the plant its buff is gone", !crawlerBuffed(crawler));
	}

	private String describeCrawler(UUID id) {
		return onServer(server -> {
			if (!(server.overworld().getEntity(id) instanceof Crawler c)) {
				return "crawler missing";
			}
			ServerPlayer player = player(server);
			return String.format(java.util.Locale.ROOT, "crawler at %s, %.1f blocks from you, target %s, fleeing %b, sees you %b",
					c.blockPosition().toShortString(), c.distanceTo(player), c.getTarget() == null ? "none" : c.getTarget().getName().getString(),
					c.isFleeing(), c.hasLineOfSight(player));
		});
	}

	private boolean crawlerBuffed(UUID id) {
		return onServer(server -> server.overworld().getEntity(id) instanceof Crawler c
				&& c.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.25);
	}

	private boolean crawlerTargetsPlayer(UUID id) {
		return onServer(server -> server.overworld().getEntity(id) instanceof Crawler c && c.getTarget() == player(server));
	}

	// =====================================================================================
	// Cursed Soul
	// =====================================================================================

	private void testCursedSoul() {
		command("time set midnight");
		teleport(300.5, FEET, -10.5, 0, 0);
		// Spawn rules, next to a lone Ghost Vapor block 5 blocks up
		BlockPos vapor = new BlockPos(300, FEET + 5, 0);
		onServer(server -> {
			server.overworld().setBlockAndUpdate(vapor, ModBlocks.GHOST_VAPOR.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
			return null;
		});
		context.waitTicks(5);
		check("may spawn 3 blocks from Ghost Vapor", onServer(server -> CursedSoul.checkCursedSoulSpawnRules(
				ModEntities.CURSED_SOUL, server.overworld(), EntitySpawnReason.NATURAL, vapor.offset(3, 0, 0), RandomSource.create())));
		check("may not spawn 8 blocks from it", !onServer(server -> CursedSoul.checkCursedSoulSpawnRules(
				ModEntities.CURSED_SOUL, server.overworld(), EntitySpawnReason.NATURAL, vapor.offset(8, 0, 0), RandomSource.create())));
		onServer(server -> {
			server.overworld().setBlockAndUpdate(vapor.offset(2, 0, 0), Blocks.GLOWSTONE.defaultBlockState());
			return null;
		});
		context.waitTicks(5);
		check("may not spawn by bright light (glowstone)", !onServer(server -> CursedSoul.checkCursedSoulSpawnRules(
				ModEntities.CURSED_SOUL, server.overworld(), EntitySpawnReason.NATURAL, vapor.offset(3, 0, 0), RandomSource.create())));

		// Spawned like a natural mob: no sword
		command("summon umbral_world:cursed_soul 305.5 -57 -5.5");
		context.waitTicks(5);
		check("it spawns with nothing in its hands (not a Vex's sword)", onServer(server -> {
			List<CursedSoul> souls = server.overworld().getEntitiesOfClass(CursedSoul.class,
					new net.minecraft.world.phys.AABB(300, -64, -10, 310, -50, 0));
			return !souls.isEmpty() && souls.stream().allMatch(soul -> soul.getMainHandItem().isEmpty());
		}));
		command("kill @e[type=umbral_world:cursed_soul]");

		// Through walls: a soul already chasing you, with a solid stone wall (5 high, 9 wide) between you
		onServer(server -> {
			fill(server.overworld(), new BlockPos(296, FEET, 16), new BlockPos(304, FEET + 4, 16), Blocks.STONE.defaultBlockState());
			return null;
		});
		teleport(300.5, FEET, 12.5, 0, 0);
		UUID soul = onServer(server -> {
			CursedSoul s = ModEntities.CURSED_SOUL.create(server.overworld(), EntitySpawnReason.COMMAND);
			s.snapTo(300.5, FEET + 1, 20.5);
			server.overworld().addFreshEntity(s);
			s.setTarget(player(server));
			return s.getUUID();
		});
		int waited = 0;
		boolean reached = false;
		List<String> trace = new ArrayList<>();
		while (waited < 200 && !reached) {
			context.waitTicks(5);
			waited += 5;
			reached = onServer(server -> player(server).getLastHurtByMob() instanceof CursedSoul);
			if (waited % 20 == 0) {
				trace.add(onServer(server -> server.overworld().getEntity(soul) instanceof CursedSoul s
						? String.format(java.util.Locale.ROOT, "z=%.1f target=%s charging=%b", s.getZ(),
								s.getTarget() == null ? "none" : "you", s.isCharging())
						: "gone"));
			}
		}
		check(String.format("chasing you, it flies through a stone wall and hits you (took %.1f seconds; wall at z=16, you at z=12.5; each second: %s)",
				waited / 20.0, trace), reached);

		// Screenshot: one frozen soul in the dark, 4 blocks in front of the player
		command("kill @e[type=umbral_world:cursed_soul]");
		teleport(330.5, FEET, 0.5, 0, 0);
		command("summon umbral_world:cursed_soul 330.5 -59 4.5 {NoAI:1b}");
		context.waitTicks(40);
		context.takeScreenshot("cursed_soul_at_night_4_blocks");
		command("kill @e[type=umbral_world:cursed_soul]");
	}

	// =====================================================================================
	// Helpers
	// =====================================================================================

	private void section(String name, Runnable test) {
		report.add("");
		report.add("== " + name + " ==");
		try {
			test.run();
		} catch (Throwable t) {
			failures++;
			report.add("FAIL  " + name + " stopped with an error: " + t);
			UmbralWorld.LOGGER.error("Umbral test section {} crashed", name, t);
		}
	}

	private void check(String what, boolean passed) {
		if (!passed) {
			failures++;
		}
		String line = (passed ? "PASS  " : "FAIL  ") + what;
		report.add(line);
		UmbralWorld.LOGGER.info("[umbral test] {}", line);
	}

	private void writeReport() {
		List<String> lines = new ArrayList<>();
		lines.add("Umbral World in-game test report: " + (failures == 0 ? "ALL PASSED" : failures + " FAILED"));
		lines.addAll(report);
		try {
			Path file = Path.of("umbral_test_report.txt").toAbsolutePath();
			Files.write(file, lines);
			UmbralWorld.LOGGER.info("[umbral test] report written to {}", file);
		} catch (IOException e) {
			UmbralWorld.LOGGER.error("Couldn't write the test report", e);
		}
	}

	private <T> T onServer(Function<MinecraftServer, T> action) {
		return world.getServer().computeOnServer(action::apply);
	}

	private void command(String command) {
		world.getServer().runCommand(command);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private void teleport(double x, double y, double z, float yaw, float pitch) {
		command(String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		world.getConnection().waitForChunksRender();
	}

	/** Turns the player to face an entity (on the client, like moving the mouse). */
	private void facePosition(UUID id) {
		Vec3 target = entityPosition(id);
		context.getInput().lookAt(BlockPos.containing(target));
	}

	private UUID spawn(net.minecraft.world.entity.EntityType<? extends Mob> type, BlockPos pos) {
		return onServer(server -> type.spawn(server.overworld(), pos, EntitySpawnReason.COMMAND).getUUID());
	}

	private Vec3 entityPosition(UUID id) {
		return onServer(server -> {
			Entity entity = server.overworld().getEntity(id);
			return entity == null ? Vec3.ZERO : entity.position();
		});
	}

	private Vec3 playerPosition() {
		return onServer(server -> player(server).position());
	}

	private double distanceToPlayer(UUID id) {
		return entityPosition(id).distanceTo(playerPosition());
	}

	private static void fill(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			level.setBlockAndUpdate(pos, state);
		}
	}

	private static int count(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
			if (level.getBlockState(pos) == state) {
				count++;
			}
		}
		return count;
	}
}
