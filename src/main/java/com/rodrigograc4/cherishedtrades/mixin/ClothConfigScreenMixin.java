package com.rodrigograc4.cherishedtrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfigScreen;

import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.widget.SearchFieldEntry;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Cloth has no option to turn these off, so on this mod's screen only: hides the tab scroll
 * arrows (both tabs always fit), the dark strip behind the tabs, and the search field at the
 * top of the list, and lines the tabs up with where the option names start.
 */
@Mixin(value = ClothConfigScreen.class, remap = false)
public abstract class ClothConfigScreenMixin {

    @Shadow public ClothConfigScreen.ListWidget<?> listWidget;
    @Shadow private SearchFieldEntry searchFieldEntry;
    @Shadow private AbstractWidget buttonLeftTab;
    @Shadow private AbstractWidget buttonRightTab;

    @Inject(method = "init", at = @At("TAIL"))
    private void cherishedTrades$simplify(CallbackInfo ci) {
        if (!isOurs()) return;

        if (buttonLeftTab != null) buttonLeftTab.visible = false;
        if (buttonRightTab != null) buttonRightTab.visible = false;

        // The search field sits between two spacer entries; drop it and the spacer after it.
        var children = listWidget.children();
        int index = children.indexOf(searchFieldEntry);
        if (index >= 0) {
            children.remove(index);
            if (index < children.size()) children.remove(index);
        }
    }

    // The strip behind the tabs: a dark gradient when a world is open...
    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fillGradient(IIIIII)V", ordinal = 0))
    private boolean cherishedTrades$tabStripInWorld(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int from, int to) {
        return !isOurs();
    }

    // ...and a solid background on the title screen.
    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lme/shedaniel/clothconfig2/gui/ClothConfigScreen;overlayBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lme/shedaniel/math/Rectangle;IIII)V"))
    private boolean cherishedTrades$tabStripOnTitle(ClothConfigScreen screen, GuiGraphicsExtractor graphics, Rectangle rect, int r, int g, int b, int a) {
        return !isOurs();
    }

    // The shadows along the top and bottom edges of the strip.
    @Inject(method = "drawTabsShades", at = @At("HEAD"), cancellable = true)
    private void cherishedTrades$tabShades(GuiGraphicsExtractor graphics, int lightColor, int darkColor, CallbackInfo ci) {
        if (isOurs()) ci.cancel();
    }

    // Tabs normally start at x = 24, after the left arrow, and are clipped to 20 px from each edge.
    // Option names are drawn from the list's row left (see DynamicEntryListWidget.getRowLeft).
    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 24, ordinal = 0))
    private int cherishedTrades$tabsStartX(int x) {
        if (!isOurs()) return x;
        return listWidget.left + listWidget.width / 2 - listWidget.getItemWidth() / 2 + 2;
    }

    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 20, ordinal = 0))
    private int cherishedTrades$tabsClipLeft(int inset) {
        return isOurs() ? 0 : inset;
    }

    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 40, ordinal = 0))
    private int cherishedTrades$tabsClipWidth(int inset) {
        return isOurs() ? 0 : inset;
    }

    private boolean isOurs() {
        return CherishedTradesConfigScreen.isOurs((ClothConfigScreen) (Object) this);
    }
}
