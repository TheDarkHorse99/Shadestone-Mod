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
import net.minecraft.world.level.MoonPhase;
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
    // The twin black suns take turns: the purple one follows the vanilla sun's path and the red
    // one the moon's (always on the opposite side). Both paths are driven by the Umbral World's
    // timeline, data/umbral_world/timeline/twin_suns.json.
    @Unique private @Nullable GpuBuffer umbral_world$purpleSunBuffer;
    @Unique private @Nullable GpuBuffer umbral_world$redSunBuffer;
    @Unique private boolean umbral_world$inUmbralWorld;

    // 1. When the sky renderer is built, also build the two suns' shapes.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void umbral_world$buildBlackSuns(TextureManager textureManager, AtlasManager atlasManager,
                                             RenderTarget renderTarget, CallbackInfo ci) {
        this.umbral_world$purpleSunBuffer = buildCelestialQuad("Purple black sun quad",
                this.celestialsAtlas.getSprite(UmbralWorld.id("purple_black_sun")));
        this.umbral_world$redSunBuffer = buildCelestialQuad("Red black sun quad",
                this.celestialsAtlas.getSprite(UmbralWorld.id("red_black_sun")));
    }

    // 2. Every frame, remember whether we're in the Umbral World.
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void umbral_world$checkDimension(ClientLevel level, float partialTicks, Camera camera,
                                             SkyRenderState state, CallbackInfo ci) {
        this.umbral_world$inUmbralWorld = level.dimension() == ModDimensions.UMBRAL_WORLD;
    }

    // 3. In the Umbral World, draw the purple black sun instead of the vanilla sun...
    @Inject(method = "renderSun", at = @At("HEAD"), cancellable = true)
    private void umbral_world$renderPurpleSun(RenderPass renderPass, float rainBrightness,
                                              PoseStack poseStack, CallbackInfo ci) {
        if (this.umbral_world$inUmbralWorld && this.umbral_world$purpleSunBuffer != null) {
            umbral_world$drawBlackSun(renderPass, poseStack, this.umbral_world$purpleSunBuffer, "Purple black sun");
            ci.cancel(); // skip vanilla's sun
        }
    }

    // 4. ...and the red black sun instead of the moon (vanilla has already turned the sky to the moon's angle).
    @Inject(method = "renderMoon", at = @At("HEAD"), cancellable = true)
    private void umbral_world$renderRedSun(RenderPass renderPass, MoonPhase moonPhase, float rainBrightness,
                                           PoseStack poseStack, CallbackInfo ci) {
        if (this.umbral_world$inUmbralWorld && this.umbral_world$redSunBuffer != null) {
            umbral_world$drawBlackSun(renderPass, poseStack, this.umbral_world$redSunBuffer, "Red black sun");
            ci.cancel(); // skip vanilla's moon
        }
    }

    @Unique
    private void umbral_world$drawBlackSun(RenderPass renderPass, PoseStack poseStack, GpuBuffer buffer, String name) {
        Matrix4f modelView = this.applyCelestialBodyTransform(poseStack, 100.0f, BLACK_SUN_SIZE);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(modelView, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

        renderPass.pushDebugGroup(() -> name);
        renderPass.setPipeline(RenderSystem.getCompiledPipeline(ModRenderPipelines.BLACK_SUN));
        RenderSystem.bindDefaultUniforms(renderPass);
        renderPass.setUniform("DynamicTransforms", transforms);
        renderPass.setUniform("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
        renderPass.setVertexBuffer(0, buffer.slice());
        renderPass.setIndexBuffer(this.quadIndices.getBuffer(6), this.quadIndices.type());
        renderPass.drawIndexed(6, 1, 0, 0, 0);
        renderPass.popDebugGroup();
    }

    // 5. Clean up when the sky renderer is thrown away.
    @Inject(method = "close", at = @At("HEAD"))
    private void umbral_world$closeBlackSuns(CallbackInfo ci) {
        if (this.umbral_world$purpleSunBuffer != null) {
            this.umbral_world$purpleSunBuffer.close();
            this.umbral_world$purpleSunBuffer = null;
        }
        if (this.umbral_world$redSunBuffer != null) {
            this.umbral_world$redSunBuffer.close();
            this.umbral_world$redSunBuffer = null;
        }
    }
}
