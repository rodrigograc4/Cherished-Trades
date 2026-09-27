package com.rodrigograc4.cherishedtrades.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Locale;

public final class WorldKey {
    private static final String UNKNOWN = "unknown";

    private static @Nullable String cached;

    private WorldKey() {}

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> cached = null);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> cached = null);
    }

    // Key of the current world/server, resolved once per connection and cached.
    public static String current() {
        if (cached == null) {
            cached = resolve(Minecraft.getInstance());
        }
        return cached;
    }

    // Uses stable ids so renames keep bookmarks: sp:<save folder>, mp:<address> or realm:<name>
    // (Realm addresses can change between sessions, and server list names are user-editable).
    private static String resolve(Minecraft client) {
        IntegratedServer integrated = client.getSingleplayerServer();
        if (integrated != null) {
            Path saveDir = integrated.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
            return "sp:" + saveDir.getFileName();
        }

        ServerData server = client.getCurrentServer();
        if (server != null) {
            return server.isRealm() ? "realm:" + server.name : "mp:" + server.ip.toLowerCase(Locale.ROOT);
        }

        return UNKNOWN;
    }
}
