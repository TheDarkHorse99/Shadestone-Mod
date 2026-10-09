package io.github.thedarkhorse99.umbralworld.item;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import io.github.thedarkhorse99.umbralworld.effect.ModEffects;

public class ShadestoneBladeEvents {
	private static final int BLINDNESS_TICKS = 80;  // 4 seconds (20 ticks = 1 second)
	private static final int COOLDOWN_TICKS = 160;  // 8 seconds


	public static void initialize() {
		AttackEntityCallback.EVENT.register((player, level, hand, entity, _) -> {
			ItemStack stack = player.getItemInHand(hand);

			if (level instanceof ServerLevel
					&& !player.isSpectator()
					&& stack.is(ModItems.SHADESTONE_BLADE)
					&& entity instanceof Player target
					&& player.getAttackStrengthScale(0.5f) > 0.9f
					&& !player.getCooldowns().isOnCooldown(stack)) {
				target.addEffect(new MobEffectInstance(ModEffects.DEEP_BLINDNESS, BLINDNESS_TICKS));
				player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
			}

			return InteractionResult.PASS;
		});
	}
}
