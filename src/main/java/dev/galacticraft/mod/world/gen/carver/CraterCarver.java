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
    public boolean carve(CarvingContext context, CraterCarverConfig config, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> posToBiome, RandomSource random, Aquifer aquiferSampler, ChunkPos pos, CarvingMask carvingMask) {
        int fallbackY = config.y.sample(random, context);
        BlockPos craterCenter = pos.getBlockAt(random.nextInt(16), fallbackY, random.nextInt(16));

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos copy = new BlockPos.MutableBlockPos();

        // GC4 EnumCraterSize distribution between minRadius (8) and maxRadius (27)
        double radius = config.minRadius + (random.nextDouble() * (config.maxRadius - config.minRadius));
        if (random.nextBoolean() && (radius < (config.minRadius + config.idealRangeOffset) || radius > (config.maxRadius - config.idealRangeOffset))) {
            radius = config.minRadius + (random.nextDouble() * (config.maxRadius - config.minRadius));
        }
        double depthMultiplier = 1.0 - ((random.nextDouble() - 0.5) * 0.3);
        boolean fresh = random.nextInt(4) == 0;

        for (int innerChunkX = 0; innerChunkX < 16; innerChunkX++) {
            for (int innerChunkZ = 0; innerChunkZ < 16; innerChunkZ++) {
                int worldX = chunk.getPos().getBlockX(innerChunkX);
                int worldZ = chunk.getPos().getBlockZ(innerChunkZ);

                double dx = Math.abs(worldX - craterCenter.getX());
                double dz = Math.abs(worldZ - craterCenter.getZ());
                int maxReach = Math.max(32, config.maxRadius + 4);
                if (dx >= maxReach || dz >= maxReach) {
                    continue;
                }

                double distSq = dx * dx + dz * dz;
                if (distSq >= radius * radius) {
                    continue;
                }

                // Quartic bowl profile with configurable baseDepth:
                // At center (sqrtY = 0): craterDepth = config.baseDepth * depthMultiplier
                // At rim (sqrtY = 1): craterDepth = 0
                double normX = dx / radius;
                double normZ = dz / radius;
                double sqrtY = normX * normX + normZ * normZ;
                double yDev = sqrtY * sqrtY * 6.0;
                double craterDepth = (config.baseDepth - (yDev * (config.baseDepth / 6.0))) * depthMultiplier;
                if (craterDepth <= 0.0) {
                    continue;
                }

                int toDig = (int) Math.ceil(craterDepth);
                if (toDig < 1) {
                    continue;
                }
                if (fresh) {
                    toDig++;
                }

                // Find actual surface height in this column so craters carve cleanly from Y=60 mares up to Y=140 highlands
                int surfaceY = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, innerChunkX, innerChunkZ);
                if (surfaceY <= context.getMinGenY() + 8) {
                    surfaceY = fallbackY;
                }

                mutable.set(innerChunkX, Math.min(chunk.getMaxBuildHeight() - 1, surfaceY + 1), innerChunkZ);
                while (mutable.getY() > context.getMinGenY() + 8 && chunk.getBlockState(mutable).isAir()) {
                    mutable.move(Direction.DOWN);
                }

                int dug = 0;
                while (dug < toDig && mutable.getY() > context.getMinGenY() + 6) {
                    BlockState current = chunk.getBlockState(mutable);
                    int maskY = mutable.getY() - context.getMinGenY();
                    if (!current.isAir() || (maskY >= 0 && maskY < chunk.getHeight() && carvingMask.get(innerChunkX, maskY, innerChunkZ))) {
                        chunk.setBlockState(mutable, AIR, false);
                        if (dug == 0 && maskY >= 0 && maskY < chunk.getHeight()) {
                            carvingMask.set(innerChunkX, maskY, innerChunkZ);
                        }
                        dug++;
                    }
                    mutable.move(Direction.DOWN);
                }

                // Floor material dressing (GC4 ChunkProviderMoon + GalaxySpace getCraterAdditions)
                if (!chunk.getBlockState(mutable).isAir() && !chunk.getBlockState(copy.set(mutable).move(Direction.DOWN)).isAir()) {
                    BlockPos worldFloorPos = new BlockPos(worldX, mutable.getY(), worldZ);
                    Holder<Biome> biomeHolder = posToBiome.apply(worldFloorPos);

                    if (isMoonBiome(biomeHolder)) {
                        BlockState floorState = pickMoonCraterFloor(biomeHolder, fresh, worldX, mutable.getY(), worldZ, sqrtY);
                        chunk.setBlockState(mutable, floorState, false);
                        // Ensure basaltic mare crater floors don't expose white moon rock underneath
                        if (biomeHolder.is(GCBiomes.Moon.BASALTIC_MARE) || biomeHolder.is(GCBiomes.Moon.OLIVINE_SPIKES)) {
                            BlockPos underPos = copy.set(mutable).move(Direction.DOWN);
                            if (!chunk.getBlockState(underPos).isAir()) {
                                chunk.setBlockState(underPos, GCBlocks.MOON_BASALT.defaultBlockState(), false);
                            }
                        }
                    } else if (isMercuryBiome(biomeHolder)) {
                        BlockState floorState = pickMercuryCraterFloor(biomeHolder, fresh, worldX, mutable.getY(), worldZ, sqrtY);
                        chunk.setBlockState(mutable, floorState, false);
                        if (biomeHolder.is(GCBiomes.Mercury.POLAR_DARK_CRATERS)) {
                            BlockPos underPos = copy.set(mutable).move(Direction.DOWN);
                            if (!chunk.getBlockState(underPos).isAir()) {
                                chunk.setBlockState(underPos, GCBlocks.MERCURY_SHOCK_BRECCIA.defaultBlockState(), false);
                            }
                        }
                    } else {
                        context.topMaterial(posToBiome, chunk, worldFloorPos, false)
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
            // Deep interior of polar craters (permanent cold trap): pure packed ice & blue ice sheets!
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
        // General Mercury surface (Inter-Crater Plains, Smooth Lava, Chaotic):
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
            // Basaltic Mare: pure dark basalt floor, with occasional shocked impact surface rock
            if (fresh && (hash % 4 == 0)) {
                return GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
            }
            return (hash % 7 == 0) ? GCBlocks.MOON_SURFACE_ROCK.defaultBlockState() : GCBlocks.MOON_BASALT.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.OLIVINE_SPIKES)) {
            // Olivine Spikes: basalt, olivine basalt, and central rich olivine basalt
            if (normalizedDistSq < 0.35 && (hash % 4 == 0)) {
                return GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState();
            }
            return (hash % 3 == 0) ? GCBlocks.OLIVINE_BASALT.defaultBlockState() : GCBlocks.MOON_BASALT.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.COMET_TUNDRA)) {
            // Comet Tundra: impact reveals dense ice in crater basin, or rocky rim
            if (normalizedDistSq < 0.45 && (hash % 3 != 0)) {
                return GCBlocks.DENSE_ICE.defaultBlockState();
            }
            return GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
        }
        // Highlands & Lowlands:
        if (fresh) {
            // Fresh crater floor exposes fractured rock and rubble
            return (hash % 4 == 0) ? GCBlocks.COBBLED_MOON_ROCK.defaultBlockState() : GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
        } else {
            // Weathered crater floor is covered in mature lunar turf, with rock outcrops
            return (hash % 6 == 0) ? GCBlocks.MOON_SURFACE_ROCK.defaultBlockState() : GCBlocks.MOON_TURF.defaultBlockState();
        }
    }

    @Override
    public boolean isStartChunk(CraterCarverConfig config, RandomSource random) {
        return random.nextFloat() <= config.probability;
    }
}
