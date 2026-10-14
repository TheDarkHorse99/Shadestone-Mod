package io.github.thedarkhorse99.umbralworld.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Places a small vein of Cinderite Ore in the bottom of a Chasm fissure, underwater:
 * in the floor, or low in a wall. Every ore block touches the water, so the vein can
 * be seen (and its bubbles too) by someone swimming along the bottom.
 *
 * JSON fields: "ore" (the block to place), "replaces" (the rock it may replace) and
 * "max_size" (most blocks in one vein; each vein is 1 to max_size).
 */
public record CinderiteOreFeature(Block ore, Block replaces, int maxSize) implements Feature {
    public static final MapCodec<CinderiteOreFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("ore").forGetter(CinderiteOreFeature::ore),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("replaces").forGetter(CinderiteOreFeature::replaces),
            Codec.intRange(1, 16).fieldOf("max_size").forGetter(CinderiteOreFeature::maxSize)
    ).apply(instance, CinderiteOreFeature::new));

    /** How far around the chosen spot (in blocks) the vein may be. */
    private static final int RADIUS = 3;

    @Override
    public MapCodec<CinderiteOreFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // 1. Only in a fissure: the top of the water must be here
        int waterTop = generator.getSeaLevel() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(origin.getX(), waterTop, origin.getZ());
        if (!level.getFluidState(pos).is(FluidTags.WATER)) {
            return false;
        }
        // 2. Sink to the floor
        while (pos.getY() > level.getMinY() && level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            pos.move(Direction.DOWN);
        }
        int floor = pos.getY() - 1;

        // 3. All the rock nearby that touches the water: the floor and the bottom of the walls
        List<BlockPos> wet = new ArrayList<>();
        for (BlockPos candidate : BlockPos.betweenClosed(
                origin.getX() - RADIUS, floor - 1, origin.getZ() - RADIUS,
                origin.getX() + RADIUS, waterTop, origin.getZ() + RADIUS)) {
            if (level.getBlockState(candidate).is(replaces) && touchesWater(level, candidate)) {
                wet.add(candidate.immutable());
            }
        }
        if (wet.isEmpty()) {
            return false;
        }

        // 4. Start at one of them and grow the vein through its wet neighbours
        int size = 1 + random.nextInt(maxSize);
        List<BlockPos> vein = new ArrayList<>();
        vein.add(wet.get(random.nextInt(wet.size())));
        while (vein.size() < size) {
            List<BlockPos> next = new ArrayList<>();
            for (BlockPos candidate : wet) {
                if (!vein.contains(candidate) && touchesAny(candidate, vein)) {
                    next.add(candidate);
                }
            }
            if (next.isEmpty()) {
                break;
            }
            vein.add(next.get(random.nextInt(next.size())));
        }
        for (BlockPos ore : vein) {
            level.setBlock(ore, this.ore.defaultBlockState(), 2);
        }
        return true;
    }

    private static boolean touchesWater(WorldGenLevel level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (level.getFluidState(pos.relative(side)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /** Next to (sharing a side or an edge with) any block already in the vein. */
    private static boolean touchesAny(BlockPos pos, List<BlockPos> vein) {
        for (BlockPos other : vein) {
            if (pos.distManhattan(other) <= 2 && Math.abs(pos.getX() - other.getX()) <= 1
                    && Math.abs(pos.getY() - other.getY()) <= 1 && Math.abs(pos.getZ() - other.getZ()) <= 1) {
                return true;
            }
        }
        return false;
    }
}
