package io.github.thedarkhorse99.umbralworld.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

public class UmbralPortalShape {
    private static final int MIN_WIDTH = 2;
    private static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 21;
    public static final int MAX_LIGHT = 4;

    private final Direction.Axis axis;
    private final Direction rightDir;
    private final BlockPos bottomLeft;
    private final int width;
    private final int height;
    private final int portalBlockCount;

    private UmbralPortalShape(Direction.Axis axis, Direction rightDir, BlockPos bottomLeft,
                              int width, int height, int portalBlockCount) {
        this.axis = axis;
        this.rightDir = rightDir;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
        this.portalBlockCount = portalBlockCount;
    }

    // ---- Finding a frame ----

    public static Optional<UmbralPortalShape> findEmptyShape(BlockGetter level, BlockPos pos) {
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            UmbralPortalShape shape = findAnyShape(level, pos, axis);
            if (shape.isValid() && shape.portalBlockCount == 0) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }

    public static UmbralPortalShape findAnyShape(BlockGetter level, BlockPos pos, Direction.Axis axis) {
        Direction rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bottomLeft = calculateBottomLeft(level, rightDir, pos);
        if (bottomLeft == null) {
            return new UmbralPortalShape(axis, rightDir, pos, 0, 0, 0);
        }
        int width = calculateWidth(level, bottomLeft, rightDir);
        if (width == 0) {
            return new UmbralPortalShape(axis, rightDir, bottomLeft, 0, 0, 0);
        }
        int height = calculateHeight(level, bottomLeft, rightDir, width);
        int portalBlocks = height == 0 ? 0 : countPortalBlocks(level, bottomLeft, rightDir, width, height);
        return new UmbralPortalShape(axis, rightDir, bottomLeft, width, height, portalBlocks);
    }

    private static @Nullable BlockPos calculateBottomLeft(BlockGetter level, Direction rightDir, BlockPos pos) {
        int minY = Math.max(level.getMinY(), pos.getY() - MAX_HEIGHT);
        while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }
        Direction leftDir = rightDir.getOpposite();
        int edge = distanceUntilEdgeAboveFrame(level, pos, leftDir) - 1;
        if (edge < 0) {
            return null;
        }
        return pos.relative(leftDir, edge);
    }

    private static int calculateWidth(BlockGetter level, BlockPos bottomLeft, Direction rightDir) {
        int width = distanceUntilEdgeAboveFrame(level, bottomLeft, rightDir);
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            return 0;
        }
        return width;
    }

    private static int distanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction direction) {
        for (int i = 0; i <= MAX_WIDTH; i++) {
            BlockPos checkPos = pos.relative(direction, i);
            BlockState state = level.getBlockState(checkPos);
            if (!isEmpty(state)) {
                return isFrame(state) ? i : 0;
            }
            if (!isFrame(level.getBlockState(checkPos.below()))) {
                return 0;
            }
        }
        return 0;
    }

    private static int calculateHeight(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width) {
        int height = distanceUntilTop(level, bottomLeft, rightDir, width);
        if (height < MIN_HEIGHT || height > MAX_HEIGHT || !hasTopFrame(level, bottomLeft, rightDir, width, height)) {
            return 0;
        }
        return height;
    }

    private static int distanceUntilTop(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width) {
        for (int h = 0; h < MAX_HEIGHT; h++) {
            BlockPos row = bottomLeft.above(h);
            if (!isFrame(level.getBlockState(row.relative(rightDir.getOpposite())))) {
                return h;
            }
            if (!isFrame(level.getBlockState(row.relative(rightDir, width)))) {
                return h;
            }
            for (int i = 0; i < width; i++) {
                if (!isEmpty(level.getBlockState(row.relative(rightDir, i)))) {
                    return h;
                }
            }
        }
        return MAX_HEIGHT;
    }

    private static boolean hasTopFrame(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width, int height) {
        for (int i = 0; i < width; i++) {
            if (!isFrame(level.getBlockState(bottomLeft.above(height).relative(rightDir, i)))) {
                return false;
            }
        }
        return true;
    }

    private static int countPortalBlocks(BlockGetter level, BlockPos bottomLeft, Direction rightDir, int width, int height) {
        int count = 0;
        for (int h = 0; h < height; h++) {
            for (int i = 0; i < width; i++) {
                if (level.getBlockState(bottomLeft.above(h).relative(rightDir, i)).is(ModBlocks.UMBRAL_PORTAL)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(ModBlocks.UMBRAL_PORTAL);
    }

    private static boolean isFrame(BlockState state) {
        return state.is(ModBlocks.SHADESTONE_BLOCK) && state.getValue(ShadestoneBlock.PLACED);
    }

    // ---- Using a found frame ----

    public boolean isValid() {
        return width >= MIN_WIDTH && width <= MAX_WIDTH && height >= MIN_HEIGHT && height <= MAX_HEIGHT;
    }

    public boolean isComplete() {
        return isValid() && portalBlockCount == width * height;
    }

    public int getBrightestLight(Level level) {
        int brightest = 0;
        for (BlockPos pos : interior()) {
            brightest = Math.max(brightest, level.getMaxLocalRawBrightness(pos));
        }
        return brightest;
    }

    public void createPortalBlocks(LevelAccessor level) {
        BlockState portalState = ModBlocks.UMBRAL_PORTAL.defaultBlockState().setValue(UmbralPortalBlock.AXIS, axis);
        for (BlockPos pos : interior()) {
            level.setBlock(pos, portalState, 18);
        }
    }

    /** The frame block directly under the opening's bottom-left corner. Always a placed Shadestone. */
    public BlockPos getAnchor() {
        return bottomLeft.below();
    }

    public BlockPos getCenter() {
        return bottomLeft.above(height / 2).relative(rightDir, width / 2);
    }

    public Vec3 getArrivalPoint() {
        Vec3 first = Vec3.atBottomCenterOf(bottomLeft);
        Vec3 last = Vec3.atBottomCenterOf(bottomLeft.relative(rightDir, width - 1));
        return first.add(last).scale(0.5);
    }

    private Iterable<BlockPos> interior() {
        return BlockPos.betweenClosed(bottomLeft, bottomLeft.above(height - 1).relative(rightDir, width - 1));
    }
}