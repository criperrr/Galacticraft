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

package dev.galacticraft.mod.research;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCCelestialBodies;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical radial KSP+ research constellation with 55 spaced nodes radiating outward from the
 * central {@code (0, 0)} Earth Prototyping hub into 5 distinct branching sectors:
 * <ul>
 *   <li><b>North (Up):</b> Energy, CommNet &amp; Superconductors</li>
 *   <li><b>North-East (Up-Right):</b> Metallurgy, Chemistry &amp; Machines</li>
 *   <li><b>North-West (Up-Left):</b> Life Support, Bio-Domes &amp; Bases</li>
 *   <li><b>South-West (Down-Left):</b> Suits, Metrology, Armor &amp; Tools</li>
 *   <li><b>South-East (Down-Right):</b> Aerospace, Astronomy &amp; Propulsion</li>
 * </ul>
 */
public final class ResearchCatalog {
    // === CORE HUB & NE WING: METALLURGY, CHEMISTRY & MACHINES (11 nodes) ===
    public static final ResourceLocation EARTH_PROTOTYPING = Constant.id("research/earth_prototyping");
    public static final ResourceLocation FERRONIOBIUM_ALUMINOTHERMY = Constant.id("research/ferroniobium_aluminothermy");
    public static final ResourceLocation HEAVY_ALLOY_FORGING = Constant.id("research/heavy_alloy_forging");
    public static final ResourceLocation LUNAR_VACUUM_METALLURGY = Constant.id("research/lunar_vacuum_metallurgy");
    public static final ResourceLocation MARTIAN_SABATIER_ISRU = Constant.id("research/martian_sabatier_isru");
    public static final ResourceLocation REGOLITH_SINTERING_PLANTS = Constant.id("research/regolith_sintering_plants");
    public static final ResourceLocation ASTEROID_SMELTING_REFINERY = Constant.id("research/asteroid_smelting_refinery");
    public static final ResourceLocation VENUS_ACID_LEACHING = Constant.id("research/venus_acid_leaching");
    public static final ResourceLocation REFRACTORY_CARBIDE_FOUNDRY = Constant.id("research/refractory_carbide_foundry");
    public static final ResourceLocation ORBITAL_NANOFABRICATION = Constant.id("research/orbital_nanofabrication");
    public static final ResourceLocation SATURN_HELIUM3_HARVESTING = Constant.id("research/saturn_helium3_harvesting");

    // === NW WING: LIFE SUPPORT, BIO-DOMES & BASES (11 nodes) ===
    public static final ResourceLocation BASIC_OXYGEN_GEAR = Constant.id("research/basic_oxygen_gear");
    public static final ResourceLocation EARTH_VACUUM_SEALING = Constant.id("research/earth_vacuum_sealing");
    public static final ResourceLocation LUNAR_POLAR_ELECTROLYSIS = Constant.id("research/lunar_polar_electrolysis");
    public static final ResourceLocation LUNAR_BIO_HABITATION = Constant.id("research/lunar_bio_habitation");
    public static final ResourceLocation PRESSURIZED_COLONY_DOMES = Constant.id("research/pressurized_colony_domes");
    public static final ResourceLocation ZERO_G_CARBON_SOFI = Constant.id("research/zero_g_carbon_sofi");
    public static final ResourceLocation CLOSED_ECOSYSTEM_BIOREACTORS = Constant.id("research/closed_ecosystem_bioreactors");
    public static final ResourceLocation RADIATION_SHIELDED_HABITATS = Constant.id("research/radiation_shielded_habitats");
    public static final ResourceLocation CRYO_HIBERNATION_PODS = Constant.id("research/cryo_hibernation_pods");
    public static final ResourceLocation ZERO_G_DILUTION_CRYOGENICS = Constant.id("research/zero_g_dilution_cryogenics");
    public static final ResourceLocation DEEP_SPACE_ARCOLOGY_DOMES = Constant.id("research/deep_space_arcology_domes");

    // === NORTH WING: ENERGY, COMMNET & SUPERCONDUCTORS (11 nodes) ===
    public static final ResourceLocation EARLY_SOLAR_PHOTOVOLTAICS = Constant.id("research/early_solar_photovoltaics");
    public static final ResourceLocation HIGH_VOLTAGE_CONDUITS = Constant.id("research/high_voltage_conduits");
    public static final ResourceLocation LUNAR_HE3_THERMAL_DESORPTION = Constant.id("research/lunar_he3_thermal_desorption");
    public static final ResourceLocation INTERPLANETARY_SCIENCE_NETWORK = Constant.id("research/interplanetary_science_network");
    public static final ResourceLocation HIGH_GAIN_COMMNET_ARRAYS = Constant.id("research/high_gain_commnet_arrays");
    public static final ResourceLocation ZERO_G_SUPERCONDUCTORS_NB3SN = Constant.id("research/zero_g_superconductors_nb3sn");
    public static final ResourceLocation MERCURY_SOLAR_VOLATILES_HE3 = Constant.id("research/mercury_solar_volatiles_he3");
    public static final ResourceLocation MERCURY_THORIUM_ISOTOPES = Constant.id("research/mercury_thorium_isotopes");
    public static final ResourceLocation QUANTUM_SUPERCOMPUTING_CORES = Constant.id("research/quantum_supercomputing_cores");
    public static final ResourceLocation ZERO_G_ANEUTRONIC_MHD_FUSION = Constant.id("research/zero_g_aneutronic_mhd_fusion");
    public static final ResourceLocation STELLAR_PLASMA_CONFINEMENT = Constant.id("research/stellar_plasma_confinement");

