package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.block.entity.ModBlockEntities;
import io.github.thedarkhorse99.umbralworld.block.entity.DarkFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class DarkFurnaceBlock extends AbstractFurnaceBlock {
    public DarkFurnaceBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DarkFurnaceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null; // all the cooking happens on the server
        }
        return createTickerHelper(type, ModBlockEntities.DARK_FURNACE,
                (_, pos, tickState, furnace) -> DarkFurnaceBlockEntity.serverTick(serverLevel, pos, tickState, furnace));
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof DarkFurnaceBlockEntity furnace) {
            player.openMenu(furnace);
        }
    }

    /** Client-side effects while cooking: smoke at the mouth and a low, slow crackle. No flames. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 0.5f, false);
        }
        Direction facing = state.getValue(FACING);
        double sideways = random.nextDouble() * 0.6 - 0.3;
        double dx = facing.getAxis() == Direction.Axis.X ? facing.getStepX() * 0.52 : sideways;
        double dy = random.nextDouble() * 6.0 / 16.0;
        double dz = facing.getAxis() == Direction.Axis.Z ? facing.getStepZ() * 0.52 : sideways;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
    }
}