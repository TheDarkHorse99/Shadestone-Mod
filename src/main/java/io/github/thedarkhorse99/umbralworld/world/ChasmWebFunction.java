package io.github.thedarkhorse99.umbralworld.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.util.Interval;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * The Chasm's web of fissures, as a terrain function ("type": "umbral_world:chasm_web" in JSON).
 *
 * The land is split into irregular cells, like cracked mud: scatter one point per square of
 * cell_size blocks, and every spot belongs to its nearest point. The fissures are the borders
 * between cells, so they always join up into one connected web with no dead ends.
 *
 * The value is how far a spot is from the nearest border, in blocks (0 right on a border).
 * The terrain JSON cuts a fissure wherever this is under half the fissure's width, so widths
 * are real block counts. Before measuring, each spot is nudged sideways by a smooth noise
 * (warp_amount blocks, changing over about warp_scale blocks) so the borders curve instead of
 * running dead straight.
 */
public record ChasmWebFunction(float cellSize, float warpAmount, float warpScale) implements DensityFunction {
	public static final MapCodec<ChasmWebFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			com.mojang.serialization.Codec.FLOAT.fieldOf("cell_size").forGetter(ChasmWebFunction::cellSize),
			com.mojang.serialization.Codec.FLOAT.fieldOf("warp_amount").forGetter(ChasmWebFunction::warpAmount),
			com.mojang.serialization.Codec.FLOAT.fieldOf("warp_scale").forGetter(ChasmWebFunction::warpScale)
	).apply(instance, ChasmWebFunction::new));

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		// Seeded from the world seed, so every world gets its own web
		RandomSource random = context.createRandom(UmbralWorld.id("chasm_web"));
		return new Sampler(this, random.nextLong(), new SimplexNoise(random), new SimplexNoise(random));
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		return this; // no inputs of its own
	}

	@Override
	public Interval range() {
		return Interval.of(0.0f, cellSize * 2.0f);
	}

	/** Only depends on x and z, so the game works it out once per column. */
	@Override
	public int domainAxes() {
		return AXIS_X | AXIS_Z;
	}

	@Override
	public MapCodec<ChasmWebFunction> codec() {
		return CODEC;
	}

	private record Sampler(ChasmWebFunction settings, long seed, SimplexNoise warpX, SimplexNoise warpZ) implements DensitySampler {
		@Override
		public void sampleVolume(SamplerContext context, DensityBuffer buffer, DensityVolume volume) {
			DensitySampler.sampleVolumeNaive(context, buffer, volume, this);
		}

		@Override
		public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
			float size = settings.cellSize();
			// Nudge the spot so the borders curve
			double x = blockX + settings.warpAmount() * warpX.get(blockX / settings.warpScale(), blockZ / settings.warpScale());
			double z = blockZ + settings.warpAmount() * warpZ.get(blockX / settings.warpScale(), blockZ / settings.warpScale());
			int cellX = (int) Math.floor(x / size);
			int cellZ = (int) Math.floor(z / size);

			// Collect the cell points around the spot (5 x 5 squares is enough to find the right borders)
			double[] pointsX = new double[25];
			double[] pointsZ = new double[25];
			int nearest = 0;
			double nearestDistance = Double.MAX_VALUE;
			int n = 0;
			for (int dx = -2; dx <= 2; dx++) {
				for (int dz = -2; dz <= 2; dz++) {
					long hash = hash(seed, cellX + dx, cellZ + dz);
					// Keep each point away from its square's edges so no cell gets too small
					pointsX[n] = (cellX + dx + 0.15 + 0.7 * unit(hash)) * size;
					pointsZ[n] = (cellZ + dz + 0.15 + 0.7 * unit(hash >>> 21 ^ hash * 31)) * size;
					double distance = square(pointsX[n] - x) + square(pointsZ[n] - z);
					if (distance < nearestDistance) {
						nearestDistance = distance;
						nearest = n;
					}
					n++;
				}
			}

			// Distance to the closest border: the halfway line between the nearest point and each other point
			double best = Double.MAX_VALUE;
			for (int i = 0; i < 25; i++) {
				if (i == nearest) {
					continue;
				}
				double towardX = pointsX[i] - pointsX[nearest];
				double towardZ = pointsZ[i] - pointsZ[nearest];
				double length = Math.sqrt(square(towardX) + square(towardZ));
				double midX = (pointsX[i] + pointsX[nearest]) / 2.0;
				double midZ = (pointsZ[i] + pointsZ[nearest]) / 2.0;
				double toBorder = ((midX - x) * towardX + (midZ - z) * towardZ) / length;
				best = Math.min(best, toBorder);
			}
			return (float) best;
		}

		private static double square(double value) {
			return value * value;
		}

		/** A well-mixed random number for one cell, the same every time for the same seed and cell. */
		private static long hash(long seed, int cellX, int cellZ) {
			long h = seed ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xC2B2AE3D27D4EB4FL);
			h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
			h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
			return h ^ (h >>> 31);
		}

		/** Turns a hash into a number from 0 to 1. */
		private static double unit(long hash) {
			return (hash >>> 11) * 0x1.0p-53;
		}
	}
}
