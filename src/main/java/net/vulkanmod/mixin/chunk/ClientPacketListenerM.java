package net.vulkanmod.mixin.chunk;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.minecraft.world.level.ChunkPos;
import net.vulkanmod.render.chunk.ChunkStatusMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerM {

    @Inject(method = "applyLightData", at = @At("RETURN"))
    private void setChunkStatus(int x, int z, ClientboundLightUpdatePacketData clientboundLightUpdatePacketData, CallbackInfo ci) {
        ChunkStatusMap.INSTANCE.setChunkStatus(x, z, ChunkStatusMap.LIGHT_READY);
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private void resetChunkStatus(ClientboundForgetLevelChunkPacket clientboundForgetLevelChunkPacket, CallbackInfo ci) {
        ChunkPos chunkPos = clientboundForgetLevelChunkPacket.pos();
        ChunkStatusMap.INSTANCE.resetChunkStatus(chunkPos.x, chunkPos.z, ChunkStatusMap.LIGHT_READY);
    }
}
