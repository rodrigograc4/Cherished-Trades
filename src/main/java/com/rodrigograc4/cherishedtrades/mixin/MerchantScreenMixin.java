package com.rodrigograc4.cherishedtrades.mixin;

import java.util.List;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.IHandlerIndex;
import com.rodrigograc4.cherishedtrades.IMerchantScreenHandlerMixin;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import com.rodrigograc4.cherishedtrades.util.SyntheticVillagerUUID;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    @Unique
    private static final Identifier FILLED_BOOKMARK = Identifier.of("cherishedtrades", "textures/bookmark.png");
    @Unique
    private static final Identifier EMPTY_BOOKMARK = Identifier.of("cherishedtrades", "textures/emptybookmark.png");

    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void cherishedTrades$drawBookmarks(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {

        if (!CherishedTradesConfig.INSTANCE.enableBookmarks) {
            return;
        }

        MerchantScreen screen = (MerchantScreen) (Object) this;
        MerchantScreenHandler handler = screen.getScreenHandler();
        TradeOfferList offers = handler.getRecipes();
        if (offers == null || offers.isEmpty()) return;

        int offset = ((MerchantScreenAccessor) screen).cherishedTrades$getIndexStartOffset();

        for (int i = 0; i < 7; i++) {
            int recipeIndex = i + offset;
            if (recipeIndex >= offers.size()) break;

            TradeOffer offer = offers.get(recipeIndex);
            int bookmarkX = 1;
            int bookmarkY = 18 + (i * 20); 

            IMerchantScreenHandlerMixin handlerMixin = (IMerchantScreenHandlerMixin) handler;
            List<TradeOffer> originalSnapshot = handlerMixin.cherishedTrades$getSnapshot();

            TradeOfferList originalOffers = new TradeOfferList();
            originalOffers.addAll(originalSnapshot);

            UUID villagerId = SyntheticVillagerUUID.fromTrades(originalOffers);


            boolean favorite = CherishedTradesManager.isFavorite(villagerId, offer);
            Identifier texture = favorite ? FILLED_BOOKMARK : EMPTY_BOOKMARK;

            context.drawTexture(
                net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, 
                texture, 
                bookmarkX, 
                bookmarkY, 
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
    private void cherishedTrades$clickBookmark(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {

        if (!CherishedTradesConfig.INSTANCE.enableBookmarks) {
            return;
        }
        
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

            int bookmarkX = 1;
            int bookmarkY = 18 + (i * 20); 

            IMerchantScreenHandlerMixin handlerMixin = (IMerchantScreenHandlerMixin) handler;
            List<TradeOffer> originalSnapshot = handlerMixin.cherishedTrades$getSnapshot();

            TradeOfferList originalOffers = new TradeOfferList();
            originalOffers.addAll(originalSnapshot);

            UUID villagerId = SyntheticVillagerUUID.fromTrades(originalOffers);

            if (localX >= bookmarkX && localX <= bookmarkX + 12 && localY >= bookmarkY && localY <= bookmarkY + 12) {
                CherishedTradesManager.toggleFavorite(villagerId, offers.get(recipeIndex));
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