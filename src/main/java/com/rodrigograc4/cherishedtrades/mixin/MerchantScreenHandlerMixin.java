package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.IHandlerIndex; // Importa a interface
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(MerchantScreenHandler.class)
public class MerchantScreenHandlerMixin implements IHandlerIndex { // Implementa aqui

    @Unique
    private final List<Integer> cherishedTrades$originalIndices = new ArrayList<>();

    @Inject(method = "setOffers", at = @At("HEAD"))
    private void onSetOffers(TradeOfferList offers, CallbackInfo ci) {
        if (offers == null || offers.isEmpty()) return;

        List<TradeOffer> originalList = new ArrayList<>(offers);
        TradeOfferList favorites = new TradeOfferList();
        TradeOfferList others = new TradeOfferList();
        
        cherishedTrades$originalIndices.clear();

        for (TradeOffer offer : originalList) {
            if (CherishedTradesManager.isFavorite(offer.getSellItem())) {
                favorites.add(offer);
            }
        }
        for (TradeOffer offer : originalList) {
            if (!CherishedTradesManager.isFavorite(offer.getSellItem())) {
                others.add(offer);
            }
        }

        offers.clear();
        offers.addAll(favorites);
        offers.addAll(others);

        for (TradeOffer visualOffer : offers) {
            for (int originalIdx = 0; originalIdx < originalList.size(); originalIdx++) {
                if (visualOffer == originalList.get(originalIdx)) {
                    cherishedTrades$originalIndices.add(originalIdx);
                    break;
                }
            }
        }
    }

    @Override // Agora é um Override da interface
    public int cherishedTrades$getRealIndex(int visualIndex) {
        if (visualIndex >= 0 && visualIndex < cherishedTrades$originalIndices.size()) {
            return cherishedTrades$originalIndices.get(visualIndex);
        }
        return visualIndex;
    }
}