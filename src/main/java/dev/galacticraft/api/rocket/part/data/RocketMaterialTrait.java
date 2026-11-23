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

package dev.galacticraft.api.rocket.part.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Passive aerospace engineering traits conferred by structural materials and functional sub-components
 * installed into modular rocket parts.
 */
public enum RocketMaterialTrait implements StringRepresentable {
    // 14 Structural Material Traits
    LIGHTWEIGHT_ALLOY("lightweight_alloy", "Lightweight Alloy", 0xD8E2EC,
            "Reduces launch delta-v consumption with a smaller thermal margin"),
    THERMAL_DUCTILITY("thermal_ductility", "Thermal Ductility", 0xCD7F32,
            "Equalizes thermal loads across adjacent rocket parts"),
    BRUTE_RIGIDITY("brute_rigidity", "Brute Rigidity", 0x8A959E,
            "Reduces structural integrity damage from hard landings by 20%"),
    HSLA_MICROALLOY("hsla_microalloy", "HSLA Microalloy", 0x6E8CA0,
            "High pressure tolerance without the mass penalty of conventional steel"),
    MICROMETEOR_SHIELD("micrometeor_shield", "Micrometeor Shield", 0x6D635B,
            "Immune to micrometeorite pitting and abrasive regolith dust wear"),
    ELASTIC_MEMORY("elastic_memory", "Elastic Memory", 0xC45A3D,
            "Absorbs 50% of heavy engine vibration and landing shock loads"),
    AEROSPACE_GRADE("aerospace_grade", "Aerospace Grade", 0xA8C8DC,
            "Superior strength-to-mass ratio with +15% structural efficiency"),
    ACID_RADIATION_SHIELD("acid_radiation_shield", "Acid & Radiation Shield", 0x5C6478,
            "Immune to sulfuric acid corrosion and blocks 80% of ionizing radiation"),
    COPV_WEAVE("copv_weave", "COPV Weave", 0x3A3F47,
            "Minimal structural mass; +25% internal volume in pressurized tanks"),
    VACUUM_REFRACTORY("vacuum_refractory", "Vacuum Refractory", 0x7BA4D0,
            "Zero thermal fatigue during sustained engine burns and cryogenic shocks"),
    SUPERCRITICAL_CATALYSIS("supercritical_catalysis", "Supercritical Catalysis", 0xE6D5A8,
            "+20% engine thrust and -15% propellant consumption in supercritical chambers"),
    ULTRALIGHT_SUPERFRAME("ultralight_superframe", "Ultralight Superframe", 0xC6D8C0,
            "Ultra-low dry mass alloy enabling heavy 7x7 to 11x11 launch vehicles"),
    CRYSTALLINE_LATTICE("crystalline_lattice", "Crystalline Lattice", 0x7FE8FF,
            "Hexagonal diamond lattice built for extreme >100 atm planetary pressures"),
    MAGNETIC_CONFINEMENT("magnetic_confinement", "Magnetic Confinement", 0x5EE0FF,
            "Eliminates resistive losses and amplifies ion/plasma thrust by +35%"),

