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

package dev.galacticraft.mod.content.rocket.part;

import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.machinelib.api.storage.SlottedStorageAccess;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.util.ItemStackUtil;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.PartCategorySchematicItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Defines Tinkers'-style dynamic crafting grid silhouettes, GUI slot coordinates, and per-slot
 * material requirements for each {@link RocketPartCategory} inside the Rocket Part Builder.
 *
 * <p>Grid indices {@code 0..8} map directly to {@code RocketWorkbenchBlockEntity} storage slots
 * {@code 2..10}. All active GUI coordinates stay strictly within the crafting region
 * {@code x in [30..84], y in [17..71]} with at least 18 px slot spacing so active slots never overlap.
 */
public final class SchematicGridLayout {
    public static final int GRID_SIZE = 9;
    public static final int GRID_ORIGIN_X = 30;
    public static final int GRID_ORIGIN_Y = 17;
    public static final int SLOT_STEP = 18;
    public static final int MAX_SLOT_STACK_SIZE = 64;

    public static final SchematicGridLayout DEFAULT_3X3 = createDefault3x3();

    private static volatile @Nullable Map<RocketPartCategory, SchematicGridLayout> categoryLayouts;

    private final @Nullable RocketPartCategory category;
    private final boolean[] activeMask;
    private final int[] slotX;
    private final int[] slotY;
    private final @Nullable SlotRequirement[] requirements;
    private final int activeSlotCount;

    private SchematicGridLayout(
            @Nullable RocketPartCategory category,
            boolean[] activeMask,
            int[] slotX,
            int[] slotY,
            @Nullable SlotRequirement[] requirements
    ) {
        if (activeMask.length != GRID_SIZE || slotX.length != GRID_SIZE
                || slotY.length != GRID_SIZE || requirements.length != GRID_SIZE) {
            throw new IllegalArgumentException("SchematicGridLayout arrays must have length " + GRID_SIZE);
        }
        this.category = category;
        this.activeMask = Arrays.copyOf(activeMask, GRID_SIZE);
        this.slotX = Arrays.copyOf(slotX, GRID_SIZE);
        this.slotY = Arrays.copyOf(slotY, GRID_SIZE);
        this.requirements = Arrays.copyOf(requirements, GRID_SIZE);
        int count = 0;
        for (boolean active : this.activeMask) {
            if (active) {
                count++;
            }
        }
        this.activeSlotCount = count;
    }

    /**
     * Per-slot crafting ingredient requirement and UI blueprint hint for an active grid slot.
     */
    public record SlotRequirement(
            @NotNull Ingredient ingredient,
            int baseCount,
            @NotNull String hintLabel
    ) {
        public SlotRequirement {
            Objects.requireNonNull(ingredient, "ingredient");
            if (baseCount < 1) {
                throw new IllegalArgumentException("baseCount must be >= 1, got " + baseCount);
            }
            Objects.requireNonNull(hintLabel, "hintLabel");
        }

        /**
         * Computes the required item count for this slot scaled by {@code multiplier}, clamped to
         * the standard 64-item slot stack limit so large parts remain craftable in Survival.
         */
        public int requiredCount(int multiplier) {
            long scaled = (long) this.baseCount * Math.max(1, multiplier);
            return (int) Math.min(MAX_SLOT_STACK_SIZE, Math.max(1L, scaled));
        }

        public @NotNull ItemStack[] displayStacks() {
            return this.ingredient.getItems();
        }

        public @NotNull ItemStack primaryDisplayStack() {
            ItemStack[] items = this.ingredient.getItems();
            return items.length > 0 ? items[0] : ItemStack.EMPTY;
        }
    }

    /**
     * Returns the dynamic silhouette layout for {@code schematicStack} if it is a
     * {@link PartCategorySchematicItem}, otherwise returns {@link #DEFAULT_3X3}.
     */
    public static @NotNull SchematicGridLayout forSchematic(@Nullable ItemStack schematicStack) {
        if (schematicStack != null && !schematicStack.isEmpty()
                && schematicStack.getItem() instanceof PartCategorySchematicItem categorySchematic) {
            return forCategory(categorySchematic.getCategory());
        }
        return DEFAULT_3X3;
    }

    /**
     * Returns the dynamic silhouette layout for {@code category}, or {@link #DEFAULT_3X3} if null.
     */
    public static @NotNull SchematicGridLayout forCategory(@Nullable RocketPartCategory category) {
        if (category == null) {
            return DEFAULT_3X3;
        }
        return getCategoryLayouts().getOrDefault(category, DEFAULT_3X3);
    }

    public @Nullable RocketPartCategory category() {
        return this.category;
    }

    public boolean isModular() {
        return this.category != null;
    }

    public int activeSlotCount() {
        return this.activeSlotCount;
    }

    public boolean isSlotActive(int gridIndex) {
        return gridIndex >= 0 && gridIndex < GRID_SIZE && this.activeMask[gridIndex];
    }

    public int slotX(int gridIndex) {
        if (gridIndex < 0 || gridIndex >= GRID_SIZE) {
            return GRID_ORIGIN_X;
        }
        return this.slotX[gridIndex];
    }

