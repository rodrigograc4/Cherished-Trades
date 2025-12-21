package com.rodrigograc4.cherishedtrades.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

    @Accessor("x")
    int cherishedTrades$getX();

    @Accessor("y")
    int cherishedTrades$getY();
}
