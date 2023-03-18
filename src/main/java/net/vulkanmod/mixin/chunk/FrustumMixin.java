package net.vulkanmod.mixin.chunk;

import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.culling.Frustum;
import net.vulkanmod.interfaces.FrustumMixed;
import net.vulkanmod.render.chunk.VFrustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class FrustumMixin implements FrustumMixed {

    @Shadow private double camX;
    @Shadow private double camY;
    @Shadow private double camZ;
    private VFrustum vFrustum;

    @Inject(method = "calculateFrustum", at = @At("RETURN"))
    private void calculateFrustum(Matrix4f modelView, Matrix4f projection, CallbackInfo ci) {
        this.vFrustum = new VFrustum(modelView, projection);
    }

    @Inject(method = "prepare", at = @At("RETURN"))
    public void prepare(double d, double e, double f, CallbackInfo ci) {
        this.vFrustum.prepare(this.camX, this.camY, this.camZ);
    }

    @Override
    public VFrustum customFrustum() {
        return vFrustum;
    }
}
