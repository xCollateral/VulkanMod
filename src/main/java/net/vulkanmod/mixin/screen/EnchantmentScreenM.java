package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.EnchantingPhrases;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import net.vulkanmod.vulkan.Drawer;
import org.joml.Matrix4d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnchantmentScreen.class)
public class EnchantmentScreenM extends HandledScreen<EnchantmentScreenHandler> {

    @Shadow private BookModel BOOK_MODEL;

    @Shadow @Final private static Identifier BOOK_TEXTURE;

    @Shadow public float pageAngle;

    @Shadow public float nextPageAngle;

    @Shadow public float pageTurningSpeed;

    @Shadow public float nextPageTurningSpeed;

    @Shadow @Final private static Identifier TEXTURE;

    public EnchantmentScreenM(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

//    @Redirect(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(DDD)V", ordinal = 0))
//    private void translate(MatrixStack instance, double x, double y, double z) {
//        instance.translate(x, y, z);
//    }

    @Inject(method = "init", at = @At("RETURN"))
    private void init(CallbackInfo ci) {

    }

    @Override
    public void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
        int k = (int)this.client.getWindow().getScaleFactor();
        RenderSystem.viewport((this.width - 320) / 2 * k, (this.height - 240) / 2 * k, 320 * k, 240 * k);
        Drawer.setViewport((this.width - 320) / 2 * k, (this.height - 240) / 2 * k, 320 * k, 240 * k);

        Matrix4f matrix4f = Matrix4f.translate(-0.34f, 0.23f, 0.0f);
        matrix4f.multiply(Matrix4f.viewboxMatrix(90.0, 1.3333334f, 9.0f, 80.0f));
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(matrix4f);
        matrices.push();
        MatrixStack.Entry entry = matrices.peek();
        entry.getPositionMatrix().loadIdentity();
        entry.getNormalMatrix().loadIdentity();
        matrices.translate(0, 3.3f, 1984.0);

        //TODO: make a proper transformation
        float f = 5.0f;
        matrices.scale(f, f, f);
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(180.0f));
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(20.0f));
        float g = MathHelper.lerp(delta, this.pageTurningSpeed, this.nextPageTurningSpeed);
        matrices.translate((1.0f - g) * 0.2f, (1.0f - g) * 0.1f, (1.0f - g) * 0.25f);
        float h = -(1.0f - g) * 90.0f - 90.0f;
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(h));
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(180.0f));
        float l = MathHelper.lerp(delta, this.pageAngle, this.nextPageAngle) + 0.25f;
        float m = MathHelper.lerp(delta, this.pageAngle, this.nextPageAngle) + 0.75f;
        l = (l - (float)MathHelper.fastFloor(l)) * 1.6f - 0.3f;
        m = (m - (float)MathHelper.fastFloor(m)) * 1.6f - 0.3f;
        if (l < 0.0f) {
            l = 0.0f;
        }
        if (m < 0.0f) {
            m = 0.0f;
        }
        if (l > 1.0f) {
            l = 1.0f;
        }
        if (m > 1.0f) {
            m = 1.0f;
        }
        this.BOOK_MODEL.setPageAngles(0.0f, l, m, g);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        VertexConsumer vertexConsumer = immediate.getBuffer(this.BOOK_MODEL.getLayer(BOOK_TEXTURE));
        this.BOOK_MODEL.render(matrices, vertexConsumer, 0xF000F0, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);
        immediate.draw();
        matrices.pop();
        RenderSystem.viewport(0, 0, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
        Drawer.setViewport(0, 0, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());

        RenderSystem.restoreProjectionMatrix();
        DiffuseLighting.enableGuiDepthLighting();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        EnchantingPhrases.getInstance().setSeed(((EnchantmentScreenHandler)this.handler).getSeed());
        int n = ((EnchantmentScreenHandler)this.handler).getLapisCount();
        for (int o = 0; o < 3; ++o) {
            int p = i + 60;
            int q = p + 20;
            this.setZOffset(0);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            int r = ((EnchantmentScreenHandler)this.handler).enchantmentPower[o];
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (r == 0) {
                this.drawTexture(matrices, p, j + 14 + 19 * o, 0, 185, 108, 19);
                continue;
            }
            String string = "" + r;
            int s = 86 - this.textRenderer.getWidth(string);
            StringVisitable stringVisitable = EnchantingPhrases.getInstance().generatePhrase(this.textRenderer, s);
            int t = 6839882;
            if (!(n >= o + 1 && this.client.player.experienceLevel >= r || this.client.player.getAbilities().creativeMode)) {
                this.drawTexture(matrices, p, j + 14 + 19 * o, 0, 185, 108, 19);
                this.drawTexture(matrices, p + 1, j + 15 + 19 * o, 16 * o, 239, 16, 16);
                this.textRenderer.drawTrimmed(stringVisitable, q, j + 16 + 19 * o, s, (t & 0xFEFEFE) >> 1);
                t = 4226832;
            } else {
                int u = mouseX - (i + 60);
                int v = mouseY - (j + 14 + 19 * o);
                if (u >= 0 && v >= 0 && u < 108 && v < 19) {
                    this.drawTexture(matrices, p, j + 14 + 19 * o, 0, 204, 108, 19);
                    t = 0xFFFF80;
                } else {
                    this.drawTexture(matrices, p, j + 14 + 19 * o, 0, 166, 108, 19);
                }
                this.drawTexture(matrices, p + 1, j + 15 + 19 * o, 16 * o, 223, 16, 16);
                this.textRenderer.drawTrimmed(stringVisitable, q, j + 16 + 19 * o, s, t);
                t = 8453920;
            }
            this.textRenderer.drawWithShadow(matrices, string, (float)(q + 86 - this.textRenderer.getWidth(string)), (float)(j + 16 + 19 * o + 7), t);
        }
    }
}
