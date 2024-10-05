package net.vulkanmod.render.profiling;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.GuiRenderer;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.task.ChunkTask;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProfilerOverlay {
    private static final long POLL_PERIOD = 100000000;

    public static ProfilerOverlay INSTANCE;
    public static boolean shouldRender;

    private static Profiler.ProfilerResults lastResults;
    private static long lastPollTime;
    private static float frametime;

    private static String buildStats;
//    private static int node = -1;

    private final Minecraft minecraft;
    private final Font font;

    public ProfilerOverlay(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.font = minecraft.font;
    }

    public static void createInstance(Minecraft minecraft) {
        INSTANCE = new ProfilerOverlay(minecraft);
    }

    public static void toggle() {
        shouldRender = !shouldRender;
        Profiler.setActive(shouldRender);
    }

    public static void onKeyPress(int key) {
//        int v = key - InputConstants.KEY_0;
//        node = v >= 0 && v <= 15 ? v-1 : node;
    }

    public void render(GuiGraphics guiGraphics) {
        GuiRenderer.guiGraphics = guiGraphics;
        GuiRenderer.pose = guiGraphics.pose();

        List<String> infoList = this.buildInfo();

        final int lineHeight = 9;
        final int xOffset = 2;
        final int backgroundColor = ColorUtil.ARGB.pack(0.05f, 0.05f, 0.05f, 0.3f);
        final int textColor = ColorUtil.ARGB.pack(1f, 1f, 1f, 1f);

        Objects.requireNonNull(this.font);

        RenderSystem.enableBlend();
        GuiRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < infoList.size(); ++i) {
            String line = infoList.get(i);
            if (!Strings.isNullOrEmpty(line)) {
                int textWidth = this.font.width(line);
                int yPosition = xOffset + lineHeight * i;
                GuiRenderer.fill(
                        1, yPosition - 1,
                        xOffset + textWidth + 1, yPosition + lineHeight - 1,
                        0, backgroundColor);
            }
        }

        GuiRenderer.endBatch();
        RenderSystem.disableBlend();

        for (int i = 0; i < infoList.size(); ++i) {
            String line = infoList.get(i);
            if (!Strings.isNullOrEmpty(line)) {
                int yPosition = xOffset + lineHeight * i;
                GuiRenderer.drawString(
                        this.font, Component.literal(line),
                        xOffset, yPosition,
                        textColor, false);
            }
        }
    }

    private List<String> buildInfo() {
        List<String> list = new ArrayList<>();
        list.add("");
        list.add("Profiler");

        this.updateResults();

        if (lastResults == null)
            return list;

        int fps = Math.round(1000.0f / frametime);
        list.add(String.format("FPS: %d Frametime: %.3f", fps, frametime));
        list.add("");

        for (Profiler.Result result : lastResults.getPartialResults()) {
            list.add(String.format("%s: %.3f", result.name, result.value));
        }

        list.add("");
        list.add(MemoryManager.getInstance().getHeapStats());

        // Section build stats
        list.add("");
        list.add("");
        list.add(String.format("Build time: %.0fms", BuildTimeProfiler.getDeltaTime()));

        if (ChunkTask.BENCH)
            list.add(buildStats);

        return list;
    }

    private void updateResults() {
        if ((System.nanoTime() - lastPollTime) < POLL_PERIOD && lastResults != null)
            return;

        Profiler.ProfilerResults results = Profiler.getMainProfiler().getProfilerResults();
        if (results == null)
            return;

        frametime = results.getResult().value;
        lastResults = results;
        lastPollTime = System.nanoTime();

        if (ChunkTask.BENCH)
            buildStats = this.getBuildStats();
    }

    private String getBuildStats() {
        BuilderResources[] resourcesArray = WorldRenderer.getInstance().getTaskDispatcher().getResourcesArray();
        int totalTime = 0;
        int buildCount = 0;

        for (BuilderResources resources : resourcesArray) {
            totalTime += resources.getTotalBuildTime();
            buildCount += resources.getBuildCount();
        }

        return String.format("Builders time: %dms avg %dms (%d builds)",
                totalTime, totalTime / resourcesArray.length, buildCount);
    }
}