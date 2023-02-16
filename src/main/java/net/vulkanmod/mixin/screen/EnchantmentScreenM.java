package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.vulkanmod.vulkan.Drawer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenM extends AbstractContainerScreen<EnchantmentMenu> {

//    @Redirect(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V", ordinal = 0))
//    private void translate(MatrixStack instance, double x, double y, double z) {
//        instance.translate(x, y, z);
//    }

    @Shadow @Final private static ResourceLocation ENCHANTING_TABLE_LOCATION;

    @Shadow public float oOpen;

    @Shadow public float open;

    @Shadow public float oFlip;

    @Shadow public float flip;

    @Shadow private BookModel bookModel;

    @Shadow @Final private static ResourceLocation ENCHANTING_BOOK_LOCATION;

    public EnchantmentScreenM(EnchantmentMenu abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void init(CallbackInfo ci) {

    }

    @Override
    public void renderBg(PoseStack poseStack, float f, int i, int j) {
        Lighting.setupForFlatItems();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, ENCHANTING_TABLE_LOCATION);
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        this.blit(poseStack, k, l, 0, 0, this.imageWidth, this.imageHeight);
        int m = (int)this.minecraft.getWindow().getGuiScale();
        RenderSystem.viewport((this.width - 320) / 2 * m, (this.height - 240) / 2 * m, 320 * m, 240 * m);
        Drawer.setViewport((this.width - 320) / 2 * k, (this.height - 240) / 2 * k, 320 * k, 240 * k);

        Matrix4f matrix4f = Matrix4f.createTranslateMatrix(-0.34f, 0.23f, 0.0f);
        matrix4f.multiply(Matrix4f.perspective(90.0, 1.3333334f, 9.0f, 80.0f));
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(matrix4f);
        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        pose.pose().setIdentity();
        pose.normal().setIdentity();
        poseStack.translate(0.0, 3.3f, 1984.0);
        float g = 5.0f;
        poseStack.scale(5.0f, 5.0f, 5.0f);
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0f));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(20.0f));
        float h = Mth.lerp(f, this.oOpen, this.open);
        poseStack.translate((1.0f - h) * 0.2f, (1.0f - h) * 0.1f, (1.0f - h) * 0.25f);
        float n = -(1.0f - h) * 90.0f - 90.0f;
        poseStack.mulPose(Vector3f.YP.rotationDegrees(n));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(180.0f));
        float o = Mth.lerp(f, this.oFlip, this.flip) + 0.25f;
        float p = Mth.lerp(f, this.oFlip, this.flip) + 0.75f;
        o = (o - (float)Mth.fastFloor(o)) * 1.6f - 0.3f;
        p = (p - (float)Mth.fastFloor(p)) * 1.6f - 0.3f;
        if (o < 0.0f) {
            o = 0.0f;
        }
        if (p < 0.0f) {
            p = 0.0f;
        }
        if (o > 1.0f) {
            o = 1.0f;
        }
        if (p > 1.0f) {
            p = 1.0f;
        }
        this.bookModel.setupAnim(0.0f, o, p, h);
        MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        VertexConsumer vertexConsumer = bufferSource.getBuffer(this.bookModel.renderType(ENCHANTING_BOOK_LOCATION));
        this.bookModel.renderToBuffer(poseStack, vertexConsumer, 0xF000F0, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        bufferSource.endBatch();
        poseStack.popPose();
        RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        Drawer.setViewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());

        RenderSystem.restoreProjectionMatrix();
        Lighting.setupFor3DItems();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        EnchantmentNames.getInstance().initSeed(((EnchantmentMenu)this.menu).getEnchantmentSeed());
        int q = ((EnchantmentMenu)this.menu).getGoldCount();
        for (int r = 0; r < 3; ++r) {
            int s = k + 60;
            int t = s + 20;
            this.setBlitOffset(0);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, ENCHANTING_TABLE_LOCATION);
            int u = ((EnchantmentMenu)this.menu).costs[r];
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (u == 0) {
                this.blit(poseStack, s, l + 14 + 19 * r, 0, 185, 108, 19);
                continue;
            }
            String string = "" + u;
            int v = 86 - this.font.width(string);
            FormattedText formattedText = EnchantmentNames.getInstance().getRandomName(this.font, v);
            int w = 6839882;
            if (!(q >= r + 1 && this.minecraft.player.experienceLevel >= u || this.minecraft.player.getAbilities().instabuild)) {
                this.blit(poseStack, s, l + 14 + 19 * r, 0, 185, 108, 19);
                this.blit(poseStack, s + 1, l + 15 + 19 * r, 16 * r, 239, 16, 16);
                this.font.drawWordWrap(formattedText, t, l + 16 + 19 * r, v, (w & 0xFEFEFE) >> 1);
                w = 4226832;
            } else {
                int x = i - (k + 60);
                int y = j - (l + 14 + 19 * r);
                if (x >= 0 && y >= 0 && x < 108 && y < 19) {
                    this.blit(poseStack, s, l + 14 + 19 * r, 0, 204, 108, 19);
                    w = 0xFFFF80;
                } else {
                    this.blit(poseStack, s, l + 14 + 19 * r, 0, 166, 108, 19);
                }
                this.blit(poseStack, s + 1, l + 15 + 19 * r, 16 * r, 223, 16, 16);
                this.font.drawWordWrap(formattedText, t, l + 16 + 19 * r, v, w);
                w = 8453920;
            }
            this.font.drawShadow(poseStack, string, (float)(t + 86 - this.font.width(string)), (float)(l + 16 + 19 * r + 7), w);
        }

