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

import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.FluidCanisterItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.tag.GCItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Central aerospace material catalog mapping all 14 structural materials and functional
 * sub-components to their physical {@link RocketMaterialProfile} modifiers and {@link RocketMaterialTrait}s.
 *
 * <p>Lazily initialized after item and block registration so unit tests and datagen can safely
 * reference this class without triggering premature registry lookups.
 */
public final class RocketMaterialRegistry {
    private static final EnumSet<SlotRole> ALL_STRUCTURAL_ROLES = EnumSet.of(
            SlotRole.STRUCTURAL_HULL,
            SlotRole.TANK_LOWER_VALVE,
            SlotRole.ENGINE_NOZZLE_THROAT,
            SlotRole.ADAPTER_BASE_RING,
            SlotRole.UTILITY_MOUNT,
            SlotRole.UTILITY_ACTUATOR
    );

    private static volatile @Nullable CatalogState state;

    private RocketMaterialRegistry() {
    }

    private record CatalogState(
            List<RocketMaterialProfile> allProfiles,
            List<RocketMaterialProfile> structuralProfiles,
            Map<ResourceLocation, RocketMaterialProfile> byId,
            Map<SlotRole, List<RocketMaterialProfile>> byRole
    ) {
    }

    public static @NotNull List<RocketMaterialProfile> allProfiles() {
        return getState().allProfiles();
    }

    /**
     * Returns the 14 canonical structural aerospace material profiles.
     */
    public static @NotNull List<RocketMaterialProfile> structuralProfiles() {
        return getState().structuralProfiles();
    }

    public static @NotNull Optional<RocketMaterialProfile> getById(@NotNull ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(getState().byId().get(id));
    }

    public static @NotNull List<RocketMaterialProfile> profilesForRole(@NotNull SlotRole role) {
        Objects.requireNonNull(role, "role");
        return getState().byRole().getOrDefault(role, List.of());
    }

