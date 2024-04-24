package net.vulkanmod.mixin.wayland;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow @Final private Window window;
    @Shadow @Final public Options options;

    @Shadow @Final private VanillaPackResources vanillaPackResources;

    /**
     * @author
     * @reason Only KWin supports setting the Icon on Wayland AFAIK
     */
    @Redirect(method="<init>", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/Window;setIcon(Lnet/minecraft/server/packs/PackResources;Lcom/mojang/blaze3d/platform/IconSet;)V"))
    private void bypassWaylandIcon(Window instance, PackResources packResources, IconSet iconSet) throws IOException {
        if(!Platform.isWayLand())
        {
            this.window.setIcon(this.vanillaPackResources, SharedConstants.getCurrentVersion().isStable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
        }
    }
}
