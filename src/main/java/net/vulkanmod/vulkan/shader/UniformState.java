package net.vulkanmod.vulkan.shader;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.UniformBuffers;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.EnumSet;

public enum UniformState {
    ModelViewMat("mat4",4, 16),
    ProjMat("mat4",4, 16),
    MVP("mat4",4, 16),
    TextureMat("mat4",4, 16),
    EndPortalLayers("int",1,1),
    FogStart("float",1,1),
    FogEnd("float",1,1),
    LineWidth("float",1,1),
    GameTime("float",1,1),
    AlphaCutout("float",1,1),
    ScreenSize("vec2",2,2),

    //    InSize("vec2",2,2),
//    OutSize("vec2",2,2),
//    BlurDir("vec2",2,2),
//    ColorModulate("vec4",4,4),
//
    Radius("float",1,1),
    Light0_Direction("vec4",4,4),
    Light1_Direction("vec4",4,4),
    ChunkOffset("vec3",4,3),
    ColorModulator("vec4",4,4),
    FogColor("vec4",4,4),
    SkyColor("vec4", 4, 4);

    public final String type;
    public final int align;
    public final int size;
    int currentOffset;

    int currentHash, newHash;
    private final MappedBuffer mappedBufferPtr;
    private boolean needsUpdate;

    private final Int2IntOpenHashMap hashedUniformOffsetMap;


    UniformState(String vec3, int align, int size) {

        type = vec3;
        this.align = align;
        this.size = size;
        mappedBufferPtr = new MappedBuffer(size * Float.BYTES);
        hashedUniformOffsetMap = new Int2IntOpenHashMap(16);
    }


    public boolean forcePushUpdate(int srcHash, UniformBuffers uniformBuffer, int frame)
    {
        boolean isUniqueHash = !this.hashedUniformOffsetMap.containsKey(srcHash);
        if(isUniqueHash) {
            final int align1 = VUtil.align(this.size * 4, this.align);
            this.hashedUniformOffsetMap.put(srcHash, uniformBuffer.getUsedBytes());
            MemoryUtil.memCopy(this.getMappedBufferPtr().ptr, uniformBuffer.getPointer(frame), align1);
            uniformBuffer.updateOffset(align1);
            this.needsUpdate=true;
        }
        this.currentHash=srcHash;
        Renderer.getDrawer().updateUniformOffset2(this.hashedUniformOffsetMap.get(this.currentHash)/64);



        return isUniqueHash;
    }

    public boolean needsUpdate2(int srcHash)
    {
        //hash the Uniform contents, then stroe the current offset

        //TODO: if need uodate then also update uniform offset/index
        // or perhaps pushing uniforms here instead
        this.newHash =srcHash;


        return this.needsUpdate = this.newHash!=this.currentHash;
    }

    public boolean needsUpdate(int srcHash)
    {
        //hash the Uniform contents, then stroe the current offset

        //TODO: if need uodate then also update uniform offset/index
        // or perhaps pushing uniforms here instead
        this.newHash =srcHash;


        return this.needsUpdate = !this.hashedUniformOffsetMap.containsKey(srcHash);
    }
    public void needsUpdateOverride(boolean shouldOverride)
    {
        this.needsUpdate=shouldOverride;
    }

    public boolean requiresUpdate() { return this.needsUpdate| this.currentHash!=this.newHash; }

    public void resetAndUpdate()
    {
        this.currentHash=newHash;
        this.needsUpdate=false;
        if(!hashedUniformOffsetMap.containsKey(currentHash))
        {
           hashedUniformOffsetMap.put(currentHash, currentOffset);
        }
    }
    public void resetAndUpdateForced()
    {
        this.needsUpdate=false;
    }

    public static void resetAll()
    {
        for (UniformState uniformState : EnumSet.of(MVP, ProjMat, ModelViewMat, TextureMat)) {
            uniformState.currentHash = 0;
            uniformState.currentOffset = 0;
            uniformState.needsUpdate=false;
            uniformState.hashedUniformOffsetMap.clear();
        }
    }

    public ByteBuffer buffer() {
        return this.mappedBufferPtr.buffer;
    }

    public MappedBuffer getMappedBufferPtr() {
        return mappedBufferPtr;
    }

    public void storeCurrentOffset(int currentOffset) {
        this.currentOffset=currentOffset;
    }

    public int getCurrentOffset() {
        return this.hashedUniformOffsetMap.get(this.currentHash);
    }

    public int getOffsetFromHash() {
        return this.hashedUniformOffsetMap.get(this.currentHash);
    }

    public boolean hasUniqueHash() {
        return this.hashedUniformOffsetMap.containsKey(this.newHash);
    }

    public void updateOffsetState() {

        if(hashedUniformOffsetMap.containsKey(this.newHash))
        {
            Renderer.getDrawer().updateUniformOffset2((hashedUniformOffsetMap.get(this.newHash)/64));
        }
    }
}
