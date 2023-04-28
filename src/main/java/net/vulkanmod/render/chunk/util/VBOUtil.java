package net.vulkanmod.render.chunk.util;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import net.vulkanmod.render.VirtualBuffer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.*;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

//Use smaller class instead of WorldRenderer in case it helps GC/Heap fragmentation e.g.
public class VBOUtil {

    //TODO: Fix MipMaps Later...
    public static final ObjectArrayList<VBO> cutoutChunks = new ObjectArrayList<>(1024);
//    public static final ObjectArrayList<VBO> cutoutMippedChunks = new ObjectArrayList<>(1024);
    public static final ObjectArrayList<VBO> translucentChunks = new ObjectArrayList<>(1024);
//    public static final VirtualBuffer virtualBufferIdx=new VirtualBuffer(16777216, VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
    public static final VirtualBuffer virtualBufferVtx=new VirtualBuffer(536870912, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
    public static final VirtualBuffer virtualBufferVtx2=new VirtualBuffer(536870912, VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
    public static Matrix4f translationOffset;
    public static double camX;
    public static double camZ;
    public static double originX;
    public static double originZ;
    private static double prevCamX;
    private static double prevCamZ;

    private static final ShaderInstance test;
    private static final ShaderInstance test2;
    private static final VertexFormatElement ELEMENT_UV2 = new VertexFormatElement(2,VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 2);
    private static final VertexFormat BLOCK2 = new VertexFormat(ImmutableMap.of("Position",ELEMENT_POSITION, "Color",ELEMENT_COLOR, "UV0",ELEMENT_UV0, "UV2",ELEMENT_UV2, "Normal",ELEMENT_NORMAL, "Padding",ELEMENT_PADDING));


    static {
        try {
            test = new ShaderInstance(Minecraft.getInstance().getResourceManager(), "rendertype_cutout", BLOCK2);
            test2 = new ShaderInstance(Minecraft.getInstance().getResourceManager(), "rendertype_translucent", BLOCK2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateCamTranslation(PoseStack pose, double d, double e, double g, Matrix4f matrix4f)
    {
        VRenderSystem.applyMVP(pose.last().pose(), matrix4f);
        camX =d;
        camZ =g;


        originX+= (prevCamX - camX);
        originZ+= (prevCamZ - camZ);
        pose.pushPose();
        {
            translationOffset= pose.last().pose();
            translationOffset.multiplyWithTranslation((float) originX, (float) -e, (float) originZ);
        }
        pose.popPose();
//        pose.multiplyWithTranslation((float) originX, (float) -e, (float) originZ);
        prevCamX= camX;
        prevCamZ= camZ;
//        VRenderSystem.applyMVP(pose, matrix4f);




    }

    public static void removeVBO(VBO vbo) {
        //            case CUTOUT_MIPPED -> cutoutMippedChunks.remove(vbo);
        (vbo.type == RenderTypes.CUTOUT?cutoutChunks:translucentChunks).remove(vbo);

    }

    @NotNull
    public static RenderTypes getLayer(RenderType renderType) {
        return switch (renderType.name) {
            case "cutout","cutout_mipped" -> RenderTypes.CUTOUT;
            case "translucent" -> RenderTypes.TRANSLUCENT;
            default -> throw new IllegalStateException("Bad RenderType: "+renderType.name);
        };
    }

    public static RenderTypes getLayer(String type) {
        return switch (type) {
            case "cutout" -> RenderTypes.CUTOUT;
            case "translucent" -> RenderTypes.TRANSLUCENT;
            default -> throw new IllegalStateException("Bad RenderType: "+type);
        };
    }

    public static RenderType getLayerToType(RenderTypes renderType2) {
        return switch (renderType2) {
            case CUTOUT-> RenderType.CUTOUT;
            case TRANSLUCENT-> RenderType.TRANSLUCENT;
        };
    }

    public enum RenderTypes
    {
//        CUTOUT_MIPPED(RenderStateShard.ShaderStateShard.RENDERTYPE_CUTOUT_MIPPED_SHADER),
        CUTOUT(test, "cutout"),
        TRANSLUCENT(test2, "translucent");

        public final String name;

        public final ShaderInstance shader;

        RenderTypes(ShaderInstance solid, String name) {
            this.name = name;
            this.shader = solid;
        };
    }
}
