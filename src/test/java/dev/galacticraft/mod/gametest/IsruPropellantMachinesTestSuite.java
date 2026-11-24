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

package dev.galacticraft.mod.gametest;

import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.api.registry.AddonRegistries;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.machinelib.api.block.entity.MachineBlockEntity;
import dev.galacticraft.machinelib.api.storage.slot.FluidResourceSlot;
import dev.galacticraft.machinelib.api.storage.slot.ItemResourceSlot;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.machine.AtmosphericFractionatorBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.ElectrolyticSeparatorBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.IsotopicCentrifugeBlockEntity;
import dev.galacticraft.mod.content.block.entity.machine.SabatierChemicalReactorBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.machine.GCMachineStatuses;
import dev.galacticraft.mod.util.FluidUtil;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.material.Fluids;
import team.reborn.energy.api.EnergyStorage;

public class IsruPropellantMachinesTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void electrolyticSeparatorStoichiometryAndDeuteriumTest(GameTestHelper context) {
        BlockPos waterPos = new BlockPos(0, 1, 0);
        BlockPos icePos = new BlockPos(2, 1, 0);
        BlockPos unpoweredPos = new BlockPos(4, 1, 0);
        BlockPos fracPos = new BlockPos(0, 1, 2);
        BlockPos sabatierPos = new BlockPos(2, 1, 2);

        context.setBlock(waterPos, GCBlocks.ELECTROLYTIC_SEPARATOR);
        context.setBlock(icePos, GCBlocks.ELECTROLYTIC_SEPARATOR);
        context.setBlock(unpoweredPos, GCBlocks.ELECTROLYTIC_SEPARATOR);
        context.setBlock(fracPos, GCBlocks.ATMOSPHERIC_FRACTIONATOR);
        context.setBlock(sabatierPos, GCBlocks.SABATIER_CHEMICAL_REACTOR);

        // Verify FluidStorage.SIDED & EnergyStorage.SIDED lookups for pipes/wires on all 3 new ISRU machines
        for (BlockPos rel : new BlockPos[]{waterPos, fracPos, sabatierPos}) {
            MachineBlockEntity machine = context.getBlockEntity(rel);
            machine.getIOConfig().get(BlockFace.TOP).setOption(ResourceType.FLUID, ResourceFlow.BOTH);
            machine.getIOConfig().get(BlockFace.BOTTOM).setOption(ResourceType.ENERGY, ResourceFlow.BOTH);
            BlockPos abs = context.absolutePos(rel);
            if (FluidStorage.SIDED.find(context.getLevel(), abs, Direction.UP) == null) {
                context.fail("FluidStorage.SIDED missing for ISRU machine at " + rel);
                return;
            }
            if (EnergyStorage.SIDED.find(context.getLevel(), abs, Direction.DOWN) == null) {
                context.fail("EnergyStorage.SIDED missing for ISRU machine at " + rel);
                return;
            }
        }

        ElectrolyticSeparatorBlockEntity waterSep = context.getBlockEntity(waterPos);
        ElectrolyticSeparatorBlockEntity iceSep = context.getBlockEntity(icePos);
        ElectrolyticSeparatorBlockEntity unpoweredSep = context.getBlockEntity(unpoweredPos);

        // 1) Water electrolysis: 200 mB H2O -> 200 mB LH2 + 100 mB LOX (2:1 stoichiometry)
        waterSep.energyStorage().setEnergy(50_000L);
        waterSep.fluidStorage().slot(ElectrolyticSeparatorBlockEntity.WATER_INPUT_TANK)
                .set(Fluids.WATER, 200L * FluidUtil.MILLIBUCKET);

        // 2) Dense Ice electrolysis: 1x DENSE_ICE -> 500 mB LH2 + 250 mB LOX + 25 mB Gases.DEUTERIUM
        iceSep.energyStorage().setEnergy(50_000L);
        iceSep.itemStorage().slot(ElectrolyticSeparatorBlockEntity.SOLID_ICE_SLOT)
                .set(GCBlocks.DENSE_ICE.asItem(), 1);

