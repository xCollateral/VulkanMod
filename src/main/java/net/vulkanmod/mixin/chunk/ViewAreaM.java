package net.vulkanmod.mixin.chunk;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewArea.class)
public class ViewAreaM {

	@Shadow public SectionRenderDispatcher.RenderSection[] sections;

	@Inject(method = "createSections", at = @At("HEAD"), cancellable = true)
	private void skipAllocation(SectionRenderDispatcher sectionRenderDispatcher, CallbackInfo ci) {
		this.sections =  new SectionRenderDispatcher.RenderSection[0];

		ci.cancel();
	}
}
