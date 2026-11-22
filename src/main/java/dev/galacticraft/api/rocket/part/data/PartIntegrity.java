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
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks the structural integrity, physical wear, and maintenance condition of a rocket part.
 *
 * @param currentIntegrity Current structural integrity points.
 * @param maxIntegrity     Maximum certified integrity points.
 * @param damageState      Current damage classification resolved from current / max.
 */
public record PartIntegrity(
        double currentIntegrity,
        double maxIntegrity,
        @NotNull PartDamageState damageState
) {
    public static final double DEFAULT_MAX_INTEGRITY = 100.0;
    public static final PartIntegrity DEFAULT = pristine(DEFAULT_MAX_INTEGRITY);

    /**
     * Compact canonical constructor enforcing defensive boundary validation.
     */
    public PartIntegrity {
        currentIntegrity = Double.isNaN(currentIntegrity) ? 0.0 : Math.max(0.0, currentIntegrity);
        maxIntegrity = Double.isNaN(maxIntegrity) ? 0.0 : Math.max(0.0, maxIntegrity);
        if (currentIntegrity > maxIntegrity) {
            currentIntegrity = maxIntegrity;
        }
        if (damageState == null || maxIntegrity <= 0.0) {
            double fraction = maxIntegrity > 0.0 ? (currentIntegrity / maxIntegrity) : 0.0;
            damageState = PartDamageState.fromIntegrity(fraction);
        }
    }

    public static final Codec<PartIntegrity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("current").forGetter(PartIntegrity::currentIntegrity),
            Codec.DOUBLE.fieldOf("max").forGetter(PartIntegrity::maxIntegrity),
            PartDamageState.CODEC.fieldOf("state").forGetter(PartIntegrity::damageState)
    ).apply(instance, PartIntegrity::new));

    public static final StreamCodec<ByteBuf, PartIntegrity> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, PartIntegrity::currentIntegrity,
            ByteBufCodecs.DOUBLE, PartIntegrity::maxIntegrity,
            PartDamageState.STREAM_CODEC, PartIntegrity::damageState,
            PartIntegrity::new
    );

    /**
     * Creates a brand-new, factory-certified part with full structural integrity.
     *
     * @param max Maximum integrity value.
     * @return A pristine {@link PartIntegrity} instance in {@link PartDamageState#NOMINAL}.
     */
    public static @NotNull PartIntegrity pristine(double max) {
        if (Double.isNaN(max) || max <= 0.0) {
            return new PartIntegrity(0.0, 0.0, PartDamageState.DAMAGED);
        }
        return new PartIntegrity(max, max, PartDamageState.NOMINAL);
    }

    /**
     * Creates a pristine part with standard 100.0 integrity points.
     */
    public static @NotNull PartIntegrity pristine() {
        return pristine(DEFAULT_MAX_INTEGRITY);
    }

    /**
     * Inflicts structural damage, reducing current integrity and recalculating {@link PartDamageState}.
     *
     * @param amount Integrity points to subtract.
     * @return A new mutated {@link PartIntegrity} instance.
     */
    public @NotNull PartIntegrity damage(double amount) {
        if (Double.isNaN(amount) || amount <= 0.0 || this.currentIntegrity <= 0.0) {
            return this;
        }
        double next = Math.max(0.0, this.currentIntegrity - amount);
        double fraction = this.maxIntegrity > 0.0 ? (next / this.maxIntegrity) : 0.0;
        return new PartIntegrity(next, this.maxIntegrity, PartDamageState.fromIntegrity(fraction));
    }

    /**
     * Restores structural integrity up to 100% of maximum.
     *
     * @param amount Integrity points to add.
     * @return A new repaired {@link PartIntegrity} instance.
     */
    public @NotNull PartIntegrity repair(double amount) {
        return this.repair(amount, 1.0);
    }

    /**
     * Restores structural integrity capped by an operational ceiling fraction.
     * For example, field repairs (sealant patches, field welding torches) cap at 0.85 (85%).
     *
     * @param amount      Integrity points to add.
     * @param maxFraction Maximum allowed ratio (e.g. 0.85 for field maintenance, 1.0 for Hangar).
     * @return A new repaired {@link PartIntegrity} instance.
     */
    public @NotNull PartIntegrity repair(double amount, double maxFraction) {
        if (Double.isNaN(amount) || amount <= 0.0 || Double.isNaN(maxFraction) || maxFraction <= 0.0 || this.maxIntegrity <= 0.0) {
            return this;
        }
        double ceiling = Math.min(this.maxIntegrity, this.maxIntegrity * Math.min(1.0, maxFraction));
        if (this.currentIntegrity >= ceiling) {
            return this;
        }
        double next = Math.min(ceiling, this.currentIntegrity + amount);
        double fraction = next / this.maxIntegrity;
        return new PartIntegrity(next, this.maxIntegrity, PartDamageState.fromIntegrity(fraction));
    }

    /**
     * @return Normalized health ratio between 0.0 (destroyed) and 1.0 (pristine).
     */
    public double fraction() {
        return this.maxIntegrity > 0.0 ? (this.currentIntegrity / this.maxIntegrity) : 0.0;
    }

    /**
     * @return True if this component is at or above 95% nominal structural health.
     */
    public boolean isPristine() {
        return this.damageState == PartDamageState.NOMINAL && this.fraction() >= 0.95;
    }
}
