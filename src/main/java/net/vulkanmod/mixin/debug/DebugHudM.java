package net.vulkanmod.mixin.debug;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
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

@Mixin(DebugHud.class)
public abstract class DebugHudM {

    @Shadow @Final private MinecraftClient client;

    @Shadow
    private static long toMiB(long bytes) {
        return 0;
    }

    @Shadow protected abstract List<String> getLeftText();

    @Shadow @Final private TextRenderer textRenderer;

    @Shadow protected abstract List<String> getRightText();

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> redirectList(Object[] elements) {
        ArrayList<String> strings = new ArrayList<>();

        long l = Runtime.getRuntime().maxMemory();
        long m = Runtime.getRuntime().totalMemory();
        long n = Runtime.getRuntime().freeMemory();
        long o = m - n;

        strings.add(String.format("Java: %s %dbit", System.getProperty("java.version"), this.client.is64Bit() ? 64 : 32));
        strings.add(String.format("Mem: % 2d%% %03d/%03dMB", o * 100L / l, toMiB(o), toMiB(l)));
        strings.add(String.format("Allocated: % 2d%% %03dMB", m * 100L / l, toMiB(m)));
        strings.add(String.format("Off-heap: " + getOffHeapMemory() + "MB"));
        strings.add("NativeMemory: " + MemoryManager.getNativeMemory() / (1024 * 1024) + "MB");
        strings.add("DeviceMemory: " + MemoryManager.getDeviceMemory() / (1024 * 1024) + "MB");
        strings.add("");
        strings.add("VulkanMod " + getVersion());
        strings.add("CPU: " + DeviceInfo.cpuInfo);
        strings.add("GPU: " + Vulkan.getDeviceInfo().deviceName);
        strings.add("Driver: " + Vulkan.getDeviceInfo().driverVersion);
        strings.add("");

        return strings;
    }

//    /**
//     * @author
//     */
//    @Overwrite
//    public void renderLeftText(MatrixStack matrices) {
//        List<String> list = this.getLeftText();
//        list.add("");
//        boolean bl = this.client.getServer() != null;
//        list.add("Debug: Pie [shift]: " + (this.client.options.debugProfilerEnabled ? "visible" : "hidden") + (bl ? " FPS + TPS" : " FPS") + " [alt]: " + (this.client.options.debugTpsEnabled ? "visible" : "hidden"));
//        list.add("For help: press F3 + Q");
//
//        RenderSystem.enableBlend();
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
//
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.textRenderer.fontHeight;
//            int k = this.textRenderer.getWidth(string);
//            int l = 2;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.fill(matrices, 1, m - 1, 2 + k + 1, m + j - 1, -1873784752);
//        }
//        GuiBatchRenderer.endBatch();
//
//        VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.textRenderer.fontHeight;
//            int k = this.textRenderer.getWidth(string);
//            int l = 2;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.drawString(this.textRenderer, bufferSource, matrices, string, 2.0f, (float)m, 0xE0E0E0);
//        }
//        bufferSource.draw();
//    }
//
//    /**
//     * @author
//     */
//    @Overwrite
//    public void renderRightText(MatrixStack matrices) {
//        List<String> list = this.getRightText();
//
//        RenderSystem.enableBlend();
//        RenderSystem.setShader(GameRenderer::getPositionColorShader);
//        GuiBatchRenderer.beginBatch(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
//
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.textRenderer.fontHeight;
//            int k = this.textRenderer.getWidth(string);
//            int l = this.client.getWindow().getScaledWidth() - 2 - k;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.fill(matrices, l - 1, m - 1, l + k + 1, m + j - 1, -1873784752);
//        }
//        GuiBatchRenderer.endBatch();
//
//        VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
//        for (int i = 0; i < list.size(); ++i) {
//            String string = list.get(i);
//            if (Strings.isNullOrEmpty(string)) continue;
//            int j = this.textRenderer.fontHeight;
//            int k = this.textRenderer.getWidth(string);
//            int l = this.client.getWindow().getScaledWidth() - 2 - k;
//            int m = 2 + j * i;
//
//            GuiBatchRenderer.drawString(this.textRenderer, bufferSource, matrices, string, (float)l, (float)m, 0xE0E0E0);
//        }
//        bufferSource.draw();
//    }

    private long getOffHeapMemory() {
        return toMiB(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
    }
}