    public int slotY(int gridIndex) {
        if (gridIndex < 0 || gridIndex >= GRID_SIZE) {
            return GRID_ORIGIN_Y;
        }
        return this.slotY[gridIndex];
    }

    public @Nullable SlotRequirement requirement(int gridIndex) {
        if (gridIndex < 0 || gridIndex >= GRID_SIZE) {
            return null;
        }
        return this.requirements[gridIndex];
    }

    public boolean[] activeMask() {
        return Arrays.copyOf(this.activeMask, GRID_SIZE);
    }

    public int[] slotX() {
        return Arrays.copyOf(this.slotX, GRID_SIZE);
    }

    public int[] slotY() {
        return Arrays.copyOf(this.slotY, GRID_SIZE);
    }

    public @Nullable SlotRequirement[] requirements() {
        return Arrays.copyOf(this.requirements, GRID_SIZE);
    }

    /**
     * Verifies that every active slot in {@code gridStorage} contains an item matching its
     * {@link SlotRequirement} with count at least {@code baseCount * multiplier}. Inactive slots
     * are ignored gracefully.
     */
    public boolean matchesGrid(@NotNull SlottedStorageAccess<Item, ItemResourceSlot> gridStorage, int multiplier) {
        Objects.requireNonNull(gridStorage, "gridStorage");
        if (!this.isModular() || gridStorage.size() < GRID_SIZE) {
            return false;
        }
        int safeMultiplier = Math.max(1, multiplier);
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!this.activeMask[i]) {
                continue;
            }
            SlotRequirement req = this.requirements[i];
            if (req == null) {
                continue;
            }
            ItemResourceSlot slot = gridStorage.slot(i);
            if (slot.isEmpty()) {
                return false;
            }
            int required = req.requiredCount(safeMultiplier);
            if (slot.getAmount() < required) {
                return false;
            }
            ItemStack stack = ItemStackUtil.create(slot);
            if (!req.ingredient().test(stack)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts exact {@code baseCount * multiplier} items from every active slot in {@code gridStorage}.
     */
    public void consumeGrid(@NotNull SlottedStorageAccess<Item, ItemResourceSlot> gridStorage, int multiplier) {
        Objects.requireNonNull(gridStorage, "gridStorage");
        if (!this.isModular() || gridStorage.size() < GRID_SIZE) {
            return;
        }
        int safeMultiplier = Math.max(1, multiplier);
        for (int i = 0; i < GRID_SIZE; i++) {
            if (!this.activeMask[i]) {
                continue;
            }
            SlotRequirement req = this.requirements[i];
            if (req == null) {
                continue;
            }
            int required = req.requiredCount(safeMultiplier);
            gridStorage.slot(i).extract(required);
        }
    }

    private static SchematicGridLayout createDefault3x3() {
        boolean[] mask = new boolean[GRID_SIZE];
        int[] xs = new int[GRID_SIZE];
        int[] ys = new int[GRID_SIZE];
        SlotRequirement[] reqs = new SlotRequirement[GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            mask[i] = true;
            xs[i] = GRID_ORIGIN_X + (i % 3) * SLOT_STEP;
            ys[i] = GRID_ORIGIN_Y + (i / 3) * SLOT_STEP;
        }
        return new SchematicGridLayout(null, mask, xs, ys, reqs);
    }

    private static Map<RocketPartCategory, SchematicGridLayout> getCategoryLayouts() {
        Map<RocketPartCategory, SchematicGridLayout> current = categoryLayouts;
        if (current != null) {
            return current;
        }
        synchronized (SchematicGridLayout.class) {
            current = categoryLayouts;
            if (current == null) {
                current = buildCategoryLayouts();
                categoryLayouts = current;
            }
            return current;
        }
    }

    private static Map<RocketPartCategory, SchematicGridLayout> buildCategoryLayouts() {
        EnumMap<RocketPartCategory, SchematicGridLayout> map = new EnumMap<>(RocketPartCategory.class);

        Ingredient heavyPlate = Ingredient.of(GCItems.TIER_1_HEAVY_DUTY_PLATE);
        Ingredient steelPlate = Ingredient.of(GCItems.COMPRESSED_STEEL);
        Ingredient aluminumPlate = Ingredient.of(GCItems.COMPRESSED_ALUMINUM);
        Ingredient titaniumPlate = Ingredient.of(GCItems.COMPRESSED_TITANIUM);
        Ingredient basicWafer = Ingredient.of(GCItems.BASIC_WAFER);
        Ingredient advancedWafer = Ingredient.of(GCItems.ADVANCED_WAFER);
        Ingredient tinCanister = Ingredient.of(GCItems.TIN_CANISTER);
        Ingredient oxygenVent = Ingredient.of(GCItems.OXYGEN_VENT);
        Ingredient viewportGlass = Ingredient.of(GCBlocks.VACUUM_GLASS, Items.GLASS);

        // COMMAND: 5 active slots [1, 3, 4, 5, 7] — capsule / nose-cone pyramid silhouette
        map.put(RocketPartCategory.COMMAND, new Builder(RocketPartCategory.COMMAND)
                .slot(1, 48, 17, heavyPlate, 2, "Nose Heat Shield")
                .slot(3, 30, 35, aluminumPlate, 2, "Port Hull Plate")
                .slot(4, 48, 35, viewportGlass, 1, "Cockpit Glass")
                .slot(5, 66, 35, aluminumPlate, 2, "Starboard Hull Plate")
                .slot(7, 48, 53, advancedWafer, 1, "Avionics Core")
                .build());

        // ENGINE: 6 active slots [1, 3, 4, 6, 7, 8] — bell nozzle silhouette
        map.put(RocketPartCategory.ENGINE, new Builder(RocketPartCategory.ENGINE)
                .slot(1, 48, 17, basicWafer, 1, "Injector Manifold")
                .slot(3, 39, 35, oxygenVent, 1, "Turbopump")
                .slot(4, 57, 35, tinCanister, 1, "Combustion Chamber")
                .slot(6, 30, 53, steelPlate, 2, "Nozzle Skirt")
                .slot(7, 48, 53, heavyPlate, 2, "Nozzle Throat")
                .slot(8, 66, 53, steelPlate, 2, "Nozzle Skirt")
                .build());

        // TANK: 6 active slots [0, 2, 3, 5, 6, 8] — tall pressurized cylinder silhouette
        map.put(RocketPartCategory.TANK, new Builder(RocketPartCategory.TANK)
                .slot(0, 36, 17, aluminumPlate, 2, "Upper Tank Ring")
                .slot(2, 60, 17, aluminumPlate, 2, "Upper Tank Ring")
                .slot(3, 36, 35, tinCanister, 2, "Propellant Cell")
                .slot(5, 60, 35, tinCanister, 2, "Propellant Cell")
                .slot(6, 36, 53, heavyPlate, 2, "Lower Bulkhead")
                .slot(8, 60, 53, heavyPlate, 2, "Lower Bulkhead")
                .build());

        // ADAPTER: 4 active slots [1, 3, 5, 7] — trapezoid / transition collar silhouette
        map.put(RocketPartCategory.ADAPTER, new Builder(RocketPartCategory.ADAPTER)
                .slot(1, 48, 17, aluminumPlate, 2, "Upper Collar")
                .slot(3, 30, 35, steelPlate, 2, "Transition Strut")
                .slot(5, 66, 35, steelPlate, 2, "Transition Strut")
                .slot(7, 48, 53, heavyPlate, 2, "Base Ring")
                .build());

        // PAYLOAD: 5 active slots [0, 2, 4, 6, 8] — hollow fairing bay & mounting core
        map.put(RocketPartCategory.PAYLOAD, new Builder(RocketPartCategory.PAYLOAD)
                .slot(0, 30, 17, aluminumPlate, 2, "Upper Fairing")
                .slot(2, 66, 17, aluminumPlate, 2, "Upper Fairing")
                .slot(4, 48, 35, basicWafer, 1, "Mounting Core")
                .slot(6, 30, 53, steelPlate, 2, "Lower Bulkhead")
                .slot(8, 66, 53, steelPlate, 2, "Lower Bulkhead")
                .build());

        // UTILITY: 4 active slots [1, 3, 4, 5] — precision cross / actuator array
        map.put(RocketPartCategory.UTILITY, new Builder(RocketPartCategory.UTILITY)
                .slot(1, 48, 22, titaniumPlate, 1, "Precision Mount")
                .slot(3, 30, 40, steelPlate, 1, "Actuator Arm")
                .slot(4, 48, 40, basicWafer, 1, "Control Wafer")
                .slot(5, 66, 40, steelPlate, 1, "Actuator Arm")
                .build());

        return Collections.unmodifiableMap(map);
    }

    private static final class Builder {
        private final RocketPartCategory category;
        private final boolean[] activeMask = new boolean[GRID_SIZE];
        private final int[] slotX = new int[GRID_SIZE];
        private final int[] slotY = new int[GRID_SIZE];
        private final @Nullable SlotRequirement[] requirements = new SlotRequirement[GRID_SIZE];

        private Builder(RocketPartCategory category) {
            this.category = category;
            for (int i = 0; i < GRID_SIZE; i++) {
                this.slotX[i] = GRID_ORIGIN_X + (i % 3) * SLOT_STEP;
                this.slotY[i] = GRID_ORIGIN_Y + (i / 3) * SLOT_STEP;
            }
        }

        private Builder slot(int index, int x, int y, Ingredient ingredient, int baseCount, String hintLabel) {
            if (index < 0 || index >= GRID_SIZE) {
                throw new IllegalArgumentException("Grid slot index out of range: " + index);
            }
            this.activeMask[index] = true;
            this.slotX[index] = x;
            this.slotY[index] = y;
            this.requirements[index] = new SlotRequirement(ingredient, baseCount, hintLabel);
            return this;
        }

        private SchematicGridLayout build() {
            return new SchematicGridLayout(this.category, this.activeMask, this.slotX, this.slotY, this.requirements);
        }
    }
}
