package io.github.thedarkhorse99.umbralworld.block;

import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Faintly see-through planks. Built on vanilla's glass class so that two planks
 * side by side don't draw the face between them.
 */
public class GhostPlanksBlock extends TransparentBlock {
    public GhostPlanksBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
