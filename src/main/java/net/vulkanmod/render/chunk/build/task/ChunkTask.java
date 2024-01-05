package net.vulkanmod.render.chunk.build.task;

import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.RenderRegion;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ChunkTask {
    public static final boolean BENCH = true;

    protected static TaskDispatcher taskDispatcher;

    public static BuildTask createBuildTask(RenderSection renderSection, RenderRegion renderRegion, boolean highPriority) {
        return new BuildTask(renderSection, renderRegion, highPriority);
    }

    protected AtomicBoolean cancelled = new AtomicBoolean(false);
    protected final RenderSection renderSection;
    public boolean highPriority = false;

    ChunkTask(RenderSection renderSection) {
        this.renderSection = renderSection;
    }

    public abstract String name();

    public abstract CompletableFuture<Result> doTask(BuilderResources builderResources);

    public void cancel() {
        this.cancelled.set(true);
    }

    public static void setTaskDispatcher(TaskDispatcher dispatcher) {
        taskDispatcher = dispatcher;
    }
    
    public enum Result {
        CANCELLED,
        SUCCESSFUL
    }
}
