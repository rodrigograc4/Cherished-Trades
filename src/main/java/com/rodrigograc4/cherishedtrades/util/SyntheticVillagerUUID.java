package com.rodrigograc4.cherishedtrades.util;

import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public class SyntheticVillagerUUID {


    public static UUID fromTrades(TradeOfferList originalOffers) {
        if (originalOffers == null || originalOffers.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        for (TradeOffer offer : originalOffers) {
            ItemStack buy1 = offer.getOriginalFirstBuyItem();
            ItemStack buy2 = offer.getDisplayedSecondBuyItem();
            ItemStack sell = offer.getSellItem();

            sb.append(buy1.getCount()).append(getShortName(buy1));
            if (!buy2.isEmpty()) {
                sb.append(buy2.getCount()).append(getShortName(buy2));
            }
            sb.append(sell.getCount()).append(getShortName(sell));
        }

        return UUID.nameUUIDFromBytes(sb.toString().getBytes());
    }

    private static String getShortName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "x";

        String fullName = stack.getItem().toString();
        int colonIndex = fullName.indexOf(':');
        String name = colonIndex >= 0 ? fullName.substring(colonIndex + 1) : fullName;
        return name.isEmpty() ? "x" : name.substring(0, 1);
    }
}
