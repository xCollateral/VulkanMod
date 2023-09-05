package net.vulkanmod.mixin.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.render.model.CubeModel;
import net.vulkanmod.render.vertex.VertexUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ModelPart.class)
public class ModelPartM {

    @Shadow @Final private List<ModelPart.Cube> cubes;

    /**
     * @author
     * @reason
     */
    @Overwrite
    protected void compile(PoseStack.Pose pose, VertexConsumer vertexConsumer, int i, int j, float r, float g, float b, float a) {
        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();
        ExtendedVertexBuilder vertexBuilder = (ExtendedVertexBuilder)vertexConsumer;

        int packedColor = VertexUtil.packColor(r, g, b, a);

        for (ModelPart.Cube cube : this.cubes) {
            ModelPartCubeMixed cubeMixed = (ModelPartCubeMixed)(cube);
            CubeModel cubeModel = cubeMixed.getCubeModel();

            ModelPart.Polygon[] var11 = cubeModel.getPolygons();
//            int var12 = var11.length;

            cubeModel.transformVertices(matrix4f);

            for (ModelPart.Polygon polygon : var11) {
                Vector3f vector3f = matrix3f.transform(new Vector3f(polygon.normal));
//                float l = vector3f.x();
//                float m = vector3f.y();
//                float n = vector3f.z();
                int packedNormal = VertexUtil.packNormal(vector3f.x(), vector3f.y(), vector3f.z());

                ModelPart.Vertex[] vertices = polygon.vertices;
//                int var20 = vertices.length;

                for (ModelPart.Vertex vertex : vertices) {
//                    float o = vertex.pos.x() / 16.0F;
//                    float p = vertex.pos.y() / 16.0F;
//                    float q = vertex.pos.z() / 16.0F;
//
//                    float o = vertex.pos.x();
//                    float p = vertex.pos.y();
//                    float q = vertex.pos.z();
//                    Vector4f vector4f = matrix4f.transform(new Vector4f(o, p, q, 1.0F));
//                    vertexConsumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), r, g, b, a, vertex.u, vertex.v, j, i, l, m, n);

                    Vector3f pos = vertex.pos;
//                    vertexConsumer.vertex(pos.x(), pos.y(), pos.z(), r, g, b, a, vertex.u, vertex.v, j, i, l, m, n);
                    vertexBuilder.vertex(pos.x(), pos.y(), pos.z(), packedColor, vertex.u, vertex.v, j, i, packedNormal);
                }
            }
        }

    }
}
