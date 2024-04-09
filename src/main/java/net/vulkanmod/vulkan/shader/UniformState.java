package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.nio.ByteBuffer;

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
    FogColor("vec4",4,4);

    public final String type;
    public final int align;
    public final int size;
    int currentOffset;

    long currentHash;
    private final MappedBuffer mappedBufferPtr;


    UniformState(String vec3, int align, int size) {

        type = vec3;
        this.align = align;
        this.size = size;
        mappedBufferPtr = new MappedBuffer(size * Float.BYTES);
    }


    public boolean needsUpdate(int srcHash)
    {
        if(currentHash!=srcHash)
        {
            currentHash=srcHash;
            return true;
        }
        return false;
    }


    public static void resetAll()
    {
        for (UniformState uniformState : UniformState.values()) {
            uniformState.currentHash = 0;
        }
    }

    public ByteBuffer buffer() {
        return this.mappedBufferPtr.buffer;
    }

    public MappedBuffer getMappedBufferPtr() {
        return mappedBufferPtr;
    }
}
