package net.vulkanmod.vulkan;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.shader.UniformState;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import static net.vulkanmod.vulkan.shader.UniformState.*;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.FloatBuffer;

import static net.vulkanmod.vulkan.shader.UniformState.MVP;
import static net.vulkanmod.vulkan.shader.UniformState.TextureMat;
public abstract class VRenderSystem {
    private static final float DEFAULT_DEPTH_VALUE = 1.0f;

    private static long window;

    public static boolean depthTest = true;
    public static boolean depthMask = true;
    public static int depthFun = 515;
    public static int topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    public static int polygonMode = VK_POLYGON_MODE_FILL;
    public static boolean canSetLineWidth = false;

    public static int colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);

    public static boolean cull = true;

    public static boolean logicOp = false;
    public static int logicOpFun = 0;

    public static float clearDepthValue = DEFAULT_DEPTH_VALUE;
    public static FloatBuffer clearColor = MemoryUtil.memCallocFloat(4);

    public static float alphaCutout = 0.0f;

    private static final float[] depthBias = new float[2];

    public static void initRenderer() {
        RenderSystem.assertInInitPhase();

        Vulkan.initVulkan(window);

        //Fix splash screen in Non-Bindless mode

        UniformState.ColorModulator.getMappedBufferPtr().putFloat(0,1);
        UniformState.ColorModulator.getMappedBufferPtr().putFloat(4, 1);
        UniformState.ColorModulator.getMappedBufferPtr().putFloat(8, 1);
        UniformState.ColorModulator.getMappedBufferPtr().putFloat(12, 1);
    }

    public static MappedBuffer getScreenSize() {
        updateScreenSize();
        return UniformState.ScreenSize.getMappedBufferPtr();
    }

    public static void updateScreenSize() {
        Window window = Minecraft.getInstance().getWindow();

        final int width = window.getWidth();
        final int height = window.getHeight();
        UniformState.ScreenSize.getMappedBufferPtr().putFloat(0, width);
        UniformState.ScreenSize.getMappedBufferPtr().putFloat(4, height);
        UniformState.ScreenSize.needsUpdate(Float.floatToRawIntBits(width)+Float.floatToRawIntBits(height));
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
        ModelViewMat.needsUpdate(MV.hashCode());
        UniformState.ProjMat.needsUpdate(P.hashCode());
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


        mat.get(TextureMat.buffer());
        TextureMat.needsUpdate(mat.hashCode());
//
//        if(!UniformState.TextureMat.hasUniqueHash())
//        {
//            UniformState.TextureMat.updateBank(Renderer.getDrawer().getUniformBuffer());
//        }
    }

    public static void setShaderColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(UniformState.ColorModulator.getMappedBufferPtr(), f1, f2, f3, f4);
        int hash = Float.floatToRawIntBits(f1) + Float.floatToRawIntBits(f2) + Float.floatToRawIntBits(f3) + Float.floatToRawIntBits(f4);
        UniformState.ColorModulator.needsUpdate(hash);
    }
    //TOOD: Schedule update when actually unique data has been provided
    public static void setShaderFogColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(UniformState.FogColor.getMappedBufferPtr(), f1, f2, f3, f4);
        int hash = Float.floatToRawIntBits(f1) + Float.floatToRawIntBits(f2) + Float.floatToRawIntBits(f3) + Float.floatToRawIntBits(f4);
        UniformState.FogColor.needsUpdate(hash);
    }

    public static MappedBuffer getShaderFogColor() {
        return UniformState.FogColor.getMappedBufferPtr();
    }

    public static void setClearColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(clearColor, f1, f2, f3, f4);
    }

    public static void clear(int mask) {
        Renderer.clearAttachments(mask);
    }

    public static void clearDepth(double depth) {
        clearDepthValue = (float) depth;
    }

    // Pipeline state

    public static void disableDepthTest() {
        depthTest = false;
    }

    public static void depthMask(boolean b) {
        depthMask = b;
    }

    public static void setPrimitiveTopologyGL(final int mode) {
        VRenderSystem.topology = switch (mode) {
            case GL11.GL_LINES, GL11.GL_LINE_STRIP  -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case GL11.GL_TRIANGLE_FAN, GL11.GL_TRIANGLES, GL11.GL_TRIANGLE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            default -> throw new RuntimeException(String.format("Unknown GL primitive topology: %s", mode));
        };
    }

    public static void setPolygonModeGL(final int mode) {
        VRenderSystem.polygonMode = switch (mode) {
            case GL11.GL_POINT -> VK_POLYGON_MODE_POINT;
            case GL11.GL_LINE -> VK_POLYGON_MODE_LINE;
            case GL11.GL_FILL -> VK_POLYGON_MODE_FILL;
            default -> throw new RuntimeException(String.format("Unknown GL polygon mode: %s", mode));
        };
    }

    public static void setLineWidth(final float width) {
        if (canSetLineWidth) {
            Renderer.setLineWidth(width);
        }
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

}
