package net.vulkanmod.mixin.render.entity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class EntityRendererM<T extends Entity> {

//    /**
//     * @author
//     * @reason
//     */
//    @Overwrite
//    public boolean shouldRender(T entity, Frustum frustum, double d, double e, double f) {
//        if (!entity.shouldRender(d, e, f)) {
//            return false;
//        } else if (entity.noCulling) {
//            return true;
//        } else {
//            AABB aABB = entity.getBoundingBoxForCulling().inflate(0.5);
//            if (aABB.hasNaN() || aABB.getSize() == 0.0) {
//                aABB = new AABB(entity.getX() - 2.0, entity.getY() - 2.0, entity.getZ() - 2.0, entity.getX() + 2.0, entity.getY() + 2.0, entity.getZ() + 2.0);
//            }
//
////            WorldRenderer.getInstance().getSectionGrid().getSectionAtBlockPos((int) entity.getX(), (int) entity.getY(), (int) entity.getZ());
//            WorldRenderer worldRenderer = WorldRenderer.getInstance();
////            return (worldRenderer.getLastFrame() == worldRenderer.getSectionGrid().getSectionAtBlockPos(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ()).getLastFrame());
//
//            return frustum.isVisible(aABB);
//        }
//    }

    @Redirect(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean isVisible(Frustum frustum, AABB aABB) {
        if(Initializer.CONFIG.entityCulling) {
            WorldRenderer worldRenderer = WorldRenderer.getInstance();

            Vec3 pos = aABB.getCenter();

            RenderSection section = worldRenderer.getSectionGrid().getSectionAtBlockPos((int) pos.x(), (int) pos.y(), (int) pos.z());

            if(section == null)
                return frustum.isVisible(aABB);

            return worldRenderer.getLastFrame() == section.getLastFrame();
        } else {
            return frustum.isVisible(aABB);
        }

    }
}
