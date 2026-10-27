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

package dev.galacticraft.mod.world.gen.feature;

import com.mojang.serialization.Codec;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryVolcanoConfig;
import dev.galacticraft.mod.world.gen.feature.config.PlanetaryVolcanoConfig.VolcanoType;
import dev.galacticraft.mod.world.gen.noise.DensityCellular2D;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-chunk planetary volcano generator ported from {@code TFCVolcanoes}:
 * <ul>
 *   <li>{@code tfcvolcanoes.util.noise.DensityCellular2D} for Voronoi region partitioning</li>
 *   <li>{@code tfcvolcanoes.util.noise.CinderFeatureNoise} for biome-specific frequency and safe cell diameter checks</li>
 *   <li>{@code tfcvolcanoes.util.TremorEvent.VolcanoType} for Cinder Cone, Stratovolcano, Shield, and Tuff Ring profiles</li>
 *   <li>{@code tfcvolcanoes.util.regions.RockRegionHelper} for extrusive (surface) vs intrusive (subsurface) igneous rock layering</li>
 *   <li>{@code tfcvolcanoes.common.entities.PyroclasticBomb} ({@code placeBlocks} &amp; {@code getRandomRock}) for scoria, obsidian, magma, and ore ejecta</li>
 * </ul>
 */
public class PlanetaryVolcanoFeature extends Feature<PlanetaryVolcanoConfig> {
    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    /**
     * Ported from {@code TremorEvent.VOLCANO_CENTERS}: tracks discovered active volcano centers per dimension
     * so atmospheric tick handlers can emit volcanic smoke plumes, rumbles, and geothermal heat around craters.
     */
    public static final Map<ResourceKey<Level>, Set<BlockPos>> VOLCANO_CENTERS = new ConcurrentHashMap<>();

