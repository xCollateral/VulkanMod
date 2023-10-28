package net.vulkanmod.forge.mixin;

import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(DisplayWindow.class)
public class DisplayWindowM {
    /**
     * @author
     */
    @Overwrite(remap = false)
    public void render(int alpha) {}

    /**
     * @author
     */
    @Overwrite(remap = false)
    public void initWindow(@Nullable String mcVersion) {}
}
