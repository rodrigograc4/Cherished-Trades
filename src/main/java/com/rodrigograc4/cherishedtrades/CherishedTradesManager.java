package com.rodrigograc4.cherishedtrades;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

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
                .resolve("cherished_trades.json");
        load();
    }

    // Gera um ID único: "minecraft:enchanted_book_mending" ou apenas "minecraft:emerald"
    public static String getTradeIdentifier(ItemStack stack) {
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        
        if (stack.isOf(Items.ENCHANTED_BOOK)) {
            var enchantments = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);
            if (enchantments != null && !enchantments.isEmpty()) {
                // Pega o primeiro encantamento do livro
                var entry = enchantments.getEnchantments().iterator().next();
                String enchantId = entry.getKey().get().getValue().toString();
                return itemId + "_" + enchantId;
            }
        }
        return itemId;
    }

    public static boolean isFavorite(ItemStack stack) {
        return FAVORITES.contains(getTradeIdentifier(stack));
    }

    public static void toggleFavorite(ItemStack stack) {
        String id = getTradeIdentifier(stack);
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