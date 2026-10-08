package io.github.thedarkhorse99.umbralworld.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import io.github.thedarkhorse99.umbralworld.UmbralWorld;
import io.github.thedarkhorse99.umbralworld.client.ModRenderPipelines;
import io.github.thedarkhorse99.umbralworld.world.ModDimensions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
    @Unique private static final float BLACK_SUN_SIZE = 30.0f; // same as vanilla's sun

    // ---- Vanilla's own fields and methods we need to use ----
    @Shadow @Final private TextureAtlas celestialsAtlas;
    @Shadow @Final private RenderSystem.AutoStorageIndexBuffer quadIndices;

    @Shadow
    private Matrix4f applyCelestialBodyTransform(PoseStack poseStack, float height, float scale) {
        throw new AssertionError();
    }

    @Shadow
    private static GpuBuffer buildCelestialQuad(String name, TextureAtlasSprite sprite) {
        throw new AssertionError();
    }

    // ---- Our own additions ----
    @Unique private @Nullable GpuBuffer umbral_world$blackSunBuffer;
    @Unique private boolean umbral_world$inUmbralWorld;

    // 1. When the sky renderer is built, also build the black sun's shape.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void umbral_world$buildBlackSun(TextureManager textureManager, AtlasManager atlasManager,
                                            RenderTarget renderTarget, CallbackInfo ci) {
        TextureAtlasSprite sprite = this.celestialsAtlas.getSprite(UmbralWorld.id("black_sun"));
        this.umbral_world$blackSunBuffer = buildCelestialQuad("Black sun quad", sprite);
    }

    // 2. Every frame, remember whether we're in the Umbral World.
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void umbral_world$checkDimension(ClientLevel level, float partialTicks, Camera camera,
                                             SkyRenderState state, CallbackInfo ci) {
        this.umbral_world$inUmbralWorld = level.dimension() == ModDimensions.UMBRAL_WORLD;
    }

    // 3. In the Umbral World, draw the black sun instead of the vanilla one.
    @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
    private void umbral_world$renderBlackSun(RenderPass renderPass, float rainBrightness,
                                             PoseStack poseStack, CallbackInfo ci) {
        GpuBuffer buffer = this.umbral_world$blackSunBuffer;
        if (!this.umbral_world$inUmbralWorld || buffer == null) {
            return; // anywhere else: let vanilla draw its normal sun
        }

        Matrix4f modelView = this.applyCelestialBodyTransform(poseStack, 100.0f, BLACK_SUN_SIZE);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(modelView, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

        renderPass.pushDebugGroup(() -> "Black sun");
        renderPass.setPipeline(RenderSystem.getCompiledPipeline(ModRenderPipelines.BLACK_SUN));
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", transforms);
        renderPass.setUniform("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
        renderPass.setVertexBuffer(0, buffer.slice());
        renderPass.setIndexBuffer(this.quadIndices.getBuffer(6), this.quadIndices.type());
        renderPass.drawIndexed(6, 1, 0, 0, 0);
        renderPass.popDebugGroup();

        ci.cancel(); // skip vanilla's sun
    }

    // 4. Clean up when the sky renderer is thrown away.
    @Inject(method = "close", at = @At("HEAD"))
    private void umbral_world$closeBlackSun(CallbackInfo ci) {
        if (this.umbral_world$blackSunBuffer != null) {
            this.umbral_world$blackSunBuffer.close();
            this.umbral_world$blackSunBuffer = null;
        }
    }
}