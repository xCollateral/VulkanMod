package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Shader;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Pipeline;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static com.mojang.blaze3d.systems.RenderSystem.*;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Shadow @Final private static ConcurrentLinkedQueue<RenderCall> recordingQueue;
    @Shadow @Final private static Tessellator RENDER_THREAD_TESSELATOR;
    @Shadow @Final private static int MINIMUM_ATLAS_TEXTURE_SIZE;
    @Shadow private static boolean isReplayingQueue;
    @Shadow private @Nullable static Thread gameThread;
    @Shadow private @Nullable static Thread renderThread;
    @Shadow private static int MAX_SUPPORTED_TEXTURE_SIZE;
    @Shadow private static boolean isInInit;
    @Shadow private static double lastDrawTime;
    @Shadow @Final private static IndexBuffer sharedSequential;
    @Shadow @Final private static IndexBuffer sharedSequentialQuad;
    @Shadow @Final private static IndexBuffer sharedSequentialLines;
    @Shadow private static Matrix4f projectionMatrix;
    @Shadow private static Matrix4f savedProjectionMatrix;
    @Shadow private static MatrixStack modelViewStack;
    @Shadow private static Matrix4f modelViewMatrix;
    @Shadow private static Matrix4f textureMatrix;
    @Shadow @Final private static int[] shaderTextures;
    @Shadow @Final private static float[] shaderColor;
    @Shadow private static float shaderFogStart;
    @Shadow private static float shaderFogEnd;
    @Shadow @Final private static float[] shaderFogColor;
    @Shadow @Final private static Vec3f[] shaderLightDirections;
    @Shadow private static float shaderGameTime;
    @Shadow private static float shaderLineWidth;
    @Shadow private @Nullable static Shader shader;

    @Shadow
    public static void assertOnGameThreadOrInit() {
    }

    /**
     * @author
     */
    @Overwrite
    public static void _setShaderTexture(int i, Identifier p_157181_) {
        if (i >= 0 && i < shaderTextures.length) {
            TextureManager texturemanager = MinecraftClient.getInstance().getTextureManager();
            AbstractTexture abstracttexture = texturemanager.getTexture(p_157181_);
            //abstracttexture.bindTexture();
            VTextureSelector.bindTexture(i, ((VAbstractTextureI)abstracttexture).getVulkanImage());

            //shaderTextures[i] = abstracttexture.getId();
        }

    }

    /**
     * @author
     */
    @Overwrite
    public static void initRenderer(int debugVerbosity, boolean debugSync) {
        VRenderSystem.initRenderer();
    }

    /**
     * @author
     */
    @Overwrite
    public static void setupDefaultState(int x, int y, int width, int height) { }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableColorLogicOp() {
        assertOnGameThread();
        //GlStateManager._enableColorLogicOp();
        //Vulkan
        VRenderSystem.enableColorLogicOp();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableColorLogicOp() {
        assertOnGameThread();
        //GlStateManager._disableColorLogicOp();
        //Vulkan
        VRenderSystem.disableColorLogicOp();
    }

    /**
     * @author
     */
    @Overwrite
    public static void logicOp(GlStateManager.LogicOp op) {
        assertOnGameThread();
        //GlStateManager._logicOp(op.value);
        //Vulkan
        VRenderSystem.logicOp(op);
    }

    /**
     * @author
     */
    @Overwrite
    public static void activeTexture(int texture) {}

    /**
     * @author
     */
    @Overwrite
    public static void glGenBuffers(Consumer<Integer> consumer) {}

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void glGenVertexArrays(Consumer<Integer> consumer) {}

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static int maxSupportedTextureSize() {
        //TODO: query vulkan for max texture size
        return VRenderSystem.maxSupportedTextureSize();
    }

    /**
     * @author
     */
    @Overwrite
    public static void clear(int mask, boolean getError) {
        VRenderSystem.clear(mask);
    }

    /**
     * @author
     */
    @Overwrite
    public static void flipFrame(long window) {
        org.lwjgl.glfw.GLFW.glfwPollEvents();
        RenderSystem.replayQueue();
        Tessellator.getInstance().getBuffer().clear();
    }

    /**
     * @author
     */
    @Overwrite
    public static void viewport(int x, int y, int width, int height) {}

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableDepthTest() {
        assertOnGameThread();
        //GlStateManager._disableDepthTest();
        VRenderSystem.disableDepthTest();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableDepthTest() {
        assertOnGameThreadOrInit();
        //GlStateManager._enableDepthTest();
        VRenderSystem.enableDepthTest();
    }

    /**
     * @author
     */
    @Overwrite
    public static void depthFunc(int p_69457_) {
        assertOnGameThread();
        //GlStateManager._depthFunc(p_69457_);
        VRenderSystem.depthFunc(p_69457_);
    }

    /**
     * @author
     */
    @Overwrite
    public static void depthMask(boolean p_69459_) {
        assertOnGameThread();
        //GlStateManager._depthMask(p_69459_);
        VRenderSystem.depthMask(p_69459_);
    }

    /**
     * @author
     */
    @Overwrite
    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableBlend() {
        Drawer.currentBlendState = Pipeline.DEFAULT_BLEND_STATE;
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableBlend() {
        Drawer.currentBlendState = Pipeline.NO_BLEND_STATE;
    }

    /**
     * @author
     */
    @Overwrite
    public static void blendFunc(GlStateManager.SrcFactor p_69409_, GlStateManager.DstFactor p_69410_) {

        Drawer.currentBlendState = new Pipeline.BlendState(p_69409_, p_69410_, p_69409_, p_69410_);
    }

    /**
     * @author
     */
    @Overwrite
    public static void blendFunc(int srcFactor, int dstFactor) {
        Drawer.currentBlendState = new Pipeline.BlendState(srcFactor, dstFactor, srcFactor, dstFactor);
    }

    /**
     * @author
     */
    @Overwrite
    public static void blendFuncSeparate(GlStateManager.SrcFactor p_69417_, GlStateManager.DstFactor p_69418_, GlStateManager.SrcFactor p_69419_, GlStateManager.DstFactor p_69420_) {
        Drawer.currentBlendState = new Pipeline.BlendState(p_69417_, p_69418_, p_69419_, p_69420_);
    }

    /**
     * @author
     */
    @Overwrite
    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
        Drawer.currentBlendState = new Pipeline.BlendState(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableCull() {
        assertOnGameThread();
        //GlStateManager._enableCull();
        //Vulkan
        VRenderSystem.enableCull();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableCull() {
        assertOnGameThread();
        //GlStateManager._disableCull();
        //Vulkan
        VRenderSystem.disableCull();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enablePolygonOffset() {
        assertOnGameThread();
//      GlStateManager._enablePolygonOffset();
        //Vulkan
        VRenderSystem.enablePolygonOffset();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disablePolygonOffset() {
        assertOnGameThread();
//      GlStateManager._disablePolygonOffset();
        //Vulkan
        VRenderSystem.disablePolygonOffset();
    }

    /**
     * @author
     */
    @Overwrite
    public static void polygonOffset(float p_69864_, float p_69865_) {
        assertOnGameThread();
//      GlStateManager._polygonOffset(p_69864_, p_69865_);
        //Vulkan
        VRenderSystem.polygonOffset(p_69864_, p_69865_);
    }

    /**
     * @author
     */
    @Overwrite
    public static void clearColor(float p_69425_, float p_69426_, float p_69427_, float p_69428_) {
        assertOnGameThreadOrInit();
//      GlStateManager._clearColor(p_69425_, p_69426_, p_69427_, p_69428_);
        //Vulkan
        VRenderSystem.clearColor(p_69425_, p_69426_, p_69427_, p_69428_);
    }

    /**
     * @author
     */
    @Overwrite
    public static void _setShaderLights(Vec3f p_157174_, Vec3f p_157175_) {
        shaderLightDirections[0] = p_157174_;
        shaderLightDirections[1] = p_157175_;

        //Vulkan
        VRenderSystem.lightDirection0.putFloat(0, p_157174_.getX());
        VRenderSystem.lightDirection0.putFloat(4, p_157174_.getY());
        VRenderSystem.lightDirection0.putFloat(8, p_157174_.getZ());

        VRenderSystem.lightDirection1.putFloat(0, p_157175_.getX());
        VRenderSystem.lightDirection1.putFloat(4, p_157175_.getY());
        VRenderSystem.lightDirection1.putFloat(8, p_157175_.getZ());
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    private static void _setShaderColor(float p_157160_, float p_157161_, float p_157162_, float p_157163_) {
        shaderColor[0] = p_157160_;
        shaderColor[1] = p_157161_;
        shaderColor[2] = p_157162_;
        shaderColor[3] = p_157163_;

        //Vulkan
        VRenderSystem.setShaderColor(p_157160_, p_157161_, p_157162_, p_157163_);
    }

    /**
     * @author
     */
    @Overwrite
    public static void renderCrosshair(int p_69882_) {
        assertOnGameThread();
        //GLX._renderCrosshair(p_69882_, true, true, true);
        //Vulkan
        VRenderSystem.renderCrosshair(p_69882_, true, true, true);
    }

    /**
     * @author
     */
    @Overwrite
    public static void setProjectionMatrix(Matrix4f p_157426_) {
        Matrix4f matrix4f = p_157426_.copy();
        if (!isOnRenderThread()) {
            recordRenderCall(() -> {
                projectionMatrix = matrix4f;
                //Vulkan
                VRenderSystem.applyProjectionMatrix(matrix4f);
                VRenderSystem.calculateMVP();
            });
        } else {
            projectionMatrix = matrix4f;
            //Vulkan
            VRenderSystem.applyProjectionMatrix(matrix4f);
            VRenderSystem.calculateMVP();
        }

    }

    /**
     * @author
     */
    @Overwrite
    public static void setTextureMatrix(Matrix4f matrix4f) {
        Matrix4f matrix4f2 = matrix4f.copy();
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                textureMatrix = matrix4f2;
                VRenderSystem.setTextureMatrix(matrix4f);
            });
        } else {
            textureMatrix = matrix4f2;
            VRenderSystem.setTextureMatrix(matrix4f);
        }
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void resetTextureMatrix() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> textureMatrix.loadIdentity());
        } else {
            textureMatrix.loadIdentity();
            VRenderSystem.setTextureMatrix(textureMatrix);
        }
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void applyModelViewMatrix() {
        Matrix4f matrix4f = modelViewStack.peek().getPositionMatrix().copy();
        if (!isOnRenderThread()) {
            recordRenderCall(() -> {
                modelViewMatrix = matrix4f;
                //Vulkan
                VRenderSystem.applyModelViewMatrix(matrix4f);
                VRenderSystem.calculateMVP();
            });
        } else {
            modelViewMatrix = matrix4f;
            //Vulkan
            VRenderSystem.applyModelViewMatrix(matrix4f);
            VRenderSystem.calculateMVP();
        }

    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    private static void _restoreProjectionMatrix() {
        projectionMatrix = savedProjectionMatrix;
        //Vulkan
        VRenderSystem.applyProjectionMatrix(projectionMatrix);
        VRenderSystem.calculateMVP();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void texParameter(int target, int pname, int param) {
        //TODO
    }
}
