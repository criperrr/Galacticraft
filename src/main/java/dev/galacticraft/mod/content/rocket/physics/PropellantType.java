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

import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.GCFluids;
import dev.galacticraft.mod.tag.GCFluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Physical propellant chemistry families used by modular rocket engines and tanks,
 * defining specific impulse ({@code Isp}), bulk density, cryogenic boil-off rate,
 * and fluid compatibility.
 */
public enum PropellantType {
    RP1("RP-1 / Kerosene", 310.0, 820.0, false, 0.0),
    SOLID("Solid Composite", 265.0, 1750.0, false, 0.0),
    METHALOX("Methalox (CH4/LOX)", 365.0, 850.0, true, 0.008),
    HYDROLOX("Hydrolox (LH2/LOX)", 455.0, 450.0, true, 0.020),
    NERVA("Nuclear Thermal (LH2)", 880.0, 350.0, true, 0.020),
    XENON_ELECTRIC("Hall / VASIMR (Xe)", 3200.0, 1600.0, true, 0.004),
    MONOPROPELLANT("Monopropellant (N2H4)", 235.0, 1020.0, false, 0.0);

    private final String displayName;
    private final double baseIspSeconds;
    private final double densityKgPerBucket;
    private final boolean cryogenic;
    private final double baseBoilOffFractionPerMinute;

    PropellantType(
            String displayName,
            double baseIspSeconds,
            double densityKgPerBucket,
            boolean cryogenic,
            double baseBoilOffFractionPerMinute
    ) {
        this.displayName = displayName;
        this.baseIspSeconds = baseIspSeconds;
        this.densityKgPerBucket = densityKgPerBucket;
        this.cryogenic = cryogenic;
        this.baseBoilOffFractionPerMinute = baseBoilOffFractionPerMinute;
    }

    public @NotNull String displayName() {
        return this.displayName;
    }

    public double baseIspSeconds() {
        return this.baseIspSeconds;
    }

    public double densityKgPerBucket() {
        return this.densityKgPerBucket;
    }

    public boolean isCryogenic() {
        return this.cryogenic;
    }

    /**
     * Baseline fraction of stored propellant boiled off per real-time minute at 293.15 K (20 C)
     * in an uninsulated metallic tank without active cooling.
     */
    public double baseBoilOffFractionPerMinute() {
        return this.baseBoilOffFractionPerMinute;
    }

    /**
     * Resolves the propellant chemistry required by a modular engine configuration.
     */
    public static @NotNull PropellantType forEngine(@NotNull RocketPartConfig config) {
        String id = combinedId(config);
        if (id.contains("vasimr") || id.contains("plasma") || id.contains("hall") || id.contains("ion")) {
            return XENON_ELECTRIC;
        }
        if (id.contains("nerva") || id.contains("ntr")) {
            return NERVA;
        }
        if (id.contains("rl10") || id.contains("vulcain") || id.contains("ssme") || id.contains("j2")
                || id.contains("hydrolox")) {
            return HYDROLOX;
        }
        if (id.contains("raptor") || id.contains("be4") || id.contains("starcluster") || id.contains("methalox")) {
            return METHALOX;
        }
        if (id.contains("srb") || id.contains("solid")) {
            return SOLID;
        }
        if (id.contains("rcs") || id.contains("draco") || id.contains("oms") || id.contains("monoprop")) {
            return MONOPROPELLANT;
        }
        return RP1;
    }

    /**
     * Resolves the primary propellant chemistry designed for a modular tank configuration.
     */
    public static @NotNull PropellantType forTank(@NotNull RocketPartConfig config) {
        String id = combinedId(config);
        if (id.contains("xenon")) {
            return XENON_ELECTRIC;
        }
        if (id.contains("hydrolox") || id.contains("supercryo")) {
            return HYDROLOX;
        }
        if (id.contains("methalox")) {
            return METHALOX;
        }
        if (id.contains("lox") || id.contains("cryo")) {
            return METHALOX;
        }
        if (id.contains("monoprop")) {
            return MONOPROPELLANT;
        }
        return RP1;
    }

