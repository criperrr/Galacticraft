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
import dev.galacticraft.mod.world.gen.structure.GCStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
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

        var structureRegistry = context.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var moonVillage = structureRegistry.getHolder(GCStructures.Moon.VILLAGE);
        if (moonVillage.isPresent() && !chunk.getReferencesForStructure(moonVillage.get().value()).isEmpty()) {
            return false;
        }

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
                if (dx >= 32 || dz >= 32) {
                    continue;
                }

                double distSq = dx * dx + dz * dz;
                if (distSq >= radius * radius) {
                    continue;
                }

                // GC4 ChunkProviderMoon.makeCrater quartic bowl profile:
                // sqrtY = (xDev^2 + zDev^2), yDev = sqrtY^2 * 6, craterDepth = 5 - yDev
                double normX = dx / radius;
                double normZ = dz / radius;
                double sqrtY = normX * normX + normZ * normZ;
                double yDev = sqrtY * sqrtY * 6.0;
                double craterDepth = (5.0 - yDev) * depthMultiplier;
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

                BlockState originalSurface = chunk.getBlockState(mutable);
                boolean isMoonColumn = originalSurface.is(GCBlocks.MOON_TURF)
                        || originalSurface.is(GCBlocks.MOON_DIRT)
                        || originalSurface.is(GCBlocks.MOON_ROCK)
                        || originalSurface.is(GCBlocks.MOON_SURFACE_ROCK)
                        || originalSurface.is(GCBlocks.MOON_BASALT)
                        || originalSurface.is(GCBlocks.OLIVINE_BASALT)
                        || originalSurface.is(GCBlocks.RICH_OLIVINE_BASALT)
                        || originalSurface.is(GCBlocks.DENSE_ICE);

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

                    if (!fresh) {
                        context.topMaterial(posToBiome, chunk, mutable, false)
                                .ifPresent(topState -> chunk.setBlockState(mutable, topState, false));
                    } else if (isMoonColumn) {
                        BlockState freshFloor = pickFreshMoonCraterFloor(biomeHolder, random, sqrtY);
                        chunk.setBlockState(mutable, freshFloor, false);
                    }
                }
            }
        }
        return true;
    }

    private static BlockState pickFreshMoonCraterFloor(Holder<Biome> biome, RandomSource random, double normalizedDistSq) {
        // GalaxySpace ChunkProviderCallisto.getCraterAdditions: impact craters expose sub-surface rock, ice, or rare minerals near the center
        if (normalizedDistSq < 0.22 && random.nextInt(40) == 0) {
            return GCBlocks.LUNAR_SAPPHIRE_ORE.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.COMET_TUNDRA)) {
            if (normalizedDistSq < 0.45 && random.nextInt(3) != 0) {
                return GCBlocks.DENSE_ICE.defaultBlockState();
            }
            return GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.OLIVINE_SPIKES)) {
            if (normalizedDistSq < 0.35 && random.nextInt(4) == 0) {
                return GCBlocks.RICH_OLIVINE_BASALT.defaultBlockState();
            }
            return random.nextBoolean() ? GCBlocks.OLIVINE_BASALT.defaultBlockState() : GCBlocks.MOON_BASALT.defaultBlockState();
        }
        if (biome.is(GCBiomes.Moon.BASALTIC_MARE)) {
            return random.nextInt(3) == 0 ? GCBlocks.MOON_SURFACE_ROCK.defaultBlockState() : GCBlocks.MOON_BASALT.defaultBlockState();
        }
        return random.nextInt(4) == 0 ? GCBlocks.COBBLED_MOON_ROCK.defaultBlockState() : GCBlocks.MOON_SURFACE_ROCK.defaultBlockState();
    }

    @Override
    public boolean isStartChunk(CraterCarverConfig config, RandomSource random) {
        return random.nextFloat() <= config.probability;
    }
}
