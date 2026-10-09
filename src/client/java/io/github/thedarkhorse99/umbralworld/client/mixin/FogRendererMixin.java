package io.github.thedarkhorse99.umbralworld.client.mixin;

import io.github.thedarkhorse99.umbralworld.client.DeepBlindnessFogEnvironment;
import java.util.List;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Shadow @Final private static List<FogEnvironment> FOG_ENVIRONMENTS;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void umbral_world$addDeepBlindnessFog(CallbackInfo ci) {
        // Slot 2 = just before vanilla Blindness, so ours wins if both apply
        FOG_ENVIRONMENTS.add(2, new DeepBlindnessFogEnvironment());
    }
}