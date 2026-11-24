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

import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition.SlotMaterialEntry;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.machinelib.api.storage.SlottedStorageAccess;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.util.ItemStackUtil;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.item.PartCategorySchematicItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines Tinkers'-style dynamic crafting grid silhouettes, anatomical blueprint slot coordinates,
 * multi-material per-slot requirements, and component explanations for each {@link RocketPartCategory}
 * and specialized part family inside the Rocket Part Builder.
 *
 * <p>Grid indices {@code 0..8} map directly to {@code RocketWorkbenchBlockEntity} storage slots
 * {@code 2..10}. All active GUI coordinates stay strictly within the CAD blueprint bay
 * {@code x in [28..116], y in [20..92]} with at least 18 px slot spacing so active slots never overlap.
 */
public final class SchematicGridLayout {
    public static final int GRID_SIZE = 9;
    public static final int GRID_ORIGIN_X = 54;
    public static final int GRID_ORIGIN_Y = 38;
    public static final int SLOT_STEP = 18;
    public static final int MAX_SLOT_STACK_SIZE = 64;

    public static final SchematicGridLayout DEFAULT_3X3 = createDefault3x3();

    private static final ResourceLocation CARBON_COMPOSITE_ID = Constant.id("carbon_composite");

    private static final Set<ResourceLocation> ENGINE_CHAMBER_NERVA = Set.of(
            Constant.id("nuclear_thermal_core"),
            Constant.id("helium3_dfd_plasma")
    );

    private static final Set<ResourceLocation> ENGINE_CHAMBER_ELECTRIC = Set.of(
            Constant.id("ion_beam_core"),
            Constant.id("lunar_sapphire_optics"),
            Constant.id("helium3_dfd_plasma"),
            Constant.id("superionic_ice_crystal"),
            Constant.id("metallic_hydrogen_cell")
    );

    private static final Set<ResourceLocation> ENGINE_CHAMBER_CHEMICAL = Set.of(
            Constant.id("tin_canister_cell"),
            Constant.id("copper_canister_cell"),
            Constant.id("fluid_canister_cell"),
            Constant.id("compressed_desh"),
            Constant.id("compressed_titanium"),
            Constant.id("compressed_niobium"),
            Constant.id("palladium_niobium"),
            Constant.id("metallic_hydrogen_cell")
    );

    private static final Set<ResourceLocation> UTILITY_ACTUATOR_SOLAR = Set.of(
            Constant.id("single_solar_module_panel"),
            Constant.id("full_solar_panel"),
            Constant.id("solar_array_panel")
    );

    private static final Set<ResourceLocation> UTILITY_ACTUATOR_BATTERY = Set.of(
            Constant.id("standard_battery_cell"),
            Constant.id("atomic_battery_cell")
    );

    private static final Set<ResourceLocation> UTILITY_ACTUATOR_RADIATOR = Set.of(
            Constant.id("compressed_aluminum"),
            Constant.id("compressed_copper"),
            Constant.id("isothermal_sofi_insulation"),
            Constant.id("sofi_cryogenic_insulation"),
            Constant.id("compressed_bronze"),
            Constant.id("compressed_titanium"),
            Constant.id("compressed_niobium"),
            Constant.id("carbon_composite")
    );

    private static final Set<ResourceLocation> UTILITY_CONTROL_RTG = Set.of(
            Constant.id("nuclear_thermal_core"),
            Constant.id("helium3_fusion_reactor_core"),
            Constant.id("atomic_battery_cell")
    );

    private static final Set<ResourceLocation> UTILITY_CONTROL_BATTERY = Set.of(
            Constant.id("standard_battery_cell"),
            Constant.id("atomic_battery_cell"),
            Constant.id("superconducting_coil_bus"),
            Constant.id("helium3_fusion_reactor_core")
    );

