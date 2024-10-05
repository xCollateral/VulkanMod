package net.vulkanmod.mixin.compatibility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.vulkan.Renderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mixin(PostChain.class)
public abstract class PostChainM {

    @Shadow private int screenWidth;
    @Shadow private int screenHeight;

    @Shadow @Final private Map<String, RenderTarget> customRenderTargets;
    @Shadow @Final private RenderTarget screenTarget;
    @Shadow @Final private List<PostPass> passes;

    @Shadow private float lastStamp;
    @Shadow private float time;

    @Shadow public abstract void addTempTarget(String string, int i, int j);
    @Shadow protected abstract void parseTargetNode(JsonElement jsonElement) throws ChainedJsonException;
    @Shadow protected abstract void parseUniformNode(JsonElement jsonElement) throws ChainedJsonException;

//    /**
//     * @author
//     * @reason
//     */
//    @Overwrite
//    private void load(TextureManager textureManager, ResourceLocation resourceLocation) throws IOException, JsonSyntaxException {
//        Resource resource = this.resourceManager.getResourceOrThrow(resourceLocation);
//
//        try {
//            Reader reader = resource.openAsReader();
//
//            try {
//                JsonObject jsonObject = GsonHelper.parse(reader);
//                JsonArray jsonArray;
//                int i;
//                JsonElement jsonElement;
//                if (GsonHelper.isArrayNode(jsonObject, "targets")) {
//                    jsonArray = jsonObject.getAsJsonArray("targets");
//                    i = 0;
//
//                    Iterator<JsonElement> iterator;
//                    for(iterator = jsonArray.iterator(); iterator.hasNext(); ++i) {
//                        jsonElement = iterator.next();
//
//                        try {
//                            this.parseTargetNode(jsonElement);
//                        } catch (Exception var14) {
//                            ChainedJsonException chainedJsonException = ChainedJsonException.forException(var14);
//                            chainedJsonException.prependJsonKey("targets[" + i + "]");
//                            throw chainedJsonException;
//                        }
//                    }
//                }
//
//                if (GsonHelper.isArrayNode(jsonObject, "passes")) {
//                    jsonArray = jsonObject.getAsJsonArray("passes");
//                    i = 0;
//
//                    Iterator<JsonElement> iterator;
//                    for(iterator = jsonArray.iterator(); iterator.hasNext(); ++i) {
//                        jsonElement = iterator.next();
//
//                        try {
//                            this.parsePassNode(textureManager, jsonElement);
//                        } catch (Exception var13) {
//                            ChainedJsonException chainedJsonException = ChainedJsonException.forException(var13);
//                            chainedJsonException.prependJsonKey("passes[" + i + "]");
//                            throw chainedJsonException;
//                        }
//                    }
//                }
//            } catch (Throwable var15) {
//                try {
//                    reader.close();
//                } catch (Throwable var12) {
//                    var15.addSuppressed(var12);
//                }
//
//                throw var15;
//            }
//
//            reader.close();
//
//        } catch (Exception var16) {
//            ChainedJsonException chainedJsonException2 = ChainedJsonException.forException(var16);
//            String var10001 = resourceLocation.getPath();
//            chainedJsonException2.setFilenameAndFlush(var10001 + " (" + resource.sourcePackId() + ")");
//            throw chainedJsonException2;
//        }
//    }

//    /**
//     * @author
//     * @reason
//     */
//    @Overwrite
//    private void parseTargetNode(JsonElement jsonElement) throws ChainedJsonException {
//        if (GsonHelper.isStringValue(jsonElement)) {
//            this.addTempTarget(jsonElement.getAsString(), this.screenWidth, this.screenHeight);
//        } else {
//            JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "target");
//            String string = GsonHelper.getAsString(jsonObject, "name");
//            int i = GsonHelper.getAsInt(jsonObject, "width", this.screenWidth);
//            int j = GsonHelper.getAsInt(jsonObject, "height", this.screenHeight);
//            if (this.customRenderTargets.containsKey(string)) {
//                throw new ChainedJsonException(string + " is already defined");
//            }
//
//            this.addTempTarget(string, i, j);
//        }
//
//    }

//    /**
//     * @author
//     * @reason
//     */
//    @Overwrite
//    private void parsePassNode(TextureManager textureManager, JsonElement jsonElement) throws IOException {
//        JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "pass");
//        String string = GsonHelper.getAsString(jsonObject, "name");
//        String string2 = GsonHelper.getAsString(jsonObject, "intarget");
//        String string3 = GsonHelper.getAsString(jsonObject, "outtarget");
//        RenderTarget renderTarget = this.getRenderTarget(string2);
//        RenderTarget renderTarget2 = this.getRenderTarget(string3);
//        if (renderTarget == null) {
//            throw new ChainedJsonException("Input target '" + string2 + "' does not exist");
//        } else if (renderTarget2 == null) {
//            throw new ChainedJsonException("Output target '" + string3 + "' does not exist");
//        } else {
//            PostPass postPass = this.addPass(string, renderTarget, renderTarget2);
//            JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "auxtargets", null);
//            if (jsonArray != null) {
//                int i = 0;
//
//                for(Iterator var12 = jsonArray.iterator(); var12.hasNext(); ++i) {
//                    JsonElement jsonElement2 = (JsonElement)var12.next();
//
//                    try {
//                        JsonObject jsonObject2 = GsonHelper.convertToJsonObject(jsonElement2, "auxtarget");
//                        String string4 = GsonHelper.getAsString(jsonObject2, "name");
//                        String string5 = GsonHelper.getAsString(jsonObject2, "id");
//                        boolean bl;
//                        String string6;
//                        if (string5.endsWith(":depth")) {
//                            bl = true;
//                            string6 = string5.substring(0, string5.lastIndexOf(58));
//                        } else {
//                            bl = false;
//                            string6 = string5;
//                        }
//
//                        RenderTarget renderTarget3 = this.getRenderTarget(string6);
//                        if (renderTarget3 == null) {
//                            if (bl) {
//                                throw new ChainedJsonException("Render target '" + string6 + "' can't be used as depth buffer");
//                            }
//
//                            ResourceLocation resourceLocation = new ResourceLocation("textures/effect/" + string6 + ".png");
//                            this.resourceManager.getResource(resourceLocation).orElseThrow(() -> {
//                                return new ChainedJsonException("Render target or texture '" + string6 + "' does not exist");
//                            });
//                            RenderSystem.setShaderTexture(0, resourceLocation);
//                            textureManager.bindForSetup(resourceLocation);
//                            AbstractTexture abstractTexture = textureManager.getTexture(resourceLocation);
//                            int j = GsonHelper.getAsInt(jsonObject2, "width");
//                            int k = GsonHelper.getAsInt(jsonObject2, "height");
//                            boolean bl2 = GsonHelper.getAsBoolean(jsonObject2, "bilinear");
//                            if (bl2) {
//                                RenderSystem.texParameter(3553, 10241, 9729);
//                                RenderSystem.texParameter(3553, 10240, 9729);
//                            } else {
//                                RenderSystem.texParameter(3553, 10241, 9728);
//                                RenderSystem.texParameter(3553, 10240, 9728);
//                            }
//
//                            Objects.requireNonNull(abstractTexture);
//                            postPass.addAuxAsset(string4, abstractTexture::getId, j, k);
//                        } else if (bl) {
//                            Objects.requireNonNull(renderTarget3);
//                            postPass.addAuxAsset(string4, renderTarget3::getDepthTextureId, renderTarget3.width, renderTarget3.height);
//                        } else {
//                            Objects.requireNonNull(renderTarget3);
//                            postPass.addAuxAsset(string4, renderTarget3::getColorTextureId, renderTarget3.width, renderTarget3.height);
//                        }
//                    } catch (Exception e) {
//                        ChainedJsonException chainedJsonException = ChainedJsonException.forException(e);
//                        chainedJsonException.prependJsonKey("auxtargets[" + i + "]");
//                        throw chainedJsonException;
//                    }
//                }
//            }
//
//            JsonArray jsonArray2 = GsonHelper.getAsJsonArray(jsonObject, "uniforms", null);
//            if (jsonArray2 != null) {
//                int l = 0;
//
//                for(Iterator<JsonElement> var29 = jsonArray2.iterator(); var29.hasNext(); ++l) {
//                    JsonElement jsonElement3 = var29.next();
//
//                    try {
//                        this.parseUniformNode(jsonElement3);
//                    } catch (Exception var25) {
//                        ChainedJsonException chainedJsonException2 = ChainedJsonException.forException(var25);
//                        chainedJsonException2.prependJsonKey("uniforms[" + l + "]");
//                        throw chainedJsonException2;
//                    }
//                }
//            }
//
//        }
//    }
//
//    private RenderTarget getRenderTarget(@Nullable String string) {
//        if (string == null) {
//            return null;
//        } else {
//            return string.equals("minecraft:main") ? this.screenTarget : this.customRenderTargets.get(string);
//        }
//    }
//
//    public PostPass addPass(String string, RenderTarget renderTarget, RenderTarget renderTarget2) throws IOException {
//        PostPass postPass = new PostPass(this.resourceManager, string, renderTarget, renderTarget2);
//        this.passes.add(this.passes.size(), postPass);
//        return postPass;
//    }

    @Shadow protected abstract void setFilterMode(int i);

    /**
     * @author
     * @reason
     */
    @Overwrite
    public void process(float f) {
        if (f < this.lastStamp) {
            this.time += 1.0F - this.lastStamp;
            this.time += f;
        } else {
            this.time += f - this.lastStamp;
        }

        this.lastStamp = f;

        while(this.time > 20.0F) {
            this.time -= 20.0F;
        }

        int filterMode = 9728;

        for(PostPass postPass : this.passes) {
            int passFilterMode = postPass.getFilterMode();
            if (filterMode != passFilterMode) {
                this.setFilterMode(passFilterMode);
                filterMode = passFilterMode;
            }

            postPass.process(this.time / 20.0F);
        }

        this.setFilterMode(9728);

        Renderer.resetViewport();
    }

}
