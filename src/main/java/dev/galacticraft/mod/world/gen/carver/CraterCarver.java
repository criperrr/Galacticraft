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

package dev.galacticraft.mod.world.gen.carver;

import com.mojang.serialization.Codec;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.world.biome.GCBiomes;
import dev.galacticraft.mod.world.gen.carver.config.CraterCarverConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.function.Function;

public class CraterCarver extends WorldCarver<CraterCarverConfig> {
    public CraterCarver(Codec<CraterCarverConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public int getRange() {
        return 3; // Optimized range: craters up to radius 27 cover at most 2-3 chunk radii
    }

    @Override
    public boolean carve(CarvingContext context, CraterCarverConfig config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> posToBiome, RandomSource random, Aquifer aquiferSampler, ChunkPos pos, CarvingMask carvingMask) {
        // 1. Fully deterministic crater PRNG derived from origin chunk pos
        long craterSeed = pos.toLong() ^ ((long) pos.x * 341873128712L + (long) pos.z * 132897987541L) ^ 0x5deece66dL;
        RandomSource craterRand = RandomSource.create(craterSeed);

        int centerBlockX = pos.getBlockX(craterRand.nextInt(16));
        int centerBlockZ = pos.getBlockZ(craterRand.nextInt(16));
        int fallbackY = config.y.sample(craterRand, context);
        BlockPos craterCenter = new BlockPos(centerBlockX, fallbackY, centerBlockZ);

        // GC4 EnumCraterSize distribution between minRadius and maxRadius
        double radius = config.minRadius + (craterRand.nextDouble() * (config.maxRadius - config.minRadius));
        if (craterRand.nextBoolean() && (radius < (config.minRadius + config.idealRangeOffset) || radius > (config.maxRadius - config.idealRangeOffset))) {
            radius = config.minRadius + (craterRand.nextDouble() * (config.maxRadius - config.minRadius));
        }
        double depthMultiplier = 1.0 - ((craterRand.nextDouble() - 0.5) * 0.3);
        boolean fresh = craterRand.nextInt(4) == 0;

        // 2. High-speed 2D AABB Intersection Culling:
        // Discard immediately if the crater circle does not intersect this chunk's bounding box
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMaxX = chunk.getPos().getMaxBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxZ = chunk.getPos().getMaxBlockZ();

        int closestX = Math.max(chunkMinX, Math.min(centerBlockX, chunkMaxX));
        int closestZ = Math.max(chunkMinZ, Math.min(centerBlockZ, chunkMaxZ));
        double distX = centerBlockX - closestX;
        double distZ = centerBlockZ - closestZ;
        if ((distX * distX + distZ * distZ) >= (radius * radius)) {
            return false;
        }

        // 3. Clamped iteration bounds - only iterate over columns that can intersect the crater
        int startX = Math.max(0, (int) Math.floor(centerBlockX - radius) - chunkMinX);
        int endX = Math.min(15, (int) Math.ceil(centerBlockX + radius) - chunkMinX);
        int startZ = Math.max(0, (int) Math.floor(centerBlockZ - radius) - chunkMinZ);
        int endZ = Math.min(15, (int) Math.ceil(centerBlockZ + radius) - chunkMinZ);

        // Sample biome once at the crater center to avoid millions of heavy biome lookups
        Holder<Biome> craterBiome = posToBiome.apply(craterCenter);
        boolean isMoon = isMoonBiome(craterBiome);
        boolean isMercury = isMercuryBiome(craterBiome);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos copy = new BlockPos.MutableBlockPos();
        double radiusSq = radius * radius;

        for (int innerChunkX = startX; innerChunkX <= endX; innerChunkX++) {
            for (int innerChunkZ = startZ; innerChunkZ <= endZ; innerChunkZ++) {
                int worldX = chunkMinX + innerChunkX;
                int worldZ = chunkMinZ + innerChunkZ;

                double dx = Math.abs(worldX - centerBlockX);
                double dz = Math.abs(worldZ - centerBlockZ);
                double distSq = dx * dx + dz * dz;
                if (distSq >= radiusSq) {
                    continue;
                }

                // Continuous parabolic impact bowl profile: depth(r) = maxDepth * (1 - r^2/R^2)
                double normSq = distSq / radiusSq;
                double maxDepth = Math.max(config.baseDepth, radius * 0.22) * depthMultiplier;
                double continuousDepth = maxDepth * (1.0 - normSq);
                if (continuousDepth <= 0.05) {
                    continue; // Seamless asymptotic boundary blend at crater rim
                }

                if (fresh) {
                    continuousDepth += 1.0;
                }

                // Sub-voxel stochastic dithering: breaks discrete integer floor terraces into smooth,
                // natural granular regolith slopes, completely eradicating the concentric step ("Mayan pyramid") artifact
                int baseDig = (int) Math.floor(continuousDepth);
                double fraction = continuousDepth - baseDig;
                long colHash = ((long) worldX * 3129871L) ^ ((long) worldZ * 116129781L) ^ craterSeed;
                double dither = ((colHash >> 16) & 0xFFFF) / 65536.0;
                int toDig = baseDig + (dither < fraction ? 1 : 0);
                if (toDig < 1) {
                    continue;
                }

                // Find surface height via Heightmap
                int surfaceY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, innerChunkX, innerChunkZ);
                if (surfaceY <= context.getMinGenY() + 8) {
                    surfaceY = fallbackY;
                }

                // Bounded search for solid ground: at most 24 blocks above/below surfaceY
                mutable.set(innerChunkX, Math.min(chunk.getMaxBuildHeight() - 1, surfaceY + 2), innerChunkZ);
                int limit = 0;
                while (mutable.getY() > context.getMinGenY() + 8 && chunk.getBlockState(mutable).isAir() && limit < 24) {
                    mutable.move(Direction.DOWN);
                    limit++;
                }

                if (chunk.getBlockState(mutable).isAir()) {
                    continue; // No solid surface found in this column
                }

                int topSolidY = mutable.getY();
                int bottomDigY = Math.max(context.getMinGenY() + 6, topSolidY - toDig);

                // Set carving mask on surface
                int maskY = topSolidY - context.getMinGenY();
                if (maskY >= 0 && maskY < chunk.getHeight()) {
                    carvingMask.set(innerChunkX, maskY, innerChunkZ);
                }

                // Carve air directly in the calculated slice
                for (int y = topSolidY; y > bottomDigY; y--) {
                    mutable.set(innerChunkX, y, innerChunkZ);
                    chunk.setBlockState(mutable, AIR, false);
                }

                // Floor material dressing
                mutable.set(innerChunkX, bottomDigY, innerChunkZ);
                copy.set(mutable).move(Direction.DOWN);
                if (!chunk.getBlockState(copy).isAir()) {
                    if (isMoon) {
                        BlockState floorState = pickMoonCraterFloor(craterBiome, fresh, worldX, bottomDigY, worldZ, normSq);
                        chunk.setBlockState(mutable, floorState, false);
                        if (craterBiome.is(GCBiomes.Moon.BASALTIC_MARE) || craterBiome.is(GCBiomes.Moon.OLIVINE_SPIKES)) {
                            if (!chunk.getBlockState(copy).isAir()) {
                                chunk.setBlockState(copy, GCBlocks.MOON_BASALT.defaultBlockState(), false);
                            }
                        }
                    } else if (isMercury) {
                        BlockState floorState = pickMercuryCraterFloor(craterBiome, fresh, worldX, bottomDigY, worldZ, normSq);
                        chunk.setBlockState(mutable, floorState, false);
                        if (craterBiome.is(GCBiomes.Mercury.POLAR_DARK_CRATERS)) {
                            if (!chunk.getBlockState(copy).isAir()) {
                                chunk.setBlockState(copy, GCBlocks.MERCURY_SHOCK_BRECCIA.defaultBlockState(), false);
                            }
                        }
                    } else {
                        BlockPos floorPos = new BlockPos(worldX, bottomDigY, worldZ);
                        context.topMaterial(posToBiome, chunk, floorPos, false)
                                .ifPresent(topState -> chunk.setBlockState(mutable, topState, false));
                    }
                }
            }
        }

        return true;
    }

