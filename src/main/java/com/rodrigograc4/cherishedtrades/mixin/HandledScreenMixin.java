package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.PriceChecker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Unique
    private static final Identifier GOLD_ARROW =
            Identifier.of("cherishedtrades", "textures/goldarrow.png");

    @Inject(method = "render", at = @At("TAIL"))
    private void cherishedTrades$renderArrowOnTop(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        if (!((Object) this instanceof MerchantScreen screen)) return;

        MerchantScreenHandler handler = screen.getScreenHandler();
        TradeOfferList offers = handler.getRecipes();
        if (offers == null || offers.isEmpty()) return;

        int offset = ((MerchantScreenAccessor) screen)
                .cherishedTrades$getIndexStartOffset();

        HandledScreenAccessor gui = (HandledScreenAccessor) screen;
        int baseX = gui.cherishedTrades$getX();
        int baseY = gui.cherishedTrades$getY();

        for (int i = 0; i < 7; i++) {
            int recipeIndex = i + offset;
            if (recipeIndex >= offers.size()) break;

            TradeOffer offer = offers.get(recipeIndex);
            if (!PriceChecker.isGreatDeal(offer)) continue;

            int arrowX = baseX + 60;
            int arrowY = baseY + 22 + (i * 20);

            context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                GOLD_ARROW,
                arrowX,
                arrowY,
                0.0F,
                0.0F,
                10,
                9,
                10,
                9
            );
        }
    }
}
