package io.github.thedarkhorse99.umbralworld.block;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A Miasma plant that gives an effect to players standing near it, and shows
 * coloured specks so careful players can see the danger coming.
 * The effect itself is handed out by {@link MiasmaPlantAuras}.
 */
public interface AuraPlant {
    /** Blocks Miasma plants can grow on. Filled in by data/umbral_world/tags/block/supports_miasma_plants.json */
    TagKey<Block> SUPPORTS_MIASMA_PLANTS = TagKey.create(Registries.BLOCK, UmbralWorld.id("supports_miasma_plants"));

    /** 1 in this many display ticks makes a speck. Lower = more specks. */
    int PARTICLE_CHANCE = 3;

    Holder<MobEffect> auraEffect();

    /** Speck colour as 0xRRGGBB. */
    int auraColor();

    /** Call from animateTick: a coloured speck somewhere in the plant's space. */
    static void spawnAuraParticle(Level level, BlockPos pos, RandomSource random, int color) {
        if (random.nextInt(PARTICLE_CHANCE) != 0) {
            return;
        }
        level.addParticle(new DustParticleOptions(color, 0.8F),
                pos.getX() + 0.15 + random.nextDouble() * 0.7,
                pos.getY() + 0.1 + random.nextDouble() * 0.9,
                pos.getZ() + 0.15 + random.nextDouble() * 0.7,
                0.0, 0.02, 0.0);
    }
}
