package io.github.thedarkhorse99.umbralworld.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;

public final class ModRenderPipelines {
    // A copy of vanilla's sun pipeline, but with normal (translucent) blending
    // instead of additive, so dark pixels can darken the sky.
    public static final RenderPipeline BLACK_SUN = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withLocation(UmbralWorld.id("pipeline/black_sun"))
                    .withVertexShader("core/position_tex")
                    .withFragmentShader("core/position_tex")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .build());

    // A copy of vanilla's glowing eyes pipeline (Spider, Enderman), but with our
    // own fragment shader that skips the fog, so the eyes shine through it.
    public static final RenderPipeline GLOWING_EYES = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_NO_LIGHTMAP_SNIPPET, RenderPipelines.EYES_SNIPPET)
                    .withLocation(UmbralWorld.id("pipeline/glowing_eyes"))
                    .withFragmentShader(UmbralWorld.id("core/glowing_eyes"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .build());

    private ModRenderPipelines() {
    }

    public static void initialize() {
    }
}