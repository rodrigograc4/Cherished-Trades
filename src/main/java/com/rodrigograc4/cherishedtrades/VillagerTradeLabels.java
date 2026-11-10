package com.rodrigograc4.cherishedtrades;

import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lines shown above a villager the player is looking at, one per bookmarked trade, e.g.
 * "Sharpness V - 17 [emerald]". Built from the stored trade keys, so it works without opening
 * the trade screen again.
 */
public final class VillagerTradeLabels {

    private static final double LOOK_RANGE = 16.0;

    // Drawn inside the text after the price, as textures/gui/sprites/emerald_icon.png: the emerald
    // item texture cut to 14x14 around the gem. Text sprites fill an 8 px square that starts 1 px
    // above the 7 px tall digits, so the gem (12 texels tall) is pushed to the bottom with 2 empty
    // rows on top, which makes it as tall as the digits; 2 empty columns on the left space it
    // from the number.
    private static final AtlasSprite EMERALD_ICON = new AtlasSprite(AtlasIds.GUI, Identifier.fromNamespaceAndPath("cherishedtrades", "emerald_icon"));

    // One stack in a trade key, as written by TradeKey: "minecraft:emerald*17[minecraft:sharpness@5]".
    private static final Pattern STACK = Pattern.compile("^([^*\\[]+)\\*(\\d+)((?:\\[[^\\]]+])*)$");
    private static final Pattern ENCHANTMENT = Pattern.compile("\\[([^@\\]]+)@(\\d+)]");

    private VillagerTradeLabels() {
    }

    public static List<Component> linesFor(Entity entity, float partialTick) {
        if (!(entity instanceof AbstractVillager)) return List.of();
        if (!CherishedTradesConfig.INSTANCE.enableBookmarks) return List.of();

        Set<String> favorites = CherishedTradesManager.getFavorites(entity.getUUID());
        if (favorites.isEmpty() || !isLookedAt(entity, partialTick)) return List.of();

        List<Component> lines = new ArrayList<>();
        for (String key : favorites) {
            describe(key).ifPresent(lines::add);
        }
        return lines;
    }

    private static boolean isLookedAt(Entity entity, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity camera = minecraft.getCameraEntity();
        // Not while a screen (like the trade screen itself) covers the world, or with the HUD
        // hidden (F1), which hides vanilla name tags too.
        if (camera == null || minecraft.gui.screen() != null || minecraft.gui.hud.isHidden()) return false;

        Vec3 eye = camera.getEyePosition(partialTick);
        Vec3 end = eye.add(camera.getViewVector(partialTick).scale(LOOK_RANGE));
        AABB box = entity.getBoundingBox().inflate(0.2);
        return box.contains(eye) || box.clip(eye, end).isPresent();
    }

    // Trade keys are "serverIndex|costA[+costB]>result".
    private static Optional<Component> describe(String key) {
        String fingerprint = key.substring(key.indexOf('|') + 1);
        int arrow = fingerprint.lastIndexOf('>');
        if (arrow < 0) return Optional.empty();

        String costA = fingerprint.substring(0, arrow).split("\\+")[0];
        Optional<Stack> cost = Stack.parse(costA);
        Optional<Stack> result = Stack.parse(fingerprint.substring(arrow + 1));
        if (cost.isEmpty() || result.isEmpty()) return Optional.empty();

        return Optional.of(Component.empty()
                .append(describeResult(result.get()))
                .append(" - ")
                .append(describeCost(cost.get())));
    }

    private static Component describeResult(Stack result) {
        if (result.item() == Items.ENCHANTED_BOOK && !result.enchantments().isEmpty()) {
            MutableComponent names = Component.empty();
            for (int i = 0; i < result.enchantments().size(); i++) {
                if (i > 0) names.append(", ");
                names.append(enchantmentName(result.enchantments().get(i)));
            }
            return names;
        }

        Component name = new ItemStack(result.item()).getHoverName();
        return result.count() > 1 ? Component.literal(result.count() + "× ").append(name) : name;
    }

    private static Component describeCost(Stack cost) {
        if (cost.item() != Items.EMERALD) {
            return Component.literal(cost.count() + "× ").append(new ItemStack(cost.item()).getHoverName());
        }
        return Component.literal(String.valueOf(cost.count())).append(Component.object(EMERALD_ICON));
    }

    // Like Enchantment.getFullname, but without its gray color so it matches the rest of the line.
    private static Component enchantmentName(StoredEnchantment stored) {
        Optional<Holder.Reference<Enchantment>> holder = enchantment(stored.id());
        MutableComponent name = holder.map(h -> h.value().description().copy())
                .orElseGet(() -> Component.literal(stored.id()));
        int maxLevel = holder.map(h -> h.value().getMaxLevel()).orElse(0);
        if (stored.level() != 1 || maxLevel != 1) {
            name.append(" ").append(Component.translatable("enchantment.level." + stored.level()));
        }
        return name;
    }

    private static Optional<Holder.Reference<Enchantment>> enchantment(String id) {
        var level = Minecraft.getInstance().level;
        Identifier identifier = Identifier.tryParse(id);
        if (level == null || identifier == null) return Optional.empty();
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceKey.create(Registries.ENCHANTMENT, identifier));
    }

    private record StoredEnchantment(String id, int level) {
    }

    private record Stack(Item item, int count, List<StoredEnchantment> enchantments) {
        static Optional<Stack> parse(String text) {
            Matcher matcher = STACK.matcher(text);
            if (!matcher.matches()) return Optional.empty();

            Identifier id = Identifier.tryParse(matcher.group(1));
            if (id == null) return Optional.empty();
            Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
            if (item.isEmpty()) return Optional.empty();

            List<StoredEnchantment> enchantments = new ArrayList<>();
            Matcher enchantment = ENCHANTMENT.matcher(matcher.group(3));
            while (enchantment.find()) {
                enchantments.add(new StoredEnchantment(enchantment.group(1), Integer.parseInt(enchantment.group(2))));
            }
            return Optional.of(new Stack(item.get(), Integer.parseInt(matcher.group(2)), enchantments));
        }
    }
}
