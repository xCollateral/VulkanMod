package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_EQUAL;

public class PipelineState {
    private static final int DEFAULT_DEPTH_OP = 515;
//    private static final int DEFAULT_DEPTH_OP = 518;

    public static PipelineState.BlendInfo blendInfo = PipelineState.defaultBlendInfo();

    public static final PipelineState DEFAULT = new PipelineState(getAssemblyRasterState(), getBlendState(), getDepthState(), getLogicOpState(), VRenderSystem.getColorMask(), null);

    public static PipelineState currentState = DEFAULT;

    public static PipelineState getCurrentPipelineState(RenderPass renderPass) {
        int assemblyRasterState = getAssemblyRasterState();
        int blendState = getBlendState();
        int currentColorMask = VRenderSystem.getColorMask();
        int depthState = getDepthState();
        int logicOp = getLogicOpState();

        if(currentState.checkEquals(assemblyRasterState, blendState, depthState, logicOp, currentColorMask, renderPass))
            return currentState;
        else
            return currentState = new PipelineState(assemblyRasterState, blendState, depthState, logicOp, currentColorMask, renderPass);
    }

    public static int getBlendState() {
        return BlendState.getState(blendInfo);
    }

    public static int getAssemblyRasterState() {
        return AssemblyRasterState.encode(VRenderSystem.cull, VRenderSystem.topology, VRenderSystem.polygonMode);
    }

    public static int getDepthState() {
        int depthState = 0;

        depthState |= VRenderSystem.depthTest ? DepthState.DEPTH_TEST_BIT : 0;
        depthState |= VRenderSystem.depthMask ? DepthState.DEPTH_MASK_BIT : 0;

        depthState |= DepthState.encodeDepthFun(VRenderSystem.depthFun);

        return depthState;
    }

    public static int getLogicOpState() {
        int logicOpState = 0;

        logicOpState |= VRenderSystem.logicOp ? LogicOpState.ENABLE_BIT : 0;

        logicOpState |= LogicOpState.encodeLogicOpFun(VRenderSystem.logicOpFun);

        return logicOpState;
    }

    final RenderPass renderPass;

    int assemblyRasterState;
    int blendState_i;
    int depthState_i;
    int colorMask_i;
    int logicOp_i;

    public PipelineState(int assemblyRasterState, int blendState, int depthState, int logicOp, int colorMask, RenderPass renderPass) {
        this.renderPass = renderPass;

        this.assemblyRasterState = assemblyRasterState;
        this.blendState_i = blendState;
        this.depthState_i = depthState;
        this.colorMask_i = colorMask;
        this.logicOp_i = logicOp;
    }

