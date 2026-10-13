package io.github.thedarkhorse99.umbralworld.effect;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * You step halfway into the shadow world, like Frodo with the Ring:
 * - you hit harder and move faster (the attribute changes below)
 * - your screen turns washed-out and purple, and hostile mobs you can see get an
 *   outline (client side: NightstalkerVision)
 * - but hostile mobs notice you from further away (mixin/LivingEntityMixin)
 */
public class NightstalkerEffect extends MobEffect {
    /** Extra attack damage. Strength I gives +3. */
    public static final double ATTACK_BONUS = 2.0;
    /** Extra speed: 0.15 = 15% faster. Speed I gives 20%. */
    public static final double SPEED_BONUS = 0.15;
    /** Hostile mobs spot you from this many times further than usual. */
    public static final double DETECTION_MULTIPLIER = 1.5;

    public NightstalkerEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x5B2C83); // deep purple: the potion and swirl colour
        addAttributeModifier(Attributes.ATTACK_DAMAGE, UmbralWorld.id("effect.nightstalker"),
                ATTACK_BONUS, AttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, UmbralWorld.id("effect.nightstalker"),
                SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
