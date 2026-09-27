package com.rodrigograc4.cherishedtrades;

import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

public class PriceChecker {

    public static boolean isGreatDeal(MerchantOffer offer) {

        if (!CherishedTradesConfig.INSTANCE.enablePriceChecker) {
            return false;
        }

        ItemStack sellItem = offer.getResult();
        if (!sellItem.is(Items.ENCHANTED_BOOK)) return false;

        ItemStack price = offer.getCostA();
        if (!price.is(Items.EMERALD)) return false;

        var enchantments = sellItem.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        var entry = enchantments.entrySet().iterator().next();
        boolean doublePrice = entry.getKey().is(EnchantmentTags.DOUBLE_TRADE_PRICE);
        int minPrice = getMinPriceForLevel(entry.getIntValue(), doublePrice);

        int allowedPrice = minPrice + CherishedTradesConfig.INSTANCE.priceOffset;

        return price.getCount() <= allowedPrice;
    }

    // Librarian books cost 2 + random(5 + 10 * level) + 3 * level emeralds, doubled for
    // #minecraft:double_trade_price (treasure) enchantments, so the cheapest roll is 2 + 3 * level.
    private static int getMinPriceForLevel(int level, boolean doublePrice) {
        int baseMin = 2 + 3 * Math.max(level, 1);
        return doublePrice ? baseMin * 2 : baseMin;
    }
}