//        DiffuseLighting.disableGuiDepthLighting();
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//        RenderSystem.setShaderTexture(0, TEXTURE);
//        int i = (this.width - this.backgroundWidth) / 2;
//        int j = (this.height - this.backgroundHeight) / 2;
//        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
//        int k = (int)this.client.getWindow().getScaleFactor();
//        RenderSystem.viewport((this.width - 320) / 2 * k, (this.height - 240) / 2 * k, 320 * k, 240 * k);
//        Drawer.setViewport((this.width - 320) / 2 * k, (this.height - 240) / 2 * k, 320 * k, 240 * k);
//
//        Matrix4f matrix4f = Matrix4f.translate(-0.34f, 0.23f, 0.0f);
//        matrix4f.multiply(Matrix4f.viewboxMatrix(90.0, 1.3333334f, 9.0f, 80.0f));
//        RenderSystem.backupProjectionMatrix();
//        RenderSystem.setProjectionMatrix(matrix4f);
//        matrices.push();
//        MatrixStack.Entry entry = matrices.peek();
//        entry.getPositionMatrix().loadIdentity();
//        entry.getNormalMatrix().loadIdentity();
//        matrices.translate(0, 3.3f, 1984.0);
//
//        //TODO: make a proper transformation
//        float f = 5.0f;
//        matrices.scale(f, f, f);
//        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0f));
//        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(20.0f));
//        float g = MathHelper.lerp(delta, this.pageTurningSpeed, this.nextPageTurningSpeed);
//        matrices.translate((1.0f - g) * 0.2f, (1.0f - g) * 0.1f, (1.0f - g) * 0.25f);
//        float h = -(1.0f - g) * 90.0f - 90.0f;
//        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(h));
//        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180.0f));
//        float l = MathHelper.lerp(delta, this.pageAngle, this.nextPageAngle) + 0.25f;
//        float m = MathHelper.lerp(delta, this.pageAngle, this.nextPageAngle) + 0.75f;
//        l = (l - (float)MathHelper.fastFloor(l)) * 1.6f - 0.3f;
//        m = (m - (float)MathHelper.fastFloor(m)) * 1.6f - 0.3f;
//        if (l < 0.0f) {
//            l = 0.0f;
//        }
//        if (m < 0.0f) {
//            m = 0.0f;
//        }
//        if (l > 1.0f) {
//            l = 1.0f;
//        }
//        if (m > 1.0f) {
//            m = 1.0f;
//        }
//        this.BOOK_MODEL.setPageAngles(0.0f, l, m, g);
//        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
//        VertexConsumer vertexConsumer = immediate.getBuffer(this.BOOK_MODEL.getLayer(BOOK_TEXTURE));
//        this.BOOK_MODEL.render(matrices, vertexConsumer, 0xF000F0, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
//        immediate.draw();
//        matrices.pop();
//        RenderSystem.viewport(0, 0, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
//        Drawer.setViewport(0, 0, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
//
//        RenderSystem.restoreProjectionMatrix();
//        DiffuseLighting.enableGuiDepthLighting();
//        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//        EnchantingPhrases.getInstance().setSeed(((EnchantmentScreenHandler)this.handler).getSeed());
//        int n = ((EnchantmentScreenHandler)this.handler).getLapisCount();
//        for (int o = 0; o < 3; ++o) {
//            int p = i + 60;
//            int q = p + 20;
//            this.setZOffset(0);
//            RenderSystem.setShader(GameRenderer::getPositionTexShader);
//            RenderSystem.setShaderTexture(0, TEXTURE);
//            int r = ((EnchantmentScreenHandler)this.handler).enchantmentPower[o];
//            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//            if (r == 0) {
//                this.drawTexture(matrices, p, j + 14 + 19 * o, 0, 185, 108, 19);
//                continue;
//            }
//            String string = "" + r;
//            int s = 86 - this.textRenderer.getWidth(string);
//            StringVisitable stringVisitable = EnchantingPhrases.getInstance().generatePhrase(this.textRenderer, s);
//            int t = 6839882;
//            if (!(n >= o + 1 && this.client.player.experienceLevel >= r || this.client.player.getAbilities().creativeMode)) {
//                this.drawTexture(matrices, p, j + 14 + 19 * o, 0, 185, 108, 19);
//                this.drawTexture(matrices, p + 1, j + 15 + 19 * o, 16 * o, 239, 16, 16);
//                this.textRenderer.drawTrimmed(stringVisitable, q, j + 16 + 19 * o, s, (t & 0xFEFEFE) >> 1);
//                t = 4226832;
//            } else {
//                int u = mouseX - (i + 60);
//                int v = mouseY - (j + 14 + 19 * o);
//                if (u >= 0 && v >= 0 && u < 108 && v < 19) {
//                    this.drawTexture(matrices, p, j + 14 + 19 * o, 0, 204, 108, 19);
//                    t = 0xFFFF80;
//                } else {
//                    this.drawTexture(matrices, p, j + 14 + 19 * o, 0, 166, 108, 19);
//                }
//                this.drawTexture(matrices, p + 1, j + 15 + 19 * o, 16 * o, 223, 16, 16);
//                this.textRenderer.drawTrimmed(stringVisitable, q, j + 16 + 19 * o, s, t);
//                t = 8453920;
//            }
//            this.textRenderer.drawWithShadow(matrices, string, (float)(q + 86 - this.textRenderer.getWidth(string)), (float)(j + 16 + 19 * o + 7), t);
//        }
    }
}
