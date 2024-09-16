package net.vulkanmod.mixin.render.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.render.model.CubeModel;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelPart.class)
public abstract class ModelPartM {
    @Shadow @Final private List<ModelPart.Cube> cubes;

    Vector3f temp = new Vector3f();

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void injCompile(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        this.renderCubes(pose, vertexConsumer, light, overlay, color);
        ci.cancel();
    }

    @Unique
    public void renderCubes(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        ExtendedVertexBuilder vertexBuilder = (ExtendedVertexBuilder)vertexConsumer;

        color = ColorUtil.RGBA.fromArgb32(color);

        for (ModelPart.Cube cube : this.cubes) {
            ModelPartCubeMixed cubeMixed = (ModelPartCubeMixed)(cube);
            CubeModel cubeModel = cubeMixed.getCubeModel();

            ModelPart.Polygon[] polygons = cubeModel.getPolygons();

            cubeModel.transformVertices(matrix4f);

            for (ModelPart.Polygon polygon : polygons) {
                matrix3f.transform(this.temp.set(polygon.normal));
                this.temp.normalize();

                int packedNormal = VertexUtil.packNormal(temp.x(), temp.y(), temp.z());

                ModelPart.Vertex[] vertices = polygon.vertices;

                for (ModelPart.Vertex vertex : vertices) {

                    Vector3f pos = vertex.pos;
                    vertexBuilder.vertex(pos.x(), pos.y(), pos.z(), color, vertex.u, vertex.v, overlay, light, packedNormal);
                }
            }
        }

    }
}
