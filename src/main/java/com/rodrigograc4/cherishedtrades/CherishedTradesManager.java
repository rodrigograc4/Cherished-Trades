package com.rodrigograc4.cherishedtrades;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class CherishedTradesManager {

    private static final Set<String> FAVORITES = new HashSet<>();
    private static Path FILE;

    public static void init() {
        FILE = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("cherished-trades.json");
        load();
    }

    public static String getTradeIdentifier(TradeOffer offer) {
        StringBuilder sb = new StringBuilder();
        sb.append(getItemStackKey(offer.getOriginalFirstBuyItem()));
        ItemStack secondBuy = offer.getDisplayedSecondBuyItem();
        if (!secondBuy.isEmpty()) {
            sb.append("_plus_");
            sb.append(getItemStackKey(secondBuy));
        }

        sb.append("_to_");
        sb.append(getItemStackKey(offer.getSellItem()));

        return sb.toString();
    }

    private static String getItemStackKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "empty";
        
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            var enchantments = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                var entrySet = enchantments.getEnchantmentEntries();
                var firstEntry = entrySet.iterator().next();
                String enchantId = firstEntry.getKey().getKey().get().getValue().toString();
                int level = firstEntry.getIntValue();
                return itemId + "_" + enchantId + "_" + level;
            }
        }
        return itemId;
    }
    
    public static boolean isFavorite(TradeOffer offer) {
        return FAVORITES.contains(getTradeIdentifier(offer));
    }

    public static void toggleFavorite(TradeOffer offer) {
        String id = getTradeIdentifier(offer);
        if (!FAVORITES.add(id)) {
            FAVORITES.remove(id);
        }
        save();
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try {
            FAVORITES.clear();
            FAVORITES.addAll(Files.readAllLines(FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        try {
            Files.write(FILE, FAVORITES);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}