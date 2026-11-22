/*
 * Copyright (c) 2019-2026 Team Galacticraft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package dev.galacticraft.mod.screen;

import com.mojang.logging.LogUtils;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.display.ItemSlotDisplay;
import dev.galacticraft.machinelib.impl.compat.vanilla.StorageSlot;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout.SlotRequirement;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Dynamic crafting grid {@link StorageSlot} whose active state, placement filter, and screen
 * coordinates adapt live to the {@link SchematicGridLayout} of the schematic in Slot 1 without
 * ever adding or removing {@link Slot} instances from the menu's fixed slot index table.
 */
public class DynamicSchematicSlot extends StorageSlot {
    public static final int HIDDEN_COORD = -2000;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final @Nullable Field SLOT_X_FIELD = resolveSlotField("field_7873", "x");
    private static final @Nullable Field SLOT_Y_FIELD = resolveSlotField("field_7872", "y");

    private final int gridIndex;
    private final Supplier<SchematicGridLayout> layoutSupplier;

    public DynamicSchematicSlot(
            @NotNull Container container,
            @NotNull ItemResourceSlot wrapped,
            @NotNull ItemSlotDisplay display,
            int storageIndex,
            @NotNull Player player,
            int gridIndex,
            @NotNull Supplier<SchematicGridLayout> layoutSupplier
    ) {
        super(container, wrapped, display, storageIndex, player);
        this.gridIndex = gridIndex;
        this.layoutSupplier = Objects.requireNonNull(layoutSupplier, "layoutSupplier");
    }

    public int getGridIndex() {
        return this.gridIndex;
    }

    public @NotNull SchematicGridLayout getActiveLayout() {
        SchematicGridLayout layout = this.layoutSupplier.get();
        return layout != null ? layout : SchematicGridLayout.DEFAULT_3X3;
    }

    public @Nullable SlotRequirement getRequirement() {
        return this.getActiveLayout().requirement(this.gridIndex);
    }

    @Override
    public boolean isActive() {
        return this.getActiveLayout().isSlotActive(this.gridIndex);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        SchematicGridLayout layout = this.getActiveLayout();
        if (!layout.isSlotActive(this.gridIndex)) {
            return false;
        }
        if (!super.mayPlace(stack)) {
            return false;
        }
        if (layout.isModular() && !stack.isEmpty()) {
            SlotRequirement req = layout.requirement(this.gridIndex);
            if (req != null && !req.ingredient().test(stack)) {
                return false;
            }
        }
        return true;
    }

    public int getTargetX() {
        SchematicGridLayout layout = this.getActiveLayout();
        return layout.isSlotActive(this.gridIndex) ? layout.slotX(this.gridIndex) : HIDDEN_COORD;
    }

    public int getTargetY() {
        SchematicGridLayout layout = this.getActiveLayout();
        return layout.isSlotActive(this.gridIndex) ? layout.slotY(this.gridIndex) : HIDDEN_COORD;
    }

    /**
     * Synchronizes this slot's inherited {@link Slot#x} and {@link Slot#y} coordinates with the
     * active {@link SchematicGridLayout}, moving inactive slots off-screen to {@link #HIDDEN_COORD}.
     */
    public void updateLayoutPosition() {
        int targetX = this.getTargetX();
        int targetY = this.getTargetY();
        if (this.x != targetX || this.y != targetY) {
            setSlotCoordinates(this, targetX, targetY);
        }
    }

    private static void setSlotCoordinates(Slot slot, int newX, int newY) {
        try {
            if (SLOT_X_FIELD != null) {
                SLOT_X_FIELD.setInt(slot, newX);
            }
            if (SLOT_Y_FIELD != null) {
                SLOT_Y_FIELD.setInt(slot, newY);
            }
        } catch (IllegalAccessException e) {
            LOGGER.warn("Failed to update DynamicSchematicSlot coordinates to ({}, {})", newX, newY, e);
        }
    }

    private static @Nullable Field resolveSlotField(String intermediaryName, String namedFallback) {
        String mapped = namedFallback;
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (loader != null && loader.getMappingResolver() != null) {
                mapped = loader.getMappingResolver()
                        .mapFieldName("intermediary", "net.minecraft.class_1735", intermediaryName, "I");
            }
        } catch (Throwable ignored) {
            // Standalone unit tests may run without FabricLoader initialized.
        }
        for (String candidate : new String[]{mapped, namedFallback, intermediaryName}) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                Field field = Slot.class.getDeclaredField(candidate);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | SecurityException ignored) {
            }
        }
        LOGGER.warn("Could not locate Slot coordinate field {} / {}", intermediaryName, namedFallback);
        return null;
    }
}
