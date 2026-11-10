package com.rodrigograc4.cherishedtrades.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfigScreen;

import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigTabButton;

/** Tabs ignore clicks within 20 px of the left edge (under the arrow); our tabs start there now. */
@Mixin(value = ClothConfigTabButton.class, remap = false)
public abstract class ClothConfigTabButtonMixin {

    @Shadow @Final private ClothConfigScreen screen;

    @ModifyConstant(method = "isMouseOver", constant = @Constant(doubleValue = 20.0))
    private double cherishedTrades$clickableFromLeftEdge(double inset) {
        return CherishedTradesConfigScreen.isOurs(screen) ? 0 : inset;
    }
}