        // 3) Unpowered rollback safety: 0 energy must consume nothing and emit nothing
        unpoweredSep.energyStorage().setEnergy(0L);
        unpoweredSep.itemStorage().slot(ElectrolyticSeparatorBlockEntity.SOLID_ICE_SLOT)
                .set(GCBlocks.DENSE_ICE.asItem(), 1);

        context.runAfterDelay(50, () -> {
            FluidResourceSlot waterLh2 = waterSep.fluidStorage().slot(ElectrolyticSeparatorBlockEntity.HYDROGEN_OUTPUT_TANK);
            FluidResourceSlot waterLox = waterSep.fluidStorage().slot(ElectrolyticSeparatorBlockEntity.OXYGEN_OUTPUT_TANK);

            if (waterLh2.getResource() != GCFluids.LIQUID_HYDROGEN || waterLh2.getAmount() != 200L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected Water electrolysis to yield 200mB LIQUID_HYDROGEN, got " + waterLh2.getAmount() + " of " + waterLh2.getResource());
                return;
            }
            if (waterLox.getResource() != GCFluids.LIQUID_OXYGEN || waterLox.getAmount() != 100L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected Water electrolysis to yield 100mB LIQUID_OXYGEN, got " + waterLox.getAmount() + " of " + waterLox.getResource());
                return;
            }
            if (waterLh2.getAmount() != waterLox.getAmount() * 2L) {
                context.fail("Expected exact 2:1 LH2:LOX stoichiometric ratio");
                return;
            }

            FluidResourceSlot iceLh2 = iceSep.fluidStorage().slot(ElectrolyticSeparatorBlockEntity.HYDROGEN_OUTPUT_TANK);
            FluidResourceSlot iceLox = iceSep.fluidStorage().slot(ElectrolyticSeparatorBlockEntity.OXYGEN_OUTPUT_TANK);
            FluidResourceSlot iceD2 = iceSep.fluidStorage().slot(ElectrolyticSeparatorBlockEntity.DEUTERIUM_OUTPUT_TANK);

            if (iceLh2.getResource() != GCFluids.LIQUID_HYDROGEN || iceLh2.getAmount() != 500L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected DENSE_ICE electrolysis to yield 500mB LIQUID_HYDROGEN, got " + iceLh2.getAmount());
                return;
            }
            if (iceLox.getResource() != GCFluids.LIQUID_OXYGEN || iceLox.getAmount() != 250L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected DENSE_ICE electrolysis to yield 250mB LIQUID_OXYGEN, got " + iceLox.getAmount());
                return;
            }
            if (iceD2.getResource() != Gases.DEUTERIUM || iceD2.getAmount() != 25L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected DENSE_ICE electrolysis to yield 25mB Gases.DEUTERIUM, got " + iceD2.getAmount() + " of " + iceD2.getResource());
                return;
            }

            if (unpoweredSep.itemStorage().slot(ElectrolyticSeparatorBlockEntity.SOLID_ICE_SLOT).getAmount() != 1
                    || !unpoweredSep.fluidStorage().slot(ElectrolyticSeparatorBlockEntity.HYDROGEN_OUTPUT_TANK).isEmpty()) {
                context.fail("Unpowered Electrolytic Separator must roll back and not consume DENSE_ICE or emit fluid");
                return;
            }

            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void atmosphericFractionatorMultiDimensionTest(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> bodies = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> marsBody = bodies.get(GCCelestialBodies.MARS);
        CelestialBody<?, ?> moonBody = bodies.get(GCCelestialBodies.MOON);
        CelestialBody<?, ?> saturnBody = bodies.get(GCCelestialBodies.SATURN);

        if (marsBody == null || moonBody == null || saturnBody == null) {
            context.fail("Required CelestialBody entries (Mars, Moon, Saturn) missing from CELESTIAL_BODY registry");
            return;
        }

        // Verify Saturn's real registered GasComposition contains He-3 (350 ppm) and Xenon (1.8 ppm)
        double saturnHe3Ppm = saturnBody.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.HELIUM_3_ID), 0.0);
        double saturnXePpm = saturnBody.atmosphere().composition().getOrDefault(ResourceKey.create(Registries.FLUID, Gases.XENON_ID), 0.0);
        if (saturnHe3Ppm < 300.0 || saturnXePpm < 1.0) {
            context.fail("Expected Saturn GasComposition to include HELIUM_3 (>=300 ppm) and XENON (>=1 ppm), got He3=" + saturnHe3Ppm + ", Xe=" + saturnXePpm);
            return;
        }

        BlockPos earthPos = new BlockPos(0, 1, 0);
        BlockPos marsPos = new BlockPos(2, 1, 0);
        BlockPos moonPos = new BlockPos(4, 1, 0);
        BlockPos saturnPos = new BlockPos(6, 1, 0);

        context.setBlock(earthPos, GCBlocks.ATMOSPHERIC_FRACTIONATOR);
        context.setBlock(marsPos, GCBlocks.ATMOSPHERIC_FRACTIONATOR);
        context.setBlock(moonPos, GCBlocks.ATMOSPHERIC_FRACTIONATOR);
        context.setBlock(saturnPos, GCBlocks.ATMOSPHERIC_FRACTIONATOR);

        AtmosphericFractionatorBlockEntity earthFrac = context.getBlockEntity(earthPos);
        AtmosphericFractionatorBlockEntity marsFrac = context.getBlockEntity(marsPos);
        AtmosphericFractionatorBlockEntity moonFrac = context.getBlockEntity(moonPos);
        AtmosphericFractionatorBlockEntity saturnFrac = context.getBlockEntity(saturnPos);

        // Earth runs directly in Overworld with no override; Mars/Moon/Saturn bind their real CelestialBody registry contexts
        marsFrac.setAtmosphereContextForTest(GCDimensions.MARS, marsBody);
        moonFrac.setAtmosphereContextForTest(GCDimensions.MOON, moonBody);
        saturnFrac.setAtmosphereContextForTest(GCDimensions.SATURN, saturnBody);

        earthFrac.energyStorage().setEnergy(50_000L);
        marsFrac.energyStorage().setEnergy(50_000L);
        moonFrac.energyStorage().setEnergy(50_000L);
        saturnFrac.energyStorage().setEnergy(50_000L);

        context.runAfterDelay(30, () -> {
            // 1) Overworld: N2 + O2
            FluidResourceSlot earthT0 = earthFrac.fluidStorage().slot(AtmosphericFractionatorBlockEntity.OUTPUT_TANK_0);
            FluidResourceSlot earthT1 = earthFrac.fluidStorage().slot(AtmosphericFractionatorBlockEntity.OUTPUT_TANK_1);
            if (earthT0.getResource() != Gases.NITROGEN || earthT0.isEmpty()) {
                context.fail("Expected Overworld Fractionator tank 0 to extract NITROGEN, got " + earthT0.getResource() + " (" + earthT0.getAmount() + ")");
                return;
            }
            if (earthT1.getResource() != Gases.OXYGEN || earthT1.isEmpty()) {
                context.fail("Expected Overworld Fractionator tank 1 to extract OXYGEN, got " + earthT1.getResource() + " (" + earthT1.getAmount() + ")");
                return;
            }

            // 2) Mars: CO2 (dominant 95%+) + N2
            FluidResourceSlot marsT0 = marsFrac.fluidStorage().slot(AtmosphericFractionatorBlockEntity.OUTPUT_TANK_0);
            FluidResourceSlot marsT1 = marsFrac.fluidStorage().slot(AtmosphericFractionatorBlockEntity.OUTPUT_TANK_1);
            if (marsT0.getResource() != Gases.CARBON_DIOXIDE || marsT0.isEmpty()) {
                context.fail("Expected Mars Fractionator tank 0 to extract CARBON_DIOXIDE, got " + marsT0.getResource() + " (" + marsT0.getAmount() + ")");
                return;
            }
            if (marsT1.getResource() != Gases.NITROGEN || marsT1.isEmpty()) {
                context.fail("Expected Mars Fractionator tank 1 to extract NITROGEN, got " + marsT1.getResource());
                return;
            }

            // 3) Moon (Vacuum): NO_ATMOSPHERE and 0 gas
            if (moonFrac.getState().getStatus() != GCMachineStatuses.NO_ATMOSPHERE) {
                context.fail("Expected Moon Fractionator status NO_ATMOSPHERE, got " + moonFrac.getState().getStatus());
                return;
            }
            for (int t = 0; t < 4; t++) {
                if (!moonFrac.fluidStorage().slot(t).isEmpty()) {
                    context.fail("Expected Moon Vacuum Fractionator tank " + t + " to remain empty");
                    return;
                }
            }

            // 4) Saturn (Gas Giant, 3.5x He-3 peak + Xenon)
            FluidResourceSlot saturnH2 = saturnFrac.fluidStorage().slot(AtmosphericFractionatorBlockEntity.OUTPUT_TANK_0);
            FluidResourceSlot saturnHe3 = saturnFrac.fluidStorage().slot(AtmosphericFractionatorBlockEntity.OUTPUT_TANK_1);
            FluidResourceSlot saturnXe = saturnFrac.fluidStorage().slot(AtmosphericFractionatorBlockEntity.OUTPUT_TANK_3);
            if (saturnH2.getResource() != Gases.HYDROGEN || saturnH2.isEmpty()) {
                context.fail("Expected Saturn Fractionator tank 0 to extract HYDROGEN, got " + saturnH2.getResource());
                return;
            }
            if (saturnHe3.getResource() != Gases.HELIUM_3 || saturnHe3.getAmount() < 35L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected Saturn Fractionator tank 1 to extract 3.5x enriched HELIUM_3 (>=35mB/cycle), got " + saturnHe3.getAmount() + " of " + saturnHe3.getResource());
                return;
            }
            if (saturnXe.getResource() != Gases.XENON || saturnXe.isEmpty()) {
                context.fail("Expected Saturn Fractionator tank 3 to extract trace XENON, got " + saturnXe.getResource());
                return;
            }

            context.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void marsFullIsruSabatierChainTest(GameTestHelper context) {
        Registry<CelestialBody<?, ?>> bodies = context.getLevel().registryAccess().registryOrThrow(AddonRegistries.CELESTIAL_BODY);
        CelestialBody<?, ?> marsBody = bodies.get(GCCelestialBodies.MARS);
        if (marsBody == null) {
            context.fail("Mars CelestialBody missing from registry");
            return;
        }

        // Stage 1: Electrolytic Separator (DENSE_ICE -> LH2) + Mars Atmospheric Fractionator (CO2)
        BlockPos fracPos = new BlockPos(0, 1, 2);
        BlockPos sepPos = new BlockPos(0, 1, 0);
        BlockPos sabNbPos = new BlockPos(2, 1, 0);
        BlockPos sabPdPos = new BlockPos(4, 1, 0);
        BlockPos sabNoCatPos = new BlockPos(6, 1, 0);
        BlockPos sabHydroPos = new BlockPos(2, 1, 2);
        BlockPos sabAcidPos = new BlockPos(4, 1, 2);

        context.setBlock(fracPos, GCBlocks.ATMOSPHERIC_FRACTIONATOR);
        context.setBlock(sepPos, GCBlocks.ELECTROLYTIC_SEPARATOR);
        context.setBlock(sabNbPos, GCBlocks.SABATIER_CHEMICAL_REACTOR);
        context.setBlock(sabPdPos, GCBlocks.SABATIER_CHEMICAL_REACTOR);
        context.setBlock(sabNoCatPos, GCBlocks.SABATIER_CHEMICAL_REACTOR);
        context.setBlock(sabHydroPos, GCBlocks.SABATIER_CHEMICAL_REACTOR);
        context.setBlock(sabAcidPos, GCBlocks.SABATIER_CHEMICAL_REACTOR);

        AtmosphericFractionatorBlockEntity marsFrac = context.getBlockEntity(fracPos);
        marsFrac.setAtmosphereContextForTest(GCDimensions.MARS, marsBody);
        marsFrac.energyStorage().setEnergy(50_000L);

        ElectrolyticSeparatorBlockEntity separator = context.getBlockEntity(sepPos);
        separator.energyStorage().setEnergy(50_000L);
        separator.itemStorage().slot(ElectrolyticSeparatorBlockEntity.SOLID_ICE_SLOT).set(GCBlocks.DENSE_ICE.asItem(), 2);

        // Wait 85 ticks for Dense Ice electrolysis (>=800mB LH2) and Mars Fractionator (>=240mB CO2), then feed Sabatier reactors
        context.runAfterDelay(85, () -> {
            FluidResourceSlot lh2Slot = separator.fluidStorage().slot(ElectrolyticSeparatorBlockEntity.HYDROGEN_OUTPUT_TANK);
            FluidResourceSlot marsCo2Slot = marsFrac.fluidStorage().slot(AtmosphericFractionatorBlockEntity.OUTPUT_TANK_0);

            if (marsCo2Slot.getResource() != Gases.CARBON_DIOXIDE || marsCo2Slot.getAmount() < 200L * FluidUtil.MILLIBUCKET) {
                context.fail("Mars Atmospheric Fractionator failed to harvest >=200mB CO2, got " + marsCo2Slot.getAmount());
                return;
            }
            if (lh2Slot.getResource() != GCFluids.LIQUID_HYDROGEN || lh2Slot.getAmount() < 800L * FluidUtil.MILLIBUCKET) {
                context.fail("Expected Dense Ice electrolysis to yield >=800mB LH2, got " + lh2Slot.getAmount());
                return;
            }

            SabatierChemicalReactorBlockEntity sabNb = context.getBlockEntity(sabNbPos);
            SabatierChemicalReactorBlockEntity sabPd = context.getBlockEntity(sabPdPos);
            SabatierChemicalReactorBlockEntity sabNoCat = context.getBlockEntity(sabNoCatPos);
            SabatierChemicalReactorBlockEntity sabHydro = context.getBlockEntity(sabHydroPos);
            SabatierChemicalReactorBlockEntity sabAcid = context.getBlockEntity(sabAcidPos);

            long co2Charge = marsCo2Slot.getAmount();

            // Niobium 1.0x catalyst Sabatier (pre-warmed bed)
            sabNb.energyStorage().setEnergy(50_000L);
            sabNb.setTemperatureC(360);
            sabNb.itemStorage().slot(SabatierChemicalReactorBlockEntity.CATALYST_SLOT).set(GCItems.NIOBIUM_INGOT, 1);
            sabNb.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_A_TANK).set(Gases.CARBON_DIOXIDE, co2Charge);
            sabNb.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_B_TANK).set(GCFluids.LIQUID_HYDROGEN, 800L * FluidUtil.MILLIBUCKET);

            // Palladium 1.5x supercritical catalyst Sabatier (order-swapped tanks to verify order-independence!)
            sabPd.energyStorage().setEnergy(50_000L);
            sabPd.setTemperatureC(360);
            sabPd.itemStorage().slot(SabatierChemicalReactorBlockEntity.CATALYST_SLOT).set(GCItems.PALLADIUM_INGOT, 1);
            sabPd.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_A_TANK).set(GCFluids.LIQUID_HYDROGEN, 800L * FluidUtil.MILLIBUCKET);
            sabPd.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_B_TANK).set(Gases.CARBON_DIOXIDE, co2Charge);

            // No-catalyst reactor must report MISSING_CATALYST and synthesize nothing
            sabNoCat.energyStorage().setEnergy(50_000L);
            sabNoCat.setTemperatureC(360);
            sabNoCat.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_A_TANK).set(Gases.CARBON_DIOXIDE, co2Charge);
            sabNoCat.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_B_TANK).set(GCFluids.LIQUID_HYDROGEN, 800L * FluidUtil.MILLIBUCKET);

