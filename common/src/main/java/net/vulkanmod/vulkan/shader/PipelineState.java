package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_EQUAL;

public class PipelineState {
    public static final BlendState DEFAULT_BLEND_STATE = defaultBlendInfo().createBlendState();
    public static final DepthState DEFAULT_DEPTH_STATE = defaultDepthState();
    public static final LogicOpState DEFAULT_LOGICOP_STATE = new LogicOpState(false, 0);
    public static final ColorMask DEFAULT_COLORMASK = new ColorMask(true, true, true, true);

    public static PipelineState.BlendInfo blendInfo = PipelineState.defaultBlendInfo();
    public static PipelineState.BlendState currentBlendState;
    public static PipelineState.DepthState currentDepthState = PipelineState.DEFAULT_DEPTH_STATE;
    public static PipelineState.LogicOpState currentLogicOpState = PipelineState.DEFAULT_LOGICOP_STATE;
    public static PipelineState.ColorMask currentColorMask = PipelineState.DEFAULT_COLORMASK;

    public static PipelineState getCurrentPipelineState(RenderPass renderPass) {
        currentBlendState = blendInfo.createBlendState();
        currentDepthState = VRenderSystem.getDepthState();
        currentColorMask = new PipelineState.ColorMask(VRenderSystem.getColorMask());

        return new PipelineState(currentBlendState, currentDepthState, currentLogicOpState, currentColorMask, renderPass);
    }

    final BlendState blendState;
    final DepthState depthState;
    final ColorMask colorMask;
    final LogicOpState logicOpState;
    final boolean cullState;
    final RenderPass renderPass;

