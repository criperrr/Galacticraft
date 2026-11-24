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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Data-driven physical location requirement for in-situ planetary research experiments.
 * Evaluates actual dimension/body astrophysical properties (gravity, pressure, temperature, acid, sky view).
 */
public record ResearchLocationRequirement(
        @NotNull List<ResourceLocation> allowedBodies,
        double minGravityG,
        double maxGravityG,
        double minPressureAtm,
        double maxPressureAtm,
        double minTempKelvin,
        double maxTempKelvin,
        boolean requireCorrosiveAcid,
        boolean requireSkyView,
        @NotNull String label
) {
    public static final ResearchLocationRequirement ANY = new ResearchLocationRequirement(
            List.of(), 0.0, 100.0, 0.0, 1000.0, 0.0, 5000.0, false, false, "Any Base"
    );

    public static final ResearchLocationRequirement ZERO_G = new ResearchLocationRequirement(
            List.of(), 0.0, 0.01, 0.0, 1000.0, 0.0, 5000.0, false, false, "0.0G Orbit / Asteroids"
    );

    public static final ResearchLocationRequirement VACUUM = new ResearchLocationRequirement(
            List.of(), 0.0, 100.0, 0.0, 0.01, 0.0, 5000.0, false, false, "Vacuum (<=0.01 atm)"
    );

    public static final ResearchLocationRequirement VENUS_HYPERBARIC = new ResearchLocationRequirement(
            List.of(), 0.0, 100.0, 50.0, 1000.0, 600.0, 5000.0, true, false, "Hyperbaric Acid (>=50 atm)"
    );

    public static final Codec<ResearchLocationRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("allowed_bodies", List.of()).forGetter(ResearchLocationRequirement::allowedBodies),
            Codec.DOUBLE.optionalFieldOf("min_gravity_g", 0.0).forGetter(ResearchLocationRequirement::minGravityG),
            Codec.DOUBLE.optionalFieldOf("max_gravity_g", 100.0).forGetter(ResearchLocationRequirement::maxGravityG),
            Codec.DOUBLE.optionalFieldOf("min_pressure_atm", 0.0).forGetter(ResearchLocationRequirement::minPressureAtm),
            Codec.DOUBLE.optionalFieldOf("max_pressure_atm", 1000.0).forGetter(ResearchLocationRequirement::maxPressureAtm),
            Codec.DOUBLE.optionalFieldOf("min_temp_kelvin", 0.0).forGetter(ResearchLocationRequirement::minTempKelvin),
            Codec.DOUBLE.optionalFieldOf("max_temp_kelvin", 5000.0).forGetter(ResearchLocationRequirement::maxTempKelvin),
            Codec.BOOL.optionalFieldOf("require_corrosive_acid", false).forGetter(ResearchLocationRequirement::requireCorrosiveAcid),
            Codec.BOOL.optionalFieldOf("require_sky_view", false).forGetter(ResearchLocationRequirement::requireSkyView),
            Codec.STRING.optionalFieldOf("label", "Any Base").forGetter(ResearchLocationRequirement::label)
    ).apply(instance, ResearchLocationRequirement::new));

    public static ResearchLocationRequirement forBody(@NotNull ResourceLocation bodyId, @NotNull String label) {
        return new ResearchLocationRequirement(
                List.of(bodyId), 0.0, 100.0, 0.0, 1000.0, 0.0, 5000.0, false, false, label
        );
    }

    public static ResearchLocationRequirement forBodies(@NotNull List<ResourceLocation> bodyIds, @NotNull String label) {
        return new ResearchLocationRequirement(
                List.copyOf(bodyIds), 0.0, 100.0, 0.0, 1000.0, 0.0, 5000.0, false, false, label
        );
    }

    public boolean matches(@NotNull PlanetaryEnvironment env, boolean canSeeSky) {
        if (this.requireSkyView && !canSeeSky) {
            return false;
        }
        if (!this.allowedBodies.isEmpty()) {
            String envPath = env.id().getPath().toLowerCase(Locale.ROOT);
            boolean matchedBody = false;
            for (ResourceLocation allowed : this.allowedBodies) {
                if (allowed.equals(env.id()) || allowed.getPath().toLowerCase(Locale.ROOT).equals(envPath)) {
                    matchedBody = true;
                    break;
                }
            }
            if (!matchedBody) {
                return false;
            }
        }
        if (env.gravityG() < this.minGravityG || env.gravityG() > this.maxGravityG) {
            return false;
        }
        if (env.pressureAtm() < this.minPressureAtm || env.pressureAtm() > this.maxPressureAtm) {
            return false;
        }
        if (env.peakTempKelvin() < this.minTempKelvin || env.nightTempKelvin() > this.maxTempKelvin) {
            return false;
        }
        if (this.requireCorrosiveAcid && !env.corrosiveAcid()) {
            return false;
        }
        return true;
    }
}