    public PlanetaryVolcanoFeature(Codec<PlanetaryVolcanoConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<PlanetaryVolcanoConfig> context) {
        WorldGenLevel level = context.level();
        PlanetaryVolcanoConfig config = context.config();
        BlockPos origin = context.origin();
        long seed = level.getSeed();

        ChunkPos chunkPos = new ChunkPos(origin);
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int sampleX = chunkPos.getMiddleBlockX();
        int sampleZ = chunkPos.getMiddleBlockZ();

        // 1. Sample deterministic Voronoi cell from DensityCellular2D (ported from TFCVolcanoes)
        DensityCellular2D cellularNoise = new DensityCellular2D(seed + 7653L, config.regionSize(), config.spread(), 1, 3);
        DensityCellular2D.Cell cell = cellularNoise.cell(sampleX, sampleZ);

        int centerX = DensityCellular2D.fastFloor(cell.x());
        int centerZ = DensityCellular2D.fastFloor(cell.z());

        // 2. Check biome frequency at the deterministic cell center (ported from CinderFeatureNoise.getFrequency)
        Holder<Biome> centerBiome = level.getUncachedNoiseBiome(
                QuartPos.fromBlock(centerX),
                QuartPos.fromBlock(80),
                QuartPos.fromBlock(centerZ)
        );
        float frequency = config.getFrequency(centerBiome);
        if (!cellularNoise.checkCellRarity(cell, frequency) || cellularNoise.maxSafeDiameter(cell) <= 0.18) {
            return false;
        }

        // 3. Deterministically derive volcano morphology, radius, height, and crater radius from cell.id()
        long cellHash = DensityCellular2D.hash(cell.id(), seed, 0x6A09E667F3BCC908L);
        VolcanoType morphType = resolveVolcanoType(config.volcanoType(), cellHash);

        double radiusRoll = DensityCellular2D.jitter(cellHash ^ 0xBB67AE8584CAA73BL);
        double heightRoll = DensityCellular2D.jitter(cellHash ^ 0x3C6EF372FE94F82BL);
        double craterRoll = DensityCellular2D.jitter(cellHash ^ 0xA54FF53A5F1D36F1L);

        int baseOuterRadius = Mth.lerpInt((float) radiusRoll, config.minRadius(), config.maxRadius());
        int baseVolcanoHeight = Mth.lerpInt((float) heightRoll, config.minHeight(), config.maxHeight());
        int baseCraterRadius = Mth.lerpInt((float) craterRoll, config.minCraterRadius(), config.maxCraterRadius());

        int outerRadius = switch (morphType) {
            case SHIELD -> (int) (baseOuterRadius * 1.18);
            case STRATOVOLCANO -> (int) (baseOuterRadius * 1.05);
            case TUFF_RING -> (int) (baseOuterRadius * 0.95);
            default -> baseOuterRadius;
        };
        int volcanoHeight = switch (morphType) {
            case SHIELD -> Math.max(18, (int) (baseVolcanoHeight * 0.72));
            case STRATOVOLCANO -> (int) (baseVolcanoHeight * 1.15);
            case TUFF_RING -> Math.max(12, (int) (baseVolcanoHeight * 0.48));
            default -> baseVolcanoHeight;
        };
        int craterRadius = switch (morphType) {
            case SHIELD -> Math.max(8, (int) (baseCraterRadius * 1.25));
            case TUFF_RING -> Math.max(10, (int) (baseCraterRadius * 1.45));
            default -> baseCraterRadius;
        };

        // Quick bounding check: skip chunk if it is completely outside the volcano's reach
        double chunkDist = Math.hypot(sampleX - centerX, sampleZ - centerZ);
        if (chunkDist > outerRadius + 20.0) {
            return false;
        }

        // 4. Query exact terrain base height at the volcano center via ChunkGenerator.getBaseHeight (no chunk load required)
        ChunkGenerator generator = context.chunkGenerator();
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        int centerGroundY = generator.getBaseHeight(centerX, centerZ, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState) - 1;
        if (centerGroundY < 82 || centerGroundY >= 170) {
            return false;
        }

        int rimPeakY = centerGroundY + volcanoHeight;
        int craterDepth = switch (morphType) {
            case TUFF_RING -> Math.max(12, volcanoHeight + 6);
            case STRATOVOLCANO -> Math.max(16, (int) (volcanoHeight * 0.62));
            default -> Math.max(14, (int) (volcanoHeight * 0.55));
        };
        int lavaSurfaceY = rimPeakY - Math.max(5, craterDepth / 2);
        int craterFloorY = rimPeakY - craterDepth;

        // Record discovered volcano center for TremorEvent-style surface smoke & rumbles
        if (Math.abs(sampleX - centerX) <= 16 && Math.abs(sampleZ - centerZ) <= 16) {
            VOLCANO_CENTERS
                    .computeIfAbsent(level.getLevel().dimension(), k -> ConcurrentHashMap.newKeySet())
                    .add(new BlockPos(centerX, lavaSurfaceY, centerZ));
        }

        // Deterministic radial lava breach azimuths (2 to 3 lava channels down the flanks)
        int flowCount = 2 + (int) (Math.abs(cellHash) % 2);
        double[] flowAngles = new double[flowCount];
        for (int f = 0; f < flowCount; f++) {
            flowAngles[f] = DensityCellular2D.jitter(cellHash ^ ((f + 1L) * 0x517CC1B727220A95L)) * Math.PI * 2.0;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean placedAny = false;

        // 5. Shape each (x, z) column inside the current chunk (ported from CenteredFeatureNoise.setColumnAndSampleHeight)
        for (int lx = 0; lx < 16; lx++) {
            int worldX = chunkMinX + lx;
            for (int lz = 0; lz < 16; lz++) {
                int worldZ = chunkMinZ + lz;
                double dx = worldX - centerX;
                double dz = worldZ - centerZ;
                double rawDist = Math.hypot(dx, dz);

                if (rawDist > outerRadius + 6.0) {
                    continue;
                }

                double angle = Math.atan2(dz, dx);
                // Multi-frequency angular & spatial noise for realistic volcanic ridges and gullies
                double angularRidge = Math.sin(angle * 5.0 + DensityCellular2D.jitter(cellHash) * 6.28) * 0.08
                        + Math.cos(angle * 9.0 - DensityCellular2D.jitter(cellHash >> 8) * 6.28) * 0.04;
                double spatialNoise = DensityCellular2D.valueNoise2D(seed ^ 0x3F89C1AL, worldX * 0.055, worldZ * 0.055) * 0.09;

                double effectiveOuterRadius = outerRadius * (1.0 + angularRidge + spatialNoise);
                double effectiveCraterRadius = craterRadius * (1.0 + angularRidge * 0.45 + spatialNoise * 0.35);

                if (rawDist > effectiveOuterRadius) {
                    continue;
                }

                int localGroundY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, worldX, worldZ) - 1;
                RandomSource colRand = RandomSource.create(DensityCellular2D.hash(worldX, worldZ, seed));

                if (rawDist <= effectiveCraterRadius) {
                    // Inside the central volcanic caldera (bubbling lava lake, obsidian/magma walls, deep magma conduit)
                    double craterNorm = rawDist / Math.max(1.0, effectiveCraterRadius);
                    double bowlFactor = craterNorm * craterNorm * craterNorm;
                    int wallTopY = Mth.floor(Mth.lerp(bowlFactor, craterFloorY, rimPeakY));

                    // Check if this column is aligned with a radial lava spillway breach to the outer flanks
                    boolean inCalderaSpillway = false;
                    double meanderCaldera = Math.sin(rawDist * 0.18 + DensityCellular2D.jitter(cellHash) * 5.0) * 0.14;
                    for (double flowAngle : flowAngles) {
                        double angleDiff = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(angle - (flowAngle + meanderCaldera))));
                        if (angleDiff <= 14.0) {
                            inCalderaSpillway = true;
                            break;
                        }
                    }

                    if (inCalderaSpillway && craterNorm > 0.65) {
                        wallTopY = Math.min(wallTopY, lavaSurfaceY - 1);
                    }

                    int startY = Math.min(localGroundY - 6, craterFloorY - 12);

                    for (int y = startY; y <= Math.max(localGroundY + 4, rimPeakY + 3); y++) {
                        cursor.set(worldX, y, worldZ);
                        if (y < craterFloorY) {
                            // Deep magma throat & intrusive rock beneath caldera
                            if (craterNorm < 0.45) {
                                BlockState fluidState = config.fluidBlock().getState(colRand, cursor);
                                level.setBlock(cursor, fluidState, 2);
                                if (fluidState.is(Blocks.LAVA) || !fluidState.getFluidState().isEmpty()) {
                                    level.scheduleTick(cursor, Fluids.LAVA, Fluids.LAVA.getTickDelay(level));
                                }
                            } else if (craterNorm < 0.72) {
                                level.setBlock(cursor, colRand.nextInt(3) == 0 ? config.magmaRock().getState(colRand, cursor) : Blocks.OBSIDIAN.defaultBlockState(), 2);
                            } else {
                                level.setBlock(cursor, config.intrusiveRock().getState(colRand, cursor), 2);
                            }
                        } else if (y <= wallTopY) {
                            // Inner caldera wall
                            level.setBlock(cursor, pickCalderaWallBlock(config, colRand, cursor, y, lavaSurfaceY), 2);
                        } else if (y <= lavaSurfaceY) {
                            // Caldera lava lake
                            BlockState fluidState = config.fluidBlock().getState(colRand, cursor);
                            level.setBlock(cursor, fluidState, 2);
                            if (fluidState.is(Blocks.LAVA) || !fluidState.getFluidState().isEmpty()) {
                                level.scheduleTick(cursor, Fluids.LAVA, Fluids.LAVA.getTickDelay(level));
                            }
                        } else {
                            // Clear air above caldera lava lake
                            if (!level.getBlockState(cursor).isAir()) {
                                level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                            }
                        }
                    }
                    placedAny = true;
                } else {
                    // Outer volcanic slopes (ported from CenteredFeatureNoise.calculateEasing)
                    double u = (rawDist - effectiveCraterRadius) / Math.max(1.0, effectiveOuterRadius - effectiveCraterRadius);
                    u = Mth.clamp(u, 0.0, 1.0);

                    double easing = computeVolcanoEasing(morphType, u);
                    // Smoothly blend base reference height from localGroundY at outer skirt to centerGroundY at crater rim
                    // so the caldera rim is level while the outer skirt blends seamlessly into any terrain slope
                    double rimAnchorBlend = Math.pow(1.0 - u, 1.65);
                    double baseRefY = Mth.lerp(rimAnchorBlend, localGroundY, centerGroundY);

                    // Organic Hermite cubic smoothstep transition at the volcano apron (u in [0.72, 1.0])
                    double skirtFade = u >= 0.72 ? (1.0 - (u - 0.72) / 0.28) : 1.0;
                    skirtFade = skirtFade * skirtFade * (3.0 - 2.0 * skirtFade);

                    double ridgeDetail = (1.0 - u) * u * (angularRidge * 14.0 + spatialNoise * 10.0);
                    int targetSurfaceY = Mth.floor(baseRefY + easing * volcanoHeight * skirtFade + ridgeDetail * skirtFade);

                    if (targetSurfaceY <= localGroundY && u > 0.88) {
                        continue;
                    }

                    // Check if this column lies along a radial lava flow channel
                    boolean inLavaChannel = false;
                    boolean onChannelBank = false;
                    if (u < 0.88) {
                        double meander = Math.sin(rawDist * 0.18 + DensityCellular2D.jitter(cellHash) * 5.0) * 0.14;
                        for (double flowAngle : flowAngles) {
                            double angleDiff = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(angle - (flowAngle + meander))));
                            double channelHalfWidthDeg = Mth.lerp(u, 13.0, 4.5);
                            if (angleDiff <= channelHalfWidthDeg) {
                                inLavaChannel = true;
                                break;
                            } else if (angleDiff <= channelHalfWidthDeg + 6.0) {
                                onChannelBank = true;
                            }
                        }
                    }

