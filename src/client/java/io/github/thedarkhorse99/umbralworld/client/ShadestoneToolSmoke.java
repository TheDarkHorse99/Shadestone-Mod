package io.github.thedarkhorse99.umbralworld.client;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Shadestone tools smoke like the ore and the block: a wisp now and then from the hand
 * holding one, and from one lying on the ground. Which items smoke is set by the item tag
 * data/umbral_world/tags/item/smoking_shadestone.json, so new Shadestone tools only need
 * adding there.
 *
 * Particles only exist on the player's own screen, so this runs on the client: every tick
 * it looks at the entities close enough to see.
 */
public final class ShadestoneToolSmoke {
    public static final TagKey<Item> SMOKING_SHADESTONE =
            TagKey.create(Registries.ITEM, UmbralWorld.id("smoking_shadestone"));

    /** Chance per tick of a wisp from a held tool (about 4 a second) and a dropped one. */
    private static final float HELD_CHANCE = 0.2f;
    private static final float DROPPED_CHANCE = 0.1f;
    /** Your own tool, seen in first person, smokes less so it doesn't get in the way. */
    private static final float FIRST_PERSON_CHANCE = 0.1f;
    /** Only bother with entities this close (in blocks): smoke further away is too small to see. */
    private static final double RANGE = 32.0;

    private ShadestoneToolSmoke() {
    }

    public static void initialize() {
        ClientTickEvents.END_LEVEL_TICK.register(ShadestoneToolSmoke::tick);
    }

    private static void tick(ClientLevel level) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        if (camera == null) {
            return;
        }
        RandomSource random = level.getRandom();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.distanceToSqr(camera) > RANGE * RANGE || entity.isInvisible()) {
                continue;
            }
            if (entity instanceof ItemEntity item) {
                if (item.getItem().is(SMOKING_SHADESTONE) && random.nextFloat() < DROPPED_CHANCE) {
                    puff(level, random, item.position().add(0.0, 0.25, 0.0));
                }
            } else if (entity instanceof LivingEntity living) {
                boolean firstPerson = living == camera && minecraft.options.getCameraType().isFirstPerson();
                HumanoidArm mainArm = living.getMainArm();
                smokeFromHand(level, random, living, living.getMainHandItem(), mainArm, firstPerson);
                smokeFromHand(level, random, living, living.getOffhandItem(), mainArm.getOpposite(), firstPerson);
            }
        }
    }

    private static void smokeFromHand(ClientLevel level, RandomSource random, LivingEntity holder, ItemStack stack,
                                      HumanoidArm arm, boolean firstPerson) {
        if (!stack.is(SMOKING_SHADESTONE) || random.nextFloat() >= (firstPerson ? FIRST_PERSON_CHANCE : HELD_CHANCE)) {
            return;
        }
        float side = arm == HumanoidArm.RIGHT ? 1.0f : -1.0f;
        Vec3 hand;
        if (firstPerson) {
            // Toward where the held item shows on screen: ahead, off to the side and down. Kept
            // 1.5 blocks out, because smoke right in front of the eyes looks like huge grey blobs.
            float yaw = holder.getYRot() * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
            hand = holder.getEyePosition()
                    .add(holder.getLookAngle().scale(1.3))
                    .add(right.scale(0.75 * side))
                    .add(0.0, -0.6, 0.0);
        } else {
            // Beside the body at hand height, turned with the body
            float yaw = holder.yBodyRot * Mth.DEG_TO_RAD;
            Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
            Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
            hand = holder.position()
                    .add(right.scale(0.4 * side))
                    .add(forward.scale(0.3))
                    .add(0.0, holder.getBbHeight() * 0.55, 0.0);
        }
        puff(level, random, hand);
    }

    /** One wisp, like ShadestoneSmoke's: vanilla smoke drifting slowly upward. */
    private static void puff(ClientLevel level, RandomSource random, Vec3 at) {
        level.addParticle(ParticleTypes.SMOKE,
                at.x + (random.nextDouble() - 0.5) * 0.2,
                at.y + (random.nextDouble() - 0.5) * 0.2,
                at.z + (random.nextDouble() - 0.5) * 0.2,
                0.0, 0.01, 0.0);
    }
}
