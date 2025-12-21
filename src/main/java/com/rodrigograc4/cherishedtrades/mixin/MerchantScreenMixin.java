package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    /* ---------------- DESENHAR ESTRELAS ---------------- */
    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void cherishedTrades$drawStars(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        MerchantScreen screen = (MerchantScreen) (Object) this;
        MerchantScreenHandler handler = screen.getScreenHandler();
        TradeOfferList offers = handler.getRecipes();
        if (offers == null || offers.isEmpty()) return;

        // Usamos o Accessor para saber onde o scroll está
        int offset = ((MerchantScreenAccessor) screen).cherishedTrades$getIndexStartOffset();

        for (int i = 0; i < 7; i++) { // O Minecraft só mostra 7 trocas por página
            int recipeIndex = i + offset;
            if (recipeIndex >= offers.size()) break;

            TradeOffer offer = offers.get(recipeIndex);
            
            // starX = 2 (canto esquerdo do botão de troca)
            // starY = 18 (início da lista) + (posição visual * altura do botão)
            int starX = 2;
            int starY = 18 + (i * 20); 

            Item item = offer.getSellItem().getItem();
            boolean favorite = CherishedTradesManager.isFavorite(item);

            String star = favorite ? "★" : "☆";
            int color = favorite ? 0xFFFFD700 : 0xFFFFFFFF;

            context.drawText(screen.getTextRenderer(), star, starX, starY, color, true);
        }
    }

    /* ---------------- CLIQUE NAS ESTRELAS ---------------- */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void cherishedTrades$clickStar(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        MerchantScreen screen = (MerchantScreen) (Object) this;
        MerchantScreenHandler handler = screen.getScreenHandler();
        TradeOfferList offers = handler.getRecipes();
        if (offers == null || offers.isEmpty()) return;

        int offset = ((MerchantScreenAccessor) screen).cherishedTrades$getIndexStartOffset();
        HandledScreenAccessor gui = (HandledScreenAccessor) screen;

        double localX = click.x() - gui.cherishedTrades$getX();
        double localY = click.y() - gui.cherishedTrades$getY();

        for (int i = 0; i < 7; i++) {
            int recipeIndex = i + offset;
            if (recipeIndex >= offers.size()) break;

            int starX = 2;
            int starY = 18 + (i * 20);

            if (localX >= starX && localX <= starX + 12 && localY >= starY && localY <= starY + 12) {
                Item item = offers.get(recipeIndex).getSellItem().getItem();
                CherishedTradesManager.toggleFavorite(item);
                
                // REORDENAÇÃO IMEDIATA:
                // Chamamos o setOffers que acabámos de mixinar para reorganizar a UI agora!
                handler.setOffers(offers);
                
                cir.setReturnValue(true);
                return;
            }
        }
    }
}