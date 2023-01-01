package net.vulkanmod.render;


import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Vec3i;
import net.vulkanmod.render.chunk.TaskDispatcher;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Drawer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.StagingBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryDedicatedAllocateInfo;

import java.util.concurrent.CompletableFuture;

import static net.vulkanmod.vulkan.Vulkan.copyStagingtoLocalBuffer;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;
import static org.lwjgl.vulkan.VK10.*;

public class RHandler
{

//    private static final LevelRenderer worldRenderer = Minecraft.getInstance().levelRenderer;
    //    public static ObjectArrayList<VBO> drawCommands=new ObjectArrayList<>(1024);
    public static ObjectArrayList<VBO> uniqueVBOs =new ObjectArrayList<>(1024);
    public static int loadedVBOs;
    public static int totalVBOs;
    public static double camX;
    public static double camY;
    public static double camZ;

    private static final int MAX_VBOS = 2048;
    public static VkDrawIndexedIndirectCommand.Buffer drawCommands=VkDrawIndexedIndirectCommand.create(MemoryUtil.nmemAlignedAlloc(8, 20* MAX_VBOS), MAX_VBOS);
    public static  long drawCmdBuffer;
    private static  long drawCmdAlloc;
    private static final int size_t=0x10000;
//    public static final long indirectDrawCommandsBuffer=1;
//    public static ChunkGrid viewArea;
   /* public static final VkDrawIndexedIndirectCommand defStruct=VkDrawIndexedIndirectCommand.malloc(MemoryStack.stackGet())
            .indexCount(0)
            .vertexOffset(0)
            .firstIndex(0)
            .firstInstance(0)
            .instanceCount(1);;;*/
    static
    {
        setupInidrectBuffer();
    }

    static void setupInidrectBuffer()
    {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pBuffer = stack.pointers(drawCmdBuffer);
            PointerBuffer pAllocation = stack.pointers(drawCmdAlloc);

            extracted(stack, pBuffer, pAllocation);

            drawCmdBuffer = pBuffer.get(0);
            drawCmdAlloc = pAllocation.get(0);
            /*PointerBuffer pBuffer1 = stack.pointers(drawCmdBuffer[1]);
            PointerBuffer pAllocation1 = stack.pointers(drawCmdAlloc[1]);
            extracted(stack, pBuffer1, pAllocation1);
            drawCmdBuffer[1] = pBuffer1.get(0);
            drawCmdAlloc[1] = pAllocation1.get(0);*/
        }
    }

    private static void extracted(MemoryStack stack, PointerBuffer pBuffer, PointerBuffer pAllocation) {
        //            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
//            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
//            bufferInfo.size(size);
//            bufferInfo.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT);
//            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
//
//            VmaAllocationCreateInfo vmaAllocationCreateInfo = VmaAllocationCreateInfo.callocStack(stack).requiredFlags(VK_MEMORY_HEAP_DEVICE_LOCAL_BIT);
//            extracted(Vma.vmaCreateBuffer(Vulkan.getAllocator(), bufferInfo, vmaAllocationCreateInfo, pBuffer, pAllocation, null),"");


        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
        bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(size_t);
        bufferInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT  | VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
//
//            vkGetBufferMemoryRequirements(Vulkan.getDevice(), )
//            vkAllocateMemory(d)

//        nvkCreateBuffer(Vulkan.getDevice(), bufferInfo.address(), NULL, pBuffer.address());

        nvkCreateBuffer(Vulkan.getDevice(), bufferInfo.address(), NULL, pBuffer.address());
//
//                VmaAllocationCreateInfo allocationInfo  = VmaAllocationCreateInfo.callocStack(stack);
//        allocationInfo.usage(Vma.VMA_MEMORY_USAGE_CPU_TO_GPU);
//        allocationInfo.requiredFlags(VK_MEMORY_HEAP_DEVICE_LOCAL_BIT);
//
//
        VkMemoryDedicatedAllocateInfo vkMemoryDedicatedAllocateInfo = VkMemoryDedicatedAllocateInfo.calloc(stack)
                .buffer(pBuffer.get(0))
                .sType$Default();
//
        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
        allocInfo.sType$Default();
        allocInfo.pNext(vkMemoryDedicatedAllocateInfo.address());
        allocInfo.allocationSize(size_t);
        allocInfo.memoryTypeIndex(0);
        nvkAllocateMemory(Vulkan.getDevice(), allocInfo.address(), NULL, pAllocation.address0());

//            allocMem(stack, pBuffer, pAllocation);
        vkBindBufferMemory(Vulkan.getDevice(), pBuffer.get(0), pAllocation.get(0), 0);
    }

    public static void AllocIndirectCmds()
    {

        if(drawCommands.position()==0) return; long a=0;
        /*for(VkDrawIndexedIndirectCommand indirectCommand : drawCommands)
        {
            MemoryUtil.memCopy(indirectCommand.address(), memAddress0(packedIndirectCmdBuffer)+a, a+=20);
        }*/

        {
//            MemoryUtil.memCopy(drawCommands.address0(), memAddress0(packedIndirectCmdBuffer), VBO.idx*20);

        }
        //packedIndirectCmdBuffer.rewind();
        StagingBuffer stagingVkBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());

        VUtil.memcpy(memByteBuffer(stagingVkBuffer.data.get(0), stagingVkBuffer.getBufferSize()), drawCommands.address0(), stagingVkBuffer.getUsedBytes(), uniqueVBOs.size()*20);

        stagingVkBuffer.offset = stagingVkBuffer.usedBytes;
        stagingVkBuffer.usedBytes += uniqueVBOs.size()*20;

        //createVertexBuffer(vertexSize, vertexCount, byteBuffer);

        //createVertexBuffer(vertexSize, vertexCount, byteBuffer);

        copyStagingtoLocalBuffer(stagingVkBuffer.getId(), stagingVkBuffer.offset, drawCmdBuffer, 0, uniqueVBOs.size() *20L);
    }
    private static Vec3i origin;
    public static int calls;
    private static long indirectAllocation;


    public static Vec3i obtainWorldOrigin(double e, double f) {
        origin = origin == null ? new Vec3i(e, 0, f) : origin;
        return origin;
    }

    public static void setWorldOrigin(int d, int f) {
       origin = new Vec3i(d,0,  f);

    }

//    public static void addWorldOrigin(int i, int j) {
//        if(origin!=null) origin.add(-i, 0,  -j);
//    }

    public static CompletableFuture<Void> uploadVBO(VBO vbo, BufferBuilder.RenderedBuffer buffers, boolean sort)
    {
//        if(buffers==null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() ->
                vbo.upload_(buffers, sort), WorldRenderer.taskDispatcher.toUpload::add);
    }

    public static CompletableFuture<Void> uploadVBO(int index, BufferBuilder.RenderedBuffer buffers, boolean sort)
    {
        if(TaskDispatcher.resetting) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() ->
                getVBOFromIndex(index).upload_(buffers, sort), WorldRenderer.taskDispatcher.toUpload::add);
    }

    public static VBO getVBOFromIndex(int index) {
        /*for(VBO vbo : uniqueVBOs)
        {
            if(vbo.index==index) return vbo;
        }
        return uniqueVBOs.get(0);

        */
        return WorldRenderer.chunkGrid.chunks[index].vbo;
    }

    public static void freeVBO(int index) {
        getVBOFromIndex(index).close();
    }
}
