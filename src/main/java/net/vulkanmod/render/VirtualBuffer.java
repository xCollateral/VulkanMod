package net.vulkanmod.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.config.Config;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.*;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

public final class VirtualBuffer {
    public final long bufferPointerSuperSet;
    private final long virtualBlockBufferSuperSet;
    public final long size_t;
    //    public int subIncr;
    public int usedBytes;
    public int subAllocs;
    public long unusedRangesS;
    public long unusedRangesM;
    public int unusedRangesCount;
    public long allocMin;
    public long allocMax;
//    public boolean bound=false;
    private final long  bufferPtrBackingAlloc;
//    private static long  PUSERDATA=nmemAlignedAlloc(8, 8);
//    private static long  PFNALLOCATION=nmemAlignedAlloc(8, 32);
//    private static long  PFNREALLOCATION=nmemAlignedAlloc(8, 48);
//    private static long  PFNFREE=nmemAlignedAlloc(8, 16);
//    private static long  PFNINTERNALALLOCATION=nmemAlignedAlloc(8, 32);
//    private static long  PFNINTERNALFREE=nmemAlignedAlloc(8, 32);
//    public static int allocBytes;

    public final ObjectArrayList<VkBufferPointer> FreeRanges = new ObjectArrayList<>(1024);

    public final ObjectArrayList<VkBufferPointer> activeRanges = new ObjectArrayList<>(1024);
    private final int vkBufferType;


    VirtualBuffer(long size_t, int type)
    {
        this.size_t=size_t;
        this.vkBufferType =type;


        try(MemoryStack stack = MemoryStack.stackPush())
        {
            VmaVirtualBlockCreateInfo blockCreateInfo = VmaVirtualBlockCreateInfo.malloc(stack);
            blockCreateInfo.size(this.size_t);
            blockCreateInfo.flags(0);
            blockCreateInfo.pAllocationCallbacks(null);

            PointerBuffer pAlloc = stack.mallocPointer(1);
            PointerBuffer pBuffer = stack.mallocPointer(1);

            bufferPointerSuperSet = createBackingBuffer(stack, pAlloc, pBuffer);
            bufferPtrBackingAlloc=pAlloc.get(0);

            PointerBuffer block = stack.mallocPointer(1);
            Vma.vmaCreateVirtualBlock(blockCreateInfo, block);
            virtualBlockBufferSuperSet = block.get(0);

//            size_t=size;
//            bound=true;



        }
    }

    public void reset(int i)
    {


//        subIncr=0;
        subAllocs=0;
        usedBytes=0;
        FreeRanges.clear();
        activeRanges.clear();
        freeThis(i);
//
    }

    private void freeThis(int i) {
        Vma.vmaClearVirtualBlock(virtualBlockBufferSuperSet);
        {
//            Vma.vmaDestroyVirtualBlock(virtualBlockBufferSuperSet);
//            vkFreeMemory(Vulkan.getDevice(), bufferPtrBackingAlloc, null);
//            vkDestroyBuffer(Vulkan.getDevice(), bufferPointerSuperSet, null);
        }
    }


