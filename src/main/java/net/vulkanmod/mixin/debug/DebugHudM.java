package net.vulkanmod.mixin.debug;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.vulkanmod.render.chunk.util.VBOUtil;
import net.vulkanmod.render.gui.GuiBatchRenderer;
import net.vulkanmod.vulkan.DeviceInfo;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.Initializer.getVersion;
import static net.vulkanmod.render.chunk.util.VBOUtil.*;
import static org.lwjgl.vulkan.VK10.*;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudM {

    @Shadow @Final private Minecraft minecraft;

    private static final String VkVersionString=getVulkanVer();

    @Shadow
    private static long bytesToMegabytes(long bytes) {
        return 0;
    }

    @Shadow @Final private Font font;

    @Shadow protected abstract List<String> getGameInformation();

    @Shadow protected abstract List<String> getSystemInformation();

    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> redirectList(Object[] elements) {
        ArrayList<String> strings = new ArrayList<>();

        long l = Runtime.getRuntime().maxMemory();
        long m = Runtime.getRuntime().totalMemory();
        long n = Runtime.getRuntime().freeMemory();
        long o = m - n;

        strings.add(String.format("Java: %s %dbit", System.getProperty("java.version"), this.minecraft.is64Bit() ? 64 : 32));
        strings.add(String.format("Mem: % 2d%% %03d/%03dMB", o * 100L / l, bytesToMegabytes(o), bytesToMegabytes(l)));
        strings.add(String.format("Allocated: % 2d%% %03dMB", m * 100L / l, bytesToMegabytes(m)));
        strings.add(String.format("Off-heap: " + getOffHeapMemory() + "MB"));
        strings.add("NativeMemory: " + MemoryManager.getInstance().getNativeMemoryMB() + "MB");
        strings.add("DeviceMemory: " + MemoryManager.getInstance().getDeviceMemoryMB() + "MB");
        strings.add("");
        strings.add("VulkanMod " + getVersion());
        strings.add("CPU: " + DeviceInfo.cpuInfo);
        strings.add("GPU: " + Vulkan.getDeviceInfo().deviceName);
        strings.add("Driver: " + Vulkan.getDeviceInfo().driverVersion);
        strings.add("Vulkan Version: " + VkVersionString);
        strings.add("");
        strings.add("-=VBO Stats=-");
        strings.add("Cutout: "+ cutoutChunks.size());
        strings.add("Translucent: "+ translucentChunks.size());
        strings.add("");
        strings.add("Total: "+ (
                cutoutChunks.size() +
                translucentChunks.size()));

        strings.add("tIndex-Buffers");
        strings.add("");

        strings.add("Used Bytes: " + (virtualBufferIdx.usedBytes >> 20) + "MB");
        strings.add("Max Size: " + (virtualBufferIdx.size_t >> 20) + "MB");
//        strings.add("Allocs: " + VirtualBuffer.allocs);
//        strings.add("allocBytes: " + VirtualBuffer.allocBytes);
        strings.add("subAllocs: " + virtualBufferIdx.subAllocs);
//        strings.add("Blocks: " + VirtualBuffer.blocks);
//        strings.add("BlocksBytes: " + VirtualBuffer.blockBytes);

        strings.add("minRange: " + virtualBufferIdx.unusedRangesS);
        strings.add("maxRange: " + virtualBufferIdx.unusedRangesM);
        strings.add("unusedRangesCount: " + virtualBufferIdx.unusedRangesCount);
        strings.add("minVBOSize: " + virtualBufferIdx.allocMin);
        strings.add("maxVBOSize: " + virtualBufferIdx.allocMax);
        strings.add("unusedBytes: " + (virtualBufferIdx.size_t- virtualBufferIdx.usedBytes >> 20) + "MB");
        strings.add("freeRanges: " + (virtualBufferIdx.FreeRanges.size()));
        strings.add("activeRanges: " + (virtualBufferIdx.activeRanges.size()));

        return strings;
    }

    private static String getVulkanVer() {
        return VK_VERSION_MAJOR(Vulkan.vkVer)+"."+VK_VERSION_MINOR(Vulkan.vkVer)+"."+VK_VERSION_PATCH(Vulkan.vkVer);
    }

    private long getOffHeapMemory() {
        return bytesToMegabytes(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
    }

//    /**
//     * @author
//     */
//    @Overwrite
//    public void drawGameInformation(PoseStack matrices) {
//        List<String> list = this.getGameInformation();
//        list.add("");
//        boolean bl = this.minecraft.getSingleplayerServer() != null;
//        list.add("Debug: Pie [shift]: " + (this.minecraft.options.renderDebugCharts ? "visible" : "hidden") + (bl ? " FPS + TPS" : " FPS") + " [alt]: " + (this.minecraft.options.renderFpsChart ? "visible" : "hidden"));
//        list.add("For help: press F3 + Q");
//
//        RenderSystem.enableBlend();
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.font.lineHeight;
//            int k = this.font.width(string);
//            int l = 2;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.fill(matrices, 1, m - 1, 2 + k + 1, m + j - 1, -1873784752);
//        }
//        GuiBatchRenderer.endBatch();
//
//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.font.lineHeight;
//            int k = this.font.width(string);
//            int l = 2;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.drawString(this.font, bufferSource, matrices, string, 2.0f, (float)m, 0xE0E0E0);
//        }
//        bufferSource.endBatch();
//    }

    @Inject(method = "drawGameInformation(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    shift = At.Shift.AFTER,
                    ordinal = 2))
    protected void renderStuffOne(PoseStack poseStack, CallbackInfo ci)
    {

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }


    @Redirect(method = "drawGameInformation(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;fill(Lcom/mojang/blaze3d/vertex/PoseStack;IIIII)V"))
    protected void renderStuffRedirectTwo(PoseStack poseStack, int m, int k, int j, int e, int d)
    {
        GuiBatchRenderer.fill(poseStack, m, k, j, e, d);
    }
    @Redirect(method = "drawGameInformation(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I"))
    protected int renderStuffRedirectThree(Font instance, PoseStack $$0, String $$1, float $$2, float $$3, int $$4)
    {
        return 0;
    }

    @Inject(method = "drawGameInformation(Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void renderStuff3(PoseStack poseStack, CallbackInfo ci, List<String> list)
    {
        GuiBatchRenderer.endBatch();

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = 2;
            int m = 2 + j * i;

            GuiBatchRenderer.drawString(this.font, bufferSource, poseStack, string, 2.0f, (float)m, 0xE0E0E0);
        }
        bufferSource.endBatch();
    }

    /**
     * @author
     */
    @Overwrite
    public void drawSystemInformation(PoseStack matrices) {
        List<String> list = this.getSystemInformation();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = this.minecraft.getWindow().getGuiScaledWidth() - 2 - k;
            int m = 2 + j * i;

            GuiBatchRenderer.fill(matrices, l - 1, m - 1, l + k + 1, m + j - 1, -1873784752);
        }
        GuiBatchRenderer.endBatch();

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = this.minecraft.getWindow().getGuiScaledWidth() - 2 - k;
            int m = 2 + j * i;

            GuiBatchRenderer.drawString(this.font, bufferSource, matrices, string, (float)l, (float)m, 0xE0E0E0);
        }
        bufferSource.endBatch();
    }
}