    /**
     * Resolves the propellant chemistry represented by an arbitrary fluid stored in a tank.
     */
    public static @Nullable PropellantType fromFluid(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return null;
        }
        if (fluid == GCFluids.LIQUID_HYDROGEN || fluid == GCFluids.FLOWING_LIQUID_HYDROGEN) {
            return HYDROLOX;
        }
        if (fluid == GCFluids.LIQUID_METHANE || fluid == GCFluids.FLOWING_LIQUID_METHANE) {
            return METHALOX;
        }
        if (fluid == GCFluids.LIQUID_OXYGEN || fluid == GCFluids.FLOWING_LIQUID_OXYGEN) {
            return METHALOX;
        }
        if (fluid == GCFluids.LIQUID_XENON || fluid == GCFluids.FLOWING_LIQUID_XENON || fluid == Gases.XENON) {
            return XENON_ELECTRIC;
        }
        if (fluid == GCFluids.MONOPROPELLANT || fluid == GCFluids.FLOWING_MONOPROPELLANT) {
            return MONOPROPELLANT;
        }
        if (fluid == GCFluids.FUEL || fluid == GCFluids.FLOWING_FUEL || fluid.is(GCFluidTags.FUEL)) {
            return RP1;
        }
        return null;
    }

    /**
     * Returns whether the given {@code fluid} is a valid rocket propellant accepted by modular
     * launch pad fuel storage and rocket tanks.
     */
    public static boolean isRocketPropellantFluid(@Nullable Fluid fluid) {
        return fromFluid(fluid) != null;
    }

    /**
     * Returns whether the given {@code fluid} is cryogenic and subject to thermal boil-off.
     */
    public static boolean isCryogenicFluid(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        return fluid == GCFluids.LIQUID_OXYGEN
                || fluid == GCFluids.FLOWING_LIQUID_OXYGEN
                || fluid == GCFluids.LIQUID_METHANE
                || fluid == GCFluids.FLOWING_LIQUID_METHANE
                || fluid == GCFluids.LIQUID_HYDROGEN
                || fluid == GCFluids.FLOWING_LIQUID_HYDROGEN
                || fluid == GCFluids.LIQUID_XENON
                || fluid == GCFluids.FLOWING_LIQUID_XENON;
    }

    /**
     * Returns the baseline boil-off fraction per real-time minute at 293.15 K for a specific loaded fluid.
     */
    public static double baseBoilOffFractionForFluid(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return 0.0;
        }
        if (fluid == GCFluids.LIQUID_HYDROGEN || fluid == GCFluids.FLOWING_LIQUID_HYDROGEN) {
            return 0.020; // LH2 boils off fastest (20 K boiling point)
        }
        if (fluid == GCFluids.LIQUID_OXYGEN || fluid == GCFluids.FLOWING_LIQUID_OXYGEN) {
            return 0.010; // LOX (90 K boiling point)
        }
        if (fluid == GCFluids.LIQUID_METHANE || fluid == GCFluids.FLOWING_LIQUID_METHANE) {
            return 0.008; // LCH4 (111 K boiling point)
        }
        if (fluid == GCFluids.LIQUID_XENON || fluid == GCFluids.FLOWING_LIQUID_XENON) {
            return 0.004; // LXe (165 K boiling point)
        }
        return 0.0;
    }

    /**
     * Checks whether a tank propellant chemistry can feed an engine requiring {@code this} chemistry.
     */
    public boolean acceptsTankType(@NotNull PropellantType tankType) {
        if (this == tankType || this == SOLID) {
            return true;
        }
        if (this == NERVA) {
            return tankType == HYDROLOX || tankType == NERVA;
        }
        if (this == RP1) {
            return tankType == RP1 || tankType == METHALOX;
        }
        if (this == MONOPROPELLANT) {
            return tankType == MONOPROPELLANT || tankType == RP1;
        }
        return false;
    }

    /**
     * Checks whether a specific loaded fluid can feed an engine requiring {@code this} chemistry.
     */
    public boolean acceptsFluid(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return this == SOLID;
        }
        return switch (this) {
            case SOLID -> true;
            case RP1 -> fluid == GCFluids.FUEL || fluid == GCFluids.FLOWING_FUEL || fluid.is(GCFluidTags.FUEL)
                    || fluid == GCFluids.LIQUID_OXYGEN || fluid == GCFluids.FLOWING_LIQUID_OXYGEN;
            case METHALOX -> fluid == GCFluids.LIQUID_METHANE || fluid == GCFluids.FLOWING_LIQUID_METHANE
                    || fluid == GCFluids.LIQUID_OXYGEN || fluid == GCFluids.FLOWING_LIQUID_OXYGEN
                    || fluid == GCFluids.FUEL;
            case HYDROLOX, NERVA -> fluid == GCFluids.LIQUID_HYDROGEN || fluid == GCFluids.FLOWING_LIQUID_HYDROGEN
                    || (this == HYDROLOX && (fluid == GCFluids.LIQUID_OXYGEN || fluid == GCFluids.FLOWING_LIQUID_OXYGEN));
            case XENON_ELECTRIC -> fluid == GCFluids.LIQUID_XENON || fluid == GCFluids.FLOWING_LIQUID_XENON
                    || fluid == Gases.XENON;
            case MONOPROPELLANT -> fluid == GCFluids.MONOPROPELLANT || fluid == GCFluids.FLOWING_MONOPROPELLANT
                    || fluid == GCFluids.FUEL;
        };
    }

    /**
     * Resolves a human-readable preferred fluid label for UI diagnostics.
     */
    public @NotNull String requiredFluidLabel() {
        return switch (this) {
            case RP1 -> "Fuel (RP-1)";
            case SOLID -> "Solid Grain (Internal)";
            case METHALOX -> "Liquid Methane / LOX";
            case HYDROLOX -> "Liquid Hydrogen / LOX";
            case NERVA -> "Liquid Hydrogen (LH2)";
            case XENON_ELECTRIC -> "Liquid Xenon / Xe Gas";
            case MONOPROPELLANT -> "Monopropellant";
        };
    }

    /**
     * Returns whether a modular part config represents an active Stirling cryocooler upgrade module.
     */
    public static boolean isCryocoolerModule(@NotNull RocketPartConfig config) {
        if (config.category() != RocketPartCategory.TANK && config.category() != RocketPartCategory.UTILITY) {
            return false;
        }
        return combinedId(config).contains("cryocooler");
    }

    private static String combinedId(RocketPartConfig config) {
        return (config.familyId() + "_" + config.modelId()).toLowerCase(Locale.ROOT);
    }
}
