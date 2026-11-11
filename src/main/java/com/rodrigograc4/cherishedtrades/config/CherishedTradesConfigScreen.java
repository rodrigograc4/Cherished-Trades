package com.rodrigograc4.cherishedtrades.config;

import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.mixin.BooleanListEntryAccessor;
import com.rodrigograc4.cherishedtrades.util.EnchantmentHelper;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Builds the config screen by hand instead of through AutoConfig, so highlighted enchantments
 * can be picked with toggles rather than typed into Cloth's string list.
 */
@Environment(EnvType.CLIENT)
public final class CherishedTradesConfigScreen {

    private static final String KEY = "text.autoconfig.cherished-trades.";
    private static final Component BOOKS_CATEGORY = Component.translatable(KEY + "category.highlightedEnchantments");

    /** Every enchantment in vanilla's #minecraft:tradeable tag, with its max level. */
    private record TradeableEnchantment(String id, int maxLevel) {
        String path() {
            return id.substring(id.indexOf(':') + 1);
        }

        Component name() {
            return Component.translatable("enchantment.minecraft." + path());
        }

        Component nameWithLevel(int level) {
            if (maxLevel == 1) return name();
            return Component.empty().append(name()).append(" ").append(Component.translatable("enchantment.level." + level));
        }
    }

    private static final List<TradeableEnchantment> ENCHANTMENTS = List.of(
            new TradeableEnchantment("minecraft:aqua_affinity", 1),
            new TradeableEnchantment("minecraft:bane_of_arthropods", 5),
            new TradeableEnchantment("minecraft:binding_curse", 1),
            new TradeableEnchantment("minecraft:blast_protection", 4),
            new TradeableEnchantment("minecraft:breach", 4),
            new TradeableEnchantment("minecraft:channeling", 1),
            new TradeableEnchantment("minecraft:density", 5),
            new TradeableEnchantment("minecraft:depth_strider", 3),
            new TradeableEnchantment("minecraft:efficiency", 5),
            new TradeableEnchantment("minecraft:feather_falling", 4),
            new TradeableEnchantment("minecraft:fire_aspect", 2),
            new TradeableEnchantment("minecraft:fire_protection", 4),
            new TradeableEnchantment("minecraft:flame", 1),
            new TradeableEnchantment("minecraft:fortune", 3),
            new TradeableEnchantment("minecraft:frost_walker", 2),
            new TradeableEnchantment("minecraft:impaling", 5),
            new TradeableEnchantment("minecraft:infinity", 1),
            new TradeableEnchantment("minecraft:knockback", 2),
            new TradeableEnchantment("minecraft:looting", 3),
            new TradeableEnchantment("minecraft:loyalty", 3),
            new TradeableEnchantment("minecraft:luck_of_the_sea", 3),
            new TradeableEnchantment("minecraft:lunge", 3),
            new TradeableEnchantment("minecraft:lure", 3),
            new TradeableEnchantment("minecraft:mending", 1),
            new TradeableEnchantment("minecraft:multishot", 1),
            new TradeableEnchantment("minecraft:piercing", 4),
            new TradeableEnchantment("minecraft:power", 5),
            new TradeableEnchantment("minecraft:projectile_protection", 4),
            new TradeableEnchantment("minecraft:protection", 4),
            new TradeableEnchantment("minecraft:punch", 2),
            new TradeableEnchantment("minecraft:quick_charge", 3),
            new TradeableEnchantment("minecraft:respiration", 3),
            new TradeableEnchantment("minecraft:riptide", 3),
            new TradeableEnchantment("minecraft:sharpness", 5),
            new TradeableEnchantment("minecraft:silk_touch", 1),
            new TradeableEnchantment("minecraft:smite", 5),
            new TradeableEnchantment("minecraft:sweeping_edge", 3),
            new TradeableEnchantment("minecraft:thorns", 3),
            new TradeableEnchantment("minecraft:unbreaking", 3),
            new TradeableEnchantment("minecraft:vanishing_curse", 1)
    );

    private CherishedTradesConfigScreen() {
    }

