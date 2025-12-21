package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    /* ===================== SHADOWS ===================== */

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected TextRenderer textRenderer;
    @Shadow protected MerchantScreenHandler handler;

    /* ===================== RENDER STAR ===================== */

    @Inject(method = "render", at = @At("TAIL"))
    private void cherishedTrades$renderStars(
            DrawContext ctx,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        List<TradeOffer> offers = handler.getRecipes();

        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);

            int starX = this.leftPos - 12;
            int starY = this.topPos + 16 + i * 20;

            Item item = offer.getSellItem().getItem();
            boolean favorite = CherishedTradesManager.isFavorite(item);

            String star = favorite ? "★" : "☆";
            int color = favorite ? 0xFFFFD700 : 0xFFFFFFFF;

            ctx.drawTextWithShadow(
                    textRenderer,
                    star,
                    starX,
                    starY,
                    color
            );

            if (mouseX >= starX && mouseX <= starX + 8 &&
                mouseY >= starY && mouseY <= starY + 8) {

                ctx.drawTooltip(
                        textRenderer,
                        List.of(Text.literal("Favorite trade")),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    /* ===================== CLICK ===================== */

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void cherishedTrades$clickStar(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        List<TradeOffer> offers = handler.getRecipes();

        for (int i = 0; i < offers.size(); i++) {
            int starX = this.leftPos - 12;
            int starY = this.topPos + 16 + i * 20;

            if (mouseX >= starX && mouseX <= starX + 8 &&
                mouseY >= starY && mouseY <= starY + 8) {

                Item item = offers.get(i).getSellItem().getItem();
                CherishedTradesManager.toggleFavorite(item);

                cir.setReturnValue(true);
                return;
            }
        }
    }
}
