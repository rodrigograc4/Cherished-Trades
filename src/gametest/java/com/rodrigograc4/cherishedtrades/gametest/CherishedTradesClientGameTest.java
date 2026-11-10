package com.rodrigograc4.cherishedtrades.gametest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.rodrigograc4.cherishedtrades.CherishedMerchantMenu;
import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.PriceChecker;
import com.rodrigograc4.cherishedtrades.VillagerTradeLabels;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfigScreen;
import com.rodrigograc4.cherishedtrades.util.WorldKey;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class CherishedTradesClientGameTest implements FabricClientGameTest {

    private static final int PAPER = 0;
    private static final int MENDING_CHEAP = 1;
    private static final int SHARPNESS_EXPENSIVE = 2;
    private static final int SHARPNESS_CHEAP = 3;
    private static final int BOOKSHELF = 4;
    private static final int BOOKSHELF_DUPLICATE = 5;

    private static final int GUI_WIDTH = 276;
    private static final int GUI_HEIGHT = 166;

    // Checks per-world isolation, multiplayer storage and the config screen.
    // Screenshots end up in the run directory's screenshots folder.
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            CherishedTradesConfig.INSTANCE.highlightedEnchantments.clear();
            CherishedTradesConfig.INSTANCE.highlightedEnchantments.add("Mending 1");
        });

        String singleplayerKey;
        UUID villagerId;
        MerchantOffer sharpnessOffer;

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            singleplayer.getConnection().waitForChunksRender();
            villagerId = setUpVillager(context, singleplayer.getServer());
            sharpnessOffer = singleplayer.getServer().computeOnServer(server -> findVillager(server, villagerId).getOffers().get(SHARPNESS_CHEAP));
            singleplayerKey = context.computeOnClient(client -> WorldKey.current());
            check(singleplayerKey.startsWith("sp:"), "singleplayer world key should start with sp:, was " + singleplayerKey);

            testSingleplayerTrades(context, singleplayer.getServer(), villagerId);
        }

        try (TestSingleplayerContext otherWorld = context.worldBuilder().create()) {
            otherWorld.getConnection().waitForChunksRender();
            context.runOnClient(client -> {
                String key = WorldKey.current();
                check(key.startsWith("sp:") && !key.equals(singleplayerKey), "second world should get its own key, was " + key);
                check(!CherishedTradesManager.isFavorite(villagerId, SHARPNESS_CHEAP, sharpnessOffer), "bookmarks must not leak into another world");
            });
        }

        try (TestDedicatedServerContext server = context.worldBuilder().createServer();
             TestDedicatedServerConnection connection = server.connect()) {
            connection.waitForChunksRender();
            UUID mpVillagerId = setUpVillager(context, server);

            context.runOnClient(client -> {
                String key = WorldKey.current();
                check(key.startsWith("mp:"), "multiplayer world key should start with mp:, was " + key);
                check(!CherishedTradesManager.isFavorite(villagerId, SHARPNESS_CHEAP, sharpnessOffer), "singleplayer bookmarks must not show on a server");
            });

            openTrades(context);
            clickGui(context, 4, bookmarkY(SHARPNESS_CHEAP));
            context.waitTicks(2);
            context.takeScreenshot("cherishedtrades-7-multiplayer");

            context.runOnClient(client -> {
                MerchantMenu menu = menu(client);
                CherishedMerchantMenu ext = (CherishedMerchantMenu) menu;
                check(ext.cherishedTrades$getRealIndex(0) == SHARPNESS_CHEAP, "bookmarked trade should be first on the server too");
                check(CherishedTradesManager.isFavorite(mpVillagerId, SHARPNESS_CHEAP, menu.getOffers().get(0)), "bookmark stored for the server's villager");
            });

            clickGui(context, 5 + 50, rowCenterY(0));
            connection.waitForServerboundPackets();
            context.waitTicks(5);
            server.runOnServer(s -> checkSharpnessInResultSlot(s));

            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitTicks(5);
        }

        context.setScreen(() -> CherishedTradesConfigScreen.create(null));
        context.waitTicks(5);
        context.takeScreenshot("cherishedtrades-8-config");

        // Second tab: the enchantment toggles. Mending 1 was set in the old typed format above.
        double[] tab = context.computeOnClient(client -> {
            Window window = client.getWindow();
            double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
            return new double[] { 120 * scale, 52 * scale };
        });
        context.getInput().setCursorPos(tab[0], tab[1]);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        context.waitTicks(5);
        context.takeScreenshot("cherishedtrades-9-config-books");
        context.setScreen(() -> null);
    }

    // Opens a real trade screen and checks bookmarks, sorting, duplicate trades, the price checker,
    // server index translation, level-ups and persistence after reopening.
    private static void testSingleplayerTrades(ClientGameTestContext context, TestServerContext server, UUID villagerId) {
        openTrades(context);
        context.takeScreenshot("cherishedtrades-1-opened");

        context.runOnClient(client -> {
            MerchantMenu menu = menu(client);
            CherishedMerchantMenu ext = (CherishedMerchantMenu) menu;
            List<MerchantOffer> offers = menu.getOffers();
            for (int i = 0; i < offers.size(); i++) {
                check(ext.cherishedTrades$getRealIndex(i) == i, "nothing bookmarked yet, row " + i + " should be server index " + i);
                check(!ext.cherishedTrades$isFavorite(i), "nothing bookmarked yet");
            }

            check(PriceChecker.isGreatDeal(offers.get(MENDING_CHEAP)), "5 emerald mending should be a great deal");
            check(!PriceChecker.isGreatDeal(offers.get(SHARPNESS_EXPENSIVE)), "50 emerald sharpness IV should not be a deal");
            check(PriceChecker.isGreatDeal(offers.get(SHARPNESS_CHEAP)), "17 emerald sharpness V should be a great deal");
            check(!PriceChecker.isGreatDeal(offers.get(PAPER)), "paper trade is not a book");

            var sharpnessV = PriceChecker.bookPrice(offers.get(SHARPNESS_CHEAP)).orElseThrow();
            check(sharpnessV.min() == 17 && sharpnessV.max() == 64, "Sharpness V range should be 17-64, was " + sharpnessV);
            var mending = PriceChecker.bookPrice(offers.get(MENDING_CHEAP)).orElseThrow();
            check(mending.min() == 10 && mending.max() == 38, "Mending is doubled: 10-38, was " + mending);

            // Discounts lower the shown price but not the price the villager rolled.
            MerchantOffer discounted = offers.get(SHARPNESS_EXPENSIVE).copy();
            discounted.setSpecialPriceDiff(-50);
            check(discounted.getCostA().getCount() < 20, "discount should apply, cost " + discounted.getCostA());
            check(!PriceChecker.isGreatDeal(discounted), "a discounted 50 emerald book is still a bad roll");
        });

        // Tooltip with the price range, taken at 2x so it can be read.
        context.getInput().resizeWindow(1708, 960);
        context.waitTicks(5);
        hoverGui(context, 5 + 68 + 8, rowCenterY(SHARPNESS_CHEAP));
        context.waitTicks(5);
        context.takeScreenshot("cherishedtrades-1-tooltip");
        context.getInput().resizeWindow(854, 480);
        context.waitTicks(5);

        clickGui(context, 4, bookmarkY(BOOKSHELF_DUPLICATE));
        context.waitTicks(2);
        context.takeScreenshot("cherishedtrades-2-duplicate");

        context.runOnClient(client -> {
            MerchantMenu menu = menu(client);
            CherishedMerchantMenu ext = (CherishedMerchantMenu) menu;
            check(ext.cherishedTrades$getRealIndex(0) == BOOKSHELF_DUPLICATE, "bookmarked duplicate should be first");
            check(ext.cherishedTrades$isFavorite(0), "row 0 is bookmarked");
            check(ext.cherishedTrades$getRealIndex(5) == BOOKSHELF, "the other bookshelf trade stays in place");
            check(!ext.cherishedTrades$isFavorite(5), "identical trade must not be bookmarked too");
            check(!CherishedTradesManager.isFavorite(villagerId, BOOKSHELF, menu.getOffers().get(5)), "identical trade must not be stored as bookmarked");
        });

        clickGui(context, 4, bookmarkY(4));
        context.waitTicks(2);
        context.takeScreenshot("cherishedtrades-3-bookmarked");

        context.runOnClient(client -> {
            CherishedMerchantMenu ext = (CherishedMerchantMenu) menu(client);
            check(ext.cherishedTrades$getRealIndex(0) == SHARPNESS_CHEAP, "sharpness first");
            check(ext.cherishedTrades$getRealIndex(1) == BOOKSHELF_DUPLICATE, "bookshelf second");
            check(ext.cherishedTrades$getRealIndex(2) == PAPER, "then the rest in server order");
        });

        clickGui(context, 5 + 50, rowCenterY(0));
        context.waitTicks(5);
        context.takeScreenshot("cherishedtrades-4-selected");
        server.runOnServer(CherishedTradesClientGameTest::checkSharpnessInResultSlot);

        server.runOnServer(s -> {
            Villager villager = findVillager(s, villagerId);
            villager.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 5), new ItemStack(Items.CLOCK), 12, 15, 0.05F));
            ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
            player.sendMerchantOffers(player.containerMenu.containerId, villager.getOffers(), 2, 10, true, true);
        });
        context.waitTicks(5);

        context.runOnClient(client -> {
            MerchantMenu menu = menu(client);
            CherishedMerchantMenu ext = (CherishedMerchantMenu) menu;
            check(menu.getOffers().size() == 7, "new trade should appear after level-up, size " + menu.getOffers().size());
            check(menu.getOffers().get(6).getResult().is(Items.CLOCK), "new clock trade should be last");
            check(ext.cherishedTrades$getRealIndex(6) == 6, "clock row should map to server index 6");
            check(ext.cherishedTrades$isFavorite(0) && ext.cherishedTrades$isFavorite(1), "bookmarks survive level-up");
        });
        context.takeScreenshot("cherishedtrades-5-levelled-up");

        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(5);
        openTrades(context);
        context.runOnClient(client -> {
            CherishedMerchantMenu ext = (CherishedMerchantMenu) menu(client);
            check(ext.cherishedTrades$getRealIndex(0) == SHARPNESS_CHEAP, "sharpness first after reopening");
            check(ext.cherishedTrades$getRealIndex(1) == BOOKSHELF_DUPLICATE, "bookshelf second after reopening");
        });

        clickGui(context, 4, bookmarkY(1));
        context.waitTicks(2);
        context.runOnClient(client -> {
            CherishedMerchantMenu ext = (CherishedMerchantMenu) menu(client);
            check(ext.cherishedTrades$getRealIndex(0) == SHARPNESS_CHEAP, "sharpness still first");
            check(ext.cherishedTrades$getRealIndex(4) == BOOKSHELF, "first bookshelf at its place");
            check(ext.cherishedTrades$getRealIndex(5) == BOOKSHELF_DUPLICATE, "second bookshelf back at its place");
            check(!ext.cherishedTrades$isFavorite(5), "bookshelf no longer bookmarked");
        });
        context.takeScreenshot("cherishedtrades-6-reopened");

        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(5);

        testDiscountAndLabels(context, server, villagerId);
    }

    // Curing a villager gives a lasting discount, shown in the tooltip; Hero of the Village is
    // temporary and left out of it. Then, with the trade screen closed, the bookmarked trades are
    // drawn above the villager the player looks at, without and with a custom name.
    private static void testDiscountAndLabels(ClientGameTestContext context, TestServerContext server, UUID villagerId) {
        openTrades(context);
        clickGui(context, 4, bookmarkY(context.computeOnClient(client -> visualRow(client, MENDING_CHEAP))));
        context.waitTicks(2);
        takeFeatureScreenshots(context);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(5);

        server.runOnServer(s -> findVillager(s, villagerId).onReputationEventFrom(
                ReputationEventType.ZOMBIE_VILLAGER_CURED, s.getPlayerList().getPlayers().getFirst()));

        // README: the whole trade screen with every feature on and a book's tooltip open.
        openTrades(context);
        context.getInput().resizeWindow(1708, 960);
        context.waitTicks(5);
        hoverGui(context, 5 + 68 + 8, rowCenterY(context.computeOnClient(client -> visualRow(client, SHARPNESS_CHEAP))));
        context.waitTicks(5);
        context.takeScreenshot("readme-price-tooltip");
        context.getInput().resizeWindow(854, 480);
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(5);

        server.runCommand("effect give @a minecraft:hero_of_the_village 600 4");
        openTrades(context);

        int expensiveRow = context.computeOnClient(client -> visualRow(client, SHARPNESS_EXPENSIVE));
        int cheapRow = context.computeOnClient(client -> visualRow(client, SHARPNESS_CHEAP));
        context.runOnClient(client -> {
            MerchantOffer offer = menu(client).getOffers().get(expensiveRow);
            var price = PriceChecker.bookPrice(offer).orElseThrow();
            check(price.discounted() < 50, "curing should give a lasting discount, got " + price);
            check(price.discounted() > offer.getCostA().getCount(), "Hero of the Village must be left out of it, shown " + offer.getCostA() + " vs " + price);
            check(!PriceChecker.isGreatDeal(offer), "a discount must not turn a bad roll into a deal");
        });

        context.getInput().resizeWindow(1708, 960);
        context.waitTicks(5);
        hoverGui(context, 5 + 68 + 8, rowCenterY(cheapRow));
        context.waitTicks(5);
        context.takeScreenshot("cherishedtrades-6b-tooltip-discount-deal");
        hoverGui(context, 5 + 68 + 8, rowCenterY(expensiveRow));
        context.waitTicks(5);
        context.takeScreenshot("cherishedtrades-6c-tooltip-discount");

        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        server.runCommand("effect clear @a");
        context.waitTicks(10);
        context.takeScreenshot("cherishedtrades-6d-villager-labels");

        server.runOnServer(s -> findVillager(s, villagerId).setCustomName(Component.literal("Bob")));
        context.waitTicks(10);
        context.takeScreenshot("cherishedtrades-6e-villager-labels-named");

        // README: a few blocks back and aiming at the body, so the crosshair stays below the head.
        // The name is made always visible, since vanilla only shows it within reach.
        server.runOnServer(s -> findVillager(s, villagerId).setCustomNameVisible(true));
        server.runCommand("tp @a 0.5 -60 -2 0 8");
        context.waitTicks(10);
        context.runOnClient(client -> check(!VillagerTradeLabels.linesFor(findClientVillager(client, villagerId), 1.0F).isEmpty(),
                "bookmarked trades should show while looking at the villager's body"));
        context.takeScreenshot("readme-villager-labels");

        context.runOnClient(client -> client.gui.hud.toggle());
        context.runOnClient(client -> check(VillagerTradeLabels.linesFor(findClientVillager(client, villagerId), 1.0F).isEmpty(),
                "labels hide with the HUD (F1), like vanilla name tags"));
        context.runOnClient(client -> client.gui.hud.toggle());

        context.getInput().resizeWindow(854, 480);
        context.waitTicks(5);
    }

    // One 2x screenshot of the trade screen per feature, with the other two turned off, for the README.
    private static void takeFeatureScreenshots(ClientGameTestContext context) {
        context.getInput().resizeWindow(1708, 960);
        context.waitTicks(5);
        hoverGui(context, GUI_WIDTH + 20, 10);

        featureScreenshot(context, "readme-bookmarks", true, false, false);
        featureScreenshot(context, "readme-highlighted", false, true, false);
        featureScreenshot(context, "readme-price-checker", false, false, true);
        context.runOnClient(client -> setFeatures(true, true, true));

        context.getInput().resizeWindow(854, 480);
        context.waitTicks(5);
    }

    private static void featureScreenshot(ClientGameTestContext context, String name, boolean bookmarks, boolean highlighted, boolean prices) {
        context.runOnClient(client -> setFeatures(bookmarks, highlighted, prices));
        context.waitTicks(3);
        context.takeScreenshot(name);
    }

    private static void setFeatures(boolean bookmarks, boolean highlighted, boolean prices) {
        CherishedTradesConfig config = CherishedTradesConfig.INSTANCE;
        config.enableBookmarks = bookmarks;
        config.enableHighlightedBooks = highlighted;
        config.enablePriceChecker = prices;
    }

    private static int visualRow(Minecraft client, int serverIndex) {
        CherishedMerchantMenu ext = (CherishedMerchantMenu) menu(client);
        for (int row = 0; row < menu(client).getOffers().size(); row++) {
            if (ext.cherishedTrades$getRealIndex(row) == serverIndex) return row;
        }
        throw new AssertionError("no row for trade " + serverIndex);
    }

    /** Spawns the test librarian in front of the player and gives the player emeralds and books. */
    private static UUID setUpVillager(ClientGameTestContext context, TestServerContext server) {
        server.runCommand("time set noon");
        server.runCommand("gamerule advance_time false");
        server.runCommand("tp @a 0.5 -60 0.5 0 0");
        context.waitTicks(5);
        UUID id = server.computeOnServer(CherishedTradesClientGameTest::spawnLibrarian);
        context.waitTicks(10);
        return id;
    }

    private static UUID spawnLibrarian(MinecraftServer server) {
        ServerLevel level = server.overworld();
        Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        villager.setPos(0.5, -60, 2.5);
        villager.setYRot(180);
        villager.setNoAi(true);
        villager.setVillagerData(villager.getVillagerData().withProfession(server.registryAccess(), VillagerProfession.LIBRARIAN));

        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.PAPER, 24), new ItemStack(Items.EMERALD), 16, 2, 0.05F));
        offers.add(bookOffer(server, Enchantments.MENDING, 1, 5));
        offers.add(bookOffer(server, Enchantments.SHARPNESS, 4, 50));
        offers.add(bookOffer(server, Enchantments.SHARPNESS, 5, 17));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 9), new ItemStack(Items.BOOKSHELF), 12, 1, 0.05F));
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 9), new ItemStack(Items.BOOKSHELF), 12, 1, 0.05F));
        villager.setOffers(offers);
        level.addFreshEntity(villager);

        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        player.getInventory().add(new ItemStack(Items.EMERALD, 64));
        player.getInventory().add(new ItemStack(Items.BOOK, 16));
        return villager.getUUID();
    }

    private static MerchantOffer bookOffer(MinecraftServer server, ResourceKey<Enchantment> enchantment, int level, int price) {
        var holder = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
        ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(holder, level));
        return new MerchantOffer(new ItemCost(Items.EMERALD, price), Optional.of(new ItemCost(Items.BOOK)), book, 12, 5, 0.2F);
    }

    private static void checkSharpnessInResultSlot(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        check(player.containerMenu instanceof MerchantMenu, "server should have the merchant menu open");
        ItemStack result = player.containerMenu.getSlot(2).getItem();
        var stored = result.get(DataComponents.STORED_ENCHANTMENTS);
        var sharpness = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        check(stored != null && stored.getLevel(sharpness) == 5, "server result slot should hold Sharpness V, got " + result + " " + stored);
    }

    private static net.minecraft.world.entity.Entity findClientVillager(Minecraft client, UUID id) {
        for (var entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(id)) return entity;
        }
        throw new AssertionError("villager " + id + " not found on the client");
    }

    private static Villager findVillager(MinecraftServer server, UUID id) {
        return (Villager) server.overworld().getEntity(id);
    }

    private static void openTrades(ClientGameTestContext context) {
        context.getInput().pressKey(options -> options.keyUse);
        context.waitForScreen(MerchantScreen.class);
        context.waitTicks(5);
    }

    private static MerchantMenu menu(Minecraft client) {
        return ((MerchantScreen) client.gui.screen()).getMenu();
    }

    private static int bookmarkY(int row) {
        return 18 + row * 20 + 4;
    }

    private static int rowCenterY(int row) {
        return 18 + row * 20 + 10;
    }

    /** Clicks at a position relative to the trade GUI's top-left corner. */
    private static void clickGui(ClientGameTestContext context, int guiX, int guiY) {
        hoverGui(context, guiX, guiY);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    /** Moves the cursor to a position relative to the trade GUI's top-left corner. */
    private static void hoverGui(ClientGameTestContext context, int guiX, int guiY) {
        double[] pos = context.computeOnClient(client -> {
            Window window = client.getWindow();
            int left = (window.getGuiScaledWidth() - GUI_WIDTH) / 2;
            int top = (window.getGuiScaledHeight() - GUI_HEIGHT) / 2;
            double scale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
            return new double[] { (left + guiX + 0.5) * scale, (top + guiY + 0.5) * scale };
        });
        context.getInput().setCursorPos(pos[0], pos[1]);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
