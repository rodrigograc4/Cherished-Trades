package com.rodrigograc4.cherishedtrades.mixin;

import java.util.concurrent.atomic.AtomicBoolean;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;

/** Cloth toggles have no setter; the "Turn all off" button flips their value directly. */
@Mixin(value = BooleanListEntry.class, remap = false)
public interface BooleanListEntryAccessor {

    @Accessor("bool")
    AtomicBoolean cherishedTrades$value();
}
