package net.vulkanmod.mixin.gui;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Gui.class)
public abstract class GuiM {

    @Shadow @Final private Minecraft minecraft;

    @Shadow protected abstract int getVisibleVehicleHeartRows(int i);

    @Shadow private int tickCount;

    @Shadow @Final private RandomSource random;

    @Shadow protected abstract int getVehicleMaxHearts(LivingEntity livingEntity);

    @Shadow protected abstract void renderHearts(GuiGraphics guiGraphics, Player player, int i, int j, int k, int l, float f, int m, int n, int o, boolean bl);

    @Shadow protected abstract LivingEntity getPlayerVehicleWithHealth();

    @Shadow private int screenWidth;

    @Shadow private int screenHeight;

    @Shadow private int displayHealth;

    @Shadow private int lastHealth;

    @Shadow private long lastHealthTime;

    @Shadow private long healthBlinkTime;

    @Shadow protected abstract Player getCameraPlayer();

    @Shadow @Final private static ResourceLocation GUI_ICONS_LOCATION;

    /**
     * @author
     */
    @Overwrite()
    private void renderPlayerHealth(GuiGraphics guiGraphics) {
        int ac;
        int ab;
        int aa;
        int z;
        int y;
        int x;
        Player player = this.getCameraPlayer();
        if (player == null) {
            return;
        }
        int i = Mth.ceil(player.getHealth());
        boolean bl = this.healthBlinkTime > (long)this.tickCount && (this.healthBlinkTime - (long)this.tickCount) / 3L % 2L == 1L;
        long l = Util.getMillis();
        if (i < this.lastHealth && player.invulnerableTime > 0) {
            this.lastHealthTime = l;
            this.healthBlinkTime = this.tickCount + 20;
        } else if (i > this.lastHealth && player.invulnerableTime > 0) {
            this.lastHealthTime = l;
            this.healthBlinkTime = this.tickCount + 10;
        }
        if (l - this.lastHealthTime > 1000L) {
            this.lastHealth = i;
            this.displayHealth = i;
            this.lastHealthTime = l;
        }
        this.lastHealth = i;
        int j = this.displayHealth;
        this.random.setSeed(this.tickCount * 312871);
        FoodData foodData = player.getFoodData();
        int k = foodData.getFoodLevel();
        int m = this.screenWidth / 2 - 91;
        int n = this.screenWidth / 2 + 91;
        int o = this.screenHeight - 39;
        float f = Math.max((float)player.getAttributeValue(Attributes.MAX_HEALTH), (float)Math.max(j, i));
        int p = Mth.ceil(player.getAbsorptionAmount());
        int q = Mth.ceil((f + (float)p) / 2.0f / 10.0f);
        int r = Math.max(10 - (q - 2), 3);
        int s = o - (q - 1) * r - 10;
        int t = o - 10;
        int u = player.getArmorValue();
        int v = -1;
        if (player.hasEffect(MobEffects.REGENERATION)) {
            v = this.tickCount % Mth.ceil(f + 5.0f);
        }
        this.minecraft.getProfiler().push("armor");
        for (int w = 0; w < 10; ++w) {
            if (u <= 0) continue;
            x = m + w * 8;
            if (w * 2 + 1 < u) {
                guiGraphics.blit(GUI_ICONS_LOCATION, x, s, 34, 9, 9, 9);
            }
            if (w * 2 + 1 == u) {
                guiGraphics.blit(GUI_ICONS_LOCATION, x, s, 25, 9, 9, 9);
            }
            if (w * 2 + 1 <= u) continue;
            guiGraphics.blit(GUI_ICONS_LOCATION, x, s, 16, 9, 9, 9);
        }
        this.minecraft.getProfiler().popPush("health");
        this.renderHearts(guiGraphics, player, m, o, r, v, f, i, j, p, bl);
        LivingEntity livingEntity = this.getPlayerVehicleWithHealth();
        x = this.getVehicleMaxHearts(livingEntity);
        if (x == 0) {
            this.minecraft.getProfiler().popPush("food");
            for (y = 0; y < 10; ++y) {
                z = o;
                aa = 16;
                ab = 0;
                if (player.hasEffect(MobEffects.HUNGER)) {
                    aa += 36;
                    ab = 13;
                }
                if (player.getFoodData().getSaturationLevel() <= 0.0f && this.tickCount % (k * 3 + 1) == 0) {
                    z += this.random.nextInt(3) - 1;
                }
                ac = n - y * 8 - 9;
                guiGraphics.blit(GUI_ICONS_LOCATION, ac, z, 16 + ab * 9, 27, 9, 9);
                if (y * 2 + 1 < k) {
                    guiGraphics.blit(GUI_ICONS_LOCATION, ac, z, aa + 36, 27, 9, 9);
                }
                if (y * 2 + 1 != k) continue;
                guiGraphics.blit(GUI_ICONS_LOCATION, ac, z, aa + 45, 27, 9, 9);
            }
            t -= 10;
        }
        this.minecraft.getProfiler().popPush("air");
        y = player.getMaxAirSupply();
        z = Math.min(player.getAirSupply(), y);
        if (player.isEyeInFluid(FluidTags.WATER) || z < y) {
            aa = this.getVisibleVehicleHeartRows(x) - 1;
            t -= aa * 10;
            ab = Mth.ceil((double)(z - 2) * 10.0 / (double)y);
            ac = Mth.ceil((double)z * 10.0 / (double)y) - ab;
            for (int ad = 0; ad < ab + ac; ++ad) {
                if (ad < ab) {
                    guiGraphics.blit(GUI_ICONS_LOCATION, n - ad * 8 - 9, t, 16, 18, 9, 9);
                    continue;
                }
                guiGraphics.blit(GUI_ICONS_LOCATION, n - ad * 8 - 9, t, 25, 18, 9, 9);
            }
        }
        this.minecraft.getProfiler().pop();
    }

    /**
     * @author
     */
    @Overwrite
    private void renderHeart(GuiGraphics guiGraphics, Gui.HeartType heartType, int i, int j, int k, boolean bl, boolean bl2) {
        guiGraphics.blit(GUI_ICONS_LOCATION, i, j, heartType.getX(bl2, bl), k, 9, 9);
    }
}
