package io.github.thedarkhorse99.umbralworld.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public final class UmbralPortalForcer {
    private static final int SEARCH_RADIUS = 16;

    private UmbralPortalForcer() {
    }

    /** Builds a new 4x5 portal near origin. Returns the bottom-left portal block, or empty if impossible. */
    public static Optional<BlockPos> createPortal(ServerLevel level, BlockPos origin, Direction.Axis portalAxis) {
        loadChunksAround(level, origin, SEARCH_RADIUS);

        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, portalAxis);
        double closestFullDistanceSqr = -1.0;
        BlockPos closestFullPosition = null;
        double closestPartialDistanceSqr = -1.0;
        BlockPos closestPartialPosition = null;
        WorldBorder worldBorder = level.getWorldBorder();
        int maxPlaceableY = Math.min(level.getMaxY(), level.getMinY() + level.getLogicalHeight() - 1);
        BlockPos.MutableBlockPos mutable = origin.mutable();

        // 1. Search outward in a spiral for a good spot.
        for (BlockPos.MutableBlockPos columnPos : BlockPos.spiralAround(origin, SEARCH_RADIUS, Direction.EAST, Direction.SOUTH)) {
            int height = Math.min(maxPlaceableY, level.getHeight(Heightmap.Types.MOTION_BLOCKING, columnPos.getX(), columnPos.getZ()));
            if (!worldBorder.isWithinBounds(columnPos) || !worldBorder.isWithinBounds(columnPos.move(direction, 1))) {
                continue;
            }
            columnPos.move(direction.getOpposite(), 1);

            for (int y = height; y >= level.getMinY(); y--) {
                columnPos.setY(y);
                if (!canPortalReplaceBlock(level, columnPos)) {
                    continue;
                }
                int firstEmptyY = y;
                while (y > level.getMinY() && canPortalReplaceBlock(level, columnPos.move(Direction.DOWN))) {
                    y--;
                }
                int deltaY = firstEmptyY - y;
                if (y + 4 > maxPlaceableY || (deltaY > 0 && deltaY < 3)) {
                    continue;
                }
                columnPos.setY(y);
                if (!canHostFrame(level, columnPos, mutable, direction, 0)) {
                    continue;
                }

                double distance = origin.distSqr(columnPos);
                boolean roomOnBothSides = canHostFrame(level, columnPos, mutable, direction, -1)
                        && canHostFrame(level, columnPos, mutable, direction, 1);
                if (roomOnBothSides && (closestFullDistanceSqr == -1.0 || closestFullDistanceSqr > distance)) {
                    closestFullDistanceSqr = distance;
                    closestFullPosition = columnPos.immutable();
                }
                if (closestFullDistanceSqr == -1.0 && (closestPartialDistanceSqr == -1.0 || closestPartialDistanceSqr > distance)) {
                    closestPartialDistanceSqr = distance;
                    closestPartialPosition = columnPos.immutable();
                }
            }
        }

        if (closestFullDistanceSqr == -1.0 && closestPartialDistanceSqr != -1.0) {
            closestFullPosition = closestPartialPosition;
            closestFullDistanceSqr = closestPartialDistanceSqr;
        }

        // 2. Nowhere suitable: build a floating platform.
        if (closestFullDistanceSqr == -1.0) {
            int maxStartY = maxPlaceableY - 9;
            int minStartY = Math.max(level.getMinY() + 1, 70);
            if (maxStartY < minStartY) {
                return Optional.empty();
            }
            closestFullPosition = new BlockPos(
                    origin.getX() - direction.getStepX(),
                    Mth.clamp(origin.getY(), minStartY, maxStartY),
                    origin.getZ() - direction.getStepZ());
            closestFullPosition = worldBorder.clampToBounds(closestFullPosition);
            Direction clockWise = direction.getClockWise();
            for (int box = -1; box < 2; box++) {
                for (int width = 0; width < 2; width++) {
                    for (int h = -1; h < 3; h++) {
                        BlockState state = h < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        mutable.setWithOffset(closestFullPosition,
                                width * direction.getStepX() + box * clockWise.getStepX(), h,
                                width * direction.getStepZ() + box * clockWise.getStepZ());
                        level.setBlockAndUpdate(mutable, state);
                    }
                }
            }
        }

        // 3. Build the frame.
        BlockState frame = ModBlocks.SHADESTONE_BLOCK.defaultBlockState().setValue(ShadestoneBlock.PLACED, true);
        for (int width = -1; width < 3; width++) {
            for (int h = -1; h < 4; h++) {
                boolean isEdge = width == -1 || width == 2 || h == -1 || h == 3;
                if (isEdge) {
                    mutable.setWithOffset(closestFullPosition, width * direction.getStepX(), h, width * direction.getStepZ());
                    level.setBlockAndUpdate(mutable, frame);
                }
            }
        }

        // 4. Fill it with portal.
        BlockState portal = ModBlocks.UMBRAL_PORTAL.defaultBlockState().setValue(UmbralPortalBlock.AXIS, portalAxis);
        for (int width = 0; width < 2; width++) {
            for (int h = 0; h < 3; h++) {
                mutable.setWithOffset(closestFullPosition, width * direction.getStepX(), h, width * direction.getStepZ());
                level.setBlock(mutable, portal, 18);
            }
        }

        return Optional.of(closestFullPosition);
    }

    private static boolean canPortalReplaceBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    private static boolean canHostFrame(ServerLevel level, BlockPos origin, BlockPos.MutableBlockPos mutable,
                                        Direction direction, int offset) {
        Direction clockWise = direction.getClockWise();
        for (int width = -1; width < 3; width++) {
            for (int h = -1; h < 4; h++) {
                mutable.setWithOffset(origin,
                        direction.getStepX() * width + clockWise.getStepX() * offset, h,
                        direction.getStepZ() * width + clockWise.getStepZ() * offset);
                if (h < 0 && !level.getBlockState(mutable).isSolid()) {
                    return false;
                }
                if (h >= 0 && !canPortalReplaceBlock(level, mutable)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void loadChunksAround(ServerLevel level, BlockPos center, int radius) {
        for (int cx = (center.getX() - radius) >> 4; cx <= (center.getX() + radius) >> 4; cx++) {
            for (int cz = (center.getZ() - radius) >> 4; cz <= (center.getZ() + radius) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }
}