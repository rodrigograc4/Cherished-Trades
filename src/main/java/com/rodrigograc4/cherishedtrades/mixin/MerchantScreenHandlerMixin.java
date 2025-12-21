package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.IHandlerIndex;
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
public class MerchantScreenHandlerMixin implements IHandlerIndex {

    @Unique
    private final List<Integer> cherishedTrades$originalIndices = new ArrayList<>();
    
    // Esta lista guarda as ofertas ORIGINAIS do servidor e nunca muda a ordem
    @Unique
    private final List<TradeOffer> cherishedTrades$snapshot = new ArrayList<>();

    @Inject(method = "setOffers", at = @At("HEAD"))
    private void onSetOffers(TradeOfferList offers, CallbackInfo ci) {
        if (offers == null || offers.isEmpty()) return;

        // Se a snapshot estiver vazia, significa que o Villager acabou de ser aberto.
        // Guardamos a ordem exata do servidor.
        if (cherishedTrades$snapshot.isEmpty()) {
            cherishedTrades$snapshot.addAll(offers);
        }

        TradeOfferList favorites = new TradeOfferList();
        TradeOfferList others = new TradeOfferList();
        cherishedTrades$originalIndices.clear();

        // SEMPRE usamos a snapshot (ordem real) para decidir a nova ordem visual
        for (TradeOffer offer : cherishedTrades$snapshot) {
            if (CherishedTradesManager.isFavorite(offer.getSellItem())) {
                favorites.add(offer);
            } else {
                others.add(offer);
            }
        }

        // Limpamos a lista que o Minecraft usa e reconstruímos
        offers.clear();
        offers.addAll(favorites);
        offers.addAll(others);

        // Criamos o mapa de tradução para o servidor não se enganar
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