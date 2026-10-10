package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** A rigid, poisonous bush: slows anything walking through it, and pricks and poisons it while it keeps moving. */
public class GraveThornsBlock extends GravePlantBlock {
    /** Defined in data/umbral_world/damage_type/grave_thorns.json, which gives thorn deaths their own message. */
    public static final ResourceKey<DamageType> DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, UmbralWorld.id("grave_thorns"));

    /** Movement multiplier while inside (vanilla sweet berry bush values: 80% sideways, 75% up/down). */
    private static final Vec3 SLOWDOWN = new Vec3(0.8, 0.75, 0.8);
    /** Moving slower than this (blocks per tick) doesn't hurt, so standing still is safe. */
    private static final double HURT_SPEED_THRESHOLD = 0.003;
    private static final float DAMAGE = 1.0f;       // half a heart per prick, like a sweet berry bush
    private static final int POISON_TICKS = 60;     // 3 seconds of Poison I (about half a heart to one heart)

    public GraveThornsBlock(BlockBehaviour.Properties properties) {
        super(Block.column(14.0, 0.0, 13.0), properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(entity instanceof LivingEntity living)) {
            return; // dropped items, arrows, boats etc. pass through untouched
        }
        living.makeStuckInBlock(state, SLOWDOWN);

        if (level instanceof ServerLevel serverLevel) {
            Vec3 movement = living.isClientAuthoritative()
                    ? living.getKnownMovement()
                    : living.oldPosition().subtract(living.position());
            if (Math.abs(movement.x()) >= HURT_SPEED_THRESHOLD || Math.abs(movement.z()) >= HURT_SPEED_THRESHOLD) {
                boolean pricked = living.hurtServer(serverLevel, level.damageSources().source(DAMAGE_TYPE), DAMAGE);
                if (pricked) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS));
                }
            }
        }
    }
}