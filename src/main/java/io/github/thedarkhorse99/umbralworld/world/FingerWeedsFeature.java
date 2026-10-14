package io.github.thedarkhorse99.umbralworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.thedarkhorse99.umbralworld.block.FingerWeedsBlock;
import io.github.thedarkhorse99.umbralworld.block.ModBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Grows one column of Finger Weeds up a Chasm fissure wall, from two blocks under the
 * water to the top, so a player who fell in can always climb out by finding one.
 *
 * The column is 3-4 blocks wide and wanders left and right as it goes up (in sweeps
 * that last a few blocks), so it looks natural. Each row follows the wall in and out:
 * the fissure walls curve and run at angles, so a row can be a little staircase.
 * Every block in a row touches the one beside it, and every row touches the row
 * below, so the climb never breaks. If the wall has a gap (another fissure joining),
 * the column edges sideways around it.
 *
 * The whole column is planned before anything is placed. World generation may only
 * change blocks close to the chunk being generated; if any part of the column would be
 * outside that area, no column is placed at all, so there are never half columns.
 *
 * "max_reach" in the JSON is how far from the chosen spot (in blocks) it looks for a wall.
 */
public record FingerWeedsFeature(int maxReach) implements Feature {
    public static final MapCodec<FingerWeedsFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(1, 12).fieldOf("max_reach").forGetter(FingerWeedsFeature::maxReach)
    ).apply(instance, FingerWeedsFeature::new));

    /** How far the column may wander from where it started, sideways and in or out. Keeps it inside the area worldgen allows. */
    private static final int MAX_DRIFT = 6;
    /** Safety limit on height. The fissures are about 115 blocks deep. */
    private static final int MAX_HEIGHT = 200;
    /** How many blocks under the water surface the column starts. */
    private static final int UNDERWATER = 2;

    @Override
    public MapCodec<FingerWeedsFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // 1. Start just above the water at the bottom of a fissure
        BlockPos start = new BlockPos(origin.getX(), generator.getSeaLevel(), origin.getZ());
        if (!level.getBlockState(start).isAir() || !level.getFluidState(start.below()).is(FluidTags.WATER)) {
            return false; // not in a fissure
        }

        // 2. Find the nearest wall
        Direction toWall = null;
        int distance = Integer.MAX_VALUE;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= maxReach && i < distance; i++) {
                BlockPos check = start.relative(direction, i);
                if (!level.getBlockState(check).isAir()) {
                    if (isWall(level, check, direction)) {
                        toWall = direction;
                        distance = i;
                    }
                    break;
                }
            }
        }
        if (toWall == null) {
            return false; // the middle of a wide gorge
        }
        Direction along = toWall.getClockWise();
        BlockState weeds = ModBlocks.FINGER_WEEDS.defaultBlockState().setValue(FingerWeedsBlock.FACING, toWall.getOpposite());

        // 3. Plan the climb, one row at a time, until the wall runs out at the top.
        // "anchor" is the middle of the column in the row below: the air or water right against the wall.
        BlockPos home = start.relative(toWall, distance - 1);
        BlockPos anchor = home.below(UNDERWATER + 1);
        List<BlockPos> rowBelow = null;
        int drift = random.nextInt(3) - 1; // -1, 0 or 1: which way the column is currently sweeping
        Map<BlockPos, BlockState> plan = new LinkedHashMap<>();
        for (int up = 0; up < MAX_HEIGHT; up++) {
            int width = 3 + random.nextInt(2);
            List<BlockPos> row = null;
            // Straight up first; if that doesn't join on (a gap or a bend in the wall), try one block either side
            for (int shift : new int[]{0, 1, -1}) {
                BlockPos rowAnchor = anchor.above().relative(along, shift);
                if (row == null && withinReach(rowAnchor, home, along, toWall)) {
                    List<BlockPos> tried = planRow(level, rowAnchor, along, toWall, width);
                    if (!tried.isEmpty() && (rowBelow == null || touches(tried, rowBelow))) {
                        row = tried;
                    }
                }
            }
            if (row == null) {
                break; // reached the top of the wall (or a dead end)
            }
            for (BlockPos spot : row) {
                BlockState here = level.getBlockState(spot);
                plan.put(spot, isWaterSource(here) ? weeds.setValue(FingerWeedsBlock.WATERLOGGED, true) : weeds);
            }
            rowBelow = row;
            anchor = row.getFirst(); // planRow puts the middle first
            // Now and then pick a new direction to sweep in, and move one step every other row
            if (random.nextInt(8) == 0) {
                drift = random.nextInt(3) - 1;
            }
            if (up % 2 == 1 && drift != 0) {
                BlockPos moved = anchor.relative(along, drift);
                if (withinReach(moved, home, along, toWall)) {
                    anchor = moved;
                } else {
                    drift = -drift;
                }
            }
        }

        // 4. Build it, if it all fits
        if (plan.isEmpty()) {
            return false;
        }
        for (BlockPos pos : plan.keySet()) {
            if (!canWrite(level, pos)) {
                return false;
            }
        }
        plan.forEach((pos, state) -> level.setBlock(pos, state, 2));
        return true;
    }

    /**
     * Plans one row: up to "width" blocks along the wall, from one block left of the anchor.
     * Starting from the middle lane, it adds lanes outward only while each one touches the
     * last, so the row is always joined up. The middle comes first in the list. Empty if
     * there's no wall at the middle lane.
     */
    private static List<BlockPos> planRow(WorldGenLevel level, BlockPos anchor, Direction along, Direction toWall, int width) {
        List<BlockPos> row = new ArrayList<>();
        BlockPos middle = findSpotAgainstWall(level, anchor, toWall);
        if (middle == null) {
            return row;
        }
        row.add(middle);
        BlockPos last = middle;
        for (int i = 1; i < width - 1; i++) { // to the right
            BlockPos spot = findSpotAgainstWall(level, last.relative(along), toWall);
            if (spot == null || !adjacent(spot, last)) {
                break;
            }
            row.add(spot);
            last = spot;
        }
        BlockPos left = findSpotAgainstWall(level, middle.relative(along, -1), toWall);
        if (left != null && adjacent(left, middle)) {
            row.add(left);
        }
        return row;
    }

    /** Touching: sharing a side, an edge or a corner. */
    private static boolean adjacent(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) <= 1 && Math.abs(a.getY() - b.getY()) <= 1 && Math.abs(a.getZ() - b.getZ()) <= 1;
    }

    private static boolean touches(List<BlockPos> row, List<BlockPos> rowBelow) {
        for (BlockPos a : row) {
            for (BlockPos b : rowBelow) {
                if (adjacent(a, b)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Walls aren't flat, so near the planned spot, slide toward or away from the wall to
     * the block of air (or water) that's right against it. Null if there's no wall here.
     */
    private static BlockPos findSpotAgainstWall(WorldGenLevel level, BlockPos planned, Direction toWall) {
        BlockPos spot = planned;
        int tries = 0;
        // Inside rock: back out toward the fissure
        while (!isOpen(level.getBlockState(spot))) {
            if (++tries > 3) {
                return null;
            }
            spot = spot.relative(toWall.getOpposite());
        }
        // In the open: move toward the wall until the next block is rock
        while (isOpen(level.getBlockState(spot.relative(toWall)))) {
            if (++tries > 4) {
                return null;
            }
            spot = spot.relative(toWall);
        }
        return isWall(level, spot.relative(toWall), toWall) ? spot.immutable() : null;
    }

    /** Air, water, or Finger Weeds already there: room for the column. */
    private static boolean isOpen(BlockState state) {
        return state.isAir() || isWaterSource(state) || state.is(ModBlocks.FINGER_WEEDS);
    }

    private static boolean isWaterSource(BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    /** Still close enough to where the column started, both along the wall and in or out from it. */
    private static boolean withinReach(BlockPos pos, BlockPos home, Direction along, Direction toWall) {
        int sideways = (pos.getX() - home.getX()) * along.getStepX() + (pos.getZ() - home.getZ()) * along.getStepZ();
        int inOut = (pos.getX() - home.getX()) * toWall.getStepX() + (pos.getZ() - home.getZ()) * toWall.getStepZ();
        return Math.abs(sideways) <= MAX_DRIFT && Math.abs(inOut) <= MAX_DRIFT;
    }

    /** During world generation, only blocks in the chunk being generated and the ones right around it may change. */
    private static boolean canWrite(WorldGenLevel level, BlockPos pos) {
        if (!(level instanceof WorldGenRegion region)) {
            return true; // placed some other way (a command): anywhere loaded is fine
        }
        return Math.abs(SectionPos.blockToSectionCoord(pos.getX()) - region.getCenter().x()) <= 1
                && Math.abs(SectionPos.blockToSectionCoord(pos.getZ()) - region.getCenter().z()) <= 1;
    }

    /** A block the weeds can hang on: solid on the side facing the fissure. */
    private static boolean isWall(WorldGenLevel level, BlockPos pos, Direction toWall) {
        return level.getBlockState(pos).isFaceSturdy(level, pos, toWall.getOpposite());
    }
}
