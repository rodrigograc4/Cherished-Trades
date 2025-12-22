package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.IHandlerIndex;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    @Unique
    private static final Identifier FILLED_STAR = Identifier.of("cherishedtrades", "textures/star.png");
    @Unique
    private static final Identifier EMPTY_STAR = Identifier.of("cherishedtrades", "textures/emptystar.png");

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
            int starX = 1;
            int starY = 22 + (i * 20); 

            boolean favorite = CherishedTradesManager.isFavorite(offer.getSellItem());
            Identifier texture = favorite ? FILLED_STAR : EMPTY_STAR;

            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, 
                texture, 
                starX, 
                starY, 
                0.0F, 
                0.0F, 
                9, 
                9, 
                9, 
                9
            );
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

            int starX = 1;
            int starY = 22 + (i * 20); 

            if (localX >= starX && localX <= starX + 12 && localY >= starY && localY <= starY + 12) {
                CherishedTradesManager.toggleFavorite(offers.get(recipeIndex).getSellItem());
                handler.setOffers(offers); 
                cir.setReturnValue(true);
                return;
            }
        }
    }

    @Inject(method = "syncRecipeIndex", at = @At("HEAD"), cancellable = true)
    private void onSyncRecipeIndex(CallbackInfo ci) {
        MerchantScreen screen = (MerchantScreen) (Object) this;
        MerchantScreenHandler handler = screen.getScreenHandler();
        
        int visualIndex = ((MerchantScreenAccessor) screen).cherishedTrades$getSelectedIndex();
        IHandlerIndex indexHelper = (IHandlerIndex) handler;
        int realIndex = indexHelper.cherishedTrades$getRealIndex(visualIndex);

        if (handler.getRecipes().size() > visualIndex) {
            handler.setRecipeIndex(visualIndex);
            handler.switchTo(visualIndex);
            
            if (net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() != null) {
                net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler().sendPacket(
                    new net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket(realIndex)
                );
            }
        }
        
        ci.cancel(); 
    }
}