package net.vulkanmod.render;


import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
    public static ObjectArrayList<VBO> uniqueVBOs =new ObjectArrayList<>(1024);
    public static double camX;
    public static double camY;
    public static double camZ;
    private static final int MAX_VBOS = 2048;
    public static VkDrawIndexedIndirectCommand.Buffer drawCommands=VkDrawIndexedIndirectCommand.create(MemoryUtil.nmemAlignedAlloc(8, 20* MAX_VBOS), MAX_VBOS);
    public static  long drawCmdBuffer;
    public static ObjectArrayList<VBO> retiredVBOs = new ObjectArrayList<>(1024);
    private static  long drawCmdAlloc;
    private static final int size_t=0x10000;

    static
    {
        setupIndirectBuffer();
    }

    static void setupIndirectBuffer()
    {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pBuffer = stack.pointers(drawCmdBuffer);
            PointerBuffer pAllocation = stack.pointers(drawCmdAlloc);

            extracted(stack, pBuffer, pAllocation);

            drawCmdBuffer = pBuffer.get(0);
            drawCmdAlloc = pAllocation.get(0);

        }
    }

    private static void extracted(MemoryStack stack, PointerBuffer pBuffer, PointerBuffer pAllocation) {


        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
        bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(size_t);
        bufferInfo.usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT  | VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);


        nvkCreateBuffer(Vulkan.getDevice(), bufferInfo.address(), NULL, pBuffer.address());

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

        if(drawCommands.position()==0) return;

        StagingBuffer stagingVkBuffer = Vulkan.getStagingBuffer(Drawer.getCurrentFrame());

        VUtil.memcpy2(memByteBuffer(stagingVkBuffer.data.get(0), stagingVkBuffer.getBufferSize()), drawCommands.address0(), stagingVkBuffer.getUsedBytes(), uniqueVBOs.size()*20);

        stagingVkBuffer.offset = stagingVkBuffer.usedBytes;
        stagingVkBuffer.usedBytes += uniqueVBOs.size()*20;

        copyStagingtoLocalBuffer(stagingVkBuffer.getId(), stagingVkBuffer.offset, drawCmdBuffer, 0, uniqueVBOs.size() *20L);
    }


    public static CompletableFuture<Void> uploadVBO(VBO vbo, BufferBuilder.RenderedBuffer buffers, boolean sort)
    {
        return CompletableFuture.runAsync(() ->
                vbo.upload_(buffers, sort), WorldRenderer.taskDispatcher.toUpload::add);
    }

    public static CompletableFuture<Void> uploadVBO(int index, BufferBuilder.RenderedBuffer buffers, boolean sort)
    {
        if(TaskDispatcher.resetting) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() ->
                getVBOFromIndex(index).upload_(buffers, sort), WorldRenderer.taskDispatcher.toUpload::add);
    }

    public static VBO getVBOFromIndex(int index)
    {

        return WorldRenderer.chunkGrid.chunks[index].vbo;
    }

    public static void freeVBO(int index) {
        getVBOFromIndex(index).close();
    }

}
