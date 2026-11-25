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

package dev.galacticraft.mod.world.gen.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.api.vector.BlockVec3;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.world.biome.GCBiomes;
import dev.galacticraft.mod.world.gen.base.MapGenAbandonedBase;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Thread-safe, deterministic 3D Asteroid Belt chunk generator.
 *
 * <p>Uses a jittered 3D spatial cell grid ({@code 48x48x48}) combined with 4 spectral belt biomes
 * (C-type Carbonaceous, S-type Silicate, M-type Metallic, and Kirkwood Resonance Gaps),
 * three 3D morphologies (rubble-pile, contact binary, elongated cometary fragment),
 * parabolic micro-craters on large bodies ({@code R >= 14}), and micro-debris swarms.
 */
public class AsteroidChunkGenerator extends ChunkGenerator {
    public static final int MIN_Y = -64;
    public static final int GEN_DEPTH = 384;
    public static final int SEA_LEVEL = -64;

    private static final int CELL_SIZE = 48;
    private static final int CELL_MARGIN = 8;
    private static final int MIN_ASTEROID_Y = -20;
    private static final int MAX_ASTEROID_Y = 276;
    private static final int MAX_SEARCH_REACH = 34;

    private static final int MIN_ASTEROID_RADIUS = 6;
    private static final int MAX_ASTEROID_RADIUS = 21;
    private static final int MIN_CRATER_RADIUS_THRESHOLD = 14;

    private static final BlockState[] EMPTY_COLUMN = new BlockState[0];

