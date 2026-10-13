package io.github.thedarkhorse99.umbralworld.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.thedarkhorse99.umbralworld.item.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla still draws armor on invisible players. The Wight's Shroud turns invisible
 * along with its wearer, so it is skipped while they are invisible.
 */
@NullMarked
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {
    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void umbral_world$hideShroud(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack,
                                         EquipmentSlot slot, int light, HumanoidRenderState state, CallbackInfo ci) {
        if (state.isInvisible && stack.is(ModItems.WIGHTS_SHROUD)) {
            ci.cancel();
        }
    }
}
