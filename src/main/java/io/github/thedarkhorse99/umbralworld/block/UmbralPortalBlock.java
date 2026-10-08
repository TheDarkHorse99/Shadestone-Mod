package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.world.ModDimensions;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

public class UmbralPortalBlock extends Block implements Portal {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final Map<Direction.Axis, VoxelShape> SHAPES =
            Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));
    private static final int LIGHT_CHECK_INTERVAL = 20; // ticks (1 second)
    private static final int EXIT_SEARCH_RADIUS = 32;

    public UmbralPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(AXIS));
    }

    // ---- Collapse: frame broken ----

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        Direction.Axis updateAxis = directionToNeighbour.getAxis();
        Direction.Axis axis = state.getValue(AXIS);
        boolean wrongAxis = axis != updateAxis && updateAxis.isHorizontal();

        if (wrongAxis || neighbourState.is(this) || UmbralPortalShape.findAnyShape(level, pos, axis).isComplete()) {
            return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
        }
        return Blocks.AIR.defaultBlockState();
    }

    // ---- Collapse: too much light ----

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, LIGHT_CHECK_INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos) > UmbralPortalShape.MAX_LIGHT) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.6f);
            level.sendParticles(ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.3, 0.5, 0.3, 0.01);
            level.removeBlock(pos, false);
        } else {
            level.scheduleTick(pos, this, LIGHT_CHECK_INTERVAL);
        }
    }

    // ---- Teleporting ----

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof Player ? 80 : 0;
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        ResourceKey<Level> targetKey = currentLevel.dimension() == ModDimensions.UMBRAL_WORLD
                ? Level.OVERWORLD
                : ModDimensions.UMBRAL_WORLD;
        ServerLevel targetLevel = currentLevel.getServer().getLevel(targetKey);
        if (targetLevel == null) {
            return null;
        }

        BlockPos approximateExit = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());

        // Is there already a portal near where we'd arrive?
        Optional<BlockPos> existing = findExitPortal(targetLevel, approximateExit);
        if (existing.isPresent()) {
            BlockPos found = existing.get();
            Direction.Axis axis = targetLevel.getBlockState(found).getValue(AXIS);
            UmbralPortalShape exitShape = UmbralPortalShape.findAnyShape(targetLevel, found, axis);
            if (exitShape.isValid()) {
                return new TeleportTransition(targetLevel, exitShape.getArrivalPoint(), Vec3.ZERO,
                        entity.getYRot(), entity.getXRot(),
                        TeleportTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(found)));
            }
        }

        // No portal found: build one
        if (entity.isSpectator()) {
            return null;
        }
        Direction.Axis sourceAxis = currentLevel.getBlockState(portalEntryPos).getOptionalValue(AXIS).orElse(Direction.Axis.X);
        Optional<BlockPos> created = UmbralPortalForcer.createPortal(targetLevel, approximateExit, sourceAxis);
        if (created.isEmpty()) {
            return null;
        }
        BlockPos bottomLeft = created.get();
        UmbralPortalShape newShape = UmbralPortalShape.findAnyShape(targetLevel, bottomLeft, sourceAxis);
        Vec3 arrival = newShape.isValid() ? newShape.getArrivalPoint() : Vec3.atBottomCenterOf(bottomLeft);
        return new TeleportTransition(targetLevel, arrival, Vec3.ZERO,
                entity.getYRot(), entity.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    private static Optional<BlockPos> findExitPortal(ServerLevel level, BlockPos center) {
        PoiManager poiManager = level.getPoiManager();
        poiManager.ensureLoadedAndValid(level, center, EXIT_SEARCH_RADIUS);
        return poiManager.getInSquare(type -> type.is(ModBlocks.UMBRAL_PORTAL_POI), center, EXIT_SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(pos -> level.getBlockState(pos).is(ModBlocks.UMBRAL_PORTAL))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(center)));
    }
}