    public static final MapCodec<AsteroidChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biomeSource").forGetter(generator -> generator.biomeSource),
                    ServerLevel.RESOURCE_KEY_CODEC.fieldOf("dimensionKey").forGetter(generator -> generator.dimensionKey),
                    Codec.LONG.fieldOf("par2").forGetter(generator -> generator.seed)
            ).apply(instance, AsteroidChunkGenerator::new));

    private final ResourceKey<Level> dimensionKey;
    private final long seed;
    private final Climate.Sampler climateSampler;

    public AsteroidChunkGenerator(BiomeSource biomeSource, ResourceKey<Level> dimensionKey, long seed) {
        super(biomeSource);
        this.dimensionKey = dimensionKey;
        this.seed = seed;
        this.climateSampler = createAsteroidClimateSampler(seed);
    }

    public enum SpectralType {
        CARBONACEOUS,
        SILICATE,
        METALLIC,
        KIRKWOOD_GAP
    }

    private enum Morphology {
        RUBBLE_PILE,
        CONTACT_BINARY,
        COMETARY_FRAGMENT
    }

    private record Crater(
            double centerX,
            double centerY,
            double centerZ,
            double normalX,
            double normalY,
            double normalZ,
            double radiusSq,
            double invRadiusSq,
            double depth
    ) {
    }

    private record AsteroidBody(
            int centerX,
            int centerY,
            int centerZ,
            int radius,
            SpectralType spectralType,
            Morphology morphology,
            // Primary ellipsoid inverse squared radii
            double invRxSq,
            double invRySq,
            double invRzSq,
            // Local orientation basis (orthonormal 3x3) for elongated/binary bodies
            double axX, double axY, double axZ,
            double ayX, double ayY, double ayZ,
            double azX, double azY, double azZ,
            // Contact binary lobe parameters (used only when morphology == CONTACT_BINARY)
            double lobe1Offset,
            double lobe1InvRSq,
            double lobe2Offset,
            double lobe2InvRSq,
            // Bounding box half-extents for fast AABB culling
            int halfExtentX,
            int halfExtentY,
            int halfExtentZ,
            // Micro-craters (non-empty when radius >= 14)
            Crater[] craters,
            long bodySeed,
            boolean hasAbandonedBase
    ) {
    }

    private static Climate.Sampler createAsteroidClimateSampler(long seed) {
        // Large-scale 2D spectral belt bands (~400-650 blocks wide)
        DensityFunction temperature = new BeltClimateNoise(seed ^ 0x4B1D_7F3A_91E2_05C3L, 1.0 / 480.0);
        DensityFunction continentalness = new BeltClimateNoise(seed ^ 0x6A09_E667_F3BC_C908L, 1.0 / 560.0);
        DensityFunction zero = DensityFunctions.zero();
        return new Climate.Sampler(temperature, zero, continentalness, zero, zero, zero, List.of());
    }

    /**
     * Stateless, thread-safe 2D FBM climate noise in {@code [-1.0, 1.0]} for spectral biome bands.
     */
    private static final class BeltClimateNoise implements DensityFunction.SimpleFunction {
        private final long salt;
        private final double frequency;

        private BeltClimateNoise(long salt, double frequency) {
            this.salt = salt;
            this.frequency = frequency;
        }

        @Override
        public double compute(FunctionContext context) {
            double x = context.blockX() * this.frequency;
            double z = context.blockZ() * this.frequency;
            double n1 = valueNoise2D(x, z, this.salt);
            double n2 = valueNoise2D(x * 2.03 + 17.3, z * 2.03 - 11.7, this.salt ^ 0x9E37_79B9_7F4A_7C15L);
            double n3 = valueNoise2D(x * 4.11 - 23.1, z * 4.11 + 31.9, this.salt ^ 0xBF58_476D_1CE4_E5B9L);
            // Scale by 1.45 so extremes (<-0.45 Kirkwood Gap and >0.45 Metallic Cluster) are well represented
            double combined = (n1 * 0.62 + n2 * 0.28 + n3 * 0.10) * 1.45;
            return Mth.clamp(combined, -1.0, 1.0);
        }

        @Override
        public double minValue() {
            return -1.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return DensityFunctions.zero().codec();
        }
    }

    public SpectralType classifySpectralType(int blockX, int blockY, int blockZ) {
        Holder<Biome> biome = this.biomeSource.getNoiseBiome(
                QuartPos.fromBlock(blockX),
                QuartPos.fromBlock(blockY),
                QuartPos.fromBlock(blockZ),
                this.climateSampler
        );
        if (biome.is(GCBiomes.Asteroid.INNER_SILICATE_BELT)) {
            return SpectralType.SILICATE;
        }
        if (biome.is(GCBiomes.Asteroid.METALLIC_CORE_CLUSTER)) {
            return SpectralType.METALLIC;
        }
        if (biome.is(GCBiomes.Asteroid.KIRKWOOD_RESONANCE_GAP)) {
            return SpectralType.KIRKWOOD_GAP;
        }
        return SpectralType.CARBONACEOUS;
    }

    @Override
    protected @NotNull MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public @NotNull CompletableFuture<ChunkAccess> createBiomes(
            @NotNull RandomState randomState,
            @NotNull Blender blender,
            @NotNull StructureManager structureManager,
            @NotNull ChunkAccess chunk
    ) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
            chunk.fillBiomesFromNoise(this.biomeSource, this.climateSampler);
            return chunk;
        }), Util.backgroundExecutor());
    }

    @Override
    public @NotNull CompletableFuture<ChunkAccess> fillFromNoise(
            @NotNull Blender blender,
            @NotNull RandomState noiseConfig,
            @NotNull StructureManager structureAccessor,
            @NotNull ChunkAccess chunk
    ) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_fill_noise", () -> {
            generateChunkData(chunk);
            return chunk;
        }), Util.backgroundExecutor());
    }

    private void generateChunkData(ChunkAccess chunk) {
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        int chunkMaxX = chunkMinX + 15;
        int chunkMaxZ = chunkMinZ + 15;

        int minCellX = Math.floorDiv(chunkMinX - MAX_SEARCH_REACH, CELL_SIZE);
        int maxCellX = Math.floorDiv(chunkMaxX + MAX_SEARCH_REACH, CELL_SIZE);
        int minCellZ = Math.floorDiv(chunkMinZ - MAX_SEARCH_REACH, CELL_SIZE);
        int maxCellZ = Math.floorDiv(chunkMaxZ + MAX_SEARCH_REACH, CELL_SIZE);
        int minCellY = Math.floorDiv(MIN_ASTEROID_Y, CELL_SIZE);
        int maxCellY = Math.floorDiv(MAX_ASTEROID_Y, CELL_SIZE);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int cx = minCellX; cx <= maxCellX; cx++) {
            for (int cz = minCellZ; cz <= maxCellZ; cz++) {
                for (int cy = minCellY; cy <= maxCellY; cy++) {
                    AsteroidBody body = getCellAsteroid(cx, cy, cz);
                    if (body == null) {
                        // In Kirkwood Resonance Gaps, rare free-floating abandoned capsules can appear in empty cells
                        maybeGenerateKirkwoodStation(chunk, cx, cy, cz, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
                        continue;
                    }

                    // Fast AABB culling against this chunk
                    if (body.centerX + body.halfExtentX < chunkMinX || body.centerX - body.halfExtentX > chunkMaxX
                            || body.centerZ + body.halfExtentZ < chunkMinZ || body.centerZ - body.halfExtentZ > chunkMaxZ) {
                        continue;
                    }

                    carveAsteroidIntoChunk(chunk, body, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, mutablePos);

                    if (body.hasAbandonedBase) {
                        MapGenAbandonedBase.generateInChunk(
                                chunk,
                                new Random(body.bodySeed ^ 0x517C_C1B7_2722_0A95L),
                                body.centerX,
                                body.centerY,
                                body.centerZ
                        );
                    }
                }
            }
        }

        // Generate micro-debris swarms (denser 1-3 block clusters in Kirkwood Resonance Gaps)
        generateMicroDebris(chunk, chunkMinX, chunkMinZ, mutablePos);
    }

    private void maybeGenerateKirkwoodStation(
            ChunkAccess chunk,
            int cx,
            int cy,
            int cz,
            int chunkMinX,
            int chunkMaxX,
            int chunkMinZ,
            int chunkMaxZ
    ) {
        long h = mix64(this.seed ^ (cx * 0x9E37_79B9_7F4A_7C15L) ^ (cy * 0xBF58_476D_1CE4_E5B9L) ^ (cz * 0x94D0_49BB_1331_11EBL));
        // Fast pre-check before biome lookup (~6% of empty cells)
        if ((h & 0xFFL) >= 15) {
            return;
        }
        int baseX = cx * CELL_SIZE + CELL_MARGIN + (int) (Math.floorMod(h >>> 8, CELL_SIZE - 2 * CELL_MARGIN));
        int baseY = Mth.clamp(cy * CELL_SIZE + CELL_MARGIN + (int) (Math.floorMod(h >>> 16, CELL_SIZE - 2 * CELL_MARGIN)), MIN_ASTEROID_Y + 8, MAX_ASTEROID_Y - 12);
        int baseZ = cz * CELL_SIZE + CELL_MARGIN + (int) (Math.floorMod(h >>> 24, CELL_SIZE - 2 * CELL_MARGIN));

        if (baseX + 4 < chunkMinX || baseX - 4 > chunkMaxX || baseZ + 4 < chunkMinZ || baseZ - 4 > chunkMaxZ) {
            return;
        }
        if (classifySpectralType(baseX, baseY, baseZ) == SpectralType.KIRKWOOD_GAP) {
            MapGenAbandonedBase.generateInChunk(chunk, new Random(h ^ 0xD1B5_4A32_D192_ED03L), baseX, baseY, baseZ);
        }
    }

    private @Nullable AsteroidBody getCellAsteroid(int cx, int cy, int cz) {
        long cellHash = mix64(this.seed
                ^ (cx * 0x9E37_79B9_7F4A_7C15L)
                ^ (cy * 0xBF58_476D_1CE4_E5B9L)
                ^ (cz * 0x94D0_49BB_1331_11EBL));

        Random rng = new Random(cellHash);

        int centerX = cx * CELL_SIZE + CELL_MARGIN + rng.nextInt(CELL_SIZE - 2 * CELL_MARGIN);
        int centerY = cy * CELL_SIZE + CELL_MARGIN + rng.nextInt(CELL_SIZE - 2 * CELL_MARGIN);
        int centerZ = cz * CELL_SIZE + CELL_MARGIN + rng.nextInt(CELL_SIZE - 2 * CELL_MARGIN);
        if (centerY < MIN_ASTEROID_Y || centerY > MAX_ASTEROID_Y) {
            return null;
        }

        SpectralType spectralType = classifySpectralType(centerX, centerY, centerZ);
        double spawnChance = switch (spectralType) {
            case CARBONACEOUS -> 0.34;
            case SILICATE -> 0.38;
            case METALLIC -> 0.28;
            case KIRKWOOD_GAP -> 0.03;
        };
        if (rng.nextDouble() >= spawnChance) {
            return null;
        }

        // Radius distribution: weighted toward medium (8-15), ~25% large (>= 14) up to 21
        double rRoll = rng.nextDouble();
        int radius;
        if (rRoll < 0.72) {
            radius = MIN_ASTEROID_RADIUS + rng.nextInt(9); // 6..14
        } else {
            radius = MIN_CRATER_RADIUS_THRESHOLD + rng.nextInt(MAX_ASTEROID_RADIUS - MIN_CRATER_RADIUS_THRESHOLD + 1); // 14..21
        }

        // 3D Morphology distribution: 65% rubble-pile, 20% contact binary, 15% elongated/cometary
        double morphRoll = rng.nextDouble();
        Morphology morphology;
        if (morphRoll < 0.65) {
            morphology = Morphology.RUBBLE_PILE;
        } else if (morphRoll < 0.85) {
            morphology = Morphology.CONTACT_BINARY;
        } else {
            morphology = Morphology.COMETARY_FRAGMENT;
        }

        // Random orthonormal basis for 3D orientation
        double theta = rng.nextDouble() * Math.PI * 2.0;
        double cosPhi = rng.nextDouble() * 2.0 - 1.0;
        double sinPhi = Math.sqrt(Math.max(0.0, 1.0 - cosPhi * cosPhi));
        double axX = sinPhi * Math.cos(theta);
        double axY = cosPhi;
        double axZ = sinPhi * Math.sin(theta);

        // Pick a non-parallel reference vector to construct ay and az
        double refX = Math.abs(axY) < 0.9 ? 0.0 : 1.0;
        double refY = Math.abs(axY) < 0.9 ? 1.0 : 0.0;
        double refZ = 0.0;
        double ayX = axY * refZ - axZ * refY;
        double ayY = axZ * refX - axX * refZ;
        double ayZ = axX * refY - axY * refX;
        double ayLen = Math.sqrt(ayX * ayX + ayY * ayY + ayZ * ayZ);
        ayX /= ayLen;
        ayY /= ayLen;
        ayZ /= ayLen;

        double azX = axY * ayZ - axZ * ayY;
        double azY = axZ * ayX - axX * ayZ;
        double azZ = axX * ayY - axY * ayX;

        double rx;
        double ry;
        double rz;
        double lobe1Offset = 0.0;
        double lobe1InvRSq = 0.0;
        double lobe2Offset = 0.0;
        double lobe2InvRSq = 0.0;
        int maxReach;

        if (morphology == Morphology.RUBBLE_PILE) {
            rx = radius * (0.85 + rng.nextDouble() * 0.40);
            ry = radius * (0.72 + rng.nextDouble() * 0.36);
            rz = radius * (0.85 + rng.nextDouble() * 0.40);
            maxReach = (int) Math.ceil(Math.max(rx, Math.max(ry, rz)) + 4.5);
        } else if (morphology == Morphology.CONTACT_BINARY) {
            double r1 = radius * (0.72 + rng.nextDouble() * 0.22);
            double r2 = radius * (0.52 + rng.nextDouble() * 0.24);
            double separation = (r1 + r2) * 0.72;
            lobe1Offset = -separation * 0.42;
            lobe2Offset = separation * 0.58;
            lobe1InvRSq = 1.0 / (r1 * r1);
            lobe2InvRSq = 1.0 / (r2 * r2);
            rx = separation * 0.5 + Math.max(r1, r2);
            ry = Math.max(r1, r2);
            rz = Math.max(r1, r2);
            maxReach = (int) Math.ceil(rx + 4.5);
        } else {
            // Elongated / cometary fragment
            rx = radius * (1.30 + rng.nextDouble() * 0.35);
            ry = radius * (0.52 + rng.nextDouble() * 0.20);
            rz = radius * (0.48 + rng.nextDouble() * 0.22);
            maxReach = (int) Math.ceil(rx + 4.5);
        }

        // Generate 1-3 parabolic micro-craters on large asteroids (radius >= 14)
        Crater[] craters;
        if (radius >= MIN_CRATER_RADIUS_THRESHOLD) {
            int craterCount = 1 + rng.nextInt(3);
            craters = new Crater[craterCount];
            for (int i = 0; i < craterCount; i++) {
                double cTheta = rng.nextDouble() * Math.PI * 2.0;
                double cCosPhi = rng.nextDouble() * 2.0 - 1.0;
                double cSinPhi = Math.sqrt(Math.max(0.0, 1.0 - cCosPhi * cCosPhi));
                double nx = cSinPhi * Math.cos(cTheta);
                double ny = cCosPhi;
                double nz = cSinPhi * Math.sin(cTheta);

                double surfaceDist = radius * 0.88;
                double cRadius = radius * (0.28 + rng.nextDouble() * 0.20);
                double cDepth = cRadius * (0.48 + rng.nextDouble() * 0.18);
                double cRadiusSq = cRadius * cRadius;
                craters[i] = new Crater(
                        centerX + nx * surfaceDist,
                        centerY + ny * surfaceDist,
                        centerZ + nz * surfaceDist,
                        nx,
                        ny,
                        nz,
                        cRadiusSq,
                        1.0 / cRadiusSq,
                        cDepth
                );
            }
        } else {
            craters = new Crater[0];
        }

        boolean hasBase = (radius >= 15 && rng.nextInt(9) == 0)
                || (spectralType == SpectralType.KIRKWOOD_GAP && radius >= 10 && rng.nextInt(3) == 0);

        return new AsteroidBody(
                centerX,
                centerY,
                centerZ,
                radius,
                spectralType,
                morphology,
                1.0 / (rx * rx),
                1.0 / (ry * ry),
                1.0 / (rz * rz),
                axX, axY, axZ,
                ayX, ayY, ayZ,
                azX, azY, azZ,
                lobe1Offset,
                lobe1InvRSq,
                lobe2Offset,
                lobe2InvRSq,
                maxReach,
                maxReach,
                maxReach,
                craters,
                cellHash,
                hasBase
        );
    }

    private void carveAsteroidIntoChunk(
            ChunkAccess chunk,
            AsteroidBody body,
            int chunkMinX,
            int chunkMaxX,
            int chunkMinZ,
            int chunkMaxZ,
            BlockPos.MutableBlockPos mutablePos
    ) {
        int xStart = Math.max(chunkMinX, body.centerX - body.halfExtentX);
        int xEnd = Math.min(chunkMaxX, body.centerX + body.halfExtentX);
        int zStart = Math.max(chunkMinZ, body.centerZ - body.halfExtentZ);
        int zEnd = Math.min(chunkMaxZ, body.centerZ + body.halfExtentZ);
        int yStart = Math.max(MIN_Y + 1, body.centerY - body.halfExtentY);
        int yEnd = Math.min(MIN_Y + GEN_DEPTH - 2, body.centerY + body.halfExtentY);

        long warpSaltX = body.bodySeed ^ 0x243F_6A88_85A3_08D3L;
        long warpSaltY = body.bodySeed ^ 0x1319_8A2E_0370_7344L;
        long warpSaltZ = body.bodySeed ^ 0xA409_3822_299F_31D0L;
        long detailSalt = body.bodySeed ^ 0x082E_FA98_EC4E_6C89L;
        long veinSalt = body.bodySeed ^ 0x4528_21E6_38D0_1377L;

        double warpFreq = 0.085;
        double warpAmp = Math.min(3.2, body.radius * 0.22);

        for (int wx = xStart; wx <= xEnd; wx++) {
            double dx0 = wx - body.centerX;
            for (int wz = zStart; wz <= zEnd; wz++) {
                double dz0 = wz - body.centerZ;
                for (int wy = yStart; wy <= yEnd; wy++) {
                    double dy0 = wy - body.centerY;

                    // 3D Domain Warping for jagged rubble-pile / irregular asteroid surfaces
                    double wpx = wx * warpFreq;
                    double wpy = wy * warpFreq;
                    double wpz = wz * warpFreq;
                    double dx = dx0 + valueNoise3D(wpx, wpy, wpz, warpSaltX) * warpAmp;
                    double dy = dy0 + valueNoise3D(wpx + 13.7, wpy - 9.3, wpz + 5.1, warpSaltY) * warpAmp;
                    double dz = dz0 + valueNoise3D(wpx - 7.9, wpy + 19.1, wpz - 11.3, warpSaltZ) * warpAmp;

                    // Transform to local body basis
                    double lx = dx * body.axX + dy * body.axY + dz * body.axZ;
                    double ly = dx * body.ayX + dy * body.ayY + dz * body.ayZ;
                    double lz = dx * body.azX + dy * body.azY + dz * body.azZ;

                    double field;
                    if (body.morphology == Morphology.CONTACT_BINARY) {
                        double lx1 = lx - body.lobe1Offset;
                        double d1 = (lx1 * lx1 + ly * ly + lz * lz) * body.lobe1InvRSq;
                        double lx2 = lx - body.lobe2Offset;
                        double d2 = (lx2 * lx2 + ly * ly + lz * lz) * body.lobe2InvRSq;
                        // Smooth minimum merges the two lobes into a realistic contact-binary neck
                        field = smoothMin(d1, d2, 0.28);
                    } else {
                        field = lx * lx * body.invRxSq + ly * ly * body.invRySq + lz * lz * body.invRzSq;
                    }

                    // High-frequency surface roughness
                    double roughness = valueNoise3D(wx * 0.21, wy * 0.21, wz * 0.21, detailSalt) * 0.16;
                    double density = field + roughness;
                    if (density > 1.0) {
                        continue;
                    }

                    // Carve parabolic micro-craters on large asteroids (R >= 14)
                    if (body.craters.length > 0 && isInsideAnyCrater(wx, wy, wz, body.craters)) {
                        continue;
                    }

                    BlockState state = selectAsteroidBlock(body, density, wx, wy, wz, veinSalt);
                    mutablePos.set(wx, wy, wz);
                    chunk.setBlockState(mutablePos, state, false);
                }
            }
        }
    }

    private static boolean isInsideAnyCrater(int wx, int wy, int wz, Crater[] craters) {
        for (Crater crater : craters) {
            double rx = wx - crater.centerX;
            double ry = wy - crater.centerY;
            double rz = wz - crater.centerZ;

            // Projection along outward crater normal
            double alongNormal = rx * crater.normalX + ry * crater.normalY + rz * crater.normalZ;
            if (alongNormal < -crater.depth) {
                continue;
            }

            // Squared tangential distance from crater axis
            double tx = rx - alongNormal * crater.normalX;
            double ty = ry - alongNormal * crater.normalY;
            double tz = rz - alongNormal * crater.normalZ;
            double tangDistSq = tx * tx + ty * ty + tz * tz;
            if (tangDistSq >= crater.radiusSq) {
                continue;
            }

            // Parabolic excavation bowl: D(r) = depth * (1 - r^2 / R^2)
            double bowlDepth = crater.depth * (1.0 - tangDistSq * crater.invRadiusSq);
            if (alongNormal > -bowlDepth) {
                return true;
            }
        }
        return false;
    }

    private static BlockState selectAsteroidBlock(
            AsteroidBody body,
            double radialDensity,
            int wx,
            int wy,
            int wz,
            long veinSalt
    ) {
        // Cometary / elongated fragments feature volatile DENSE_ICE pockets
        if (body.morphology == Morphology.COMETARY_FRAGMENT && radialDensity > 0.35) {
            double iceNoise = valueNoise3D(wx * 0.19, wy * 0.19, wz * 0.19, veinSalt ^ 0x1A2B_3C4D_5E6F_7081L);
            if (iceNoise > 0.42) {
                return GCBlocks.DENSE_ICE.defaultBlockState();
            }
        }

        double veinNoise = valueNoise3D(wx * 0.24, wy * 0.24, wz * 0.24, veinSalt);
        long voxelHash = mix64(veinSalt ^ (wx * 341873128712L) ^ (wy * 132897987541L) ^ (wz * 42317861L));
        int roll = (int) Math.floorMod(voxelHash, 1000);

        return switch (body.spectralType) {
            case CARBONACEOUS -> selectCarbonaceousBlock(radialDensity, veinNoise, roll);
            case SILICATE -> selectSilicateBlock(radialDensity, veinNoise, roll);
            case METALLIC -> selectMetallicBlock(radialDensity, veinNoise, roll);
            case KIRKWOOD_GAP -> selectCarbonaceousBlock(radialDensity, veinNoise, roll);
        };
    }

    private static BlockState selectCarbonaceousBlock(double radialDensity, double veinNoise, int roll) {
        // Type C: dark ASTEROID_ROCK_2, ASTEROID_GRAPHITE_ORE, DENSE_ICE pockets, ASTEROID_SILICON_ORE
        if (radialDensity < 0.35 && veinNoise > 0.48) {
            return GCBlocks.DENSE_ICE.defaultBlockState();
        }
        if (veinNoise > 0.56) {
            if (roll < 420) {
                return GCBlocks.ASTEROID_GRAPHITE_ORE.defaultBlockState();
            }
            if (roll < 740) {
                return GCBlocks.DENSE_ICE.defaultBlockState();
            }
            if (roll < 950) {
                return GCBlocks.ASTEROID_SILICON_ORE.defaultBlockState();
            }
            return GCBlocks.ASTEROID_REDSTONE_ORE.defaultBlockState();
        }
        if (radialDensity > 0.72 || roll < 700) {
            return GCBlocks.ASTEROID_ROCK_2.defaultBlockState();
        }
        return GCBlocks.ASTEROID_ROCK.defaultBlockState();
    }

    private static BlockState selectSilicateBlock(double radialDensity, double veinNoise, int roll) {
        // Type S: ASTEROID_ROCK_1, ASTEROID_IRON_ORE, ASTEROID_ALUMINUM_ORE, ASTEROID_SILICON_ORE, ASTEROID_COPPER_ORE
        if (veinNoise > 0.52) {
            if (roll < 340) {
                return GCBlocks.ASTEROID_IRON_ORE.defaultBlockState();
            }
            if (roll < 620) {
                return GCBlocks.ASTEROID_ALUMINUM_ORE.defaultBlockState();
            }
            if (roll < 820) {
                return GCBlocks.ASTEROID_SILICON_ORE.defaultBlockState();
            }
            return GCBlocks.ASTEROID_COPPER_ORE.defaultBlockState();
        }
        if (radialDensity > 0.75 || roll < 720) {
            return GCBlocks.ASTEROID_ROCK_1.defaultBlockState();
        }
        return GCBlocks.ASTEROID_ROCK.defaultBlockState();
    }

    private static BlockState selectMetallicBlock(double radialDensity, double veinNoise, int roll) {
        // Type M: METEORIC_IRON_BLOCK, ILMENITE_ORE, ASTEROID_NIOBIUM_ORE, ASTEROID_GOLD_ORE, rare diamond/palladium
        if (radialDensity < 0.28) {
            if (roll < 380) {
                return GCBlocks.METEORIC_IRON_BLOCK.defaultBlockState();
            }
            if (roll < 620) {
                return GCBlocks.ILMENITE_ORE.defaultBlockState();
            }
            if (roll < 800) {
                return GCBlocks.ASTEROID_NIOBIUM_ORE.defaultBlockState();
            }
            if (roll < 940) {
                return GCBlocks.ASTEROID_GOLD_ORE.defaultBlockState();
            }
            if (roll < 975) {
                return Blocks.DIAMOND_ORE.defaultBlockState();
            }
            return GCBlocks.JUPITER_PALLADIUM_ORE.defaultBlockState();
        }
        if (veinNoise > 0.46) {
            if (roll < 320) {
                return GCBlocks.ILMENITE_ORE.defaultBlockState();
            }
            if (roll < 560) {
                return GCBlocks.METEORIC_IRON_BLOCK.defaultBlockState();
            }
            if (roll < 780) {
                return GCBlocks.ASTEROID_NIOBIUM_ORE.defaultBlockState();
            }
            if (roll < 960) {
                return GCBlocks.ASTEROID_GOLD_ORE.defaultBlockState();
            }
            return roll < 985 ? Blocks.DIAMOND_ORE.defaultBlockState() : GCBlocks.JUPITER_PALLADIUM_ORE.defaultBlockState();
        }
        return roll < 550 ? GCBlocks.ASTEROID_ROCK.defaultBlockState() : GCBlocks.ASTEROID_ROCK_2.defaultBlockState();
    }

    private void generateMicroDebris(ChunkAccess chunk, int chunkMinX, int chunkMinZ, BlockPos.MutableBlockPos mutablePos) {
        long chunkHash = mix64(this.seed ^ (chunkMinX * 0x9E37_79B9_7F4A_7C15L) ^ (chunkMinZ * 0xBF58_476D_1CE4_E5B9L));
        Random rng = new Random(chunkHash);

        SpectralType chunkType = classifySpectralType(chunkMinX + 8, 128, chunkMinZ + 8);
        int swarmCount;
        if (chunkType == SpectralType.KIRKWOOD_GAP) {
            // Kirkwood Gap: micro-debris swarms (1-3 blocks each)
            swarmCount = 5 + rng.nextInt(7);
        } else {
            // Sparse background micrometeoroids
            swarmCount = rng.nextBoolean() ? 1 + rng.nextInt(3) : 0;
        }

        int yRange = MAX_ASTEROID_Y - MIN_ASTEROID_Y;
        for (int i = 0; i < swarmCount; i++) {
            int baseX = chunkMinX + 1 + rng.nextInt(14);
            int baseY = MIN_ASTEROID_Y + rng.nextInt(yRange);
            int baseZ = chunkMinZ + 1 + rng.nextInt(14);
            int clusterSize = chunkType == SpectralType.KIRKWOOD_GAP ? (1 + rng.nextInt(3)) : (1 + rng.nextInt(2));

            BlockState debrisState = pickDebrisBlock(chunkType, rng);
            for (int b = 0; b < clusterSize; b++) {
                int bx = Mth.clamp(baseX + (b == 0 ? 0 : rng.nextInt(3) - 1), chunkMinX, chunkMinX + 15);
                int by = Mth.clamp(baseY + (b == 0 ? 0 : rng.nextInt(3) - 1), MIN_Y + 1, MIN_Y + GEN_DEPTH - 2);
                int bz = Mth.clamp(baseZ + (b == 0 ? 0 : rng.nextInt(3) - 1), chunkMinZ, chunkMinZ + 15);
                mutablePos.set(bx, by, bz);
                if (chunk.getBlockState(mutablePos).isAir()) {
                    chunk.setBlockState(mutablePos, debrisState, false);
                }
            }
        }
    }

    private static BlockState pickDebrisBlock(SpectralType type, Random rng) {
        int r = rng.nextInt(100);
        return switch (type) {
            case CARBONACEOUS -> r < 70 ? GCBlocks.ASTEROID_ROCK_2.defaultBlockState()
                    : (r < 90 ? GCBlocks.DENSE_ICE.defaultBlockState() : GCBlocks.ASTEROID_GRAPHITE_ORE.defaultBlockState());
            case SILICATE -> r < 72 ? GCBlocks.ASTEROID_ROCK_1.defaultBlockState()
                    : (r < 88 ? GCBlocks.ASTEROID_IRON_ORE.defaultBlockState() : GCBlocks.ASTEROID_ALUMINUM_ORE.defaultBlockState());
            case METALLIC -> r < 55 ? GCBlocks.ASTEROID_ROCK.defaultBlockState()
                    : (r < 85 ? GCBlocks.METEORIC_IRON_BLOCK.defaultBlockState() : GCBlocks.ILMENITE_ORE.defaultBlockState());
            case KIRKWOOD_GAP -> r < 40 ? GCBlocks.ASTEROID_ROCK.defaultBlockState()
                    : (r < 75 ? GCBlocks.ASTEROID_ROCK_1.defaultBlockState()
                    : (r < 92 ? GCBlocks.ASTEROID_ROCK_2.defaultBlockState() : GCBlocks.ILMENITE_ORE.defaultBlockState()));
        };
    }

    @Override
    public void applyCarvers(
            @NotNull WorldGenRegion region,
            long seed,
            @NotNull RandomState randomState,
            @NotNull BiomeManager biomeManager,
            @NotNull StructureManager structureManager,
            @NotNull ChunkAccess chunkAccess,
            GenerationStep.@NotNull Carving carving
    ) {
        // Carving (micro-craters and 3D domain-warped sculpting) is handled deterministically during fillFromNoise.
    }

    @Override
    public void buildSurface(
            @NotNull WorldGenRegion region,
            @NotNull StructureManager structureManager,
            @NotNull RandomState randomState,
            @NotNull ChunkAccess chunkAccess
    ) {
    }

    @Override
    public void spawnOriginalMobs(@NotNull WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.@NotNull Types types, @NotNull LevelHeightAccessor levelHeightAccessor, @NotNull RandomState randomState) {
        int minCellX = Math.floorDiv(x - MAX_SEARCH_REACH, CELL_SIZE);
        int maxCellX = Math.floorDiv(x + MAX_SEARCH_REACH, CELL_SIZE);
        int minCellZ = Math.floorDiv(z - MAX_SEARCH_REACH, CELL_SIZE);
        int maxCellZ = Math.floorDiv(z + MAX_SEARCH_REACH, CELL_SIZE);
        int minCellY = Math.floorDiv(MIN_ASTEROID_Y, CELL_SIZE);
        int maxCellY = Math.floorDiv(MAX_ASTEROID_Y, CELL_SIZE);

        int highest = MIN_Y;
        for (int cx = minCellX; cx <= maxCellX; cx++) {
            for (int cz = minCellZ; cz <= maxCellZ; cz++) {
                for (int cy = maxCellY; cy >= minCellY; cy--) {
                    AsteroidBody body = getCellAsteroid(cx, cy, cz);
                    if (body != null
                            && Math.abs(x - body.centerX) <= body.halfExtentX
                            && Math.abs(z - body.centerZ) <= body.halfExtentZ) {
                        highest = Math.max(highest, Math.min(MIN_Y + GEN_DEPTH - 2, body.centerY + body.radius));
                    }
                }
            }
        }
        return highest;
    }

    @Override
    public @NotNull NoiseColumn getBaseColumn(int x, int z, @NotNull LevelHeightAccessor levelHeightAccessor, @NotNull RandomState randomState) {
        return new NoiseColumn(MIN_Y, EMPTY_COLUMN);
    }

    @Override
    public void addDebugScreenInfo(@NotNull List<String> list, @NotNull RandomState randomState, @NotNull BlockPos blockPos) {
        SpectralType type = classifySpectralType(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        int cellX = Math.floorDiv(blockPos.getX(), CELL_SIZE);
        int cellY = Math.floorDiv(blockPos.getY(), CELL_SIZE);
        int cellZ = Math.floorDiv(blockPos.getZ(), CELL_SIZE);
        list.add("Asteroid Belt: " + type.name() + " | Cell [" + cellX + ", " + cellY + ", " + cellZ + "]");
    }

    /**
     * Deterministically locates the nearest large asteroid center near {@code (x0, z0)}.
     */
    public @Nullable BlockVec3 isLargeAsteroidAt(int x0, int z0) {
        int baseCellX = Math.floorDiv(x0, CELL_SIZE);
        int baseCellZ = Math.floorDiv(z0, CELL_SIZE);
        int minCellY = Math.floorDiv(MIN_ASTEROID_Y, CELL_SIZE);
        int maxCellY = Math.floorDiv(MAX_ASTEROID_Y, CELL_SIZE);

        for (int ring = 0; ring <= 8; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    for (int cy = minCellY; cy <= maxCellY; cy++) {
                        AsteroidBody body = getCellAsteroid(baseCellX + dx, cy, baseCellZ + dz);
                        if (body != null && body.radius >= MIN_CRATER_RADIUS_THRESHOLD) {
                            return new BlockVec3(body.centerX, body.centerY, body.centerZ);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static double smoothMin(double a, double b, double k) {
        double h = Math.max(k - Math.abs(a - b), 0.0) / k;
        return Math.min(a, b) - h * h * k * 0.25;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D0_49BB_1331_11EBL;
        return z ^ (z >>> 31);
    }

    private static double hashToSignedUnit(int x, int y, int z, long salt) {
        long h = mix64(salt ^ (x * 0x9E37_79B9_7F4A_7C15L) ^ (y * 0xBF58_476D_1CE4_E5B9L) ^ (z * 0x94D0_49BB_1331_11EBL));
        return ((h >>> 11) * (1.0 / (1L << 53))) * 2.0 - 1.0;
    }

    private static double hashToSignedUnit2D(int x, int z, long salt) {
        long h = mix64(salt ^ (x * 0x9E37_79B9_7F4A_7C15L) ^ (z * 0x94D0_49BB_1331_11EBL));
        return ((h >>> 11) * (1.0 / (1L << 53))) * 2.0 - 1.0;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double valueNoise2D(double x, double z, long salt) {
        int x0 = Mth.floor(x);
        int z0 = Mth.floor(z);
        double fx = fade(x - x0);
        double fz = fade(z - z0);

        double v00 = hashToSignedUnit2D(x0, z0, salt);
        double v10 = hashToSignedUnit2D(x0 + 1, z0, salt);
        double v01 = hashToSignedUnit2D(x0, z0 + 1, salt);
        double v11 = hashToSignedUnit2D(x0 + 1, z0 + 1, salt);

        double nx0 = Mth.lerp(fx, v00, v10);
        double nx1 = Mth.lerp(fx, v01, v11);
        return Mth.lerp(fz, nx0, nx1);
    }

    private static double valueNoise3D(double x, double y, double z, long salt) {
        int x0 = Mth.floor(x);
        int y0 = Mth.floor(y);
        int z0 = Mth.floor(z);
        double fx = fade(x - x0);
        double fy = fade(y - y0);
        double fz = fade(z - z0);

        double v000 = hashToSignedUnit(x0, y0, z0, salt);
        double v100 = hashToSignedUnit(x0 + 1, y0, z0, salt);
        double v010 = hashToSignedUnit(x0, y0 + 1, z0, salt);
        double v110 = hashToSignedUnit(x0 + 1, y0 + 1, z0, salt);
        double v001 = hashToSignedUnit(x0, y0, z0 + 1, salt);
        double v101 = hashToSignedUnit(x0 + 1, y0, z0 + 1, salt);
        double v011 = hashToSignedUnit(x0, y0 + 1, z0 + 1, salt);
        double v111 = hashToSignedUnit(x0 + 1, y0 + 1, z0 + 1, salt);

        double nx00 = Mth.lerp(fx, v000, v100);
        double nx10 = Mth.lerp(fx, v010, v110);
        double nx01 = Mth.lerp(fx, v001, v101);
        double nx11 = Mth.lerp(fx, v011, v111);

        double nxy0 = Mth.lerp(fy, nx00, nx10);
        double nxy1 = Mth.lerp(fy, nx01, nx11);
        return Mth.lerp(fz, nxy0, nxy1);
    }
}
