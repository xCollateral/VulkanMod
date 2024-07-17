package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.UniformState;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;

import static com.mojang.blaze3d.systems.RenderSystem.*;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Shadow private static Matrix4f projectionMatrix;
    @Shadow private static Matrix4f savedProjectionMatrix;
    @Shadow @Final private static PoseStack modelViewStack;
    @Shadow private static Matrix4f modelViewMatrix;
    @Shadow private static Matrix4f textureMatrix;
    @Shadow @Final private static int[] shaderTextures;
    @Shadow @Final private static float[] shaderColor;
    @Shadow @Final private static final Vector3f[] shaderLightDirections = {new Vector3f(), new Vector3f()};

    @Shadow
    public static void assertOnGameThreadOrInit() {
    }

    @Shadow @Final private static float[] shaderFogColor;

    @Shadow private static @Nullable Thread renderThread;

    @Shadow private static float shaderFogStart;
    @Shadow private static float shaderFogEnd;

    /**
     * @author
     */
    //TODO: May have potential for texture selection...
    @Overwrite
    public static void _setShaderTexture(int i, ResourceLocation location) {
        if (i >= 0 && i < shaderTextures.length) {
            //TODO: Replace w/ DescriptorAbstraction Array for Faster lookups
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            AbstractTexture abstractTexture = textureManager.getTexture(location);
            VTextureSelector.bindTexture(i, ((VAbstractTextureI)abstractTexture).getVulkanImage());

            shaderTextures[i] = abstractTexture.getId();
        }

    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _setShaderTexture(int i, int id) {
        if (i >= 0 && i < VTextureSelector.SIZE) {
            GlTexture glTexture = GlTexture.getTexture(id);
            VulkanImage vulkanImage = glTexture != null ? glTexture.getVulkanImage() : null;

            if(vulkanImage == null)
                return;

            final int id1 = glTexture.getId();
            VTextureSelector.bindTexture(i, vulkanImage);
            shaderTextures[i] = id1;
        }

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
        assertOnGameThread();
        VRenderSystem.enableColorLogicOp();
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    private static void _setShaderFogStart(float f) {

        if(f!=Float.MAX_VALUE && shaderFogStart != f)
        {

            UniformState.FogStart.getMappedBufferPtr().putFloat(0, f);
            UniformState.FogStart.setUpdateState(true);
        }
        shaderFogStart = f;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite(remap = false)
    private static void _setShaderFogEnd(float f) {

        if(f!=Float.MAX_VALUE && shaderFogEnd != f)
        {
            UniformState.FogEnd.getMappedBufferPtr().putFloat(0, f);
            UniformState.FogEnd.setUpdateState(true);
        }
        shaderFogEnd = f;
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableColorLogicOp() {
        assertOnGameThread();
        VRenderSystem.disableColorLogicOp();
    }

    /**
     * @author
     */
    @Overwrite
    public static void logicOp(GlStateManager.LogicOp op) {
        assertOnGameThread();
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
        assertOnGameThreadOrInit();
        VRenderSystem.setClearColor(r, g, b, a);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void clearDepth(double d) {
        VRenderSystem.clearDepth(d);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void flipFrame(long window) {
        org.lwjgl.glfw.GLFW.glfwPollEvents();
        RenderSystem.replayQueue();
        Tesselator.getInstance().getBuilder().clear();
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
        VRenderSystem.enableDepthTest();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void depthFunc(int i) {
        assertOnGameThread();
        VRenderSystem.depthFunc(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void depthMask(boolean b) {
        assertOnGameThread();
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
        assertOnGameThread();
        VRenderSystem.enableCull();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disableCull() {
        assertOnGameThread();
        VRenderSystem.disableCull();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void polygonMode(final int i, final int j) {
        assertOnGameThread();
        VRenderSystem.setPolygonModeGL(i);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void enablePolygonOffset() {
        assertOnGameThread();
        VRenderSystem.enablePolygonOffset();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void disablePolygonOffset() {
        assertOnGameThread();
        VRenderSystem.disablePolygonOffset();
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void polygonOffset(float p_69864_, float p_69865_) {
        assertOnGameThread();
        VRenderSystem.polygonOffset(p_69864_, p_69865_);
    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    public static void _setShaderLights(Vector3f p_157174_, Vector3f p_157175_) {

        if(shaderLightDirections[0].hashCode() != p_157174_.hashCode() && shaderLightDirections[1].hashCode() != p_157175_.hashCode())
        {

//            try(MemoryStack stack = stackPush()) {
//                ByteBuffer byteBuffer = stack.malloc(32);
//
//                p_157174_.get(0, byteBuffer);
//                p_157175_.get(16, byteBuffer);
//
//                vkCmdPushConstants(Renderer.getCommandBuffer(), Renderer.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, byteBuffer);
//
//            }
            (shaderLightDirections[0] = p_157174_).getToAddress(UniformState.Light0_Direction.ptr());
            (shaderLightDirections[1] = p_157175_).getToAddress(UniformState.Light1_Direction.ptr());
            UniformState.Light0_Direction.needsUpdate(p_157174_.hashCode());
            UniformState.Light1_Direction.needsUpdate(p_157175_.hashCode());
        }

    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    private static void _setShaderColor(float r, float g, float b, float a) {
        if(extracted(shaderColor, r, g, b, a))
        {
            shaderColor[0] = r;
            shaderColor[1] = g;
            shaderColor[2] = b;
            shaderColor[3] = a;

            VRenderSystem.setShaderColor(r, g, b, a);
        }

    }

    /**
     * @author
     */
    @Overwrite(remap = false)
    private static void _setShaderFogColor(float f, float g, float h, float i) {
        if(extracted(shaderFogColor, f, g, h, i))
        {
            shaderFogColor[0] = f;
            shaderFogColor[1] = g;
            shaderFogColor[2] = h;
            shaderFogColor[3] = i;
            VRenderSystem.setShaderFogColor(f, g, h, i);
        }
    }

    @Unique
    private static boolean extracted(float[] shaderFogColor, float f, float g, float h, float i) {
       return shaderFogColor[0] != f || shaderFogColor[1] != g || shaderFogColor[2] != h || shaderFogColor[3] != i;
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
        Matrix4f matrix4f = new Matrix4f(modelViewStack.last().pose());
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