    // === SW WING: SUITS, METROLOGY, ARMOR & TOOLS (11 nodes) ===
    public static final ResourceLocation FIELD_METROLOGY_INSTRUMENTS = Constant.id("research/field_metrology_instruments");
    public static final ResourceLocation REINFORCED_EVA_TOOLS = Constant.id("research/reinforced_eva_tools");
    public static final ResourceLocation LUNAR_OLIVINE_OPTICS = Constant.id("research/lunar_olivine_optics");
    public static final ResourceLocation PRECISION_SPECTROSCOPY_OPTICS = Constant.id("research/precision_spectroscopy_optics");
    public static final ResourceLocation MARTIAN_DESH_ELASTOMERS = Constant.id("research/martian_desh_elastomers");
    public static final ResourceLocation THERMAL_EXOSUIT_SYSTEMS = Constant.id("research/thermal_exosuit_systems");
    public static final ResourceLocation ASTEROID_TITANIUM_COMPOSITES = Constant.id("research/asteroid_titanium_composites");
    public static final ResourceLocation VENUS_HYPERBARIC_HULLS = Constant.id("research/venus_hyperbaric_hulls");
    public static final ResourceLocation POWERED_EVA_EXOSKELETONS = Constant.id("research/powered_eva_exoskeletons");
    public static final ResourceLocation PLASMA_EXCAVATION_TOOLS = Constant.id("research/plasma_excavation_tools");
    public static final ResourceLocation URANUS_LONSDALEITE_PRESSURE = Constant.id("research/uranus_lonsdaleite_pressure");

    // === SE WING: AEROSPACE, ASTRONOMY & PROPULSION (11 nodes) ===
    public static final ResourceLocation EARTH_LUNAR_EPHEMERIS = Constant.id("research/earth_lunar_ephemeris");
    public static final ResourceLocation ERA1_ROCKETRY = Constant.id("research/era1_rocketry");
    public static final ResourceLocation LUNAR_DEEP_SKY_SURVEY = Constant.id("research/lunar_deep_sky_survey");
    public static final ResourceLocation HIGH_ALTITUDE_OPTICS_DOMES = Constant.id("research/high_altitude_optics_domes");
    public static final ResourceLocation OUTER_SYSTEM_EPHEMERIS = Constant.id("research/outer_system_ephemeris");
    public static final ResourceLocation METHALOX_HEAVY_ENGINES = Constant.id("research/methalox_heavy_engines");
    public static final ResourceLocation ORBITAL_SHIPYARD_DOCKS = Constant.id("research/orbital_shipyard_docks");
    public static final ResourceLocation ZERO_G_HEAVY_ORBITAL_WELDING = Constant.id("research/zero_g_heavy_orbital_welding");
    public static final ResourceLocation JUPITER_PALLADIUM_CATALYSIS = Constant.id("research/jupiter_palladium_catalysis");
    public static final ResourceLocation DEEP_SPACE_OPTICAL_INTERFEROMETRY = Constant.id("research/deep_space_optical_interferometry");
    public static final ResourceLocation NEPTUNE_SUPERIONIC_MAGNETOPLASMA = Constant.id("research/neptune_superionic_magnetoplasma");

    private static final Map<ResourceLocation, ResearchNode> NODES = new LinkedHashMap<>();
    private static final Map<ResearchBranch, List<ResearchNode>> BY_BRANCH = new EnumMap<>(ResearchBranch.class);

