package com.rodrigograc4.cherishedtrades.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rodrigograc4.cherishedtrades.CherishedMerchantMenu;
import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.VillagerInteractionTracker;
import com.rodrigograc4.cherishedtrades.util.TradeKey;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin implements CherishedMerchantMenu {

    @Shadow
    public abstract MerchantOffers getOffers();

    @Unique
    private final List<MerchantOffer> cherishedTrades$serverOrder = new ArrayList<>();

    @Unique
    private int[] cherishedTrades$realIndices = new int[0];

    @Unique
    private boolean[] cherishedTrades$favorites = new boolean[0];

    @Unique
    private UUID cherishedTrades$villagerId;

    // Client-side constructor, created when the server opens a trade screen.
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void cherishedTrades$captureVillager(int containerId, Inventory inventory, CallbackInfo ci) {
        cherishedTrades$villagerId = VillagerInteractionTracker.consume();
    }

    // Every call is a fresh list from the server (e.g. after a level-up), so it replaces the old order.
    @Inject(method = "setOffers", at = @At("HEAD"))
    private void cherishedTrades$onSetOffers(MerchantOffers offers, CallbackInfo ci) {
        if (offers == null || offers.isEmpty()) return;

        cherishedTrades$serverOrder.clear();
        cherishedTrades$serverOrder.addAll(offers);

        if (cherishedTrades$villagerId == null) {
            cherishedTrades$villagerId = TradeKey.fallbackVillagerId(cherishedTrades$serverOrder);
        }

        int size = cherishedTrades$serverOrder.size();
        cherishedTrades$favorites = new boolean[size];
        for (int i = 0; i < size; i++) {
            cherishedTrades$favorites[i] = CherishedTradesManager.isFavorite(cherishedTrades$villagerId, i, cherishedTrades$serverOrder.get(i));
        }

        cherishedTrades$sort(offers);
    }

    // Stable partition: favorites first, each group keeping the server's order.
    @Unique
    private void cherishedTrades$sort(MerchantOffers offers) {
        int size = cherishedTrades$serverOrder.size();

        int[] realIndices = new int[size];
        int next = 0;
        for (int i = 0; i < size; i++) {
            if (cherishedTrades$favorites[i]) realIndices[next++] = i;
        }
        for (int i = 0; i < size; i++) {
            if (!cherishedTrades$favorites[i]) realIndices[next++] = i;
        }

        offers.clear();
        for (int realIndex : realIndices) {
            offers.add(cherishedTrades$serverOrder.get(realIndex));
        }
        cherishedTrades$realIndices = realIndices;
    }

    @Override
    public int cherishedTrades$getRealIndex(int visualIndex) {
        if (visualIndex >= 0 && visualIndex < cherishedTrades$realIndices.length) {
            return cherishedTrades$realIndices[visualIndex];
        }
        return visualIndex;
    }

    @Override
    public boolean cherishedTrades$isFavorite(int visualIndex) {
        int realIndex = cherishedTrades$getRealIndex(visualIndex);
        return realIndex >= 0 && realIndex < cherishedTrades$favorites.length && cherishedTrades$favorites[realIndex];
    }

    @Override
    public void cherishedTrades$toggleFavorite(int visualIndex) {
        int realIndex = cherishedTrades$getRealIndex(visualIndex);
        if (realIndex < 0 || realIndex >= cherishedTrades$favorites.length) return;

        CherishedTradesManager.toggleFavorite(cherishedTrades$villagerId, realIndex, cherishedTrades$serverOrder.get(realIndex));
        cherishedTrades$favorites[realIndex] = !cherishedTrades$favorites[realIndex];
        cherishedTrades$sort(getOffers());
    }
}