    private static volatile @Nullable Map<RocketPartCategory, SchematicGridLayout> categoryLayouts;
    private static final Map<String, SchematicGridLayout> familyLayoutCache = new ConcurrentHashMap<>();

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
     * Multi-material crafting requirement and UI blueprint hint for an active grid slot.
     *
     * <p>Accepts any {@link ItemStack} matching one of {@link #acceptedProfiles()} (evaluating
     * fluid-bearing profiles such as Helium-3 canisters before generic container profiles).
     */
    public record SlotRequirement(
            @NotNull SlotRole role,
            @NotNull List<RocketMaterialProfile> acceptedProfiles,
            int baseCount,
            @NotNull String hintLabel,
            @NotNull String hudName,
            @NotNull String description
    ) {
        public SlotRequirement(
                @NotNull SlotRole role,
                @NotNull List<RocketMaterialProfile> acceptedProfiles,
                int baseCount,
                @NotNull String hintLabel
        ) {
            this(role, acceptedProfiles, baseCount, hintLabel, hintLabel, role.displayName());
        }

        public SlotRequirement {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(acceptedProfiles, "acceptedProfiles");
            if (acceptedProfiles.isEmpty()) {
                throw new IllegalArgumentException("SlotRequirement for role " + role + " must accept at least one RocketMaterialProfile");
            }
            acceptedProfiles = List.copyOf(acceptedProfiles);
            if (baseCount < 1) {
                throw new IllegalArgumentException("baseCount must be >= 1, got " + baseCount);
            }
            Objects.requireNonNull(hintLabel, "hintLabel");
            Objects.requireNonNull(hudName, "hudName");
            Objects.requireNonNull(description, "description");
        }

        /**
         * Resolves the best-matching {@link RocketMaterialProfile} accepted by this slot for
         * {@code stack}, checking fluid-constrained profiles before generic container profiles.
         */
        public @NotNull Optional<RocketMaterialProfile> resolveProfile(@Nullable ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return Optional.empty();
            }
            for (RocketMaterialProfile profile : this.acceptedProfiles) {
                if (profile.requiresFluid() && profile.matches(stack)) {
                    return Optional.of(profile);
                }
            }
            for (RocketMaterialProfile profile : this.acceptedProfiles) {
                if (!profile.requiresFluid() && profile.matches(stack)) {
                    return Optional.of(profile);
                }
            }
            return Optional.empty();
        }

        /**
         * Returns {@code true} if {@code stack} matches any accepted material profile for this slot.
         */
        public boolean matches(@Nullable ItemStack stack) {
            return this.resolveProfile(stack).isPresent();
        }

        /**
         * Returns an {@link Ingredient} covering the display items accepted by this slot.
         * Callers validating stacks with fluid requirements should prefer {@link #matches(ItemStack)}.
         */
        public @NotNull Ingredient ingredient() {
            ItemStack[] stacks = this.displayStacks();
            return stacks.length == 0 ? Ingredient.EMPTY : Ingredient.of(stacks);
        }

        /**
         * Computes the required item count for this slot scaled by {@code multiplier}, clamped to
         * the standard 64-item slot stack limit so large parts remain craftable in Survival.
         */
        public int requiredCount(int multiplier) {
            long scaled = (long) this.baseCount * Math.max(1, multiplier);
            return (int) Math.min(MAX_SLOT_STACK_SIZE, Math.max(1L, scaled));
        }

        /**
         * Computes the required item count for {@code stack}, clamped to {@code stack}'s maximum
         * stack size so lower-stacking aerospace hardware (e.g. 16-stack parachutes/canisters or
         * 1-stack fusion cells/batteries) always remain craftable at every rocket diameter.
         */
        public int requiredCount(int multiplier, @Nullable ItemStack stack) {
            int required = this.requiredCount(multiplier);
            if (stack == null || stack.isEmpty()) {
                return required;
            }
            return Math.min(required, Math.max(1, stack.getMaxStackSize()));
        }

