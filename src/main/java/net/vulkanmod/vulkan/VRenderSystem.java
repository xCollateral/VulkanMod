package net.vulkanmod.vulkan;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class VRenderSystem {

    private static String capsString = "";
    private static String cpuInfo;
    
    private static long window;

    public static boolean depthTest = true;
    public static boolean depthMask = true;
    public static int depthFun = 515;

    public static int colorMask = Pipeline.ColorMask.getColorMask(true, true, true, true);

    public static boolean cull = true;

    public static float clearDepth = 1.0f;
    public static FloatBuffer clearColor = MemoryUtil.memAllocFloat(4);

    public static ByteBuffer modelViewMatrix = MemoryUtil.memAlloc(16 * 4);
    public static ByteBuffer projectionMatrix = MemoryUtil.memAlloc(16 * 4);
    public static ByteBuffer TextureMatrix = MemoryUtil.memAlloc(16 * 4);
    public static ByteBuffer MVP = MemoryUtil.memAlloc(16 * 4);

    public static ByteBuffer ChunkOffset = MemoryUtil.memAlloc(3 * 4);
    public static ByteBuffer lightDirection0 = MemoryUtil.memAlloc(3 * 4);
    public static ByteBuffer lightDirection1 = MemoryUtil.memAlloc(3 * 4);

    public static ByteBuffer shaderColor = MemoryUtil.memAlloc(4 * 4);
    public static ByteBuffer shaderFogColor = MemoryUtil.memAlloc(4 * 4);

    private static final float[] depthBias = new float[2];

    public static void initRenderer()
    {
        RenderSystem.assertInInitPhase();

        try {
            CentralProcessor centralprocessor = (new SystemInfo()).getHardware().getProcessor();
            cpuInfo = String.format("%dx %s", centralprocessor.getLogicalProcessorCount(), centralprocessor.getProcessorIdentifier().getName()).replaceAll("\\s+", " ");
        } catch (Throwable throwable) {
        }

        Vulkan.initVulkan(window);

    }

    public static ByteBuffer getChunkOffset() { return ChunkOffset; }

    public static int maxSupportedTextureSize() {
        return Vulkan.deviceProperties.limits().maxImageDimension2D();
    }

    public static void renderCrosshair(int p_69348_, boolean p_69349_, boolean p_69350_, boolean p_69351_) {
        RenderSystem.assertOnRenderThread();
//        GlStateManager._disableTexture();
//        GlStateManager._depthMask(false);
//        GlStateManager._disableCull();
        VRenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        Tesselator tesselator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.lineWidth(4.0F);
        bufferbuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        if (p_69349_) {
            bufferbuilder.vertex(0.0D, 0.0D, 0.0D).color(0, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.vertex((double)p_69348_, 0.0D, 0.0D).color(0, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
        }

        if (p_69350_) {
            bufferbuilder.vertex(0.0D, 0.0D, 0.0D).color(0, 0, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
            bufferbuilder.vertex(0.0D, (double)p_69348_, 0.0D).color(0, 0, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
        }

        if (p_69351_) {
            bufferbuilder.vertex(0.0D, 0.0D, 0.0D).color(0, 0, 0, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.vertex(0.0D, 0.0D, (double)p_69348_).color(0, 0, 0, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
        }

        tesselator.end();
        RenderSystem.lineWidth(2.0F);
        bufferbuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        if (p_69349_) {
            bufferbuilder.vertex(0.0D, 0.0D, 0.0D).color(255, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
            bufferbuilder.vertex((double)p_69348_, 0.0D, 0.0D).color(255, 0, 0, 255).normal(1.0F, 0.0F, 0.0F).endVertex();
        }

        if (p_69350_) {
            bufferbuilder.vertex(0.0D, 0.0D, 0.0D).color(0, 255, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
            bufferbuilder.vertex(0.0D, (double)p_69348_, 0.0D).color(0, 255, 0, 255).normal(0.0F, 1.0F, 0.0F).endVertex();
        }

        if (p_69351_) {
            bufferbuilder.vertex(0.0D, 0.0D, 0.0D).color(127, 127, 255, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
            bufferbuilder.vertex(0.0D, 0.0D, (double)p_69348_).color(127, 127, 255, 255).normal(0.0F, 0.0F, 1.0F).endVertex();
        }

        tesselator.end();
        RenderSystem.lineWidth(1.0F);
//        GlStateManager._enableCull();
        RenderSystem.depthMask(true);
//        GlStateManager._enableTexture();
    }

    public static void applyMVPAffine(Matrix4f MV, Matrix4f P) {
        applyModelViewMatrix(MV);
        applyProjectionMatrix(P);
        calculateMVPAffine();
    }
    public static void applyMVP(Matrix4f MV, Matrix4f P) {
        applyModelViewMatrix(MV);
        applyProjectionMatrix(P);
        calculateMVP();
    }

    public static void applyModelViewMatrix(Matrix4f mat) {
        mat.store(modelViewMatrix.asFloatBuffer());
        //MemoryUtil.memPutFloat(MemoryUtil.memAddress(modelViewMatrix), 1);
    }

    public static void applyProjectionMatrix(Matrix4f mat) {
        mat.store(projectionMatrix.asFloatBuffer());
    }

    public static void calculateMVPAffine() {
        org.joml.Matrix4f MV = new org.joml.Matrix4f(modelViewMatrix.asFloatBuffer());
        org.joml.Matrix4f P = new org.joml.Matrix4f(projectionMatrix.asFloatBuffer());

        P.mulPerspectiveAffine(MV).get(MVP);
    }
    public static void calculateMVP() {
        org.joml.Matrix4f MV = new org.joml.Matrix4f(modelViewMatrix.asFloatBuffer());
        org.joml.Matrix4f P = new org.joml.Matrix4f(projectionMatrix.asFloatBuffer());

        P.mul(MV).get(MVP);
    }

    public static void setTextureMatrix(Matrix4f mat) {
        mat.store(TextureMatrix.asFloatBuffer());
    }

    public static ByteBuffer getTextureMatrix() {
        return TextureMatrix;
    }

    public static ByteBuffer getModelViewMatrix() {
        return modelViewMatrix;
    }

    public static ByteBuffer getProjectionMatrix() {
        return projectionMatrix;
    }

    public static ByteBuffer getMVP() {
        return MVP;
    }

    public static void setChunkOffset(float f1, float f2, float f3) {
        ChunkOffset.putFloat(0, f1);
        ChunkOffset.putFloat(4, f2);
        ChunkOffset.putFloat(8, f3);
    }

    public static void setShaderColor(float f1, float f2, float f3, float f4) {
        shaderColor.putFloat(0, f1);
        shaderColor.putFloat(4, f2);
        shaderColor.putFloat(8, f3);
        shaderColor.putFloat(12, f4);
    }

    public static void setFogShaderColor(float f1, float f2, float f3, float f4) {
        shaderFogColor.putFloat(0, f1);
        shaderFogColor.putFloat(4, f2);
        shaderFogColor.putFloat(8, f3);
        shaderFogColor.putFloat(12, f4);
    }

    public static ByteBuffer getShaderColor() {
        return shaderColor;
    }

    public static ByteBuffer getShaderFogColor() {
        return shaderFogColor;
    }

    public static void enableColorLogicOp() {
        Drawer.currentLogicOpState = new Pipeline.LogicOpState(true, 0);
    }

    public static void disableColorLogicOp() {
        Drawer.currentLogicOpState = Pipeline.DEFAULT_LOGICOP_STATE;
    }

    public static void logicOp(GlStateManager.LogicOp p_69836_) {
        Drawer.currentLogicOpState.setLogicOp(p_69836_);
    }

    public static void clearColor(float f1, float f2, float f3, float f4) {
        clearColor.put(0, f1);
        clearColor.put(1, f2);
        clearColor.put(2, f3);
        clearColor.put(3, f4);

    }

    public static void clear(int v) {
        Drawer.clearAttachments(v);
    }

    public static void disableDepthTest() {
        depthTest = false;
    }

    public static void depthMask(boolean b) {
        depthMask = b;
    }

    public static Pipeline.DepthState getDepthState() {
        return new Pipeline.DepthState(depthTest, depthMask, depthFun);
    }

    public static void colorMask(boolean b, boolean b1, boolean b2, boolean b3) {
        colorMask = Pipeline.ColorMask.getColorMask(b, b1, b2, b3);
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

    public static void polygonOffset(float v, float v1) {
        depthBias[0] = v;
        depthBias[1] = v1;
    }

    public static void enablePolygonOffset() {
        Drawer.setDepthBias(depthBias[0], depthBias[1]);
    }

    public static void disablePolygonOffset() {
        Drawer.setDepthBias(0.0F, 0.0F);
    }

    public static Vector2f getScreenSize() {
        Window window = Minecraft.getInstance().getWindow();
        return new Vector2f((float)window.getWidth(), (float)window.getHeight());
    }
    
    public static void setWindow(long window) {
        VRenderSystem.window = window;
    }

    public static void depthFunc(int p_69457_) {
        depthFun = p_69457_;
    }
}