    private boolean checkEquals(int assemblyRasterState, int blendState, int depthState, int logicOp, int colorMask, RenderPass renderPass) {
        return (blendState == this.blendState_i) && (depthState == this.depthState_i)
                && renderPass == this.renderPass && logicOp == this.logicOp_i
                && (assemblyRasterState == this.assemblyRasterState)
                && colorMask == this.colorMask_i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PipelineState that = (PipelineState) o;
        return (blendState_i == that.blendState_i) && (depthState_i == that.depthState_i)
                && this.renderPass == that.renderPass && logicOp_i == that.logicOp_i
                && this.assemblyRasterState == that.assemblyRasterState
                && this.colorMask_i == that.colorMask_i;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blendState_i, depthState_i, logicOp_i, assemblyRasterState, colorMask_i, renderPass);
    }

    public static BlendInfo defaultBlendInfo() {
        return new BlendInfo(true, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
                VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD);
    }

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


        public int createBlendState() {
            return BlendState.getState(this);
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
        public static final int SRC_RGB_OFFSET = 0;
        public static final int DST_RGB_OFFSET = 5;
        public static final int SRC_A_OFFSET = 10;
        public static final int DST_A_OFFSET = 15;
        public static final int FUN_OFFSET = 20;

        public static final int ENABLE_BIT = 1 << 24;

        public static final int OP_MASK = 0xF;
        public static final int FACTOR_MASK = 0x1F;

        public static int getState(BlendInfo blendInfo) {
            int s = 0;
            s |= blendInfo.enabled ? ENABLE_BIT : 0;
            s |= encode(blendInfo.srcRgbFactor, SRC_RGB_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.dstRgbFactor, DST_RGB_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.srcAlphaFactor, SRC_A_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.dstAlphaFactor, DST_A_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.blendOp, FUN_OFFSET, OP_MASK);

            return s;
        }

        public static boolean enable(int i) {
            return (i & ENABLE_BIT) != 0;
        }

        public static int encode(int i, int offset, int mask) {
            return (i & mask) << offset;
        }

        public static int decode(int i, int offset, int bits) {
            return (i >>> offset) & bits;
        }

        public static int getSrcRgbFactor(int s) {
            return decode(s, SRC_RGB_OFFSET, FACTOR_MASK);
        }

        public static int getDstRgbFactor(int s) {
            return decode(s, DST_RGB_OFFSET, FACTOR_MASK);
        }

        public static int getSrcAlphaFactor(int s) {
            return decode(s, SRC_A_OFFSET, FACTOR_MASK);
        }

        public static int getDstAlphaFactor(int s) {
            return decode(s, DST_A_OFFSET, FACTOR_MASK);
        }

        public static int blendOp(int state) {
            return state >>> FUN_OFFSET;
        }

    }

    public abstract static class LogicOpState {
        public static final int ENABLE_BIT = 1;

        public static final int FUN_OFFSET = 1;
        public static final int FUN_BITS = 5;

        public static boolean enable(int i) {
            return (i & ENABLE_BIT) != 0;
        }

        public static int encodeLogicOpFun(int glFun) {
            int fun = glToVulkan(glFun);

            return fun << FUN_OFFSET;
        }

        public static int decodeFun(int state) {
            return state >>> FUN_OFFSET;
        }

        public static int glToVulkan(int f) {
            return switch (f) {
                case 5387 -> VK_LOGIC_OP_OR_REVERSE;
                //TODO complete

                default -> VK_LOGIC_OP_AND;
            };
        }

    }

    public abstract static class AssemblyRasterState {
        public static final int POLYGON_MODE_MASK = 7;

        public static final int TOPOLOGY_OFFSET = 3;
        public static final int TOPOLOGY_BITS = 4;
        public static final int TOPOLOGY_MASK = 0b11111;

        public static final int CULL_MODE_OFFSET = TOPOLOGY_OFFSET + TOPOLOGY_BITS;
        public static final int CULL_MODE_BITS = 2;
        public static final int CULL_MODE_MASK = 0b11;

        public static int encode(boolean cull, int topology, int polygonMode) {
            int state = (polygonMode | (topology << TOPOLOGY_OFFSET));
            state |= ((cull ? VK_CULL_MODE_BACK_BIT : VK_CULL_MODE_NONE) << CULL_MODE_OFFSET);

            return state;
        }

        public static int decodeTopology(int state) {
            return (state >>> TOPOLOGY_OFFSET) & TOPOLOGY_MASK;
        }

        public static int decodePolygonMode(int state) {
            return state & POLYGON_MODE_MASK;
        }

        public static int decodeCullMode(int state) {
            return (state >>> CULL_MODE_OFFSET) & CULL_MODE_MASK;
        }
    }

    public static abstract class ColorMask {

        public static int getColorMask(boolean r, boolean g, boolean b, boolean a) {
            return (r ? VK_COLOR_COMPONENT_R_BIT : 0)
                    | (g ? VK_COLOR_COMPONENT_G_BIT : 0)
                    | (b ? VK_COLOR_COMPONENT_B_BIT : 0)
                    | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

    }

    public static abstract class DepthState {
        public static final int DEPTH_TEST_BIT = 1;
        public static final int DEPTH_MASK_BIT = 2;

        public static final int DEPTH_FUN_OFFSET = 2;
        public static final int DEPTH_FUN_BITS = 4;

        public static boolean depthTest(int i) {
            return (i & DEPTH_TEST_BIT) != 0;
        }

        public static boolean depthMask(int i) {
            return (i & DEPTH_MASK_BIT) != 0;
        }

        public static int encodeDepthFun(int glFun) {
            int fun = glToVulkan(glFun);

            return fun << DEPTH_FUN_OFFSET;
        }

        public static int decodeDepthFun(int state) {
            return state >>> DEPTH_FUN_OFFSET;
        }

        private static int glToVulkan(int value) {
            return switch (value) {
                case 515 -> VK_COMPARE_OP_LESS_OR_EQUAL;
                case 519 -> VK_COMPARE_OP_ALWAYS;
                case 516 -> VK_COMPARE_OP_GREATER;
                case 518 -> VK_COMPARE_OP_GREATER_OR_EQUAL;
                case 514 -> VK_COMPARE_OP_EQUAL;
                default -> throw new RuntimeException("unknown blend factor..");

//                case 515 -> VK_COMPARE_OP_GREATER_OR_EQUAL;
//                case 519 -> VK_COMPARE_OP_ALWAYS;
//                case 516 -> VK_COMPARE_OP_GREATER;
//                case 518 -> VK_COMPARE_OP_LESS_OR_EQUAL;
//                case 514 -> VK_COMPARE_OP_EQUAL;
//                default -> throw new RuntimeException("unknown blend factor..");

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

    }
}
