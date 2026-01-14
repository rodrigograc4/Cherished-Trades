package com.rodrigograc4.cherishedtrades;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CherishedTradesManager {

    private static final Map<String, Map<UUID, Set<String>>> FAVORITES_BY_WORLD = new HashMap<>();
    private static Path FILE;

    public static void init() {
        FILE = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("cherished-trades.json");
        load();
    }

    private static Map<UUID, Set<String>> getWorldMap(String worldName) {
        return FAVORITES_BY_WORLD.computeIfAbsent(worldName, k -> new HashMap<>());
    }

    public static boolean isFavorite(String worldName, UUID villagerId, TradeOffer offer) {
        Map<UUID, Set<String>> favorites = getWorldMap(worldName);
        Set<String> trades = favorites.get(villagerId);
        return trades != null && trades.contains(getTradeIdentifier(offer));
    }

    public static void toggleFavorite(String worldName, UUID villagerId, TradeOffer offer) {
        Map<UUID, Set<String>> favorites = getWorldMap(worldName);
        Set<String> trades = favorites.computeIfAbsent(villagerId, k -> new HashSet<>());
        String tradeId = getTradeIdentifier(offer);

        if (!trades.add(tradeId)) {
            trades.remove(tradeId);
        }

        if (trades.isEmpty()) {
            favorites.remove(villagerId);
        }

        save();
    }

    public static String getTradeIdentifier(TradeOffer offer) {
        StringBuilder sb = new StringBuilder();
        sb.append(getItemStackKey(offer.getOriginalFirstBuyItem()));

        ItemStack secondBuy = offer.getDisplayedSecondBuyItem();
        if (!secondBuy.isEmpty()) {
            sb.append("_plus_").append(getItemStackKey(secondBuy));
        }

        sb.append("_to_").append(getItemStackKey(offer.getSellItem()));
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
        FAVORITES_BY_WORLD.clear();
        if (!Files.exists(FILE)) return;

        try (Reader reader = Files.newBufferedReader(FILE)) {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("worlds")) return;

            JsonObject worlds = root.getAsJsonObject("worlds");
            for (String worldName : worlds.keySet()) {
                JsonArray bookmarksArray = worlds.getAsJsonArray(worldName);
                Map<UUID, Set<String>> worldFavorites = new HashMap<>();

                for (JsonElement element : bookmarksArray) {
                    JsonObject obj = element.getAsJsonObject();
                    UUID villagerId = UUID.fromString(obj.get("villager").getAsString());
                    Set<String> trades = new HashSet<>();
                    obj.getAsJsonArray("trades").forEach(t -> trades.add(t.getAsString()));
                    worldFavorites.put(villagerId, trades);
                }

                FAVORITES_BY_WORLD.put(worldName, worldFavorites);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject root = new JsonObject();
        JsonObject worlds = new JsonObject();

        for (var worldEntry : FAVORITES_BY_WORLD.entrySet()) {
            String worldName = worldEntry.getKey();
            Map<UUID, Set<String>> favorites = worldEntry.getValue();
            JsonArray bookmarksArray = new JsonArray();

            for (var entry : favorites.entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("villager", entry.getKey().toString());
                JsonArray tradesArray = new JsonArray();
                entry.getValue().forEach(tradesArray::add);
                obj.add("trades", tradesArray);
                bookmarksArray.add(obj);
            }

            worlds.add(worldName, bookmarksArray);
        }

        root.add("worlds", worlds);

        try (Writer writer = Files.newBufferedWriter(FILE)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
