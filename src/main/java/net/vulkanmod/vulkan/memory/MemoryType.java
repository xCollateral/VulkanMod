package net.vulkanmod.vulkan.memory;

import java.nio.ByteBuffer;

public abstract class MemoryType {
    protected Type type;

    abstract void createBuffer(Buffer buffer, int size);
    abstract void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer);


//    abstract void copyToBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer);
    abstract void copyFromBuffer(Buffer buffer, long bufferSize, ByteBuffer byteBuffer);

    /**
     * Replace data from byte 0
     */
    abstract void uploadBuffer(Buffer buffer, ByteBuffer byteBuffer);

    abstract boolean mappable();

    public Type getType() {
       return this.type;
    }

    public enum Type {
        DEVICE_LOCAL,
        HOST_LOCAL
    }
}
