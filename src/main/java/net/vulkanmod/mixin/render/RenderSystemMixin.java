package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

import static com.mojang.blaze3d.systems.RenderSystem.*;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Shadow private static Matrix4f projectionMatrix;
    @Shadow private static Matrix4f savedProjectionMatrix;
    @Shadow @Final private static Matrix4fStack modelViewStack;
    @Shadow private static Matrix4f modelViewMatrix;
    @Shadow private static Matrix4f textureMatrix;
    @Shadow @Final private static int[] shaderTextures;
    @Shadow @Final private static float[] shaderColor;
    @Shadow @Final private static Vector3f[] shaderLightDirections;

    @Shadow @Final private static float[] shaderFogColor;

    @Shadow private static @Nullable Thread renderThread;

    @Shadow
    public static void assertOnRenderThread() {
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void initRenderer(int debugVerbosity, boolean debugSync) {
        VRenderSystem.initRenderer();

        renderThread.setPriority(Thread.NORM_PRIORITY + 2);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setupDefaultState(int x, int y, int width, int height) { }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableColorLogicOp() {
        assertOnRenderThread();
        VRenderSystem.enableColorLogicOp();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableColorLogicOp() {
        assertOnRenderThread();
        VRenderSystem.disableColorLogicOp();
    }

    /**
     * @author
     */
    @Overwrite
    public static void logicOp(GlStateManager.LogicOp op) {
        assertOnRenderThread();
        VRenderSystem.logicOp(op);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void activeTexture(int texture) {
        GlTexture.activeTexture(texture);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
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
        return VRenderSystem.maxSupportedTextureSize();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void clear(int mask, boolean getError) {
        VRenderSystem.clear(mask);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void clearColor(float r, float g, float b, float a) {
        VRenderSystem.setClearColor(r, g, b, a);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void clearDepth(double d) {
        VRenderSystem.clearDepth(d);
    }

    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
    private static void removeSwapBuffers(long window) {
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void viewport(int x, int y, int width, int height) {
        Renderer.setViewport(x, y, width, height);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableScissor(int x, int y, int width, int height) {
        Renderer.setScissor(x, y, width, height);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableScissor() {
        Renderer.resetScissor();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableDepthTest() {
        assertOnRenderThread();
        //GlStateManager._disableDepthTest();
        VRenderSystem.disableDepthTest();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableDepthTest() {
        assertOnRenderThreadOrInit();
        VRenderSystem.enableDepthTest();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void depthFunc(int i) {
        assertOnRenderThread();
        VRenderSystem.depthFunc(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void depthMask(boolean b) {
        assertOnRenderThread();
        VRenderSystem.depthMask(b);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void blendEquation(int i) {
        assertOnRenderThread();
        //TODO
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableBlend() {
        VRenderSystem.enableBlend();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableBlend() {
        VRenderSystem.disableBlend();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void blendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
        VRenderSystem.blendFunc(sourceFactor, destFactor);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void blendFunc(int srcFactor, int dstFactor) {
        VRenderSystem.blendFunc(srcFactor, dstFactor);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void blendFuncSeparate(GlStateManager.SourceFactor p_69417_, GlStateManager.DestFactor p_69418_, GlStateManager.SourceFactor p_69419_, GlStateManager.DestFactor p_69420_) {
        VRenderSystem.blendFuncSeparate(p_69417_, p_69418_, p_69419_, p_69420_);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
        VRenderSystem.blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enableCull() {
        assertOnRenderThread();
        VRenderSystem.enableCull();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableCull() {
        assertOnRenderThread();
        VRenderSystem.disableCull();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void polygonMode(final int i, final int j) {
        assertOnRenderThread();
        VRenderSystem.setPolygonModeGL(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enablePolygonOffset() {
        assertOnRenderThread();
        VRenderSystem.enablePolygonOffset();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disablePolygonOffset() {
        assertOnRenderThread();
        VRenderSystem.disablePolygonOffset();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void polygonOffset(float p_69864_, float p_69865_) {
        assertOnRenderThread();
        VRenderSystem.polygonOffset(p_69864_, p_69865_);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setShaderLights(Vector3f dir0, Vector3f dir1) {
        shaderLightDirections[0] = dir0;
        shaderLightDirections[1] = dir1;

        VRenderSystem.lightDirection0.buffer.putFloat(0, dir0.x());
        VRenderSystem.lightDirection0.buffer.putFloat(4, dir0.y());
        VRenderSystem.lightDirection0.buffer.putFloat(8, dir0.z());

        VRenderSystem.lightDirection1.buffer.putFloat(0, dir1.x());
        VRenderSystem.lightDirection1.buffer.putFloat(4, dir1.y());
        VRenderSystem.lightDirection1.buffer.putFloat(8, dir1.z());
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    private static void _setShaderColor(float r, float g, float b, float a) {
        shaderColor[0] = r;
        shaderColor[1] = g;
        shaderColor[2] = b;
        shaderColor[3] = a;

        VRenderSystem.setShaderColor(r, g, b, a);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setShaderFogColor(float f, float g, float h, float i) {
        shaderFogColor[0] = f;
        shaderFogColor[1] = g;
        shaderFogColor[2] = h;
        shaderFogColor[3] = i;

        VRenderSystem.setShaderFogColor(f, g, h, i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setProjectionMatrix(Matrix4f projectionMatrix, VertexSorting vertexSorting) {
        Matrix4f matrix4f = new Matrix4f(projectionMatrix);
        if (!isOnRenderThread()) {
            recordRenderCall(() -> {
                RenderSystemMixin.projectionMatrix = matrix4f;

                VRenderSystem.applyProjectionMatrix(matrix4f);
                VRenderSystem.calculateMVP();
            });
        } else {
            RenderSystemMixin.projectionMatrix = matrix4f;

            VRenderSystem.applyProjectionMatrix(matrix4f);
            VRenderSystem.calculateMVP();
        }

    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void setTextureMatrix(Matrix4f matrix4f) {
        Matrix4f matrix4f2 = new Matrix4f(matrix4f);
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
            RenderSystem.recordRenderCall(() -> textureMatrix.identity());
        } else {
            textureMatrix.identity();
            VRenderSystem.setTextureMatrix(textureMatrix);
        }
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void applyModelViewMatrix() {
        Matrix4f matrix4f = new Matrix4f(modelViewStack);
        if (!isOnRenderThread()) {
            recordRenderCall(() -> {
                modelViewMatrix = matrix4f;
                //Vulkan
                VRenderSystem.applyModelViewMatrix(matrix4f);
                VRenderSystem.calculateMVP();
            });
        } else {
            modelViewMatrix = matrix4f;

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

        VRenderSystem.applyProjectionMatrix(projectionMatrix);
        VRenderSystem.calculateMVP();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void texParameter(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }
}
