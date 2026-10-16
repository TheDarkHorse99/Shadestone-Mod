package io.github.thedarkhorse99.umbralworld.sound;

import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

/**
 * The mod's own sounds. They don't have sound files of their own: each one is listed in
 * assets/umbral_world/sounds.json as a random pick from vanilla sounds, so one block can sound
 * like two (Barrow Refuse plays gravel or mud each time).
 */
public final class ModSounds {
	public static final SoundEvent BARROW_REFUSE_BREAK = register("block.barrow_refuse.break");
	public static final SoundEvent BARROW_REFUSE_STEP = register("block.barrow_refuse.step");
	public static final SoundEvent BARROW_REFUSE_PLACE = register("block.barrow_refuse.place");
	public static final SoundEvent BARROW_REFUSE_HIT = register("block.barrow_refuse.hit");
	public static final SoundEvent BARROW_REFUSE_FALL = register("block.barrow_refuse.fall");
	/** Walking or climbing through Finger Weeds: wet, slimy squelches. */
	public static final SoundEvent FINGER_WEEDS_STEP = register("block.finger_weeds.step");

	/** Gravel and mud mixed: crunchy and squelchy. */
	public static final SoundType BARROW_REFUSE = new SoundType(1.0f, 1.0f,
			BARROW_REFUSE_BREAK, BARROW_REFUSE_STEP, BARROW_REFUSE_PLACE, BARROW_REFUSE_HIT, BARROW_REFUSE_FALL);
	/** Hanging roots, but slimy underfoot and when climbing (the game plays a climbable block's step sound as you climb). */
	public static final SoundType FINGER_WEEDS = new SoundType(1.0f, 1.0f,
			SoundEvents.HANGING_ROOTS_BREAK, FINGER_WEEDS_STEP, SoundEvents.HANGING_ROOTS_PLACE,
			SoundEvents.HANGING_ROOTS_HIT, SoundEvents.HANGING_ROOTS_FALL);

	private ModSounds() {
	}

	private static SoundEvent register(String name) {
		Identifier id = UmbralWorld.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** Loads the class, so the sounds are registered. */
	public static void initialize() {
	}
}
