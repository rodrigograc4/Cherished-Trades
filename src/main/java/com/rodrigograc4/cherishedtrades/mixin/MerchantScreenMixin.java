package com.rodrigograc4.cherishedtrades.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.rodrigograc4.cherishedtrades.CherishedMerchantMenu;
import com.rodrigograc4.cherishedtrades.PriceChecker;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import com.rodrigograc4.cherishedtrades.util.EnchantmentHelper;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    @Unique
    private static final Identifier FILLED_BOOKMARK = Identifier.fromNamespaceAndPath("cherishedtrades", "textures/bookmark.png");
    @Unique
    private static final Identifier EMPTY_BOOKMARK = Identifier.fromNamespaceAndPath("cherishedtrades", "textures/emptybookmark.png");
    @Unique
    private static final Identifier GOLD_ARROW = Identifier.fromNamespaceAndPath("cherishedtrades", "textures/goldarrow.png");
    @Unique
    private static final Identifier BLOCKED_ARROW = Identifier.fromNamespaceAndPath("cherishedtrades", "textures/blockedarrow.png");

    @Unique
    private static final int VISIBLE_OFFERS = 7;

    @Shadow
    private int shopItem;

    @Shadow
    private int scrollOff;

    private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Unique
    private CherishedMerchantMenu cherishedTrades$menu() {
        return (CherishedMerchantMenu) this.menu;
    }

    // Labels are drawn translated to the GUI's top-left corner, underneath the trade items.
    @Inject(method = "extractLabels", at = @At("TAIL"))
    private void cherishedTrades$drawBookmarks(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        MerchantOffers offers = this.menu.getOffers();
        if (offers.isEmpty()) return;

        boolean bookmarks = CherishedTradesConfig.INSTANCE.enableBookmarks;

        for (int i = 0; i < VISIBLE_OFFERS; i++) {
            int recipeIndex = i + this.scrollOff;
            if (recipeIndex >= offers.size()) break;

            MerchantOffer offer = offers.get(recipeIndex);

            if (EnchantmentHelper.isHighlighted(offer.getResult())) {
                int slotX = 6;
                int slotY = 19 + (i * 20);
                graphics.fill(slotX, slotY, slotX + 86, slotY + 18, 0x609c31e4);
            }

            if (bookmarks) {
                boolean favorite = cherishedTrades$menu().cherishedTrades$isFavorite(recipeIndex);
                Identifier texture = favorite ? FILLED_BOOKMARK : EMPTY_BOOKMARK;
                graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 1, 18 + (i * 20), 0.0F, 0.0F, 9, 9, 9, 9);
            }
        }
    }

    // Drawn after vanilla's trade arrows so the gold arrow replaces them.
    @Inject(method = "extractContents", at = @At("TAIL"))
    private void cherishedTrades$drawDealArrows(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MerchantOffers offers = this.menu.getOffers();
        if (offers.isEmpty()) return;

        for (int i = 0; i < VISIBLE_OFFERS; i++) {
            int recipeIndex = i + this.scrollOff;
            if (recipeIndex >= offers.size()) break;

            MerchantOffer offer = offers.get(recipeIndex);
            if (!PriceChecker.isGreatDeal(offer)) continue;

            Identifier texture = offer.isOutOfStock() ? BLOCKED_ARROW : GOLD_ARROW;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos + 60, this.topPos + 22 + (i * 20), 0.0F, 0.0F, 10, 9, 10, 9);
        }
    }

    // Toggles a bookmark when its icon is clicked, keeping the same trade selected after re-sorting.
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void cherishedTrades$clickBookmark(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!CherishedTradesConfig.INSTANCE.enableBookmarks || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return;

        MerchantOffers offers = this.menu.getOffers();
        if (offers.isEmpty()) return;

        double localX = event.x() - this.leftPos;
        double localY = event.y() - this.topPos;

        for (int i = 0; i < VISIBLE_OFFERS; i++) {
            int recipeIndex = i + this.scrollOff;
            if (recipeIndex >= offers.size()) break;

            int bookmarkX = 1;
            int bookmarkY = 18 + (i * 20);

            if (localX >= bookmarkX && localX <= bookmarkX + 12 && localY >= bookmarkY && localY <= bookmarkY + 12) {
                CherishedMerchantMenu menu = cherishedTrades$menu();
                int selectedRealIndex = menu.cherishedTrades$getRealIndex(this.shopItem);

                menu.cherishedTrades$toggleFavorite(recipeIndex);

                for (int visual = 0; visual < offers.size(); visual++) {
                    if (menu.cherishedTrades$getRealIndex(visual) == selectedRealIndex) {
                        this.shopItem = visual;
                        this.menu.setSelectionHint(visual);
                        break;
                    }
                }

                cir.setReturnValue(true);
                return;
            }
        }
    }

    // The server still has the trades in its original order, so translate the clicked row.
    @ModifyArg(
            method = "postButtonClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundSelectTradePacket;<init>(I)V")
    )
    private int cherishedTrades$toServerIndex(int visualIndex) {
        return cherishedTrades$menu().cherishedTrades$getRealIndex(visualIndex);
    }
}
