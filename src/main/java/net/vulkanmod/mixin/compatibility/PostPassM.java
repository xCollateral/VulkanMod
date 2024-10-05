package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

@Mixin(PostPass.class)
public class PostPassM {

    @Shadow @Final public RenderTarget inTarget;

    @Shadow @Final public RenderTarget outTarget;

    @Shadow @Final private EffectInstance effect;

    @Shadow @Final private List<IntSupplier> auxAssets;

    @Shadow @Final private List<String> auxNames;

    @Shadow @Final private List<Integer> auxWidths;

    @Shadow @Final private List<Integer> auxHeights;

    @Shadow private Matrix4f shaderOrthoMatrix;

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void process(float f) {
        this.inTarget.unbindWrite();
        float g = (float)this.outTarget.width;
        float h = (float)this.outTarget.height;
        RenderSystem.viewport(0, 0, (int)g, (int)h);

        Objects.requireNonNull(this.inTarget);
        this.effect.setSampler("DiffuseSampler", this.inTarget::getColorTextureId);

        if(this.inTarget instanceof MainTarget)
            this.inTarget.bindRead();

        for(int i = 0; i < this.auxAssets.size(); ++i) {
            this.effect.setSampler(this.auxNames.get(i), this.auxAssets.get(i));
            this.effect.safeGetUniform("AuxSize" + i).set((float) this.auxWidths.get(i), (float) this.auxHeights.get(i));
        }

        this.effect.safeGetUniform("ProjMat").set(this.shaderOrthoMatrix);
        this.effect.safeGetUniform("InSize").set((float)this.inTarget.width, (float)this.inTarget.height);
        this.effect.safeGetUniform("OutSize").set(g, h);
        this.effect.safeGetUniform("Time").set(f);
        Minecraft minecraft = Minecraft.getInstance();
        this.effect.safeGetUniform("ScreenSize").set((float)minecraft.getWindow().getWidth(), (float)minecraft.getWindow().getHeight());

        this.outTarget.clear(Minecraft.ON_OSX);
        this.outTarget.bindWrite(false);

        VRenderSystem.disableCull();
        VRenderSystem.depthFunc(519);
        VRenderSystem.setPrimitiveTopologyGL(GL11.GL_TRIANGLES);

        Renderer.setInvertedViewport(0, 0, this.outTarget.width, this.outTarget.height);
        Renderer.resetScissor();

        this.effect.apply();

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.addVertex(0.0f, 0.0f, 500.0f);
        bufferBuilder.addVertex(g, 0.0f, 500.0f);
        bufferBuilder.addVertex(g, h, 500.0f);
        bufferBuilder.addVertex(0.0f, h, 500.0f);
        BufferUploader.draw(bufferBuilder.buildOrThrow());
        RenderSystem.depthFunc(515);

        this.effect.clear();
        this.outTarget.unbindWrite();
        this.inTarget.unbindRead();

        for (Object object : this.auxAssets) {
            if (object instanceof RenderTarget) {
                ((RenderTarget) object).unbindRead();
            }
        }

        VRenderSystem.enableCull();
    }
}