    private static boolean isMoonBiome(Holder<Biome> biome) {
        return biome.is(GCBiomes.Moon.BASALTIC_MARE)
                || biome.is(GCBiomes.Moon.OLIVINE_SPIKES)
                || biome.is(GCBiomes.Moon.COMET_TUNDRA)
                || biome.is(GCBiomes.Moon.LUNAR_HIGHLANDS)
                || biome.is(GCBiomes.Moon.LUNAR_LOWLANDS);
    }

    private static boolean isMercuryBiome(Holder<Biome> biome) {
        return biome.is(GCBiomes.Mercury.POLAR_DARK_CRATERS)
                || biome.is(GCBiomes.Mercury.CALORIS_BASIN)
                || biome.is(GCBiomes.Mercury.CHAOTIC_TERRAIN)
                || biome.is(GCBiomes.Mercury.INTER_CRATER_PLAINS)
                || biome.is(GCBiomes.Mercury.SMOOTH_LAVA_PLAINS);
    }

    private static BlockState pickMercuryCraterFloor(Holder<Biome> biome, boolean fresh, int worldX, int worldY, int worldZ, double normalizedDistSq) {
        int hash = posHash(worldX, worldY, worldZ);
        if (biome.is(GCBiomes.Mercury.POLAR_DARK_CRATERS)) {
            if (normalizedDistSq < 0.40) {
                return (hash % 4 == 0) ? Blocks.BLUE_ICE.defaultBlockState() : Blocks.PACKED_ICE.defaultBlockState();
            } else if (normalizedDistSq < 0.65) {
                return (hash % 2 == 0) ? Blocks.PACKED_ICE.defaultBlockState() : GCBlocks.MERCURY_FROST_REGOLITH.defaultBlockState();
            }
            return GCBlocks.MERCURY_FROST_REGOLITH.defaultBlockState();
        }
        if (biome.is(GCBiomes.Mercury.CALORIS_BASIN)) {
            if (normalizedDistSq < 0.35 && (hash % 3 == 0)) {
                return GCBlocks.MERCURY_GRAPHITE_ROCK.defaultBlockState();
            }
            return (hash % 4 == 0) ? GCBlocks.MERCURY_SHOCK_BRECCIA.defaultBlockState() : GCBlocks.MERCURY_BASALT.defaultBlockState();
        }
        if (fresh || normalizedDistSq < 0.30) {
            return (hash % 3 == 0) ? GCBlocks.MERCURY_SHOCK_BRECCIA.defaultBlockState() : GCBlocks.MERCURY_BASALT.defaultBlockState();
        }
        return (hash % 5 == 0) ? GCBlocks.MERCURY_COMPACTED_REGOLITH.defaultBlockState() : GCBlocks.MERCURY_REGOLITH.defaultBlockState();
    }

