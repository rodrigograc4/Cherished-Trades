package com.rodrigograc4.cherishedtrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.rodrigograc4.cherishedtrades.VillagerTradeLabels;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Mob;

/**
 * Vanilla shows a mob's name when the general rules allow it (LivingEntityRenderer: distance, HUD,
 * invisibility, teams...) and the name is always visible or the mob is under the crosshair. This
 * records that decision as if the mob had a name, so trade labels appear exactly when a name tag
 * would.
 */
@Mixin(MobRenderer.class)
public abstract class MobRendererMixin {

    @ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/Mob;D)Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z"))
    private boolean cherishedTrades$recordNameRule(boolean generalRulesAllow, Mob mob, double distanceSq) {
        // Vanilla compares with the render dispatcher's copy of this, taken from it every frame.
        boolean underCrosshair = mob == Minecraft.getInstance().crosshairPickEntity;
        VillagerTradeLabels.recordNameRule(mob, generalRulesAllow && (mob.isCustomNameVisible() || underCrosshair));
        return generalRulesAllow;
    }
}
