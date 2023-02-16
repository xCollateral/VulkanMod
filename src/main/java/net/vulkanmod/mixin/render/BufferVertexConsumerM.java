package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BufferVertexConsumer.class)
public interface BufferVertexConsumerM
{
    /**
     * @author
     * @reason
     */
    @Overwrite
    static byte normalIntValue(float f) {
        return (byte) ((int) (f * 127.0F) & 255);
    }
}
