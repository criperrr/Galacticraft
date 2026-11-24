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

package dev.galacticraft.mod.content.rocket.physics;

import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.part.data.PartDamageState;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition.SlotMaterialEntry;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.content.block.entity.machine.AeroRefurbishmentBenchBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import dev.galacticraft.mod.content.rocket.part.RocketPartSpec;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator.FlightValidationReport;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator.HazardCode;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator.PartSnapshot;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator.VehicleSnapshot;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Emergent Rocket Physics ('Simulated Tiers'), Cryo Boil-Off & Refurbishment Bench")
class RocketEmergentPhysicsTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCBlocks.register();
        GCItems.register();
        GCFluids.register();
    }

    private static RocketPartConfig configFor(String modelId, RocketPartCategory fallbackCategory) {
        RocketPartSpec spec = RocketPartCatalog.getInstance().getPart(modelId);
        if (spec != null) {
            return spec.toConfig(RocketLivery.WHITE);
        }
        return RocketPartCatalog.getInstance().defaultConfigFor(fallbackCategory);
    }

    private static PartSnapshot makePart(
            RocketPartConfig config,
            RocketPartComposition composition
    ) {
        RocketPartCostCalculator.CostProfile profile = RocketPartCostCalculator.calculate(config, composition);
        return new PartSnapshot(
                config,
                profile.stats(),
                PartIntegrity.pristine(profile.maxIntegrity()),
                composition,
                profile.boilOffMultiplier()
        );
    }

    private static RocketPartComposition hullComposition(Item hullItem, RocketMaterialTrait trait) {
        var id = BuiltInRegistries.ITEM.getKey(hullItem);
        return new RocketPartComposition(List.of(
                SlotMaterialEntry.of(3, SlotRole.STRUCTURAL_HULL, id, List.of(trait)),
                SlotMaterialEntry.of(5, SlotRole.STRUCTURAL_HULL, id, List.of(trait))
        ));
    }

    @Test
    @DisplayName("Heavy steel RP-1 rocket fails T/W on Jupiter (2.36G), while Magnesium-Lithium + Starcluster passes")
    void heavyRp1FailsJupiterWhileMagnesiumStarclusterPasses() {
        // Heavy steel rocket with small RP-1 engine
        RocketPartConfig heavyCap = configFor("capsule_orion_5x5_3b", RocketPartCategory.COMMAND);
        RocketPartConfig heavyTank = configFor("tank_methalox_bulkhead_5x5_3b", RocketPartCategory.TANK);
        RocketPartConfig smallRp1Engine = configFor("engine_falcon1d_3x3_2b", RocketPartCategory.ENGINE);
        RocketPartComposition steelComp = hullComposition(GCItems.COMPRESSED_STEEL, RocketMaterialTrait.BRUTE_RIGIDITY);

        VehicleSnapshot heavyRp1 = new VehicleSnapshot(
                List.of(
                        makePart(heavyCap, steelComp),
                        makePart(heavyTank, steelComp),
                        makePart(heavyTank, steelComp),
                        makePart(smallRp1Engine, steelComp)
                ),
                0L,
                10_000L,
                GCFluids.FUEL
        );

        FlightValidationReport jupiterFailReport = RocketFlightValidator.evaluate(
                heavyRp1, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.JUPITER
        );
        assertFalse(jupiterFailReport.canLiftoffDestination(),
                "Heavy steel RP-1 rocket must fail T/W on Jupiter (2.36G), got T/W=" + jupiterFailReport.destinationTwr());
        assertTrue(jupiterFailReport.destinationTwr() <= 1.0);
        assertTrue(jupiterFailReport.hasHazard(HazardCode.LOW_DESTINATION_TWR));

        // Ultralight Magnesium-Lithium hull + high-thrust Starcluster engine with Palladium catalysis
        RocketPartConfig starcluster = configFor("engine_starcluster33_9x9_3b", RocketPartCategory.ENGINE);
        RocketPartConfig methaloxTank = configFor("tank_methalox_bulkhead_5x5_3b", RocketPartCategory.TANK);
        RocketPartComposition magComp = hullComposition(GCItems.MAGNESIUM_INGOT, RocketMaterialTrait.ULTRALIGHT_SUPERFRAME);
        RocketPartComposition starEngineComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(3, SlotRole.ENGINE_TURBOPUMP,
                        BuiltInRegistries.ITEM.getKey(GCItems.FLUID_MANIPULATOR),
                        List.of(RocketMaterialTrait.STAGED_COMBUSTION)),
                SlotMaterialEntry.of(4, SlotRole.ENGINE_CHAMBER,
                        BuiltInRegistries.ITEM.getKey(GCItems.HELIUM3_CELL),
                        List.of(RocketMaterialTrait.ANEUTRONIC_HE3_PLASMA)),
                SlotMaterialEntry.of(6, SlotRole.STRUCTURAL_HULL,
                        BuiltInRegistries.ITEM.getKey(GCItems.PALLADIUM_INGOT),
                        List.of(RocketMaterialTrait.SUPERCRITICAL_CATALYSIS)),
                SlotMaterialEntry.of(8, SlotRole.STRUCTURAL_HULL,
                        BuiltInRegistries.ITEM.getKey(GCItems.PALLADIUM_INGOT),
                        List.of(RocketMaterialTrait.SUPERCRITICAL_CATALYSIS))
        ));

        VehicleSnapshot magStarcluster = new VehicleSnapshot(
                List.of(
                        makePart(heavyCap, magComp),
                        makePart(methaloxTank, magComp),
                        makePart(starcluster, starEngineComp)
                ),
                0L,
                25_000L,
                GCFluids.LIQUID_METHANE
        );

        FlightValidationReport jupiterPassReport = RocketFlightValidator.evaluate(
                magStarcluster, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.JUPITER
        );
        assertTrue(jupiterPassReport.canLiftoffDestination(),
                "Magnesium-Lithium + Starcluster must pass T/W > 1.0 on Jupiter (2.36G), got T/W=" + jupiterPassReport.destinationTwr());
        assertTrue(jupiterPassReport.destinationTwr() > 1.0);
        assertTrue(jupiterPassReport.dfdBurnPowerGenPerTick() > 0L,
                "DFD He-3 engine must generate positive electrical power during burn");
    }

    @Test
    @DisplayName("Isp hierarchy scales physically RP-1 < Methalox < Hydrolox < NERVA < Hall/VASIMR and DFD adds +40% Isp")
    void ispHierarchyAndDfdBoost() {
        double rp1 = RocketFlightValidator.computeEngineIsp(PropellantType.RP1, null);
        double methalox = RocketFlightValidator.computeEngineIsp(PropellantType.METHALOX, null);
        double hydrolox = RocketFlightValidator.computeEngineIsp(PropellantType.HYDROLOX, null);
        double nerva = RocketFlightValidator.computeEngineIsp(PropellantType.NERVA, null);
        double vasimr = RocketFlightValidator.computeEngineIsp(PropellantType.XENON_ELECTRIC, null);

        assertTrue(rp1 < methalox && methalox < hydrolox && hydrolox < nerva && nerva < vasimr);

        RocketPartComposition dfdComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(4, SlotRole.ENGINE_CHAMBER,
                        BuiltInRegistries.ITEM.getKey(GCItems.HELIUM3_CELL),
                        List.of(RocketMaterialTrait.ANEUTRONIC_HE3_PLASMA))
        ));
        double nervaDfd = RocketFlightValidator.computeEngineIsp(PropellantType.NERVA, dfdComp);
        assertEquals(nerva * 1.40, nervaDfd, 0.01);
    }

    @Test
    @DisplayName("Venus (92 atm, 735 K, H2SO4) degrades unprotected aluminum parts, while Refurbishment Bench restores 100% and stamps protective coatings")
    void venusWearAndRefurbishmentBenchRepairAndCoat() {
        RocketPartConfig capsuleCfg = configFor("capsule_vostok_1x1_1b", RocketPartCategory.COMMAND);
        RocketPartComposition aluComp = hullComposition(GCItems.COMPRESSED_ALUMINUM, RocketMaterialTrait.LIGHTWEIGHT_ALLOY);
        RocketPartCostCalculator.CostProfile aluProfile = RocketPartCostCalculator.calculate(capsuleCfg, aluComp);

        ItemStack capsuleStack = ModularRocketPartBlockItem.createConfiguredStack(
                capsuleCfg,
                aluProfile.stats(),
                PartIntegrity.pristine(aluProfile.maxIntegrity()),
                aluComp
        );

        // Unprotected aluminum exposed to Venus suffers pressure + acid wear
        PartIntegrity damaged = RocketFlightValidator.applyEnvironmentalWearToStack(
                capsuleStack, PlanetaryEnvironment.VENUS, 12.0
        );
        assertTrue(damaged.fraction() < 0.85,
                "Expected significant Venus wear on unprotected aluminum capsule, got fraction=" + damaged.fraction());

        // Refurbish + stamp Anti-Acid Lead cladding
        AeroRefurbishmentBenchBlockEntity.RefurbishPlan leadPlan =
                AeroRefurbishmentBenchBlockEntity.evaluatePlan(capsuleStack, new ItemStack(GCItems.LEAD_INGOT));
        assertNotNull(leadPlan, "Lead ingot must both repair and stamp anti-acid cladding");
        assertEquals(AeroRefurbishmentBenchBlockEntity.MODE_REPAIR_AND_COAT, leadPlan.mode());

        ItemStack leadCoated = leadPlan.outputStack();
        PartIntegrity restored = leadCoated.get(GCDataComponents.PART_INTEGRITY);
        RocketPartComposition leadComp = leadCoated.get(GCDataComponents.ROCKET_PART_COMPOSITION);
        assertNotNull(restored);
        assertNotNull(leadComp);
        assertEquals(1.0, restored.fraction(), 1e-6);
        assertEquals(PartDamageState.NOMINAL, restored.damageState());
        assertTrue(leadComp.hasTrait(RocketMaterialTrait.ACID_RADIATION_SHIELD),
                "Lead coating must stamp ACID_RADIATION_SHIELD into RocketPartComposition");

        // Second identical coating on 100% pristine part must be rejected (no wasted materials)
        assertNull(AeroRefurbishmentBenchBlockEntity.evaluatePlan(leadCoated, new ItemStack(GCItems.LEAD_INGOT)));

        // Stamp Lonsdaleite high-pressure plating + Olivine TPS thermal coating
        AeroRefurbishmentBenchBlockEntity.RefurbishPlan lonsdaleitePlan =
                AeroRefurbishmentBenchBlockEntity.evaluatePlan(leadCoated, new ItemStack(GCItems.LONSDALEITE_GEM));
        assertNotNull(lonsdaleitePlan);
        ItemStack venusHardened = lonsdaleitePlan.outputStack();
        RocketPartStats hardenedStats = venusHardened.get(GCDataComponents.ROCKET_PART_STATS);
        assertNotNull(hardenedStats);
        assertTrue(hardenedStats.pressureToleranceAtm() > aluProfile.stats().pressureToleranceAtm() * 2.0,
                "Lonsdaleite plating must boost pressure tolerance dramatically");

        // Verify Venus acid hazard is now eliminated
        VehicleSnapshot hardenedVehicle = VehicleSnapshot.fromStacks(List.of(venusHardened), 0L, 10_000L, null);
        FlightValidationReport venusReport = RocketFlightValidator.evaluate(
                hardenedVehicle, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.VENUS
        );
        assertTrue(venusReport.acidProtected());
        assertFalse(venusReport.acidHazard());
    }

    @Test
    @DisplayName("Solar 1/r^2 decays beyond Mars; RTG and D-3He onboard reactor sustain outer-planet power")
    void solarInverseSquareDecayAndOuterPlanetPower() {
        assertTrue(PlanetaryEnvironment.MERCURY.solarIrradianceFactor() > 5.0);
        assertEquals(1.0, PlanetaryEnvironment.EARTH.solarIrradianceFactor(), 1e-6);
        assertEquals(1.0, PlanetaryEnvironment.MOON.solarIrradianceFactor(), 1e-6);
        assertTrue(PlanetaryEnvironment.MARS.solarIrradianceFactor() < 0.50);
        assertTrue(PlanetaryEnvironment.JUPITER.solarIrradianceFactor() < 0.05);
        assertTrue(PlanetaryEnvironment.NEPTUNE.solarIrradianceFactor() < 0.002);

        RocketPartConfig solarCfg = configFor("power_solar_array_3x3_1b", RocketPartCategory.UTILITY);
        RocketPartConfig capCfg = configFor("capsule_vostok_1x1_1b", RocketPartCategory.COMMAND);

        VehicleSnapshot solarOnly = new VehicleSnapshot(
                List.of(makePart(capCfg, null), makePart(solarCfg, null)),
                0L, 0L, null
        );
        FlightValidationReport jupiterSolar = RocketFlightValidator.evaluate(
                solarOnly, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.JUPITER
        );
        assertTrue(jupiterSolar.powerDeficit(), "Solar-only vehicle must suffer power deficit at Jupiter (0.037x)");
        assertTrue(jupiterSolar.hasHazard(HazardCode.LOW_SOLAR_POWER));

        // Add D-3He onboard reactor trait via HELIUM3_FUSION_CELL
        RocketPartComposition he3Avionics = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(7, SlotRole.COMMAND_AVIONICS,
                        BuiltInRegistries.ITEM.getKey(GCItems.HELIUM3_FUSION_CELL),
                        List.of(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR))
        ));
        VehicleSnapshot he3Vehicle = new VehicleSnapshot(
                List.of(makePart(capCfg, he3Avionics), makePart(solarCfg, null)),
                0L, 0L, null
        );
        FlightValidationReport neptuneHe3 = RocketFlightValidator.evaluate(
                he3Vehicle, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.NEPTUNE
        );
        assertFalse(neptuneHe3.powerDeficit(), "Onboard D-3He reactor must keep net power positive even at Neptune");
        assertTrue(neptuneHe3.passivePowerGenPerTick() >= 120L);
    }

    @Test
    @DisplayName("Cryogenic LH2 boil-off: uninsulated loses fuel, SOFI <= 0.15x, powered cryocooler = 0.0x, He-3 <2K = 0.0x with zero power drain")
    void cryogenicBoilOffModes() {
        RocketPartConfig tankCfg = configFor("tank_hydrolox_supercryo_7x7_2b", RocketPartCategory.TANK);
        RocketPartConfig cryocoolerCfg = configFor("tank_upgrade_cryocooler_3x3_1b", RocketPartCategory.TANK);
        long storedLh2 = 81_000L * 20L; // 20 buckets LH2

        // 1. Bare uninsulated tank
        PartSnapshot bareTank = makePart(tankCfg, null);
        double bareRate = RocketFlightValidator.computeVehicleBoilOffDropletsPerMinute(
                List.of(bareTank), GCFluids.LIQUID_HYDROGEN, PropellantType.HYDROLOX,
                storedLh2, 300.0, false, false, false
        );
        assertTrue(bareRate > 10_000.0, "Bare LH2 tank at 300K must boil off noticeably, got " + bareRate);

        // 2. SOFI insulated tank (<= 0.15x)
        RocketPartComposition sofiComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(9, SlotRole.PROTECTIVE_COATING,
                        BuiltInRegistries.ITEM.getKey(GCItems.SOFI_INSULATION),
                        List.of(RocketMaterialTrait.SOFI_INSULATION))
        ));
        PartSnapshot sofiTank = makePart(tankCfg, sofiComp);
        double sofiRate = RocketFlightValidator.computeVehicleBoilOffDropletsPerMinute(
                List.of(sofiTank), GCFluids.LIQUID_HYDROGEN, PropellantType.HYDROLOX,
                storedLh2, 300.0, false, false, false
        );
        assertTrue(sofiRate > 0.0 && sofiRate <= bareRate * 0.16,
                "SOFI insulation must cut boil-off to <= 0.15x, got " + sofiRate + " vs bare " + bareRate);

        // 3. Powered tank_upgrade_cryocooler -> 0.0x
        PartSnapshot coolerPart = makePart(cryocoolerCfg, null);
        double poweredCryoRate = RocketFlightValidator.computeVehicleBoilOffDropletsPerMinute(
                List.of(bareTank, coolerPart), GCFluids.LIQUID_HYDROGEN, PropellantType.HYDROLOX,
                storedLh2, 300.0, true, true, false
        );
        assertEquals(0.0, poweredCryoRate, 1e-6, "Powered cryocooler must zero LH2 boil-off");

        // Unpowered cryocooler does NOT stop boil-off
        double unpoweredCryoRate = RocketFlightValidator.computeVehicleBoilOffDropletsPerMinute(
                List.of(bareTank, coolerPart), GCFluids.LIQUID_HYDROGEN, PropellantType.HYDROLOX,
                storedLh2, 300.0, true, false, false
        );
        assertTrue(unpoweredCryoRate > 0.0, "Unpowered cryocooler must still allow LH2 boil-off");

        // 4. Superfluid He-3 <2K -> 0.0x with ZERO electrical power required
        RocketPartComposition he3CryoComp = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(3, SlotRole.TANK_PROPELLANT_CELL,
                        BuiltInRegistries.ITEM.getKey(GCItems.HELIUM3_CELL),
                        List.of(RocketMaterialTrait.SUPERFLUID_CRYO_2K))
        ));
        VehicleSnapshot he3CryoVehicle = new VehicleSnapshot(
                List.of(makePart(tankCfg, he3CryoComp)),
                storedLh2,
                0L, // zero stored electrical energy
                GCFluids.LIQUID_HYDROGEN
        );
        FlightValidationReport cryoReport = RocketFlightValidator.evaluate(
                he3CryoVehicle, PlanetaryEnvironment.EARTH, PlanetaryEnvironment.MARS
        );
        assertTrue(cryoReport.superfluidCryo2kActive());
        assertTrue(cryoReport.zeroBoilOffActive());
        assertEquals(0.0, cryoReport.boilOffDropletsPerMinute(), 1e-6);
        assertEquals(RocketFlightValidator.BASE_AVIONICS_DRAW_GJ_PER_TICK, cryoReport.powerDrawPerTick(),
                "Superfluid <2K cooling must NOT add continuous cryocooler electrical draw");
    }
}
