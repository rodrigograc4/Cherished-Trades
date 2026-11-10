package com.rodrigograc4.cherishedtrades;

import net.minecraft.network.chat.Component;

import java.util.List;

/** Added to EntityRenderState by a mixin: the bookmarked-trade lines to draw above a villager. */
public interface VillagerLabelState {

    List<Component> cherishedTrades$getTradeLines();

    void cherishedTrades$setTradeLines(List<Component> lines);
}
