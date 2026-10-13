package io.github.thedarkhorse99.umbralworld.world;

import com.mojang.serialization.MapCodec;
import io.github.thedarkhorse99.umbralworld.block.GhostLogBlock;
import io.github.thedarkhorse99.umbralworld.block.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Grows one Ghost Tree: a thin trunk that splits into right-angled branches, like a
 * chorus plant, with a puff of Ghost Vapor at the end of each branch.
 * Used both by world generation and by the Ghost Sapling.
 */
public record GhostTreeFeature() implements Feature {
    public static final MapCodec<GhostTreeFeature> CODEC = MapCodec.unit(GhostTreeFeature::new);

    /** How far (in blocks) logs may reach sideways from the trunk. Keeps trees inside the area worldgen allows. */
    private static final int MAX_SPREAD = 4;
    /** How far vapor reaches from each branch tip. */
    private static final int VAPOR_RADIUS = 2;

    @Override
    public MapCodec<GhostTreeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // 1. Plan the shape first, without touching the world
        Set<BlockPos> logs = new LinkedHashSet<>();
        List<BlockPos> tips = new ArrayList<>();

        int trunkHeight = 4 + random.nextInt(4); // 4-7 blocks
        for (int i = 0; i < trunkHeight; i++) {
            logs.add(origin.above(i));
        }
        BlockPos top = origin.above(trunkHeight - 1);
        branch(top, 0, origin, random, logs, tips);

        // Sometimes the trunk keeps going straight up past the branches
        if (random.nextBoolean()) {
            BlockPos p = top;
            for (int i = 0, n = 1 + random.nextInt(3); i < n; i++) {
                p = p.above();
                logs.add(p);
            }
            tips.add(p);
        }

        // 2. Give up if anything is in the way, so trees never cut into terrain or each other
        for (BlockPos pos : logs) {
            if (level.isOutsideBuildHeight(pos) || !isFree(level.getBlockState(pos))) {
                return false;
            }
        }

        // 3. Plan the vapor: a rough ball around each tip, a bit ragged at the edges
        Set<BlockPos> vapor = new LinkedHashSet<>();
        for (BlockPos tip : tips) {
            for (BlockPos pos : BlockPos.betweenClosed(tip.offset(-VAPOR_RADIUS, -1, -VAPOR_RADIUS),
                    tip.offset(VAPOR_RADIUS, VAPOR_RADIUS, VAPOR_RADIUS))) {
                int distance = Math.abs(pos.getX() - tip.getX()) + Math.abs(pos.getY() - tip.getY())
                        + Math.abs(pos.getZ() - tip.getZ());
                if (distance > VAPOR_RADIUS || (distance == VAPOR_RADIUS && random.nextFloat() < 0.35F)) {
                    continue;
                }
                BlockPos immutable = pos.immutable();
                if (!logs.contains(immutable) && !level.isOutsideBuildHeight(immutable)
                        && isFree(level.getBlockState(immutable))) {
                    vapor.add(immutable);
                }
            }
        }

        // 4. Build it: logs first, then work out how each log connects
        BlockState log = ModBlocks.GHOST_LOG.defaultBlockState();
        for (BlockPos pos : logs) {
            setBlock(level, pos, log);
        }
        for (BlockPos pos : logs) {
            setBlock(level, pos, GhostLogBlock.withConnections(level, pos, log));
        }

        // 5. Vapor needs to know how far it is from a log, like leaves do, or it would fade away at once
        Map<BlockPos, Integer> distances = distancesFromLogs(logs, vapor);
        BlockState vaporState = ModBlocks.GHOST_VAPOR.defaultBlockState();
        for (Map.Entry<BlockPos, Integer> entry : distances.entrySet()) {
            setBlock(level, entry.getKey(), vaporState.setValue(LeavesBlock.DISTANCE, entry.getValue()));
        }
        return true;
    }

    /** Splits into 2-3 arms (1-2 on smaller branches). Each goes sideways, then up, then maybe splits again. */
    private static void branch(BlockPos start, int depth, BlockPos origin, RandomSource random,
                               Set<BlockPos> logs, List<BlockPos> tips) {
        List<Direction> directions = new ArrayList<>(List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST));
        for (int i = directions.size() - 1; i > 0; i--) {
            Collections.swap(directions, i, random.nextInt(i + 1));
        }
        int arms = depth == 0 ? 2 + random.nextInt(2) : 1 + random.nextInt(2);

        for (int a = 0; a < arms; a++) {
            Direction direction = directions.get(a);
            BlockPos p = start;
            for (int i = 0, out = 1 + random.nextInt(2); i < out; i++) {
                BlockPos next = p.relative(direction);
                if (Math.abs(next.getX() - origin.getX()) > MAX_SPREAD || Math.abs(next.getZ() - origin.getZ()) > MAX_SPREAD) {
                    break;
                }
                p = next;
                logs.add(p);
            }
            for (int i = 0, up = 1 + random.nextInt(3); i < up; i++) {
                p = p.above();
                logs.add(p);
            }
            if (depth == 0 && random.nextInt(3) != 0) {
                branch(p, depth + 1, origin, random, logs, tips);
            } else {
                tips.add(p);
            }
        }
    }

    /** Walks outward from the logs through the vapor, counting steps (1 = touching a log). */
    private static Map<BlockPos, Integer> distancesFromLogs(Set<BlockPos> logs, Set<BlockPos> vapor) {
        Map<BlockPos, Integer> distances = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(logs);
        Map<BlockPos, Integer> steps = new HashMap<>();
        for (BlockPos pos : logs) {
            steps.put(pos, 0);
        }
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int next = steps.get(pos) + 1;
            if (next >= LeavesBlock.DECAY_DISTANCE) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (vapor.contains(neighbor) && !steps.containsKey(neighbor)) {
                    steps.put(neighbor, next);
                    distances.put(neighbor, next);
                    queue.add(neighbor);
                }
            }
        }
        return distances;
    }

    private static boolean isFree(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }
}
