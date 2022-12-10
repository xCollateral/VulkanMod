package net.vulkanmod.mixin.texture;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.texture.AtlasSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.vulkanmod.vulkan.TransferQueue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.stream.Collectors;

@Mixin(ModelBakery.class)
public abstract class ModelLoaderM {

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private Map<ResourceLocation, Pair<TextureAtlas, TextureAtlas.Preparations>> atlasPreparations;

    @Shadow private @Nullable AtlasSet atlasSet;

    @Shadow @Final private Map<ResourceLocation, UnbakedModel> topLevelModels;

    @Shadow @Final private Map<ResourceLocation, BakedModel> bakedTopLevelModels;

    @Shadow public @Nullable abstract BakedModel bake(ResourceLocation resourceLocation, ModelState modelState);

    /**
     * @author
     */
    @Overwrite
    public AtlasSet uploadTextures(TextureManager textureManager, ProfilerFiller profilerFiller) {
        profilerFiller.push("atlas");

        TransferQueue.startRecording();

        for (Pair<TextureAtlas, TextureAtlas.Preparations> pair : this.atlasPreparations.values()) {
            TextureAtlas textureAtlas = pair.getFirst();
            TextureAtlas.Preparations preparations = pair.getSecond();
            textureAtlas.reload(preparations);
            textureManager.register(textureAtlas.location(), textureAtlas);
            textureManager.bindForSetup(textureAtlas.location());
            textureAtlas.updateFilter(preparations);
        }

        TransferQueue.endRecording();

        this.atlasSet = new AtlasSet(this.atlasPreparations.values().stream().map(Pair::getFirst).collect(Collectors.toList()));
        profilerFiller.popPush("baking");
        this.topLevelModels.keySet().forEach(resourceLocation -> {
            BakedModel bakedModel = null;
            try {
                bakedModel = this.bake((ResourceLocation)resourceLocation, BlockModelRotation.X0_Y0);
            }
            catch (Exception exception) {
                LOGGER.warn("Unable to bake model: '{}': {}", resourceLocation, (Object)exception);
            }
            if (bakedModel != null) {
                this.bakedTopLevelModels.put((ResourceLocation)resourceLocation, bakedModel);
            }
        });
        profilerFiller.pop();
        return this.atlasSet;
    }
}