    public PipelineState(BlendState blendState, DepthState depthState, LogicOpState logicOpState, ColorMask colorMask, RenderPass renderPass) {
        this.blendState = blendState;
        this.depthState = depthState;
        this.logicOpState = logicOpState;
        this.colorMask = colorMask;
        this.renderPass = renderPass;
        this.cullState = VRenderSystem.cull;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PipelineState that = (PipelineState) o;
        return blendState.equals(that.blendState) && depthState.equals(that.depthState)
                && this.renderPass == that.renderPass
                && logicOpState.equals(that.logicOpState) && (cullState == that.cullState) && colorMask.equals(that.colorMask);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blendState, depthState, logicOpState, cullState, renderPass);
    }

    public static BlendInfo defaultBlendInfo() {
        return new BlendInfo(true, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
                VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD);
    }

    public static DepthState defaultDepthState() {
        return new DepthState(true, true, 515);
    }

    public static ColorMask defaultColorMask() { return new ColorMask(true, true, true, true); }

    public static class BlendInfo {
        public boolean enabled;
        public int srcRgbFactor;
        public int dstRgbFactor;
        public int srcAlphaFactor;
        public int dstAlphaFactor;
        public int blendOp;

        public BlendInfo(boolean enabled, int srcRgbFactor, int dstRgbFactor, int srcAlphaFactor, int dstAlphaFactor, int blendOp) {
            this.enabled = enabled;
            this.srcRgbFactor = srcRgbFactor;
            this.dstRgbFactor = dstRgbFactor;
            this.srcAlphaFactor = srcAlphaFactor;
            this.dstAlphaFactor = dstAlphaFactor;
            this.blendOp = blendOp;
        }

        public void setBlendFunction(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
            this.srcRgbFactor = glToVulkanBlendFactor(sourceFactor.value);
            this.srcAlphaFactor = glToVulkanBlendFactor(sourceFactor.value);
            this.dstRgbFactor = glToVulkanBlendFactor(destFactor.value);
            this.dstAlphaFactor = glToVulkanBlendFactor(destFactor.value);
        }

        public void setBlendFuncSeparate(GlStateManager.SourceFactor srcRgb, GlStateManager.DestFactor dstRgb, GlStateManager.SourceFactor srcAlpha, GlStateManager.DestFactor dstAlpha) {
            this.srcRgbFactor = glToVulkanBlendFactor(srcRgb.value);
            this.srcAlphaFactor = glToVulkanBlendFactor(srcAlpha.value);
            this.dstRgbFactor = glToVulkanBlendFactor(dstRgb.value);
            this.dstAlphaFactor = glToVulkanBlendFactor(dstAlpha.value);
        }

        /* gl to Vulkan conversion */
        public void setBlendFunction(int sourceFactor, int destFactor) {
            this.srcRgbFactor = glToVulkanBlendFactor(sourceFactor);
            this.srcAlphaFactor = glToVulkanBlendFactor(sourceFactor);
            this.dstRgbFactor = glToVulkanBlendFactor(destFactor);
            this.dstAlphaFactor = glToVulkanBlendFactor(destFactor);
        }

        /* gl to Vulkan conversion */
        public void setBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this.srcRgbFactor = glToVulkanBlendFactor(srcRgb);
            this.srcAlphaFactor = glToVulkanBlendFactor(srcAlpha);
            this.dstRgbFactor = glToVulkanBlendFactor(dstRgb);
            this.dstAlphaFactor = glToVulkanBlendFactor(dstAlpha);
        }

        public void setBlendOp(int i) {
            this.blendOp = glToVulkanBlendOp(i);
        }

        public BlendState createBlendState() {
            return new BlendState(this.enabled, this.srcRgbFactor, this.dstRgbFactor, this.srcAlphaFactor, this.dstAlphaFactor, this.blendOp);
        }

        private static int glToVulkanBlendOp(int value) {
            return switch (value) {
                case 0x8006 -> VK_BLEND_OP_ADD;
                case 0x8007 -> VK_BLEND_OP_MIN;
                case 0x8008 -> VK_BLEND_OP_MAX;
                case 0x800A -> VK_BLEND_OP_SUBTRACT;
                case 0x800B -> VK_BLEND_OP_REVERSE_SUBTRACT;
                default -> throw new RuntimeException("unknown blend factor: " + value);


//                GL_FUNC_ADD = 0x8006,
//                GL_MIN      = 0x8007,
//                GL_MAX      = 0x8008;
//                GL_FUNC_SUBTRACT         = 0x800A,
//                GL_FUNC_REVERSE_SUBTRACT = 0x800B;
            };
        }

        private static int glToVulkanBlendFactor(int value) {
            return switch (value) {
                case 1 -> VK_BLEND_FACTOR_ONE;
                case 0 -> VK_BLEND_FACTOR_ZERO;
                case 771 -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
                case 770 -> VK_BLEND_FACTOR_SRC_ALPHA;
                case 775 -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
                case 769 -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
                case 774 -> VK_BLEND_FACTOR_DST_COLOR;
                case 768 -> VK_BLEND_FACTOR_SRC_COLOR;
                default -> throw new RuntimeException("unknown blend factor: " + value);


//                        CONSTANT_ALPHA(32771),
//                        CONSTANT_COLOR(32769),
//                        DST_ALPHA(772),
//                        DST_COLOR(774),
//                        ONE(1),
//                        ONE_MINUS_CONSTANT_ALPHA(32772),
//                        ONE_MINUS_CONSTANT_COLOR(32770),
//                        ONE_MINUS_DST_ALPHA(773),
//                        ONE_MINUS_DST_COLOR(775),
//                        ONE_MINUS_SRC_ALPHA(771),
//                        ONE_MINUS_SRC_COLOR(769),
//                        SRC_ALPHA(770),
//                        SRC_ALPHA_SATURATE(776),
//                        SRC_COLOR(768),
//                        ZERO(0);
            };
        }
    }

    public static class BlendState {
        public final boolean enabled;
        public final int srcRgbFactor;
        public final int dstRgbFactor;
        public final int srcAlphaFactor;
        public final int dstAlphaFactor;
        public final int blendOp;

        protected BlendState(boolean enabled, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, int blendOp) {
            this.enabled = enabled;
            this.srcRgbFactor = srcRgb;
            this.dstRgbFactor = dstRgb;
            this.srcAlphaFactor = srcAlpha;
            this.dstAlphaFactor = dstAlpha;
            this.blendOp = blendOp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return this.equals((BlendState) o);
        }

        public boolean equals(BlendState blendState) {
            if(!this.enabled && !blendState.enabled) return true;
            if(this.enabled != blendState.enabled) return false;
            return srcRgbFactor == blendState.srcRgbFactor && dstRgbFactor == blendState.dstRgbFactor && srcAlphaFactor == blendState.srcAlphaFactor && dstAlphaFactor == blendState.dstAlphaFactor && blendOp == blendState.blendOp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(srcRgbFactor, dstRgbFactor, srcAlphaFactor, dstAlphaFactor, blendOp);
        }
    }

    public static class LogicOpState {
        public final boolean enabled;
        private int logicOp;

        public LogicOpState(boolean enable, int op) {
            this.enabled = enable;
            this.logicOp = op;
        }

        public void setLogicOp(GlStateManager.LogicOp logicOp) {
            switch (logicOp) {
                case OR_REVERSE -> setLogicOp(VK_LOGIC_OP_OR_REVERSE);
            }

        }

        public void setLogicOp(int logicOp) {
            this.logicOp = logicOp;
        }

        public int getLogicOp() {
            return logicOp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LogicOpState logicOpState = (LogicOpState) o;
            if(this.enabled != logicOpState.enabled) return false;
            return logicOp == logicOpState.logicOp;
        }

        public int hashCode() {
            return Objects.hash(enabled, logicOp);
        }
    }

    public static class ColorMask {
        public final int colorMask;

        public ColorMask(boolean r, boolean g, boolean b, boolean a) {
            this.colorMask = (r ? VK_COLOR_COMPONENT_R_BIT : 0) | (g ? VK_COLOR_COMPONENT_G_BIT : 0) | (b ? VK_COLOR_COMPONENT_B_BIT : 0) | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

        public ColorMask(int mask) {
            this.colorMask = mask;
        }

        public static int getColorMask(boolean r, boolean g, boolean b, boolean a) {
            return (r ? VK_COLOR_COMPONENT_R_BIT : 0) | (g ? VK_COLOR_COMPONENT_G_BIT : 0) | (b ? VK_COLOR_COMPONENT_B_BIT : 0) | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColorMask colorMask = (ColorMask) o;
            return this.colorMask == colorMask.colorMask;
        }
    }

    public static class DepthState {
        public final boolean depthTest;
        public final boolean depthMask;
        public final int function;

        public DepthState(boolean depthTest, boolean depthMask, int function) {
            this.depthTest = depthTest;
            this.depthMask = depthMask;
            this.function = glToVulkan(function);
        }

        private static int glToVulkan(int value) {
            return switch (value) {
                case 515 -> VK_COMPARE_OP_LESS_OR_EQUAL;
                case 519 -> VK_COMPARE_OP_ALWAYS;
                case 516 -> VK_COMPARE_OP_GREATER;
                case 518 -> VK_COMPARE_OP_GREATER_OR_EQUAL;
                case 514 -> VK_COMPARE_OP_EQUAL;
                default -> throw new RuntimeException("unknown blend factor..");


//                public static final int GL_NEVER = 512;
//                public static final int GL_LESS = 513;
//                public static final int GL_EQUAL = 514;
//                public static final int GL_LEQUAL = 515;
//                public static final int GL_GREATER = 516;
//                public static final int GL_NOTEQUAL = 517;
//                public static final int GL_GEQUAL = 518;
//                public static final int GL_ALWAYS = 519;
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DepthState that = (DepthState) o;
            return depthTest == that.depthTest && depthMask == that.depthMask && function == that.function;
        }

        @Override
        public int hashCode() {
            return Objects.hash(depthTest, depthMask, function);
        }
    }
}
