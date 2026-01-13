package com.rodrigograc4.cherishedtrades.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

@Config(name = "cherished-trades")
public class CherishedTradesConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static transient CherishedTradesConfig INSTANCE;

    @ConfigEntry.Gui.Tooltip
    public boolean enableBookmarks = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enablePriceChecker = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 50)
    public int priceOffset = 0;

    public static void init() {
        AutoConfig.register(CherishedTradesConfig.class, JanksonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(CherishedTradesConfig.class).getConfig();
    }

    public static void save() {
        AutoConfig.getConfigHolder(CherishedTradesConfig.class).save();
    }
}
