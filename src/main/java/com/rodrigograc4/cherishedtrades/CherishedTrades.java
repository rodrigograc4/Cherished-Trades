package com.rodrigograc4.cherishedtrades;

import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import net.fabricmc.api.ClientModInitializer;

public class CherishedTrades implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CherishedTradesConfig.init();
    }
}
