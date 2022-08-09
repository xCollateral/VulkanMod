package net.vulkanmod.mixin.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ClientChatListener;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.*;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.vulkanmod.render.gui.GuiBatchRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Mixin(InGameHud.class)
public abstract class InGameHudM extends DrawableHelper {

    @Shadow @Final private Random random;
    @Shadow @Final private MinecraftClient client;
    @Shadow private int ticks;
    @Shadow private int lastHealthValue;
    @Shadow private int renderHealthValue;
    @Shadow private long lastHealthCheckTime;
    @Shadow private long heartJumpEndTick;
    @Shadow private int scaledWidth;
    @Shadow private int scaledHeight;

    @Shadow public abstract void render(MatrixStack matrices, float tickDelta);

    @Shadow protected abstract PlayerEntity getCameraPlayer();

    @Shadow protected abstract LivingEntity getRiddenEntity();

    @Shadow protected abstract int getHeartCount(LivingEntity entity);

    @Shadow protected abstract int getHeartRows(int heartCount);

    @Shadow protected abstract void renderHealthBar(MatrixStack matrices, PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking);

    @Shadow public abstract void clear();

    /**
     * @author
     */
    @Overwrite()
    private void renderStatusBars(MatrixStack matrices) {
        int ac;
        int ab;
        int aa;
        int z;
        int y;
        int x;
        PlayerEntity playerEntity = this.getCameraPlayer();
        if (playerEntity == null) {
            return;
        }
        int i = MathHelper.ceil(playerEntity.getHealth());
        boolean bl = this.heartJumpEndTick > (long)this.ticks && (this.heartJumpEndTick - (long)this.ticks) / 3L % 2L == 1L;
        long l = Util.getMeasuringTimeMs();
        if (i < this.lastHealthValue && playerEntity.timeUntilRegen > 0) {
            this.lastHealthCheckTime = l;
            this.heartJumpEndTick = this.ticks + 20;
        } else if (i > this.lastHealthValue && playerEntity.timeUntilRegen > 0) {
            this.lastHealthCheckTime = l;
            this.heartJumpEndTick = this.ticks + 10;
        }
        if (l - this.lastHealthCheckTime > 1000L) {
            this.lastHealthValue = i;
            this.renderHealthValue = i;
            this.lastHealthCheckTime = l;
        }
        this.lastHealthValue = i;
        int j = this.renderHealthValue;
        this.random.setSeed(this.ticks * 312871);
        HungerManager hungerManager = playerEntity.getHungerManager();
        int k = hungerManager.getFoodLevel();
        int m = this.scaledWidth / 2 - 91;
        int n = this.scaledWidth / 2 + 91;
        int o = this.scaledHeight - 39;
        float f = Math.max((float)playerEntity.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH), (float)Math.max(j, i));
        int p = MathHelper.ceil(playerEntity.getAbsorptionAmount());
        int q = MathHelper.ceil((f + (float)p) / 2.0f / 10.0f);
        int r = Math.max(10 - (q - 2), 3);
        int s = o - (q - 1) * r - 10;
        int t = o - 10;
        int u = playerEntity.getArmor();
        int v = -1;
        if (playerEntity.hasStatusEffect(StatusEffects.REGENERATION)) {
            v = this.ticks % MathHelper.ceil(f + 5.0f);
        }
        this.client.getProfiler().push("armor");
        GuiBatchRenderer.beginBatch(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        for (int w = 0; w < 10; ++w) {
            if (u <= 0) continue;
            x = m + w * 8;
            if (w * 2 + 1 < u) {
                GuiBatchRenderer.blit(matrices, this.getZOffset(), x, s, 34, 9, 9, 9);
            }
            if (w * 2 + 1 == u) {
                GuiBatchRenderer.blit(matrices, this.getZOffset(), x, s, 25, 9, 9, 9);
            }
            if (w * 2 + 1 <= u) continue;
            GuiBatchRenderer.blit(matrices, this.getZOffset(), x, s, 16, 9, 9, 9);
        }
        this.client.getProfiler().swap("health");
        this.renderHealthBar(matrices, playerEntity, m, o, r, v, f, i, j, p, bl);
        LivingEntity livingEntity = this.getRiddenEntity();
        x = this.getHeartCount(livingEntity);
        if (x == 0) {
            this.client.getProfiler().swap("food");
            for (y = 0; y < 10; ++y) {
                z = o;
                aa = 16;
                ab = 0;
                if (playerEntity.hasStatusEffect(StatusEffects.HUNGER)) {
                    aa += 36;
                    ab = 13;
                }
                if (playerEntity.getHungerManager().getSaturationLevel() <= 0.0f && this.ticks % (k * 3 + 1) == 0) {
                    z += this.random.nextInt(3) - 1;
                }
                ac = n - y * 8 - 9;
                GuiBatchRenderer.blit(matrices, this.getZOffset(), ac, z, 16 + ab * 9, 27, 9, 9);
                if (y * 2 + 1 < k) {
                    GuiBatchRenderer.blit(matrices, this.getZOffset(), ac, z, aa + 36, 27, 9, 9);
                }
                if (y * 2 + 1 != k) continue;
                GuiBatchRenderer.blit(matrices, this.getZOffset(), ac, z, aa + 45, 27, 9, 9);
            }
            t -= 10;
        }
        this.client.getProfiler().swap("air");
        y = playerEntity.getMaxAir();
        z = Math.min(playerEntity.getAir(), y);
        if (playerEntity.isSubmergedIn(FluidTags.WATER) || z < y) {
            aa = this.getHeartRows(x) - 1;
            t -= aa * 10;
            ab = MathHelper.ceil((double)(z - 2) * 10.0 / (double)y);
            ac = MathHelper.ceil((double)z * 10.0 / (double)y) - ab;
            for (int ad = 0; ad < ab + ac; ++ad) {
                if (ad < ab) {
                    GuiBatchRenderer.blit(matrices, this.getZOffset(), n - ad * 8 - 9, t, 16, 18, 9, 9);
                    continue;
                }
                GuiBatchRenderer.blit(matrices, this.getZOffset(), n - ad * 8 - 9, t, 25, 18, 9, 9);
            }
        }

        GuiBatchRenderer.endBatch();
        this.client.getProfiler().pop();

    }

    /**
     * @author
     */
    @Overwrite
    private void drawHeart(MatrixStack matrices, InGameHud.HeartType type, int x, int y, int v, boolean blinking, boolean halfHeart) {
        GuiBatchRenderer.blit(matrices, this.getZOffset(), x, y, type.getU(halfHeart, blinking), v, 9, 9);
    }
}
