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

import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Logical family grouping one or more parametric size variants of a modular rocket part.
 */
public record RocketPartFamily(
        @NotNull String familyId,
        @NotNull String displayName,
        @NotNull RocketPartCategory category,
        boolean parametric,
        @NotNull List<RocketPartSpec> variants,
        @NotNull List<Integer> availableDiameters,
        @NotNull List<Integer> availableTopDiameters,
        @NotNull List<Integer> availableHeights
) {
    private static final Comparator<RocketPartSpec> VARIANT_ORDER = Comparator
            .comparingInt(RocketPartSpec::diameter)
            .thenComparingInt(RocketPartSpec::topDiameter)
            .thenComparingInt(RocketPartSpec::heightBlocks)
            .thenComparing(RocketPartSpec::modelId);

    public RocketPartFamily {
        if (familyId == null || familyId.isBlank()) {
            throw new IllegalArgumentException("familyId must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(variants, "variants");
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("variants must not be empty for family " + familyId);
        }
        variants = List.copyOf(variants);
        availableDiameters = List.copyOf(availableDiameters != null ? availableDiameters : List.of());
        availableTopDiameters = List.copyOf(availableTopDiameters != null ? availableTopDiameters : List.of());
        availableHeights = List.copyOf(availableHeights != null ? availableHeights : List.of());
    }

    /**
     * Constructs a canonically sorted {@link RocketPartFamily} from an unsorted collection of variants.
     */
    public static @NotNull RocketPartFamily fromVariants(
            @NotNull String familyId,
            @NotNull String displayName,
            @NotNull RocketPartCategory category,
            @NotNull List<RocketPartSpec> rawVariants
    ) {
        List<RocketPartSpec> sorted = new ArrayList<>(rawVariants);
        sorted.sort(VARIANT_ORDER);

        TreeSet<Integer> diameters = new TreeSet<>();
        TreeSet<Integer> topDiameters = new TreeSet<>();
        TreeSet<Integer> heights = new TreeSet<>();
        boolean isParametric = sorted.size() > 1;
        for (RocketPartSpec spec : sorted) {
            diameters.add(spec.diameter());
            topDiameters.add(spec.topDiameter());
            heights.add(spec.heightBlocks());
            if (spec.parametric()) {
                isParametric = true;
            }
        }

        return new RocketPartFamily(
                familyId,
                displayName,
                category,
                isParametric,
                sorted,
                new ArrayList<>(diameters),
                new ArrayList<>(topDiameters),
                new ArrayList<>(heights)
        );
    }

    public @NotNull RocketPartSpec defaultVariant() {
        return this.variants.get(0);
    }

    /**
     * Finds the exact variant matching {@code (diameter, topDiameter, heightBlocks)} if exported,
     * otherwise selects the closest valid exported variant in this family prioritized by
     * diameter, top diameter, and height so UI dimension cycling always lands on a real asset.
     */
    public @NotNull RocketPartSpec findBestMatch(int diameter, int topDiameter, int heightBlocks) {
        RocketPartSpec best = this.variants.get(0);
        long bestScore = Long.MAX_VALUE;
        for (RocketPartSpec candidate : this.variants) {
            int dd = Math.abs(candidate.diameter() - diameter);
            int dt = Math.abs(candidate.topDiameter() - topDiameter);
            int dh = Math.abs(candidate.heightBlocks() - heightBlocks);
            if (dd == 0 && dt == 0 && dh == 0) {
                return candidate;
            }
            long score = dd * 10_000L + dt * 100L + dh;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Returns the sorted distinct top diameters available specifically for a given base {@code diameter}.
     */
    public @NotNull List<Integer> availableTopDiametersFor(int diameter) {
        TreeSet<Integer> set = new TreeSet<>();
        for (RocketPartSpec spec : this.variants) {
            if (spec.diameter() == diameter) {
                set.add(spec.topDiameter());
            }
        }
        return set.isEmpty() ? this.availableTopDiameters : List.copyOf(set);
    }

    /**
     * Returns the sorted distinct heights available specifically for a given {@code (diameter, topDiameter)} pair.
     */
    public @NotNull List<Integer> availableHeightsFor(int diameter, int topDiameter) {
        TreeSet<Integer> set = new TreeSet<>();
        for (RocketPartSpec spec : this.variants) {
            if (spec.diameter() == diameter && spec.topDiameter() == topDiameter) {
                set.add(spec.heightBlocks());
            }
        }
        return set.isEmpty() ? this.availableHeights : List.copyOf(set);
    }
}
