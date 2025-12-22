package com.rodrigograc4.cherishedtrades;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.village.TradeOffer;

public class PriceChecker {

    public static boolean isGreatDeal(TradeOffer offer) {
        ItemStack sellItem = offer.getSellItem();
        if (!sellItem.isOf(Items.ENCHANTED_BOOK)) return false;

        var enchantments = sellItem.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        var entry = enchantments.getEnchantmentEntries().iterator().next();
        RegistryEntry<Enchantment> enchant = entry.getKey();
        int level = entry.getIntValue();
        
        int currentPrice = offer.getDisplayedFirstBuyItem().getCount();

        boolean isTreasure = enchant.isIn(EnchantmentTags.NON_TREASURE) == false;
        
        if (enchant.isIn(EnchantmentTags.CURSE)) isTreasure = true;

        int minPrice = getMinPriceForLevel(level, isTreasure);
        
        return currentPrice <= minPrice;
    }

    private static int getMinPriceForLevel(int level, boolean isTreasure) {
        int baseMin = switch (level) {
            case 1 -> 5;
            case 2 -> 8;
            case 3 -> 11;
            case 4 -> 14;
            case 5 -> 17;
            default -> 5;
        };
        return isTreasure ? baseMin * 2 : baseMin;
    }
}