    public static Screen create(Screen parent) {
        CherishedTradesConfig config = CherishedTradesConfig.INSTANCE;
        CherishedTradesConfig defaults = new CherishedTradesConfig();

        // Normalized to "minecraft:sharpness 5". Entries the toggles don't cover (modded
        // enchantments typed into the file) are kept as they are.
        Set<String> highlighted = new LinkedHashSet<>();
        for (String entry : config.highlightedEnchantments) {
            highlighted.add(EnchantmentHelper.convertToIdFormat(entry));
        }

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(KEY + "title"))
                .setSavingRunnable(() -> {
                    config.highlightedEnchantments = new ArrayList<>(highlighted);
                    CherishedTradesConfig.save();
                });
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable(KEY + "category.general"));
        general.addEntry(entries.startBooleanToggle(option("enableBookmarks"), config.enableBookmarks)
                .setDefaultValue(defaults.enableBookmarks)
                .setTooltip(tooltip("enableBookmarks"))
                .setSaveConsumer(value -> config.enableBookmarks = value)
                .build());
        general.addEntry(entries.startBooleanToggle(option("enableBookmarkLabels"), config.enableBookmarkLabels)
                .setDefaultValue(defaults.enableBookmarkLabels)
                .setTooltip(tooltip("enableBookmarkLabels"))
                .setSaveConsumer(value -> config.enableBookmarkLabels = value)
                .build());
        general.addEntry(entries.startBooleanToggle(option("enableHighlightedBooks"), config.enableHighlightedBooks)
                .setDefaultValue(defaults.enableHighlightedBooks)
                .setTooltip(tooltip("enableHighlightedBooks"))
                .setSaveConsumer(value -> config.enableHighlightedBooks = value)
                .build());
        general.addEntry(entries.startBooleanToggle(option("enablePriceChecker"), config.enablePriceChecker)
                .setDefaultValue(defaults.enablePriceChecker)
                .setTooltip(tooltip("enablePriceChecker"))
                .setSaveConsumer(value -> config.enablePriceChecker = value)
                .build());
        general.addEntry(entries.startIntSlider(option("dealPercent"), config.dealPercent, 0, 50)
                .setDefaultValue(defaults.dealPercent)
                .setTooltip(tooltip("dealPercent"))
                .setTextGetter(value -> value == 0
                        ? Component.translatable(KEY + "option.dealPercent.lowest")
                        : Component.literal(value + "%"))
                .setSaveConsumer(value -> config.dealPercent = value)
                .build());
        general.addEntry(clearBookmarksButton());

        ConfigCategory books = builder.getOrCreateCategory(BOOKS_CATEGORY);
        books.addEntry(entries.startTextDescription(Component.translatable(KEY + "highlightedEnchantments.description")).build());

        Set<String> defaultHighlighted = new LinkedHashSet<>(defaults.highlightedEnchantments);
        List<TradeableEnchantment> sorted = new ArrayList<>(ENCHANTMENTS);
        sorted.sort(Comparator.comparing(e -> e.name().getString()));

        List<BooleanListEntry> toggles = new ArrayList<>();
        for (TradeableEnchantment enchantment : sorted) {
            for (int level = 1; level <= enchantment.maxLevel(); level++) {
                toggles.add(levelToggle(entries, highlighted, defaultHighlighted, enchantment, level));
            }
        }

        books.addEntry(turnAllOffButton(toggles));
        toggles.forEach(books::addEntry);

        return builder.build();
    }

    /** True for the Highlighted Books tab, the only one that keeps Cloth's search field. */
    public static boolean isBooksCategory(Component category) {
        return category.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(KEY + "category.highlightedEnchantments");
    }

    // Like the toggles themselves, this only takes effect when the screen is saved.
    private static ButtonListEntry turnAllOffButton(List<BooleanListEntry> toggles) {
        return new ButtonListEntry(
                option("turnAllOff"),
                () -> Component.translatable(KEY + "option.turnAllOff.button"),
                () -> toggles.stream().anyMatch(BooleanListEntry::getValue),
                () -> Optional.of(new Component[]{tooltip("turnAllOff")}),
                () -> toggles.forEach(toggle -> ((BooleanListEntryAccessor) toggle).cherishedTrades$value().set(false)));
    }

    // Bookmarks are kept per world or server, so this needs one open. From the title screen the
    // button stays greyed out and its tooltip says why.
    private static ButtonListEntry clearBookmarksButton() {
        return new ButtonListEntry(
                option("clearBookmarks"),
                () -> {
                    if (!inWorld()) return Component.translatable(KEY + "option.clearBookmarks.button");
                    int count = CherishedTradesManager.countFavoritesInCurrentWorld();
                    if (count == 0) return Component.translatable(KEY + "option.clearBookmarks.none");
                    return Component.translatable(KEY + "option.clearBookmarks.count" + (count == 1 ? ".one" : ""), count);
                },
                () -> inWorld() && CherishedTradesManager.countFavoritesInCurrentWorld() > 0,
                () -> Optional.of(new Component[]{tooltip(inWorld() ? "clearBookmarks" : "clearBookmarks.noWorld")}),
                CherishedTradesConfigScreen::confirmClearBookmarks);
    }

    // Clearing can't be undone, so it asks first and happens right away instead of on save.
    private static void confirmClearBookmarks() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen configScreen = minecraft.gui.screen();
        int count = CherishedTradesManager.countFavoritesInCurrentWorld();

        minecraft.gui.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) CherishedTradesManager.clearCurrentWorld();
                    minecraft.gui.setScreen(configScreen);
                },
                Component.translatable(KEY + "clearBookmarks.confirm.title" + (count == 1 ? ".one" : ""), count),
                Component.translatable(KEY + "clearBookmarks.confirm.message"),
                Component.translatable(KEY + "clearBookmarks.confirm.yes"),
                CommonComponents.GUI_CANCEL));
    }

    private static boolean inWorld() {
        return Minecraft.getInstance().level != null;
    }

    /** True for the screen built here; used by the Cloth mixin to strip search and tab arrows. */
    public static boolean isOurs(Screen screen) {
        return screen.getTitle().getContents() instanceof TranslatableContents contents
                && contents.getKey().equals(KEY + "title");
    }

    private static BooleanListEntry levelToggle(ConfigEntryBuilder entries, Set<String> highlighted, Set<String> defaults,
                                                TradeableEnchantment enchantment, int level) {
        String id = enchantment.id() + " " + level;
        return entries.startBooleanToggle(enchantment.nameWithLevel(level), highlighted.contains(id))
                .setDefaultValue(defaults.contains(id))
                .setSaveConsumer(value -> {
                    if (value) highlighted.add(id);
                    else highlighted.remove(id);
                })
                .build();
    }

    private static Component option(String name) {
        return Component.translatable(KEY + "option." + name);
    }

    private static Component tooltip(String name) {
        return Component.translatable(KEY + "option." + name + ".@Tooltip");
    }
}
