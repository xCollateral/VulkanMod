package net.vulkanmod.vulkan;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.shader.UniformState;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static net.vulkanmod.vulkan.shader.UniformState.MVP;

public abstract class VRenderSystem {
    private static long window;

    public static boolean depthTest = true;
    public static boolean depthMask = true;
    public static int depthFun = 515;

    public static int colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);

    public static boolean cull = true;

    public static boolean logicOp = false;
    public static int logicOpFun = 0;
    public static boolean useLines = false;

    public static final float clearDepth = 1.0f;
    public static FloatBuffer clearColor = MemoryUtil.memCallocFloat(4);

    public static float alphaCutout = 0.0f;

    static
    {
        //Set shaderColor to White first (Fixes Mojang splash visibility)
        ColorUtil.setRGBA_Buffer(UniformState.ColorModulator.getMappedBufferPtr(), 1, 1, 1, 1);
    }

    private static final float[] depthBias = new float[2];

    public static void initRenderer()
    {
        RenderSystem.assertInInitPhase();

        Vulkan.initVulkan(window);
    }

    public static MappedBuffer getScreenSize() {
        updateScreenSize();
        return UniformState.ScreenSize.getMappedBufferPtr();
    }

    public static void updateScreenSize() {
        Window window = Minecraft.getInstance().getWindow();

        UniformState.ScreenSize.getMappedBufferPtr().putFloat(0, window.getWidth());
        UniformState.ScreenSize.getMappedBufferPtr().putFloat(4, window.getHeight());
    }

    public static void setWindow(long window) {
        VRenderSystem.window = window;
    }

    public static int maxSupportedTextureSize() {
        return DeviceManager.deviceProperties.limits().maxImageDimension2D();
    }

    public static void applyMVP(Matrix4f MV, Matrix4f P) {
        MV.get(UniformState.ModelViewMat.buffer().asFloatBuffer());//MemoryUtil.memPutFloat(MemoryUtil.memAddress(modelViewMatrix), 1);
        P.get(UniformState.ProjMat.buffer().asFloatBuffer());
        calculateMVP();
    }

    public static void applyModelViewMatrix(Matrix4f mat) {
        mat.get(UniformState.ModelViewMat.buffer().asFloatBuffer());
        //MemoryUtil.memPutFloat(MemoryUtil.memAddress(modelViewMatrix), 1);
    }
    //TODO: SKip if Hashcode Matches
    public static void applyProjectionMatrix(Matrix4f mat) {
        mat.get(UniformState.ProjMat.buffer().asFloatBuffer());
    }

    public static void calculateMVP() {
        Matrix4f MV = new Matrix4f(UniformState.ModelViewMat.buffer().asFloatBuffer());
        Matrix4f P = new Matrix4f(UniformState.ProjMat.buffer().asFloatBuffer());

        final Matrix4f mul = P.mul(MV);
        mul.get(MVP.buffer());
        MVP.needsUpdate(mul.hashCode());
    }

    public static void setTextureMatrix(Matrix4f mat) {
        mat.get(UniformState.TextureMat.buffer().asFloatBuffer());
    }
    public static void setChunkOffset(float f1, float f2, float f3) {
        long ptr = UniformState.ChunkOffset.getMappedBufferPtr().ptr;
        VUtil.UNSAFE.putFloat(ptr, f1);
        VUtil.UNSAFE.putFloat(ptr + 4, f2);
        VUtil.UNSAFE.putFloat(ptr + 8, f3);
    }

    public static void setShaderColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(UniformState.ColorModulator.getMappedBufferPtr(), f1, f2, f3, f4);
    }
    //TOOD: Schedule update when actually unique data has been provided
    public static void setShaderFogColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(UniformState.FogColor.getMappedBufferPtr(), f1, f2, f3, f4);
    }

    public static MappedBuffer getShaderFogColor() {
        return UniformState.FogColor.getMappedBufferPtr();
    }

    public static void clearColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(clearColor, f1, f2, f3, f4);
    }

    public static void clear(int v) {
        Renderer.clearAttachments(v);
    }

    // Pipeline state

    public static void disableDepthTest() {
        depthTest = false;
    }

    public static void depthMask(boolean b) {
        depthMask = b;
    }

    public static void colorMask(boolean b, boolean b1, boolean b2, boolean b3) {
        colorMask = PipelineState.ColorMask.getColorMask(b, b1, b2, b3);
    }

    public static int getColorMask() {
        return colorMask;
    }

    public static void enableDepthTest() {
        depthTest = true;
    }

    public static void enableCull() {
        cull = true;
    }

    public static void disableCull() {
        cull = false;
    }

    public static void depthFunc(int depthFun) {
        VRenderSystem.depthFun = depthFun;
    }

    public static void enableBlend() {
        PipelineState.blendInfo.enabled = true;
    }

    public static void disableBlend() {
        PipelineState.blendInfo.enabled = false;
    }

    public static void blendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
        PipelineState.blendInfo.setBlendFunction(sourceFactor, destFactor);
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        PipelineState.blendInfo.setBlendFunction(srcFactor, dstFactor);
    }

    public static void blendFuncSeparate(GlStateManager.SourceFactor p_69417_, GlStateManager.DestFactor p_69418_, GlStateManager.SourceFactor p_69419_, GlStateManager.DestFactor p_69420_) {
        PipelineState.blendInfo.setBlendFuncSeparate(p_69417_, p_69418_, p_69419_, p_69420_);
    }

    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
        PipelineState.blendInfo.setBlendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
    }

    public static void enableColorLogicOp() {
        logicOp = true;
    }

    public static void disableColorLogicOp() {
        logicOp = false;
    }

    public static void logicOp(GlStateManager.LogicOp logicOp) {
        logicOpFun = logicOp.value;
    }

    public static void polygonOffset(float v, float v1) {
        depthBias[0] = v;
        depthBias[1] = v1;
    }

    public static void enablePolygonOffset() {
        Renderer.setDepthBias(depthBias[0], depthBias[1]);
    }

    public static void disablePolygonOffset() {
        Renderer.setDepthBias(0.0F, 0.0F);
    }

    public static void polygonMode(int i, int j) {
        useLines = j == GL11.GL_LINES;
        /*switch (j)
        {
            case GL11.GL_LINES -> true; *//*VK_PRIMITIVE_TOPOLOGY_LINE_LIST*//*
            case GL11.GL_LINE_STRIP -> true; *//*VK_PRIMITIVE_TOPOLOGY_LINE_STRIP*//*
            case GL11.GL_TRIANGLES, GL11.GL_QUADS -> false; *//*VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST*//*
            case GL11.GL_TRIANGLE_STRIP -> false; *//*VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP*//*
            case GL11.GL_TRIANGLE_FAN -> false; *//*VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN*//*
            default -> throw new IllegalStateException("Unexpected value: " + j);
        };*/
    }
}