    private void createBuffer(MemoryStack stack, PointerBuffer pBuffer) {

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
        bufferInfo.sType$Default();
        bufferInfo.pNext(NULL);
        bufferInfo.size(this.size_t);
        bufferInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | this.vkBufferType);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);


        nvkCreateBuffer(Vulkan.getDevice(), bufferInfo.address(), NULL, pBuffer.address());
    }

    private void allocMem(MemoryStack stack, PointerBuffer pBuffer, PointerBuffer pAllocation) {
//
        VkMemoryDedicatedAllocateInfo vkMemoryDedicatedAllocateInfo = VkMemoryDedicatedAllocateInfo.mallocStack(stack);
        vkMemoryDedicatedAllocateInfo.buffer(pBuffer.get(0));
        vkMemoryDedicatedAllocateInfo.image(0);
        vkMemoryDedicatedAllocateInfo.pNext(0);
        vkMemoryDedicatedAllocateInfo.sType$Default();


        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.mallocStack(stack);
        allocInfo.sType$Default();
        allocInfo.pNext(vkMemoryDedicatedAllocateInfo.address());
        allocInfo.allocationSize(this.size_t);
        allocInfo.memoryTypeIndex(0);

//        VkAllocationFunction vkAllocationFunction = VkAllocationFunction.create(PFNALLOCATION);
//        VkReallocationFunction vkReallocationFunction = VkReallocationFunction.create(PFNREALLOCATION);
//        VkFreeFunction vkFreeFunction = VkFreeFunction.create(PFNFREE);
//        VkInternalAllocationNotification vkInternalAllocationNotification = VkInternalAllocationNotification.create(PFNINTERNALALLOCATION);
//        VkInternalFreeNotification vkInternalAllocationNotification1 = VkInternalFreeNotification.create(PFNINTERNALFREE);
//
//        VkAllocationCallbacks allocationCallbacks = VkAllocationCallbacks.calloc(stack)
//                        .pUserData(PUSERDATA)
//                        .pfnAllocation(vkAllocationFunction)
//                        .pfnReallocation(vkReallocationFunction)
//                        .pfnFree(vkFreeFunction)
//                        .pfnInternalAllocation(vkInternalAllocationNotification)
//                        .pfnInternalFree(vkInternalAllocationNotification1);

//            Vma.vmaCreateBuffer(Vulkan.getAllocator(), bufferInfo, allocationInfo, pBuffer, pAllocation, null);
        nvkAllocateMemory(Vulkan.getDevice(), allocInfo.address(), NULL, pAllocation.address0());
    }

    private long createBackingBuffer(MemoryStack stack, PointerBuffer pAlloc, PointerBuffer pBuffer) {
        createBuffer(stack, pBuffer);
        allocMem(stack, pBuffer, pAlloc);

        vkBindBufferMemory(Vulkan.getDevice(), pBuffer.get(0), pAlloc.get(0), 0);

        return pBuffer.get(0);

//        size_t= size;
    }

    //TODO: Possible Fragmentation Workaround/Draw Call Coalescing: Use two unaligned blocks and attempt to merge them together if fit within the same (relative) alignment
     VkBufferPointer addSubIncr(int index, int actualSize) {
        int alignedSize=alignAs(actualSize);

        if(size_t<=usedBytes+alignedSize)
        {
            System.out.println(size_t+"-->"+(usedBytes+alignedSize)+"-->"+size_t*2);
            WorldRenderer.setNeedsUpdate();
            WorldRenderer.allChanged((int) (size_t+size_t));
        }

        try(MemoryStack stack = MemoryStack.stackPush())
        {
            var a = (checkforFreeable(alignedSize));
            VmaVirtualAllocationCreateInfo allocCreateInfo = VmaVirtualAllocationCreateInfo.malloc(stack);
            allocCreateInfo.size(alignedSize);
            allocCreateInfo.alignment(Config.vboAlignmentActual);
            allocCreateInfo.flags((a!=null) ? 0 : VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT);
            allocCreateInfo.pUserData(NULL);

            PointerBuffer pAlloc = stack.mallocPointer(1);

            LongBuffer pOffset = a!=null ? stack.longs(a.i2()) : null;
//            subIncr += alignedSize;
            usedBytes+=alignedSize;
            Vma.vmaVirtualAllocate(virtualBlockBufferSuperSet, allocCreateInfo, pAlloc, pOffset);

            long allocation = pAlloc.get(0);


            subAllocs++;
            VmaVirtualAllocationInfo allocCreateInfo1 = VmaVirtualAllocationInfo.malloc(stack);

            if(allocation==0)
            {
                System.out.println(size_t+"-->"+(size_t-usedBytes)+"-->"+(usedBytes+alignedSize)+"-->"+alignedSize+"-->"+size_t);
                WorldRenderer.setNeedsUpdate();
                WorldRenderer.allChanged((int) (size_t));
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

    public boolean isAlreadyLoaded(int index, int remaining) {
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

    private VkBufferPointer checkforFreeable(int size) {
        for (int i = 0; i < FreeRanges.size(); i++) {
            VkBufferPointer bufferPointer = FreeRanges.get(i);
            if (bufferPointer.size_t() >= size) {
                return FreeRanges.remove(i);
            }
        }
        return null;
    }

    //Not Supported on LWJGL 3.3.1
    private void updateStatistics(MemoryStack stack) {
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

    public void addFreeableRange(int index, VkBufferPointer bufferPointer)
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

    private VkBufferPointer getActiveRangeFromIdx(int index) {
        for (VkBufferPointer vkBufferPointer : activeRanges) {
            if (vkBufferPointer.index() == index) {
                return vkBufferPointer;
            }
        }
        return null;
    }

    private void addToFreeableRanges(VkBufferPointer bufferPointer) {
        FreeRanges.add(bufferPointer);
    }

    //Makes Closing the game very slow
    public void cleanUp()
    {
        Vma.vmaClearVirtualBlock(virtualBlockBufferSuperSet);
        {
            Vma.vmaDestroyVirtualBlock(virtualBlockBufferSuperSet);
            vkFreeMemory(Vulkan.getDevice(), bufferPtrBackingAlloc, null);
            vkDestroyBuffer(Vulkan.getDevice(), bufferPointerSuperSet, null);
        }
        System.out.println("FREED");
    }

}
