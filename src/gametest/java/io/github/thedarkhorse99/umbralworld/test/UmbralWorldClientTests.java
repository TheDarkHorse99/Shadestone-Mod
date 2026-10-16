package io.github.thedarkhorse99.umbralworld.test;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.block.ModBlocks;
import io.github.thedarkhorse99.umbralworld.block.entity.CorpseCandleBlockEntity;
import io.github.thedarkhorse99.umbralworld.entity.ChasmSkate;
import io.github.thedarkhorse99.umbralworld.entity.ChasmSwimmer;
import io.github.thedarkhorse99.umbralworld.entity.Crawler;
import io.github.thedarkhorse99.umbralworld.entity.CursedSoul;
import io.github.thedarkhorse99.umbralworld.entity.Ghoul;
import io.github.thedarkhorse99.umbralworld.entity.ModEntities;
import io.github.thedarkhorse99.umbralworld.entity.Murkfiend;
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
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
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
			section("Twin suns", this::testTwinSuns);
			section("Chasm terrain", this::testChasm);
			section("Miasma terrain", this::testMiasmaHills);
			section("Small things", this::testSmallThings);
			section("Chasm life", this::testChasmLife);
			section("Chasm mobs", this::testChasmMobs);
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
		// Keep sneaking until the player has stopped sliding: letting go of sneak while still
		// moving counts as walking, which the Ghoul is meant to hear this close
		context.waitTicks(10);
		double sneakDistance = distanceToPlayer(ghoul);
		check(String.format("sneaking up to %.1f blocks away doesn't scare it", sneakDistance), !ghoulScared(ghoul));
		context.getInput().releaseKey(options -> options.keyShift);

		// Walk towards it until it notices (or 3 seconds)
		facePosition(ghoul);
		double walkStart = distanceToPlayer(ghoul);
		context.getInput().holdKey(options -> options.keyUp);
		int walked = 0;
		while (walked < 60 && !ghoulScared(ghoul)) {
			context.waitTick();
			walked++;
		}
		double heardAt = distanceToPlayer(ghoul);
		context.getInput().releaseKey(options -> options.keyUp);
		check(String.format("walking up scares it (heard you at %.1f blocks after %d ticks of walking from %.1f blocks; expected about 8 or less)", heardAt, walked, walkStart),
				ghoulScared(ghoul) && heardAt < 9.0);

		// How fast does it run away?
		Vec3 start = entityPosition(ghoul);
		List<String> fleeTrace = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			context.waitTicks(10);
			fleeTrace.add(onServer(server -> server.overworld().getEntity(ghoul) instanceof Ghoul g
					? String.format(java.util.Locale.ROOT, "%.1f,%.1f %s", g.getX(), g.getZ(), g.getNavigation().isDone() ? "stopped" : "moving")
					: "gone"));
		}
		double fleeSpeed = entityPosition(ghoul).subtract(start).horizontalDistance() / 2.0;
		check(String.format("it runs away (%.1f blocks/second over 2 seconds; positions every half second: %s)", fleeSpeed, fleeTrace), fleeSpeed > 2.0);

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
		// Its side and back, in daylight
		command("time set noon");
		command("tp @e[type=umbral_world:ghoul,x=150.5,y=-60,z=4.5,distance=..1] 150.5 -60 4.5 90 0");
		context.waitTicks(20);
		context.takeScreenshot("ghoul_overworld_day_side");
		command("tp @e[type=umbral_world:ghoul,x=150.5,y=-60,z=4.5,distance=..1] 150.5 -60 4.5 0 0");
		context.waitTicks(20);
		context.takeScreenshot("ghoul_overworld_day_back");

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
				// A frozen mob's body and head don't turn on their own: face them at the camera
				g.setYBodyRot(180.0f);
				g.setYHeadRot(180.0f);
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
		// A frozen Crawler 3 blocks away in daylight: front, side and back
		command("time set noon");
		command("summon umbral_world:crawler 230.5 -60 3.5 {NoAI:1b,Rotation:[180f,0f]}");
		teleport(230.5, FEET, 0.5, 0, 25);
		context.waitTicks(30);
		context.takeScreenshot("crawler_day_front");
		command("tp @e[type=umbral_world:crawler,x=230.5,y=-60,z=3.5,distance=..1] 230.5 -60 3.5 90 0");
		context.waitTicks(20);
		context.takeScreenshot("crawler_day_side");
		command("tp @e[type=umbral_world:crawler,x=230.5,y=-60,z=3.5,distance=..1] 230.5 -60 3.5 0 0");
		context.waitTicks(20);
		context.takeScreenshot("crawler_day_back");
		command("kill @e[type=umbral_world:crawler,x=230.5,y=-60,z=3.5,distance=..1]");

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
		while (waited < 300 && !reached) {
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
	// Twin black suns
	// =====================================================================================

	private void testTwinSuns() {
		// High above the Umbral World's fog, so the sky is clear
		onServer(server -> {
			fill(server.getLevel(ModDimensions.UMBRAL_WORLD), new BlockPos(-2, 299, -2), new BlockPos(2, 299, 2), Blocks.STONE.defaultBlockState());
			return null;
		});
		command("execute in umbral_world:umbral_world run tp @a 0.5 300 0.5 0 -90");
		world.getConnection().waitForChunksRender();
		String[][] times = {
				{"umbral_world:purple_noon", "sky_purple_sun_overhead", "-90"},
				{"umbral_world:red_noon", "sky_red_sun_overhead", "-90"},
				{"umbral_world:purple_sunrise", "sky_purple_sunrise_looking_east", "-10"},
				{"umbral_world:red_sunrise", "sky_red_sunrise_looking_east", "-10"},
		};
		for (String[] time : times) {
			command("time of umbral_world:umbral_world set " + time[0]);
			command("execute in umbral_world:umbral_world run tp @a 0.5 300 0.5 -90 " + time[2]);
			context.waitTicks(30);
			context.takeScreenshot(time[1]);
		}
		long umbralTime = onServer(server -> server.getLevel(ModDimensions.UMBRAL_WORLD).getDefaultClockTime());
		report.add("INFO  Umbral World time after setting red sunrise: " + umbralTime);
		command("execute in minecraft:overworld run tp @a 0.5 -60 0.5");
		world.getConnection().waitForChunksRender();
	}

	// =====================================================================================
	// Chasm terrain
	// =====================================================================================

	/** A fissure found while scanning: the run of low columns from startX to endX (inclusive) along row z. */
	private record Fissure(int startX, int endX, int z, int floor) {
		int width() {
			return endX - startX + 1;
		}
	}

	private void testChasm() {
		// 1. How much of the world each biome takes: 128 x 128 samples over a 4096-block square, straight from the noise
		java.util.Map<String, Integer> shares = onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
			for (int x = -2048; x < 2048; x += 32) {
				for (int z = -2048; z < 2048; z += 32) {
					String biome = umbral.getUncachedNoiseBiome(x >> 2, 16, z >> 2).unwrapKey().orElseThrow().identifier().getPath();
					counts.merge(biome, 1, Integer::sum);
				}
			}
			counts.replaceAll((biome, n) -> (int) Math.round(n * 100.0 / (128 * 128)));
			return counts;
		});
		report.add("INFO  biome shares (%): " + shares);
		int chasmShare = shares.getOrDefault("chasm", 0);
		check("the Chasm covers roughly a third of the world (25-42%; measured " + chasmShare + "%)", chasmShare >= 25 && chasmShare <= 42);

		// 2. Scan the terrain deep inside a Chasm: heights from the generator itself, without building chunks
		List<Fissure> fissures = new ArrayList<>();
		int[] plateau = {Integer.MAX_VALUE, Integer.MIN_VALUE};
		int[] scanned = {0, 0}; // Chasm columns, fissure columns
		BlockPos centre = onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			ChunkGenerator generator = umbral.getChunkSource().getGenerator();
			RandomState random = umbral.getChunkSource().randomState();
			BlockPos found = null;
			search:
			for (int r = 0; r < 4096; r += 64) {
				for (int x = -r; x <= r; x += 64) {
					for (int z : new int[]{-r, r}) {
						if (isChasm(umbral, x, z) && isChasm(umbral, x + 256, z) && isChasm(umbral, x - 256, z)
								&& isChasm(umbral, x, z + 256) && isChasm(umbral, x, z - 256)) {
							found = new BlockPos(x, 0, z);
							break search;
						}
					}
				}
			}
			if (found == null) {
				return null;
			}
			for (int row = 0; row < 32; row++) {
				int z = found.getZ() - 256 + row * 16;
				int runStart = Integer.MIN_VALUE;
				int runFloor = Integer.MAX_VALUE;
				for (int x = found.getX() - 256; x <= found.getX() + 256; x++) {
					boolean chasm = isChasm(umbral, x, z);
					int height = chasm ? generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, umbral, random) : Integer.MAX_VALUE;
					boolean low = chasm && height < 20;
					if (chasm) {
						scanned[0]++;
						if (low) {
							scanned[1]++;
						} else {
							plateau[0] = Math.min(plateau[0], height);
							plateau[1] = Math.max(plateau[1], height);
						}
					}
					if (low) {
						if (runStart == Integer.MIN_VALUE) {
							runStart = x;
						}
						runFloor = Math.min(runFloor, height);
					} else if (runStart != Integer.MIN_VALUE) {
						fissures.add(new Fissure(runStart, x - 1, z, runFloor));
						runStart = Integer.MIN_VALUE;
						runFloor = Integer.MAX_VALUE;
					}
				}
			}
			// A map seen from above, 4 blocks per letter: ' ' another biome, '.' fissure bottom,
			// 'o' halfway down (y 20-50), '-' lower than the land (y 50-62), '#' the flat land
			report.add("INFO  map of 512 x 512 blocks around " + found.toShortString() + " (north at the top):");
			for (int z = found.getZ() - 256; z < found.getZ() + 256; z += 8) {
				StringBuilder line = new StringBuilder("      ");
				for (int x = found.getX() - 256; x < found.getX() + 256; x += 4) {
					if (!isChasm(umbral, x, z)) {
						line.append(' ');
						continue;
					}
					int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, umbral, random);
					line.append(height < 20 ? '.' : height < 50 ? 'o' : height < 62 ? '-' : '#');
				}
				report.add(line.toString());
			}
			return found;
		});
		check("found a spot deep inside a Chasm", centre != null);
		if (centre == null) {
			return;
		}
		List<Integer> widths = fissures.stream().map(Fissure::width).sorted().toList();
		int floorLow = fissures.stream().mapToInt(Fissure::floor).min().orElse(0);
		int floorHigh = fissures.stream().mapToInt(Fissure::floor).max().orElse(0);
		report.add("INFO  scanned 32 rows of 513 blocks around " + centre.toShortString() + ": " + scanned[0] + " Chasm columns, "
				+ scanned[1] + " in fissures (" + Math.round(scanned[1] * 100.0 / Math.max(1, scanned[0])) + "%)");
		report.add("INFO  fissure crossings: " + widths.size() + ", widths (blocks, sorted): " + widths);
		report.add("INFO  plateau top between y=" + plateau[0] + " and y=" + plateau[1] + "; fissure floors between y=" + floorLow + " and y=" + floorHigh);
		check("the scan crossed fissures (" + widths.size() + ")", widths.size() >= 4);
		if (widths.isEmpty()) {
			return;
		}
		int median = widths.get(widths.size() / 2);
		check("the land between fissures is flat (top varies by " + (plateau[1] - plateau[0]) + " blocks; under 16)", plateau[1] - plateau[0] < 16);
		check("fissure floors sit just under the water (y=" + floorLow + " to " + floorHigh + "; between -60 and -47)", floorLow >= -60 && floorHigh <= -47);
		check("most fissures are thin (median width " + median + "; 5-12 blocks)", median >= 5 && median <= 12);
		check("some gorges are wide (widest " + widths.getLast() + "; 20+ blocks)", widths.getLast() >= 20);

		// Every block of every fissure, in a 321 x 321 square: how narrow is the narrowest spot, and does it all join up?
		int[] web = onServer(server -> chasmWeb(server.getLevel(ModDimensions.UMBRAL_WORLD), centre, 160));
		report.add("INFO  fissure web in 321 x 321 blocks: " + web[0] + " fissure columns, narrowest spot " + web[1]
				+ " blocks across, " + web[2] + " pieces cut off from the rest");
		check("no fissure is narrower than 5 blocks (narrowest " + web[1] + ")", web[1] >= 5);
		check("all fissures join up into one web (" + web[2] + " pieces cut off)", web[2] == 0);

		// 3. Look at a typical fissure and the widest gorge from their edges, then check the blocks of the first one
		Fissure thin = fissures.stream().filter(f -> f.width() >= 4 && f.width() <= 10).findFirst().orElse(fissures.getFirst());
		Fissure gorge = fissures.stream().max(java.util.Comparator.comparingInt(Fissure::width)).orElseThrow();
		lookIntoFissure(gorge, "chasm_widest_gorge_from_edge");
		lookIntoFissure(thin, "chasm_thin_fissure_from_edge");
		String[] column = onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			int x = (thin.startX() + thin.endX()) / 2;
			BlockPos edge = new BlockPos(thin.startX() - 2, 0, thin.z());
			int top = umbral.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, edge.getX(), edge.getZ()) - 1;
			BlockState surface = umbral.getBlockState(edge.atY(top));
			BlockState deep = umbral.getBlockState(edge.atY(top - 12));
			BlockState water = umbral.getBlockState(new BlockPos(x, -47, thin.z()));
			BlockState middle = umbral.getBlockState(new BlockPos(x, 10, thin.z()));
			return new String[]{
					String.valueOf(surface.is(ModBlocks.DEAD_SEA_SHALE)), String.valueOf(deep.is(ModBlocks.SHADESTONE_BLOCK)),
					String.valueOf(water.is(Blocks.WATER)), String.valueOf(middle.isAir()),
					"y=" + top + " " + surface + " / " + deep + " / " + water + " / " + middle};
		});
		report.add("INFO  edge surface / 12 below it / fissure at y=-47 / fissure at y=10: " + column[4]);
		// The height map (used for mob spawning, rain and more) must see the real ground: a solid block
		// only counts if it's in the #minecraft:blocks_motion_no_leaves tag
		int[] heights = onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			int columns = 0;
			int wrong = 0;
			int shaleTops = 0;
			int landColumns = 0;
			for (int dx = -24; dx <= -3; dx++) {
				for (int dz = -12; dz <= 12; dz++) {
					BlockPos pos = new BlockPos(thin.startX() + dx, 0, thin.z() + dz);
					int y = 120;
					while (y > -64 && (umbral.getBlockState(pos.atY(y)).isAir() || (umbral.getBlockState(pos.atY(y)).is(ModBlocks.FINGER_WEEDS) && umbral.getFluidState(pos.atY(y)).isEmpty()))) { // dry weeds can be walked through, so they don't count as ground
						y--;
					}
					columns++;
					int mapTop = umbral.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - 1;
					if (mapTop != y) {
						wrong++;
						if (wrong <= 3) {
							report.add("INFO  height map wrong at " + pos.getX() + ", " + pos.getZ() + ": map says y=" + mapTop + " ("
									+ umbral.getBlockState(pos.atY(mapTop)) + "), real top y=" + y + " (" + umbral.getBlockState(pos.atY(y)) + ")");
						}
					}
					// Only the flat land counts for the shale (some columns here are other fissures)
					if (y > 50) {
						landColumns++;
						if (umbral.getBlockState(pos.atY(y)).is(ModBlocks.DEAD_SEA_SHALE)) {
							shaleTops++;
						}
					}
				}
			}
			return new int[]{columns, wrong, shaleTops, landColumns};
		});
		check("the height map matches the real ground beside the fissure (" + heights[1] + " of " + heights[0] + " columns wrong)", heights[1] == 0);
		check("the flat land beside the fissure is Dead Sea Shale (" + heights[2] + " of " + heights[3] + " columns; 90%+)", heights[2] * 10 >= heights[3] * 9);
		check("the Chasm's surface is Dead Sea Shale", Boolean.parseBoolean(column[0]));
		check("Shadestone lies under the shale", Boolean.parseBoolean(column[1]));
		check("water fills the bottom of the fissure", Boolean.parseBoolean(column[2]));
		check("the fissure is open air halfway down", Boolean.parseBoolean(column[3]));
		int bottomX = (thin.startX() + thin.endX()) / 2;
		command(String.format(java.util.Locale.ROOT, "execute in umbral_world:umbral_world run tp @a %d.5 -46 %d.5 0 -70", bottomX, thin.z()));
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		context.takeScreenshot("chasm_from_the_water_looking_up");
		command("execute in minecraft:overworld run tp @a 0.5 -60 0.5");
		world.getConnection().waitForChunksRender();
	}

	/**
	 * Looks at every column in a square around the centre. Returns {fissure columns, the narrowest
	 * fissure width, pieces of fissure that don't connect to the edge of the square}. A piece that
	 * reaches the square's edge may join the rest outside it, so only fully enclosed pieces count
	 * as cut off. Spots near another biome are left out (fissures fade out at the Chasm's edge).
	 */
	private static int[] chasmWeb(ServerLevel umbral, BlockPos centre, int radius) {
		ChunkGenerator generator = umbral.getChunkSource().getGenerator();
		RandomState random = umbral.getChunkSource().randomState();
		int size = radius * 2 + 1;
		boolean[][] low = new boolean[size][size];
		boolean[][] interior = new boolean[size][size];
		int fissureColumns = 0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				int x = centre.getX() - radius + i;
				int z = centre.getZ() - radius + j;
				interior[i][j] = isChasm(umbral, x, z) && isChasm(umbral, x + 24, z) && isChasm(umbral, x - 24, z)
						&& isChasm(umbral, x, z + 24) && isChasm(umbral, x, z - 24);
				low[i][j] = isChasm(umbral, x, z) && generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, umbral, random) < 20;
				if (low[i][j]) {
					fissureColumns++;
				}
			}
		}
		// Narrowest: for each fissure column, the shorter of its east-west and north-south runs
		// (never less than the true width; only counted where the run doesn't reach the square's edge)
		int narrowest = Integer.MAX_VALUE;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (!low[i][j] || !interior[i][j]) {
					continue;
				}
				int west = i;
				while (west > 0 && low[west - 1][j]) {
					west--;
				}
				int east = i;
				while (east < size - 1 && low[east + 1][j]) {
					east++;
				}
				int north = j;
				while (north > 0 && low[i][north - 1]) {
					north--;
				}
				int south = j;
				while (south < size - 1 && low[i][south + 1]) {
					south++;
				}
				boolean xOpen = west > 0 && east < size - 1;
				boolean zOpen = north > 0 && south < size - 1;
				int across = Math.min(xOpen ? east - west + 1 : Integer.MAX_VALUE, zOpen ? south - north + 1 : Integer.MAX_VALUE);
				narrowest = Math.min(narrowest, across);
			}
		}
		// Cut-off pieces: flood-fill each piece of fissure (corners count as touching)
		boolean[][] seen = new boolean[size][size];
		int cutOff = 0;
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				if (!low[i][j] || seen[i][j]) {
					continue;
				}
				boolean reachesOut = false;
				java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
				queue.add(new int[]{i, j});
				seen[i][j] = true;
				while (!queue.isEmpty()) {
					int[] cell = queue.poll();
					if (cell[0] == 0 || cell[1] == 0 || cell[0] == size - 1 || cell[1] == size - 1 || !interior[cell[0]][cell[1]]) {
						reachesOut = true;
					}
					for (int di = -1; di <= 1; di++) {
						for (int dj = -1; dj <= 1; dj++) {
							int ni = cell[0] + di;
							int nj = cell[1] + dj;
							if (ni >= 0 && nj >= 0 && ni < size && nj < size && low[ni][nj] && !seen[ni][nj]) {
								seen[ni][nj] = true;
								queue.add(new int[]{ni, nj});
							}
						}
					}
				}
				if (!reachesOut) {
					cutOff++;
				}
			}
		}
		return new int[]{fissureColumns, narrowest == Integer.MAX_VALUE ? 0 : narrowest, cutOff};
	}

	/** Stands 2 blocks back from a fissure's west edge, facing east and looking down into it. */
	private void lookIntoFissure(Fissure fissure, String screenshot) {
		int height = onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			return umbral.getChunkSource().getGenerator().getBaseHeight(fissure.startX() - 2, fissure.z(), Heightmap.Types.OCEAN_FLOOR_WG,
					umbral, umbral.getChunkSource().randomState());
		});
		command(String.format(java.util.Locale.ROOT, "execute in umbral_world:umbral_world run tp @a %d.5 %d %d.5 -90 35",
				fissure.startX() - 2, height, fissure.z()));
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		context.takeScreenshot(screenshot + "_" + fissure.width() + "_wide");
	}

	private static boolean isChasm(ServerLevel level, int x, int z) {
		return isBiome(level, "chasm", x, z);
	}

	private static boolean isBiome(ServerLevel level, String biome, int x, int z) {
		return level.getUncachedNoiseBiome(x >> 2, 16, z >> 2).is(ResourceKey.create(Registries.BIOME, UmbralWorld.id(biome)));
	}

	// =====================================================================================
	// Miasma terrain
	// =====================================================================================

	private void testMiasmaHills() {
		// How hilly each biome is: heights on a grid 256 blocks across, deep inside each biome
		String[] biomes = {"grave", "miasma"};
		double[] spread = new double[2];
		BlockPos[] centres = new BlockPos[2];
		for (int b = 0; b < 2; b++) {
			String biome = biomes[b];
			Object[] result = onServer(server -> {
				ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
				ChunkGenerator generator = umbral.getChunkSource().getGenerator();
				RandomState random = umbral.getChunkSource().randomState();
				for (int r = 0; r < 4096; r += 64) {
					for (int x = -r; x <= r; x += 64) {
						for (int z : new int[]{-r, r}) {
							if (isBiome(umbral, biome, x, z) && isBiome(umbral, biome, x + 160, z) && isBiome(umbral, biome, x - 160, z)
									&& isBiome(umbral, biome, x, z + 160) && isBiome(umbral, biome, x, z - 160)) {
								List<Integer> heights = new ArrayList<>();
								for (int dx = -128; dx <= 128; dx += 8) {
									for (int dz = -128; dz <= 128; dz += 8) {
										if (isBiome(umbral, biome, x + dx, z + dz)) {
											heights.add(generator.getBaseHeight(x + dx, z + dz, Heightmap.Types.OCEAN_FLOOR_WG, umbral, random));
										}
									}
								}
								double mean = heights.stream().mapToInt(Integer::intValue).average().orElse(0);
								double deviation = Math.sqrt(heights.stream().mapToDouble(h -> (h - mean) * (h - mean)).average().orElse(0));
								int low = heights.stream().mapToInt(Integer::intValue).min().orElse(0);
								int high = heights.stream().mapToInt(Integer::intValue).max().orElse(0);
								return new Object[]{new BlockPos(x, 0, z), deviation,
										String.format(java.util.Locale.ROOT, "%s around %d, %d: height y=%d to %d, average %.1f, typical rise and fall %.1f blocks",
												biome, x, z, low, high, mean, deviation)};
							}
						}
					}
				}
				return null;
			});
			check("found a spot deep inside the " + biome, result != null);
			if (result == null) {
				return;
			}
			centres[b] = (BlockPos) result[0];
			spread[b] = (double) result[1];
			report.add("INFO  " + result[2]);
		}
		check(String.format(java.util.Locale.ROOT, "the Miasma is clearly hillier than the Grave (%.1f vs %.1f blocks; 2x+)", spread[1], spread[0]),
				spread[1] >= spread[0] * 2.0);

		// The forest: count Ghost and Phantom Tree logs around the Miasma spot, then look at it
		BlockPos miasma = centres[1];
		int logs = onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			int count = 0;
			for (BlockPos pos : BlockPos.betweenClosed(miasma.offset(-24, 30, -24), miasma.offset(24, 140, 24))) {
				BlockState state = umbral.getBlockState(pos);
				if (state.is(ModBlocks.GHOST_LOG) || state.is(ModBlocks.PHANTOM_LOG)) {
					count++;
				}
			}
			return count;
		});
		report.add("INFO  Ghost/Phantom log blocks in a 49 x 49 area of the Miasma: " + logs);
		check("the Miasma has trees (" + logs + " log blocks in 49 x 49)", logs > 0);
		int ground = onServer(server -> server.getLevel(ModDimensions.UMBRAL_WORLD)
				.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, miasma.getX(), miasma.getZ()));
		command(String.format(java.util.Locale.ROOT, "execute in umbral_world:umbral_world run tp @a %d.5 %d %d.5 0 10",
				miasma.getX(), ground + 1, miasma.getZ()));
		world.getConnection().waitForChunksRender();
		context.waitTicks(40);
		context.takeScreenshot("miasma_hills_and_trees");
		command("execute in minecraft:overworld run tp @a 0.5 -60 0.5");
		world.getConnection().waitForChunksRender();
	}

	// =====================================================================================
	// Small things: Grave plants coming back to life, snuffed flames, the void, smoking tools
	// =====================================================================================

	private void testSmallThings() {
		// 1. Grave plants on grass turn into tall vanilla flowers; on Barrow Dirt they don't
		String revived = onServer(server -> {
			ServerLevel level = server.overworld();
			RandomSource random = level.getRandom();
			StringBuilder result = new StringBuilder();
			Object[][] plants = {
					{ModBlocks.WITHERED_TUFTS, Blocks.ROSE_BUSH, Blocks.GRASS_BLOCK},
					{ModBlocks.GRAVE_THORNS, Blocks.LILAC, Blocks.GRASS_BLOCK},
					{ModBlocks.WITHERED_TUFTS, ModBlocks.WITHERED_TUFTS, ModBlocks.BARROW_DIRT},
			};
			int x = 400;
			for (Object[] plant : plants) {
				BlockPos pos = new BlockPos(x, FEET, 0);
				x += 3;
				level.setBlockAndUpdate(pos.below(), ((net.minecraft.world.level.block.Block) plant[2]).defaultBlockState());
				level.setBlockAndUpdate(pos, ((net.minecraft.world.level.block.Block) plant[0]).defaultBlockState());
				int ticks = 0;
				while (ticks < 200 && level.getBlockState(pos).is((net.minecraft.world.level.block.Block) plant[0])) {
					level.getBlockState(pos).randomTick(level, pos, random);
					ticks++;
				}
				BlockState bottom = level.getBlockState(pos);
				BlockState top = level.getBlockState(pos.above());
				boolean ok = plant[0] == plant[1]
						? bottom.is((net.minecraft.world.level.block.Block) plant[0])
						: bottom.is((net.minecraft.world.level.block.Block) plant[1]) && top.is((net.minecraft.world.level.block.Block) plant[1]);
				result.append(ok ? "ok" : "WRONG").append(" (").append(ticks).append(" random ticks); ");
			}
			return result.toString();
		});
		check("Withered Tufts -> Rose Bush and Grave Thorns -> Lilac on grass, nothing on Barrow Dirt (" + revived + ")",
				!revived.contains("WRONG"));

		// 2. Lit candles and campfires go out in the Umbral World, but not in the Overworld
		BlockPos platform = new BlockPos(2000, 250, 2000);
		onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			fill(umbral, platform.offset(-3, 0, -3), platform.offset(3, 0, 3), Blocks.STONE.defaultBlockState());
			return null;
		});
		command("execute in umbral_world:umbral_world run tp @a 2000.5 251 2000.5");
		world.getConnection().waitForChunksRender();
		onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			umbral.setBlockAndUpdate(platform.offset(2, 1, 0), Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true));
			umbral.setBlockAndUpdate(platform.offset(-2, 1, 0), Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
			ServerLevel overworld = server.overworld();
			overworld.setBlockAndUpdate(new BlockPos(410, FEET, 0), Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true));
			return null;
		});
		boolean litAtFirst = onServer(server -> server.getLevel(ModDimensions.UMBRAL_WORLD)
				.getBlockState(platform.offset(2, 1, 0)).getValue(net.minecraft.world.level.block.CampfireBlock.LIT));
		context.waitTicks(40);
		boolean[] out = onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			return new boolean[]{
					!umbral.getBlockState(platform.offset(2, 1, 0)).getValue(net.minecraft.world.level.block.CampfireBlock.LIT),
					!umbral.getBlockState(platform.offset(-2, 1, 0)).getValue(net.minecraft.world.level.block.CandleBlock.LIT),
					server.overworld().getBlockState(new BlockPos(410, FEET, 0)).getValue(net.minecraft.world.level.block.CampfireBlock.LIT)
			};
		});
		check("a campfire lit in the Umbral World starts lit...", litAtFirst);
		check("... and is snuffed within 2 seconds", out[0]);
		check("a candle lit in the Umbral World is snuffed within 2 seconds", out[1]);
		check("a campfire in the Overworld stays lit", out[2]);

		// 3. Smoke from a held Shadestone Blade (screenshots only: smoke is client-side)
		onServer(server -> {
			player(server).setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SHADESTONE_BLADE));
			return null;
		});
		command("execute in umbral_world:umbral_world run tp @a 2000.5 251 2000.5 0 0");
		context.waitTicks(40);
		context.takeScreenshot("blade_smoke_first_person");
		context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(40);
		context.takeScreenshot("blade_smoke_third_person");
		context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		onServer(server -> {
			player(server).setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			return null;
		});

		// 4. Falling out of the bottom of the Overworld lands you in the Umbral World
		command("execute in minecraft:overworld run tp @a 500.5 -130 0.5");
		context.waitTicks(60);
		String landed = onServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = (ServerLevel) player.level();
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, player.getBlockX(), player.getBlockZ());
			return level.dimension().identifier() + " y=" + String.format(java.util.Locale.ROOT, "%.1f", player.getY())
					+ " ground=" + ground + " health=" + player.getHealth();
		});
		boolean safe = onServer(server -> {
			ServerPlayer player = player(server);
			return player.level().dimension() == ModDimensions.UMBRAL_WORLD
					&& player.isAlive() && player.getHealth() >= player.getMaxHealth() - 1.0f;
		});
		check("falling into the Overworld's void lands you safely in the Umbral World (" + landed + ")", safe);
		context.takeScreenshot("void_fall_arrival");

		// ... and so does falling out of the End
		command("execute in minecraft:the_end run tp @a 3000.5 -70 0.5");
		context.waitTicks(60);
		boolean fromEnd = onServer(server -> {
			ServerPlayer player = player(server);
			return player.level().dimension() == ModDimensions.UMBRAL_WORLD
					&& player.isAlive() && player.getHealth() >= player.getMaxHealth() - 1.0f;
		});
		check("falling into the End's void also lands you safely in the Umbral World", fromEnd);
		command("execute in minecraft:overworld run tp @a 0.5 -60 0.5");
		world.getConnection().waitForChunksRender();
	}

	// =====================================================================================
	// Chasm life: Finger Weeds and Cinderite Ore, in real generated chunks
	// =====================================================================================

	private void testChasmLife() {
		int radius = 5; // chunks each way: 11 x 11 chunks
		// Fields: weed blocks, columns, columns reaching the top, unbroken columns, Chasm chunks, ore blocks, veins,
		// ore blocks touching water, ore blocks below the water top, average column width (x10)
		int[] stats = new int[10];
		List<String> notes = new ArrayList<>();
		BlockPos[] climbStart = {null};
		Direction[] climbFacing = {null};
		BlockPos[] oreSpot = {null};
		BlockPos centre = onServer(server -> {
			ServerLevel umbral = server.getLevel(ModDimensions.UMBRAL_WORLD);
			BlockPos found = null;
			search:
			for (int r = 0; r < 4096; r += 64) {
				for (int x = -r; x <= r; x += 64) {
					for (int z : new int[]{-r, r}) {
						if (isChasm(umbral, x, z) && isChasm(umbral, x + 96, z) && isChasm(umbral, x - 96, z)
								&& isChasm(umbral, x, z + 96) && isChasm(umbral, x, z - 96)) {
							found = new BlockPos(x, 0, z);
							break search;
						}
					}
				}
			}
			if (found == null) {
				return null;
			}
			int seaLevel = umbral.getChunkSource().getGenerator().getSeaLevel();
			java.util.Set<BlockPos> weeds = new java.util.HashSet<>();
			java.util.Set<BlockPos> ores = new java.util.HashSet<>();
			int fcx = found.getX() >> 4;
			int fcz = found.getZ() >> 4;
			for (int cx = fcx - radius; cx <= fcx + radius; cx++) {
				for (int cz = fcz - radius; cz <= fcz + radius; cz++) {
					umbral.getChunk(cx, cz); // generates the chunk completely, features and all
					if (Math.abs(cx - fcx) < radius && Math.abs(cz - fcz) < radius && isChasm(umbral, (cx << 4) + 8, (cz << 4) + 8)) {
						stats[4]++;
					}
				}
			}
			for (int cx = fcx - radius; cx <= fcx + radius; cx++) {
				for (int cz = fcz - radius; cz <= fcz + radius; cz++) {
					for (BlockPos pos : BlockPos.betweenClosed((cx << 4), seaLevel - 20, (cz << 4), (cx << 4) + 15, 100, (cz << 4) + 15)) {
						BlockState state = umbral.getBlockState(pos);
						if (state.is(ModBlocks.FINGER_WEEDS)) {
							weeds.add(pos.immutable());
						} else if (state.is(ModBlocks.CINDERITE_ORE)) {
							ores.add(pos.immutable());
						}
					}
				}
			}
			stats[0] = weeds.size();
			List<Integer> widths = new ArrayList<>(); // blocks per row (x10) of each column
			for (List<BlockPos> column : groups(weeds)) {
				// Skip columns touching the outer ring of chunks: they may carry on outside the scanned area
				boolean nearEdge = column.stream().anyMatch(pos -> Math.abs((pos.getX() >> 4) - fcx) >= radius
						|| Math.abs((pos.getZ() >> 4) - fcz) >= radius);
				if (nearEdge) {
					continue;
				}
				stats[1]++;
				int low = column.stream().mapToInt(BlockPos::getY).min().orElse(0);
				int high = column.stream().mapToInt(BlockPos::getY).max().orElse(0);
				java.util.Map<Integer, Integer> perRow = new java.util.HashMap<>();
				for (BlockPos pos : column) {
					perRow.merge(pos.getY(), 1, Integer::sum);
				}
				boolean unbroken = perRow.size() == high - low + 1;
				boolean fromWaterToTop = low <= seaLevel + 1 && high >= 55;
				if (unbroken) {
					stats[3]++;
				}
				if (fromWaterToTop) {
					stats[2]++;
				}
				widths.add(10 * column.size() / perRow.size());
				if (notes.size() < 3 || (!(unbroken && fromWaterToTop) && notes.size() < 12)) {
					String gap = "";
					if (!fromWaterToTop) {
						// What's just above the top of this piece: deleted (air against a wall) or the wall ending?
						BlockPos top = column.stream().filter(pos -> pos.getY() == high).findFirst().orElseThrow();
						Direction facing = umbral.getBlockState(top).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
						BlockPos above = top.above();
						gap = " | above: " + umbral.getBlockState(above).getBlock().getName().getString()
								+ ", behind it: " + umbral.getBlockState(above.relative(facing.getOpposite())).getBlock().getName().getString()
								+ ", chunk " + (top.getX() >> 4) + "," + (top.getZ() >> 4) + " at " + top.toShortString();
					}
					notes.add("at " + column.getFirst().toShortString() + ": y " + low + " to " + high + ", " + column.size() + " blocks" + (unbroken ? "" : ", HAS GAPS") + gap);
				}
				if (climbStart[0] == null && unbroken && fromWaterToTop) {
					// The middle block of the bottom row
					List<BlockPos> bottom = column.stream().filter(pos -> pos.getY() == low).sorted(
							java.util.Comparator.<BlockPos>comparingInt(pos -> pos.getX()).thenComparingInt(pos -> pos.getZ())).toList();
					climbStart[0] = bottom.get(bottom.size() / 2);
					climbFacing[0] = umbral.getBlockState(climbStart[0]).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
				}
			}
			// The median, so two columns that happen to grow into each other don't skew it
			stats[9] = widths.isEmpty() ? 0 : widths.stream().sorted().toList().get(widths.size() / 2);
			for (List<BlockPos> vein : groups(ores)) {
				stats[6]++;
				if (oreSpot[0] == null) {
					oreSpot[0] = vein.getFirst();
				}
			}
			stats[5] = ores.size();
			for (BlockPos ore : ores) {
				boolean wet = false;
				for (Direction side : Direction.values()) {
					wet |= umbral.getFluidState(ore.relative(side)).is(net.minecraft.tags.FluidTags.WATER);
				}
				if (wet) {
					stats[7]++;
				}
				if (ore.getY() < seaLevel) {
					stats[8]++;
				}
			}
			return found;
		});
		check("found a spot deep inside a Chasm", centre != null);
		if (centre == null) {
			return;
		}
		report.add("INFO  " + (2 * radius + 1) * (2 * radius + 1) + " chunks around " + centre.toShortString() + "; the inner "
				+ (2 * radius - 1) * (2 * radius - 1) + " (where Finger Weeds are judged) have " + stats[4] + " Chasm chunks");
		report.add("INFO  Finger Weeds: " + stats[0] + " blocks in " + stats[1] + " columns (median width " + stats[9] / 10.0 + "); first few: " + notes);
		report.add("INFO  Cinderite Ore: " + stats[5] + " blocks in " + stats[6] + " veins");
		check("Finger Weeds columns generate (" + stats[1] + " whole columns in " + stats[4] + " Chasm chunks)", stats[1] > 0);
		check("every column is unbroken from bottom to top (" + stats[3] + " of " + stats[1] + ")", stats[3] == stats[1]);
		check("columns run from the water to the top of the wall (" + stats[2] + " of " + stats[1] + "; 90%+)", stats[2] * 10 >= stats[1] * 9);
		check("columns are 3-4 blocks wide (median " + stats[9] / 10.0 + ")", stats[9] >= 25 && stats[9] <= 45);
		check("Cinderite Ore generates (" + stats[5] + " blocks, " + stats[6] + " veins)", stats[5] > 0);
		check("every Cinderite Ore touches water (" + stats[7] + " of " + stats[5] + ")", stats[7] == stats[5]);
		check("every Cinderite Ore is under the water line (" + stats[8] + " of " + stats[5] + ")", stats[8] == stats[5]);

		// Drops and smelting
		double raw = onServer(server -> {
			ServerLevel level = server.overworld();
			LootTable loot = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, UmbralWorld.id("blocks/cinderite_ore")));
			LootParams params = new LootParams.Builder(level)
					.withParameter(LootContextParams.BLOCK_STATE, ModBlocks.CINDERITE_ORE.defaultBlockState())
					.withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
					.withParameter(LootContextParams.TOOL, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE))
					.create(LootContextParamSets.BLOCK);
			int count = 0;
			for (int i = 0; i < 200; i++) {
				for (ItemStack stack : loot.getRandomItems(params)) {
					if (stack.is(ModItems.RAW_CINDERITE)) {
						count += stack.getCount();
					}
				}
			}
			return count / 200.0;
		});
		check("mined with a pickaxe, Cinderite Ore drops 1 Raw Cinderite (average " + raw + ")", raw == 1.0);
		boolean smelts = onServer(server -> server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
				new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(ModItems.RAW_CINDERITE)), server.overworld())
				.map(recipe -> recipe.id().identifier().equals(UmbralWorld.id("cinderite_ingot_from_smelting"))).orElse(false));
		check("Raw Cinderite smelts into a Cinderite Ingot", smelts);

		// Climb: from the water at the foot of a column, face the wall and hold forward
		command("effect give @a minecraft:night_vision infinite 0 true");
		if (climbStart[0] != null) {
			BlockPos start = climbStart[0];
			Direction facing = climbFacing[0];
			String standThere = String.format(java.util.Locale.ROOT, "execute in umbral_world:umbral_world run tp @a %.2f %.2f %.2f %.1f -10",
					start.getX() + 0.5 + facing.getStepX() * 0.2, start.getY() + 0.2, start.getZ() + 0.5 + facing.getStepZ() * 0.2,
					facing.getOpposite().toYRot());
			command(standThere);
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("finger_weeds_close_up");
			command(standThere);
			double before = playerPosition().y;
			// Forward and jump, like climbing vines
			context.getInput().holdKey(options -> options.keyUp);
			context.getInput().holdKey(options -> options.keyJump);
			context.waitTicks(60);
			context.getInput().releaseKey(options -> options.keyJump);
			context.getInput().releaseKey(options -> options.keyUp);
			double after = playerPosition().y;
			check(String.format(java.util.Locale.ROOT, "holding forward and jump against a column climbs it (y %.1f -> %.1f in 3 seconds)", before, after),
					after - before >= 3.0);
			// A look at the column from across the fissure, looking a little down
			BlockPos across = start.relative(facing, 6).above(12);
			command(String.format(java.util.Locale.ROOT, "execute in umbral_world:umbral_world run tp @a %d.5 %d %d.5 %.1f 30",
					across.getX(), across.getY(), across.getZ(), facing.getOpposite().toYRot()));
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("finger_weeds_from_across");
		}
		if (oreSpot[0] != null) {
			BlockPos above = oreSpot[0].above(3);
			command(String.format(java.util.Locale.ROOT, "execute in umbral_world:umbral_world run tp @a %d.5 %d %d.5 0 70",
					above.getX(), above.getY(), above.getZ()));
			world.getConnection().waitForChunksRender();
			context.waitTicks(40);
			context.takeScreenshot("cinderite_ore_underwater");
		}
		command("effect clear @a minecraft:night_vision");

		// The Umbral fog hides everything down there, so also show them in Overworld daylight:
		// a stone wall with a patch of Finger Weeds, and Cinderite Ore in a pool
		onServer(server -> {
			ServerLevel level = server.overworld();
			fill(level, new BlockPos(600, FEET, 10), new BlockPos(606, FEET + 6, 10), Blocks.STONE.defaultBlockState());
			BlockState weeds = ModBlocks.FINGER_WEEDS.defaultBlockState()
					.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH);
			for (int x = 601; x <= 605; x++) {
				for (int y = FEET; y <= FEET + 5; y++) {
					if (Math.abs(x - 603) + (y == FEET + 5 ? 1 : 0) <= 1 || (x - 601 + y) % 2 == 0) {
						level.setBlockAndUpdate(new BlockPos(x, y, 9), weeds);
					}
				}
			}
			fill(level, new BlockPos(608, GROUND - 2, 4), new BlockPos(612, GROUND, 8), Blocks.STONE.defaultBlockState());
			fill(level, new BlockPos(609, GROUND - 1, 5), new BlockPos(611, GROUND, 7), Blocks.WATER.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(610, GROUND - 2, 6), ModBlocks.CINDERITE_ORE.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(612, GROUND - 1, 6), ModBlocks.CINDERITE_ORE.defaultBlockState());
			return null;
		});
		command("time set noon");
		command("execute in minecraft:overworld run tp @a 603.5 -60 4.5 0 10");
		world.getConnection().waitForChunksRender();
		context.waitTicks(30);
		context.takeScreenshot("finger_weeds_daylight");
		command("execute in minecraft:overworld run tp @a 603.5 -58.5 7.0 0 10");
		context.waitTicks(20);
		context.takeScreenshot("finger_weeds_close");
		command("execute in minecraft:overworld run tp @a 600.0 -58.5 6.5 -50 15");
		context.waitTicks(20);
		context.takeScreenshot("finger_weeds_close_from_the_side");
		command("execute in minecraft:overworld run tp @a 610.5 -59 3.5 0 50");
		world.getConnection().waitForChunksRender();
		context.waitTicks(40);
		context.takeScreenshot("cinderite_ore_in_water_daylight");
		// A floor of Dead Sea Shale with a low step, to see the top and the sides
		onServer(server -> {
			ServerLevel level = server.overworld();
			fill(level, new BlockPos(620, GROUND, 0), new BlockPos(634, GROUND, 14), ModBlocks.DEAD_SEA_SHALE.defaultBlockState());
			fill(level, new BlockPos(626, FEET, 8), new BlockPos(630, FEET + 1, 12), ModBlocks.DEAD_SEA_SHALE.defaultBlockState());
			return null;
		});
		command("execute in minecraft:overworld run tp @a 627.5 -58 0.5 0 35");
		world.getConnection().waitForChunksRender();
		context.waitTicks(30);
		context.takeScreenshot("dead_sea_shale_daylight");
		command("execute in minecraft:overworld run tp @a 0.5 -60 0.5");
		world.getConnection().waitForChunksRender();
	}

	/**
	 * Watches a mob for the given number of ticks and returns its best speed over any stretch
	 * of "window" ticks, in blocks per second: its top speed, not counting turning and speeding up.
	 */
	/** A real player's top swimming speed (blocks per second): holding forward, and sprint too if asked. */
	private double playerPeakSpeed(boolean sprint) {
		context.getInput().holdKey(options -> options.keyUp);
		if (sprint) {
			context.getInput().holdKey(options -> options.keySprint);
		}
		List<Vec3> positions = new ArrayList<>();
		for (int i = 0; i <= 40; i++) {
			positions.add(playerPosition());
			context.waitTicks(1);
		}
		context.getInput().releaseKey(options -> options.keyUp);
		if (sprint) {
			context.getInput().releaseKey(options -> options.keySprint);
		}
		double best = 0.0;
		for (int i = 10; i < positions.size(); i++) {
			Vec3 a = positions.get(i - 10);
			Vec3 b = positions.get(i);
			best = Math.max(best, Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z)) * 2.0);
		}
		return best;
	}

	private double peakSpeed(UUID id, int ticks, int window) {
		List<Vec3> positions = new ArrayList<>();
		for (int i = 0; i <= ticks; i++) {
			positions.add(entityPosition(id));
			context.waitTicks(1);
		}
		double best = 0.0;
		for (int i = window; i < positions.size(); i++) {
			best = Math.max(best, positions.get(i).distanceTo(positions.get(i - window)) * 20.0 / window);
		}
		return best;
	}

	/** Splits blocks into groups that touch each other (sides, edges or corners). */
	private static List<List<BlockPos>> groups(java.util.Set<BlockPos> blocks) {
		List<List<BlockPos>> result = new ArrayList<>();
		java.util.Set<BlockPos> left = new java.util.HashSet<>(blocks);
		while (!left.isEmpty()) {
			BlockPos first = left.iterator().next();
			left.remove(first);
			List<BlockPos> group = new ArrayList<>();
			java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(List.of(first));
			while (!queue.isEmpty()) {
				BlockPos pos = queue.poll();
				group.add(pos);
				for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
					if (left.remove(near)) {
						queue.add(near.immutable());
					}
				}
			}
			result.add(group);
		}
		return result;
	}

	// =====================================================================================
	// Chasm mobs: the Murkfiend and the Chasm Skate, in a glass pool
	// =====================================================================================

	private void testChasmMobs() {
		// A pool 31 x 13, 6 deep, on the flat world's grass, with glass walls
		onServer(server -> {
			ServerLevel level = server.overworld();
			fill(level, new BlockPos(699, FEET, -7), new BlockPos(731, FEET + 5, 7), Blocks.GLASS.defaultBlockState());
			fill(level, new BlockPos(700, FEET, -6), new BlockPos(730, FEET + 5, 6), Blocks.WATER.defaultBlockState());
			return null;
		});
		command("time set noon");

		// 0. How fast a player swims, for comparison: holding forward, then sprinting too
		command("execute in minecraft:overworld run tp @a 702.5 -57 0.5 -90 0");
		world.getConnection().waitForChunksRender();
		context.waitTicks(10);
		double swim = playerPeakSpeed(false);
		command("execute in minecraft:overworld run tp @a 702.5 -57 0.5 -90 0");
		context.waitTicks(10);
		double sprintSwim = playerPeakSpeed(true);
		report.add(String.format(java.util.Locale.ROOT, "INFO  a player swims at %.1f blocks/second, %.1f sprint-swimming", swim, sprintSwim));

		// 1. The Murkfiend chases a swimmer down and bites
		command("execute in minecraft:overworld run tp @a 702.5 -57 0.5 -90 0");
		world.getConnection().waitForChunksRender();
		UUID murkfiend = spawn(ModEntities.MURKFIEND, new BlockPos(721, -57, 0));
		double chaseSpeed = peakSpeed(murkfiend, 80, 20); // 19 blocks away: room to reach full speed, well inside its follow range (24, give or take a random bit)
		boolean chasing = onServer(server -> ((Mob) server.overworld().getEntity(murkfiend)).getTarget() instanceof ServerPlayer);
		check("the Murkfiend goes after a swimming player", chasing);
		check(String.format(java.util.Locale.ROOT, "it's faster than a swimming player but slower than sprint-swimming (%.1f vs %.1f and %.1f blocks/second)",
				chaseSpeed, swim, sprintSwim), chaseSpeed > swim && chaseSpeed < sprintSwim);
		int[] waited = {0};
		while (waited[0] < 100 && !onServer(server -> player(server).getLastHurtByMob() instanceof Murkfiend)) {
			context.waitTicks(5);
			waited[0] += 5;
		}
		check(String.format(java.util.Locale.ROOT, "it bites (after %.1f more seconds)", waited[0] / 20.0),
				onServer(server -> player(server).getLastHurtByMob() instanceof Murkfiend));

		// 2. Climb out and it gives up
		command("execute in minecraft:overworld run tp @a 703.5 -60 10.5");
		context.waitTicks(30);
		check("when you climb out of the water it gives up on you",
				onServer(server -> ((Mob) server.overworld().getEntity(murkfiend)).getTarget() == null));
		check("... and it stays in the water", onServer(server -> server.overworld().getEntity(murkfiend).isInWater()));

		// 3. On land it dries out and dies
		UUID beached = spawn(ModEntities.MURKFIEND, new BlockPos(708, FEET, 12));
		context.waitTicks(200);
		check("a Murkfiend on land dies within 10 seconds", onServer(server -> {
			Entity entity = server.overworld().getEntity(beached);
			return entity == null || !entity.isAlive();
		}));
		command("kill @e[type=umbral_world:murkfiend]");

		// 4. The Chasm Skate lies still until you swim close
		command("execute in minecraft:overworld run tp @a 705.5 -57 0.5 90 0");
		world.getConnection().waitForChunksRender();
		UUID skate = spawn(ModEntities.CHASM_SKATE, new BlockPos(720, FEET, 0));
		context.waitTicks(20);
		Vec3 restStart = entityPosition(skate);
		context.waitTicks(40);
		double restMoved = entityPosition(skate).distanceTo(restStart);
		check(String.format(java.util.Locale.ROOT, "the Skate lies still while you swim 15 blocks away (moved %.2f blocks in 2 seconds)", restMoved),
				restMoved < 0.3 && onServer(server -> ((Mob) server.overworld().getEntity(skate)).getTarget() == null));

		// Swim close: 4 blocks to the side, 3 up
		command("execute in minecraft:overworld run tp @a 716.5 -57 0.5");
		double burstSpeed = peakSpeed(skate, 30, 5);
		check("swim close to it and it attacks in a burst of speed", onServer(server -> {
			ChasmSkate entity = (ChasmSkate) server.overworld().getEntity(skate);
			return entity.getTarget() instanceof ServerPlayer;
		}));
		// After the burst: back off 10 blocks and time how fast it follows
		context.waitTicks(20);
		Vec3 here = entityPosition(skate);
		command(String.format(java.util.Locale.ROOT, "execute in minecraft:overworld run tp @a %.2f -57 %.2f", here.x - 10.0, here.z));
		context.waitTicks(20);
		double slowSpeed = peakSpeed(skate, 40, 20);
		check(String.format(java.util.Locale.ROOT, "the burst is much faster than a swimming player (%.1f vs %.1f blocks/second)", burstSpeed, swim),
				burstSpeed >= swim * 1.75);
		check(String.format(java.util.Locale.ROOT, "after the burst it's slower than a swimming player (%.1f vs %.1f blocks/second)", slowSpeed, swim),
				slowSpeed >= 0.5 && slowSpeed < swim);
		// Let it reach you
		command("execute in minecraft:overworld run tp @a " + String.format(java.util.Locale.ROOT, "%.2f -58 %.2f",
				entityPosition(skate).x + 1.0, entityPosition(skate).z));
		waited[0] = 0;
		while (waited[0] < 100 && !onServer(server -> player(server).getLastHurtByMob() instanceof ChasmSkate)) {
			context.waitTicks(5);
			waited[0] += 5;
		}
		check(String.format(java.util.Locale.ROOT, "it stings (after %.1f seconds next to it)", waited[0] / 20.0),
				onServer(server -> player(server).getLastHurtByMob() instanceof ChasmSkate));
		command("kill @e[type=umbral_world:chasm_skate]");

		// 5. Spawn rules
		boolean[] rules = onServer(server -> {
			ServerLevel level = server.overworld();
			RandomSource random = level.getRandom();
			return new boolean[]{
					ChasmSwimmer.checkChasmWaterSpawnRules(ModEntities.MURKFIEND, level, EntitySpawnReason.SPAWNER, new BlockPos(710, FEET + 2, 0), random),
					ChasmSwimmer.checkChasmWaterSpawnRules(ModEntities.MURKFIEND, level, EntitySpawnReason.SPAWNER, new BlockPos(710, FEET, 12), random),
					ChasmSwimmer.checkChasmWaterSpawnRules(ModEntities.MURKFIEND, level, EntitySpawnReason.NATURAL, new BlockPos(710, FEET + 2, 0), random),
					ChasmSkate.checkChasmSkateSpawnRules(ModEntities.CHASM_SKATE, level, EntitySpawnReason.SPAWNER, new BlockPos(710, FEET, 0), random),
					ChasmSkate.checkChasmSkateSpawnRules(ModEntities.CHASM_SKATE, level, EntitySpawnReason.SPAWNER, new BlockPos(710, FEET + 3, 0), random),
			};
		});
		check("Murkfiends may spawn in water", rules[0]);
		check("... not on land", !rules[1]);
		check("... not in daylight", !rules[2]);
		check("Skates may spawn on the floor under water", rules[3]);
		check("... not in mid-water", !rules[4]);

		// 6. Looks (with the texture guides standing in for Mac's art)
		command("summon umbral_world:murkfiend 712.5 -57.5 0.5 {NoAI:1b,Rotation:[0f,0f]}");
		command("summon umbral_world:chasm_skate 712.5 -60 4.5 {NoAI:1b,Rotation:[30f,0f]}");
		command("execute in minecraft:overworld run tp @a 708.5 -57 0.5 -90 10");
		world.getConnection().waitForChunksRender();
		context.waitTicks(30);
		context.takeScreenshot("murkfiend_side_view");
		// Closer, from all round (it faces south, +z)
		command("effect give @a minecraft:water_breathing infinite 0 true");
		String[][] views = {
				{"712.5 -57.6 3.5 180 5", "murkfiend_front"},
				{"715.0 -57.3 3.0 135 15", "murkfiend_front_three_quarter"},
				{"710.0 -57.3 -2.5 -45 15", "murkfiend_back_three_quarter"},
				{"712.5 -56.7 0.6 0 90", "murkfiend_from_above"},
		};
		for (String[] view : views) {
			command("execute in minecraft:overworld run tp @a " + view[0]);
			context.waitTicks(20);
			context.takeScreenshot(view[1]);
		}
		command("effect clear @a minecraft:water_breathing");
		command("execute in minecraft:overworld run tp @a 712.5 -56 1.5 0 60");
		context.waitTicks(30);
		context.takeScreenshot("chasm_skate_from_above");
		command("kill @e[type=umbral_world:murkfiend]");
		command("kill @e[type=umbral_world:chasm_skate]");
		command("execute in minecraft:overworld run tp @a 0.5 -60 0.5");
		world.getConnection().waitForChunksRender();
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