                    int fillStartY = Math.min(localGroundY - 3, centerGroundY - 2);
                    // The channel begins at lavaSurfaceY at the caldera rim (u = 0) and cascades down to localGroundY
                    double channelGradientY = Mth.lerp(u / 0.88, (double) lavaSurfaceY, (double) localGroundY);
                    int finalTopY = inLavaChannel
                            ? Math.max(localGroundY, (int) Math.min(targetSurfaceY - 2, Math.round(channelGradientY)))
                            : targetSurfaceY;

                    for (int y = fillStartY; y <= finalTopY; y++) {
                        cursor.set(worldX, y, worldZ);
                        int depthFromTop = finalTopY - y;
                        BlockState stateToPlace;

                        if (inLavaChannel && depthFromTop == 0 && u < 0.88) {
                            stateToPlace = config.fluidBlock().getState(colRand, cursor);
                        } else if (inLavaChannel && depthFromTop <= 2) {
                            stateToPlace = config.magmaRock().getState(colRand, cursor);
                        } else if (onChannelBank && depthFromTop <= 2) {
                            stateToPlace = colRand.nextInt(3) == 0
                                    ? config.magmaRock().getState(colRand, cursor)
                                    : config.scorchedRock().getState(colRand, cursor);
                        } else {
                            stateToPlace = pickIgneousSlopeRock(config, colRand, cursor, u, depthFromTop);
                        }

                        level.setBlock(cursor, stateToPlace, 2);
                        if (stateToPlace.is(Blocks.LAVA) || !stateToPlace.getFluidState().isEmpty()) {
                            level.scheduleTick(cursor, Fluids.LAVA, Fluids.LAVA.getTickDelay(level));
                        }
                    }

