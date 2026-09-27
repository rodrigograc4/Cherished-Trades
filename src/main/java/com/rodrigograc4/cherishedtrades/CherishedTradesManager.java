package com.rodrigograc4.cherishedtrades;

import com.google.gson.*;
import com.rodrigograc4.cherishedtrades.util.TradeKey;
import com.rodrigograc4.cherishedtrades.util.WorldKey;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.trading.MerchantOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CherishedTradesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("cherishedtrades");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int FORMAT_VERSION = 2;

    private static final Map<String, Map<UUID, Set<String>>> FAVORITES_BY_WORLD = new HashMap<>();
    private static Path FILE;

    public static void init() {
        FILE = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("cherished-trades.json");
        load();
    }

    public static boolean isFavorite(UUID villagerId, int serverIndex, MerchantOffer offer) {
        Map<UUID, Set<String>> favorites = FAVORITES_BY_WORLD.get(WorldKey.current());
        if (favorites == null) return false;
        Set<String> trades = favorites.get(villagerId);
        return trades != null && trades.contains(TradeKey.of(serverIndex, offer));
    }

    public static void toggleFavorite(UUID villagerId, int serverIndex, MerchantOffer offer) {
        String world = WorldKey.current();
        Map<UUID, Set<String>> favorites = FAVORITES_BY_WORLD.computeIfAbsent(world, k -> new HashMap<>());
        Set<String> trades = favorites.computeIfAbsent(villagerId, k -> new LinkedHashSet<>());
        String tradeKey = TradeKey.of(serverIndex, offer);

        if (!trades.add(tradeKey)) {
            trades.remove(tradeKey);
        }

        if (trades.isEmpty()) {
            favorites.remove(villagerId);
            if (favorites.isEmpty()) {
                FAVORITES_BY_WORLD.remove(world);
            }
        }

        save();
    }

    // Reads bookmarks stored as { "version": 2, "worlds": { world: { villager uuid: [trade keys] } } }.
    // Files from 1.1.0 and older used different keys and are ignored.
    private static void load() {
        FAVORITES_BY_WORLD.clear();
        if (!Files.exists(FILE)) return;

        try (Reader reader = Files.newBufferedReader(FILE)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return;

            if (!root.has("version") || root.get("version").getAsInt() != FORMAT_VERSION) {
                LOGGER.info("Ignoring bookmarks from an older version of Cherished Trades in {}", FILE);
                return;
            }

            for (var worldEntry : root.getAsJsonObject("worlds").entrySet()) {
                Map<UUID, Set<String>> villagers = new HashMap<>();

                for (var villagerEntry : worldEntry.getValue().getAsJsonObject().entrySet()) {
                    Set<String> trades = new LinkedHashSet<>();
                    villagerEntry.getValue().getAsJsonArray().forEach(t -> trades.add(t.getAsString()));
                    villagers.put(UUID.fromString(villagerEntry.getKey()), trades);
                }

                FAVORITES_BY_WORLD.put(worldEntry.getKey(), villagers);
            }

        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load {}, starting with no bookmarks", FILE, e);
            FAVORITES_BY_WORLD.clear();
        }
    }

    private static void save() {
        JsonObject worlds = new JsonObject();

        for (var worldEntry : FAVORITES_BY_WORLD.entrySet()) {
            JsonObject villagers = new JsonObject();

            for (var villagerEntry : worldEntry.getValue().entrySet()) {
                JsonArray trades = new JsonArray();
                villagerEntry.getValue().forEach(trades::add);
                villagers.add(villagerEntry.getKey().toString(), trades);
            }

            worlds.add(worldEntry.getKey(), villagers);
        }

        JsonObject root = new JsonObject();
        root.addProperty("version", FORMAT_VERSION);
        root.add("worlds", worlds);

        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save {}", FILE, e);
        }
    }
}
