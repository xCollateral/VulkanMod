package net.vulkanmod.vulkan.shader.descriptor;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

public class DescriptorAbstractionArray {

//    private final Int2IntArrayMap textureID2DescIDMap = new Int2IntArrayMap(32);

    final int[] texIds, descriptorIndices;

    final long[] textureSamplerHndls;
    final int maxSize;
    private final int shaderStage;
    private final int descriptorBinding;

    private int samplerRange;
    private final Int2IntOpenHashMap texID2DescIdx; //alignedIDs

    /*Abstracts between OpenGl texture Bindings and and Initialised descrtior indicies for this particualr dEscriptir Binding*/
    public DescriptorAbstractionArray(int reserveTextureRange, int maxSize, int shaderStage, int descriptorType, int descriptorBinding) {
        texIds = new int[maxSize];
        descriptorIndices = new int[maxSize];
        this.maxSize = maxSize;
        this.shaderStage = shaderStage;
        this.descriptorBinding = descriptorBinding;
        textureSamplerHndls = new long[maxSize];

        samplerRange = reserveTextureRange; //Thanks to Partially Bound, don't need to worry about initialising all the handles in the Descriptor Array
        this.texID2DescIdx = new Int2IntOpenHashMap(maxSize);


/*        Arrays.fill(texIds, -1);
        Arrays.fill(descriptorIndices, -1);
        Arrays.fill(textureSamplerHndls, -1);*/
    }

    //Initialise a specific descriptor handle
    //TODO: Maybe bypass TextureIDs for Reserved slots + (e.g. Atlases)
    // + May alos be optimal as a tmep workaround for Async Texture Atlas loader
    //Used for hardcoding "hot Paths" for single etyre only pipelines + Dealing w/ Async Textures (i.e. tetxures that utlsie async Stitching such as Atlases e.g.)

    //Requies a ResourceLocation to enforce ID/Slot HardCoding + Guarentee Alignment /correctness + Validation e.g.
    public void reserveTextureIDDescriptor(int idx, long imageViewHandl, ResourceLocation resourceLocation) {
        //Initialize Arrays to zero, as that will default to index texture index 0 which is Missingno/Missing Deture rn
        //+ Actsu as a failsafe to avoid crashes/Driver fails as well


//        int idx = getResourceLocationIDfromBinding(resourceLocation.toString());

        if (textureSamplerHndls[idx] != 0) throw new RuntimeException();


        textureSamplerHndls[idx] = imageViewHandl;

//        alignedIDs.put(texID, idx);

        samplerRange = Math.max(idx, samplerRange);

    }

    private int getResourceLocationIDfromBinding(String string) {
        return switch (string) {
            case "minecraft:missing/0" -> 0;
            case "minecraft:textures/atlas/blocks.png" -> 1;
            case "minecraft:textures/atlas/particles.png" -> 2;
            case "minecraft:textures/atlas/gui.png" -> 3;
            case "minecraft:dynamic/light_map_1" -> 6;
            default -> throw new IllegalStateException("Unexpected value: " + string);
        };
    }

    //Add a new textureID registation/index to the Descripotr Array
    public void registerTexture(int texID) {
        if (texID2DescIdx.containsKey(texID)) return;
//        if (texIds[texID] != 0 && texIds[texID] != imageView)
//            throw new RuntimeException(texIds[texID] + " != " + imageView);
//        texIds[texID] = ++samplerRange;
//        textureSamplerHndls[samplerRange] = imageView;

        texID2DescIdx.put(texID, samplerRange++);

    }

    //Convert textureIDs to SamplerIndices
    public int TextureID2SamplerIdx(int textureID) {
//        return this.texIds[textureID];
        if(this.texID2DescIdx.containsKey(textureID))
        {
            return this.texID2DescIdx.get(textureID);
        }
        else return 0;
    }

    //Convert a TextureID to an Imageview
    public long TextureID2imageView(int textureID) {
        return textureSamplerHndls[this.TextureID2SamplerIdx(textureID)];
    }
    //TODO; Handle Freeing Images
    //
    //Using LongIterator to avoid Unboxing

    public Int2IntOpenHashMap getAlignedIDs() {
        return texID2DescIdx;
    }

    public int currentSize() {
        return this.texID2DescIdx.size();//this.samplerRange;
    }

    public int getStage() {
        return this.shaderStage;
    }

    public int getBinding() {
        return this.descriptorBinding;
    }
}