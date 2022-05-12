package net.vulkanmod.mixin.renderer;

import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexFormats.class)
public class VertexFormatsM {

    @Shadow public static VertexFormatElement COLOR_ELEMENT;

//    static {
//        int i = 0;
//        i = 6;
//        VertexFormats.COLOR_ELEMENT = new VertexFormatElement(0, VertexFormatElement.DataType.FLOAT, VertexFormatElement.Type.COLOR, 4);
//    }

    @Inject(method = "<clinit>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexFormat;<init>(Lcom/google/common/collect/ImmutableMap;)V", ordinal = 0))
    private static void replaceColor(CallbackInfo ci) {
        COLOR_ELEMENT = new VertexFormatElement(0, VertexFormatElement.DataType.FLOAT, VertexFormatElement.Type.COLOR, 4);

    }
}
