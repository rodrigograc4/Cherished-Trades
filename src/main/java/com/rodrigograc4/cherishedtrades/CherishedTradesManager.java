package com.rodrigograc4.cherishedtrades;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.village.TradeOffer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CherishedTradesManager {

    private static final Map<UUID, Set<String>> FAVORITES = new HashMap<>();
    private static Path FILE;

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
        FAVORITES.clear();
        if (!Files.exists(FILE)) return;

        try (Reader reader = Files.newBufferedReader(FILE)) {
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("bookmarks")) return;

            JsonArray bookmarks = root.getAsJsonArray("bookmarks");
            for (var element : bookmarks) {
                JsonObject obj = element.getAsJsonObject();
                UUID villagerId = UUID.fromString(obj.get("villager").getAsString());
                Set<String> trades = new HashSet<>();
                obj.getAsJsonArray("trades").forEach(t -> trades.add(t.getAsString()));
                FAVORITES.put(villagerId, trades);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray bookmarks = new JsonArray();

        for (var entry : FAVORITES.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("villager", entry.getKey().toString());

            JsonArray trades = new JsonArray();
            entry.getValue().forEach(trades::add);

            obj.add("trades", trades);
            bookmarks.add(obj);
        }

        JsonObject root = new JsonObject();
        root.add("bookmarks", bookmarks);

        try (Writer writer = Files.newBufferedWriter(FILE)) {
            gson.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
