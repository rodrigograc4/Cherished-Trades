package com.rodrigograc4.cherishedtrades.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.rodrigograc4.cherishedtrades.VillagerLabelState;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements VillagerLabelState {

    @Unique
    private List<Component> cherishedTrades$tradeLines = List.of();

    @Override
    public List<Component> cherishedTrades$getTradeLines() {
        return cherishedTrades$tradeLines;
    }

    @Override
    public void cherishedTrades$setTradeLines(List<Component> lines) {
        cherishedTrades$tradeLines = lines;
    }
}
