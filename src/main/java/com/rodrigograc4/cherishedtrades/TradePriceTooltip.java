package com.rodrigograc4.cherishedtrades;

import com.rodrigograc4.cherishedtrades.PriceChecker.BookPrice;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.List;
import java.util.Optional;

/**
 * Adds the possible price range and the villager's own price to enchanted book tooltips in the
 * trade screen, so you can see how good a price is instead of just whether it is the best.
 */
public final class TradePriceTooltip {

    private static final int RESULT_SLOT = 2;
    /** The middle tone of textures/goldarrow.png. */
    public static final int DEAL_COLOR = 0xFFC457;

    private TradePriceTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (!CherishedTradesConfig.INSTANCE.enablePriceChecker) return;
            findOffer(stack).flatMap(PriceChecker::bookPrice).ifPresent(price -> addLines(price, lines));
        });
    }

    private static void addLines(BookPrice price, List<Component> lines) {
        lines.add(Component.empty());
        lines.add(Component.translatable("cherishedtrades.tooltip.priceRange", price.min(), price.max())
                .withStyle(ChatFormatting.GRAY));

        // Gold like the deal arrow when that arrow is shown; otherwise the same color as the text.
        boolean deal = PriceChecker.isGreatDeal(price);
        lines.add(Component.translatable("cherishedtrades.tooltip.villagerPrice", number(price.rolled(), deal))
                .withStyle(ChatFormatting.GRAY));

        // Only the lasting discount from reputation; temporary ones are already visible in the trade list.
        if (price.discounted() < price.rolled()) {
            String key = price.discounted() == 1 ? "cherishedtrades.tooltip.discounted.one" : "cherishedtrades.tooltip.discounted";
            lines.add(Component.translatable(key, number(price.discounted(), deal)).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component number(int value, boolean deal) {
        var text = Component.literal(String.valueOf(value));
        return deal ? text.withColor(DEAL_COLOR) : text;
    }

    // The trade list shows each offer's own result stack, so it can be matched by identity. The
    // result slot holds a copy, which is matched by content instead.
    private static Optional<MerchantOffer> findOffer(ItemStack stack) {
        if (!(Minecraft.getInstance().gui.screen() instanceof MerchantScreen screen)) return Optional.empty();

        MerchantMenu menu = screen.getMenu();
        boolean inResultSlot = menu.getSlot(RESULT_SLOT).getItem() == stack;

        for (MerchantOffer offer : menu.getOffers()) {
            if (offer.getResult() == stack) return Optional.of(offer);
        }
        if (inResultSlot) {
            for (MerchantOffer offer : menu.getOffers()) {
                if (ItemStack.isSameItemSameComponents(offer.getResult(), stack)) return Optional.of(offer);
            }
        }
        return Optional.empty();
    }
}
