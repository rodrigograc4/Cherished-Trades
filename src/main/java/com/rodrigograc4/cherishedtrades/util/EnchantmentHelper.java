package com.rodrigograc4.cherishedtrades.util;

import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnchantmentHelper {

    private static List<String> cachedEntries = List.of();
    private static Set<String> cachedIds = Set.of();

    public static String convertToIdFormat(String displayName) {
        String name = displayName.toLowerCase().trim();
        int level = 1;

        if (name.endsWith(" 5")) { level = 5; name = name.substring(0, name.length() - 2); }
        else if (name.endsWith(" 4")) { level = 4; name = name.substring(0, name.length() - 2); }
        else if (name.endsWith(" 3")) { level = 3; name = name.substring(0, name.length() - 2); }
        else if (name.endsWith(" 2")) { level = 2; name = name.substring(0, name.length() - 2); }
        else if (name.endsWith(" 1")) { level = 1; name = name.substring(0, name.length() - 2); }

        name = name.trim().replace(" ", "_");

        if (name.equals("curse_of_vanishing")) name = "vanishing_curse";
        if (name.equals("curse_of_binding")) name = "binding_curse";

        return "minecraft:" + name + " " + level;
    }

    public static boolean isHighlighted(ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) return false;

        var enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        Set<String> highlighted = getHighlightedIds();
        if (highlighted.isEmpty()) return false;

        for (var entry : enchantments.entrySet()) {
            if (highlighted.contains(entry.getKey().getRegisteredName() + " " + entry.getIntValue())) {
                return true;
            }
        }
        return false;
    }

    // The config list can be edited from Mod Menu at any time, so rebuild only when it changes.
    private static Set<String> getHighlightedIds() {
        List<String> entries = CherishedTradesConfig.INSTANCE.highlightedEnchantments;
        if (!entries.equals(cachedEntries)) {
            Set<String> ids = new HashSet<>();
            for (String entry : entries) {
                ids.add(convertToIdFormat(entry));
            }
            cachedEntries = List.copyOf(entries);
            cachedIds = ids;
        }
        return cachedIds;
    }
}