            // Hydrazine Monopropellant (N2 + 2 LH2 -> MONOPROPELLANT)
            sabHydro.energyStorage().setEnergy(50_000L);
            sabHydro.setTemperatureC(430);
            sabHydro.itemStorage().slot(SabatierChemicalReactorBlockEntity.CATALYST_SLOT).set(GCItems.PALLADIUM_INGOT, 1);
            sabHydro.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_A_TANK).set(Gases.NITROGEN, 200L * FluidUtil.MILLIBUCKET);
            sabHydro.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_B_TANK).set(GCFluids.LIQUID_HYDROGEN, 400L * FluidUtil.MILLIBUCKET);

            // Contact Sulfuric Acid (MERCURY_SULFIDE_ORE + WATER -> SULFURIC_ACID)
            sabAcid.energyStorage().setEnergy(50_000L);
            sabAcid.setTemperatureC(310);
            sabAcid.itemStorage().slot(SabatierChemicalReactorBlockEntity.CATALYST_SLOT).set(GCItems.NIOBIUM_INGOT, 1);
            sabAcid.itemStorage().slot(SabatierChemicalReactorBlockEntity.SOLID_REAGENT_SLOT).set(GCBlocks.MERCURY_SULFIDE_ORE.asItem(), 1);
            sabAcid.fluidStorage().slot(SabatierChemicalReactorBlockEntity.REAGENT_A_TANK).set(Fluids.WATER, 500L * FluidUtil.MILLIBUCKET);

            // Wait 60 ticks: Pd completes 2 cycles (27t * 2 = 54t) while Nb completes 1 cycle (40t)
            context.runAfterDelay(60, () -> {
                FluidResourceSlot nbCh4 = sabNb.fluidStorage().slot(SabatierChemicalReactorBlockEntity.PRODUCT_A_TANK);
                FluidResourceSlot nbH2o = sabNb.fluidStorage().slot(SabatierChemicalReactorBlockEntity.PRODUCT_B_TANK);
                FluidResourceSlot pdCh4 = sabPd.fluidStorage().slot(SabatierChemicalReactorBlockEntity.PRODUCT_A_TANK);

                if (nbCh4.getResource() != GCFluids.LIQUID_METHANE || nbCh4.getAmount() < 100L * FluidUtil.MILLIBUCKET) {
                    context.fail("Expected Niobium Sabatier reactor to produce >=100mB LIQUID_METHANE, got " + nbCh4.getAmount() + " of " + nbCh4.getResource());
                    return;
                }
                if (nbH2o.getResource() != Fluids.WATER || nbH2o.getAmount() < 200L * FluidUtil.MILLIBUCKET) {
                    context.fail("Expected Sabatier reactor to recycle >=200mB WATER, got " + nbH2o.getAmount() + " of " + nbH2o.getResource());
                    return;
                }
                if (pdCh4.getResource() != GCFluids.LIQUID_METHANE || pdCh4.getAmount() <= nbCh4.getAmount()) {
                    context.fail("Expected Palladium 1.5x catalyst Sabatier to outproduce Niobium (" + pdCh4.getAmount() + " vs " + nbCh4.getAmount() + ")");
                    return;
                }
                if (sabNb.itemStorage().slot(SabatierChemicalReactorBlockEntity.CATALYST_SLOT).isEmpty()
                        || sabPd.itemStorage().slot(SabatierChemicalReactorBlockEntity.CATALYST_SLOT).isEmpty()) {
                    context.fail("Catalyst ingot (Niobium/Palladium) must NOT be consumed by Sabatier reactions");
                    return;
                }
                if (!sabNoCat.fluidStorage().slot(SabatierChemicalReactorBlockEntity.PRODUCT_A_TANK).isEmpty()) {
                    context.fail("Sabatier Reactor without catalyst must NOT synthesize methane");
                    return;
                }

                FluidResourceSlot hydroOut = sabHydro.fluidStorage().slot(SabatierChemicalReactorBlockEntity.PRODUCT_A_TANK);
                if (hydroOut.getResource() != GCFluids.MONOPROPELLANT || hydroOut.isEmpty()) {
                    context.fail("Expected Hydrazine synthesis to produce MONOPROPELLANT, got " + hydroOut.getResource());
                    return;
                }

                FluidResourceSlot acidOut = sabAcid.fluidStorage().slot(SabatierChemicalReactorBlockEntity.PRODUCT_A_TANK);
                if (acidOut.getResource() != GCFluids.SULFURIC_ACID || acidOut.isEmpty()) {
                    context.fail("Expected Mercury Sulfide + Water synthesis to produce SULFURIC_ACID, got " + acidOut.getResource());
                    return;
                }

                context.succeed();
            });
        });
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
    public void isotopicCentrifugeRadioisotopeAndScrapTest(GameTestHelper context) {
        BlockPos sulfidePos = new BlockPos(0, 1, 0);
        BlockPos uraniumPos = new BlockPos(2, 1, 0);
        BlockPos scrapPos = new BlockPos(4, 1, 0);

        context.setBlock(sulfidePos, GCBlocks.ISOTOPIC_CENTRIFUGE);
        context.setBlock(uraniumPos, GCBlocks.ISOTOPIC_CENTRIFUGE);
        context.setBlock(scrapPos, GCBlocks.ISOTOPIC_CENTRIFUGE);

        IsotopicCentrifugeBlockEntity sulfideCent = context.getBlockEntity(sulfidePos);
        IsotopicCentrifugeBlockEntity uraniumCent = context.getBlockEntity(uraniumPos);
        IsotopicCentrifugeBlockEntity scrapCent = context.getBlockEntity(scrapPos);

        sulfideCent.setRotorRpm(IsotopicCentrifugeBlockEntity.TARGET_RPM);
        sulfideCent.energyStorage().setEnergy(50_000L);
        sulfideCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        sulfideCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.SOLID_INPUT_SLOT).set(GCBlocks.MERCURY_SULFIDE_ORE.asItem(), 1);

        uraniumCent.setRotorRpm(IsotopicCentrifugeBlockEntity.TARGET_RPM);
        uraniumCent.energyStorage().setEnergy(50_000L);
        uraniumCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        uraniumCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.SOLID_INPUT_SLOT).set(GCItems.URANIUM_INGOT, 1);

        scrapCent.setRotorRpm(IsotopicCentrifugeBlockEntity.TARGET_RPM);
        scrapCent.energyStorage().setEnergy(50_000L);
        scrapCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.COIL_SLOT).set(GCItems.SUPERCONDUCTING_COIL, 1);
        scrapCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.SOLID_INPUT_SLOT).set(GCItems.TIN_CANISTER, 1);

        context.runAfterDelay(90, () -> {
            ItemResourceSlot sulfideOut = sulfideCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.BYPRODUCT_SLOT);
            ItemResourceSlot uraniumOut = uraniumCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.BYPRODUCT_SLOT);
            ItemResourceSlot scrapOut = scrapCent.itemStorage().slot(IsotopicCentrifugeBlockEntity.BYPRODUCT_SLOT);

            if (sulfideOut.getResource() != GCItems.RADIOISOTOPE_CORE || sulfideOut.getAmount() != 1) {
                context.fail("Expected MERCURY_SULFIDE_ORE in Isotopic Centrifuge to yield 1x RADIOISOTOPE_CORE, got " + sulfideOut.getAmount() + "x " + sulfideOut.getResource());
                return;
            }
            if (uraniumOut.getResource() != GCItems.RADIOISOTOPE_CORE || uraniumOut.getAmount() != 2) {
                context.fail("Expected URANIUM_INGOT in Isotopic Centrifuge to yield 2x RADIOISOTOPE_CORE, got " + uraniumOut.getAmount() + "x " + uraniumOut.getResource());
                return;
            }
            if (scrapOut.getResource() != GCItems.TIN_INGOT || scrapOut.getAmount() != 2) {
                context.fail("Expected TIN_CANISTER scrap recovery in Isotopic Centrifuge to yield 2x TIN_INGOT, got " + scrapOut.getAmount() + "x " + scrapOut.getResource());
                return;
            }
            context.succeed();
        });
    }
}
