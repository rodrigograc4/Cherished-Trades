package com.rodrigograc4.cherishedtrades;

import java.util.List;
import net.minecraft.village.TradeOffer;

public interface IMerchantScreenHandlerMixin {
    List<TradeOffer> cherishedTrades$getSnapshot();
}
