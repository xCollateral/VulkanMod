package net.vulkanmod.mixin.cloud;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DimensionSpecialEffects.OverworldEffects.class)
public abstract class CloudMixin extends DimensionSpecialEffects {
    public CloudMixin(float f, boolean bl, SkyType skyType, boolean bl2, boolean bl3) {
        super(f, bl, skyType, bl2, bl3);
    }

    @Override
    public float getCloudHeight() {
        return Initializer.CONFIG.cloudHeight;
    }
}
