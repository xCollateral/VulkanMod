package net.vulkanmod.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.config.Config;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.*;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryDedicatedAllocateInfo;

import java.nio.LongBuffer;

import static net.vulkanmod.render.chunk.WorldRenderer.lastViewDistance;
import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class VirtualBuffer {
    private static long virtualBlockBufferSuperSet;
    public static int size_t;
    public static int subIncr;
    public static int usedBytes;
    public static final ObjectArrayList<VkBufferPointer> FreeRanges = new ObjectArrayList<>(1024);
    public static int subAllocs;
    public static long unusedRangesS;
    public static long unusedRangesM;
    public static int unusedRangesCount;
    public static long allocMin;
    public static long allocMax;
    public static long bufferPointerSuperSet;
    public static boolean bound=false;
    private static long bufferPtrBackingAlloc;
//    public static int allocBytes;

    private static final int initsize_t;

    static {
        initsize_t = (lastViewDistance * lastViewDistance) * 24 * Config.baseAlignSize;
        initBufferSuperSet(initsize_t);
    }
    //Could just Set Allocation to 2GB and not bother with resizing as initially allocated space is treated as "Reserved Space" and not used space
    //However it would get in the way of other programs and naively assumes the GPU has 2GB available which may cause issues with older GPUs/iGPUs
    public static void reset(int i)
    {
        if(i==0) i=size_t;

//        Drawer.skipRendering=true;

        subIncr=0;
        subAllocs=0;
        usedBytes=0;
        FreeRanges.clear();
        freeThis(i);
//        if(size_t==i) return;

       /* for(VBO a : RHandler.uniqueVBOs)
        {
            a.close();
        }*/





        bound=false;



        initBufferSuperSet(i);
//        Drawer.skipRendering=false;
    }

    private static void freeThis(int i) {
        Vma.vmaClearVirtualBlock(virtualBlockBufferSuperSet);
        if(size_t!=i){
            Vma.vmaDestroyVirtualBlock(virtualBlockBufferSuperSet);
            vkFreeMemory(Vulkan.getDevice(), bufferPtrBackingAlloc, null);
            vkDestroyBuffer(Vulkan.getDevice(), bufferPointerSuperSet, null);
        }
    }


    private static void createBuffer(long size, MemoryStack stack, PointerBuffer pBuffer) {

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack)
                .sType$Default()
                .pNext(NULL)
                .size(size)
                .usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

//
//            vkGetBufferMemoryRequirements(Vulkan.getDevice(), )
//            vkAllocateMemory(d)

        nvkCreateBuffer(Vulkan.getDevice(), bufferInfo.address(), NULL, pBuffer.address());
    }

    private static void allocMem(long size, MemoryStack stack, PointerBuffer pBuffer, PointerBuffer pAllocation) {
//        VmaAllocationCreateInfo allocationInfo  = VmaAllocationCreateInfo.callocStack(stack);
//        allocationInfo.usage(VMA_MEMORY_USAGE_CPU_ONLY);
//        allocationInfo.requiredFlags(VK_MEMORY_HEAP_DEVICE_LOCAL_BIT);


//        VkMemoryRequirements memRequirements = VkMemoryRequirements.mallocStack(stack);
//        vkGetBufferMemoryRequirements(Vulkan.getDevice(), pBuffer.get(0), memRequirements);
        VkMemoryDedicatedAllocateInfo vkMemoryDedicatedAllocateInfo = VkMemoryDedicatedAllocateInfo.mallocStack(stack)
                .buffer(pBuffer.get(0))
                .image(0)
                .pNext(0)
                .sType$Default();


        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.mallocStack(stack)
                .sType$Default()
                .pNext(vkMemoryDedicatedAllocateInfo.address())
                .allocationSize(size)
                .memoryTypeIndex(0);

//            Vma.vmaCreateBuffer(Vulkan.getAllocator(), bufferInfo, allocationInfo, pBuffer, pAllocation, null);
        nvkAllocateMemory(Vulkan.getDevice(), allocInfo.address(), NULL, pAllocation.address0());
    }

    private static void initBufferSuperSet(int size) {


        try(MemoryStack stack = MemoryStack.stackPush())
        {
            VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.malloc(stack)
                    .size(size)
                    .flags(0)
                    .pAllocationCallbacks(null);
            PointerBuffer pAlloc = stack.mallocPointer(1);
            PointerBuffer pBuffer = stack.mallocPointer(1);

            createBackingBuffer(size, stack, pAlloc, pBuffer);

            PointerBuffer block = stack.mallocPointer(1);
            Vma.vmaCreateVirtualBlock(blockCreateInfo, block);
            virtualBlockBufferSuperSet = block.get(0);

            size_t=size;
            bound=true;



        }
    }

    private static void createBackingBuffer(int size, MemoryStack stack, PointerBuffer pAlloc, PointerBuffer pBuffer) {
        createBuffer(size, stack, pBuffer);
        allocMem(size, stack, pBuffer, pAlloc);

        vkBindBufferMemory(Vulkan.getDevice(), pBuffer.get(0), pAlloc.get(0), 0);

        bufferPointerSuperSet= pBuffer.get(0);
        bufferPtrBackingAlloc= pAlloc.get(0);

//        size_t= size;
    }

    //TODO: draw Call Coalescing: if get/grab two unaligned blocks and attempt to merge them together if fit within the same (relative) alignment
    static VkBufferPointer addSubIncr(int size) {
        size=alignAs(size);
//        VkBufferPointer bufferPointer = checkforFreeable(size);
//        if(bufferPointer!=null) return reallocSubIncr(bufferPointer);

        if(size_t<=usedBytes+size)
        {
            System.out.println(size_t+"-->"+(usedBytes+size)+"-->"+size_t*2);
            WorldRenderer.setNeedsUpdate();
            WorldRenderer.allChanged(size_t+size_t);
        }

        try(MemoryStack stack = MemoryStack.stackPush())
        {
            var a = (checkforFreeable(size));
            VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.malloc(stack)
                    .size(size)
                    .alignment(Config.vboAlignmentActual)
                    .flags((a!=null) ? VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT : VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT)
                    .pUserData(NULL);
            PointerBuffer pAlloc = stack.mallocPointer(1);

            LongBuffer pOffset = a!=null ? stack.longs(a.i2) : null;
            subIncr += size;
            usedBytes+=size;
            Vma.vmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo, pAlloc, pOffset);

            long allocation = pAlloc.get(0);

//            Vma.vmaSetVirtualAllocationUserData(virtualBlockBufferSuperSet, allocation, 0);
            subAllocs++;
            VmaVirtualAllocationInfo allocCreateInfo1 = VmaVirtualAllocationInfo.malloc(stack);

            if(allocation==0)
            {
                /*VirtualBuffer.reset(size_t*2);
                VmaVirtualAllocationCreateInfo allocCreateInfo2 = VmaVirtualAllocationCreateInfo.malloc(stack)
                        .size(size)
                        .alignment(0)
                        .flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT)
                        .pUserData(NULL);
//                pOffset.put(0, 0);
                Vma.vmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo2, pAlloc, null);
                allocation = pAlloc.get(0);*/
                System.out.println(size_t+"-->"+(size_t-usedBytes)+"-->"+(usedBytes+size)+"-->"+size+"-->"+size_t+initsize_t);
                WorldRenderer.setNeedsUpdate();
                WorldRenderer.allChanged(size_t+initsize_t);
                pAlloc=stack.mallocPointer(1);
                Vma.vmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo, pAlloc, stack.longs(0));
                allocation=pAlloc.get(0);

            }
            vmaGetVirtualAllocationInfo(virtualBlockBufferSuperSet, allocation, allocCreateInfo1);

            updateStatistics(stack);
            return new VkBufferPointer(pAlloc.get(0), (int) allocCreateInfo1.offset(), (int) allocCreateInfo1.size());
        }
    }

    private static int alignAs(int size) {
        return size + (Config.vboAlignmentActual - (size-1&Config.vboAlignmentActual-1) - 1);
    }

    private static VkBufferPointer checkforFreeable(int size) {
        for (int i = 0; i < FreeRanges.size(); i++) {
            VkBufferPointer bufferPointer = FreeRanges.get(i);
            if (bufferPointer.size_t >= size) {
                return FreeRanges.remove(i);
            }
        }
        return null;
    }

    private static void updateStatistics(MemoryStack stack) {
        VmaDetailedStatistics vmaStatistics = VmaDetailedStatistics.malloc(stack);
        vmaCalculateVirtualBlockStatistics(virtualBlockBufferSuperSet, vmaStatistics);
//        vmaGetVirtualBlockStatistics(virtualBlockBufferSuperSet, vmaStatistics.statistics());
//        usedBytes= (int) vmaStatistics.statistics().allocationBytes();
//        allocs=vmaStatistics.statistics().allocationCount();
//        allocBytes= (int) vmaStatistics.statistics().allocationBytes();
//        blocks=vmaStatistics.statistics().blockCount();
//        blockBytes=vmaStatistics.statistics().blockBytes();
        unusedRangesS=vmaStatistics.unusedRangeSizeMin();
        unusedRangesM=vmaStatistics.unusedRangeSizeMax();
        unusedRangesCount=vmaStatistics.unusedRangeCount();
        allocMin=vmaStatistics.allocationSizeMin();
        allocMax=vmaStatistics.allocationSizeMax();
    }

    public static void addFreeableRange(VkBufferPointer bufferPointer)
    {
        if(usedBytes==0)return;
        if(bufferPointer==null) return;
        if(bufferPointer.allocation==0) return;
//        if(bufferPointer.sizes==0) return;
        Vma.vmaVirtualFree(virtualBlockBufferSuperSet, bufferPointer.allocation);
        subAllocs--;
        usedBytes-=bufferPointer.size_t;
        addToFreeableRanges(bufferPointer);
    }

    private static void addToFreeableRanges(VkBufferPointer bufferPointer) {
        FreeRanges.add(bufferPointer);
    }

    private static VmaVirtualAllocationInfo setOffsetRangesStats(long allocation, MemoryStack stack) {
        VmaVirtualAllocationInfo allocCreateInfo = VmaVirtualAllocationInfo.malloc(stack);
        Vma.vmaGetVirtualAllocationInfo(virtualBlockBufferSuperSet, allocation, allocCreateInfo);
        return allocCreateInfo;
    }
}
