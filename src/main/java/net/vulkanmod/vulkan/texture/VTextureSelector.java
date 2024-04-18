package net.vulkanmod.vulkan.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;

import java.nio.ByteBuffer;

public abstract class VTextureSelector {
    public static final int SIZE = 8/*128*/;

    private static final VulkanImage[] boundTextures = new VulkanImage[SIZE];
    private static final int[] boundIDs = new int[SIZE];
//    private static final VulkanImage[] loadedTextures = new VulkanImage[SIZE];

    private static final int[] levels = new int[SIZE];

    private static final VulkanImage whiteTexture = VulkanImage.createWhiteTexture();

    private static int activeTexture = 0;
    private static int lastTextureId;
//    private static VulkanImage lastTexture = null;

    //Shader can alias/Access a max of 8 Images at once
    //Not to be confused w. tetxureID: atciveTexture specifies /reserved WHAT image the shader MUST use, ANW WHICH DEST Bining/SAMPLER is is BOUND/ASSIGNED?ABTRACTED/REGSUTERED/Aliased to to
    //Thse slots are then utilsied by the current pipeline/state to pick which image sia siigend to#Smapler, and then the textureID it must use
    //These teuxreIDs are then abtarted in a DescriptAbstartcionArray, allowing TetxureIDs <-> to descriptorIds, allowing speicifc tetxures to eb hardcoded

    //activeTetxurespecific WHICH DesritprSet this Image is targeted for: 0 - 1 are ALWAYS VERTEX, while 2+ is FRAG


    //0: -> Fragment Index
    //1; -> Vertex Index: Overlay
    //2; -> Vertex Index: LightMap
    public static void bindTexture(int activeTexture, VulkanImage texture, int id) {
        if(activeTexture < 0 || activeTexture > 7) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, 7]", activeTexture));
            return;
        }
        if(id == 0 || id == -1) {
            Initializer.LOGGER.error("bad ID!:" + id);
            return;
        }

//        if(textureID==id) return;
        boundIDs[activeTexture] = id;
        boundTextures[activeTexture] = texture;
        levels[activeTexture] = -1;
    }

    public static void bindImage(int i, VulkanImage texture, int level) {
        if(i < 0 || i > 7) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, 7]", i));
            return;
        }

        boundTextures[i] = texture;
        levels[i] = level;
    }

    public static void uploadSubTexture(int mipLevel, int width, int height, int xOffset, int yOffset, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        VulkanImage texture = boundTextures[activeTexture];

        if(texture == null) {
            throw new NullPointerException("Texture is null at index: " + activeTexture);
        }
//        if(width+xOffset >= texture.width || height+yOffset >= texture.height)
//        {
//            Initializer.LOGGER.error("Bad Copy dims:! "+ width + "->" +height + " + "+texture.width + "->"+texture.height+" Out of bounds!");
//            Initializer.LOGGER.error("Bad Copy dims:! "+ xOffset + "->" +yOffset);
//            return;
//        }

        texture.uploadSubTextureAsync(mipLevel, width, height, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, unpackRowLength, buffer);
    }

    //Allow Shaders to obtain a samplerDescriptorArray index w/ the current OpenGL activeTexture offset
    // i.e. avoids needing to manage OPENGl tetxure Unit binding + Vulkan image Smapelr incicies Abstracions directly
    public static void registerUniqueBinding(ImageDescriptor imageDescriptor, String targetShaderId, String SamplerIdentifier)
    {
        int currentStageIdx = getTextureIdx(SamplerIdentifier);
//        VulkanImage activeVulkanImage = getLoadedTexture();
        VulkanImage activeVulkanImage = boundTextures[currentStageIdx];

        final int targetStage = 0;
//        Renderer.getDescriptorSetArray().addTexture(targetStage, imageDescriptor, activeVulkanImage.getSampler());
    }

//    private static VulkanImage getLoadedTexture() {
//        return loadedTextures[lastTextureId];
//    }
//
//    private static VulkanImage getActiveTexture() {
//        return loadedTextures[activeTexture];
//    }


    //TODO: Descriptor Indexing
    // + might atcually be the solution to the abstraction + registering problem w. OpenGLs tetxure limits
    public static int getTextureIdx(String name) {
        return switch (name) {
            case "Sampler0", "DiffuseSampler" -> 0;
            case "Sampler1", "SamplerProj" -> 1;
            case "Sampler2" -> 2;
            case "Sampler3" -> 3;
            case "Sampler4" -> 4;
            case "Sampler5" -> 5;
            case "Sampler6" -> 6;
            case "Sampler7" -> 7;
            default -> throw new IllegalStateException("Unknown sampler name: " + name);
        };
    }
    public static int getTextureBinding(String name) {
        return switch (name) {
            case "DiffuseSampler" -> 0;
            case "Sampler0" -> 2;
            case "Sampler2", "SamplerProj" -> 3;
            case "Sampler1" -> 4;
            default -> throw new IllegalStateException("Unknown sampler name: " + name);
        };
    }
    //TODO: makes sure Samplers are stored persistently
    // + Donlt invalide Texture Units/Slots like w/ the OpenGL model
    // + GL_REXYRE0 is always bound: perhape sfolloin OGP  dkie. too closely...
    public static VulkanImage getImage(int i) {
        final VulkanImage boundTexture = boundTextures[i];
        return boundTexture==null ? boundTextures[0] : boundTexture;
    }



    public static void registerTexture(int vulkanImage, int bindingID, ResourceLocation resourceLocation)
    {

//        if(boundTextures[bindingID]!=null) throw new RuntimeException();





//        GlTexture.bindIdToImage(bindingID, vulkanImage);

        Initializer.LOGGER.info("Registered texture: " + lastTextureId + " <-> " + bindingID + " <-> " + vulkanImage + "! -> "+resourceLocation);


//        vulkanImage.readOnlyLayout();
//
//        boundTextures[bindingID]=vulkanImage;




    }


    public static void setLightTexture(VulkanImage texture) {
        boundTextures[2] = texture;
    }

    public static void setOverlayTexture(VulkanImage texture) {
        boundTextures[1] = texture;
//        loadedTextures[1] = texture;
    }

    public static void setActiveTexture(int activeTexture) {
        if(activeTexture < 0 || activeTexture > 7) {
            throw new IllegalStateException(String.format("On Texture binding: index %d out of range [0, 7]", activeTexture));
        }

//        VTextureSelector.id = id;
        VTextureSelector.activeTexture = activeTexture;
    }

    public static VulkanImage getBoundTexture(int i) { return boundTextures[i]; }
    public static int getBoundId(int i) { return boundIDs[i]; }

    public static void assertImageState(int i)
    {
        final int boundID = boundIDs[i];

        if(boundID == 0 || boundID == -1)
        {
//            Initializer.LOGGER.error(boundID);
            return;
        }
        final int id2 = RenderSystem.getShaderTexture(i);
        if(id2 != boundID)
        {
            Initializer.LOGGER.error("Bad Image ID!: " + i +" Stage "+ id2 + " != " + boundID);
        }
        final VulkanImage id = GlTexture.getTexture(boundID).getVulkanImage();
        final VulkanImage id1 = boundTextures[i];
        if(id.getId() != id1.getId())
        {
            Initializer.LOGGER.error("Bad Image State!: " + boundID);
        }
    }

    public static VulkanImage getWhiteTexture() { return whiteTexture; }
}
