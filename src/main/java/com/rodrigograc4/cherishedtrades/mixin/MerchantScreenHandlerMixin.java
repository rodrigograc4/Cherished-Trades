package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreenHandler.class)
public class MerchantScreenHandlerMixin {

    @Inject(method = "setOffers", at = @At("HEAD"))
    private void onSetOffers(TradeOfferList offers, CallbackInfo ci) {
        if (offers == null || offers.isEmpty()) return;

        TradeOfferList favorites = new TradeOfferList();
        TradeOfferList others = new TradeOfferList();

        for (TradeOffer offer : offers) {
            // Agora verifica o Stack completo (Item + Encantamento)
            if (CherishedTradesManager.isFavorite(offer.getSellItem())) {
                favorites.add(offer);
            } else {
                others.add(offer);
            }
        }

        offers.clear();
        offers.addAll(favorites);
        offers.addAll(others);
    }
}