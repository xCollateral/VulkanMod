package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.vulkanmod.vulkan.shader.UniformState;

import java.util.Arrays;

public class InlineUniformBlock {
    private final int binding;
    private final UniformState[] uniformState;
//    private final int[] staticUniformOffsets;

    private final int size_t;
    //Order of varGS dicated staticInlineUniformOffsets
    public InlineUniformBlock(int binding, UniformState... uniformState) {


        this.binding = binding;
        this.uniformState = uniformState;

        this.size_t = Arrays.stream(uniformState).mapToInt(UniformState::getByteSize).sum();


//        staticUniformOffsets = new Int2ObjectOpenHashMap<>(uniformState.length);
//        int offset = 0;
//        for(var a : uniformState)
//        {
//            staticUniformOffsets.put(offset, a);
//            offset+=a.getByteSize();
//        }

    }

    public int binding() {
        return binding;
    }

//    public Int2ObjectOpenHashMap<UniformState> getStaticUniformOffsets() {
//        return staticUniformOffsets;
//    }

    public UniformState[] uniformState() {
        return uniformState;
    }

    public int size_t() {
        return size_t;
    }
}
