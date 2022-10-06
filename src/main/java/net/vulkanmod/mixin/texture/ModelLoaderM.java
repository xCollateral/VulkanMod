package net.vulkanmod.mixin.texture;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.render.model.*;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.vulkanmod.vulkan.TransferQueue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.stream.Collectors;

@Mixin(ModelLoader.class)
public abstract class ModelLoaderM {

    @Shadow @Final private Map<Identifier, Pair<SpriteAtlasTexture, SpriteAtlasTexture.Data>> spriteAtlasData;

    @Shadow private @Nullable SpriteAtlasManager spriteAtlasManager;

    @Shadow @Final private Map<Identifier, UnbakedModel> modelsToBake;

    @Shadow public @Nullable abstract BakedModel bake(Identifier id, ModelBakeSettings settings);

    @Shadow @Final private Map<Identifier, BakedModel> bakedModels;

    @Shadow @Final private static Logger LOGGER;

    /**
     * @author
     */
    @Overwrite
    public SpriteAtlasManager upload(TextureManager textureManager, Profiler profiler) {
        profiler.push("atlas");

        TransferQueue.startRecording();

        for (Pair<SpriteAtlasTexture, SpriteAtlasTexture.Data> pair : this.spriteAtlasData.values()) {
            SpriteAtlasTexture spriteAtlasTexture = pair.getFirst();
            SpriteAtlasTexture.Data data = pair.getSecond();
            spriteAtlasTexture.upload(data);
            textureManager.registerTexture(spriteAtlasTexture.getId(), spriteAtlasTexture);
            textureManager.bindTexture(spriteAtlasTexture.getId());
            spriteAtlasTexture.applyTextureFilter(data);
        }

        TransferQueue.endRecording();

        this.spriteAtlasManager = new SpriteAtlasManager(this.spriteAtlasData.values().stream().map(Pair::getFirst).collect(Collectors.toList()));
        profiler.swap("baking");
        this.modelsToBake.keySet().forEach(identifier -> {
            BakedModel bakedModel = null;
            try {
                bakedModel = this.bake((Identifier)identifier, ModelRotation.X0_Y0);
            }
            catch (Exception exception) {
                LOGGER.warn("Unable to bake model: '{}': {}", identifier, (Object)exception);
            }
            if (bakedModel != null) {
                this.bakedModels.put((Identifier)identifier, bakedModel);
            }
        });
        profiler.pop();
        return this.spriteAtlasManager;
    }
}
