package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Synchronization {
    private static final int allocSize = 10000;
    private static final LongBuffer fences = MemoryUtil.memAllocLong(allocSize);
    private static final PointerBuffer freeableCmdBuffers = MemoryUtil.memAllocPointer(allocSize);
    private static int idx = 0;

    public synchronized static void addFence(long fence) {
        fences.put(idx, fence);
        idx++;
    }

    // I don't know how Java handling CPU processes, but I'm hope about Java preemptive
    // Also, we can handle errors of fences.
    // May drop bit GPU performance, but increase CPU efficiency.
    public synchronized static void preemptiveWaitFence(long[] longs) {
        VkDevice device = Vulkan.getDevice();
        for (int i=0;i<longs.length;i++) {
            int status = VK_NOT_READY;
            do {
                // in JS should have `setImmediate` to be preemptive
            } while((status = vkGetFenceStatus(device, longs[i])) == VK_NOT_READY);
        }
    }

    public synchronized static void waitFences() {
//        TransferQueue.resetCurrent();

        if(idx == 0) return;

        VkDevice device = Vulkan.getDevice();

        fences.limit(idx);

        int count = 50;
        long[] longs = new long[count];
        int i;
        for (i = 0; i + count - 1 < idx; i += count) {
            fences.position(i);
            fences.get(longs, 0, count);
            preemptiveWaitFence(longs);
            //vkWaitForFences(device, longs, true, VUtil.UINT64_MAX);
        }
        if(idx - i > 0) {
            longs = new long[idx - i];
            fences.position(i);
            fences.get(longs, 0, idx - i);
            preemptiveWaitFence(longs);
            //vkWaitForFences(device, longs, true, VUtil.UINT64_MAX);
        }

//        Profiler profiler = new Profiler("sync");
//        profiler.start();
//        vkWaitForFences(device, fences, true, VUtil.UINT64_MAX);
//        profiler.push("wait");
//        vkResetCommandPool(device, commandPool, 0);
//        profiler.push("free");

//        profiler.push("end");
//        profiler.end();

        TransferQueue.resetCurrent();

        fences.limit(allocSize);
        idx = 0;
    }

}
