package com.rodrigograc4.cherishedtrades.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

import java.util.HashSet;
import java.util.Set;

@Config(name = "cherished_trades")
public class CherishedTradesConfig implements ConfigData {

    public static CherishedTradesConfig INSTANCE;

    public Set<String> favoriteItems = new HashSet<>();

    public static void init() {
        AutoConfig.register(CherishedTradesConfig.class, JanksonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(CherishedTradesConfig.class).getConfig();
    }

    public static void save() {
        AutoConfig.getConfigHolder(CherishedTradesConfig.class).save();
    }
}
