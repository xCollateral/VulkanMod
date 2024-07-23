package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.vulkanmod.vulkan.util.VUtil;

import java.util.BitSet;

public class DescriptorAbstractionArray {

    private static final int maxLimit = BindlessDescriptorSet.maxPerStageDescriptorSamplers; //absoluteMaxBoundSamplerLimit
    private final BitSet descriptorIndices; //Only need max of 65536 textures
    private final int shaderStage;
    private final int descriptorType;
    private final int descriptorBinding;
    private final Int2IntMap texID2DescIdx, DescIdx2texID; //alignedIDs
    int maxSize;


    public DescriptorAbstractionArray(int maxSize, int shaderStage, int descriptorType, int descriptorBinding) {
        this.maxSize = maxSize;
        this.shaderStage = shaderStage;
        this.descriptorType = descriptorType;
        this.descriptorBinding = descriptorBinding;


        this.texID2DescIdx = new Int2IntOpenHashMap(maxSize);
        this.DescIdx2texID = new Int2IntOpenHashMap(maxSize);

        descriptorIndices = new BitSet(maxSize);
    }

    public void reserveTextureIDDescriptor(int idx, int textureID) {

        if (idx < 0 || textureID == -1) throw new RuntimeException();

        if (texID2DescIdx.containsKey(textureID)) throw new RuntimeException();


        texID2DescIdx.put(textureID, idx);

    }

    //Add a new textureID registation/index to the Descripotr Array
    public boolean registerTexture(int texID) {
        if (texID2DescIdx.containsKey(texID)) return false;

//        if(texID2DescIdx.size()>=maxLimit)
//            return -1; //Out of maxPerStageSamplers

        final int samplerIndex = descriptorIndices.nextClearBit(0);
        texID2DescIdx.put(texID, samplerIndex);
        DescIdx2texID.put(samplerIndex, texID);
        descriptorIndices.set(samplerIndex);
        return true; //vertSize;

    }

    //Hardcode a texture to a fixed Sampler Index: Avoids the need to fallback to Non-Uniform Indexing
    public void registerImmutableTexture(int texID, int SamplerIndex) {

        if (texID2DescIdx.containsKey(texID)) return;


        texID2DescIdx.put(texID, SamplerIndex);
        DescIdx2texID.put(SamplerIndex, texID);
        descriptorIndices.set(SamplerIndex);

    }

    //Convert textureIDs to SamplerIndices
    public int TextureID2SamplerIdx(int textureID) {
//        return this.texIds[textureID];
        if (this.texID2DescIdx.containsKey(textureID)) {
            return this.texID2DescIdx.get(textureID);
        } else return 0;
    }

    public Int2IntMap getAlignedIDs() {
        return texID2DescIdx;
    }

    public Int2IntMap getAlignedIDs2() {
        return DescIdx2texID;
    }

    public int currentSize() {
        return this.texID2DescIdx.size();//this.samplerRange;
    }
/*    public int currentLim() {
        return this.samplerRange;
    }*/

    public int getStage() {
        return this.shaderStage;
    }

    public int getBinding() {
        return this.descriptorBinding;
    }

    public boolean removeTexture(int id) {

        if (!this.texID2DescIdx.containsKey(id)) return false;
        int freedDescIdx = this.texID2DescIdx.remove(id);
        DescIdx2texID.remove(freedDescIdx);
        descriptorIndices.clear(freedDescIdx);
        return true;
    }

    public int getDescType() {
        return this.descriptorType;
    }

    public boolean checkCapacity() {

        return this.descriptorIndices.length() > maxSize; //avoid dedicated Sets from resizing incorrectly
    }

    public int resize() {
        final int align = VUtil.align(this.descriptorIndices.size(), 64);
        return Math.min(this.maxSize = align == maxSize ? align + 64 : align, maxLimit);
    }
}