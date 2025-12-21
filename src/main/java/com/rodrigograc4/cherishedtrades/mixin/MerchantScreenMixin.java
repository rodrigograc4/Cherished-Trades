package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void cherishedTrades$drawStars(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        MerchantScreen screen = (MerchantScreen) (Object) this;
        MerchantScreenHandler handler = screen.getScreenHandler();
        TradeOfferList offers = handler.getRecipes();
        if (offers == null || offers.isEmpty()) return;

        int offset = ((MerchantScreenAccessor) screen).cherishedTrades$getIndexStartOffset();

        for (int i = 0; i < 7; i++) {
            int recipeIndex = i + offset;
            if (recipeIndex >= offers.size()) break;

            TradeOffer offer = offers.get(recipeIndex);
            int starX = 2;
            int starY = 18 + (i * 20); 

            // Passamos o ItemStack completo agora
            boolean favorite = CherishedTradesManager.isFavorite(offer.getSellItem());

            String star = favorite ? "★" : "☆";
            int color = favorite ? 0xFFFFD700 : 0xFFFFFFFF;

            context.drawText(screen.getTextRenderer(), star, starX, starY, color, true);
        }
    }

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
                // Toggle usando o ItemStack
                CherishedTradesManager.toggleFavorite(offers.get(recipeIndex).getSellItem());
                
                // Força a atualização da lista no Handler para reordenar na hora
                handler.setOffers(offers);
                
                cir.setReturnValue(true);
                return;
            }
        }
    }
}