package com.rodrigograc4.cherishedtrades.gametest;

import java.util.ArrayList;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfig;
import com.rodrigograc4.cherishedtrades.config.CherishedTradesConfigScreen;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

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
            config.priceOffset = 3;
            config.highlightedEnchantments = new ArrayList<>(new CherishedTradesConfig().highlightedEnchantments);
        });

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            // Lets the "Saving world" notice from world creation fade out.
            context.waitTicks(60);

            context.setScreen(() -> CherishedTradesConfigScreen.create(null));
            moveTo(context, 0, 0);
            context.waitTicks(5);
            context.takeScreenshot("preview-1-general");

            click(context, 120, 52);
            context.waitTicks(5);
            context.takeScreenshot("preview-2-books");

            // Scroll down to the books switched on above.
            scrollAndShoot(context, 104, "preview-3-books-mending");
            scrollAndShoot(context, 200, "preview-4-books-unbreaking");

            context.setScreen(() -> null);
        }
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
