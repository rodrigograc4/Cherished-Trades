package com.rodrigograc4.cherishedtrades.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

import java.util.ArrayList;
import java.util.List;

@Config(name = "cherished-trades")
public class CherishedTradesConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static transient CherishedTradesConfig INSTANCE;

    @ConfigEntry.Gui.Tooltip
    public boolean enableBookmarks = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableBookmarkLabels = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableHighlightedBooks = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enablePriceChecker = true;

    /**
     * How many of a book's possible prices count as a good deal, as a percentage of them starting
     * from the cheapest. Replaces priceOffset (a flat number of emeralds) from 1.3.1 and older.
     */
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 50)
    public int dealPercent = 5;

    @ConfigEntry.Gui.Tooltip
    public List<String> highlightedEnchantments = new ArrayList<>(List.of("minecraft:mending 1", "minecraft:unbreaking 3"));

    public static void init() {
        AutoConfig.register(CherishedTradesConfig.class, JanksonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(CherishedTradesConfig.class).getConfig();
    }

    public static void save() {
        AutoConfig.getConfigHolder(CherishedTradesConfig.class).save();
    }
}
