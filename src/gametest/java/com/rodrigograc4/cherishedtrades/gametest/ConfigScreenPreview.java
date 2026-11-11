package com.rodrigograc4.cherishedtrades.gametest;

import java.util.ArrayList;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.rodrigograc4.cherishedtrades.CherishedTradesManager;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfigScreen;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Not a check: takes screenshots of the config screen at 2x resolution so it can be reviewed.
 * They land in build/run/clientGameTest/screenshots as preview-*.png.
 */
public class ConfigScreenPreview implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1708, 960);
        context.runOnClient(client -> {
            CherishedTradesConfig config = CherishedTradesConfig.INSTANCE;
            config.dealPercent = new CherishedTradesConfig().dealPercent;
            config.highlightedEnchantments = new ArrayList<>(new CherishedTradesConfig().highlightedEnchantments);
        });

        // From the title screen there is no world to clear bookmarks from.
        context.setScreen(() -> CherishedTradesConfigScreen.create(null));
        moveTo(context, 0, 0);
        context.waitTicks(5);
        scrollAndShoot(context, 5, "preview-0-general-title-screen");
        context.setScreen(() -> null);

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            // Lets the "Saving world" notice from world creation fade out.
            context.waitTicks(60);

            context.runOnClient(client -> {
                MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 5), new ItemStack(Items.BOOKSHELF), 12, 1, 0.05F);
                UUID villager = UUID.randomUUID();
                for (int i = 0; i < 5; i++) {
                    CherishedTradesManager.toggleFavorite(villager, i, offer);
                }
            });

            readmeScreenshots(context);

            context.setScreen(() -> CherishedTradesConfigScreen.create(null));
            moveTo(context, 0, 0);
            context.waitTicks(5);
            context.takeScreenshot("preview-1-general");
            scrollAndShoot(context, 5, "preview-1b-general-clear");
            click(context, 314, 194);
            context.waitTicks(5);
            context.takeScreenshot("preview-1c-clear-confirm");
            context.setScreen(() -> CherishedTradesConfigScreen.create(null));
            context.waitTicks(5);

            click(context, 120, 52);
            context.waitTicks(5);
            context.takeScreenshot("preview-2-books");

            click(context, 215, 89);
            context.getInput().typeChars("sharp");
            moveTo(context, 0, 0);
            context.waitTicks(5);
            context.takeScreenshot("preview-2b-books-search");
            context.setScreen(() -> CherishedTradesConfigScreen.create(null));
            context.waitTicks(5);
            click(context, 120, 52);
            context.waitTicks(5);

            // Scroll down to the books switched on above.
            scrollAndShoot(context, 104, "preview-3-books-mending");
            scrollAndShoot(context, 200, "preview-4-books-unbreaking");

            // "Turn All Off" switches every book off, and it sticks once saved.
            context.setScreen(() -> CherishedTradesConfigScreen.create(null));
            context.waitTicks(5);
            click(context, 120, 52);
            context.waitTicks(5);
            click(context, 314, 156);
            context.waitTicks(2);
            scrollAndShoot(context, 104, "preview-5-books-all-off");
            click(context, 276, 223);
            context.waitTicks(5);
            context.runOnClient(client -> {
                var saved = CherishedTradesConfig.INSTANCE.highlightedEnchantments;
                if (!saved.isEmpty()) throw new AssertionError("Turn All Off then Save should leave no books, left " + saved);
                CherishedTradesConfig defaults = new CherishedTradesConfig();
                CherishedTradesConfig.INSTANCE.highlightedEnchantments = new ArrayList<>(defaults.highlightedEnchantments);
                CherishedTradesConfig.INSTANCE.enablePriceChecker = defaults.enablePriceChecker;
                CherishedTradesConfig.save();
            });

            context.setScreen(() -> null);
            context.runOnClient(client -> CherishedTradesManager.clearCurrentWorld());
        }
    }

    // README: a slightly taller window so every General option fits without scrolling. The GUI
    // scale stays the same, so the clicks below line up as in the previews.
    private static void readmeScreenshots(ClientGameTestContext context) {
        context.getInput().resizeWindow(1708, 1080);
        context.setScreen(() -> CherishedTradesConfigScreen.create(null));
        moveTo(context, 0, 0);
        context.waitTicks(5);
        context.takeScreenshot("readme-settings");

        click(context, 120, 52);
        context.waitTicks(5);
        click(context, 215, 89);
        context.getInput().typeChars("unbreaking");
        moveTo(context, 0, 0);
        context.waitTicks(5);
        context.takeScreenshot("readme-settings-books");

        context.setScreen(() -> CherishedTradesConfigScreen.create(null));
        context.waitTicks(5);
        click(context, 314, 209);
        moveTo(context, 0, 0);
        context.waitTicks(5);
        context.takeScreenshot("readme-clear-bookmarks");

        context.setScreen(() -> null);
        context.getInput().resizeWindow(1708, 960);
        context.waitTicks(5);
    }

    private static void scrollAndShoot(ClientGameTestContext context, int notches, String name) {
        moveTo(context, 213, 200);
        for (int i = 0; i < notches; i++) {
            context.getInput().scroll(-1);
        }
        moveTo(context, 0, 0);
        context.waitTicks(10);
        context.takeScreenshot(name);
    }

    /** Clicks at a position given in GUI-scaled coordinates. */
    private static void click(ClientGameTestContext context, int guiX, int guiY) {
        moveTo(context, guiX, guiY);
        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
    }

    private static void moveTo(ClientGameTestContext context, int guiX, int guiY) {
        double scale = context.computeOnClient(client -> {
            Window window = client.getWindow();
            return (double) window.getScreenWidth() / window.getGuiScaledWidth();
        });
        context.getInput().setCursorPos(guiX * scale, guiY * scale);
    }
}
