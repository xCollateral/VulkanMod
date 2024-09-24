package net.vulkanmod.mixin.chunk;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewArea.class)
public abstract class ViewAreaM {

	@Shadow public SectionRenderDispatcher.RenderSection[] sections;

	@Shadow protected abstract void setViewDistance(int i);

	@Inject(method = "createSections", at = @At("HEAD"))
	private void skipAllocation(SectionRenderDispatcher sectionRenderDispatcher, CallbackInfo ci) {
		// It's not possible to completely skip allocation since it would cause an error if repositionCamera is called
		this.setViewDistance(0);
	}
}
