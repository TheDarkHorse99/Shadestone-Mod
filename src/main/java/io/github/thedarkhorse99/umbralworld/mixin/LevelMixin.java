package io.github.thedarkhorse99.umbralworld.mixin;

import io.github.thedarkhorse99.umbralworld.block.UmbralSnuffing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tells UmbralSnuffing about every block change, so it can put out flames lit in the Umbral World. */
@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("RETURN"))
    private void umbral_world$snuffFlames(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            UmbralSnuffing.onBlockChanged((Level) (Object) this, pos, state);
        }
    }
}
