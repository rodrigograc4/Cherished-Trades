package com.rodrigograc4.cherishedtrades.util;

import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.component.DataComponentTypes;

public class SyntheticVillagerUUID {


    public static UUID fromTrades(TradeOfferList originalOffers) {
        if (originalOffers == null || originalOffers.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        for (TradeOffer offer : originalOffers) {
            ItemStack buy1 = offer.getOriginalFirstBuyItem();
            ItemStack buy2 = offer.getDisplayedSecondBuyItem();
            ItemStack sell = offer.getSellItem();

            sb.append(getItemFullString(buy1)).append(buy1.getCount());
            if (!buy2.isEmpty()) {
                sb.append(getItemFullString(buy2)).append(buy2.getCount());
            }
            sb.append(getItemFullString(sell)).append(sell.getCount());
        }

        System.out.println("[SyntheticVillagerUUID] Generated string for UUID: " + sb);
        return UUID.nameUUIDFromBytes(sb.toString().getBytes());
    }

    private static String getItemFullString(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";

        String base = stack.getItem().toString();
        
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            var enchantments = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                StringBuilder enchString = new StringBuilder();
                enchantments.getEnchantmentEntries().forEach(entry -> {
                    String enchantId = entry.getKey().getKey().get().getValue().toString();
                    int level = entry.getIntValue();
                    enchString.append(enchantId).append("_").append(level).append("|");
                });
                return base + "|" + enchString.toString();
            }
        }

        return base;
    }
}
