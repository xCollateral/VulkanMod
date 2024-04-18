package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

public class DescriptorAbstractionArray implements Iterable<Long> {

//    private final Int2IntArrayMap textureID2DescIDMap = new Int2IntArrayMap(32);

    final int[] texIds, descriptorIndices;

    final long[] textureSamplerHndls;
    final int maxSize;
    private final int shaderStage;
    private final int descriptorBinding;

    private int samplerRange;

    /*Abstracts between OpenGl texture Bindings and and Initialised descrtior indicies for this particualr dEscriptir Binding*/
    public DescriptorAbstractionArray(int maxSize, int shaderStage, int descriptorType, int descriptorBinding) {
        texIds = new int[maxSize];
        descriptorIndices = new int[maxSize];
        this.maxSize = maxSize;
        this.shaderStage = shaderStage;
        this.descriptorBinding = descriptorBinding;
        textureSamplerHndls=new long[maxSize];



/*        Arrays.fill(texIds, -1);
        Arrays.fill(descriptorIndices, -1);
        Arrays.fill(textureSamplerHndls, -1);*/
    }

    //Initialise a specific descriptor handle
    public void initialiseDescriptorIndex(int idx, long imageViewHandl)
    {
        //Initialize Arrays to zero, as that will default to index texture index 0 which is Missingno/Missing Deture rn
        //+ Actsu as a failsafe to avoid crashes/Driver fails as well

        if(texIds[idx]!=0) throw new RuntimeException();
        if(textureSamplerHndls[idx]!=0) throw new RuntimeException();

        texIds[idx]=idx;
        textureSamplerHndls[idx]=imageViewHandl;

        samplerRange=Math.max(idx, samplerRange);

    }

    //Add a new textureID registation/index to the Descripotr Array
    public void registerTexture(int texID, long imageView)
    {
        if(texIds[texID]!=0) return;
        if(texIds[texID]!=0&&texIds[texID]!=imageView) throw new RuntimeException(texIds[texID] + " != "+ imageView);
        texIds[texID]=++samplerRange;
        textureSamplerHndls[samplerRange]=imageView;

    }
    //Convert textureIDs to SamplerIndices
    public int TextureID2SamplerIdx(int textureID)
    {
        return this.texIds[textureID];
    }
    //Convert a TextureID to an Imageview
    public long TextureID2imageView(int textureID)
    {
        return textureSamplerHndls[this.TextureID2SamplerIdx(textureID)];
    }

    //Using LongIterator to avoid Unboxing
    public @NotNull Iterator<Long> iterator() {
        return new Iterator<>() {
            int pos = 0;
            final int limit = samplerRange;

            @Override
            public boolean hasNext() {
                return pos < limit;
            }

            @Override
            public Long next() {
                return textureSamplerHndls[pos++];
            }
        };
    }

    @Override
    public void forEach(Consumer<? super Long> action) {
        for (int i = 0; i < this.samplerRange; ++i) {
            action.accept(this.textureSamplerHndls[i]);
        }

    }
}