    // Functional Sub-Component Traits
    ATMOSPHERIC_PARACHUTE("atmospheric_parachute", "Atmospheric Parachute", 0xF0F0F0,
            "Enables soft atmospheric descent without expending landing propellant"),
    SUPERSONIC_CHUTE("supersonic_chute", "Supersonic Chute", 0xFF9F43,
            "High-Mach thermal deceleration canopy for thin or scorching atmospheres"),
    ABLATIVE_TPS("ablative_tps", "Ablative TPS", 0x8FBC5A,
            "Sacrificial olivine thermal protection adding +1,200 K reentry margin"),
    REFRACTORY_CANOPY("refractory_canopy", "Refractory Canopy", 0x98D96B,
            "Reinforced olivine viewport resisting high atmospheric pressure and heat"),
    FLIGHT_COMPUTER("flight_computer", "Flight Computer", 0x55B8FF,
            "Digital guidance reducing fuel burn by 10% and expanding battery reserve by 25%"),
    PROSPECTING_HUD("prospecting_hud", "Prospecting HUD", 0x62E6B5,
            "Optical telemetry highlighting surface ores and terrain hazards"),
    REMOTE_TELEMETRY("remote_telemetry", "Remote Telemetry", 0x72C3FF,
            "Enables uncrewed remote flight control and automated pad landing"),
    ACTIVE_CLIMATE_CONTROL("active_climate_control", "Active Climate Control", 0x52D1DC,
            "Maintains habitable cabin temperature across extreme planetary environments"),
    SUPERCONDUCTING_BUS("superconducting_bus", "Superconducting Bus", 0x5CA8FF,
            "Nb3Sn power bus doubling electrical storage and enabling instant MHD ignition"),
    SOFI_INSULATION("sofi_insulation", "SOFI Insulation", 0xE68A3E,
            "Spray-on cryogenic foam cutting LOX/CH4/LH2 boil-off by up to 85%"),
    PRESSURE_RELIEF_PURGE("pressure_relief_purge", "Pressure Relief Purge", 0xA4B5C4,
            "Automatic overpressure venting prevents catastrophic propellant tank rupture"),
    HIGH_FLOW_MANIFOLD("high_flow_manifold", "High-Flow Manifold", 0xF5B041,
            "+25% propellant transfer rate and +10% coupled engine feed thrust"),
    ACTIVE_ZERO_BOILOFF("active_zero_boiloff", "Active Zero Boil-Off", 0x45C4F7,
            "Eliminates cryogenic boil-off using active onboard cryocooler power"),
    STAGED_COMBUSTION("staged_combustion", "Staged Combustion", 0xFF6B35,
            "Closed-cycle turbopump delivering +35% thrust and +15% propellant economy"),
    AERODYNAMIC_SEPARATION("aerodynamic_separation", "Aerodynamic Separation", 0xB8C7D9,
            "Low-drag pneumatic fairing and interstage separation hardware"),
    NUCLEAR_THERMAL_CORE("nuclear_thermal_core", "Nuclear Thermal Core", 0x80FF44,
            "High-energy fission core for NERVA propulsion and deep-space RTG modules"),
    ION_BEAM_EMITTER("ion_beam_emitter", "Ion Beam Emitter", 0x6EC6FF,
            "Electromagnetic Hall/ion plasma accelerator grid with high specific impulse"),
    ANEUTRONIC_BOARD_REACTOR("aneutronic_board_reactor", "Aneutronic Board Reactor", 0x70F8FF,
            "Clean D-3He onboard reactor supplying +120 GJ/t continuously without heavy shielding"),
    SUPERFLUID_CRYO_2K("superfluid_cryo_2k", "Superfluid Cryo <2K", 0x3CE0FF,
            "Helium-3 dilution cooling below 2K with 0% boil-off and zero electrical drain"),
    ANEUTRONIC_HE3_PLASMA("aneutronic_he3_plasma", "Aneutronic He-3 Plasma", 0x8C7BFF,
            "Direct Fusion Drive cycle: +45% thrust, +40% Isp, and net-positive onboard power"),
    MILLIKELVIN_CONTAINMENT("millikelvin_containment", "Millikelvin Containment", 0x99F0FF,
            "Sub-2K cryogenic vault stabilizing reactive exotic samples during transport");

    public static final Codec<RocketMaterialTrait> CODEC = StringRepresentable.fromEnum(RocketMaterialTrait::values);

    private static final IntFunction<RocketMaterialTrait> BY_ID = ByIdMap.continuous(
            RocketMaterialTrait::ordinal,
            values(),
            ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, RocketMaterialTrait> STREAM_CODEC = ByteBufCodecs.idMapper(
            BY_ID,
            RocketMaterialTrait::ordinal
    );

    private static final Map<String, RocketMaterialTrait> BY_NAME;

    static {
        Map<String, RocketMaterialTrait> map = new HashMap<>();
        for (RocketMaterialTrait trait : values()) {
            map.put(trait.serializedName, trait);
        }
        BY_NAME = Collections.unmodifiableMap(map);
    }

    private final String serializedName;
    private final String displayName;
    private final int rgbColor;
    private final String description;

    RocketMaterialTrait(String serializedName, String displayName, int rgbColor, String description) {
        this.serializedName = serializedName;
        this.displayName = displayName;
        this.rgbColor = rgbColor & 0xFFFFFF;
        this.description = description;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public @NotNull String displayName() {
        return this.displayName;
    }

    public int rgbColor() {
        return this.rgbColor;
    }

    public @NotNull String description() {
        return this.description;
    }

    public @NotNull String translationKey() {
        return "tooltip.galacticraft.rocket_trait." + this.serializedName;
    }

    public @NotNull String descriptionKey() {
        return "tooltip.galacticraft.rocket_trait." + this.serializedName + ".desc";
    }

    public static @Nullable RocketMaterialTrait byName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }
}
