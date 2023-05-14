package net.vulkanmod.mixin.vertex;

import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.core.Direction;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.render.vertex.VertexUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

public class VertexConsumersM {

    @Mixin(targets = "com/mojang/blaze3d/vertex/VertexMultiConsumer$Double")
    public static class DoubleM implements ExtendedVertexBuilder {


        @Shadow @Final private VertexConsumer first;
        @Shadow @Final private VertexConsumer second;

        @Override
        public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
            ExtendedVertexBuilder firstExt = (ExtendedVertexBuilder) this.first;
            ExtendedVertexBuilder secondExt = (ExtendedVertexBuilder) this.second;

            firstExt.vertex(x, y, z, packedColor, u, v, overlay, light, packedNormal);
            secondExt.vertex(x, y, z, packedColor, u, v, overlay, light, packedNormal);
        }
    }

    @Mixin(SheetedDecalTextureGenerator.class)
    public static abstract class SheetDecalM implements ExtendedVertexBuilder {

        @Shadow @Final private VertexConsumer delegate;

        @Shadow @Final private Matrix3f normalInversePose;

        @Shadow @Final private Matrix4f cameraInversePose;

        @Shadow @Final private float textureScale;

        @Override
        public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
            float nx = VertexUtil.unpackN1(packedNormal);
            float ny = VertexUtil.unpackN2(packedNormal);
            float nz = VertexUtil.unpackN3(packedNormal);

            Vector3f vector3f = this.normalInversePose.transform(new Vector3f(nx, ny, nz));
            Direction direction = Direction.getNearest(vector3f.x(), vector3f.y(), vector3f.z());
            Vector4f vector4f = this.cameraInversePose.transform(new Vector4f(x, y, z, 1.0F));
            vector4f.rotateY(3.1415927F);
            vector4f.rotateX(-1.5707964F);
            vector4f.rotate(direction.getRotation());
            float f = -vector4f.x() * this.textureScale;
            float g = -vector4f.y() * this.textureScale;
            this.delegate.vertex(x, y, z).color(1.0F, 1.0F, 1.0F, 1.0F).uv(f, g).uv2(light).normal(nx, ny, nz).endVertex();

        }
    }
}
