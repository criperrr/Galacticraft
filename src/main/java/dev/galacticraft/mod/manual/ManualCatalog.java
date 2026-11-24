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

package dev.galacticraft.mod.manual;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCCelestialBodies;
import dev.galacticraft.mod.research.ResearchCatalog;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical built-in entries for the Galacticraft 5 Aerospace Manual, exported as Codec JSON files
 * by {@code GCManualProvider} into {@code data/galacticraft/manual_entries/<tab>/<id>.json}.
 */
public final class ManualCatalog {
    public static final ResourceLocation ENTRY_MISSION_OVERVIEW = Constant.id("mission/getting_started");
    public static final ResourceLocation ENTRY_LAUNCH_CHECKLIST_MOON = Constant.id("mission/lunar_checklist");
    public static final ResourceLocation ENTRY_LAUNCH_CHECKLIST_MARS = Constant.id("mission/mars_checklist");
    public static final ResourceLocation ENTRY_LAUNCH_CHECKLIST_VENUS = Constant.id("mission/venus_checklist");

    public static final ResourceLocation ENTRY_MATERIALS_SIMULATOR = Constant.id("materials/flight_simulator");
    public static final ResourceLocation ENTRY_MATERIALS_EARLY_ALLOYS = Constant.id("materials/early_alloys");
    public static final ResourceLocation ENTRY_MATERIALS_ADVANCED_COMPOSITES = Constant.id("materials/advanced_composites");
    public static final ResourceLocation ENTRY_MATERIALS_EXOTIC_OUTER = Constant.id("materials/exotic_outer_alloys");

    public static final ResourceLocation ENTRY_MULTIBLOCK_LAUNCH_PAD = Constant.id("machines/launch_pad_3d");
    public static final ResourceLocation ENTRY_MULTIBLOCK_AIRLOCK = Constant.id("machines/airlock_3d");
    public static final ResourceLocation ENTRY_MULTIBLOCK_SOLAR = Constant.id("machines/solar_panel_3d");
    public static final ResourceLocation ENTRY_MULTIBLOCK_CRYO = Constant.id("machines/cryogenic_chamber_3d");
    public static final ResourceLocation ENTRY_CHAIN_NIOBIUM = Constant.id("machines/niobium_metallurgy");
    public static final ResourceLocation ENTRY_CHAIN_SABATIER = Constant.id("machines/sabatier_isru");
    public static final ResourceLocation ENTRY_CHAIN_ACID_LEACHING = Constant.id("machines/venus_acid_leaching");
    public static final ResourceLocation ENTRY_CHAIN_HELIUM3_FUSION = Constant.id("machines/helium3_fusion");
    public static final ResourceLocation ENTRY_MACHINE_FABRICATOR = Constant.id("machines/circuit_fabricator");

    public static final ResourceLocation ENTRY_ATLAS_EARTH = Constant.id("atlas/earth");
    public static final ResourceLocation ENTRY_ATLAS_MOON = Constant.id("atlas/moon");
    public static final ResourceLocation ENTRY_ATLAS_MARS = Constant.id("atlas/mars");
    public static final ResourceLocation ENTRY_ATLAS_ASTEROIDS = Constant.id("atlas/asteroids");
    public static final ResourceLocation ENTRY_ATLAS_MERCURY = Constant.id("atlas/mercury");
    public static final ResourceLocation ENTRY_ATLAS_VENUS = Constant.id("atlas/venus");
    public static final ResourceLocation ENTRY_ATLAS_JUPITER = Constant.id("atlas/jupiter");
    public static final ResourceLocation ENTRY_ATLAS_SATURN = Constant.id("atlas/saturn");
    public static final ResourceLocation ENTRY_ATLAS_URANUS = Constant.id("atlas/uranus");
    public static final ResourceLocation ENTRY_ATLAS_NEPTUNE = Constant.id("atlas/neptune");

    public static final ResourceLocation ENTRY_LAB_IN_SITU_GUIDE = Constant.id("lab_log/in_situ_labs");
    public static final ResourceLocation ENTRY_LAB_TELEMETRY_PROBES = Constant.id("lab_log/flight_telemetry");

    public static final ResourceLocation ENTRY_INDEX_GUIDE = Constant.id("index/recipe_index");

    private static final List<ManualEntry> BUILTIN = buildEntries();

    private ManualCatalog() {
    }

    public static @NotNull List<ManualEntry> builtInEntries() {
        return BUILTIN;
    }

