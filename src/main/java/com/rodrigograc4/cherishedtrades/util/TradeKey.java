package com.rodrigograc4.cherishedtrades.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

public final class TradeKey {

    private TradeKey() {}

    // The server index is unique even for identical trades, since level-ups only append to the list.
    // The fingerprint stops a villager with regenerated trades from inheriting old bookmarks.
    public static String of(int serverIndex, MerchantOffer offer) {
        return serverIndex + "|" + fingerprint(offer);
    }

    // Fallback id for a trade screen not opened by right-clicking a villager (e.g. a plugin shop),
    // derived from its whole trade list.
    public static UUID fallbackVillagerId(List<MerchantOffer> offers) {
        StringBuilder sb = new StringBuilder();
        for (MerchantOffer offer : offers) {
            sb.append(fingerprint(offer)).append(';');
        }
        return UUID.nameUUIDFromBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // Base prices only: the displayed price changes with demand, discounts and Hero of the Village.
    private static String fingerprint(MerchantOffer offer) {
        StringBuilder sb = new StringBuilder();
        appendStack(sb, offer.getBaseCostA());
        ItemStack costB = offer.getCostB();
        if (!costB.isEmpty()) {
            sb.append('+');
            appendStack(sb, costB);
        }
        sb.append('>');
        appendStack(sb, offer.getResult());
        return sb.toString();
    }

    private static void appendStack(StringBuilder sb, ItemStack stack) {
        sb.append(stack.getItem()).append('*').append(stack.getCount());

        var enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            enchantments = stack.get(DataComponents.ENCHANTMENTS);
        }
        if (enchantments != null && !enchantments.isEmpty()) {
            enchantments.entrySet().stream()
                    .map(entry -> entry.getKey().getRegisteredName() + "@" + entry.getIntValue())
                    .sorted()
                    .forEach(enchant -> sb.append('[').append(enchant).append(']'));
        }
    }
}
