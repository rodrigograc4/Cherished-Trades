package com.rodrigograc4.cherishedtrades.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.IHandlerIndex;
import com.rodrigograc4.cherishedtrades.IMerchantScreenHandlerMixin;
import com.rodrigograc4.cherishedtrades.util.SyntheticVillagerUUID;

import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

@Mixin(MerchantScreenHandler.class)
public class MerchantScreenHandlerMixin implements IHandlerIndex, IMerchantScreenHandlerMixin {

    @Unique
    private final List<Integer> cherishedTrades$originalIndices = new ArrayList<>();
    
    @Unique
    private final List<TradeOffer> cherishedTrades$snapshot = new ArrayList<>();

    @Override
    public List<TradeOffer> cherishedTrades$getSnapshot() {
        return cherishedTrades$snapshot;
    }

    @Inject(method = "setOffers", at = @At("HEAD"))
    private void onSetOffers(TradeOfferList offers, CallbackInfo ci) {
        if (offers == null || offers.isEmpty()) return;

        if (cherishedTrades$snapshot.isEmpty()) {
            cherishedTrades$snapshot.addAll(offers);
        }

        TradeOfferList favorites = new TradeOfferList();
        TradeOfferList others = new TradeOfferList();
        cherishedTrades$originalIndices.clear();

        List<TradeOffer> originalSnapshot = cherishedTrades$getSnapshot();

        TradeOfferList originalOffers = new TradeOfferList();
        originalOffers.addAll(originalSnapshot);

        UUID villagerId = SyntheticVillagerUUID.fromTrades(originalOffers);

        for (TradeOffer offer : cherishedTrades$snapshot) {
            if (CherishedTradesManager.isFavorite(villagerId, offer)) {
                favorites.add(offer);
            } else {
                others.add(offer);
            }
        }

        offers.clear();
        offers.addAll(favorites);
        offers.addAll(others);

        for (TradeOffer visualOffer : offers) {
            for (int i = 0; i < cherishedTrades$snapshot.size(); i++) {
                if (visualOffer == cherishedTrades$snapshot.get(i)) {
                    cherishedTrades$originalIndices.add(i);
                    break;
                }
            }
        }
    }

    @Override
    public int cherishedTrades$getRealIndex(int visualIndex) {
        if (visualIndex >= 0 && visualIndex < cherishedTrades$originalIndices.size()) {
            return cherishedTrades$originalIndices.get(visualIndex);
        }
        return visualIndex;
    }
}