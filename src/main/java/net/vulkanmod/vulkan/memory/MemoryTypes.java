package net.vulkanmod.vulkan.memory;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.vulkan.VkMemoryHeap;
import org.lwjgl.vulkan.VkMemoryType;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public enum MemoryTypes {
    GPU_MEM(true, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),

    BAR_MEM(true, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
    RAM_MEM(false, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, 0);

    private final long maxSize;
    private long usedBytes;
    private final int flags;

    MemoryTypes(boolean useVRAM, int... optimalFlags) {

//        this.maxSize = maxSize;
//        this.resizableBAR = size > 0xD600000;

        final boolean useVRAM1 = useVRAM || !hasHeapFlag(0);
        for (int optimalFlagMask : optimalFlags) {
            final String requiredFlagString = getMemoryTypeFlags(optimalFlagMask);
            Initializer.LOGGER.info("Requesting Flags: "+ requiredFlagString + "...");
            for (VkMemoryType memoryType : DeviceManager.memoryProperties.memoryTypes()) {

                VkMemoryHeap memoryHeap = DeviceManager.memoryProperties.memoryHeaps(memoryType.heapIndex());
                final int availableFlags = memoryType.propertyFlags();
                final int extractedFlags = optimalFlagMask & availableFlags;
                final boolean hasRequiredFlags = extractedFlags == optimalFlagMask;

                if(availableFlags!=0)  Initializer.LOGGER.info(
                        "AvailableFlags: " + getMemoryTypeFlags(availableFlags) + "\t --> " + "SelectedFlags: " + getMemoryTypeFlags(extractedFlags));

                final boolean hasMemType = useVRAM1 == ((availableFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0);
                if (hasRequiredFlags && hasMemType) {
                    this.maxSize = memoryHeap.size();
                    this.flags = optimalFlagMask;

                    Initializer.LOGGER.info("Found Requested Flags for: "+this.name()+"\n"
                            + "     Memory Heap Index/Bank: "+ memoryType.heapIndex() +"\n"
                            + "     IsVRAM: "+ memoryHeap.flags() +"\n"
                            + "     MaxSize: " + this.maxSize+ " Bytes" +"\n"
                            + "     AvailableFlags:" + getMemoryTypeFlags(availableFlags) + "\n"
                            + "     EnabledFlags:" + requiredFlagString);
//                    this.mappable = ((this.flags = optimalFlagMask) & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0;

                    return;
                }
            }
            Initializer.LOGGER.error(requiredFlagString + " Not Found, using next Fallback");
//            optimalFlagMask ^= optimalFlags[currentFlagCount]; //remove each Property bit, based on varargs priority ordering from right to left
        }

        throw new RuntimeException("Unsupported MemoryType: "+this.name() + ": Try updating your driver and/or Vulkan version");

//        VkMemoryType memoryType = DeviceManager.memoryProperties.memoryTypes(0);
//        VkMemoryHeap memoryHeap = DeviceManager.memoryProperties.memoryHeaps(0);
//        this.maxSize = memoryHeap.size();
//        this.flags = memoryType.propertyFlags();
//            return;
//
//

    }

    private String getMemoryTypeFlags(int memFlags)
    {
        if(memFlags==0) return " | NONE/SKIPPING";
        final int[] x = new int[]{1,2,4,8,16};
        StringBuilder memTypeFlags = new StringBuilder();
        for (int memFlag : x) {
            boolean hasMemFlag = (memFlag & memFlags)!=0;
            if(hasMemFlag)
            {
                switch (memFlag){
                    case VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT -> memTypeFlags.append(" | DEVICE_LOCAL");
                    case VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT -> memTypeFlags.append(" | HOST_VISIBLE");
                    case VK_MEMORY_PROPERTY_HOST_COHERENT_BIT -> memTypeFlags.append(" | HOST_COHERENT");
                    case VK_MEMORY_PROPERTY_HOST_CACHED_BIT -> memTypeFlags.append(" | HOST_CACHED");
                    case VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT -> memTypeFlags.append(" | LAZILY_ALLOCATED");
                }
            }
        }
        return memTypeFlags.toString();
    }

    private static boolean hasHeapFlag(int heapFlag) {
        for(VkMemoryHeap memoryHeap : DeviceManager.memoryProperties.memoryHeaps()) {
            if(memoryHeap.flags()==heapFlag) return true;
        }
        return false;
    }

    void createBuffer(Buffer buffer, int size)
    {

        //Allow resizable bar heaps to bypass the staging buffer
        final int usage = buffer.usage | (this.mappable() ? 0 : VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT);

        MemoryManager.getInstance().createBuffer(buffer, size, usage, this.flags);
        this.usedBytes+=size;
    }

//    void addSubCopy(Buffer buffer, long bufferSize, ByteBuffer byteBuffer)
//    {
//        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
//
//        var a = new SubCopyCommand()
//        if(subCpyContiguous)
//    }
//
//    void executeSubCopy(Buffer srcBuffer, Buffer dstBuffer)
//    {
//
//    }
    void copyToBuffer(Buffer buffer, int bufferSize, ByteBuffer byteBuffer)
    {
         if(!this.mappable()){
             StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
             stagingBuffer.copyBuffer(bufferSize, byteBuffer);
             DeviceManager.getTransferQueue().copyBufferCmd(stagingBuffer.id, stagingBuffer.offset, buffer.getId(), buffer.getUsedBytes(), bufferSize);
         }
         else VUtil.memcpy(byteBuffer, buffer.data.getByteBuffer(0, buffer.bufferSize), bufferSize, buffer.getUsedBytes());
    }


    void freeBuffer(Buffer buffer)
    {
        MemoryManager.getInstance().addToFreeable(buffer);
        this.usedBytes-=buffer.bufferSize;
    }


//    abstract void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer);
//    abstract void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer);

    /**
     * Replace data from byte 0
     */
    public void uploadBuffer(Buffer buffer, ByteBuffer byteBuffer, int dstOffset)
    {
      if(!this.mappable())
      {
          int bufferSize = byteBuffer.remaining();
          StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
          stagingBuffer.copyBuffer(bufferSize, byteBuffer);

          DeviceManager.getTransferQueue().copyBufferCmd(stagingBuffer.id, stagingBuffer.offset, buffer.getId(), dstOffset, bufferSize);
      }

      else VUtil.memcpy(byteBuffer, buffer.data.getByteBuffer(0, buffer.bufferSize), byteBuffer.remaining(), dstOffset);
    }

    final boolean mappable() { return (this.flags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0; }

    public int usedBytes() { return (int) (this.usedBytes >> 20); }

    public int maxSize() { return (int) (this.maxSize >> 20); }

//    public int checkUsage(int usage) {
//        return (usage & this.flags) !=0 ? usage : this.flags;
//    }
}
