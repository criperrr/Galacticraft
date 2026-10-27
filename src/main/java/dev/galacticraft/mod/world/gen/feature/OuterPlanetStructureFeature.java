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
import dev.galacticraft.mod.world.gen.feature.config.OuterPlanetStructureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * High-performance parameterized terrain structure generator for the outer Solar System planets
 * (Jupiter, Saturn, Uranus, and Neptune).
 *
 * <p>Operates strictly within the safe 3x3 chunk neighborhood (maximum horizontal radius at most 12 blocks),
 * reuses a single {@link BlockPos.MutableBlockPos} per placement, uses quadratic distance comparisons
 * instead of {@code Math.sqrt} in inner loops, and places blocks with {@link Block#UPDATE_CLIENTS}
 * to avoid expensive neighbor shape/lighting cascades during world generation.
 */
public class OuterPlanetStructureFeature extends Feature<OuterPlanetStructureConfig> {
    private static final double SQRT_3 = 1.7320508075688772;

    public OuterPlanetStructureFeature(Codec<OuterPlanetStructureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OuterPlanetStructureConfig> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        OuterPlanetStructureConfig config = context.config();

        return switch (config.kind()) {
            case TILTED_SPIRE -> placeTiltedSpire(level, random, origin, config);
            case LIGHTNING_FULGURITE -> placeLightningFulgurite(level, random, origin, config);
            case HEXAGON_TERRACE -> placeHexagonTerrace(level, random, origin, config);
            case WIND_RIDGE -> placeWindRidge(level, random, origin, config);
            case CRYO_GEYSER -> placeCryoGeyser(level, random, origin, config);
            case ABYSSAL_SPELEOTHEM -> placeAbyssalSpeleothem(level, random, origin, config);
            case DIAMONDBERG -> placeDiamondberg(level, random, origin, config);
        };
    }

    private static BlockPos findSurfaceGround(WorldGenLevel level, BlockPos origin, BlockPos.MutableBlockPos cursor) {
        cursor.set(origin);
        for (int i = 0; i < 8 && level.getBlockState(cursor).isAir(); i++) {
            cursor.move(Direction.DOWN);
        }
        BlockState state = level.getBlockState(cursor);
        if (state.isAir() || state.is(Blocks.BEDROCK) || !state.getFluidState().isEmpty()) {
            return null;
        }
        return cursor.immutable();
    }

    /**
     * Generates tilted crystalline/glacial spires (Uranus 98-degree axial-tilt methane spires,
     * Saturn Ring Rain ice monoliths, and Jupiter ammonia crystal spires).
     */
    private boolean placeTiltedSpire(WorldGenLevel level, RandomSource random, BlockPos origin, OuterPlanetStructureConfig config) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos basePos = findSurfaceGround(level, origin, cursor);
        if (basePos == null) {
            return false;
        }

        int height = Mth.randomBetweenInclusive(random, config.minHeight(), config.maxHeight());
        double baseRadius = Mth.lerp(random.nextDouble(), config.minRadius(), config.maxRadius());

        // Directional axial tilt bias on X plus slight random variation on Z
        double tiltX = (0.45 + random.nextDouble() * 0.45) * config.tiltStrength();
        double tiltZ = (random.nextDouble() - 0.5) * 0.50 * config.tiltStrength();

        int baseX = basePos.getX();
        int baseY = basePos.getY();
        int baseZ = basePos.getZ();

        for (int dy = -2; dy <= height; dy++) {
            double progress = dy <= 0 ? 0.0 : (double) dy / (double) height;
            double currentRadius = Math.max(0.65, baseRadius * (1.0 - progress * 0.80));
            double radiusSq = currentRadius * currentRadius;
            double coreRadiusSq = radiusSq * 0.22;
            int rCeil = Mth.ceil(currentRadius + 0.5);

            double maxShift = Math.max(0.0, 12.0 - rCeil);
            double centerX = baseX + Mth.clamp(tiltX * Math.max(0, dy), -maxShift, maxShift);
            double centerZ = baseZ + Mth.clamp(tiltZ * Math.max(0, dy), -maxShift, maxShift);
            int minX = Mth.floor(centerX - rCeil);
            int maxX = Mth.ceil(centerX + rCeil);
            int minZ = Mth.floor(centerZ - rCeil);
            int maxZ = Mth.ceil(centerZ + rCeil);

            for (int wx = minX; wx <= maxX; wx++) {
                double dx = wx - centerX;
                for (int wz = minZ; wz <= maxZ; wz++) {
                    double dz = wz - centerZ;
                    double distSq = dx * dx + dz * dz;
                    double jitter = (random.nextDouble() - 0.5) * 0.45 * currentRadius;
                    if (distSq > radiusSq + jitter) {
                        continue;
                    }

                    cursor.set(wx, baseY + dy, wz);
                    BlockState existing = level.getBlockState(cursor);
                    if (existing.is(Blocks.BEDROCK)) {
                        continue;
                    }

                    BlockState toPlace;
                    if (progress > 0.85) {
                        toPlace = config.accentOrCrownBlock().getState(random, cursor);
                    } else if (distSq <= coreRadiusSq) {
                        toPlace = config.coreOrFluidBlock().getState(random, cursor);
                    } else if (random.nextInt(3) == 0) {
                        toPlace = config.secondaryBlock().getState(random, cursor);
                    } else {
                        toPlace = config.primaryBlock().getState(random, cursor);
                    }
                    level.setBlock(cursor, toPlace, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /**
     * Generates vitrified, branching lightning fulgurite towers on Jupiter with a glowing conductive core.
     */
    private boolean placeLightningFulgurite(WorldGenLevel level, RandomSource random, BlockPos origin, OuterPlanetStructureConfig config) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos basePos = findSurfaceGround(level, origin, cursor);
        if (basePos == null) {
            return false;
        }

        int height = Mth.randomBetweenInclusive(random, config.minHeight(), config.maxHeight());
        double baseRadius = Mth.lerp(random.nextDouble(), config.minRadius(), config.maxRadius());
        double phaseX = random.nextDouble() * Mth.TWO_PI;
        double phaseZ = random.nextDouble() * Mth.TWO_PI;

        int baseX = basePos.getX();
        int baseY = basePos.getY();
        int baseZ = basePos.getZ();

        for (int dy = -3; dy <= height; dy++) {
            double progress = dy <= 0 ? 0.0 : (double) dy / (double) height;
            double currentRadius = Math.max(0.85, baseRadius * (1.0 - progress * 0.65));
            double radiusSq = currentRadius * currentRadius;

            double centerX = baseX + Math.sin(dy * 0.45 + phaseX) * 1.6 * config.tiltStrength();
            double centerZ = baseZ + Math.cos(dy * 0.40 + phaseZ) * 1.6 * config.tiltStrength();
            int rCeil = Mth.ceil(currentRadius + 1.0);
            int minX = Mth.floor(centerX - rCeil);
            int maxX = Mth.ceil(centerX + rCeil);
            int minZ = Mth.floor(centerZ - rCeil);
            int maxZ = Mth.ceil(centerZ + rCeil);

            for (int wx = minX; wx <= maxX; wx++) {
                double dx = wx - centerX;
                for (int wz = minZ; wz <= maxZ; wz++) {
                    double dz = wz - centerZ;
                    double distSq = dx * dx + dz * dz;
                    if (distSq > radiusSq + (random.nextDouble() - 0.5) * 0.5) {
                        continue;
                    }

                    cursor.set(wx, baseY + dy, wz);
                    if (level.getBlockState(cursor).is(Blocks.BEDROCK)) {
                        continue;
                    }

                    BlockState toPlace;
                    if (distSq <= 0.65) {
                        toPlace = config.coreOrFluidBlock().getState(random, cursor);
                    } else if (dy <= 1 && random.nextInt(3) == 0) {
                        toPlace = config.accentOrCrownBlock().getState(random, cursor);
                    } else if (random.nextInt(3) == 0) {
                        toPlace = config.secondaryBlock().getState(random, cursor);
                    } else {
                        toPlace = config.primaryBlock().getState(random, cursor);
                    }
                    level.setBlock(cursor, toPlace, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /**
     * Generates stepped 6-sided geometric terraces on Saturn (North Polar Hexagon wave structures).
     */
    private boolean placeHexagonTerrace(WorldGenLevel level, RandomSource random, BlockPos origin, OuterPlanetStructureConfig config) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos basePos = findSurfaceGround(level, origin, cursor);
        if (basePos == null) {
            return false;
        }

        int totalHeight = Mth.randomBetweenInclusive(random, config.minHeight(), config.maxHeight());
        double outerRadius = Mth.lerp(random.nextDouble(), config.minRadius(), config.maxRadius());

        int baseX = basePos.getX();
        int baseY = basePos.getY();
        int baseZ = basePos.getZ();

        for (int dy = -2; dy <= totalHeight; dy++) {
            double stepProgress = dy <= 0 ? 0.0 : (double) (dy / 2) / Math.max(1.0, (double) (totalHeight / 2));
            double hexRadius = Math.max(1.5, outerRadius * (1.0 - stepProgress * 0.68));
            int rCeil = Mth.ceil(hexRadius + 1.0);

            for (int dx = -rCeil; dx <= rCeil; dx++) {
                double absX = Math.abs(dx);
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    double absZ = Math.abs(dz);
                    // Exact L6 hexagonal norm
                    double hexDist = Math.max(absX, 0.5 * (absX + SQRT_3 * absZ));
                    if (hexDist > hexRadius) {
                        continue;
                    }

                    cursor.set(baseX + dx, baseY + dy, baseZ + dz);
                    if (level.getBlockState(cursor).is(Blocks.BEDROCK)) {
                        continue;
                    }

                    BlockState toPlace;
                    if (hexDist >= hexRadius - 0.85) {
                        toPlace = config.accentOrCrownBlock().getState(random, cursor);
                    } else if (hexDist <= 1.25 && dy == totalHeight) {
                        toPlace = config.coreOrFluidBlock().getState(random, cursor);
                    } else if ((dy & 1) == 0) {
                        toPlace = config.primaryBlock().getState(random, cursor);
                    } else {
                        toPlace = config.secondaryBlock().getState(random, cursor);
                    }
                    level.setBlock(cursor, toPlace, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /**
     * Generates aerodynamic East-West supersonic wind-carved ridges (yardangs) on Neptune and frozen sulfide outcrops on Uranus.
     */
    private boolean placeWindRidge(WorldGenLevel level, RandomSource random, BlockPos origin, OuterPlanetStructureConfig config) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos basePos = findSurfaceGround(level, origin, cursor);
        if (basePos == null) {
            return false;
        }

        int height = Mth.randomBetweenInclusive(random, config.minHeight(), config.maxHeight());
        double longRadius = Mth.lerp(random.nextDouble(), config.minRadius(), config.maxRadius());
        double shortRadius = Math.max(1.8, longRadius * 0.42);

        int baseX = basePos.getX();
        int baseY = basePos.getY();
        int baseZ = basePos.getZ();

        int maxDx = Mth.ceil(longRadius + 2.0);
        int maxDz = Mth.ceil(shortRadius + 1.0);

        for (int dy = -2; dy <= height; dy++) {
            double progress = dy <= 0 ? 0.0 : (double) dy / (double) height;
            double curLong = Math.max(1.2, longRadius * (1.0 - progress * 0.55));
            double curShort = Math.max(0.8, shortRadius * (1.0 - progress * 0.72));
            double invLongSq = 1.0 / (curLong * curLong);
            double invShortSq = 1.0 / (curShort * curShort);
            double windShearX = progress * curLong * 0.35 * config.tiltStrength();

            for (int dx = -maxDx; dx <= maxDx; dx++) {
                double shiftedDx = dx - windShearX;
                double normX = shiftedDx * shiftedDx * invLongSq;
                if (normX > 1.05) {
                    continue;
                }
                for (int dz = -maxDz; dz <= maxDz; dz++) {
                    double normDist = normX + dz * dz * invShortSq;
                    if (normDist > 1.0 + (random.nextDouble() - 0.5) * 0.15) {
                        continue;
                    }

                    cursor.set(baseX + dx, baseY + dy, baseZ + dz);
                    if (level.getBlockState(cursor).is(Blocks.BEDROCK)) {
                        continue;
                    }

                    BlockState toPlace;
                    if (normDist < 0.22 && random.nextInt(4) == 0) {
                        toPlace = config.accentOrCrownBlock().getState(random, cursor);
                    } else if (normDist < 0.45) {
                        toPlace = config.coreOrFluidBlock().getState(random, cursor);
                    } else if (random.nextInt(3) == 0) {
                        toPlace = config.secondaryBlock().getState(random, cursor);
                    } else {
                        toPlace = config.primaryBlock().getState(random, cursor);
                    }
                    level.setBlock(cursor, toPlace, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /**
     * Generates active cryovolcanic nitrogen geysers (Neptune / Triton style) with a subterranean chamber,
     * vertical conduit, eruptive cone crowned by {@code VAPOR_SPOUT}, and downwind dark soot streak.
     */
    private boolean placeCryoGeyser(WorldGenLevel level, RandomSource random, BlockPos origin, OuterPlanetStructureConfig config) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos basePos = findSurfaceGround(level, origin, cursor);
        if (basePos == null) {
            return false;
        }

        int baseX = basePos.getX();
        int surfaceY = basePos.getY();
        int baseZ = basePos.getZ();

        int coneHeight = Mth.randomBetweenInclusive(random, config.minHeight(), config.maxHeight());
        int chamberDepth = 16 + random.nextInt(10);
        int chamberY = Math.max(level.getMinBuildHeight() + 12, surfaceY - chamberDepth);
        double chamberRadius = Mth.lerp(random.nextDouble(), config.minRadius(), config.maxRadius());
        double outerShellSq = (chamberRadius + 1.4) * (chamberRadius + 1.4);
        double innerCavitySq = chamberRadius * chamberRadius;
        int rCeil = Mth.ceil(chamberRadius + 2.0);

        // 1. Subterranean pressurized cryomagma chamber
        for (int dy = -rCeil; dy <= rCeil; dy++) {
            int wy = chamberY + dy;
            for (int dx = -rCeil; dx <= rCeil; dx++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    double distSq = dx * dx + dy * dy * 1.3 + dz * dz;
                    if (distSq > outerShellSq) {
                        continue;
                    }
                    cursor.set(baseX + dx, wy, baseZ + dz);
                    if (level.getBlockState(cursor).is(Blocks.BEDROCK)) {
                        continue;
                    }
                    if (distSq <= innerCavitySq) {
                        BlockState fill = dy <= 0
                                ? config.coreOrFluidBlock().getState(random, cursor)
                                : Blocks.AIR.defaultBlockState();
                        level.setBlock(cursor, fill, Block.UPDATE_CLIENTS);
                    } else {
                        level.setBlock(cursor, config.primaryBlock().getState(random, cursor), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        // 2. Vertical conduit and surface eruptive cone
        for (int wy = chamberY; wy <= surfaceY + coneHeight; wy++) {
            double coneProgress = wy <= surfaceY ? 0.0 : (double) (wy - surfaceY) / (double) coneHeight;
            double wallRadius = wy <= surfaceY
                    ? 2.2
                    : Mth.lerp(coneProgress, 3.8, 1.5);
            double wallRadiusSq = wallRadius * wallRadius;
            int cr = Mth.ceil(wallRadius + 0.5);

            for (int dx = -cr; dx <= cr; dx++) {
                for (int dz = -cr; dz <= cr; dz++) {
                    double distSq = dx * dx + dz * dz;
                    if (distSq > wallRadiusSq) {
                        continue;
                    }
                    cursor.set(baseX + dx, wy, baseZ + dz);
                    if (dx == 0 && dz == 0) {
                        BlockState ventState = (wy == surfaceY + coneHeight)
                                ? config.accentOrCrownBlock().getState(random, cursor)
                                : Blocks.AIR.defaultBlockState();
                        level.setBlock(cursor, ventState, Block.UPDATE_CLIENTS);
                    } else {
                        BlockState rimState = (wy >= surfaceY && random.nextInt(3) == 0)
                                ? config.secondaryBlock().getState(random, cursor)
                                : config.primaryBlock().getState(random, cursor);
                        level.setBlock(cursor, rimState, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        // 3. Downwind (+X) dark hydrocarbon/nitrogen soot fan on the surrounding surface
        for (int dx = 2; dx <= 8; dx++) {
            int fanWidth = 1 + (dx / 3);
            for (int dz = -fanWidth; dz <= fanWidth; dz++) {
                if (random.nextInt(3) == 0) {
                    continue;
                }
                cursor.set(baseX + dx, surfaceY + 3, baseZ + dz);
                BlockPos fanGround = findSurfaceGround(level, cursor, cursor);
                if (fanGround != null) {
                    level.setBlock(fanGround, config.secondaryBlock().getState(random, fanGround), Block.UPDATE_CLIENTS);
                }
            }
        }

        return true;
    }

    /**
     * Generates monumental subterranean speleothems and hourglass pillars (Uranus superionic ice pillars,
     * Saturn helium-rain columns, and Jupiter metallic hydrogen columns).
     */
    private boolean placeAbyssalSpeleothem(WorldGenLevel level, RandomSource random, BlockPos origin, OuterPlanetStructureConfig config) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        if (!level.getBlockState(cursor).isAir()) {
            boolean foundAir = false;
            for (int dy = 1; dy <= 20; dy++) {
                cursor.set(origin.getX(), origin.getY() + dy, origin.getZ());
                if (level.getBlockState(cursor).isAir()) {
                    foundAir = true;
                    break;
                }
                cursor.set(origin.getX(), origin.getY() - dy, origin.getZ());
                if (level.getBlockState(cursor).isAir()) {
                    foundAir = true;
                    break;
                }
            }
            if (!foundAir) {
                return false;
            }
        }

        int probeY = cursor.getY();
        int maxScanY = Math.min(level.getMaxBuildHeight() - 6, probeY + config.maxHeight());
        int minScanY = Math.max(level.getMinBuildHeight() + 6, probeY - config.maxHeight());

        int ceilingY = -999;
        for (int y = probeY; y <= maxScanY; y++) {
            cursor.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                if (!state.is(Blocks.BEDROCK) && state.getFluidState().isEmpty()) {
                    ceilingY = y;
                }
                break;
            }
        }

        int floorY = -999;
        for (int y = probeY; y >= minScanY; y--) {
            cursor.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir()) {
                if (!state.is(Blocks.BEDROCK)) {
                    floorY = y;
                }
                break;
            }
        }

        if (ceilingY == -999 || floorY == -999) {
            return false;
        }

        int clearance = ceilingY - floorY;
        if (clearance < config.minHeight() || clearance > config.maxHeight() + 24) {
            return false;
        }

        double endRadius = Mth.lerp(random.nextDouble(), config.minRadius(), config.maxRadius());
        double waistRadius = Math.max(1.1, endRadius * 0.58);
        double waistY = floorY + clearance * 0.5;
        int centerX = origin.getX();
        int centerZ = origin.getZ();

        for (int y = floorY; y <= ceilingY; y++) {
            double t = Math.abs(y - waistY) / Math.max(1.0, clearance * 0.5);
            double radius = Mth.lerp(t * t, waistRadius, endRadius);
            double radiusSq = radius * radius;
            double coreRadiusSq = Math.max(0.65, radiusSq * 0.24);
            int rCeil = Mth.ceil(radius + 0.5);

            for (int dx = -rCeil; dx <= rCeil; dx++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    double distSq = dx * dx + dz * dz;
                    if (distSq > radiusSq + (random.nextDouble() - 0.5) * 0.4) {
                        continue;
                    }

                    cursor.set(centerX + dx, y, centerZ + dz);
                    if (level.getBlockState(cursor).is(Blocks.BEDROCK)) {
                        continue;
                    }

                    BlockState toPlace;
                    if (distSq <= coreRadiusSq) {
                        toPlace = (y % 4 == 0)
                                ? config.accentOrCrownBlock().getState(random, cursor)
                                : config.coreOrFluidBlock().getState(random, cursor);
                    } else if (random.nextInt(3) == 0) {
                        toPlace = config.secondaryBlock().getState(random, cursor);
                    } else {
                        toPlace = config.primaryBlock().getState(random, cursor);
                    }
                    level.setBlock(cursor, toPlace, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /**
     * Generates massive diamondbergs floating/anchored in the deep mantle caverns of Neptune and Uranus.
     */
    private boolean placeDiamondberg(WorldGenLevel level, RandomSource random, BlockPos origin, OuterPlanetStructureConfig config) {
        BlockPos.MutableBlockPos cursor = origin.mutable();
        // Ensure we are near a deep cavern floor or open mantle cavity
        boolean hasCavity = false;
        for (int dy = -8; dy <= 8; dy++) {
            cursor.set(origin.getX(), origin.getY() + dy, origin.getZ());
            if (level.getBlockState(cursor).isAir()) {
                hasCavity = true;
                break;
            }
        }
        if (!hasCavity) {
            return false;
        }

        int centerX = cursor.getX();
        int centerY = cursor.getY();
        int centerZ = cursor.getZ();

        int totalHeight = Mth.randomBetweenInclusive(random, config.minHeight(), config.maxHeight());
        int crownHeight = Math.max(4, (int) (totalHeight * 0.42));
        int keelDepth = Math.max(5, totalHeight - crownHeight);
        double maxRadius = Mth.lerp(random.nextDouble(), config.minRadius(), config.maxRadius());

        for (int dy = -keelDepth; dy <= crownHeight; dy++) {
            int wy = centerY + dy;
            if (wy <= level.getMinBuildHeight() + 5 || wy >= level.getMaxBuildHeight() - 5) {
                continue;
            }

            double verticalProgress = dy >= 0
                    ? (double) dy / (double) crownHeight
                    : (double) (-dy) / (double) keelDepth;
            double sliceRadius = Math.max(0.8, maxRadius * (1.0 - Math.pow(verticalProgress, 0.85)));
            double sliceRadiusSq = sliceRadius * sliceRadius;
            double innerCoreSq = sliceRadiusSq * 0.25;
            double midMantleSq = sliceRadiusSq * 0.55;
            int rCeil = Mth.ceil(sliceRadius + 0.5);

            for (int dx = -rCeil; dx <= rCeil; dx++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    // Faceted crystalline metric combining Euclidean and Manhattan norms
                    double distSq = dx * dx + dz * dz + 0.25 * Math.abs(dx * dz);
                    if (distSq > sliceRadiusSq + (random.nextDouble() - 0.5) * 0.45) {
                        continue;
                    }

                    cursor.set(centerX + dx, wy, centerZ + dz);
                    if (level.getBlockState(cursor).is(Blocks.BEDROCK)) {
                        continue;
                    }

                    BlockState toPlace;
                    if (distSq <= innerCoreSq) {
                        toPlace = random.nextInt(3) == 0
                                ? config.accentOrCrownBlock().getState(random, cursor)
                                : config.coreOrFluidBlock().getState(random, cursor);
                    } else if (distSq <= midMantleSq) {
                        toPlace = random.nextBoolean()
                                ? config.coreOrFluidBlock().getState(random, cursor)
                                : config.secondaryBlock().getState(random, cursor);
                    } else if (random.nextInt(4) == 0) {
                        toPlace = config.secondaryBlock().getState(random, cursor);
                    } else {
                        toPlace = config.primaryBlock().getState(random, cursor);
                    }
                    level.setBlock(cursor, toPlace, Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }
}