    static {
        for (ResearchBranch branch : ResearchBranch.values()) {
            BY_BRANCH.put(branch, new ArrayList<>());
        }

        // =========================================================================
        // CENTER HUB (0, 0) & NORTH-EAST WING: METALLURGY, CHEMISTRY & MACHINES
        // =========================================================================
        register(new ResearchNode(
                EARTH_PROTOTYPING, 1, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.STRUCTURES,
                35, 0, null, 0, 0,
                List.of(), ResearchLocationRequirement.ANY, "Research Workbench",
                "Base Industrial Prototyping",
                "Central R&D foundation: non-electric prototyping using raw niobium, aluminum, tin, silicon, coal and paper.",
                List.of("Coal Generator", "Compressor", "Circuit Fabricator"),
                Constant.id("compressor")
        ));
        register(new ResearchNode(
                FERRONIOBIUM_ALUMINOTHERMY, 1, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.THERMODYNAMICS,
                65, 25, null, 2, -1,
                List.of(EARTH_PROTOTYPING), ResearchLocationRequirement.ANY, "Research Workbench",
                "Ferroniobium Aluminothermy",
                "Aluminothermic reduction of pyrochlore niobium oxide into HSLA ferroniobium aerospace alloys.",
                List.of("Ferroniobium Ingot", "Compressed Ferroniobium", "HSLA Chassis"),
                Constant.id("ferroniobium_ingot")
        ));
        register(new ResearchNode(
                HEAVY_ALLOY_FORGING, 2, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.STRUCTURES,
                125, 45, null, 4, -1,
                List.of(FERRONIOBIUM_ALUMINOTHERMY), ResearchLocationRequirement.ANY, "Research Workbench",
                "Heavy Alloy Rolling & Presses",
                "High-tonnage electric plate rolling and multi-layer aerospace sheet lamination.",
                List.of("Electric Compressor", "Heavy-Duty Plate T1", "Reinforced Hull Bulkheads"),
                Constant.id("research_workbench")
        ));
        register(new ResearchNode(
                LUNAR_VACUUM_METALLURGY, 2, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.THERMODYNAMICS,
                180, 65, ResearchMilestone.VACUUM_TRIAL_CERTIFIED, 5, -3,
                List.of(HEAVY_ALLOY_FORGING), ResearchLocationRequirement.VACUUM, "Environmental Test Chamber",
                "Vacuum Arc Metallurgy & C-103",
                "Exploits natural lunar/mercurian vacuum to smelt pure refractory niobium and C-103 superalloys.",
                List.of("Electric Arc Furnace", "Vacuum Metallurgic Infuser", "Pure Niobium", "C-103 Superalloy"),
                Constant.id("compressed_niobium")
        ));
        register(new ResearchNode(
                MARTIAN_SABATIER_ISRU, 3, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.CRYOGENICS,
                270, 95, ResearchMilestone.MARS_OBSERVED, 7, -2,
                List.of(LUNAR_VACUUM_METALLURGY), ResearchLocationRequirement.forBody(GCCelestialBodies.MARS.location(), "Mars (95% CO2)"), "Geological Analyzer",
                "Martian Sabatier ISRU Cycle",
                "Captures Martian CO2 and reacts it with H2 to synthesize Methalox propellant and recycled water.",
                List.of("Atmospheric Fractionator", "Sabatier Chemical Reactor", "Liquid Methane ISRU"),
                Constant.id("sabatier_chemical_reactor")
        ));
        register(new ResearchNode(
                REGOLITH_SINTERING_PLANTS, 3, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.STRUCTURES,
                350, 120, null, 7, -4,
                List.of(LUNAR_VACUUM_METALLURGY), ResearchLocationRequirement.forBody(GCCelestialBodies.MARS.location(), "Mars / Moon Surface"), "Geological Analyzer",
                "Automated Regolith Sintering",
                "Microwave sintering fuses raw planetary regolith into radiation-opaque foundation blocks and bricks.",
                List.of("Regolith Bricks", "Basalt Heat Shields", "Automated Ore Kilns"),
                Constant.id("geological_analyzer")
        ));
        register(new ResearchNode(
                ASTEROID_SMELTING_REFINERY, 4, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.THERMODYNAMICS,
                480, 165, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, 9, -3,
                List.of(MARTIAN_SABATIER_ISRU, REGOLITH_SINTERING_PLANTS), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Zero-G Ore Refining & Crushing",
                "Containerless electromagnetic smelting separates M-type asteroid nickel-iron-titanium ingots.",
                List.of("Zero-G Induction Smelter", "2x Asteroid Ore Yield", "Ultra-Pure Alloy Flux"),
                Constant.id("electrostatic_levitator_ring")
        ));
        register(new ResearchNode(
                VENUS_ACID_LEACHING, 4, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.CRYOGENICS,
                590, 200, ResearchMilestone.VENUS_92ATM_CERTIFIED, 10, -1,
                List.of(ASTEROID_SMELTING_REFINERY), ResearchLocationRequirement.VENUS_HYPERBARIC, "Geological Analyzer",
                "Hyperbaric Acid Ore Leaching",
                "Uses Venusian sulfuric acid chemistry to triple refractory ore oxide extraction yields.",
                List.of("Chemical Leaching Plant", "3x Niobium Oxide", "2x Titanium Leaching"),
                Constant.id("chemical_leaching_plant")
        ));
        register(new ResearchNode(
                REFRACTORY_CARBIDE_FOUNDRY, 5, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.THERMODYNAMICS,
                780, 265, null, 11, -4,
                List.of(ASTEROID_SMELTING_REFINERY, VENUS_ACID_LEACHING), ResearchLocationRequirement.VENUS_HYPERBARIC, "Environmental Test Chamber",
                "Ultra-Refractory Carbides",
                "Tantalum-niobium carbide ceramics withstand 4,000K plasma exhaust and hypersonic aerobraking.",
                List.of("Carbide Nozzle Liners", "Corrosive Acid Liner", "Aerobrake Heat Tiles"),
                Constant.id("corrosive_acid_liner")
        ));
        register(new ResearchNode(
                ORBITAL_NANOFABRICATION, 5, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.STRUCTURES,
                960, 330, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, 12, -2,
                List.of(REFRACTORY_CARBIDE_FOUNDRY), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Orbital Molecular Fabrication",
                "Zero-gravity vapor deposition grows defect-free single-crystal turbine blades and metamaterials.",
                List.of("Single-Crystal Alloys", "Zero-G Wafer Fab", "Ultra-Light Truss Struts"),
                Constant.id("cryo_magnet_suspension")
        ));
        register(new ResearchNode(
                SATURN_HELIUM3_HARVESTING, 6, ResearchBranch.METALLURGY_MACHINES, ResearchDomain.CRYOGENICS,
                1350, 440, ResearchMilestone.OUTER_PLANET_OBSERVED, 14, -3,
                List.of(ORBITAL_NANOFABRICATION), ResearchLocationRequirement.forBody(GCCelestialBodies.SATURN.location(), "Saturn Mantle / Atm"), "Geological Analyzer",
                "Saturnian 3He & Mg-Li Alloys",
                "Extracts 3.5x enriched atmospheric Helium-3 and ultralight magnesium-lithium superstructures.",
                List.of("Saturn 3He Fractionation", "Magnesium-Lithium Alloys", "7x7-11x11 Lightweight Hulls"),
                Constant.id("compressed_magnesium")
        ));

        // =========================================================================
        // NORTH-WEST WING: LIFE SUPPORT, BIO-DOMES & BASES (11 nodes)
        // =========================================================================
        register(new ResearchNode(
                BASIC_OXYGEN_GEAR, 1, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.CRYOGENICS,
                35, 0, null, -2, -1,
                List.of(), ResearchLocationRequirement.ANY, "Research Workbench",
                "Pressurized Oxygen Support I",
                "Foundational breathing apparatus and pressurized gas canisters for vacuum extravehicular activity.",
                List.of("Oxygen Mask", "Oxygen Gear", "Small Oxygen Tank", "Oxygen Collector"),
                Constant.id("oxygen_mask")
        ));
        register(new ResearchNode(
                EARTH_VACUUM_SEALING, 1, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.STRUCTURES,
                75, 30, ResearchMilestone.VACUUM_TRIAL_CERTIFIED, -4, -1,
                List.of(BASIC_OXYGEN_GEAR), ResearchLocationRequirement.ANY, "Environmental Test Chamber",
                "Simulated Vacuum & Airlock Seals",
                "Earthside pump-down vacuum testing validates pressurized glass bulkheads and airtight room sealing.",
                List.of("Oxygen Sealer", "Vacuum Glass", "Airlock Frame & Controller"),
                Constant.id("oxygen_sealer")
        ));
        register(new ResearchNode(
                LUNAR_POLAR_ELECTROLYSIS, 2, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.CRYOGENICS,
                135, 50, null, -5, -3,
                List.of(EARTH_VACUUM_SEALING), ResearchLocationRequirement.forBody(GCCelestialBodies.MOON.location(), "Moon Polar Ice"), "Geological Analyzer",
                "Polar Ice Electrolysis & Cryo-Life",
                "Splits dense lunar ice into breathable LOX, LH2 fuel and heavy deuterium isotopes.",
                List.of("Electrolytic Separator", "Cryogenic Chamber", "Oxygen Storage Module"),
                Constant.id("electrolytic_separator")
        ));
        register(new ResearchNode(
                LUNAR_BIO_HABITATION, 2, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.CRYOGENICS,
                190, 70, null, -7, -2,
                List.of(LUNAR_POLAR_ELECTROLYSIS), ResearchLocationRequirement.forBody(GCCelestialBodies.MOON.location(), "Moon Regolith"), "Geological Analyzer",
                "Exobiology & Sealed Hydroponics",
                "Assays lunar flora and regolith volatiles to sustain closed-loop off-world colony nutrition.",
                List.of("Food Canner", "Moon Tangle Cultivation", "Sealed Bio-Planters"),
                Constant.id("food_canner")
        ));
        register(new ResearchNode(
                PRESSURIZED_COLONY_DOMES, 3, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.STRUCTURES,
                280, 100, null, -7, -4,
                List.of(LUNAR_POLAR_ELECTROLYSIS), ResearchLocationRequirement.VACUUM, "Environmental Test Chamber",
                "Modular Geodesic Colony Domes",
                "High-span oxygen bubble projectors and double-pane vacuum glass for large planetary settlements.",
                List.of("Oxygen Bubble Distributor", "Vacuum Turbopump Stage", "Large Habitat Modules"),
                Constant.id("vacuum_turbopump_stage")
        ));
        register(new ResearchNode(
                ZERO_G_CARBON_SOFI, 3, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.STRUCTURES,
                370, 130, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, -9, -3,
                List.of(PRESSURIZED_COLONY_DOMES), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Microgravity Carbon Fiber & SOFI",
                "Zero-gravity polymer spinning yields defect-free carbon weaves and cryogenic SOFI insulation foam.",
                List.of("Carbon Fiber Plate", "SOFI Cryogenic Insulation", "Composite Autoclave"),
                Constant.id("sofi_insulation")
        ));
        register(new ResearchNode(
                CLOSED_ECOSYSTEM_BIOREACTORS, 4, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.CRYOGENICS,
                510, 175, null, -9, -1,
                List.of(LUNAR_BIO_HABITATION, ZERO_G_CARBON_SOFI), ResearchLocationRequirement.forBody(GCCelestialBodies.MARS.location(), "Mars / Orbital Bio-Lab"), "Geological Analyzer",
                "Closed-Loop Algal Bioreactors",
                "Continuous CO2-to-O2 photosynthetic scrubbers eliminate oxygen resupply for deep-space outposts.",
                List.of("Bio-Scrubber Array", "Cryo Thermal Exchanger", "Zero-Loss Water Loop"),
                Constant.id("cryo_thermal_exchanger")
        ));
        register(new ResearchNode(
                RADIATION_SHIELDED_HABITATS, 4, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.NUCLEAR_PLASMA,
                640, 220, ResearchMilestone.VENUS_92ATM_CERTIFIED, -11, -2,
                List.of(CLOSED_ECOSYSTEM_BIOREACTORS), ResearchLocationRequirement.VENUS_HYPERBARIC, "Environmental Test Chamber",
                "Magnetospheric & Lead-Glass Habs",
                "Multi-meter water jackets and lead-doped glass shield crews from solar flares and Jovian belts.",
                List.of("Radiation Bunkers", "Titanium Pressure Manifold", "Hyperbaric Bulkheads"),
                Constant.id("titanium_pressure_manifold")
        ));
        register(new ResearchNode(
                CRYO_HIBERNATION_PODS, 5, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.CRYOGENICS,
                820, 280, null, -11, -4,
                List.of(ZERO_G_CARBON_SOFI, RADIATION_SHIELDED_HABITATS), ResearchLocationRequirement.ZERO_G, "Environmental Test Chamber",
                "Long-Duration Cryogenic Sleep",
                "Metabolic stasis pods enable multi-year transits to Jupiter, Saturn, Uranus and Neptune.",
                List.of("Advanced Cryogenic Chamber", "Biostasis Regulators", "Deep-Transit Crew Pods"),
                Constant.id("environmental_test_chamber")
        ));
        register(new ResearchNode(
                ZERO_G_DILUTION_CRYOGENICS, 5, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.CRYOGENICS,
                1020, 350, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, -13, -3,
                List.of(CRYO_HIBERNATION_PODS), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Sub-2K 3He Dilution Cryogenics",
                "Superfluid Helium-3 dilution refrigerators eliminate propellant boil-off in deep space.",
                List.of("Helium-3 Superfluid Cell (<2K)", "Zero Boil-Off Cryo Tanks", "Milli-Kelvin Containment"),
                Constant.id("helium3_cell")
        ));
        register(new ResearchNode(
                DEEP_SPACE_ARCOLOGY_DOMES, 6, ResearchBranch.LIFE_SUPPORT_BASES, ResearchDomain.STRUCTURES,
                1420, 460, ResearchMilestone.DEEP_SKY_CATALOGED, -15, -3,
                List.of(ZERO_G_DILUTION_CRYOGENICS), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Self-Sustaining Deep Arcologies",
                "Autonomous kilometer-scale rotating biosphere habitats engineered for interstellar expeditions.",
                List.of("Arcology Core", "Artificial Gravity Rings", "Permanent Outer-Rim Colonies"),
                Constant.id("zero_g_research_chamber")
        ));

        // =========================================================================
        // NORTH WING: ENERGY, COMMNET & SUPERCONDUCTORS (11 nodes)
        // =========================================================================
        register(new ResearchNode(
                EARLY_SOLAR_PHOTOVOLTAICS, 1, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.ELECTROMAGNETISM,
                45, 15, null, 0, -2,
                List.of(EARTH_PROTOTYPING), ResearchLocationRequirement.ANY, "Research Workbench",
                "Silicon Photovoltaics & Batteries",
                "Crystalline silicon wafers convert sunlight into regulated DC current stored in portable cells.",
                List.of("Basic Solar Panel", "Basic Wafer", "Energy Storage Module"),
                Constant.id("basic_wafer")
        ));
        register(new ResearchNode(
                HIGH_VOLTAGE_CONDUITS, 1, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.ELECTROMAGNETISM,
                80, 30, null, 0, -4,
                List.of(EARLY_SOLAR_PHOTOVOLTAICS), ResearchLocationRequirement.ANY, "Research Workbench",
                "Aluminum High-Voltage Grids",
                "Insulated heavy-gauge aluminum power buses and precision voltage switches for laboratory arrays.",
                List.of("Heavy Aluminum Wire", "Energy Storage Cluster", "Advanced Wafer"),
                Constant.id("advanced_wafer")
        ));
        register(new ResearchNode(
                LUNAR_HE3_THERMAL_DESORPTION, 2, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.NUCLEAR_PLASMA,
                150, 55, ResearchMilestone.LUNAR_HE3_SPECTRUM, -2, -5,
                List.of(HIGH_VOLTAGE_CONDUITS), ResearchLocationRequirement.forBody(GCCelestialBodies.MOON.location(), "Moon Ilmenite"), "Geological Analyzer",
                "Lunar 3He Thermal Desorption",
                "Confirms 10x solar-wind Helium-3 retention inside lunar ilmenite crystals above 700C.",
                List.of("Regolith Thermal Desorber", "Solar Volatile Gas Extraction"),
                Constant.id("regolith_thermal_desorber")
        ));
        register(new ResearchNode(
                INTERPLANETARY_SCIENCE_NETWORK, 2, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.ELECTROMAGNETISM,
                200, 75, ResearchMilestone.COMMNET_RELAY_LOCKED, 2, -5,
                List.of(HIGH_VOLTAGE_CONDUITS), ResearchLocationRequirement.ANY, "Research Data Link",
                "Interplanetary Science Data Link",
                "Frequency-locked deep-space relays synchronize multi-planet laboratory discoveries in real time.",
                List.of("Research Data Link", "Parabolic Dish Extender", "+20%/Relay Bonus"),
                Constant.id("frequency_module")
        ));
        register(new ResearchNode(
                HIGH_GAIN_COMMNET_ARRAYS, 3, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.ELECTROMAGNETISM,
                295, 110, ResearchMilestone.COMMNET_RELAY_LOCKED, 3, -7,
                List.of(INTERPLANETARY_SCIENCE_NETWORK), ResearchLocationRequirement.VACUUM, "Research Data Link",
                "Ka-Band Phased CommNet Arrays",
                "Phase-locked Ka-band uplinks eliminate interplanetary signal drift and boost science telemetry.",
                List.of("Phase-Locked Loop Module", "Deep-Space Ka-Band Feed", "Global Relay Telemetry"),
                Constant.id("deep_space_ka_band_feed")
        ));
        register(new ResearchNode(
                ZERO_G_SUPERCONDUCTORS_NB3SN, 3, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.ELECTROMAGNETISM,
                400, 145, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, 0, -7,
                List.of(LUNAR_HE3_THERMAL_DESORPTION, INTERPLANETARY_SCIENCE_NETWORK), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Zero-G Nb3Sn Superconductors",
                "Microgravity crystal growth eliminates grain boundaries in Nb3Sn superconducting magnet coils.",
                List.of("Superconducting Coil (Nb3Sn)", "Superconducting Confinement", "Short-Range Telepad"),
                Constant.id("superconducting_coil")
        ));
        register(new ResearchNode(
                MERCURY_SOLAR_VOLATILES_HE3, 4, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.THERMODYNAMICS,
                520, 180, null, -3, -7,
                List.of(LUNAR_HE3_THERMAL_DESORPTION), ResearchLocationRequirement.forBody(GCCelestialBodies.MERCURY.location(), "Mercury (6.6x Solar)"), "Geological Analyzer",
                "Mercurian Solar Volatile 3He",
                "Analyzes Mercury hollow deposits saturated by 6.6x solar wind for 3x high-yield 3He sublimation.",
                List.of("3x Mercury Hollow Desorption", "Solar Array Wafer", "Advanced Solar Panel"),
                Constant.id("solar_array_panel")
        ));
        register(new ResearchNode(
                MERCURY_THORIUM_ISOTOPES, 4, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.NUCLEAR_PLASMA,
                660, 230, null, -2, -9,
                List.of(MERCURY_SOLAR_VOLATILES_HE3, ZERO_G_SUPERCONDUCTORS_NB3SN), ResearchLocationRequirement.forBody(GCCelestialBodies.MERCURY.location(), "Mercury Sulfides"), "Geological Analyzer",
                "Isotopic Separation & RTG Cores",
                "Refines exotic Mercurian sulfides and uranium isotopes inside superconducting gas centrifuges.",
                List.of("Isotopic Centrifuge", "Radioisotope Core", "Atomic Battery", "RTG Power"),
                Constant.id("radioisotope_core")
        ));
        register(new ResearchNode(
                QUANTUM_SUPERCOMPUTING_CORES, 5, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.ELECTROMAGNETISM,
                850, 295, null, 2, -9,
                List.of(HIGH_GAIN_COMMNET_ARRAYS, ZERO_G_SUPERCONDUCTORS_NB3SN), ResearchLocationRequirement.ZERO_G, "Aerospace R&D Terminal",
                "Quantum Telemetry & Archive Cores",
                "Cryogenic superconducting qubits boost R&D diminishing-return recovery and autonomous tracking.",
                List.of("Archival Coprocessor Blade", "Quantum Archive Core", "50% Repeat Science Yield"),
                Constant.id("quantum_archive_core")
        ));
        register(new ResearchNode(
                ZERO_G_ANEUTRONIC_MHD_FUSION, 5, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.NUCLEAR_PLASMA,
                1080, 370, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, 0, -11,
                List.of(MERCURY_THORIUM_ISOTOPES, QUANTUM_SUPERCOMPUTING_CORES), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Aneutronic D-3He MHD Fusion",
                "Zero-G superconducting plasma confinement enables clean 2,500-5,000 GJ/t direct MHD fusion.",
                List.of("Helium-3 Fusion Reactor", "Helium-3 Fusion Cell", "Direct Fusion Drive (DFD)"),
                Constant.id("helium3_fusion_reactor")
        ));
        register(new ResearchNode(
                STELLAR_PLASMA_CONFINEMENT, 6, ResearchBranch.ENERGY_SUPERCONDUCTORS, ResearchDomain.NUCLEAR_PLASMA,
                1550, 500, ResearchMilestone.DEEP_SKY_CATALOGED, 0, -13,
                List.of(ZERO_G_ANEUTRONIC_MHD_FUSION), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Compact Stellar Plasma Traps",
                "Closed magnetic bottles harness ultra-dense plasma streams for capital-class orbital power grids.",
                List.of("Stellar Torus Grid", "Infinite Zero-Loss Power Bus", "Deep-Space Warp Telemetry"),
                Constant.id("superconducting_confinement_coil")
        ));

        // =========================================================================
        // SOUTH-WEST WING: SUITS, METROLOGY, ARMOR & TOOLS (11 nodes)
        // =========================================================================
        register(new ResearchNode(
                FIELD_METROLOGY_INSTRUMENTS, 1, ResearchBranch.SUITS_TOOLS, ResearchDomain.STRUCTURES,
                40, 15, null, -2, 1,
                List.of(EARTH_PROTOTYPING), ResearchLocationRequirement.ANY, "Research Workbench",
                "Vernier Calipers & Lab Instruments",
                "Micrometer gauges and optical drafting tools tighten engineering tolerances across all benches.",
                List.of("Vernier Caliper Rig", "Precision Drafting Stylus", "Optical Blueprint Projector"),
                Constant.id("vernier_caliper_rig")
        ));
        register(new ResearchNode(
                REINFORCED_EVA_TOOLS, 1, ResearchBranch.SUITS_TOOLS, ResearchDomain.STRUCTURES,
                80, 30, null, -4, 1,
                List.of(FIELD_METROLOGY_INSTRUMENTS), ResearchLocationRequirement.ANY, "Research Workbench",
                "Heavy-Duty EVA Tools & Drills",
                "Torque-balanced wrenches, core drills and steel-grade field armor for early orbital sorties.",
                List.of("Standard Wrench", "Heavy EVA Drills", "Steel Alloy Armor"),
                Constant.id("precision_drafting_stylus")
        ));
        register(new ResearchNode(
                LUNAR_OLIVINE_OPTICS, 2, ResearchBranch.SUITS_TOOLS, ResearchDomain.THERMODYNAMICS,
                140, 50, null, -5, 3,
                List.of(REINFORCED_EVA_TOOLS), ResearchLocationRequirement.forBody(GCCelestialBodies.MOON.location(), "Moon Olivine/Sapphire"), "Geological Analyzer",
                "Olivine Ablatives & Sapphire Optics",
                "Lunar olivine silicates and corundum sapphires enable refractory TPS tiles and sensor lenses.",
                List.of("Olivine Glass", "Ablative Heat Tile", "Sensor Glasses", "Beam Core"),
                Constant.id("ablative_heat_tile")
        ));
        register(new ResearchNode(
                PRECISION_SPECTROSCOPY_OPTICS, 2, ResearchBranch.SUITS_TOOLS, ResearchDomain.ELECTROMAGNETISM,
                210, 80, null, -7, 2,
                List.of(LUNAR_OLIVINE_OPTICS), ResearchLocationRequirement.forBody(GCCelestialBodies.MOON.location(), "Moon / Mars Lab"), "Geological Analyzer",
                "Laser Raman & Mass Sensors",
                "Tunable Raman lasers and quadrupole mass filters double rare mineral extraction during assays.",
                List.of("Raman Laser Spectrometer", "Quadrupole Mass Filter", "Micro-XRF Emitter"),
                Constant.id("raman_laser_spectrometer")
        ));
        register(new ResearchNode(
                MARTIAN_DESH_ELASTOMERS, 3, ResearchBranch.SUITS_TOOLS, ResearchDomain.STRUCTURES,
                300, 110, null, -7, 4,
                List.of(LUNAR_OLIVINE_OPTICS), ResearchLocationRequirement.forBody(GCCelestialBodies.MARS.location(), "Mars Desh"), "Geological Analyzer",
                "Martian Desh Alloys & Valves",
                "Elastic iron-titanium Desh mineralogy unlocks high-shock armor, tools and precision valves.",
                List.of("Desh Armor & Tools", "Atmospheric Valve", "Fluid Manipulator", "Isothermal Fabric"),
                Constant.id("compressed_desh")
        ));
        register(new ResearchNode(
                THERMAL_EXOSUIT_SYSTEMS, 3, ResearchBranch.SUITS_TOOLS, ResearchDomain.THERMODYNAMICS,
                390, 140, null, -9, 3,
                List.of(PRECISION_SPECTROSCOPY_OPTICS, MARTIAN_DESH_ELASTOMERS), ResearchLocationRequirement.forBody(GCCelestialBodies.MARS.location(), "Mars / Orbit"), "Environmental Test Chamber",
                "Active Thermal Control Padding",
                "Multi-layer radiant foil and active coolant weave regulate body heat from -200C to +450C.",
                List.of("Thermal Padding T1", "Cryo Mass Spectrometer", "Isotopic Decay Counter"),
                Constant.id("isothermal_fabric")
        ));
        register(new ResearchNode(
                ASTEROID_TITANIUM_COMPOSITES, 4, ResearchBranch.SUITS_TOOLS, ResearchDomain.STRUCTURES,
                530, 185, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, -10, 1,
                List.of(THERMAL_EXOSUIT_SYSTEMS), ResearchLocationRequirement.ZERO_G, "Geological Analyzer",
                "Asteroid Ilmenite Titanium Grade",
                "Metallic M-type asteroid ilmenite cuts aerospace dry weight by 40% with titanium armor and COPVs.",
                List.of("Titanium Armor & Tools", "Composite Autoclave", "COPV Pressure Vessels"),
                Constant.id("compressed_titanium")
        ));
        register(new ResearchNode(
                VENUS_HYPERBARIC_HULLS, 4, ResearchBranch.SUITS_TOOLS, ResearchDomain.STRUCTURES,
                670, 235, ResearchMilestone.VENUS_92ATM_CERTIFIED, -11, 4,
                List.of(THERMAL_EXOSUIT_SYSTEMS, ASTEROID_TITANIUM_COMPOSITES), ResearchLocationRequirement.VENUS_HYPERBARIC, "Environmental Test Chamber",
                "92 atm Hyperbaric & Acid Armor",
                "Live Venusian 92 atm sulfuric acid chamber trials certify lead anti-acid plating and T2 isothermal suits.",
                List.of("Lead Anti-Acid Coatings", "Isothermal Padding T2", "Hyperbaric Capsules", "Geothermal Gen"),
                Constant.id("lead_ingot")
        ));
        register(new ResearchNode(
                POWERED_EVA_EXOSKELETONS, 5, ResearchBranch.SUITS_TOOLS, ResearchDomain.STRUCTURES,
                840, 290, null, -12, 2,
                List.of(VENUS_HYPERBARIC_HULLS), ResearchLocationRequirement.forBody(GCCelestialBodies.JUPITER.location(), "High-G Surface"), "Environmental Test Chamber",
                "Servo-Assisted High-G Exosuits",
                "Gyro-stabilized actuators let astronauts walk, carry heavy payloads and build under 2.36G gravity.",
                List.of("Piezo Gyro Gimbal", "High-G Servo Exosuit", "Gravity Dampening Boots"),
                Constant.id("piezo_gyro_gimbal")
        ));
        register(new ResearchNode(
                PLASMA_EXCAVATION_TOOLS, 5, ResearchBranch.SUITS_TOOLS, ResearchDomain.THERMODYNAMICS,
                1040, 360, null, -13, 4,
                List.of(POWERED_EVA_EXOSKELETONS), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Magnetic Plasma Mining Cutters",
                "Focused plasma lances slice ultra-dense planetary mantles and metallic asteroid cores in seconds.",
                List.of("Plasma Bore Cutter", "Magnetic Ore Extractor", "Thermal Lance Shielding"),
                Constant.id("quadrupole_mass_filter")
        ));
        register(new ResearchNode(
                URANUS_LONSDALEITE_PRESSURE, 6, ResearchBranch.SUITS_TOOLS, ResearchDomain.STRUCTURES,
                1480, 480, ResearchMilestone.OUTER_PLANET_OBSERVED, -15, 3,
                List.of(PLASMA_EXCAVATION_TOOLS), ResearchLocationRequirement.forBody(GCCelestialBodies.URANUS.location(), "Uranus Abyssal Ice"), "Geological Analyzer",
                "Hexagonal Lonsdaleite Lattices",
                "High-pressure hexagonal diamond lattices endure 100+ atm abyssal descent and deep mining.",
                List.of("Lonsdaleite Pressure Coatings", "Abyssal Command Domes", "Lonsdaleite Mirror Blank"),
                Constant.id("lonsdaleite_gem")
        ));

        // =========================================================================
        // SOUTH-EAST WING: AEROSPACE, ASTRONOMY & PROPULSION (11 nodes)
        // =========================================================================
        register(new ResearchNode(
                EARTH_LUNAR_EPHEMERIS, 1, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.ELECTROMAGNETISM,
                45, 20, ResearchMilestone.MOON_OBSERVED, 2, 1,
                List.of(), ResearchLocationRequirement.ANY, "Observatory Telescope",
                "Earth-Moon Orbital Ephemeris",
                "Night-sky optical tracking from Earth maps Lunar transfer windows and Low Earth Orbit.",
                List.of("Moon Star-Chart Route", "Low Earth Orbit Telemetry", "Optical Achromat Lens"),
                Constant.id("optical_achromat_lens")
        ));
        register(new ResearchNode(
                ERA1_ROCKETRY, 1, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.THERMODYNAMICS,
                85, 35, null, 1, 3,
                List.of(EARTH_PROTOTYPING, EARTH_LUNAR_EPHEMERIS), ResearchLocationRequirement.ANY, "Research Workbench",
                "Orbital RP-1 Launch Architecture",
                "Integrates kerosene refining, reinforced pads and 1x1-3x3 modular rocketry for lunar missions.",
                List.of("Refinery", "Rocket Launch Pad", "Rocket Workbench", "1x1-3x3 RP-1 Rocketry"),
                Constant.id("rocket_workbench")
        ));
        register(new ResearchNode(
                LUNAR_DEEP_SKY_SURVEY, 2, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.ELECTROMAGNETISM,
                155, 60, ResearchMilestone.MARS_OBSERVED, 4, 2,
                List.of(EARTH_LUNAR_EPHEMERIS, ERA1_ROCKETRY), ResearchLocationRequirement.VACUUM, "Observatory Telescope",
                "Vacuum Deep-Sky Astrometry",
                "Atmospheric-free lunar astronomy resolves Mars, Venus, Mercury and Asteroid Belt trajectories.",
                List.of("Mars Route", "Venus Route", "Mercury Route", "Asteroid Belt Route"),
                Constant.id("sapphire_cassegrain_mirror")
        ));
        register(new ResearchNode(
                HIGH_ALTITUDE_OPTICS_DOMES, 2, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.ELECTROMAGNETISM,
                220, 85, null, 3, 4,
                List.of(LUNAR_DEEP_SKY_SURVEY), ResearchLocationRequirement.VACUUM, "Observatory Telescope",
                "2x2 & 3x3 Adaptive Observatories",
                "Motorized sidereal mounts and adaptive deformable mirrors reach 220x-500x optical resolution.",
                List.of("Equatorial Sidereal Drive", "Adaptive Optics Actuator", "500x Great Observatory"),
                Constant.id("adaptive_optics_actuator")
        ));
        register(new ResearchNode(
                OUTER_SYSTEM_EPHEMERIS, 3, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.ELECTROMAGNETISM,
                320, 120, ResearchMilestone.OUTER_PLANET_OBSERVED, 6, 3,
                List.of(LUNAR_DEEP_SKY_SURVEY, HIGH_ALTITUDE_OPTICS_DOMES), ResearchLocationRequirement.forBodies(
                        List.of(GCCelestialBodies.MARS.location(), GCCelestialBodies.ASTEROID.location()),
                        "Mars / Orbit / Asteroids"),
                "Observatory Telescope",
                "Outer Solar System Ephemeris",
                "Deep-space parallax baseline from Mars, Orbit or Asteroids maps Jupiter, Saturn, Uranus and Neptune.",
                List.of("Jupiter Route", "Saturn Route", "Uranus Route", "Neptune Route"),
                Constant.id("beam_core")
        ));
        register(new ResearchNode(
                METHALOX_HEAVY_ENGINES, 3, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.THERMODYNAMICS,
                420, 155, null, 6, 5,
                List.of(HIGH_ALTITUDE_OPTICS_DOMES, OUTER_SYSTEM_EPHEMERIS), ResearchLocationRequirement.forBody(GCCelestialBodies.MARS.location(), "Mars Launch Pad"), "Research Workbench",
                "Full-Flow Methalox Engines",
                "Closed-cycle methane-LOX turbopumps enable reusable multi-planet boosters fueled by Martian ISRU.",
                List.of("Methalox Engine Bell", "5x5 Medium-Lift Core", "Reusable Booster Fins"),
                Constant.id("observatory_telescope")
        ));
        register(new ResearchNode(
                ORBITAL_SHIPYARD_DOCKS, 4, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.STRUCTURES,
                550, 195, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, 8, 4,
                List.of(OUTER_SYSTEM_EPHEMERIS, METHALOX_HEAVY_ENGINES), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "Zero-G Modular Space Stations",
                "Orbital docking collars, fuel depots and microgravity drydocks eliminate Earth launch gravity drag.",
                List.of("Space Station Core", "Orbital Fuel Depot", "Docking Ring Telemetry"),
                Constant.id("research_data_link")
        ));
        register(new ResearchNode(
                ZERO_G_HEAVY_ORBITAL_WELDING, 4, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.STRUCTURES,
                700, 245, ResearchMilestone.ZERO_G_LEVITATION_CERTIFIED, 9, 2,
                List.of(ORBITAL_SHIPYARD_DOCKS), ResearchLocationRequirement.ZERO_G, "Zero-G Research Chamber",
                "0G Heavy 7x7-11x11 Orbital Welding",
                "Weightless electron-beam welding assembles massive 7x7, 9x9 and 11x11 interplanetary hulls.",
                List.of("7x7-11x11 Heavy Frames", "Orbital Shipyards", "Aero Refurbishment Bench"),
                Constant.id("aero_refurbishment_bench")
        ));
        register(new ResearchNode(
                JUPITER_PALLADIUM_CATALYSIS, 5, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.THERMODYNAMICS,
                890, 310, ResearchMilestone.OUTER_PLANET_OBSERVED, 10, 5,
                List.of(ORBITAL_SHIPYARD_DOCKS, ZERO_G_HEAVY_ORBITAL_WELDING), ResearchLocationRequirement.forBody(GCCelestialBodies.JUPITER.location(), "Jupiter (2.36G)"), "Geological Analyzer",
                "Supercritical Palladium Engines",
                "Jovian palladium catalysis and metallic hydrogen fuel high-T/W heavy lift against 2.36G gravity.",
                List.of("Palladium-Niobium Nozzles", "Metallic Hydrogen Cells", "Shield Controller"),
                Constant.id("compressed_palladium")
        ));
        register(new ResearchNode(
                DEEP_SPACE_OPTICAL_INTERFEROMETRY, 5, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.ELECTROMAGNETISM,
                1100, 385, ResearchMilestone.DEEP_SKY_CATALOGED, 12, 3,
                List.of(JUPITER_PALLADIUM_CATALYSIS), ResearchLocationRequirement.ZERO_G, "Observatory Telescope",
                "Interplanetary Optical Atlas",
                "Infrared bolometers and lonsdaleite mirrors map nebulae, galaxies and Kuiper comets for 2x science.",
                List.of("Infrared Bolometer Array", "Lonsdaleite Primary Mirror", "Interstellar Star Charts"),
                Constant.id("infrared_bolometer_array")
        ));
        register(new ResearchNode(
                NEPTUNE_SUPERIONIC_MAGNETOPLASMA, 6, ResearchBranch.AEROSPACE_PROPULSION, ResearchDomain.NUCLEAR_PLASMA,
                1600, 520, ResearchMilestone.DEEP_SKY_CATALOGED, 14, 4,
                List.of(JUPITER_PALLADIUM_CATALYSIS, DEEP_SPACE_OPTICAL_INTERFEROMETRY), ResearchLocationRequirement.forBody(GCCelestialBodies.NEPTUNE.location(), "Neptune (-200C)"), "Geological Analyzer",
                "Superionic VASIMR Magnetoplasma",
                "Neptunian superionic ice crystals and heavy xenon enable frontier Hall/Ion and VASIMR drives.",
                List.of("VASIMR Plasma Drive", "Xenon Hall Thrusters", "Superionic Magnet Coils"),
                Constant.id("superionic_ice_crystal")
        ));
    }

    private ResearchCatalog() {
    }

    private static void register(@NotNull ResearchNode node) {
        NODES.put(node.id(), node);
        BY_BRANCH.get(node.branch()).add(node);
    }

    public static @NotNull Collection<ResearchNode> allNodes() {
        return Collections.unmodifiableCollection(NODES.values());
    }

    public static @Nullable ResearchNode get(@Nullable ResourceLocation id) {
        return id == null ? null : NODES.get(id);
    }

    public static @NotNull List<ResearchNode> nodesForBranch(@NotNull ResearchBranch branch) {
        return Collections.unmodifiableList(BY_BRANCH.getOrDefault(branch, List.of()));
    }

    public static int totalNodeCount() {
        return NODES.size();
    }
}
