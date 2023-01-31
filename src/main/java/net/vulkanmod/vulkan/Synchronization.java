package net.vulkanmod.vulkan;

import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Synchronization {
    private static final int allocSize = 20000;
    private static final LongBuffer fences = MemoryUtil.memAllocLong(allocSize);
    private static int idx = 0;

    public synchronized static void addFence(long fence) {
        fences.put(idx, fence);
        idx++;
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
            vkWaitForFences(device, longs, true, VUtil.UINT64_MAX);
        }
        if(idx - i > 0) {
            longs = new long[idx - i];
            fences.position(i);
            fences.get(longs, 0, idx - i);
            vkWaitForFences(device, longs, true, VUtil.UINT64_MAX);
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