    private static List<ManualEntry> buildEntries() {
        List<ManualEntry> list = new ArrayList<>();

        // =====================================================================
        // TAB 1: MISSION CONTROL & PRE-LAUNCH CHECKLISTS
        // =====================================================================
        list.add(new ManualEntry(
                ENTRY_MISSION_OVERVIEW,
                ManualTab.MISSION, 1, 10,
                "manual.galacticraft.entry.getting_started.title",
                Constant.id("aerospace_manual"),
                Optional.empty(),
                "manual.galacticraft.hint.default",
                List.of(Constant.id("aerospace_manual"), Constant.id("research_workbench"), Constant.id("aerospace_rd_terminal"), Constant.id("raw_niobium")),
                List.of(
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.getting_started.h1",
                                "manual.galacticraft.entry.getting_started.body1",
                                "tip",
                                "manual.galacticraft.entry.getting_started.tip1",
                                List.of(Constant.id("research_workbench"), Constant.id("compressor"), Constant.id("circuit_fabricator"), Constant.id("aerospace_rd_terminal")),
                                List.of(ENTRY_LAUNCH_CHECKLIST_MOON, ENTRY_CHAIN_NIOBIUM, ENTRY_MATERIALS_SIMULATOR)
                        ),
                        new ManualPage.CraftingPage(
                                "manual.galacticraft.entry.getting_started.craft_manual",
                                Constant.id("aerospace_manual"),
                                "manual.galacticraft.entry.getting_started.craft_manual_cap"
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_LAUNCH_CHECKLIST_MOON,
                ManualTab.MISSION, 1, 20,
                "manual.galacticraft.entry.lunar_checklist.title",
                Constant.id("rocket_launch_pad"),
                Optional.empty(),
                "manual.galacticraft.hint.era1_rocketry",
                List.of(Constant.id("oxygen_mask"), Constant.id("oxygen_gear"), Constant.id("small_oxygen_tank"), Constant.id("parachute"), Constant.id("fuel_bucket")),
                List.of(
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.lunar_checklist.h1",
                                "manual.galacticraft.entry.lunar_checklist.body1",
                                "warn",
                                "manual.galacticraft.entry.lunar_checklist.warn1",
                                List.of(Constant.id("oxygen_mask"), Constant.id("oxygen_gear"), Constant.id("small_oxygen_tank"), Constant.id("parachute")),
                                List.of(ENTRY_MULTIBLOCK_LAUNCH_PAD, ENTRY_ATLAS_MOON)
                        ),
                        new ManualPage.LaunchChecklistPage(
                                "manual.galacticraft.entry.lunar_checklist.check_title",
                                GCCelestialBodies.MOON.location(),
                                List.of(
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.oxygen_mask",
                                                List.of(Constant.id("oxygen_mask")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.oxygen_gear",
                                                List.of(Constant.id("oxygen_gear")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.oxygen_tank",
                                                List.of(Constant.id("small_oxygen_tank"), Constant.id("medium_oxygen_tank"), Constant.id("large_oxygen_tank"), Constant.id("infinite_oxygen_tank")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.parachute",
                                                List.of(Constant.id("parachute")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.return_fuel",
                                                List.of(Constant.id("fuel_bucket"), Constant.id("fluid_canister")), 1, false
                                        )
                                )
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_LAUNCH_CHECKLIST_MARS,
                ManualTab.MISSION, 2, 30,
                "manual.galacticraft.entry.mars_checklist.title",
                Constant.id("compressed_desh"),
                Optional.of(ResearchCatalog.LUNAR_DEEP_SKY_SURVEY),
                "manual.galacticraft.hint.lunar_deep_sky",
                List.of(Constant.id("thermal_padding_chestpiece"), Constant.id("geological_analyzer"), Constant.id("blank_data_drive")),
                List.of(
                        new ManualPage.LaunchChecklistPage(
                                "manual.galacticraft.entry.mars_checklist.check_title",
                                GCCelestialBodies.MARS.location(),
                                List.of(
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.oxygen_mask",
                                                List.of(Constant.id("oxygen_mask")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.oxygen_tank",
                                                List.of(Constant.id("medium_oxygen_tank"), Constant.id("large_oxygen_tank"), Constant.id("infinite_oxygen_tank")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.thermal_suit",
                                                List.of(Constant.id("thermal_padding_chestpiece"), Constant.id("isothermal_padding_chestpiece")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.data_drive",
                                                List.of(Constant.id("blank_data_drive")), 1, false
                                        )
                                )
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_LAUNCH_CHECKLIST_VENUS,
                ManualTab.MISSION, 4, 40,
                "manual.galacticraft.entry.venus_checklist.title",
                Constant.id("isothermal_padding_chestpiece"),
                Optional.of(ResearchCatalog.VENUS_HYPERBARIC_HULLS),
                "manual.galacticraft.hint.venus_hyperbaric",
                List.of(Constant.id("isothermal_padding_chestpiece"), Constant.id("lead_ingot"), Constant.id("shield_controller")),
                List.of(
                        new ManualPage.LaunchChecklistPage(
                                "manual.galacticraft.entry.venus_checklist.check_title",
                                GCCelestialBodies.VENUS.location(),
                                List.of(
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.isothermal_t2",
                                                List.of(Constant.id("isothermal_padding_chestpiece")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.lead_shielding",
                                                List.of(Constant.id("lead_ingot"), Constant.id("lonsdaleite_gem")), 1, true
                                        ),
                                        new ManualPage.LaunchChecklistPage.ChecklistRequirement(
                                                "manual.galacticraft.check.oxygen_tank",
                                                List.of(Constant.id("large_oxygen_tank"), Constant.id("infinite_oxygen_tank")), 1, true
                                        )
                                )
                        )
                )
        ));

        // =====================================================================
        // TAB 2: TINKERS' AEROSPACE MATERIALS & FLIGHT VIABILITY SIMULATOR
        // =====================================================================
        list.add(new ManualEntry(
                ENTRY_MATERIALS_SIMULATOR,
                ManualTab.MATERIALS, 1, 10,
                "manual.galacticraft.entry.flight_simulator.title",
                Constant.id("part_assembler"),
                Optional.empty(),
                "manual.galacticraft.hint.default",
                List.of(Constant.id("part_assembler"), Constant.id("rocket_workbench"), Constant.id("aero_refurbishment_bench")),
                List.of(
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.flight_simulator.h1",
                                "manual.galacticraft.entry.flight_simulator.body1",
                                "info",
                                "manual.galacticraft.entry.flight_simulator.info1",
                                List.of(Constant.id("part_assembler"), Constant.id("aero_refurbishment_bench")),
                                List.of(ENTRY_MATERIALS_EARLY_ALLOYS, ENTRY_MATERIALS_ADVANCED_COMPOSITES)
                        ),
                        new ManualPage.PartMaterialPage(
                                "manual.galacticraft.entry.flight_simulator.calc_title",
                                List.of(),
                                true
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_MATERIALS_EARLY_ALLOYS,
                ManualTab.MATERIALS, 1, 20,
                "manual.galacticraft.entry.early_alloys.title",
                Constant.id("compressed_ferroniobium"),
                Optional.empty(),
                "manual.galacticraft.hint.ferroniobium",
                List.of(Constant.id("compressed_aluminum"), Constant.id("compressed_bronze"), Constant.id("compressed_steel"), Constant.id("compressed_ferroniobium"), Constant.id("compressed_meteoric_iron")),
                List.of(
                        new ManualPage.PartMaterialPage(
                                "manual.galacticraft.entry.early_alloys.list_title",
                                List.of(
                                        Constant.id("compressed_aluminum"),
                                        Constant.id("compressed_bronze"),
                                        Constant.id("compressed_steel"),
                                        Constant.id("compressed_ferroniobium"),
                                        Constant.id("compressed_meteoric_iron")
                                ),
                                false
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_MATERIALS_ADVANCED_COMPOSITES,
                ManualTab.MATERIALS, 3, 30,
                "manual.galacticraft.entry.advanced_composites.title",
                Constant.id("compressed_titanium"),
                Optional.of(ResearchCatalog.LUNAR_VACUUM_METALLURGY),
                "manual.galacticraft.hint.lunar_vacuum",
                List.of(Constant.id("compressed_desh"), Constant.id("compressed_titanium"), Constant.id("lead_ingot"), Constant.id("carbon_fiber_plate"), Constant.id("compressed_niobium")),
                List.of(
                        new ManualPage.PartMaterialPage(
                                "manual.galacticraft.entry.advanced_composites.list_title",
                                List.of(
                                        Constant.id("compressed_desh"),
                                        Constant.id("compressed_titanium"),
                                        Constant.id("lead_shielding"),
                                        Constant.id("carbon_composite"),
                                        Constant.id("compressed_niobium")
                                ),
                                false
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_MATERIALS_EXOTIC_OUTER,
                ManualTab.MATERIALS, 5, 40,
                "manual.galacticraft.entry.exotic_outer.title",
                Constant.id("superionic_ice_crystal"),
                Optional.of(ResearchCatalog.JUPITER_PALLADIUM_CATALYSIS),
                "manual.galacticraft.hint.outer_exotic",
                List.of(Constant.id("compressed_palladium"), Constant.id("compressed_magnesium"), Constant.id("lonsdaleite_gem"), Constant.id("superionic_ice_crystal")),
                List.of(
                        new ManualPage.PartMaterialPage(
                                "manual.galacticraft.entry.exotic_outer.list_title",
                                List.of(
                                        Constant.id("palladium_niobium"),
                                        Constant.id("magnesium_lithium"),
                                        Constant.id("lonsdaleite_lattice"),
                                        Constant.id("superionic_ice_crystal")
                                ),
                                false
                        )
                )
        ));

        // =====================================================================
        // TAB 3: INDUSTRIAL ENGINEERING, 3D MULTIBLOCKS & FLOWCHARTS
        // =====================================================================
        list.add(new ManualEntry(
                ENTRY_MULTIBLOCK_LAUNCH_PAD,
                ManualTab.MACHINES, 1, 10,
                "manual.galacticraft.entry.launch_pad_3d.title",
                Constant.id("rocket_launch_pad"),
                Optional.empty(),
                "manual.galacticraft.hint.era1_rocketry",
                List.of(Constant.id("rocket_launch_pad"), Constant.id("fuel_loader"), Constant.id("reinforced_launch_pad")),
                List.of(
                        new ManualPage.Multiblock3DPage(
                                "manual.galacticraft.entry.launch_pad_3d.mb_title",
                                "manual.galacticraft.entry.launch_pad_3d.caption",
                                List.of(
                                        List.of("PPP", "PPP", "PPP"),
                                        List.of("   ", "   ", "  F")
                                ),
                                orderedPalette(
                                        "P", Constant.id("rocket_launch_pad"),
                                        "F", Constant.id("fuel_loader")
                                )
                        ),
                        new ManualPage.CraftingPage(
                                "manual.galacticraft.entry.launch_pad_3d.recipe",
                                Constant.id("rocket_launch_pad"),
                                "manual.galacticraft.entry.launch_pad_3d.recipe_cap"
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_MULTIBLOCK_AIRLOCK,
                ManualTab.MACHINES, 1, 20,
                "manual.galacticraft.entry.airlock_3d.title",
                Constant.id("air_lock_controller"),
                Optional.of(ResearchCatalog.EARTH_VACUUM_SEALING),
                "manual.galacticraft.hint.vacuum_sealing",
                List.of(Constant.id("air_lock_frame"), Constant.id("air_lock_controller"), Constant.id("oxygen_sealer"), Constant.id("vacuum_glass")),
                List.of(
                        new ManualPage.Multiblock3DPage(
                                "manual.galacticraft.entry.airlock_3d.mb_title",
                                "manual.galacticraft.entry.airlock_3d.caption",
                                List.of(
                                        List.of("FFFF"),
                                        List.of("F  F"),
                                        List.of("F  F"),
                                        List.of("FCFF")
                                ),
                                orderedPalette(
                                        "F", Constant.id("air_lock_frame"),
                                        "C", Constant.id("air_lock_controller")
                                )
                        ),
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.airlock_3d.h1",
                                "manual.galacticraft.entry.airlock_3d.body1",
                                "tip",
                                "manual.galacticraft.entry.airlock_3d.tip1",
                                List.of(Constant.id("air_lock_frame"), Constant.id("air_lock_controller"), Constant.id("oxygen_sealer")),
                                List.of(ENTRY_MULTIBLOCK_SOLAR)
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_MULTIBLOCK_SOLAR,
                ManualTab.MACHINES, 1, 30,
                "manual.galacticraft.entry.solar_panel_3d.title",
                Constant.id("basic_solar_panel"),
                Optional.of(ResearchCatalog.EARLY_SOLAR_PHOTOVOLTAICS),
                "manual.galacticraft.hint.early_solar",
                List.of(Constant.id("basic_solar_panel"), Constant.id("advanced_solar_panel"), Constant.id("aluminum_wire"), Constant.id("energy_storage_module")),
                List.of(
                        new ManualPage.Multiblock3DPage(
                                "manual.galacticraft.entry.solar_panel_3d.mb_title",
                                "manual.galacticraft.entry.solar_panel_3d.caption",
                                List.of(
                                        List.of(" B ", " W ", " E "),
                                        List.of("   ", "   ", "   ")
                                ),
                                orderedPalette(
                                        "B", Constant.id("basic_solar_panel"),
                                        "W", Constant.id("aluminum_wire"),
                                        "E", Constant.id("energy_storage_module")
                                )
                        ),
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.solar_panel_3d.h1",
                                "manual.galacticraft.entry.solar_panel_3d.body1",
                                "warn",
                                "manual.galacticraft.entry.solar_panel_3d.warn1",
                                List.of(Constant.id("basic_solar_panel"), Constant.id("advanced_solar_panel"), Constant.id("atomic_battery")),
                                List.of(ENTRY_MATERIALS_SIMULATOR)
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_MULTIBLOCK_CRYO,
                ManualTab.MACHINES, 2, 35,
                "manual.galacticraft.entry.cryo_3d.title",
                Constant.id("cryogenic_chamber"),
                Optional.of(ResearchCatalog.LUNAR_POLAR_ELECTROLYSIS),
                "manual.galacticraft.hint.lunar_electrolysis",
                List.of(Constant.id("cryogenic_chamber"), Constant.id("electrolytic_separator"), Constant.id("oxygen_storage_module")),
                List.of(
                        new ManualPage.Multiblock3DPage(
                                "manual.galacticraft.entry.cryo_3d.mb_title",
                                "manual.galacticraft.entry.cryo_3d.caption",
                                List.of(
                                        List.of("M M", " C ", "M M"),
                                        List.of("   ", " P ", "   "),
                                        List.of("   ", " P ", "   ")
                                ),
                                orderedPalette(
                                        "C", Constant.id("cryogenic_chamber"),
                                        "P", Constant.id("cryogenic_chamber_part"),
                                        "M", Constant.id("oxygen_storage_module")
                                )
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_MACHINE_FABRICATOR,
                ManualTab.MACHINES, 1, 38,
                "manual.galacticraft.entry.circuit_fabricator.title",
                Constant.id("circuit_fabricator"),
                Optional.empty(),
                "manual.galacticraft.hint.default",
                List.of(Constant.id("circuit_fabricator"), Constant.id("basic_wafer"), Constant.id("advanced_wafer"), Constant.id("silicon")),
                List.of(
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.circuit_fabricator.h1",
                                "manual.galacticraft.entry.circuit_fabricator.body1",
                                "tip",
                                "manual.galacticraft.entry.circuit_fabricator.tip1",
                                List.of(Constant.id("circuit_fabricator"), Constant.id("basic_wafer"), Constant.id("advanced_wafer")),
                                List.of(ENTRY_CHAIN_NIOBIUM)
                        ),
                        new ManualPage.CraftingPage(
                                "manual.galacticraft.entry.circuit_fabricator.recipe",
                                Constant.id("basic_wafer"),
                                "manual.galacticraft.entry.circuit_fabricator.recipe_cap"
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_CHAIN_NIOBIUM,
                ManualTab.MACHINES, 1, 40,
                "manual.galacticraft.entry.niobium_chain.title",
                Constant.id("ferroniobium_ingot"),
                Optional.of(ResearchCatalog.FERRONIOBIUM_ALUMINOTHERMY),
                "manual.galacticraft.hint.ferroniobium",
                List.of(Constant.id("raw_niobium"), Constant.id("niobium_oxide"), Constant.id("ferroniobium_ingot"), Constant.id("niobium_ingot"), Constant.id("compressed_niobium"), Constant.id("superconducting_coil")),
                List.of(
                        new ManualPage.FlowchartPage(
                                "manual.galacticraft.entry.niobium_chain.flow_title",
                                List.of(
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("raw_niobium"), "manual.galacticraft.flow.nb.s1", "manual.galacticraft.flow.nb.d1", 0xFF60A5FA),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("niobium_oxide"), "manual.galacticraft.flow.nb.s2", "manual.galacticraft.flow.nb.d2", 0xFF38BDF8),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("ferroniobium_ingot"), "manual.galacticraft.flow.nb.s3", "manual.galacticraft.flow.nb.d3", 0xFFF59E0B),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("niobium_ingot"), "manual.galacticraft.flow.nb.s4", "manual.galacticraft.flow.nb.d4", 0xFFA855F7),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("superconducting_coil"), "manual.galacticraft.flow.nb.s5", "manual.galacticraft.flow.nb.d5", 0xFF10B981)
                                ),
                                "manual.galacticraft.entry.niobium_chain.caption"
                        ),
                        new ManualPage.InfuserPage(
                                "manual.galacticraft.entry.niobium_chain.infuser_title",
                                Constant.id("ferroniobium_ingot"),
                                "block.galacticraft.vacuum_metallurgic_infuser",
                                List.of(Constant.id("niobium_oxide"), Constant.id("aluminum_ingot")),
                                Constant.id("ferroniobium_ingot"),
                                "manual.galacticraft.entry.niobium_chain.infuser_note"
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_CHAIN_SABATIER,
                ManualTab.MACHINES, 3, 50,
                "manual.galacticraft.entry.sabatier_chain.title",
                Constant.id("sabatier_chemical_reactor"),
                Optional.of(ResearchCatalog.MARTIAN_SABATIER_ISRU),
                "manual.galacticraft.hint.martian_sabatier",
                List.of(Constant.id("atmospheric_fractionator"), Constant.id("sabatier_chemical_reactor"), Constant.id("liquid_methane_bucket"), Constant.id("electrolytic_separator")),
                List.of(
                        new ManualPage.FlowchartPage(
                                "manual.galacticraft.entry.sabatier_chain.flow_title",
                                List.of(
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("atmospheric_fractionator"), "manual.galacticraft.flow.sab.s1", "manual.galacticraft.flow.sab.d1", 0xFFEF4444),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("electrolytic_separator"), "manual.galacticraft.flow.sab.s2", "manual.galacticraft.flow.sab.d2", 0xFF38BDF8),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("sabatier_chemical_reactor"), "manual.galacticraft.flow.sab.s3", "manual.galacticraft.flow.sab.d3", 0xFF10B981),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("liquid_methane_bucket"), "manual.galacticraft.flow.sab.s4", "manual.galacticraft.flow.sab.d4", 0xFF34D399)
                                ),
                                "manual.galacticraft.entry.sabatier_chain.caption"
                        ),
                        new ManualPage.InfuserPage(
                                "manual.galacticraft.entry.sabatier_chain.reactor_title",
                                Constant.id("sabatier_chemical_reactor"),
                                "block.galacticraft.sabatier_chemical_reactor",
                                List.of(Constant.id("fluid_canister"), Constant.id("liquid_hydrogen_bucket")),
                                Constant.id("liquid_methane_bucket"),
                                "manual.galacticraft.entry.sabatier_chain.reactor_note"
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_CHAIN_ACID_LEACHING,
                ManualTab.MACHINES, 4, 60,
                "manual.galacticraft.entry.acid_leaching.title",
                Constant.id("chemical_leaching_plant"),
                Optional.of(ResearchCatalog.VENUS_ACID_LEACHING),
                "manual.galacticraft.hint.venus_leaching",
                List.of(Constant.id("chemical_leaching_plant"), Constant.id("sulfuric_acid_bucket"), Constant.id("raw_niobium"), Constant.id("raw_titanium")),
                List.of(
                        new ManualPage.FlowchartPage(
                                "manual.galacticraft.entry.acid_leaching.flow_title",
                                List.of(
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("sulfuric_acid_bucket"), "manual.galacticraft.flow.acid.s1", "manual.galacticraft.flow.acid.d1", 0xFFEAB308),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("chemical_leaching_plant"), "manual.galacticraft.flow.acid.s2", "manual.galacticraft.flow.acid.d2", 0xFFF97316),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("niobium_oxide"), "manual.galacticraft.flow.acid.s3", "manual.galacticraft.flow.acid.d3", 0xFF10B981)
                                ),
                                "manual.galacticraft.entry.acid_leaching.caption"
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_CHAIN_HELIUM3_FUSION,
                ManualTab.MACHINES, 5, 70,
                "manual.galacticraft.entry.helium3_fusion.title",
                Constant.id("helium3_fusion_reactor"),
                Optional.of(ResearchCatalog.ZERO_G_ANEUTRONIC_MHD_FUSION),
                "manual.galacticraft.hint.helium3_fusion",
                List.of(Constant.id("regolith_thermal_desorber"), Constant.id("isotopic_centrifuge"), Constant.id("helium3_cell"), Constant.id("helium3_fusion_cell"), Constant.id("helium3_fusion_reactor")),
                List.of(
                        new ManualPage.FlowchartPage(
                                "manual.galacticraft.entry.helium3_fusion.flow_title",
                                List.of(
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("regolith_thermal_desorber"), "manual.galacticraft.flow.he3.s1", "manual.galacticraft.flow.he3.d1", 0xFF38BDF8),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("isotopic_centrifuge"), "manual.galacticraft.flow.he3.s2", "manual.galacticraft.flow.he3.d2", 0xFFA855F7),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("helium3_fusion_cell"), "manual.galacticraft.flow.he3.s3", "manual.galacticraft.flow.he3.d3", 0xFF22D3EE),
                                        new ManualPage.FlowchartPage.FlowStep(Constant.id("helium3_fusion_reactor"), "manual.galacticraft.flow.he3.s4", "manual.galacticraft.flow.he3.d4", 0xFF10B981)
                                ),
                                "manual.galacticraft.entry.helium3_fusion.caption"
                        )
                )
        ));

        // =====================================================================
        // TAB 4: PLANETARY ATLAS & ORE STRATIGRAPHY (ALL 10 SOLAR SYSTEM BODIES)
        // =====================================================================
        list.add(planetEntry(ENTRY_ATLAS_EARTH, 1, 10, GCCelestialBodies.EARTH.location(), Constant.id("niobium_ore"), Optional.empty(), "manual.galacticraft.hint.default",
                "manual.galacticraft.atlas.earth.title", "manual.galacticraft.atlas.earth.hazards",
                List.of(
                        ore(Constant.id("aluminum_ore"), "block.galacticraft.aluminum_ore", 8, 64, 1.4, 0xFFD8E2EC),
                        ore(Constant.id("tin_ore"), "block.galacticraft.tin_ore", -16, 48, 1.3, 0xFFC0CBD6),
                        ore(Constant.id("silicon_ore"), "block.galacticraft.silicon_ore", -48, 24, 1.0, 0xFF7B8AA0),
                        ore(Constant.id("niobium_ore"), "block.galacticraft.niobium_ore", -56, 16, 0.8, 0xFF5FA8D3)
                )));

        list.add(planetEntry(ENTRY_ATLAS_MOON, 1, 20, GCCelestialBodies.MOON.location(), Constant.id("moon_turf"), Optional.empty(), "manual.galacticraft.hint.moon_ephemeris",
                "manual.galacticraft.atlas.moon.title", "manual.galacticraft.atlas.moon.hazards",
                List.of(
                        ore(Constant.id("moon_copper_ore"), "block.galacticraft.moon_copper_ore", 12, 72, 1.5, 0xFFD97742),
                        ore(Constant.id("moon_ilmenite_ore"), "block.galacticraft.moon_ilmenite_ore", -16, 44, 3.0, 0xFF60A5FA),
                        ore(Constant.id("lunar_sapphire_ore"), "block.galacticraft.lunar_sapphire_ore", -44, 18, 1.8, 0xFF3B82F6),
                        ore(Constant.id("olivine_basalt"), "block.galacticraft.olivine_basalt", -56, 20, 2.2, 0xFF84CC16)
                )));

        list.add(planetEntry(ENTRY_ATLAS_MARS, 2, 30, GCCelestialBodies.MARS.location(), Constant.id("desh_ore"), Optional.of(ResearchCatalog.LUNAR_DEEP_SKY_SURVEY), "manual.galacticraft.hint.lunar_deep_sky",
                "manual.galacticraft.atlas.mars.title", "manual.galacticraft.atlas.mars.hazards",
                List.of(
                        ore(Constant.id("mars_iron_ore"), "block.galacticraft.mars_iron_ore", 10, 72, 2.5, 0xFFD97757),
                        ore(Constant.id("desh_ore"), "block.galacticraft.desh_ore", -32, 36, 3.0, 0xFFE05A3F),
                        ore(Constant.id("mars_titanium_ore"), "block.galacticraft.mars_titanium_ore", -48, 12, 1.2, 0xFF93C5FD),
                        ore(Constant.id("mars_niobium_ore"), "block.galacticraft.mars_niobium_ore", -56, 8, 1.0, 0xFF60A5FA)
                )));

        list.add(planetEntry(ENTRY_ATLAS_ASTEROIDS, 3, 40, GCCelestialBodies.ASTEROID.location(), Constant.id("ilmenite_ore"), Optional.of(ResearchCatalog.LUNAR_DEEP_SKY_SURVEY), "manual.galacticraft.hint.lunar_deep_sky",
                "manual.galacticraft.atlas.asteroids.title", "manual.galacticraft.atlas.asteroids.hazards",
                List.of(
                        ore(Constant.id("dense_ice"), "block.galacticraft.dense_ice", 20, 110, 3.2, 0xFF7DD3FC),
                        ore(Constant.id("asteroid_iron_ore"), "block.galacticraft.asteroid_iron_ore", 10, 100, 3.5, 0xFFCBD5E1),
                        ore(Constant.id("ilmenite_ore"), "block.galacticraft.ilmenite_ore", 0, 96, 3.2, 0xFF38BDF8),
                        ore(Constant.id("asteroid_niobium_ore"), "block.galacticraft.asteroid_niobium_ore", 0, 80, 1.8, 0xFF818CF8)
                )));

        list.add(planetEntry(ENTRY_ATLAS_MERCURY, 4, 50, GCCelestialBodies.MERCURY.location(), Constant.id("mercury_sulfide_ore"), Optional.of(ResearchCatalog.LUNAR_DEEP_SKY_SURVEY), "manual.galacticraft.hint.lunar_deep_sky",
                "manual.galacticraft.atlas.mercury.title", "manual.galacticraft.atlas.mercury.hazards",
                List.of(
                        ore(Constant.id("mercury_hollow_deposit"), "block.galacticraft.mercury_hollow_deposit", 48, 96, 3.0, 0xFF38BDF8),
                        ore(Constant.id("mercury_native_iron_ore"), "block.galacticraft.mercury_native_iron_ore", -16, 60, 3.5, 0xFF94A3B8),
                        ore(Constant.id("mercury_graphite_ore"), "block.galacticraft.mercury_graphite_ore", -32, 32, 2.6, 0xFF475569),
                        ore(Constant.id("mercury_sulfide_ore"), "block.galacticraft.mercury_sulfide_ore", -56, 16, 2.4, 0xFFF59E0B)
                )));

        list.add(planetEntry(ENTRY_ATLAS_VENUS, 4, 60, GCCelestialBodies.VENUS.location(), Constant.id("galena_ore"), Optional.of(ResearchCatalog.LUNAR_DEEP_SKY_SURVEY), "manual.galacticraft.hint.lunar_deep_sky",
                "manual.galacticraft.atlas.venus.title", "manual.galacticraft.atlas.venus.hazards",
                List.of(
                        ore(Constant.id("venus_aluminum_ore"), "block.galacticraft.venus_aluminum_ore", 8, 64, 1.8, 0xFFD8E2EC),
                        ore(Constant.id("galena_ore"), "block.galacticraft.galena_ore", -24, 48, 3.2, 0xFF64748B),
                        ore(Constant.id("solar_ore"), "block.galacticraft.solar_ore", -48, 24, 2.8, 0xFFFACC15),
                        ore(Constant.id("venus_niobium_ore"), "block.galacticraft.venus_niobium_ore", -56, 12, 1.6, 0xFF60A5FA)
                )));

        list.add(planetEntry(ENTRY_ATLAS_JUPITER, 5, 70, GCCelestialBodies.JUPITER.location(), Constant.id("jupiter_palladium_ore"), Optional.of(ResearchCatalog.OUTER_SYSTEM_EPHEMERIS), "manual.galacticraft.hint.outer_ephemeris",
                "manual.galacticraft.atlas.jupiter.title", "manual.galacticraft.atlas.jupiter.hazards",
                List.of(
                        ore(Constant.id("jupiter_niobium_ore"), "block.galacticraft.jupiter_niobium_ore", -12, 56, 2.0, 0xFF60A5FA),
                        ore(Constant.id("jupiter_palladium_ore"), "block.galacticraft.jupiter_palladium_ore", -44, 28, 3.0, 0xFFFDE68A),
                        ore(Constant.id("jupiter_metallic_hydrogen_rock"), "block.galacticraft.jupiter_metallic_hydrogen_rock", -60, -8, 2.4, 0xFF38BDF8)
                )));

        list.add(planetEntry(ENTRY_ATLAS_SATURN, 5, 80, GCCelestialBodies.SATURN.location(), Constant.id("saturn_magnesium_ore"), Optional.of(ResearchCatalog.OUTER_SYSTEM_EPHEMERIS), "manual.galacticraft.hint.outer_ephemeris",
                "manual.galacticraft.atlas.saturn.title", "manual.galacticraft.atlas.saturn.hazards",
                List.of(
                        ore(Constant.id("saturn_ring_ice_rock"), "block.galacticraft.saturn_ring_ice_rock", 24, 88, 3.0, 0xFFBAE6FD),
                        ore(Constant.id("saturn_magnesium_ore"), "block.galacticraft.saturn_magnesium_ore", -24, 44, 3.2, 0xFF86EFAC),
                        ore(Constant.id("saturn_helium_mantle_rock"), "block.galacticraft.saturn_helium_mantle_rock", -60, 0, 3.5, 0xFF22D3EE)
                )));

        list.add(planetEntry(ENTRY_ATLAS_URANUS, 6, 90, GCCelestialBodies.URANUS.location(), Constant.id("uranus_uranium_ore"), Optional.of(ResearchCatalog.OUTER_SYSTEM_EPHEMERIS), "manual.galacticraft.hint.outer_ephemeris",
                "manual.galacticraft.atlas.uranus.title", "manual.galacticraft.atlas.uranus.hazards",
                List.of(
                        ore(Constant.id("uranus_methane_ice"), "block.galacticraft.uranus_methane_ice", 16, 80, 2.8, 0xFF67E8F9),
                        ore(Constant.id("uranus_uranium_ore"), "block.galacticraft.uranus_uranium_ore", -32, 32, 3.2, 0xFF4ADE80),
                        ore(Constant.id("uranus_diamond_bearing_rock"), "block.galacticraft.uranus_diamond_bearing_rock", -60, -4, 2.5, 0xFF7FE8FF)
                )));

        list.add(planetEntry(ENTRY_ATLAS_NEPTUNE, 6, 100, GCCelestialBodies.NEPTUNE.location(), Constant.id("superionic_ice_crystal"), Optional.of(ResearchCatalog.OUTER_SYSTEM_EPHEMERIS), "manual.galacticraft.hint.outer_ephemeris",
                "manual.galacticraft.atlas.neptune.title", "manual.galacticraft.atlas.neptune.hazards",
                List.of(
                        ore(Constant.id("neptune_nitrogen_ice"), "block.galacticraft.neptune_nitrogen_ice", 16, 84, 3.0, 0xFF60A5FA),
                        ore(Constant.id("neptune_titanium_ore"), "block.galacticraft.neptune_titanium_ore", -24, 40, 2.4, 0xFF93C5FD),
                        ore(Constant.id("neptune_abyssal_mantle_rock"), "block.galacticraft.neptune_abyssal_mantle_rock", -60, -8, 3.4, 0xFF38BDF8)
                )));

        // =====================================================================
        // TAB 5: LABORATORY LOG BY LOCATION
        // =====================================================================
        list.add(new ManualEntry(
                ENTRY_LAB_IN_SITU_GUIDE,
                ManualTab.LAB_LOG, 1, 10,
                "manual.galacticraft.entry.in_situ_labs.title",
                Constant.id("geological_analyzer"),
                Optional.empty(),
                "manual.galacticraft.hint.default",
                List.of(Constant.id("research_workbench"), Constant.id("observatory_telescope"), Constant.id("geological_analyzer"), Constant.id("environmental_test_chamber"), Constant.id("zero_g_research_chamber"), Constant.id("research_data_link")),
                List.of(
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.in_situ_labs.h1",
                                "manual.galacticraft.entry.in_situ_labs.body1",
                                "info",
                                "manual.galacticraft.entry.in_situ_labs.info1",
                                List.of(Constant.id("research_workbench"), Constant.id("observatory_telescope"), Constant.id("geological_analyzer"), Constant.id("environmental_test_chamber"), Constant.id("zero_g_research_chamber")),
                                List.of(ENTRY_LAB_TELEMETRY_PROBES)
                        )
                )
        ));

        list.add(new ManualEntry(
                ENTRY_LAB_TELEMETRY_PROBES,
                ManualTab.LAB_LOG, 2, 20,
                "manual.galacticraft.entry.flight_telemetry.title",
                Constant.id("recorded_data_drive"),
                Optional.of(ResearchCatalog.ERA1_ROCKETRY),
                "manual.galacticraft.hint.era1_rocketry",
                List.of(Constant.id("blank_data_drive"), Constant.id("recorded_data_drive"), Constant.id("telemetry_analyzer"), Constant.id("orbital_survey_map")),
                List.of(
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.flight_telemetry.h1",
                                "manual.galacticraft.entry.flight_telemetry.body1",
                                "tip",
                                "manual.galacticraft.entry.flight_telemetry.tip1",
                                List.of(Constant.id("blank_data_drive"), Constant.id("recorded_data_drive"), Constant.id("telemetry_analyzer"), Constant.id("orbital_survey_map")),
                                List.of(ENTRY_ATLAS_MARS, ENTRY_ATLAS_VENUS)
                        )
                )
        ));

        // =====================================================================
        // TAB 6: GLOBAL INDEX & EMI/REI/JEI BRIDGE
        // =====================================================================
        list.add(new ManualEntry(
                ENTRY_INDEX_GUIDE,
                ManualTab.INDEX, 1, 10,
                "manual.galacticraft.entry.recipe_index.title",
                Constant.id("aerospace_rd_terminal"),
                Optional.empty(),
                "manual.galacticraft.hint.default",
                List.of(Constant.id("aerospace_manual"), Constant.id("aerospace_rd_terminal")),
                List.of(
                        new ManualPage.TextPage(
                                "manual.galacticraft.entry.recipe_index.h1",
                                "manual.galacticraft.entry.recipe_index.body1",
                                "tip",
                                "manual.galacticraft.entry.recipe_index.tip1",
                                List.of(Constant.id("compressor"), Constant.id("circuit_fabricator"), Constant.id("part_assembler"), Constant.id("telemetry_analyzer")),
                                List.of(ENTRY_MISSION_OVERVIEW, ENTRY_MATERIALS_SIMULATOR, ENTRY_MULTIBLOCK_LAUNCH_PAD)
                        )
                )
        ));

        return Collections.unmodifiableList(list);
    }

    private static ManualEntry planetEntry(
            ResourceLocation id,
            int era,
            int order,
            ResourceLocation bodyId,
            ResourceLocation iconItem,
            Optional<ResourceLocation> requiredResearch,
            String lockHintKey,
            String titleKey,
            String hazardsKey,
            List<ManualPage.PlanetStratigraphyPage.OreBand> ores
    ) {
        List<ResourceLocation> related = new ArrayList<>();
        related.add(iconItem);
        for (ManualPage.PlanetStratigraphyPage.OreBand ore : ores) {
            if (!related.contains(ore.oreItem())) {
                related.add(ore.oreItem());
            }
        }
        return new ManualEntry(
                id,
                ManualTab.ATLAS,
                era,
                order,
                titleKey,
                iconItem,
                requiredResearch,
                lockHintKey,
                List.copyOf(related),
                List.of(new ManualPage.PlanetStratigraphyPage(titleKey, bodyId, ores, hazardsKey))
        );
    }

    private static ManualPage.PlanetStratigraphyPage.OreBand ore(
            ResourceLocation item, String labelKey, int minY, int maxY, double rarity, int color
    ) {
        return new ManualPage.PlanetStratigraphyPage.OreBand(item, labelKey, minY, maxY, rarity, color);
    }

    private static Map<String, ResourceLocation> orderedPalette(Object... pairs) {
        Map<String, ResourceLocation> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put((String) pairs[i], (ResourceLocation) pairs[i + 1]);
        }
        return map;
    }
}
