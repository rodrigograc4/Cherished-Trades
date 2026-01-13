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
import java.util.*;

public class CherishedTradesManager {

    private static final Map<UUID, Set<String>> FAVORITES = new HashMap<>();
    private static Path FILE;

    private static final String TRADE_SEPARATOR = "|";

    public static void init() {
        FILE = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("cherished-trades.json");
        load();
    }

    public static boolean isFavorite(UUID villagerId, TradeOffer offer) {
        Set<String> favorites = FAVORITES.get(villagerId);
        if (favorites == null) return false;
        return favorites.contains(getTradeIdentifier(offer));
    }

    public static void toggleFavorite(UUID villagerId, TradeOffer offer) {
        Set<String> favorites = FAVORITES.computeIfAbsent(villagerId, id -> new HashSet<>());
        String tradeId = getTradeIdentifier(offer);

        if (!favorites.add(tradeId)) {
            favorites.remove(tradeId);
        }

        if (favorites.isEmpty()) {
            FAVORITES.remove(villagerId);
        }

        save();
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
                var entry = enchantments.getEnchantmentEntries().iterator().next();
                String enchantId = entry.getKey().getKey().get().getValue().toString();
                int level = entry.getIntValue();
                return itemId + "_" + enchantId + "_" + level;
            }
        }

        return itemId;
    }

    private static void load() {
        FAVORITES.clear();
        if (!Files.exists(FILE)) return;

        try {
            for (String line : Files.readAllLines(FILE)) {
                String[] split = line.split(":", 2);
                if (split.length != 2) continue;

                UUID villagerId;
                try {
                    villagerId = UUID.fromString(split[0]);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                Set<String> trades = new HashSet<>();
                if (!split[1].isEmpty()) {
                    trades.addAll(Arrays.asList(split[1].split("\\" + TRADE_SEPARATOR)));
                }

                FAVORITES.put(villagerId, trades);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        List<String> lines = new ArrayList<>();

        for (var entry : FAVORITES.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            String line = entry.getKey() + ":" + String.join(TRADE_SEPARATOR, entry.getValue());
            lines.add(line);
        }

        try {
            Files.write(FILE, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}