    /**
     * Resolves the best-matching {@link RocketMaterialProfile} for {@code stack} when placed into
     * a specific workbench {@code role}. Fluid-specific profiles (such as Helium-3 canisters) are
     * evaluated before generic container profiles so specialized traits always take precedence.
     */
    public static @NotNull Optional<RocketMaterialProfile> resolve(@Nullable ItemStack stack, @NotNull SlotRole role) {
        Objects.requireNonNull(role, "role");
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        List<RocketMaterialProfile> candidates = profilesForRole(role);
        for (RocketMaterialProfile profile : candidates) {
            if (profile.requiresFluid() && profile.matches(stack)) {
                return Optional.of(profile);
            }
        }
        for (RocketMaterialProfile profile : candidates) {
            if (!profile.requiresFluid() && profile.matches(stack)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the first matching {@link RocketMaterialProfile} for {@code stack} across all roles,
     * prioritizing fluid-specific matches first.
     */
    public static @NotNull Optional<RocketMaterialProfile> resolve(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        List<RocketMaterialProfile> profiles = allProfiles();
        for (RocketMaterialProfile profile : profiles) {
            if (profile.requiresFluid() && profile.matches(stack)) {
                return Optional.of(profile);
            }
        }
        for (RocketMaterialProfile profile : profiles) {
            if (!profile.requiresFluid() && profile.matches(stack)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the {@link RocketMaterialProfile} corresponding to a persisted
     * {@link RocketPartComposition.SlotMaterialEntry}, disambiguating fluid-bearing items
     * (such as Helium-3 filled {@code FLUID_CANISTER} vs generic {@code FLUID_CANISTER})
     * via the entry's recorded {@link SlotRole} and {@link RocketMaterialTrait}s.
     */
    public static @NotNull Optional<RocketMaterialProfile> resolve(@Nullable RocketPartComposition.SlotMaterialEntry entry) {
        if (entry == null) {
            return Optional.empty();
        }
        SlotRole role = SlotRole.byName(entry.slotRole());
        List<RocketMaterialProfile> candidates = role != null ? profilesForRole(role) : allProfiles();
        Optional<RocketMaterialProfile> matched = matchEntryInCandidates(entry, candidates);
        if (matched.isPresent() || role == null) {
            return matched;
        }
        return matchEntryInCandidates(entry, allProfiles());
    }

    private static Optional<RocketMaterialProfile> matchEntryInCandidates(
            RocketPartComposition.SlotMaterialEntry entry,
            List<RocketMaterialProfile> candidates
    ) {
        ResourceLocation itemId = entry.itemId();
        Item item = BuiltInRegistries.ITEM.get(itemId);
        ItemStack probeStack = item != Items.AIR ? new ItemStack(item) : ItemStack.EMPTY;
        Set<RocketMaterialTrait> entryTraits = entry.traits().isEmpty()
                ? Collections.emptySet()
                : new HashSet<>(entry.traits());

        // Pass 1: exact trait set match on matching item/tag
        for (RocketMaterialProfile profile : candidates) {
            if (!matchesItemOrTag(profile, itemId, probeStack)) {
                continue;
            }
            if (entryTraits.isEmpty()) {
                if (profile.traits().isEmpty() && !profile.requiresFluid()) {
                    return Optional.of(profile);
                }
            } else if (profile.traits().size() == entryTraits.size() && entryTraits.containsAll(profile.traits())) {
                return Optional.of(profile);
            }
        }

        // Pass 2: profile traits are a non-empty subset of entry traits (handles extra contextual traits like AERODYNAMIC_SEPARATION)
        if (!entryTraits.isEmpty()) {
            RocketMaterialProfile best = null;
            int bestOverlap = 0;
            for (RocketMaterialProfile profile : candidates) {
                if (!matchesItemOrTag(profile, itemId, probeStack) || profile.traits().isEmpty()) {
                    continue;
                }
                if (entryTraits.containsAll(profile.traits()) && profile.traits().size() > bestOverlap) {
                    best = profile;
                    bestOverlap = profile.traits().size();
                }
            }
            if (best != null) {
                return Optional.of(best);
            }
        }

        // Pass 3: fallback to first non-fluid profile matching item/tag
        for (RocketMaterialProfile profile : candidates) {
            if (!profile.requiresFluid() && matchesItemOrTag(profile, itemId, probeStack)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    private static boolean matchesItemOrTag(RocketMaterialProfile profile, ResourceLocation itemId, ItemStack probeStack) {
        if (profile.itemIds().contains(itemId)) {
            return true;
        }
        return profile.itemTag() != null && !probeStack.isEmpty() && probeStack.is(profile.itemTag());
    }

    /**
     * Builds a representative display {@link ItemStack} for GUI ghost icons and tooltips,
     * pre-filling fluid containers when {@link RocketMaterialProfile#requiredFluidId()} is set.
     */
    public static @NotNull ItemStack createDisplayStack(@NotNull RocketMaterialProfile profile) {
        Objects.requireNonNull(profile, "profile");
        ResourceLocation primaryId = profile.primaryItemId();
        if (primaryId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(primaryId);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        if (profile.requiredFluidId() != null && item instanceof FluidCanisterItem) {
            Fluid fluid = BuiltInRegistries.FLUID.get(profile.requiredFluidId());
            if (fluid != Fluids.EMPTY) {
                return FluidCanisterItem.getFilledCanister(item, fluid);
            }
        }
        return new ItemStack(item);
    }

    private static CatalogState getState() {
        CatalogState current = state;
        if (current != null) {
            return current;
        }
        synchronized (RocketMaterialRegistry.class) {
            current = state;
            if (current == null) {
                current = buildCatalog();
                state = current;
            }
            return current;
        }
    }

    private static CatalogState buildCatalog() {
        List<RocketMaterialProfile> structural = new ArrayList<>(14);
        List<RocketMaterialProfile> all = new ArrayList<>(64);

        // =========================================================================
        // 1. THE 14 STRUCTURAL MATERIALS (Table 5.1)
        // =========================================================================

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("compressed_aluminum"), "Compressed Aluminum")
                .item(GCItems.COMPRESSED_ALUMINUM)
                .tag(GCItemTags.ALUMINUM_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .massModifier(-0.30)
                .thermalBonusKelvin(-150.0)
                .pressureMultiplier(0.85)
                .integrityMultiplier(1.00)
                .color(0xD8E2EC)
                .trait(RocketMaterialTrait.LIGHTWEIGHT_ALLOY)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("compressed_bronze"), "Compressed Bronze")
                .item(GCItems.COMPRESSED_BRONZE)
                .tag(GCItemTags.BRONZE_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .massModifier(0.15)
                .thermalBonusKelvin(100.0)
                .pressureMultiplier(1.15)
                .integrityMultiplier(1.15)
                .color(0xCD7F32)
                .trait(RocketMaterialTrait.THERMAL_DUCTILITY)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("compressed_steel"), "Compressed Steel")
                .item(GCItems.COMPRESSED_STEEL)
                .tag(GCItemTags.STEEL_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .massModifier(0.35)
                .thermalBonusKelvin(250.0)
                .pressureMultiplier(1.25)
                .integrityMultiplier(1.25)
                .color(0x8A959E)
                .trait(RocketMaterialTrait.BRUTE_RIGIDITY)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("compressed_ferroniobium"), "Compressed Ferroniobium")
                .item(GCItems.COMPRESSED_FERRONIOBIUM)
                .tag(GCItemTags.FERRONIOBIUM_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .massModifier(-0.10)
                .thermalBonusKelvin(450.0)
                .pressureMultiplier(1.45)
                .integrityMultiplier(1.40)
                .color(0x6E8CA0)
                .trait(RocketMaterialTrait.HSLA_MICROALLOY)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("compressed_meteoric_iron"), "Compressed Meteoric Iron")
                .item(GCItems.COMPRESSED_METEORIC_IRON)
                .tag(GCItemTags.METEORIC_IRON_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .massModifier(0.40)
                .thermalBonusKelvin(350.0)
                .pressureMultiplier(1.40)
                .integrityMultiplier(1.60)
                .color(0x6D635B)
                .trait(RocketMaterialTrait.MICROMETEOR_SHIELD)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("compressed_desh"), "Compressed Desh")
                .item(GCItems.COMPRESSED_DESH)
                .tag(GCItemTags.DESH_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .roles(SlotRole.ENGINE_CHAMBER)
                .massModifier(-0.15)
                .thermalBonusKelvin(500.0)
                .pressureMultiplier(1.80)
                .integrityMultiplier(1.50)
                .color(0xC45A3D)
                .trait(RocketMaterialTrait.ELASTIC_MEMORY)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("compressed_titanium"), "Compressed Titanium")
                .item(GCItems.COMPRESSED_TITANIUM)
                .tag(GCItemTags.TITANIUM_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .roles(SlotRole.ENGINE_CHAMBER)
                .massModifier(-0.40)
                .thermalBonusKelvin(750.0)
                .pressureMultiplier(2.10)
                .integrityMultiplier(1.75)
                .color(0xA8C8DC)
                .trait(RocketMaterialTrait.AEROSPACE_GRADE)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("lead_shielding"), "Lead Shielding")
                .item(GCItems.LEAD_INGOT)
                .tag(GCItemTags.LEAD_INGOTS)
                .roles(ALL_STRUCTURAL_ROLES)
                .massModifier(0.75)
                .thermalBonusKelvin(100.0)
                .pressureMultiplier(1.30)
                .integrityMultiplier(1.30)
                .color(0x5C6478)
                .trait(RocketMaterialTrait.ACID_RADIATION_SHIELD)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("carbon_composite"), "Carbon Fiber Composite")
                .items(GCItems.CARBON_FIBER_PLATE, GCItems.CARBON_FRAGMENTS)
                .roles(ALL_STRUCTURAL_ROLES)
                .roles(SlotRole.TANK_PROPELLANT_CELL, SlotRole.ENGINE_TURBOPUMP)
                .massModifier(-0.60)
                .thermalBonusKelvin(400.0)
                .pressureMultiplier(2.40)
                .integrityMultiplier(1.20)
                .capacityBonus(0.25)
                .color(0x3A3F47)
                .trait(RocketMaterialTrait.COPV_WEAVE)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("compressed_niobium"), "Niobium C-103 Superalloy")
                .item(GCItems.COMPRESSED_NIOBIUM)
                .tag(GCItemTags.NIOBIUM_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .roles(SlotRole.ENGINE_CHAMBER)
                .massModifier(-0.15)
                .thermalBonusKelvin(1400.0)
                .pressureMultiplier(2.00)
                .integrityMultiplier(1.80)
                .color(0x7BA4D0)
                .trait(RocketMaterialTrait.VACUUM_REFRACTORY)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("palladium_niobium"), "Palladium-Niobium Alloy")
                .items(GCItems.COMPRESSED_PALLADIUM, GCItems.PALLADIUM_INGOT)
                .tag(GCItemTags.PALLADIUM_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .roles(SlotRole.ENGINE_CHAMBER)
                .massModifier(0.05)
                .thermalBonusKelvin(1800.0)
                .pressureMultiplier(2.60)
                .integrityMultiplier(2.00)
                .thrustBonus(0.20)
                .color(0xE6D5A8)
                .trait(RocketMaterialTrait.SUPERCRITICAL_CATALYSIS)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("magnesium_lithium"), "Magnesium-Lithium Alloy")
                .items(GCItems.COMPRESSED_MAGNESIUM, GCItems.MAGNESIUM_INGOT)
                .tag(GCItemTags.MAGNESIUM_PLATES)
                .roles(ALL_STRUCTURAL_ROLES)
                .massModifier(-0.55)
                .thermalBonusKelvin(550.0)
                .pressureMultiplier(1.90)
                .integrityMultiplier(1.50)
                .color(0xC6D8C0)
                .trait(RocketMaterialTrait.ULTRALIGHT_SUPERFRAME)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("lonsdaleite_lattice"), "Lonsdaleite Lattice")
                .item(GCItems.LONSDALEITE_GEM)
                .tag(GCItemTags.LONSDALEITE_GEMS)
                .roles(ALL_STRUCTURAL_ROLES)
                .roles(SlotRole.COMMAND_VIEWPORT)
                .massModifier(-0.20)
                .thermalBonusKelvin(2200.0)
                .pressureMultiplier(4.50)
                .integrityMultiplier(3.00)
                .color(0x7FE8FF)
                .trait(RocketMaterialTrait.CRYSTALLINE_LATTICE)
                .build());

        registerStructural(structural, all, RocketMaterialProfile.builder(Constant.id("superionic_ice_crystal"), "Superionic Ice Crystal")
                .item(GCItems.SUPERIONIC_ICE_CRYSTAL)
                .tag(GCItemTags.SUPERIONIC_ICE_CRYSTALS)
                .roles(ALL_STRUCTURAL_ROLES)
                .roles(SlotRole.ENGINE_CHAMBER)
                .massModifier(-0.25)
                .thermalBonusKelvin(2800.0)
                .pressureMultiplier(3.50)
                .integrityMultiplier(2.50)
                .thrustBonus(0.35)
                .color(0x5EE0FF)
                .trait(RocketMaterialTrait.MAGNETIC_CONFINEMENT)
                .build());

        // =========================================================================
        // 2. COMPOSITE HEAVY-DUTY PLATES & COPPER THERMAL PLATES
        // =========================================================================

        all.add(RocketMaterialProfile.builder(Constant.id("tier_1_heavy_duty_plate"), "Tier 1 Heavy-Duty Plate")
                .item(GCItems.TIER_1_HEAVY_DUTY_PLATE)
                .tag(GCItemTags.TIER_1_HEAVY_DUTY_PLATES)
                .roles(SlotRole.COMMAND_NOSE_RECOVERY, SlotRole.TANK_LOWER_VALVE, SlotRole.ENGINE_NOZZLE_THROAT,
                        SlotRole.ADAPTER_BASE_RING, SlotRole.UTILITY_MOUNT)
                .massModifier(0.10)
                .thermalBonusKelvin(350.0)
                .pressureMultiplier(1.25)
                .integrityMultiplier(1.30)
                .color(0x9BA4AD)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("tier_2_heavy_duty_plate"), "Tier 2 Heavy-Duty Plate")
                .item(GCItems.TIER_2_HEAVY_DUTY_PLATE)
                .tag(GCItemTags.TIER_2_HEAVY_DUTY_PLATES)
                .roles(SlotRole.COMMAND_NOSE_RECOVERY, SlotRole.TANK_LOWER_VALVE, SlotRole.ENGINE_NOZZLE_THROAT,
                        SlotRole.ADAPTER_BASE_RING, SlotRole.UTILITY_MOUNT)
                .massModifier(0.05)
                .thermalBonusKelvin(650.0)
                .pressureMultiplier(1.55)
                .integrityMultiplier(1.60)
                .color(0xB8896E)
                .trait(RocketMaterialTrait.ELASTIC_MEMORY)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("tier_3_heavy_duty_plate"), "Tier 3 Heavy-Duty Plate")
                .item(GCItems.TIER_3_HEAVY_DUTY_PLATE)
                .tag(GCItemTags.TIER_3_HEAVY_DUTY_PLATES)
                .roles(SlotRole.COMMAND_NOSE_RECOVERY, SlotRole.TANK_LOWER_VALVE, SlotRole.ENGINE_NOZZLE_THROAT,
                        SlotRole.ADAPTER_BASE_RING, SlotRole.UTILITY_MOUNT)
                .massModifier(-0.10)
                .thermalBonusKelvin(950.0)
                .pressureMultiplier(1.90)
                .integrityMultiplier(1.90)
                .color(0x9BC2D6)
                .trait(RocketMaterialTrait.AEROSPACE_GRADE)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("compressed_copper"), "Compressed Copper")
                .item(GCItems.COMPRESSED_COPPER)
                .tag(GCItemTags.COPPER_PLATES)
                .roles(SlotRole.UTILITY_ACTUATOR)
                .massModifier(0.10)
                .thermalBonusKelvin(300.0)
                .pressureMultiplier(1.05)
                .integrityMultiplier(1.05)
                .color(0xD97742)
                .trait(RocketMaterialTrait.THERMAL_DUCTILITY)
                .build());

        // =========================================================================
        // 3. COMMAND: TOP RECOVERY / TPS, VIEWPORTS, AVIONICS & HE-3 REACTOR
        // =========================================================================

        RocketMaterialProfile.Builder parachuteBuilder = RocketMaterialProfile.builder(Constant.id("parachute_recovery"), "Atmospheric Parachute")
                .items(GCItems.PARACHUTE, GCItems.CANVAS)
                .tag(GCItemTags.PARACHUTES)
                .roles(SlotRole.COMMAND_NOSE_RECOVERY)
                .massModifier(-0.05)
                .thermalBonusKelvin(0.0)
                .pressureMultiplier(1.00)
                .integrityMultiplier(1.00)
                .color(0xF0F0F0)
                .trait(RocketMaterialTrait.ATMOSPHERIC_PARACHUTE);
        for (DyeColor color : DyeColor.values()) {
            parachuteBuilder.item(GCItems.DYED_PARACHUTES.get(color));
        }
        all.add(parachuteBuilder.build());

        all.add(RocketMaterialProfile.builder(Constant.id("supersonic_chute"), "Isothermal Supersonic Chute")
                .item(GCItems.ISOTHERMAL_FABRIC)
                .roles(SlotRole.COMMAND_NOSE_RECOVERY)
                .massModifier(-0.08)
                .thermalBonusKelvin(300.0)
                .pressureMultiplier(1.15)
                .integrityMultiplier(1.15)
                .color(0xFF9F43)
                .traits(RocketMaterialTrait.ATMOSPHERIC_PARACHUTE, RocketMaterialTrait.SUPERSONIC_CHUTE)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("olivine_ablative_tps"), "Olivine Ablative TPS")
                .item(GCItems.OLIVINE_SHARD)
                .tag(GCItemTags.OLIVINE_SHARDS)
                .roles(SlotRole.COMMAND_NOSE_RECOVERY, SlotRole.ENGINE_NOZZLE_THROAT, SlotRole.ADAPTER_BASE_RING)
                .massModifier(0.08)
                .thermalBonusKelvin(1200.0)
                .pressureMultiplier(1.35)
                .integrityMultiplier(1.45)
                .color(0x8FBC5A)
                .trait(RocketMaterialTrait.ABLATIVE_TPS)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("ablative_heat_tile_tps"), "Autoclaved Ablative Heat Tile")
                .item(GCItems.ABLATIVE_HEAT_TILE)
                .roles(SlotRole.COMMAND_NOSE_RECOVERY, SlotRole.ENGINE_NOZZLE_THROAT, SlotRole.ADAPTER_BASE_RING)
                .massModifier(0.04)
                .thermalBonusKelvin(1600.0)
                .pressureMultiplier(1.65)
                .integrityMultiplier(1.70)
                .color(0x7CB342)
                .trait(RocketMaterialTrait.ABLATIVE_TPS)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("standard_glass_viewport"), "Standard Glass")
                .item(Items.GLASS)
                .roles(SlotRole.COMMAND_VIEWPORT)
                .massModifier(0.0)
                .thermalBonusKelvin(-100.0)
                .pressureMultiplier(0.70)
                .integrityMultiplier(0.85)
                .color(0xCCE6F4)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("vacuum_glass_viewport"), "Vacuum Glass")
                .item(GCBlocks.VACUUM_GLASS)
                .roles(SlotRole.COMMAND_VIEWPORT)
                .massModifier(0.0)
                .thermalBonusKelvin(150.0)
                .pressureMultiplier(1.20)
                .integrityMultiplier(1.10)
                .color(0xA6D8F0)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("clear_vacuum_glass_viewport"), "Clear Vacuum Glass")
                .item(GCBlocks.CLEAR_VACUUM_GLASS)
                .roles(SlotRole.COMMAND_VIEWPORT)
                .massModifier(-0.02)
                .thermalBonusKelvin(200.0)
                .pressureMultiplier(1.30)
                .integrityMultiplier(1.15)
                .color(0xBFE8FA)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("strong_vacuum_glass_viewport"), "Strong Vacuum Glass")
                .item(GCBlocks.STRONG_VACUUM_GLASS)
                .roles(SlotRole.COMMAND_VIEWPORT)
                .massModifier(0.06)
                .thermalBonusKelvin(350.0)
                .pressureMultiplier(1.55)
                .integrityMultiplier(1.40)
                .color(0x7EAAC4)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("olivine_glass_canopy"), "Olivine Refractory Glass")
                .item(GCBlocks.OLIVINE_GLASS)
                .roles(SlotRole.COMMAND_VIEWPORT)
                .massModifier(0.04)
                .thermalBonusKelvin(900.0)
                .pressureMultiplier(2.20)
                .integrityMultiplier(1.85)
                .color(0x98D96B)
                .trait(RocketMaterialTrait.REFRACTORY_CANOPY)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("sensor_lens_optics"), "Optical Sensor Lens")
                .item(GCItems.SENSOR_LENS)
                .roles(SlotRole.COMMAND_VIEWPORT, SlotRole.PAYLOAD_CORE)
                .massModifier(-0.05)
                .thermalBonusKelvin(250.0)
                .pressureMultiplier(1.25)
                .integrityMultiplier(1.15)
                .color(0x62E6B5)
                .trait(RocketMaterialTrait.PROSPECTING_HUD)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("lunar_sapphire_optics"), "Lunar Sapphire Optics")
                .item(GCItems.LUNAR_SAPPHIRE)
                .tag(GCItemTags.LUNAR_SAPPHIRES)
                .roles(SlotRole.COMMAND_VIEWPORT, SlotRole.ENGINE_CHAMBER, SlotRole.PAYLOAD_CORE)
                .massModifier(-0.04)
                .thermalBonusKelvin(500.0)
                .pressureMultiplier(1.75)
                .integrityMultiplier(1.50)
                .thrustBonus(0.15)
                .color(0x4D94FF)
                .trait(RocketMaterialTrait.PROSPECTING_HUD)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("basic_wafer_avionics"), "Basic Wafer")
                .item(GCItems.BASIC_WAFER)
                .roles(SlotRole.COMMAND_AVIONICS, SlotRole.ENGINE_INJECTOR, SlotRole.PAYLOAD_CORE, SlotRole.UTILITY_CONTROL)
                .massModifier(0.0)
                .thermalBonusKelvin(0.0)
                .pressureMultiplier(1.00)
                .integrityMultiplier(1.00)
                .color(0x4D7C5A)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("advanced_wafer_computer"), "Advanced Flight Computer")
                .item(GCItems.ADVANCED_WAFER)
                .roles(SlotRole.COMMAND_AVIONICS, SlotRole.ENGINE_INJECTOR, SlotRole.PAYLOAD_CORE, SlotRole.UTILITY_CONTROL)
                .massModifier(-0.05)
                .thermalBonusKelvin(150.0)
                .pressureMultiplier(1.10)
                .integrityMultiplier(1.15)
                .thrustBonus(0.10)
                .color(0x55B8FF)
                .trait(RocketMaterialTrait.FLIGHT_COMPUTER)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("frequency_module_telemetry"), "Frequency Telemetry Module")
                .item(GCItems.FREQUENCY_MODULE)
                .tag(GCItemTags.FREQUENCY_MODULES)
                .roles(SlotRole.COMMAND_AVIONICS, SlotRole.PAYLOAD_CORE, SlotRole.UTILITY_CONTROL)
                .massModifier(-0.05)
                .thermalBonusKelvin(100.0)
                .pressureMultiplier(1.05)
                .integrityMultiplier(1.10)
                .color(0x72C3FF)
                .trait(RocketMaterialTrait.REMOTE_TELEMETRY)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("superconducting_coil_bus"), "Nb3Sn Superconducting Coil")
                .item(GCItems.SUPERCONDUCTING_COIL)
                .roles(SlotRole.COMMAND_AVIONICS, SlotRole.ENGINE_INJECTOR, SlotRole.PAYLOAD_CORE, SlotRole.UTILITY_CONTROL)
                .massModifier(-0.08)
                .thermalBonusKelvin(300.0)
                .pressureMultiplier(1.25)
                .integrityMultiplier(1.30)
                .thrustBonus(0.25)
                .color(0x5CA8FF)
                .trait(RocketMaterialTrait.SUPERCONDUCTING_BUS)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("ambient_climate_controller"), "Ambient Climate Controller")
                .item(GCItems.AMBIENT_THERMAL_CONTROLLER)
                .roles(SlotRole.COMMAND_AVIONICS)
                .massModifier(0.05)
                .thermalBonusKelvin(600.0)
                .pressureMultiplier(1.20)
                .integrityMultiplier(1.20)
                .color(0x52D1DC)
                .trait(RocketMaterialTrait.ACTIVE_CLIMATE_CONTROL)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("helium3_fusion_reactor_core"), "Helium-3 Aneutronic Fusion Cell")
                .item(GCItems.HELIUM3_FUSION_CELL)
                .roles(SlotRole.COMMAND_AVIONICS, SlotRole.UTILITY_CONTROL)
                .massModifier(-0.10)
                .thermalBonusKelvin(450.0)
                .pressureMultiplier(1.35)
                .integrityMultiplier(1.40)
                .color(0x70F8FF)
                .trait(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR)
                .build());

        // =========================================================================
        // 4. TANK: CELLS, CRYOGENICS, HELIUM-3 SUPERFLUID <2K & VALVES
        // =========================================================================

        all.add(RocketMaterialProfile.builder(Constant.id("tin_canister_cell"), "Tin Canister")
                .item(GCItems.TIN_CANISTER)
                .tag(GCItemTags.TIN_CANISTERS)
                .roles(SlotRole.TANK_PROPELLANT_CELL, SlotRole.ENGINE_CHAMBER)
                .massModifier(0.0)
                .thermalBonusKelvin(0.0)
                .pressureMultiplier(1.00)
                .integrityMultiplier(1.00)
                .color(0xC8CDD2)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("copper_canister_cell"), "Copper Canister")
                .item(GCItems.COPPER_CANISTER)
                .tag(GCItemTags.COPPER_CANISTERS)
                .roles(SlotRole.TANK_PROPELLANT_CELL, SlotRole.ENGINE_CHAMBER)
                .massModifier(0.04)
                .thermalBonusKelvin(100.0)
                .pressureMultiplier(1.10)
                .integrityMultiplier(1.05)
                .capacityBonus(0.15)
                .color(0xD9824B)
                .build());

        // Helium-3 filled Fluid Canister (checked before generic Fluid Canister)
        all.add(RocketMaterialProfile.builder(Constant.id("helium3_canister_cryo"), "Helium-3 Cryo Canister")
                .item(GCItems.FLUID_CANISTER)
                .requiredFluid(Gases.HELIUM_3_ID)
                .roles(SlotRole.TANK_PROPELLANT_CELL)
                .massModifier(-0.20)
                .thermalBonusKelvin(450.0)
                .pressureMultiplier(1.70)
                .integrityMultiplier(1.45)
                .capacityBonus(0.30)
                .boilOffMultiplier(0.0)
                .color(0x3CE0FF)
                .trait(RocketMaterialTrait.SUPERFLUID_CRYO_2K)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("helium3_cell_cryo"), "Helium-3 Superfluid Cell")
                .item(GCItems.HELIUM3_CELL)
                .roles(SlotRole.TANK_PROPELLANT_CELL)
                .massModifier(-0.20)
                .thermalBonusKelvin(500.0)
                .pressureMultiplier(1.80)
                .integrityMultiplier(1.50)
                .capacityBonus(0.25)
                .boilOffMultiplier(0.0)
                .color(0x3CE0FF)
                .trait(RocketMaterialTrait.SUPERFLUID_CRYO_2K)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("fluid_canister_cell"), "Pressurized Fluid Canister")
                .item(GCItems.FLUID_CANISTER)
                .roles(SlotRole.TANK_PROPELLANT_CELL, SlotRole.ENGINE_CHAMBER, SlotRole.PAYLOAD_CORE)
                .massModifier(0.02)
                .thermalBonusKelvin(120.0)
                .pressureMultiplier(1.35)
                .integrityMultiplier(1.20)
                .capacityBonus(0.30)
                .color(0x7FAFD4)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("thermal_cloth_insulation"), "Thermal Cloth Liner")
                .item(GCItems.THERMAL_CLOTH)
                .roles(SlotRole.TANK_PROPELLANT_CELL)
                .massModifier(-0.05)
                .thermalBonusKelvin(250.0)
                .pressureMultiplier(1.05)
                .integrityMultiplier(1.10)
                .boilOffMultiplier(0.35)
                .color(0xE09E50)
                .trait(RocketMaterialTrait.SOFI_INSULATION)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("isothermal_sofi_insulation"), "Isothermal SOFI Insulation")
                .item(GCItems.ISOTHERMAL_FABRIC)
                .roles(SlotRole.TANK_PROPELLANT_CELL, SlotRole.UTILITY_ACTUATOR)
                .massModifier(-0.10)
                .thermalBonusKelvin(450.0)
                .pressureMultiplier(1.15)
                .integrityMultiplier(1.20)
                .boilOffMultiplier(0.15)
                .color(0xE68A3E)
                .trait(RocketMaterialTrait.SOFI_INSULATION)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("sofi_cryogenic_insulation"), "Autoclaved SOFI Cryo Foam")
                .item(GCItems.SOFI_INSULATION)
                .roles(SlotRole.TANK_PROPELLANT_CELL, SlotRole.UTILITY_ACTUATOR)
                .massModifier(-0.14)
                .thermalBonusKelvin(650.0)
                .pressureMultiplier(1.35)
                .integrityMultiplier(1.35)
                .boilOffMultiplier(0.05)
                .color(0xF07D26)
                .trait(RocketMaterialTrait.SOFI_INSULATION)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("blue_ice_cryo_sink"), "Blue Ice Cryo Heat Sink")
                .item(Items.BLUE_ICE)
                .roles(SlotRole.TANK_PROPELLANT_CELL)
                .massModifier(0.12)
                .thermalBonusKelvin(150.0)
                .pressureMultiplier(1.00)
                .integrityMultiplier(0.95)
                .boilOffMultiplier(0.55)
                .color(0x74B9FF)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("metallic_hydrogen_cell"), "Metallic Hydrogen Cell")
                .item(GCItems.METALLIC_HYDROGEN_CELL)
                .roles(SlotRole.TANK_PROPELLANT_CELL, SlotRole.ENGINE_CHAMBER)
                .massModifier(-0.15)
                .thermalBonusKelvin(400.0)
                .pressureMultiplier(2.00)
                .integrityMultiplier(1.50)
                .thrustBonus(0.30)
                .capacityBonus(0.60)
                .color(0x5EE0FF)
                .trait(RocketMaterialTrait.MAGNETIC_CONFINEMENT)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("atmospheric_valve_purge"), "Atmospheric Relief Valve")
                .item(GCItems.ATMOSPHERIC_VALVE)
                .roles(SlotRole.TANK_LOWER_VALVE, SlotRole.ENGINE_TURBOPUMP)
                .massModifier(0.02)
                .thermalBonusKelvin(100.0)
                .pressureMultiplier(1.35)
                .integrityMultiplier(1.20)
                .thrustBonus(0.15)
                .color(0xA4B5C4)
                .trait(RocketMaterialTrait.PRESSURE_RELIEF_PURGE)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("fluid_manipulator_tank_pump"), "High-Flow Fluid Manipulator")
                .item(GCItems.FLUID_MANIPULATOR)
                .roles(SlotRole.TANK_LOWER_VALVE)
                .massModifier(0.05)
                .thermalBonusKelvin(150.0)
                .pressureMultiplier(1.25)
                .integrityMultiplier(1.25)
                .thrustBonus(0.10)
                .capacityBonus(0.10)
                .color(0xF5B041)
                .trait(RocketMaterialTrait.HIGH_FLOW_MANIFOLD)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("active_cryocooler_controller"), "Active Cryocooler Controller")
                .item(GCItems.AMBIENT_THERMAL_CONTROLLER)
                .roles(SlotRole.TANK_LOWER_VALVE)
                .massModifier(0.06)
                .thermalBonusKelvin(500.0)
                .pressureMultiplier(1.25)
                .integrityMultiplier(1.25)
                .boilOffMultiplier(0.0)
                .color(0x45C4F7)
                .trait(RocketMaterialTrait.ACTIVE_ZERO_BOILOFF)
                .build());

        // =========================================================================
        // 5. ENGINE: TURBOPUMPS, CHAMBERS, NUCLEAR/ION/DFD HE-3 PLASMA
        // =========================================================================

        all.add(RocketMaterialProfile.builder(Constant.id("oxygen_vent_turbopump"), "Standard Turbopump Vent")
                .items(GCItems.OXYGEN_VENT, GCItems.OXYGEN_FAN)
                .roles(SlotRole.ENGINE_TURBOPUMP)
                .massModifier(0.0)
                .thermalBonusKelvin(0.0)
                .pressureMultiplier(1.00)
                .integrityMultiplier(1.00)
                .color(0xB2BEC3)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("fluid_manipulator_staged"), "Staged-Combustion Turbopump")
                .item(GCItems.FLUID_MANIPULATOR)
                .roles(SlotRole.ENGINE_TURBOPUMP)
                .massModifier(0.08)
                .thermalBonusKelvin(250.0)
                .pressureMultiplier(1.30)
                .integrityMultiplier(1.25)
                .thrustBonus(0.35)
                .color(0xFF6B35)
                .trait(RocketMaterialTrait.STAGED_COMBUSTION)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("nuclear_thermal_core"), "Nuclear Thermal Fission Core")
                .items(GCItems.RADIOISOTOPE_CORE, GCItems.URANIUM_INGOT)
                .roles(SlotRole.ENGINE_CHAMBER, SlotRole.UTILITY_CONTROL)
                .massModifier(0.25)
                .thermalBonusKelvin(650.0)
                .pressureMultiplier(1.40)
                .integrityMultiplier(1.35)
                .thrustBonus(0.20)
                .color(0x80FF44)
                .trait(RocketMaterialTrait.NUCLEAR_THERMAL_CORE)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("ion_beam_core"), "Ion Plasma Beam Core")
                .item(GCItems.BEAM_CORE)
                .roles(SlotRole.ENGINE_CHAMBER)
                .massModifier(-0.05)
                .thermalBonusKelvin(400.0)
                .pressureMultiplier(1.20)
                .integrityMultiplier(1.25)
                .thrustBonus(0.25)
                .color(0x6EC6FF)
                .trait(RocketMaterialTrait.ION_BEAM_EMITTER)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("helium3_dfd_plasma"), "Helium-3 Direct Fusion Core")
                .item(GCItems.HELIUM3_CELL)
                .roles(SlotRole.ENGINE_CHAMBER)
                .massModifier(-0.10)
                .thermalBonusKelvin(900.0)
                .pressureMultiplier(1.60)
                .integrityMultiplier(1.50)
                .thrustBonus(0.45)
                .color(0x8C7BFF)
                .trait(RocketMaterialTrait.ANEUTRONIC_HE3_PLASMA)
                .build());

        // =========================================================================
        // 6. PAYLOAD & UTILITY SUB-COMPONENTS
        // =========================================================================

        all.add(RocketMaterialProfile.builder(Constant.id("cargo_chest_core"), "Standard Cargo Rack")
                .item(Items.CHEST)
                .roles(SlotRole.PAYLOAD_CORE)
                .massModifier(0.0)
                .thermalBonusKelvin(0.0)
                .pressureMultiplier(1.00)
                .integrityMultiplier(1.00)
                .color(0xB8864B)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("parachest_recovery_core"), "Parachest Recovery Bay")
                .item(GCBlocks.PARACHEST)
                .roles(SlotRole.PAYLOAD_CORE)
                .massModifier(0.04)
                .thermalBonusKelvin(100.0)
                .pressureMultiplier(1.10)
                .integrityMultiplier(1.15)
                .color(0xD8A86A)
                .trait(RocketMaterialTrait.ATMOSPHERIC_PARACHUTE)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("helium3_millikelvin_containment"), "Helium-3 Millikelvin Vault")
                .item(GCItems.HELIUM3_CELL)
                .roles(SlotRole.PAYLOAD_CORE)
                .massModifier(-0.05)
                .thermalBonusKelvin(350.0)
                .pressureMultiplier(1.50)
                .integrityMultiplier(1.40)
                .boilOffMultiplier(0.0)
                .color(0x99F0FF)
                .trait(RocketMaterialTrait.MILLIKELVIN_CONTAINMENT)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("single_solar_module_panel"), "Single Solar Module")
                .item(GCItems.SINGLE_SOLAR_MODULE)
                .roles(SlotRole.UTILITY_ACTUATOR)
                .massModifier(-0.05)
                .thermalBonusKelvin(50.0)
                .pressureMultiplier(0.95)
                .integrityMultiplier(0.95)
                .color(0x2B579A)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("full_solar_panel"), "Full Solar Panel")
                .item(GCItems.FULL_SOLAR_PANEL)
                .roles(SlotRole.UTILITY_ACTUATOR, SlotRole.COMMAND_AVIONICS)
                .massModifier(0.0)
                .thermalBonusKelvin(120.0)
                .pressureMultiplier(1.05)
                .integrityMultiplier(1.10)
                .color(0x3B78D8)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("solar_array_panel"), "Venusian Solar Array Panel")
                .item(GCItems.SOLAR_ARRAY_PANEL)
                .roles(SlotRole.UTILITY_ACTUATOR, SlotRole.COMMAND_AVIONICS)
                .massModifier(-0.08)
                .thermalBonusKelvin(350.0)
                .pressureMultiplier(1.30)
                .integrityMultiplier(1.35)
                .color(0xF5C542)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("standard_battery_cell"), "Standard Battery Cell")
                .item(GCItems.BATTERY)
                .roles(SlotRole.UTILITY_ACTUATOR, SlotRole.UTILITY_CONTROL)
                .massModifier(0.05)
                .thermalBonusKelvin(50.0)
                .pressureMultiplier(1.00)
                .integrityMultiplier(1.05)
                .color(0x52B788)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("atomic_battery_cell"), "Atomic Battery Cell")
                .item(GCItems.ATOMIC_BATTERY)
                .roles(SlotRole.UTILITY_ACTUATOR, SlotRole.UTILITY_CONTROL, SlotRole.COMMAND_AVIONICS)
                .massModifier(0.12)
                .thermalBonusKelvin(300.0)
                .pressureMultiplier(1.35)
                .integrityMultiplier(1.40)
                .color(0x80FF44)
                .trait(RocketMaterialTrait.NUCLEAR_THERMAL_CORE)
                .build());

        // =========================================================================
        // 7. REFURBISHMENT BENCH PROTECTIVE COATINGS
        // =========================================================================

        all.add(RocketMaterialProfile.builder(Constant.id("coating_lead_anti_acid"), "Lead Anti-Acid Cladding")
                .item(GCItems.LEAD_INGOT)
                .tag(GCItemTags.LEAD_INGOTS)
                .roles(SlotRole.PROTECTIVE_COATING)
                .massModifier(0.08)
                .thermalBonusKelvin(120.0)
                .pressureMultiplier(1.25)
                .integrityMultiplier(1.15)
                .color(0x5C6478)
                .trait(RocketMaterialTrait.ACID_RADIATION_SHIELD)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("coating_ablative_tps"), "Olivine TPS Heat Shield")
                .item(GCItems.ABLATIVE_HEAT_TILE)
                .roles(SlotRole.PROTECTIVE_COATING)
                .massModifier(0.06)
                .thermalBonusKelvin(1200.0)
                .pressureMultiplier(1.15)
                .integrityMultiplier(1.20)
                .color(0x8FBC5A)
                .trait(RocketMaterialTrait.ABLATIVE_TPS)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("coating_sofi_cryo"), "SOFI Cryogenic Foam Jacket")
                .item(GCItems.SOFI_INSULATION)
                .roles(SlotRole.PROTECTIVE_COATING)
                .massModifier(0.02)
                .thermalBonusKelvin(400.0)
                .pressureMultiplier(1.10)
                .integrityMultiplier(1.10)
                .boilOffMultiplier(0.15)
                .color(0xE68A3E)
                .trait(RocketMaterialTrait.SOFI_INSULATION)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("coating_lonsdaleite_pressure"), "Lonsdaleite Pressure Plating")
                .item(GCItems.LONSDALEITE_GEM)
                .roles(SlotRole.PROTECTIVE_COATING)
                .massModifier(0.04)
                .thermalBonusKelvin(1500.0)
                .pressureMultiplier(2.50)
                .integrityMultiplier(1.50)
                .color(0x7FE8FF)
                .trait(RocketMaterialTrait.CRYSTALLINE_LATTICE)
                .build());

        all.add(RocketMaterialProfile.builder(Constant.id("coating_niobium_c103"), "C-103 Refractory Coating")
                .item(GCItems.COMPRESSED_NIOBIUM)
                .roles(SlotRole.PROTECTIVE_COATING)
                .massModifier(0.05)
                .thermalBonusKelvin(900.0)
                .pressureMultiplier(1.40)
                .integrityMultiplier(1.25)
                .color(0x7BA4D0)
                .trait(RocketMaterialTrait.VACUUM_REFRACTORY)
                .build());

        // Index by ID and by SlotRole
        Map<ResourceLocation, RocketMaterialProfile> byId = new LinkedHashMap<>(all.size());
        EnumMap<SlotRole, List<RocketMaterialProfile>> byRoleMutable = new EnumMap<>(SlotRole.class);
        for (SlotRole role : SlotRole.values()) {
            byRoleMutable.put(role, new ArrayList<>());
        }

        for (RocketMaterialProfile profile : all) {
            RocketMaterialProfile prev = byId.put(profile.id(), profile);
            if (prev != null) {
                throw new IllegalStateException("Duplicate RocketMaterialProfile id: " + profile.id());
            }
            for (SlotRole role : profile.allowedRoles()) {
                byRoleMutable.get(role).add(profile);
            }
        }

        EnumMap<SlotRole, List<RocketMaterialProfile>> byRole = new EnumMap<>(SlotRole.class);
        for (Map.Entry<SlotRole, List<RocketMaterialProfile>> entry : byRoleMutable.entrySet()) {
            byRole.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }

        return new CatalogState(
                Collections.unmodifiableList(all),
                Collections.unmodifiableList(structural),
                Collections.unmodifiableMap(byId),
                Collections.unmodifiableMap(byRole)
        );
    }

    private static void registerStructural(
            List<RocketMaterialProfile> structural,
            List<RocketMaterialProfile> all,
            RocketMaterialProfile profile
    ) {
        structural.add(profile);
        all.add(profile);
    }
}
