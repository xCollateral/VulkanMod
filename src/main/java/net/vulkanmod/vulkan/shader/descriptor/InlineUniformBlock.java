package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.vulkanmod.vulkan.shader.UniformState;

import java.util.Arrays;

public class InlineUniformBlock {
    private final int binding;
    private final UniformState[] uniformState;
    private final int size_t;
    //Order of varargs dictates the offset of each Constant Uniform
    public InlineUniformBlock(int binding, UniformState... uniformState) {

        //todo: add missing Max Uniform Block size check

        this.binding = binding;
        this.uniformState = uniformState;

        this.size_t = Arrays.stream(uniformState).mapToInt(UniformState::getByteSize).sum();

        if(size_t>256) throw new RuntimeException("Uniform block too large: "+size_t);

    }

    public int binding() {
        return binding;
    }

    public UniformState[] uniformState() {
        return uniformState;
    }

    public int size_t() {
        return size_t;
    }
}
