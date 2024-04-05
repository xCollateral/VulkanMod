package net.vulkanmod.mixin.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.DeviceInfo;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.vulkanmod.Initializer.getVersion;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayM {

    @Shadow @Final private Minecraft minecraft;

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
        strings.add("BARMemory: " + MemoryType.BAR_MEM.usedBytes()+"/" + MemoryType.BAR_MEM.maxSize() + "MB");
        strings.add("DeviceMemory: " + MemoryType.GPU_MEM.usedBytes()+"/" + MemoryType.GPU_MEM.maxSize() + "MB");
        strings.add("");
//        strings.add("PCIe Transfers");
//        strings.add("GPU Transfers");
        strings.add("VulkanMod " + getVersion());
        strings.add("CPU: " + DeviceInfo.cpuInfo);
        strings.add("GPU: " + Vulkan.getDeviceInfo().deviceName);
        strings.add("Driver: " + Vulkan.getDeviceInfo().driverVersion);
        strings.add("Vulkan: " + Vulkan.getDeviceInfo().vkVersion);
        strings.add("");
        strings.add("");

        Collections.addAll(strings, WorldRenderer.getInstance().getChunkAreaManager().getStats());

        return strings;
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

//    @Inject(method = "drawGameInformation",
//            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
//                    shift = At.Shift.AFTER,
//                    ordinal = 2))
//    protected void inject1(GuiGraphics guiGraphics, CallbackInfo ci)
//    {
//
//        RenderSystem.enableBlend();
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//    }
//
//
//    @Redirect(method = "renderLines",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
//    protected void redirectFill(GuiGraphics instance, int i, int j, int k, int l, int m)
//    {
//        GuiBatchRenderer.fill(instance.pose(), m, k, j, l, m);
//    }
//
//    @Redirect(method = "drawGameInformation(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I"))
//    protected int renderStuffRedirectThree(Font instance, PoseStack $$0, String $$1, float $$2, float $$3, int $$4)
//    {
//        return 0;
//    }
//
//    @Inject(method = "drawGameInformation(Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"),
//            locals = LocalCapture.CAPTURE_FAILHARD)
//    public void renderStuff3(PoseStack poseStack, CallbackInfo ci, List<String> list)
//    {
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
//            GuiBatchRenderer.drawString(this.font, bufferSource, poseStack, string, 2.0f, (float)m, 0xE0E0E0);
//        }
//        bufferSource.endBatch();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite
//    public void drawSystemInformation(PoseStack matrices) {
//        List<String> list = this.getSystemInformation();
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
//            int l = this.minecraft.getWindow().getGuiScaledWidth() - 2 - k;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.fill(matrices, l - 1, m - 1, l + k + 1, m + j - 1, -1873784752);
//        }
//        GuiBatchRenderer.endBatch();
//
//        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.font.lineHeight;
//            int k = this.font.width(string);
//            int l = this.minecraft.getWindow().getGuiScaledWidth() - 2 - k;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.drawString(this.font, bufferSource, matrices, string, (float)l, (float)m, 0xE0E0E0);
//        }
//        bufferSource.endBatch();
//    }
}
