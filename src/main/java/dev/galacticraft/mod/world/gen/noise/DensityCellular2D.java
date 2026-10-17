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

package dev.galacticraft.mod.world.gen.noise;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from {@code tfcvolcanoes.util.noise.DensityCellular2D} and {@code tfcvolcanoes.util.noise.CinderFeatureNoise}.
 * Provides deterministic 2D Voronoi/Cellular region partitioning with density-modulated feature points,
 * distance-to-neighbor safe diameter checks, and per-cell rarity rolls for planetary volcanoes and thermal hotspots.
 */
public final class DensityCellular2D {
    public final long seed;
    public final double regionSize;
    public final double spread;
    public final int minPoints;
    public final int maxPoints;

    public DensityCellular2D(long seed, double regionSize, double spread, int minPoints, int maxPoints) {
        this.seed = seed;
        this.regionSize = Math.max(16.0, regionSize);
        this.spread = Math.max(0.0001, spread);
        this.minPoints = Math.max(1, minPoints);
        this.maxPoints = Math.max(this.minPoints, maxPoints);
    }

    public DensityCellular2D spread(double s) {
        return new DensityCellular2D(this.seed, this.regionSize, s, this.minPoints, this.maxPoints);
    }

    public Cell cell(double worldX, double worldZ) {
        double scale = this.spread / this.regionSize;
        double x = worldX * scale;
        double z = worldZ * scale;
        int ix = fastFloor(x);
        int iz = fastFloor(z);
        double f1 = Double.MAX_VALUE;
        double f2 = Double.MAX_VALUE;
        double bestX = 0.0;
        double bestZ = 0.0;
        long bestId = 0L;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = ix + dx;
                int cz = iz + dz;
                double d = valueNoise2D(this.seed, cx * 0.1, cz * 0.1);
                double t = (d + 1.0) * 0.5;
                int count = this.minPoints + (int) Math.round(t * (this.maxPoints - this.minPoints));

                for (int i = 0; i < count; i++) {
                    long id = hash(cx ^ this.seed, cz ^ (this.seed * 6364136223846793005L), i);
                    double px = cx + 0.15 + jitter(id) * 0.70;
                    double pz = cz + 0.15 + jitter(id * -7046029254386353131L) * 0.70;
                    double dxp = px - x;
                    double dzp = pz - z;
                    double dist = dxp * dxp + dzp * dzp;
                    if (dist < f1) {
                        f2 = f1;
                        f1 = dist;
                        bestX = px / scale;
                        bestZ = pz / scale;
                        bestId = id;
                    } else if (dist < f2) {
                        f2 = dist;
                    }
                }
            }
        }

        return new Cell(bestX, bestZ, bestId, Math.sqrt(f1), Math.sqrt(f2));
    }

    /**
     * Ported from {@code CinderFeatureNoise.checkCellRarity}: deterministic cell rarity roll in [0, 1).
     */
    public boolean checkCellRarity(Cell cell, float frequency) {
        if (frequency <= 0.0F) {
            return false;
        }
        if (frequency >= 1.0F) {
            return true;
        }
        long rarityHash = hash(cell.id(), this.seed, 0x9E3779B97F4A7C15L);
        return jitter(rarityHash) < (double) frequency;
    }

    /**
     * Ported from {@code CinderFeatureNoise.maxSafeDiameter}: ensures the Voronoi cell has sufficient separation
     * from neighboring cells so volcanoes never overlap or clip each other.
     */
    public double maxSafeDiameter(Cell cell) {
        return cell.f2() - cell.f1();
    }

    /**
     * Ported from {@code CinderFeatureNoise.calculateCenter}: returns the deterministic volcano center {@link BlockPos}
     * if the cell passes the biome frequency check and safe diameter check, or {@code null} otherwise.
     */
    @Nullable
    public BlockPos calculateCenter(int x, int y, int z, float frequency) {
        Cell cell = this.cell(x, z);
        if (this.checkCellRarity(cell, frequency) && this.maxSafeDiameter(cell) > 0.18) {
            return new BlockPos(fastFloor(cell.x()), y, fastFloor(cell.z()));
        }
        return null;
    }

    /**
     * Deterministic 2D thermal hotspot intensity in [0.0, 1.0] for planetary geothermal activity.
     * Combines wide regional simplex-like noise (ported from {@code TFCVHelpers.wideNoise}) with local cellular fissures.
     */
    public static double sampleThermalHotspot(long seed, double x, double z) {
        double regional = valueNoise2D(seed ^ 0x5DEECE66DL, x * 0.012, z * 0.012);
        double detail = valueNoise2D(seed ^ 0xB504F333L, x * 0.045, z * 0.045) * 0.45;
        double combined = (regional + detail + 1.45) / 2.90;
        return Math.max(0.0, Math.min(1.0, combined));
    }

    public static double valueNoise2D(long seed, double x, double z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        double fx = x - x0;
        double fz = z - z0;
        double ux = fx * fx * (3.0 - 2.0 * fx);
        double uz = fz * fz * (3.0 - 2.0 * fz);

        double n00 = jitter(hash(x0, z0, seed)) * 2.0 - 1.0;
        double n10 = jitter(hash(x0 + 1, z0, seed)) * 2.0 - 1.0;
        double n01 = jitter(hash(x0, z0 + 1, seed)) * 2.0 - 1.0;
        double n11 = jitter(hash(x0 + 1, z0 + 1, seed)) * 2.0 - 1.0;

        double nx0 = n00 + ux * (n10 - n00);
        double nx1 = n01 + ux * (n11 - n01);
        return nx0 + uz * (nx1 - nx0);
    }

    public static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    public static long hash(long x, long z, long i) {
        long h = x * -7046029254386353131L ^ z * -4417276706812531889L ^ i * 1609587929392839161L;
        h ^= h >> 33;
        h *= -49064778989728563L;
        h ^= h >> 33;
        h *= -4265267296055464877L;
        return h ^ h >> 33;
    }

    public static double jitter(long h) {
        return (h & 16777215L) / 1.6777215E7;
    }

    public record Cell(double x, double z, long id, double f1, double f2) {
    }
}