        /**
         * Returns representative display {@link ItemStack}s for every accepted profile in order,
         * used to cycle ghost icons in empty active slots.
         */
        public @NotNull ItemStack[] displayStacks() {
            List<ItemStack> stacks = new ArrayList<>(this.acceptedProfiles.size());
            for (RocketMaterialProfile profile : this.acceptedProfiles) {
                ItemStack stack = RocketMaterialRegistry.createDisplayStack(profile);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            return stacks.toArray(ItemStack[]::new);
        }

        public @NotNull ItemStack primaryDisplayStack() {
            for (RocketMaterialProfile profile : this.acceptedProfiles) {
                ItemStack stack = RocketMaterialRegistry.createDisplayStack(profile);
                if (!stack.isEmpty()) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
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
     * Returns the default category silhouette layout for {@code category}, or {@link #DEFAULT_3X3} if null.
     */
    public static @NotNull SchematicGridLayout forCategory(@Nullable RocketPartCategory category) {
        if (category == null) {
            return DEFAULT_3X3;
        }
        return getCategoryLayouts().getOrDefault(category, DEFAULT_3X3);
    }

    /**
     * Returns a category + family-aware silhouette layout for {@code config}, narrowing specialized
     * slots (such as NERVA fission/DFD chambers, VASIMR/Ion emitters, solar/RTG/battery cores) while
     * preserving the canonical category silhouette coordinates.
     */
    public static @NotNull SchematicGridLayout forConfig(@Nullable RocketPartConfig config) {
        if (config == null) {
            return DEFAULT_3X3;
        }
        String familyKey = config.category().getSerializedName() + ":" + config.familyId().toLowerCase(Locale.ROOT);
        return familyLayoutCache.computeIfAbsent(familyKey, ignored -> buildFamilyLayout(config.category(), config.familyId()));
    }

    /**
     * Returns the most specific layout available for the workbench state, preferring
     * {@link #forConfig(RocketPartConfig)} when {@code config} matches the inserted schematic.
     */
    public static @NotNull SchematicGridLayout forWorkbench(@Nullable ItemStack schematicStack, @Nullable RocketPartConfig config) {
        if (schematicStack != null && !schematicStack.isEmpty()
                && schematicStack.getItem() instanceof PartCategorySchematicItem categorySchematic) {
            if (config != null && config.category() == categorySchematic.getCategory()) {
                return forConfig(config);
            }
            return forCategory(categorySchematic.getCategory());
        }
        return DEFAULT_3X3;
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
            ItemStack stack = ItemStackUtil.create(slot);
            if (!req.matches(stack)) {
                return false;
            }
            int required = req.requiredCount(safeMultiplier, stack);
            if (slot.getAmount() < required) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extracts a {@link RocketPartComposition} from all active grid slots currently holding a
     * compatible material stack. Works on both partially filled grids (for live GUI preview) and
     * complete grids (for final fabrication stamping).
     */
    public @NotNull RocketPartComposition extractComposition(@NotNull SlottedStorageAccess<Item, ItemResourceSlot> gridStorage) {
        Objects.requireNonNull(gridStorage, "gridStorage");
        if (!this.isModular() || gridStorage.size() < GRID_SIZE) {
            return RocketPartComposition.EMPTY;
        }
        List<SlotMaterialEntry> entries = new ArrayList<>(this.activeSlotCount);
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
                continue;
            }
            ItemStack stack = ItemStackUtil.create(slot);
            Optional<RocketMaterialProfile> resolved = req.resolveProfile(stack);
            if (resolved.isEmpty()) {
                continue;
            }
            RocketMaterialProfile profile = resolved.get();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            List<RocketMaterialTrait> traits = resolveContextualTraits(this.category, req.role(), profile);
            entries.add(SlotMaterialEntry.of(i, req.role(), itemId, traits));
        }
        return entries.isEmpty() ? RocketPartComposition.EMPTY : new RocketPartComposition(entries);
    }

    private static List<RocketMaterialTrait> resolveContextualTraits(
            @Nullable RocketPartCategory category,
            SlotRole role,
            RocketMaterialProfile profile
    ) {
        if (category == RocketPartCategory.ADAPTER && CARBON_COMPOSITE_ID.equals(profile.id())) {
            LinkedHashSet<RocketMaterialTrait> combined = new LinkedHashSet<>(profile.traits());
            combined.add(RocketMaterialTrait.AERODYNAMIC_SEPARATION);
            return List.copyOf(combined);
        }
        return profile.traits();
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
            ItemResourceSlot slot = gridStorage.slot(i);
            ItemStack stack = slot.isEmpty() ? ItemStack.EMPTY : ItemStackUtil.create(slot);
            int required = req.requiredCount(safeMultiplier, stack);
            slot.extract(required);
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
        for (RocketPartCategory category : RocketPartCategory.values()) {
            map.put(category, buildFamilyLayout(category, ""));
        }
        return Collections.unmodifiableMap(map);
    }

    private static SchematicGridLayout buildFamilyLayout(RocketPartCategory category, @Nullable String familyId) {
        String family = familyId == null ? "" : familyId.toLowerCase(Locale.ROOT);
        return switch (category) {
            case COMMAND -> buildCommandLayout();
            case ENGINE -> buildEngineLayout(family);
            case TANK -> buildTankLayout();
            case ADAPTER -> buildAdapterLayout();
            case PAYLOAD -> buildPayloadLayout();
            case UTILITY -> buildUtilityLayout(family);
        };
    }

    // COMMAND: 5 active slots [1, 3, 4, 5, 7] — conical space capsule silhouette
    private static SchematicGridLayout buildCommandLayout() {
        return new Builder(RocketPartCategory.COMMAND)
                .slot(1, 73, 22, SlotRole.COMMAND_NOSE_RECOVERY,
                        orderedProfiles(SlotRole.COMMAND_NOSE_RECOVERY, Constant.id("tier_1_heavy_duty_plate")),
                        2, "Nose Recovery / Heat Shield",
                        "Topo / Escudo", "Paraquedas de pouso ou escudo TPS")
                .slot(3, 35, 52, SlotRole.STRUCTURAL_HULL,
                        orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_aluminum")),
                        2, "Port Hull Plate",
                        "Casco Bombordo", "Massa seca, calor e press\u00e3o (esq.)")
                .slot(4, 73, 52, SlotRole.COMMAND_VIEWPORT,
                        orderedProfiles(SlotRole.COMMAND_VIEWPORT, Constant.id("vacuum_glass_viewport")),
                        1, "Viewport / Optics",
                        "Visor / \u00d3ptica", "C\u00fapula de press\u00e3o, calor ou HUD")
                .slot(5, 111, 52, SlotRole.STRUCTURAL_HULL,
                        orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_aluminum")),
                        2, "Starboard Hull Plate",
                        "Casco Estibordo", "Aceita liga h\u00edbrida com bombordo")
                .slot(7, 73, 84, SlotRole.COMMAND_AVIONICS,
                        orderedProfiles(SlotRole.COMMAND_AVIONICS, Constant.id("advanced_wafer_computer")),
                        1, "Avionics & Power Core",
                        "N\u00facleo Avi\u00f4nica", "Computador, telemetria ou fus\u00e3o He-3")
                .build();
    }

    // ENGINE: 6 active slots [1, 3, 4, 6, 7, 8] — flared bell nozzle silhouette
    private static SchematicGridLayout buildEngineLayout(String family) {
        List<RocketMaterialProfile> chamberProfiles;
        String chamberHint;
        String chamberHudName;
        String chamberDesc;
        if (family.contains("ntr") || family.contains("nerva")) {
            chamberProfiles = filteredProfiles(SlotRole.ENGINE_CHAMBER, ENGINE_CHAMBER_NERVA, Constant.id("nuclear_thermal_core"));
            chamberHint = "NERVA Reactor / DFD Core";
            chamberHudName = "N\u00facleo NERVA";
            chamberDesc = "Ur\u00e2nio/RTG f\u00edssil ou plasma DFD He-3";
        } else if (family.contains("ion") || family.contains("plasma") || family.contains("vasimr")) {
            chamberProfiles = filteredProfiles(SlotRole.ENGINE_CHAMBER, ENGINE_CHAMBER_ELECTRIC, Constant.id("ion_beam_core"));
            chamberHint = "Plasma Emitter / DFD Core";
            chamberHudName = "Emissor Plasma";
            chamberDesc = "Beam Core/Safira ou plasma DFD He-3";
        } else if (!family.isEmpty()) {
            chamberProfiles = filteredProfiles(SlotRole.ENGINE_CHAMBER, ENGINE_CHAMBER_CHEMICAL, Constant.id("tin_canister_cell"));
            chamberHint = "Combustion Chamber";
            chamberHudName = "C\u00e2mara Queima";
            chamberDesc = "Press\u00e3o de c\u00e2mara e cat\u00e1lise";
        } else {
            chamberProfiles = orderedProfiles(SlotRole.ENGINE_CHAMBER, Constant.id("tin_canister_cell"));
            chamberHint = "Combustion Chamber / Core";
            chamberHudName = "C\u00e2mara / N\u00facleo";
            chamberDesc = "Ciclo propulsivo, f\u00edssil ou DFD";
        }

        return new Builder(RocketPartCategory.ENGINE)
                .slot(1, 73, 21, SlotRole.ENGINE_INJECTOR,
                        orderedProfiles(SlotRole.ENGINE_INJECTOR, Constant.id("basic_wafer_avionics")),
                        1, "Injector & Control",
                        "Injetor / Controle", "Acelera\u00e7\u00e3o, igni\u00e7\u00e3o e efici\u00eancia")
                .slot(3, 46, 48, SlotRole.ENGINE_TURBOPUMP,
                        orderedProfiles(SlotRole.ENGINE_TURBOPUMP, Constant.id("oxygen_vent_turbopump")),
                        1, "Turbopump / Feed",
                        "Turbobomba", "Vaz\u00e3o e ciclo Staged (+35% kN)")
                .slot(4, 100, 48, SlotRole.ENGINE_CHAMBER,
                        chamberProfiles,
                        1, chamberHint,
                        chamberHudName, chamberDesc)
                .slot(6, 34, 86, SlotRole.STRUCTURAL_HULL,
                        orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_steel")),
                        2, "Nozzle Skirt (L)",
                        "Saia Bocal (E)", "C-103 (+K) ou Pal\u00e1dio (+empuxo)")
                .slot(7, 73, 76, SlotRole.ENGINE_NOZZLE_THROAT,
                        orderedProfiles(SlotRole.ENGINE_NOZZLE_THROAT, Constant.id("tier_1_heavy_duty_plate")),
                        2, "Nozzle Thermal Throat",
                        "Garganta T\u00e9rmica", "Ponto mais quente: limite (K)")
                .slot(8, 112, 86, SlotRole.STRUCTURAL_HULL,
                        orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_steel")),
                        2, "Nozzle Skirt (R)",
                        "Saia Bocal (D)", "Aceita liga h\u00edbrida com esquerda")
                .build();
    }

    // TANK: 6 active slots [0, 2, 3, 5, 6, 8] — tall pressurized cryogenic cylinder silhouette
    private static SchematicGridLayout buildTankLayout() {
        List<RocketMaterialProfile> hullProfiles = orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_aluminum"));
        List<RocketMaterialProfile> cellProfiles = orderedProfiles(SlotRole.TANK_PROPELLANT_CELL, Constant.id("tin_canister_cell"));
        List<RocketMaterialProfile> valveProfiles = orderedProfiles(SlotRole.TANK_LOWER_VALVE, Constant.id("tier_1_heavy_duty_plate"));

        return new Builder(RocketPartCategory.TANK)
                .slot(0, 44, 24, SlotRole.STRUCTURAL_HULL, hullProfiles, 2, "Upper Tank Ring (L)",
                        "Anel Superior (E)", "Casco: massa seca e press\u00e3o")
                .slot(2, 102, 24, SlotRole.STRUCTURAL_HULL, hullProfiles, 2, "Upper Tank Ring (R)",
                        "Anel Superior (D)", "Aceita liga h\u00edbrida ou COPV")
                .slot(3, 44, 56, SlotRole.TANK_PROPELLANT_CELL, cellProfiles, 2, "Propellant Cell & Cryo",
                        "C\u00e9lula Cryo (E)", "Volume, SOFI ou He-3 (<2K 0% boil)")
                .slot(5, 102, 56, SlotRole.TANK_PROPELLANT_CELL, cellProfiles, 2, "Propellant Cell & Cryo",
                        "C\u00e9lula Cryo (D)", "Segunda c\u00e9lula ou isolamento")
                .slot(6, 44, 88, SlotRole.TANK_LOWER_VALVE, valveProfiles, 2, "Lower Bulkhead & Valves",
                        "Antepara / V\u00e1lv. (E)", "V\u00e1lvula, bomba ou cryocooler")
                .slot(8, 102, 88, SlotRole.TANK_LOWER_VALVE, valveProfiles, 2, "Lower Bulkhead & Valves",
                        "Antepara / V\u00e1lv. (D)", "Refor\u00e7o inferior e vaz\u00e3o")
                .build();
    }

    // ADAPTER: 4 active slots [1, 3, 5, 7] — tapered frustum / interstage collar silhouette
    private static SchematicGridLayout buildAdapterLayout() {
        return new Builder(RocketPartCategory.ADAPTER)
                .slot(1, 73, 22, SlotRole.STRUCTURAL_HULL,
                        orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_aluminum")),
                        2, "Upper Collar",
                        "Colar Superior", "Interface superior de acoplamento")
                .slot(3, 38, 56, SlotRole.STRUCTURAL_HULL,
                        orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_steel")),
                        2, "Transition Strut / Fairing",
                        "Longarina (E)", "Carbono libera separa\u00e7\u00e3o aero")
                .slot(5, 108, 56, SlotRole.STRUCTURAL_HULL,
                        orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_steel")),
                        2, "Transition Strut / Fairing",
                        "Longarina (D)", "Rigidez estrutural e peso morto")
                .slot(7, 73, 88, SlotRole.ADAPTER_BASE_RING,
                        orderedProfiles(SlotRole.ADAPTER_BASE_RING, Constant.id("tier_1_heavy_duty_plate")),
                        2, "Base Ring / Blast Shield",
                        "Anel Base / TPS", "Resiste jato do est\u00e1gio superior")
                .build();
    }

    // PAYLOAD: 5 active slots [0, 2, 4, 6, 8] — split clamshell fairing & scientific core
    private static SchematicGridLayout buildPayloadLayout() {
        List<RocketMaterialProfile> upperHull = orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_aluminum"));
        List<RocketMaterialProfile> lowerHull = orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("compressed_steel"));
        List<RocketMaterialProfile> coreProfiles = orderedProfiles(SlotRole.PAYLOAD_CORE, Constant.id("basic_wafer_avionics"));

        return new Builder(RocketPartCategory.PAYLOAD)
                .slot(0, 38, 24, SlotRole.STRUCTURAL_HULL, upperHull, 2, "Upper Fairing (L)",
                        "Coifa Sup. (E)", "Prote\u00e7\u00e3o aerodin\u00e2mica superior")
                .slot(2, 108, 24, SlotRole.STRUCTURAL_HULL, upperHull, 2, "Upper Fairing (R)",
                        "Coifa Sup. (D)", "Metade direita da carenagem")
                .slot(4, 73, 56, SlotRole.PAYLOAD_CORE, coreProfiles, 1, "Payload & Cryo Core",
                        "N\u00facleo de Carga", "Ba\u00fa, sensores ou cofre mK He-3")
                .slot(6, 38, 88, SlotRole.STRUCTURAL_HULL, lowerHull, 2, "Lower Bulkhead (L)",
                        "Antepara Base (E)", "Base estrutural esquerda")
                .slot(8, 108, 88, SlotRole.STRUCTURAL_HULL, lowerHull, 2, "Lower Bulkhead (R)",
                        "Antepara Base (D)", "Base estrutural direita")
                .build();
    }

    // UTILITY: 4 active slots [1, 3, 4, 5] — pylon mount, central hub & bilateral wing/actuators
    private static SchematicGridLayout buildUtilityLayout(String family) {
        List<RocketMaterialProfile> mountProfiles = orderedProfiles(SlotRole.UTILITY_MOUNT, Constant.id("compressed_steel"));
        List<RocketMaterialProfile> actuatorProfiles;
        List<RocketMaterialProfile> controlProfiles;
        String actuatorHint;
        String controlHint;
        String actuatorHudName;
        String controlHudName;
        String actuatorDesc;
        String controlDesc;

        if (family.contains("power_solar_array")) {
            actuatorProfiles = filteredProfiles(SlotRole.UTILITY_ACTUATOR, UTILITY_ACTUATOR_SOLAR, Constant.id("full_solar_panel"));
            controlProfiles = orderedProfiles(SlotRole.UTILITY_CONTROL, Constant.id("basic_wafer_avionics"));
            actuatorHint = "Solar Panel Wing";
            controlHint = "Power Controller";
            actuatorHudName = "Asa Solar";
            controlHudName = "Controlador";
            actuatorDesc = "Painel solar (escala gera\u00e7\u00e3o GJ/t)";
            controlDesc = "Regulador de pot\u00eancia ou fus\u00e3o He-3";
        } else if (family.contains("power_rtg")) {
            actuatorProfiles = orderedProfiles(SlotRole.STRUCTURAL_HULL, Constant.id("lead_shielding"));
            controlProfiles = filteredProfiles(SlotRole.UTILITY_CONTROL, UTILITY_CONTROL_RTG, Constant.id("nuclear_thermal_core"));
            actuatorHint = "Radiation Shielding";
            controlHint = "RTG / Fusion Core";
            actuatorHudName = "Blindagem";
            controlHudName = "N\u00facleo RTG/He-3";
            actuatorDesc = "Blindagem lateral (Chumbo ou liga)";
            controlDesc = "RTG f\u00edssil ou reator aneutr\u00f4nico He-3";
        } else if (family.contains("power_battery_bank")) {
            actuatorProfiles = filteredProfiles(SlotRole.UTILITY_ACTUATOR, UTILITY_ACTUATOR_BATTERY, Constant.id("standard_battery_cell"));
            controlProfiles = filteredProfiles(SlotRole.UTILITY_CONTROL, UTILITY_CONTROL_BATTERY, Constant.id("standard_battery_cell"));
            actuatorHint = "Battery Cell Bank";
            controlHint = "Power Bus Core";
            actuatorHudName = "Banco Bateria";
            controlHudName = "Barramento";
            actuatorDesc = "C\u00e9lulas de bateria / at\u00f4mica";
            controlDesc = "Bobina Nb3Sn ou fus\u00e3o He-3";
        } else if (family.contains("thermal_radiator")) {
            actuatorProfiles = filteredProfiles(SlotRole.UTILITY_ACTUATOR, UTILITY_ACTUATOR_RADIATOR, Constant.id("compressed_aluminum"));
            controlProfiles = orderedProfiles(SlotRole.UTILITY_CONTROL, Constant.id("basic_wafer_avionics"));
            actuatorHint = "Radiator Fin";
            controlHint = "Thermal Control Core";
            actuatorHudName = "Aleta Radiador";
            controlHudName = "Controle T\u00e9rmico";
            actuatorDesc = "Dissipa\u00e7\u00e3o de calor (Al/Cu/SOFI)";
            controlDesc = "Gest\u00e3o t\u00e9rmica e sensores";
        } else {
            actuatorProfiles = orderedProfiles(SlotRole.UTILITY_ACTUATOR, Constant.id("compressed_steel"));
            controlProfiles = orderedProfiles(SlotRole.UTILITY_CONTROL, Constant.id("basic_wafer_avionics"));
            actuatorHint = "Actuator / Panel";
            controlHint = "Control / Power Core";
            actuatorHudName = "Atuador / Bra\u00e7o";
            controlHudName = "N\u00facleo Controle";
            actuatorDesc = "Bra\u00e7o mec\u00e2nico ou painel lateral";
            controlDesc = "Avi\u00f4nica, controle ou energia He-3";
        }

        return new Builder(RocketPartCategory.UTILITY)
                .slot(1, 73, 25, SlotRole.UTILITY_MOUNT, mountProfiles, 1, "Precision Mount",
                        "Suporte Topo", "Fixa\u00e7\u00e3o estrutural e absor\u00e7\u00e3o")
                .slot(3, 36, 63, SlotRole.UTILITY_ACTUATOR, actuatorProfiles, 1, actuatorHint,
                        actuatorHudName + " (E)", actuatorDesc)
                .slot(4, 73, 63, SlotRole.UTILITY_CONTROL, controlProfiles, 1, controlHint,
                        controlHudName, controlDesc)
                .slot(5, 110, 63, SlotRole.UTILITY_ACTUATOR, actuatorProfiles, 1, actuatorHint,
                        actuatorHudName + " (D)", actuatorDesc)
                .build();
    }

    private static List<RocketMaterialProfile> orderedProfiles(SlotRole role, @Nullable ResourceLocation preferredFirstId) {
        List<RocketMaterialProfile> base = RocketMaterialRegistry.profilesForRole(role);
        if (preferredFirstId == null || base.size() <= 1) {
            return base;
        }
        List<RocketMaterialProfile> ordered = new ArrayList<>(base.size());
        for (RocketMaterialProfile profile : base) {
            if (preferredFirstId.equals(profile.id())) {
                ordered.add(profile);
            }
        }
        for (RocketMaterialProfile profile : base) {
            if (!preferredFirstId.equals(profile.id())) {
                ordered.add(profile);
            }
        }
        return Collections.unmodifiableList(ordered);
    }

    private static List<RocketMaterialProfile> filteredProfiles(
            SlotRole role,
            Set<ResourceLocation> allowedIds,
            @Nullable ResourceLocation preferredFirstId
    ) {
        List<RocketMaterialProfile> base = RocketMaterialRegistry.profilesForRole(role);
        List<RocketMaterialProfile> filtered = new ArrayList<>(allowedIds.size());
        for (RocketMaterialProfile profile : base) {
            if (allowedIds.contains(profile.id())) {
                filtered.add(profile);
            }
        }
        if (filtered.isEmpty()) {
            return orderedProfiles(role, preferredFirstId);
        }
        if (preferredFirstId == null || filtered.size() <= 1) {
            return Collections.unmodifiableList(filtered);
        }
        List<RocketMaterialProfile> ordered = new ArrayList<>(filtered.size());
        for (RocketMaterialProfile profile : filtered) {
            if (preferredFirstId.equals(profile.id())) {
                ordered.add(profile);
            }
        }
        for (RocketMaterialProfile profile : filtered) {
            if (!preferredFirstId.equals(profile.id())) {
                ordered.add(profile);
            }
        }
        return Collections.unmodifiableList(ordered);
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

        private Builder slot(
                int index,
                int x,
                int y,
                SlotRole role,
                List<RocketMaterialProfile> acceptedProfiles,
                int baseCount,
                String hintLabel,
                String hudName,
                String description
        ) {
            if (index < 0 || index >= GRID_SIZE) {
                throw new IllegalArgumentException("Grid slot index out of range: " + index);
            }
            this.activeMask[index] = true;
            this.slotX[index] = x;
            this.slotY[index] = y;
            this.requirements[index] = new SlotRequirement(role, acceptedProfiles, baseCount, hintLabel, hudName, description);
            return this;
        }

        private SchematicGridLayout build() {
            return new SchematicGridLayout(this.category, this.activeMask, this.slotX, this.slotY, this.requirements);
        }
    }
}
