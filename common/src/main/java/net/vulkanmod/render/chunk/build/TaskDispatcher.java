package net.vulkanmod.render.chunk.build;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.util.thread.ProcessorMailbox;
import net.vulkanmod.render.chunk.*;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;

public class TaskDispatcher {
    private static final Logger LOGGER = LogUtils.getLogger();

    private int highPriorityQuota = 2;

    private final Queue<Runnable> toUpload = Queues.newLinkedBlockingDeque();
    public final ThreadBuilderPack fixedBuffers;

    //TODO volatile?
    private boolean stopThreads;
    private Thread[] threads;
    private int idleThreads;
    private final Queue<ChunkTask> highPriorityTasks = Queues.newConcurrentLinkedQueue();
    private final Queue<ChunkTask> lowPriorityTasks = Queues.newConcurrentLinkedQueue();

    public TaskDispatcher() {
        this.fixedBuffers = new ThreadBuilderPack();

        this.stopThreads = true;
    }

    public void createThreads() {
        if(!this.stopThreads)
            return;

        this.stopThreads = false;

        int j = Math.max((Runtime.getRuntime().availableProcessors() - 1) / 2, 1);

        this.threads = new Thread[j];

        for (int i = 0; i < j; i++) {
            ThreadBuilderPack builderPack = new ThreadBuilderPack();
            Thread thread = new Thread(
                    () -> runTaskThread(builderPack));

            this.threads[i] = thread;
            thread.start();
        }
    }

    private void runTaskThread(ThreadBuilderPack builderPack) {
        while(!this.stopThreads) {
            ChunkTask task = this.pollTask();

            if(task == null)
                synchronized (this) {
                    try {
                        this.idleThreads++;
                        this.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    this.idleThreads--;
                }

            if(task == null)
                continue;

            task.doTask(builderPack);
        }
    }

    public void schedule(ChunkTask chunkTask) {
        if(chunkTask == null)
            return;

        if (chunkTask.highPriority) {
                this.highPriorityTasks.offer(chunkTask);
            } else {
                this.lowPriorityTasks.offer(chunkTask);
            }

        synchronized (this) {
            notify();
        }
    }

    @Nullable
    private ChunkTask pollTask() {
        ChunkTask task = this.highPriorityTasks.poll();

        if(task == null)
            task = this.lowPriorityTasks.poll();

        return task;
    }

    public void stopThreads() {
        if(this.stopThreads)
            return;

        this.stopThreads = true;

        synchronized (this) {
            notifyAll();
        }

        for (Thread thread : this.threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public boolean uploadAllPendingUploads() {

        Runnable runnable;
        boolean flag = false;
        while((runnable = this.toUpload.poll()) != null) {
            flag = true;
            runnable.run();
        }

        AreaUploadManager.INSTANCE.submitUploads();

        return flag;
    }

    public void scheduleSectionUpdate(RenderSection section, EnumMap<TerrainRenderType, UploadBuffer> uploadBuffers) {
        this.toUpload.add(
                () -> this.doSectionUpdate(section, uploadBuffers)
        );
    }

    private void doSectionUpdate(RenderSection section, EnumMap<TerrainRenderType, UploadBuffer> uploadBuffers) {
        ChunkArea renderArea = section.getChunkArea();
        DrawBuffers drawBuffers = renderArea.getDrawBuffers();

        for(TerrainRenderType renderType : TerrainRenderType.VALUES) {
            UploadBuffer uploadBuffer = uploadBuffers.get(renderType);

            if(uploadBuffer != null) {
                drawBuffers.upload(uploadBuffer, section.getDrawParameters(renderType));
            } else {
                section.getDrawParameters(renderType).reset(renderArea);
            }
        }
    }

    public void scheduleUploadChunkLayer(RenderSection section, TerrainRenderType renderType, UploadBuffer uploadBuffer) {
        this.toUpload.add(
                () -> this.doUploadChunkLayer(section, renderType, uploadBuffer)
        );
    }

    private void doUploadChunkLayer(RenderSection section, TerrainRenderType renderType, UploadBuffer uploadBuffer) {
        ChunkArea renderArea = section.getChunkArea();
        DrawBuffers drawBuffers = renderArea.getDrawBuffers();

        drawBuffers.upload(uploadBuffer, section.getDrawParameters(renderType));
    }

    public int getIdleThreadsCount() {
        return this.idleThreads;
    }

    public void clearBatchQueue() {
        while(!this.highPriorityTasks.isEmpty()) {
            ChunkTask chunkTask = this.highPriorityTasks.poll();
            if (chunkTask != null) {
                chunkTask.cancel();
            }
        }

        while(!this.lowPriorityTasks.isEmpty()) {
            ChunkTask chunkTask = this.lowPriorityTasks.poll();
            if (chunkTask != null) {
                chunkTask.cancel();
            }
        }

//        this.toBatchCount = 0;
    }

    public String getStats() {
//        this.toBatchCount = this.highPriorityTasks.size() + this.lowPriorityTasks.size();
//        return String.format("tB: %03d, toUp: %02d, FB: %02d", this.toBatchCount, this.toUpload.size(), this.freeBufferCount);
        return String.format("iT: %d", this.idleThreads);
    }

}
