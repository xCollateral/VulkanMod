package net.vulkanmod.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.*;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryDedicatedAllocateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.Checks.check;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.util.vma.Vma.vmaGetVirtualBlockStatistics;
import static org.lwjgl.vulkan.VK10.*;

public class VirtualBuffer {
    private static long virtualBlockBufferSuperSet;
    private static int size_t;
    public static int subIncr;
    public static int usedBytes;
    public static int allocs;
    public static int blocks;
    public static long blockBytes;
    private static final ObjectArrayList<VkBufferPointer> FreeRanges = new ObjectArrayList<>(1024);
    public static int subAllocs;
    public static long unusedRangesS;
    public static long unusedRangesM;
    public static int unusedRangesCount;
    public static long allocMin;
    public static long allocMax;
    public static long bufferPointerSuperSet;
//    public static int allocBytes;

    static {
        initBufferSuperSet(0x20000000);
    }
    public static void reset()
    {
        FreeRanges.clear();
    }


    private static void createBuffer(long size, MemoryStack stack, PointerBuffer pBuffer) {

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
        bufferInfo.sType$Default();
        bufferInfo.pNext(NULL);
        bufferInfo.size(size);
        bufferInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
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

        VkMemoryDedicatedAllocateInfo vkMemoryDedicatedAllocateInfo = VkMemoryDedicatedAllocateInfo.calloc(stack)
                .buffer(pBuffer.get(0))
                .sType$Default();

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack)
                .sType$Default()
                .pNext(vkMemoryDedicatedAllocateInfo.address())
                .allocationSize(size)
                .memoryTypeIndex(0);

//            Vma.vmaCreateBuffer(Vulkan.getAllocator(), bufferInfo, allocationInfo, pBuffer, pAllocation, null);
        nvkAllocateMemory(Vulkan.getDevice(), allocInfo.address(), NULL, pAllocation.address0());
    }

    private static void initBufferSuperSet(int size) {

        if(size_t==size) return;

        try(MemoryStack stack = MemoryStack.stackPush())
        {
            VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.malloc(stack)
                    .size(size)
                    .flags(0)
                    .pAllocationCallbacks(null);
            PointerBuffer pAlloc = stack.mallocPointer(1);
            PointerBuffer pBuffer = stack.mallocPointer(1);

            createBackingBuffer(size, stack, pAlloc, pBuffer);

            PointerBuffer block = stack.pointers(virtualBlockBufferSuperSet);
            Vma.vmaCreateVirtualBlock(blockCreateInfo, block);
            virtualBlockBufferSuperSet = block.get(0);




        }
    }

    private static void createBackingBuffer(int size, MemoryStack stack, PointerBuffer pAlloc, PointerBuffer pBuffer) {
        createBuffer(size, stack, pBuffer);
        allocMem(size, stack, pBuffer, pAlloc);

        vkBindBufferMemory(Vulkan.getDevice(), pBuffer.get(0), pAlloc.get(0), 0);

        bufferPointerSuperSet= pBuffer.get(0);

        size_t= size;
    }

    //TODO: draw Call Coalesing: if /get/grab two unaliggned blocks and attemot to merge them together if fit within teh same (relative) alignment
    static VkBufferPointer addSubIncr(int size) {
        size=alignAs(size);
//        VkBufferPointer bufferPointer = checkforFreeable(size);
//        if(bufferPointer!=null) return reallocSubIncr(bufferPointer);

        try(MemoryStack stack = MemoryStack.stackPush())
        {
            VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.malloc(stack)
                    .size(size)
                    .alignment(VBO.size_t)
                    .flags((checkforFreeable(size)) ? VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT : VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT)
                    .pUserData(NULL);
            PointerBuffer pAlloc = stack.mallocPointer(1);

            LongBuffer pOffset = stack.longs(0);
            subIncr += size;

            int a = Vma.nvmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo.address(), pAlloc.address0(), (pOffset.get(0)));
            if(a== VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY) {
                throw new RuntimeException("Out of Mem!: " + usedBytes +"-->"+(usedBytes+size));
            }
            long allocation = pAlloc.get(0);
//            Vma.vmaSetVirtualAllocationUserData(virtualBlockBufferSuperSet, allocation, 0);
            subAllocs++;
            VmaVirtualAllocationInfo vmaVirtualAllocationInfo = setOffsetRangesStats(allocation, stack);

            updateStatistics(stack);
            return new VkBufferPointer(pAlloc.get(0), (int) vmaVirtualAllocationInfo.offset(), (int) vmaVirtualAllocationInfo.size());
        }
    }

    private static int alignAs(int size) {
        return size + (VBO.size_t - (size-1&VBO.size_t-1) - 1);
    }

    private static boolean checkforFreeable(int size) {
        for(VkBufferPointer bufferPointer : FreeRanges)
        {
            if(bufferPointer.sizes<=size) {
                return FreeRanges.remove(FreeRanges.indexOf(bufferPointer))!=null;
            }
        }
        return false;
    }

    private static void updateStatistics(MemoryStack stack) {
        VmaDetailedStatistics vmaStatistics = VmaDetailedStatistics.malloc(stack);
        vmaCalculateVirtualBlockStatistics(virtualBlockBufferSuperSet, vmaStatistics);
        vmaGetVirtualBlockStatistics(virtualBlockBufferSuperSet, vmaStatistics.statistics());
        usedBytes= (int) vmaStatistics.statistics().allocationBytes();
        allocs=vmaStatistics.statistics().allocationCount();
//        allocBytes= (int) vmaStatistics.statistics().allocationBytes();
        blocks=vmaStatistics.statistics().blockCount();
        blockBytes=vmaStatistics.statistics().blockBytes();
        unusedRangesS=vmaStatistics.unusedRangeSizeMin();
        unusedRangesM=vmaStatistics.unusedRangeSizeMax();
        unusedRangesCount=vmaStatistics.unusedRangeCount();
        allocMin=vmaStatistics.allocationSizeMin();
        allocMax=vmaStatistics.allocationSizeMax();
    }

    public static void addFreeableRange(VkBufferPointer bufferPointer)
    {
        if(bufferPointer==null) return;
        if(bufferPointer.allocation==0) return;
//        if(bufferPointer.sizes==0) return;
        Vma.vmaVirtualFree(virtualBlockBufferSuperSet, bufferPointer.allocation);
        subAllocs--;
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