                    // Optional fumarole vapor spouts on upper-middle flanks
                    if (config.spawnFumaroles() && !inLavaChannel && u > 0.15 && u < 0.55 && colRand.nextInt(190) == 0) {
                        cursor.set(worldX, finalTopY, worldZ);
                        level.setBlock(cursor, GCBlocks.VAPOR_SPOUT.defaultBlockState(), 2);
                        cursor.set(worldX, finalTopY - 1, worldZ);
                        level.setBlock(cursor, config.magmaRock().getState(colRand, cursor), 2);
                    }

                    // PyroclasticBomb scoria & ore boulder deposits on slopes (ported from PyroclasticBomb.placeBlocks)
                    if (config.spawnPyroclasticScoria() && !inLavaChannel && u > 0.18 && u < 0.92 && lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13) {
                        int bombChance = u < 0.48 ? 42 : 75;
                        if (colRand.nextInt(bombChance) == 0) {
                            placePyroclasticBombBlocks(level, new BlockPos(worldX, finalTopY + 1, worldZ), colRand, config);
                        }
                    }

                    placedAny = true;
                }
            }
        }

        return placedAny;
    }

    /**
     * Ported from {@code TremorEvent.VolcanoType} & {@code CenteredFeatureNoise.calculateEasing}:
     * computes the normalized slope height in [0.0, 1.0] for distance fraction {@code u} in [0.0, 1.0].
     */
    private static double computeVolcanoEasing(VolcanoType type, double u) {
        double inv = 1.0 - u;
        return switch (type) {
            case STRATOVOLCANO -> Math.pow(inv, 1.65) * (0.85 + 0.15 * inv);
            case SHIELD -> Math.pow(1.0 - u * u, 1.45);
            case TUFF_RING -> Math.pow(inv, 2.2) * (1.0 + 0.6 * u);
            default -> Math.pow(inv, 1.35) * (inv * (1.0 + 0.35 * u));
        };
    }

    private static VolcanoType resolveVolcanoType(VolcanoType configured, long cellHash) {
        if (configured != VolcanoType.MIXED) {
            return configured;
        }
        int pick = (int) (Math.abs(cellHash >> 17) % 4);
        return switch (pick) {
            case 0 -> VolcanoType.STRATOVOLCANO;
            case 1 -> VolcanoType.SHIELD;
            case 2 -> VolcanoType.CINDER_CONE;
            default -> VolcanoType.TUFF_RING;
        };
    }

    /**
     * Ported from {@code RockRegionHelper.getIgneousRockAt}:
     * uses extrusive rocks (Basalt, Scorched Rock, Volcanic Rock) near the surface and rim,
     * and intrusive rocks (Hard Rock, Basalt) deeper inside the volcanic edifice.
     */
    private static BlockState pickIgneousSlopeRock(
            PlanetaryVolcanoConfig config,
            RandomSource rand,
            BlockPos pos,
            double u,
            int depthFromTop
    ) {
        if (depthFromTop <= 2) {
            // Near crater rim (u < 0.25): high heat magma, obsidian, and scorched volcanic rock
            if (u < 0.22) {
                int r = rand.nextInt(10);
                if (r < 2) return config.magmaRock().getState(rand, pos);
                if (r < 4) return Blocks.OBSIDIAN.defaultBlockState();
                if (r < 8) return config.scorchedRock().getState(rand, pos);
                return config.extrusiveRock().getState(rand, pos);
            }
            // Middle flanks (0.22 <= u < 0.65): basalt, smooth basalt, and scorched rock
            if (u < 0.65) {
                int r = rand.nextInt(10);
                if (r == 0) return config.magmaRock().getState(rand, pos);
                if (r < 4) return config.scorchedRock().getState(rand, pos);
                if (r < 6) return Blocks.SMOOTH_BASALT.defaultBlockState();
                return config.extrusiveRock().getState(rand, pos);
            }
            // Lower apron (u >= 0.65): transitions smoothly from basalt/extrusive rock into native rock
            if (rand.nextFloat() < (float) ((u - 0.65) / 0.35)) {
                return config.intrusiveRock().getState(rand, pos);
            }
            return rand.nextInt(3) == 0 ? config.scorchedRock().getState(rand, pos) : config.extrusiveRock().getState(rand, pos);
        }
        return depthFromTop <= 5 && rand.nextBoolean()
                ? config.extrusiveRock().getState(rand, pos)
                : config.intrusiveRock().getState(rand, pos);
    }

    private static BlockState pickCalderaWallBlock(
            PlanetaryVolcanoConfig config,
            RandomSource rand,
            BlockPos pos,
            int y,
            int lavaSurfaceY
    ) {
        if (y <= lavaSurfaceY + 2) {
            int r = rand.nextInt(8);
            if (r < 3) return config.magmaRock().getState(rand, pos);
            if (r < 5) return Blocks.OBSIDIAN.defaultBlockState();
            if (r == 5) return Blocks.CRYING_OBSIDIAN.defaultBlockState();
        }
        return rand.nextInt(3) == 0
                ? config.scorchedRock().getState(rand, pos)
                : config.extrusiveRock().getState(rand, pos);
    }

    /**
     * Directly ported from {@code tfcvolcanoes.common.entities.PyroclasticBomb#placeBlocks(Level, BlockPos, RandomSource)}.
     * Places pyroclastic scoria clusters, volcanic pillars, and embedded ore bombs on the volcano slopes.
     */
    public static void placePyroclasticBombBlocks(
            WorldGenLevel level,
            BlockPos pos,
            RandomSource random,
            PlanetaryVolcanoConfig config
    ) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        if (random.nextFloat() < 0.5F) {
            setPyroclasticBlockCheck(level, pos, random, config);
            for (Direction dir : ALL_DIRECTIONS) {
                if (random.nextFloat() < 0.8F) {
                    mutablePos.setWithOffset(pos, dir);
                    setPyroclasticBlockCheck(level, mutablePos, random, config);
                }
            }
        } else if (random.nextFloat() < 0.5F) {
            mutablePos.setWithOffset(pos, 0, -2, 0);
            int height = 3 + random.nextInt(3);
            for (int i = 0; i < height; i++) {
                setPyroclasticBlockCheck(level, mutablePos, random, config);
                mutablePos.move(0, 1, 0);
            }

            mutablePos.setWithOffset(pos, 0, -2, 0);
            mutablePos.move(Direction.Plane.HORIZONTAL.getRandomDirection(random));
            for (int i = 0; i < height - 1; i++) {
                setPyroclasticBlockCheck(level, mutablePos, random, config);
                mutablePos.move(0, 1, 0);
            }
        } else {
            mutablePos.set(pos);
            setPyroclasticBlockCheck(level, mutablePos, random, config);
            mutablePos.move(0, -1, 0);
            setPyroclasticBlockCheck(level, mutablePos, random, config);
        }
    }

    private static void setPyroclasticBlockCheck(
            WorldGenLevel level,
            BlockPos pos,
            RandomSource random,
            PlanetaryVolcanoConfig config
    ) {
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || (existing.canBeReplaced() && existing.getFluidState().isEmpty())) {
            level.setBlock(pos, getRandomPyroclasticRock(pos, random, config), 2);
        }
    }

    /**
     * Directly ported from {@code tfcvolcanoes.common.entities.PyroclasticBomb#getRandomRock()}.
     * Selects extrusive scoria/basalt rock, with configured chances for rich ore ejecta,
     * obsidian/crying obsidian, and glowing magma blocks.
     */
    public static BlockState getRandomPyroclasticRock(
            BlockPos pos,
            RandomSource random,
            PlanetaryVolcanoConfig config
    ) {
        BlockState placedBlock = random.nextInt(3) == 0
                ? config.scorchedRock().getState(random, pos)
                : config.extrusiveRock().getState(random, pos);

        List<BlockState> ores = config.pyroclasticOres();
        if (!ores.isEmpty() && random.nextInt(Math.max(1, config.pyroclasticOreChance())) == 0) {
            if (random.nextInt(10) == 0) {
                return random.nextInt(10) == 0
                        ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : Blocks.OBSIDIAN.defaultBlockState();
            }
            return ores.get(random.nextInt(ores.size()));
        }

        if (random.nextInt(8) == 0) {
            return config.magmaRock().getState(random, pos);
        }

        return placedBlock;
    }
}
