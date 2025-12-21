package com.rodrigograc4.cherishedtrades.mixin;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin {

    /* ---------------- DESENHAR ESTRELAS ---------------- */

    @Inject(
        method = "drawForeground",
        at = @At("TAIL")
    )
    private void cherishedTrades$drawStars(
            DrawContext context,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        MerchantScreen screen = (MerchantScreen) (Object) this;
        MerchantScreenHandler handler = screen.getScreenHandler();
        List<TradeOffer> offers = handler.getRecipes();

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int baseX = accessor.cherishedTrades$getX();
        int baseY = accessor.cherishedTrades$getY();

        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);

            int starX = 0;
            int starY = baseY - 14 + i * 20 ;

            Item item = offer.getSellItem().getItem();
            boolean favorite = CherishedTradesManager.isFavorite(item);

            String star = favorite ? "★" : "☆";
            int color = favorite ? 0xFFFFD700 : 0xFFFFFFFF;

            context.drawTextWithShadow(
                    screen.getTextRenderer(),
                    star,
                    starX,
                    starY,
                    color
            );

            if (mouseX >= starX && mouseX <= starX + 8 &&
                mouseY >= starY && mouseY <= starY + 8) {

                context.drawTooltip(
                        screen.getTextRenderer(),
                        List.of(Text.literal("Favorite trade")),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    /* ---------------- CLIQUE NAS ESTRELAS ---------------- */

    @Inject(
        method = "mouseClicked",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cherishedTrades$clickStar(
            Click click,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MerchantScreen screen = (MerchantScreen) (Object) this;
        MerchantScreenHandler handler = screen.getScreenHandler();
        List<TradeOffer> offers = handler.getRecipes();

        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int baseX = accessor.cherishedTrades$getX();
        int baseY = accessor.cherishedTrades$getY();

        double mouseX = click.x();
        double mouseY = click.y();

        for (int i = 0; i < offers.size(); i++) {
            int starX = 0;
            int starY = baseY - 14 + i * 20 ;

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
