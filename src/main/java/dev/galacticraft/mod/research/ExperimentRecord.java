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

import org.jetbrains.annotations.NotNull;

/**
 * KSP-style Science Archive entry tracking diminishing returns per unique {@code experimentId@bodyId}.
 */
public record ExperimentRecord(
        @NotNull String key,
        @NotNull String title,
        @NotNull String planetLabel,
        @NotNull String labLabel,
        @NotNull ResearchDomain domain,
        int maxSpPool,
        int collectedSp,
        int runCount
) {
    /**
     * KSP-style diminishing returns factor for the next execution of this experiment.
     * Run 1 = 100%, Run 2 = 35% (or 50% with Quantum Archive Core), Run 3 = 10%, Run 4+ = 0%.
     */
    public static float diminishingFactor(int priorRuns, boolean quantumArchiveBoost) {
        return switch (priorRuns) {
            case 0 -> 1.00F;
            case 1 -> quantumArchiveBoost ? 0.50F : 0.35F;
            case 2 -> quantumArchiveBoost ? 0.15F : 0.10F;
            default -> 0.00F;
        };
    }

    public int remainingSp() {
        return Math.max(0, this.maxSpPool - this.collectedSp);
    }

    public int nextDiminishingPct(boolean quantumBoost) {
        return Math.round(diminishingFactor(this.runCount, quantumBoost) * 100.0F);
    }
}
