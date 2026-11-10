package com.rodrigograc4.cherishedtrades;

import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

public class PriceChecker {

    /**
     * The emerald price of an enchanted book trade.
     *
     * @param rolled     the price this villager rolled, before any discount or demand. It never
     *                   changes for this trade.
     * @param discounted the price with the player's lasting reputation discount (curing the
     *                   villager from a zombie, trading with it), leaving out Hero of the Village
     *                   and demand, which wear off
     * @param min        the cheapest price any villager can roll for this book
     * @param max        the most expensive one
     */
    public record BookPrice(int rolled, int discounted, int min, int max) {
    }

    public static boolean isGreatDeal(MerchantOffer offer) {
        if (!CherishedTradesConfig.INSTANCE.enablePriceChecker) return false;
        return bookPrice(offer).map(PriceChecker::isGreatDeal).orElse(false);
    }

    public static boolean isGreatDeal(BookPrice price) {
        return price.rolled() <= price.min() + CherishedTradesConfig.INSTANCE.priceOffset;
    }

    public static Optional<BookPrice> bookPrice(MerchantOffer offer) {
        ItemStack sellItem = offer.getResult();
        if (!sellItem.is(Items.ENCHANTED_BOOK)) return Optional.empty();

        ItemStack baseCost = offer.getBaseCostA();
        if (!baseCost.is(Items.EMERALD)) return Optional.empty();

        var enchantments = sellItem.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return Optional.empty();

        var entry = enchantments.entrySet().iterator().next();
        int level = Math.max(entry.getIntValue(), 1);
        boolean doublePrice = entry.getKey().is(EnchantmentTags.DOUBLE_TRADE_PRICE);

        return Optional.of(new BookPrice(
                baseCost.getCount(),
                reputationPrice(offer),
                getMinPrice(level, doublePrice),
                getMaxPrice(level, doublePrice)));
    }

    // The villager adds two discounts to specialPriceDiff when trading starts (Villager.updateSpecialPrices):
    // -floor(reputation * priceMultiplier), and with Hero of the Village
    // -max(floor((0.3 + 0.0625 * amplifier) * baseCount), 1). Taking the hero part back out leaves
    // the reputation part, which lasts.
    private static int reputationPrice(MerchantOffer offer) {
        int base = offer.getBaseCostA().getCount();
        int reputationDiff = offer.getSpecialPriceDiff() + heroDiscount(base);
        return Mth.clamp(base + reputationDiff, 1, offer.getBaseCostA().getMaxStackSize());
    }

    private static int heroDiscount(int base) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0;
        MobEffectInstance hero = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        if (hero == null) return 0;
        float scale = 0.3F + 0.0625F * hero.getAmplifier();
        return Math.max((int) Math.floor(scale * base), 1);
    }

    // Librarian books cost 2 + random(5 + 10 * level) + 3 * level emeralds, doubled for
    // #minecraft:double_trade_price (treasure) enchantments and capped at a stack of 64.
    // random(n) is 0..n-1, so the cheapest roll is 2 + 3 * level and the dearest 6 + 13 * level.
    public static int getMinPrice(int level, boolean doublePrice) {
        int price = 2 + 3 * level;
        return Math.min(doublePrice ? price * 2 : price, 64);
    }

    private static int getMaxPrice(int level, boolean doublePrice) {
        int price = 6 + 13 * level;
        return Math.min(doublePrice ? price * 2 : price, 64);
    }
}
