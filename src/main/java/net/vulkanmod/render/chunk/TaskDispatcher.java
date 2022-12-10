package net.vulkanmod.render.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.util.thread.ProcessorMailbox;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

public class TaskDispatcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_WORKERS_32_BIT = 4;
    private static final VertexFormat VERTEX_FORMAT = DefaultVertexFormat.BLOCK;
    private static final int MAX_HIGH_PRIORITY_QUOTA = 2;
    private final Queue<ChunkTask> toBatchHighPriority = Queues.newLinkedBlockingDeque();
    private final Queue<ChunkTask> toBatchLowPriority = Queues.newLinkedBlockingDeque();
    private int highPriorityQuota = 2;
    private final LinkedBlockingDeque<ChunkBufferBuilderPack> freeBuffers;
    private final Queue<Runnable> toUpload = Queues.newLinkedBlockingDeque();
    private volatile int toBatchCount;
    private volatile int freeBufferCount;
    final ChunkBufferBuilderPack fixedBuffers;
    private final ProcessorMailbox<Runnable> mailbox;
    private final Executor executor;

    public TaskDispatcher(Executor executor, ChunkBufferBuilderPack fixedBuffers) {
        int j = Math.max((Runtime.getRuntime().availableProcessors() - 1) >> 1, 1);
        this.fixedBuffers = fixedBuffers;
        List<ChunkBufferBuilderPack> list = Lists.newArrayListWithExpectedSize(j);

        try {
            for(int i1 = 0; i1 < j; ++i1) {
                list.add(new ChunkBufferBuilderPack());
            }
        } catch (OutOfMemoryError outofmemoryerror) {
            LOGGER.warn("Allocated only {}/{} buffers", list.size(), j);
            int j1 = Math.min(list.size() * 2 / 3, list.size() - 1);

            for(int k1 = 0; k1 < j1; ++k1) {
                list.remove(list.size() - 1);
            }

            System.gc();
        }

//        this.freeBuffers = Queues.newArrayDeque(list);
        this.freeBuffers = Queues.newLinkedBlockingDeque(list);
        this.freeBufferCount = this.freeBuffers.size();
        this.executor = executor;
        this.mailbox = ProcessorMailbox.create(executor, "Chunk Renderer");
//        this.mailbox.tell(this::runTask);
    }

    private void runTask() {
        if (!this.freeBuffers.isEmpty()) {
            ChunkTask task = this.pollTask();
            if (task != null) {
                ChunkBufferBuilderPack chunkbufferbuilderpack = this.freeBuffers.poll();

                this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
                this.freeBufferCount = this.freeBuffers.size();

                CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName(task.name(), () -> {
                    return task.doTask(chunkbufferbuilderpack);
                }), this.executor).thenCompose((p_194416_) -> {
                    return p_194416_;
                }).whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwable, "Batching chunks"));
                    } else {
                        this.mailbox.tell(() -> {
                            if (result == ChunkTask.Result.SUCCESSFUL) {
                                chunkbufferbuilderpack.clearAll();
                            } else {
                                chunkbufferbuilderpack.discardAll();
                            }

                            this.freeBuffers.add(chunkbufferbuilderpack);
                            this.freeBufferCount = this.freeBuffers.size();
                            this.runTask();
                        });
                    }
                });

//                if(chunkbufferbuilderpack == null) return;
//                CompletableFuture<ChunkTask.Result> future = task.doTask(chunkbufferbuilderpack);
//                ChunkTask.Result result;
//                try {
////                    result = future.get(5000, TimeUnit.MILLISECONDS);
//                    result = future.get();
//                } catch (InterruptedException | ExecutionException e) {
//                    throw new RuntimeException(e);
//                }
//
//                if (result == ChunkTask.Result.SUCCESSFUL) {
//                    chunkbufferbuilderpack.clearAll();
//                } else {
//                    chunkbufferbuilderpack.discardAll();
//                }
//
//                this.freeBuffers.add(chunkbufferbuilderpack);
//                this.freeBufferCount = this.freeBuffers.size();
//
//                this.executor.execute(this::runTask);

            }
        }
    }

    @Nullable
    private ChunkTask pollTask() {
        if (this.highPriorityQuota <= 0) {
            ChunkTask chunkTask = this.toBatchLowPriority.poll();
            if (chunkTask != null) {
                this.highPriorityQuota = 2;
                return chunkTask;
            }
        }

        ChunkTask chunkTask = this.toBatchHighPriority.poll();
        if (chunkTask != null) {
            --this.highPriorityQuota;
            return chunkTask;
        } else {
            this.highPriorityQuota = 2;
            return this.toBatchLowPriority.poll();
        }
    }

    public void schedule(ChunkTask chunkTask) {
        this.mailbox.tell(() -> {
            if (chunkTask.highPriority) {
                this.toBatchHighPriority.offer(chunkTask);
            } else {
                this.toBatchLowPriority.offer(chunkTask);
            }

            this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
            this.runTask();
        });


//        this.executor.execute(() -> {
//            if (chunkTask.highPriority) {
//                this.toBatchHighPriority.add(chunkTask);
//            } else {
//                this.toBatchLowPriority.add(chunkTask);
//            }
//
//            this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
//            this.runTask();
//        });
    }

    public void uploadAllPendingUploads() {

        if(!this.toUpload.isEmpty()) WorldRenderer.getInstance().setNeedsUpdate();

        Runnable runnable;
        while((runnable = this.toUpload.poll()) != null) {
            runnable.run();
        }

//        //TODO later: make a proper thread manager

    }

    public CompletableFuture<Void> scheduleUploadChunkLayer(BufferBuilder.RenderedBuffer renderedBuffer, VertexBuffer vertexBuffer) {
        return CompletableFuture.runAsync(() -> {
        }, this.toUpload::add).thenCompose((p_199940_) -> {
            return this.doUploadChunkLayer(renderedBuffer, vertexBuffer);
        });
//        this.toUpload.add(() -> doUploadChunkLayer(renderedBuffer, vertexBuffer));
//        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> doUploadChunkLayer(BufferBuilder.RenderedBuffer renderedBuffer, VertexBuffer vertexBuffer) {
        vertexBuffer.upload(renderedBuffer);
        return CompletableFuture.completedFuture(null);
    }

    public void dispose() {
        this.clearBatchQueue();
        this.mailbox.close();
        this.freeBuffers.clear();
    }

    public void clearBatchQueue() {
        while(!this.toBatchHighPriority.isEmpty()) {
            ChunkTask chunkTask = this.toBatchHighPriority.poll();
            if (chunkTask != null) {
                chunkTask.cancel();
            }
        }

        while(!this.toBatchLowPriority.isEmpty()) {
            ChunkTask chunkTask = this.toBatchLowPriority.poll();
            if (chunkTask != null) {
                chunkTask.cancel();
            }
        }

        this.toBatchCount = 0;
    }

    public String getStats() {
        this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
        return String.format("tB: %03d, toUp: %02d, FB: %02d", this.toBatchCount, this.toUpload.size(), this.freeBufferCount);
    }

}
