package io.github.thedarkhorse99.umbralworld.client.mixin;

import io.github.thedarkhorse99.umbralworld.client.NightstalkerVision;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives enemies the Glowing-style outline on a Nightstalker's screen. */
@NullMarked
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void umbral_world$nightstalkerOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (NightstalkerVision.shouldOutline(entity)) {
            cir.setReturnValue(true);
        }
    }
}
