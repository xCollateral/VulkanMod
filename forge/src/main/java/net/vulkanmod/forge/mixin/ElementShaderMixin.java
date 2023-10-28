package net.vulkanmod.forge.mixin;

import net.minecraftforge.fml.earlydisplay.ElementShader;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ElementShader.class)
public abstract class ElementShaderMixin implements IElementShader {
    /**
     * @author
     * @reason
     */
    @Override
    public void init() {

    }

}
