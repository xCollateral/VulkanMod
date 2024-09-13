package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.lwjgl.system.MemoryStack;

public class SubSet {
    private static final int perSetMax = BindlessDescriptorSet.maxPerStageDescriptorSamplers;

    final long[] descriptorSets = new long[2];
    private final int baseOffset;  //Starting offset for FragSamplers
    //    private final int fragTextureLimit;
    private final int vertSize;
    private final Int2IntOpenHashMap texID2DescIdx;
    int fragCount;
    private int currentFragMax;

    public SubSet(int baseOffset, int vertSize, int fragTextureLimit) {
        this.baseOffset = baseOffset;
        this.vertSize = vertSize;
        this.currentFragMax = fragTextureLimit;//vertSize;
//        this.fragCount= initialSize;
//        this.fragTextureLimit = /*this.fragCount = */fragTextureLimit;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            allocSets(fragTextureLimit, stack);
        }


        texID2DescIdx = new Int2IntOpenHashMap(fragTextureLimit);
    }

    //Can overwrite descSets where due to resetting descriptorPool
    void allocSets(int fragTextureLimit, MemoryStack stack) {
        for (int i = 0; i < 2; i++) {
            this.descriptorSets[i] = DescriptorManager.allocateDescriptorSet(stack, fragTextureLimit);
        }
    }
//
//    public void increment()
//    {
//        if(currentFragMax > fragCount && fragCount<=perSetMax)
//            fragCount++;
//    }
//
//    public void decrement()
//    {
//        if(!isSetEmpty())
//            fragCount--;
//    }

//    private boolean isSetEmpty() {
//        return fragCount==0;
//    }


    public long getSetHandle(int frame) {
        return descriptorSets[frame];
    }

    //
    public boolean needsResize() {

        if (texID2DescIdx.size() == perSetMax) return false;

        final boolean b = currentFragMax < texID2DescIdx.size();
        return b;// || fragCount==perSetMax;
    }


    public int getBaseIndex() {
        return this.baseOffset;
    }

    public void addTexture(int textureID, int samplerIndex) {
        this.texID2DescIdx.put(textureID, samplerIndex - baseOffset);
    }

    public void removeTexture(int id) {
        this.texID2DescIdx.remove(id);
    }

    public Int2IntMap getAlignedIDs() {
        return this.texID2DescIdx;
    }

//    public int fragSize() {
//        return this.fragCount;
//    }
//
//    public int vertSize() {
//        return this.vertSize;
//    }

    public int resize() {
        int a = this.currentFragMax << 1;
        while (a < texID2DescIdx.size()) {
            a <<= 1;
        }
        final int align = Math.min(a, perSetMax);
        return currentFragMax = align;
    }

    public int currentSize() {
        return currentFragMax;
    }
}
