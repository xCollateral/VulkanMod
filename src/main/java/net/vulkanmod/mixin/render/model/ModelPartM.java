package net.vulkanmod.mixin.render.model;

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
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(ModelPart.class)
public class ModelPartM {

    @Shadow @Final private List<ModelPart.Cube> cubes;

    Vector3f temp = new Vector3f();

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void compile(PoseStack.Pose pose, VertexConsumer vertexConsumer, int i, int j, int color) {
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
                    vertexBuilder.vertex(pos.x(), pos.y(), pos.z(), color, vertex.u, vertex.v, j, i, packedNormal);
                }
            }
        }

    }
}
