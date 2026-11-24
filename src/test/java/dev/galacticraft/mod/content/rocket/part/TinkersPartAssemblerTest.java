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

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition.SlotMaterialEntry;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.FluidCanisterItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.CostProfile;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator.SlotContribution;
import dev.galacticraft.mod.content.rocket.part.SchematicGridLayout.SlotRequirement;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tinkers' Part Assembler Multi-Material Layouts, Parametric Formulas & Tooltips")
class TinkersPartAssemblerTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        Gases.init();
        GCBlocks.register();
        GCItems.register();
    }

    @Test
    @DisplayName("Family-aware SchematicGridLayout accepts multi-material sets for NERVA, VASIMR, Tank, Command, Utility & Payload")
    void familyAwareMultiMaterialSlotFiltering() {
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();

        // 1. NERVA chamber (slot 4) accepts RADIOISOTOPE_CORE, URANIUM_INGOT, and HELIUM3_CELL, rejects BEAM_CORE & TIN_CANISTER
        RocketPartConfig nerva = catalog.getPart("engine_ntr_nerva_5x5_3b").toConfig(RocketLivery.WHITE);
        SchematicGridLayout nervaLayout = SchematicGridLayout.forConfig(nerva);
        SlotRequirement nervaChamber = nervaLayout.requirement(4);
        assertNotNull(nervaChamber);
        assertTrue(nervaChamber.matches(new ItemStack(GCItems.RADIOISOTOPE_CORE)));
        assertTrue(nervaChamber.matches(new ItemStack(GCItems.URANIUM_INGOT)));
        assertTrue(nervaChamber.matches(new ItemStack(GCItems.HELIUM3_CELL)));
        assertFalse(nervaChamber.matches(new ItemStack(GCItems.BEAM_CORE)));
        assertFalse(nervaChamber.matches(new ItemStack(GCItems.TIN_CANISTER)));
        // Nozzle skirts (slots 6 & 8) accept all 14 structural materials
        assertEquals(14, nervaLayout.requirement(6).acceptedProfiles().size());
        assertEquals(14, nervaLayout.requirement(8).acceptedProfiles().size());

        // 2. VASIMR chamber (slot 4) accepts BEAM_CORE, LUNAR_SAPPHIRE, HELIUM3_CELL, SUPERIONIC_ICE_CRYSTAL; rejects RADIOISOTOPE_CORE
        RocketPartConfig vasimr = catalog.getPart("engine_plasma_vasimr_5x5_3b").toConfig(RocketLivery.BLUE);
        SchematicGridLayout vasimrLayout = SchematicGridLayout.forConfig(vasimr);
        SlotRequirement vasimrChamber = vasimrLayout.requirement(4);
        assertNotNull(vasimrChamber);
        assertTrue(vasimrChamber.matches(new ItemStack(GCItems.BEAM_CORE)));
        assertTrue(vasimrChamber.matches(new ItemStack(GCItems.LUNAR_SAPPHIRE)));
        assertTrue(vasimrChamber.matches(new ItemStack(GCItems.HELIUM3_CELL)));
        assertTrue(vasimrChamber.matches(new ItemStack(GCItems.SUPERIONIC_ICE_CRYSTAL)));
        assertFalse(vasimrChamber.matches(new ItemStack(GCItems.RADIOISOTOPE_CORE)));

        // 3. TANK cell (slots 3 & 5) accepts BLUE_ICE, ISOTHERMAL_FABRIC, Helium-3 FLUID_CANISTER, HELIUM3_CELL
        RocketPartConfig cryoTank = catalog.getPart("tank_lox_cryo_3x3_2b").toConfig(RocketLivery.ORANGE);
        SchematicGridLayout tankLayout = SchematicGridLayout.forConfig(cryoTank);
        SlotRequirement tankCell = tankLayout.requirement(3);
        assertNotNull(tankCell);
        ItemStack he3Canister = FluidCanisterItem.getFilledCanister(GCItems.FLUID_CANISTER, Gases.HELIUM_3);
        assertTrue(tankCell.matches(new ItemStack(Items.BLUE_ICE)));
        assertTrue(tankCell.matches(new ItemStack(GCItems.ISOTHERMAL_FABRIC)));
        assertTrue(tankCell.matches(he3Canister));
        assertEquals(List.of(RocketMaterialTrait.SUPERFLUID_CRYO_2K), tankCell.resolveProfile(he3Canister).orElseThrow().traits());
        assertTrue(tankCell.matches(new ItemStack(GCItems.HELIUM3_CELL)));

        // 4. COMMAND & UTILITY support/power slots accept ATOMIC_BATTERY, SOLAR_PANEL, HELIUM3_FUSION_CELL
        RocketPartConfig gemini = catalog.getPart("capsule_gemini_3x3_2b").toConfig(RocketLivery.WHITE);
        SlotRequirement cmdAvionics = SchematicGridLayout.forConfig(gemini).requirement(7);
        assertNotNull(cmdAvionics);
        assertTrue(cmdAvionics.matches(new ItemStack(GCItems.ATOMIC_BATTERY)));
        assertTrue(cmdAvionics.matches(new ItemStack(GCItems.SOLAR_ARRAY_PANEL)));
        assertTrue(cmdAvionics.matches(new ItemStack(GCItems.HELIUM3_FUSION_CELL)));

        RocketPartConfig rtg = catalog.getPart("power_rtg_1x1_1b").toConfig(RocketLivery.WHITE);
        SlotRequirement rtgCore = SchematicGridLayout.forConfig(rtg).requirement(4);
        assertNotNull(rtgCore);
        assertTrue(rtgCore.matches(new ItemStack(GCItems.RADIOISOTOPE_CORE)));
        assertTrue(rtgCore.matches(new ItemStack(GCItems.ATOMIC_BATTERY)));
        assertTrue(rtgCore.matches(new ItemStack(GCItems.HELIUM3_FUSION_CELL)));

        // 5. PAYLOAD core (slot 4) accepts HELIUM3_CELL with MILLIKELVIN_CONTAINMENT
        RocketPartConfig sampleBay = catalog.getPart("science_sample_containment_bay_3x3_2b").toConfig(RocketLivery.WHITE);
        SlotRequirement payloadCore = SchematicGridLayout.forConfig(sampleBay).requirement(4);
        assertNotNull(payloadCore);
        assertTrue(payloadCore.matches(new ItemStack(GCItems.HELIUM3_CELL)));
        assertEquals(List.of(RocketMaterialTrait.MILLIKELVIN_CONTAINMENT),
                payloadCore.resolveProfile(new ItemStack(GCItems.HELIUM3_CELL)).orElseThrow().traits());
    }

    @Test
    @DisplayName("Parametric formulas compute deterministic mass, thermal, pressure, thrust, capacity, boil-off & integrity")
    void parametricCostCalculatorEquations() {
        RocketPartCatalog catalog = RocketPartCatalog.getInstance();
        RocketPartConfig gemini = catalog.getPart("capsule_gemini_3x3_2b").toConfig(RocketLivery.WHITE);
        CostProfile base = RocketPartCostCalculator.calculate(gemini);

        // Build Titanium + Olivine Glass + Advanced Wafer + Parachute vs Aluminum + Glass + Basic Wafer + Parachute
        RocketPartComposition basicComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(1, SlotRole.COMMAND_NOSE_RECOVERY, BuiltInRegistries.ITEM.getKey(GCItems.PARACHUTE), List.of(RocketMaterialTrait.ATMOSPHERIC_PARACHUTE)),
                SlotMaterialEntry.of(3, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_ALUMINUM), List.of(RocketMaterialTrait.LIGHTWEIGHT_ALLOY)),
                SlotMaterialEntry.of(4, SlotRole.COMMAND_VIEWPORT, BuiltInRegistries.ITEM.getKey(Items.GLASS), List.of()),
                SlotMaterialEntry.of(5, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_ALUMINUM), List.of(RocketMaterialTrait.LIGHTWEIGHT_ALLOY)),
                SlotMaterialEntry.of(7, SlotRole.COMMAND_AVIONICS, BuiltInRegistries.ITEM.getKey(GCItems.BASIC_WAFER), List.of())
        ));

        RocketPartComposition titaniumComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(1, SlotRole.COMMAND_NOSE_RECOVERY, BuiltInRegistries.ITEM.getKey(GCItems.PARACHUTE), List.of(RocketMaterialTrait.ATMOSPHERIC_PARACHUTE)),
                SlotMaterialEntry.of(3, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_TITANIUM), List.of(RocketMaterialTrait.AEROSPACE_GRADE)),
                SlotMaterialEntry.of(4, SlotRole.COMMAND_VIEWPORT, BuiltInRegistries.ITEM.getKey(GCBlocks.OLIVINE_GLASS.asItem()), List.of(RocketMaterialTrait.REFRACTORY_CANOPY)),
                SlotMaterialEntry.of(5, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_TITANIUM), List.of(RocketMaterialTrait.AEROSPACE_GRADE)),
                SlotMaterialEntry.of(7, SlotRole.COMMAND_AVIONICS, BuiltInRegistries.ITEM.getKey(GCItems.ADVANCED_WAFER), List.of(RocketMaterialTrait.FLIGHT_COMPUTER))
        ));

        RocketPartComposition hybridComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(1, SlotRole.COMMAND_NOSE_RECOVERY, BuiltInRegistries.ITEM.getKey(GCItems.PARACHUTE), List.of(RocketMaterialTrait.ATMOSPHERIC_PARACHUTE)),
                SlotMaterialEntry.of(3, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_TITANIUM), List.of(RocketMaterialTrait.AEROSPACE_GRADE)),
                SlotMaterialEntry.of(4, SlotRole.COMMAND_VIEWPORT, BuiltInRegistries.ITEM.getKey(GCBlocks.OLIVINE_GLASS.asItem()), List.of(RocketMaterialTrait.REFRACTORY_CANOPY)),
                SlotMaterialEntry.of(5, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.LEAD_INGOT), List.of(RocketMaterialTrait.ACID_RADIATION_SHIELD)),
                SlotMaterialEntry.of(7, SlotRole.COMMAND_AVIONICS, BuiltInRegistries.ITEM.getKey(GCItems.ADVANCED_WAFER), List.of(RocketMaterialTrait.FLIGHT_COMPUTER))
        ));

        RocketPartComposition pureLeadComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(1, SlotRole.COMMAND_NOSE_RECOVERY, BuiltInRegistries.ITEM.getKey(GCItems.PARACHUTE), List.of(RocketMaterialTrait.ATMOSPHERIC_PARACHUTE)),
                SlotMaterialEntry.of(3, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.LEAD_INGOT), List.of(RocketMaterialTrait.ACID_RADIATION_SHIELD)),
                SlotMaterialEntry.of(4, SlotRole.COMMAND_VIEWPORT, BuiltInRegistries.ITEM.getKey(GCBlocks.OLIVINE_GLASS.asItem()), List.of(RocketMaterialTrait.REFRACTORY_CANOPY)),
                SlotMaterialEntry.of(5, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.LEAD_INGOT), List.of(RocketMaterialTrait.ACID_RADIATION_SHIELD)),
                SlotMaterialEntry.of(7, SlotRole.COMMAND_AVIONICS, BuiltInRegistries.ITEM.getKey(GCItems.ADVANCED_WAFER), List.of(RocketMaterialTrait.FLIGHT_COMPUTER))
        ));

        CostProfile basicCost = RocketPartCostCalculator.calculate(gemini, basicComp);
        CostProfile titaniumCost = RocketPartCostCalculator.calculate(gemini, titaniumComp);
        CostProfile hybridCost = RocketPartCostCalculator.calculate(gemini, hybridComp);
        CostProfile leadCost = RocketPartCostCalculator.calculate(gemini, pureLeadComp);

        // Titanium build must be lighter and far more tolerant than Aluminum+Glass build
        assertTrue(titaniumCost.stats().dryWeightKg() < basicCost.stats().dryWeightKg());
        assertTrue(titaniumCost.stats().thermalToleranceKelvin() > basicCost.stats().thermalToleranceKelvin() + 1500.0);
        assertTrue(titaniumCost.stats().pressureToleranceAtm() > basicCost.stats().pressureToleranceAtm() * 2.0);
        assertTrue(titaniumCost.maxIntegrity() > basicCost.maxIntegrity());

        // Hybrid Titanium+Lead hull must sit strictly between pure Titanium and pure Lead in mass, thermal, and pressure
        assertTrue(hybridCost.stats().dryWeightKg() > titaniumCost.stats().dryWeightKg());
        assertTrue(hybridCost.stats().dryWeightKg() < leadCost.stats().dryWeightKg());
        assertTrue(hybridCost.stats().thermalToleranceKelvin() < titaniumCost.stats().thermalToleranceKelvin());
        assertTrue(hybridCost.stats().thermalToleranceKelvin() > leadCost.stats().thermalToleranceKelvin());
        assertTrue(hybridCost.stats().pressureToleranceAtm() < titaniumCost.stats().pressureToleranceAtm());
        assertTrue(hybridCost.stats().pressureToleranceAtm() > leadCost.stats().pressureToleranceAtm());

        // Slot contributions: single Titanium port hull slot (slot 3) contributes -20% mass & +375 K
        List<SlotContribution> contribs = RocketPartCostCalculator.computeSlotContributions(gemini, hybridComp);
        SlotContribution portHull = contribs.stream().filter(c -> c.slotIndex() == 3).findFirst().orElseThrow();
        assertEquals(-0.20, portHull.massDeltaFraction(), 1.0e-6);
        assertEquals(375.0, portHull.thermalDeltaKelvin(), 1.0e-6);
        assertTrue(portHull.pressureDeltaAtm() > 10.0);

        // Safe mass floor >= 0.25x base even with ultralight Carbon Composite everywhere
        RocketPartComposition ultraLightTankComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(0, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.CARBON_FRAGMENTS), List.of(RocketMaterialTrait.COPV_WEAVE)),
                SlotMaterialEntry.of(2, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.CARBON_FRAGMENTS), List.of(RocketMaterialTrait.COPV_WEAVE)),
                SlotMaterialEntry.of(3, SlotRole.TANK_PROPELLANT_CELL, BuiltInRegistries.ITEM.getKey(GCItems.CARBON_FRAGMENTS), List.of(RocketMaterialTrait.COPV_WEAVE)),
                SlotMaterialEntry.of(5, SlotRole.TANK_PROPELLANT_CELL, BuiltInRegistries.ITEM.getKey(GCItems.CARBON_FRAGMENTS), List.of(RocketMaterialTrait.COPV_WEAVE)),
                SlotMaterialEntry.of(6, SlotRole.TANK_LOWER_VALVE, BuiltInRegistries.ITEM.getKey(GCItems.CARBON_FRAGMENTS), List.of(RocketMaterialTrait.COPV_WEAVE)),
                SlotMaterialEntry.of(8, SlotRole.TANK_LOWER_VALVE, BuiltInRegistries.ITEM.getKey(GCItems.CARBON_FRAGMENTS), List.of(RocketMaterialTrait.COPV_WEAVE))
        ));
        RocketPartConfig tankCfg = catalog.getPart("tank_lox_cryo_3x3_2b").toConfig(RocketLivery.ORANGE);
        CostProfile tankBase = RocketPartCostCalculator.calculate(tankCfg);
        CostProfile tankCarbon = RocketPartCostCalculator.calculate(tankCfg, ultraLightTankComp);
        assertTrue(tankCarbon.stats().dryWeightKg() >= Math.floor(tankBase.stats().dryWeightKg() * 0.25),
                "Mass must never drop below the 0.25x safety floor");
        assertTrue(tankCarbon.stats().fuelCapacityDroplets() > tankBase.stats().fuelCapacityDroplets(),
                "COPV Carbon Composite tank must gain propellant volume capacity");
        assertTrue(base.stats().dryWeightKg() > 0.0);
    }

    @Test
    @DisplayName("Tinkers' tooltip renders Construction line, % vs Base deltas, Traits, and SHIFT per-slot table including Helium-3")
    void tinkersTooltipSummaryAndShiftBreakdown() {
        RocketPartConfig tankCfg = RocketPartCatalog.getInstance().getPart("tank_lox_cryo_3x3_2b").toConfig(RocketLivery.ORANGE);
        RocketPartComposition he3TankComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(0, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_TITANIUM), List.of(RocketMaterialTrait.AEROSPACE_GRADE)),
                SlotMaterialEntry.of(2, SlotRole.STRUCTURAL_HULL, BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_TITANIUM), List.of(RocketMaterialTrait.AEROSPACE_GRADE)),
                SlotMaterialEntry.of(3, SlotRole.TANK_PROPELLANT_CELL, BuiltInRegistries.ITEM.getKey(GCItems.FLUID_CANISTER), List.of(RocketMaterialTrait.SUPERFLUID_CRYO_2K)),
                SlotMaterialEntry.of(5, SlotRole.TANK_PROPELLANT_CELL, BuiltInRegistries.ITEM.getKey(GCItems.ISOTHERMAL_FABRIC), List.of(RocketMaterialTrait.SOFI_INSULATION)),
                SlotMaterialEntry.of(6, SlotRole.TANK_LOWER_VALVE, BuiltInRegistries.ITEM.getKey(GCItems.FLUID_MANIPULATOR), List.of(RocketMaterialTrait.HIGH_FLOW_MANIFOLD)),
                SlotMaterialEntry.of(8, SlotRole.TANK_LOWER_VALVE, BuiltInRegistries.ITEM.getKey(GCItems.ATMOSPHERIC_VALVE), List.of(RocketMaterialTrait.PRESSURE_RELIEF_PURGE))
        ));
        CostProfile cost = RocketPartCostCalculator.calculate(tankCfg, he3TankComp);
        assertEquals(0.0, cost.boilOffMultiplier(), 1.0e-6);

        ItemStack tankStack = ModularRocketPartBlockItem.createConfiguredStack(
                tankCfg,
                cost.stats(),
                PartIntegrity.pristine(cost.maxIntegrity()),
                he3TankComp
        );

        // Normal summary tooltip
        List<Component> normal = new ArrayList<>();
        ModularRocketPartBlockItem.buildHoverTooltip(tankStack, RocketPartCategory.TANK, normal, false);
        String normalText = joinLines(normal);
        assertTrue(normalText.contains("Build:"), "Expected Build materials line");
        assertTrue(normalText.contains("Compressed Titanium"), "Expected Titanium in Build line");
        assertTrue(normalText.contains("Helium-3 Cryo Canister"), "Expected Helium-3 Cryo Canister disambiguation");
        assertTrue(normalText.contains("% vs Base)"), "Expected % vs Base dry mass delta");
        assertTrue(normalText.contains("0.00x (Zero)"), "Expected zero boil-off display");
        assertTrue(normalText.contains("Superfluid Cryo <2K"), "Expected SUPERFLUID_CRYO_2K trait");
        assertTrue(normalText.contains("SOFI Insulation"), "Expected SOFI_INSULATION trait");
        assertTrue(normalText.contains("Hold SHIFT to inspect 6 sub-components"), "Expected SHIFT prompt");
        assertFalse(normalText.contains("Sub-Component Breakdown"), "Breakdown must stay collapsed without SHIFT");
        for (Component line : normal) {
            assertTrue(line.getString().length() <= 54,
                    "Normal tooltip line must stay compact to prevent screen overflow: " + line.getString());
        }

        // SHIFT expanded tooltip
        List<Component> shifted = new ArrayList<>();
        ModularRocketPartBlockItem.buildHoverTooltip(tankStack, RocketPartCategory.TANK, shifted, true);
        String shiftText = joinLines(shifted);
        assertTrue(shiftText.contains("Sub-Component Breakdown (6 slots):"), "Expected SHIFT breakdown header");
        assertTrue(shiftText.contains("Upper Tank Ring (L): Compressed Titanium"), "Expected slot 0 detail");
        assertTrue(shiftText.contains("Propellant Cell & Cryo:"), "Expected slot 3 He-3 header");
        assertTrue(shiftText.contains("Helium-3 Cryo Canister"), "Expected slot 3 He-3 material name");
        assertTrue(shiftText.contains("0% Boil-Off"), "Expected 0% Boil-Off per-slot delta");
        assertTrue(shiftText.contains("-20% Mass"), "Expected -20% Mass contribution for Titanium hull slot");
        assertFalse(shiftText.contains("Hold SHIFT"), "SHIFT prompt must not show while SHIFT is held");
        for (Component line : shifted) {
            assertTrue(line.getString().length() <= 54,
                    "SHIFT tooltip line must stay compact to prevent screen overflow: " + line.getString());
        }

        // Parachute & empty Fluid Canister stack to 16; filled Fluid Canister remains individual (stack 1)
        assertEquals(16, GCItems.PARACHUTE.getDefaultMaxStackSize());
        assertEquals(16, GCItems.FLUID_CANISTER.getDefaultMaxStackSize());
        assertEquals(16, new ItemStack(GCItems.FLUID_CANISTER).getMaxStackSize());
        ItemStack filledHe3Canister = FluidCanisterItem.getFilledCanister(GCItems.FLUID_CANISTER, Gases.HELIUM_3);
        assertEquals(1, filledHe3Canister.getMaxStackSize());
        for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
            assertEquals(16, GCItems.DYED_PARACHUTES.get(dye).getDefaultMaxStackSize());
        }
        RocketPartConfig orion = RocketPartCatalog.getInstance().getPart("capsule_orion_5x5_3b").toConfig(RocketLivery.WHITE);
        SchematicGridLayout orionLayout = SchematicGridLayout.forConfig(orion);
        SlotRequirement noseSlot = orionLayout.requirement(1);
        SlotRequirement avionicsSlot = orionLayout.requirement(7);
        assertEquals(8, noseSlot.requiredCount(4, new ItemStack(GCItems.PARACHUTE)));
        assertEquals(16, noseSlot.requiredCount(16, new ItemStack(GCItems.PARACHUTE)));
        assertEquals(1, avionicsSlot.requiredCount(16, new ItemStack(GCItems.HELIUM3_FUSION_CELL)));
        SlotRequirement tankCellSlot = SchematicGridLayout.forConfig(tankCfg).requirement(3);
        assertEquals(1, tankCellSlot.requiredCount(4, filledHe3Canister));
        assertEquals(8, tankCellSlot.requiredCount(4, new ItemStack(GCItems.FLUID_CANISTER)));
    }

    private static String joinLines(List<Component> lines) {
        StringBuilder sb = new StringBuilder();
        for (Component c : lines) {
            sb.append(c.getString()).append('\n');
        }
        return sb.toString();
    }
}
