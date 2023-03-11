package net.vulkanmod.render.chunk.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.NotNull;

//Use smaller class instead of WorldRenderer in case it helps GC/Heap fragmentation e.g.
public class VBOUtil {
    public static final ObjectArrayList<VBO> solidChunks = new ObjectArrayList<>(1024);
    public static final ObjectArrayList<VBO> cutoutChunks = new ObjectArrayList<>(1024);
    public static final ObjectArrayList<VBO> cutoutMippedChunks = new ObjectArrayList<>(1024);
    public static final ObjectArrayList<VBO> tripwireChunks = new ObjectArrayList<>(1024);
    public static final ObjectArrayList<VBO> translucentChunks = new ObjectArrayList<>(1024);
    public static Matrix4f translationOffset;
    public static double camX;
    public static double camZ;
    public static double originX;
    public static double originZ;
    private static double prevCamX;
    private static double prevCamZ;

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
        switch (vbo.type)
        {
            case SOLID -> solidChunks.remove(vbo);
            case CUTOUT_MIPPED -> cutoutMippedChunks.remove(vbo);
            case CUTOUT -> cutoutChunks.remove(vbo);
            case TRANSLUCENT-> translucentChunks.remove(vbo);
            case TRIPWIRE -> tripwireChunks.remove(vbo);
        }
    }

    @NotNull
    public static RenderTypes getLayer(RenderType renderType) {
        return switch (renderType.name) {
            case "cutout_mipped" -> RenderTypes.CUTOUT_MIPPED;
            case "cutout" -> RenderTypes.CUTOUT;
            case "translucent" -> RenderTypes.TRANSLUCENT;
            case "tripwire" -> RenderTypes.TRIPWIRE;
            default -> RenderTypes.SOLID;
        };
    }

    public static RenderTypes getLayer(String type) {
        return switch (type) {
            case "cutout_mipped" -> RenderTypes.CUTOUT_MIPPED;
            case "cutout" -> RenderTypes.CUTOUT;
            case "translucent" -> RenderTypes.TRANSLUCENT;
            case "tripwire" -> RenderTypes.TRIPWIRE;
            default -> RenderTypes.SOLID;
        };
    }

    public enum RenderTypes
    {
        SOLID(RenderStateShard.ShaderStateShard.RENDERTYPE_SOLID_SHADER),
        CUTOUT_MIPPED(RenderStateShard.ShaderStateShard.RENDERTYPE_CUTOUT_MIPPED_SHADER),
        CUTOUT(RenderStateShard.ShaderStateShard.RENDERTYPE_CUTOUT_SHADER),
        TRANSLUCENT(RenderStateShard.ShaderStateShard.RENDERTYPE_TRANSLUCENT_SHADER),
        TRIPWIRE(RenderStateShard.ShaderStateShard.RENDERTYPE_TRIPWIRE_SHADER);

        public final String name;

        public final ShaderInstance shader;

        RenderTypes(RenderStateShard.ShaderStateShard solid) {

            this.name = solid.name;
            this.shader = solid.shader.get().get();
        };
    }
}
