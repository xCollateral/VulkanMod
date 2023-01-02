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

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public class VirtualBuffer {
    private static long virtualBlockBufferSuperSet;
    public static final int size_t = Integer.MAX_VALUE;
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
    public static final ObjectArrayList<VkBufferPointer> activeRanges = new ObjectArrayList<>(1024);

    static {
        initsize_t = (size_t);
        initBufferSuperSet(size_t);
    }

    public static void reset(int i)
    {


        subIncr=0;
        subAllocs=0;
        usedBytes=0;
        FreeRanges.clear();
        activeRanges.clear();
        freeThis(i);
//
    }

    private static void freeThis(int i) {
        Vma.vmaClearVirtualBlock(virtualBlockBufferSuperSet);
        {
//            Vma.vmaDestroyVirtualBlock(virtualBlockBufferSuperSet);
//            vkFreeMemory(Vulkan.getDevice(), bufferPtrBackingAlloc, null);
//            vkDestroyBuffer(Vulkan.getDevice(), bufferPointerSuperSet, null);
        }
    }


    private static void createBuffer(long size, MemoryStack stack, PointerBuffer pBuffer) {

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack)
                .sType$Default()
                .pNext(NULL)
                .size(size)
                .usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);


        nvkCreateBuffer(Vulkan.getDevice(), bufferInfo.address(), NULL, pBuffer.address());
    }

    private static void allocMem(long size, MemoryStack stack, PointerBuffer pBuffer, PointerBuffer pAllocation) {
//
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

//            size_t=size;
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

    //TODO: draw Call Coalescing: Use two unaligned blocks and attempt to merge them together if fit within the same (relative) alignment
    static VkBufferPointer addSubIncr(int index, int actualSize) {
        int alignedSize=alignAs(actualSize);

        if(size_t<=usedBytes+alignedSize)
        {
            System.out.println(size_t+"-->"+(usedBytes+alignedSize)+"-->"+size_t*2);
            WorldRenderer.setNeedsUpdate();
            WorldRenderer.allChanged(size_t+size_t);
        }

        try(MemoryStack stack = MemoryStack.stackPush())
        {
            var a = (checkforFreeable(alignedSize));
            VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.malloc(stack)
                    .size(alignedSize)
                    .alignment(Config.vboAlignmentActual)
                    .flags((a!=null) ? 0 : VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT)
                    .pUserData(NULL);

            PointerBuffer pAlloc = stack.mallocPointer(1);

            LongBuffer pOffset = a!=null ? stack.longs(a.i2()) : null;
            subIncr += alignedSize;
            usedBytes+=alignedSize;
            Vma.vmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo, pAlloc, pOffset);

            long allocation = pAlloc.get(0);


            subAllocs++;
            VmaVirtualAllocationInfo allocCreateInfo1 = VmaVirtualAllocationInfo.malloc(stack);

            if(allocation==0)
            {
                System.out.println(size_t+"-->"+(size_t-usedBytes)+"-->"+(usedBytes+alignedSize)+"-->"+alignedSize+"-->"+size_t+initsize_t);
                WorldRenderer.setNeedsUpdate();
                WorldRenderer.allChanged(size_t+initsize_t);
                pAlloc=stack.mallocPointer(1);
                Vma.vmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo, pAlloc, stack.longs(0));
                allocation=pAlloc.get(0);

            }
            vmaGetVirtualAllocationInfo(virtualBlockBufferSuperSet, allocation, allocCreateInfo1);

            updateStatistics(stack);
            VkBufferPointer vkBufferPointer = new VkBufferPointer(index, (int) allocCreateInfo1.offset(), (int) allocCreateInfo1.size(), pAlloc.get(0));
            activeRanges.add(vkBufferPointer);
            return vkBufferPointer;
        }
    }

    public static boolean isAlreadyLoaded(int index, int remaining) {
        VkBufferPointer vkBufferPointer = getActiveRangeFromIdx(index);
        if(vkBufferPointer==null) return false;
        if(vkBufferPointer.size_t()>=remaining)
        {
            return true;
        }
        addFreeableRange(index, vkBufferPointer);
        return false;

    }

    private static int alignAs(int size) {
        return size + (Config.vboAlignmentActual - (size-1&Config.vboAlignmentActual-1) - 1);
    }

    private static VkBufferPointer checkforFreeable(int size) {
        for (int i = 0; i < FreeRanges.size(); i++) {
            VkBufferPointer bufferPointer = FreeRanges.get(i);
            if (bufferPointer.size_t() >= size) {
                return FreeRanges.remove(i);
            }
        }
        return null;
    }

    //Not Supported on LWJGL 3.3.1
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

    public static void addFreeableRange(int index, VkBufferPointer bufferPointer)
    {
        if(usedBytes==0)return;
        if(bufferPointer==null) return;
        if(bufferPointer.allocation()==0) return;
//        if(bufferPointer.sizes==0) return;
        Vma.vmaVirtualFree(virtualBlockBufferSuperSet, bufferPointer.allocation());
        for (int i = 0; i < activeRanges.size(); i++) {
            VkBufferPointer vkBufferPointer = activeRanges.get(i);
            if (vkBufferPointer.index() == index) {
                activeRanges.remove(i);
                break;
            }
        }
        subAllocs--;
        usedBytes-=bufferPointer.size_t();
        addToFreeableRanges(bufferPointer);
    }

    private static VkBufferPointer getActiveRangeFromIdx(int index) {
        for (VkBufferPointer vkBufferPointer : activeRanges) {
            if (vkBufferPointer.index() == index) {
                return vkBufferPointer;
            }
        }
        return null;
    }

    private static void addToFreeableRanges(VkBufferPointer bufferPointer) {
        FreeRanges.add(bufferPointer);
    }

}
