package com.rodrigograc4.cherishedtrades.config;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A row with a name on the left and a button on the right, laid out like Cloth's toggles so it
 * lines up with them. Cloth has no entry for an action, only for values, so this one holds none.
 */
@Environment(EnvType.CLIENT)
public class ButtonListEntry extends TooltipListEntry<Object> {

    // Cloth's toggles are 150 px wide including their reset button.
    private static final int BUTTON_WIDTH = 150;

    private final Button button;
    private final Supplier<Component> label;
    private final BooleanSupplier active;

    /**
     * @param label   the button's text, asked for every frame so it can show a count
     * @param active  whether the button can be pressed right now, also asked every frame
     * @param tooltip shown while hovering the row
     */
    // Both of TooltipListEntry's constructors are deprecated with no replacement; Cloth's own
    // entries use them too.
    @SuppressWarnings("deprecation")
    public ButtonListEntry(Component fieldName, Supplier<Component> label, BooleanSupplier active,
                           Supplier<Optional<Component[]>> tooltip, Runnable onPress) {
        super(fieldName, tooltip, false);
        this.label = label;
        this.active = active;
        this.button = Button.builder(label.get(), b -> onPress.run()).bounds(0, 0, BUTTON_WIDTH, 20).build();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight,
                                   int mouseX, int mouseY, boolean isHovered, float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        var font = Minecraft.getInstance().font;
        graphics.text(font, getDisplayedFieldName(), x, y + 6, getPreferredTextColor());

        button.active = isEditable() && active.getAsBoolean();
        button.setMessage(label.get());
        button.setX(x + entryWidth - BUTTON_WIDTH);
        button.setY(y);
        button.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    // No tags means the search field never hides this row.
    @Override
    public Iterator<String> getSearchTags() {
        return Collections.emptyIterator();
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of(button);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return List.of(button);
    }
}