    private static int posHash(int x, int y, int z) {
        long hash = ((long) x * 3129871L) ^ ((long) z * 6187919L) ^ ((long) y * 1000003L);
        hash = (hash ^ (hash >> 16)) * 0x45d9f3bL;
        hash = (hash ^ (hash >> 16)) * 0x45d9f3bL;
        hash = hash ^ (hash >> 16);
        return (int) (hash & 0x7fffffff);
    }

    private static BlockState pickMoonCraterFloor(Holder<Biome> biome, boolean fresh, int worldX, int worldY, int worldZ, double normalizedDistSq) {
        int hash = posHash(worldX, worldY, worldZ);
        if (biome.is(GCBiomes.Moon.BASALTIC_MARE)) {
            if (fresh && (hash % 4 == 0)) {
                return GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
            }
            return (hash % 7 == 0) ? GCBlocks.MOON_SURFACE_ROCK.defaultBlockState() : GCBlocks.MOON_BASALT.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.OLIVINE_SPIKES)) {
            if (normalizedDistSq < 0.35 && (hash % 4 == 0)) {
                return GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState();
            }
            return (hash % 3 == 0) ? GCBlocks.OLIVINE_BASALT.defaultBlockState() : GCBlocks.MOON_BASALT.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.COMET_TUNDRA)) {
            if (normalizedDistSq < 0.45 && (hash % 3 != 0)) {
                return GCBlocks.DENSE_ICE.defaultBlockState();
            }
            return GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
        }
        if (fresh) {
            return (hash % 4 == 0) ? GCBlocks.COBBLED_MOON_ROCK.defaultBlockState() : GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
        } else {
            return (hash % 6 == 0) ? GCBlocks.MOON_SURFACE_ROCK.defaultBlockState() : GCBlocks.MOON_TURF.defaultBlockState();
        }
    }

    @Override
    public boolean isStartChunk(CraterCarverConfig config, RandomSource random) {
        return random.nextFloat() <= config.probability;
    }
}
