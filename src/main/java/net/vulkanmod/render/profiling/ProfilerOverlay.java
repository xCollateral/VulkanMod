package net.vulkanmod.render.profiling;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.task.ChunkTask;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.gui.GuiBatchRenderer;
import net.vulkanmod.vulkan.memory.MemoryManager;

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

    private static int node = -1;

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

    Minecraft minecraft;
    Font font;

    public ProfilerOverlay(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.font = minecraft.font;
    }

    public void render(PoseStack poseStack) {
        this.drawProfilerInfo(poseStack);
    }

    protected void drawProfilerInfo(PoseStack poseStack) {
        List<String> list = buildInfo();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        GuiBatchRenderer.beginBatch(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (!Strings.isNullOrEmpty(string)) {
                Objects.requireNonNull(this.font);
                int j = 9;
                int k = this.font.width(string);
                int m = 2 + j * i;
                GuiBatchRenderer.fill(poseStack, 1, m - 1, 2 + k + 1, m + j - 1, -1873784752);

            }
        }
        GuiBatchRenderer.endBatch();
        RenderSystem.disableBlend();

        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);
            if (!Strings.isNullOrEmpty(string)) {
                Objects.requireNonNull(this.font);
                int j = 9;
                int m = 2 + j * i;

                GuiBatchRenderer.drawString(this.font, bufferSource, poseStack, string, 2.0f, (float) m, 0xE0E0E0);
            }
        }

        bufferSource.endBatch();

    }

    private List<String> buildInfo() {
        List<String> list = new ArrayList<>();
        list.add("");
        list.add("Profiler");

        updateResults();

        if (lastResults == null)
            return list;

        int fps = Math.round(1000.0f / frametime);

        list.add(String.format("FPS: %d Frametime: %.3f", fps, frametime));
        list.add("");

        var partialResults = lastResults.getPartialResults();
        for (Profiler.Result result : partialResults) {
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
            buildStats = getBuildStats();
    }

    private String getBuildStats() {
        var resourcesArray = WorldRenderer.getInstance().getTaskDispatcher().getResourcesArray();

        int totalTime = 0, buildCount = 0;
        for (BuilderResources resources1 : resourcesArray) {
            totalTime += resources1.getTotalBuildTime();
            buildCount += resources1.getBuildCount();
        }

        return String.format("Builders time: %dms avg %dms (%d builds)",
                totalTime, totalTime / resourcesArray.length, buildCount);
    }
}
