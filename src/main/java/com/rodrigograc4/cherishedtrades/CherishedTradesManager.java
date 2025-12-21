package com.rodrigograc4.cherishedtrades;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
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

    public static boolean isFavorite(Item item) {
        return FAVORITES.contains(Registries.ITEM.getId(item).toString());
    }

    public static void toggleFavorite(Item item) {
        String id = Registries.ITEM.getId(item).toString();

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
