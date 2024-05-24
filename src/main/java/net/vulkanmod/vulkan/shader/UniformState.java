package net.vulkanmod.vulkan.shader;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.UniformBuffer;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.EnumSet;
//TODO; need to replace this with a less rigid system fpr Postprocess uniforms
public enum UniformState {
    ModelViewMat("mat4",4, 16, 512, 256),
    ProjMat("mat4",4, 16, 0, 0),
    MVP("mat4",4, 16, 0, 512),
    TextureMat("mat4",4, 16, 768, 256),
    EndPortalLayers("int",1,1, 0, 0),
    FogStart("float",1,1, 0, 0),
    FogEnd("float",1,1, 0, 0),
    LineWidth("float",1,1, 0, 0),
    GameTime("float",1,1, 0, 0),
    AlphaCutout("float",1,1, 0, 0),
    ScreenSize("vec2",2,2, 0, 0),

    //    InSize("vec2",2,2),
//    OutSize("vec2",2,2),
//    BlurDir("vec2",2,2),
//    ColorModulate("vec4",4,4),
//
    Radius("float",1,1, 0, 0),
    Light0_Direction("vec4",4,4, 0, 0),
    Light1_Direction("vec4",4,4, 0, 0),
    ChunkOffset("vec3",4,3, 0, 0),
    ColorModulator("vec4",4,4, 0, 0),
    FogColor("vec4",4,4, 0, 0),
    SkyColor("vec4", 4, 4, 0, 0);

    public final String type;
    public final int align;
    public final int size;
    private final int bankOffset;
    int currentOffset;

    int currentHash, newHash;
    private final MappedBuffer mappedBufferPtr;
    private final int maxLimit;
    private boolean needsUpdate;

    private final Int2IntOpenHashMap hashedUniformOffsetMap;
    private int usedSize;
    private final int STATE_MSK;


    UniformState(String vec3, int align, int size, int bankOffset, int maxLimit) {

        type = vec3;
        this.align = align;
        this.size = size;
        mappedBufferPtr = new MappedBuffer(size * Float.BYTES);
        this.maxLimit = maxLimit;
        hashedUniformOffsetMap = new Int2IntOpenHashMap(16);
        this.bankOffset=bankOffset;
        this.STATE_MSK= switch (this.name())
        {
            default -> 0;
            case "TextureMat" -> 3;
            case "ModelViewMat" -> 5;
        };
    }


    public int updateBank(UniformBuffer uniformBuffer)
    {
        boolean isUniqueHash = !this.hashedUniformOffsetMap.containsKey(this.newHash);
        if(isUniqueHash) {
            this.usedSize = usedSize % maxLimit;
            this.hashedUniformOffsetMap.put(this.newHash, this.usedSize);
            MemoryUtil.memCopy(this.getMappedBufferPtr().ptr, uniformBuffer.getBasePointer() + this.usedSize + this.bankOffset, getByteSize());
            this.usedSize+= getByteSize();

            this.needsUpdate=true;
        }
        this.currentHash=this.newHash;


        return this.hashedUniformOffsetMap.get(this.currentHash) / getByteSize() << STATE_MSK;
    }

    public int getByteSize() {
        return this.size * Float.BYTES;
    }

    public boolean needsUpdate(int srcHash)
    {
        //hash the Uniform contents, then stroe the current offset

        //TODO: if need uodate then also update uniform offset/index
        // or perhaps pushing uniforms here instead
        this.newHash =srcHash;


        return this.needsUpdate = !this.hashedUniformOffsetMap.containsKey(srcHash);
    }

    public boolean requiresUpdate() { return this.needsUpdate| this.currentHash!=this.newHash; }

    public void resetAndUpdate()
    {
        this.currentHash=newHash;
        this.needsUpdate=false;
    }

    public static void resetAll()
    {
        for (UniformState uniformState : EnumSet.of(MVP, ProjMat, ModelViewMat, TextureMat)) {
            uniformState.currentHash = 0;
            uniformState.currentOffset = 0;
            uniformState.needsUpdate=false;
            uniformState.hashedUniformOffsetMap.clear();
            uniformState.usedSize = 0;
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
        return this.hashedUniformOffsetMap.get(this.currentHash) / getByteSize() << STATE_MSK;
    }

    public int getOffsetFromHash() {
        return this.hashedUniformOffsetMap.get(this.currentHash);
    }

    public boolean hasUniqueHash() {
        return this.hashedUniformOffsetMap.containsKey(this.newHash);
    }

    public void updateOffsetState(UniformBuffer uniformBuffers, int baseAlignment) {

        if(hashedUniformOffsetMap.containsKey(this.newHash))
        {
            Renderer.getDrawer().updateUniformOffset2((hashedUniformOffsetMap.get(this.newHash)/ baseAlignment));
        }
    }
}
