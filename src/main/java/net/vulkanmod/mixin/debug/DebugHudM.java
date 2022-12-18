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
import net.vulkanmod.render.RHandler;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.render.gui.GuiBatchRenderer;
import net.vulkanmod.vulkan.DeviceInfo;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static net.vulkanmod.Initializer.getVersion;

@Mixin(value = DebugScreenOverlay.class, priority = 999)
public abstract class DebugHudM {

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
        strings.add("NativeMemory: " + MemoryManager.getInstance().getNativeMemoryMB() + "MB");
        strings.add("DeviceMemory: " + MemoryManager.getInstance().getDeviceMemoryMB() + "MB");
        strings.add("");
        strings.add("VulkanMod " + getVersion());
        strings.add("CPU: " + DeviceInfo.cpuInfo);
        strings.add("GPU: " + Vulkan.getDeviceInfo().deviceName);
        strings.add("Driver: " + Vulkan.getDeviceInfo().driverVersion);
        strings.add("");
        strings.add("Loaded VBOs: " + RHandler.uniqueVBOs.size());
        strings.add("Used Bytes: " + (VirtualBuffer.usedBytes >> 20) + "MB");
        strings.add("Max Size: " + (VirtualBuffer.size_t >> 20) + "MB");
//        strings.add("Allocs: " + VirtualBuffer.allocs);
//        strings.add("allocBytes: " + VirtualBuffer.allocBytes);
        strings.add("subAllocs: " + VirtualBuffer.subAllocs);
//        strings.add("Blocks: " + VirtualBuffer.blocks);
//        strings.add("BlocksBytes: " + VirtualBuffer.blockBytes);
        strings.add("subIncr: " + VirtualBuffer.subIncr);
        strings.add("minRange: " + VirtualBuffer.unusedRangesS);
        strings.add("maxRange: " + VirtualBuffer.unusedRangesM);
        strings.add("unusedRangesCount: " + VirtualBuffer.unusedRangesCount);
        strings.add("minVBOSize: " + VirtualBuffer.allocMin);
        strings.add("maxVBOSize: " + VirtualBuffer.allocMax);
        strings.add("unusedBytes: " + (VirtualBuffer.size_t-VirtualBuffer.usedBytes));
        strings.add("freeRanges: " + (VirtualBuffer.FreeRanges.size()));

        return strings;
    }

    private long getOffHeapMemory() {
        return bytesToMegabytes(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
    }

    /**
     * @author
     */
    @Overwrite
    public void drawGameInformation(PoseStack matrices) {
        List<String> list = this.getGameInformation();
        list.add("");
        boolean bl = this.minecraft.getSingleplayerServer() != null;
        list.add("Debug: Pie [shift]: " + (this.minecraft.options.renderDebugCharts ? "visible" : "hidden") + (bl ? " FPS + TPS" : " FPS") + " [alt]: " + (this.minecraft.options.renderFpsChart ? "visible" : "hidden"));
        list.add("For help: press F3 + Q");

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = 2;
            int m = 2 + j * i;

            GuiBatchRenderer.fill(matrices, 1, m - 1, 2 + k + 1, m + j - 1, -1873784752);
        }
        GuiBatchRenderer.endBatch();

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (Strings.isNullOrEmpty(string)) continue;
            int j = this.font.lineHeight;
            int k = this.font.width(string);
            int l = 2;
            int m = 2 + j * i;

            GuiBatchRenderer.drawString(this.font, bufferSource, matrices, string, 2.0f, (float)m, 0xE0E0E0);
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
