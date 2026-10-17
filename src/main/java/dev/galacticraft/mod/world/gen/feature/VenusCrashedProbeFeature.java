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
import dev.galacticraft.mod.content.block.entity.CrashedProbeBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Direct port of GC4 Legacy {@code WorldGenCrashedProbe}.
 * Carves a small impact crater on Venus, scatters {@link GCBlocks#SCORCHED_VENUS_ROCK} debris,
 * and places a {@link GCBlocks#CRASHED_PROBE} containing salvaged space probe components.
 */
public class VenusCrashedProbeFeature extends Feature<NoneFeatureConfiguration> {
    public VenusCrashedProbeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();

        int surfaceY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ()) - 1;
        if (surfaceY <= 50) {
            return false;
        }

        BlockPos position = new BlockPos(origin.getX(), surfaceY, origin.getZ());
        int radius = 5 + rand.nextInt(4);
        int radiusSq = radius * radius;

        // Check crater area does not intersect liquid or steep dropoffs
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, position.getX() + dx, position.getZ() + dz);
                if (Math.abs(h - surfaceY) > 6 || level.getBlockState(new BlockPos(position.getX() + dx, h - 1, position.getZ() + dz)).liquid()) {
                    return false;
                }
            }
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState scorchedRock = GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState();

        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dy = -radius; dy <= radius + 4; ++dy) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    double distSq = dx * dx + (dy - 3) * (dy - 3) + dz * dz;
                    if (distSq <= radiusSq) {
                        BlockPos pos = position.offset(dx, dy, dz);
                        level.setBlock(pos, air, 2);
                    }
                }
            }
        }

        // Scatter scorched rock around crater floor (1 in 5 chance, matching GC4 WorldGenCrashedProbe)
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                if (dx * dx + dz * dz <= radiusSq && rand.nextInt(5) == 0) {
                    int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, position.getX() + dx, position.getZ() + dz) - 1;
                    BlockPos floorPos = new BlockPos(position.getX() + dx, floorY, position.getZ() + dz);
                    if (level.getBlockState(floorPos).isSolid()) {
                        level.setBlock(floorPos, scorchedRock, 2);
                    }
                }
            }
        }

        int centerFloorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, position.getX(), position.getZ());
        BlockPos probePos = new BlockPos(position.getX(), centerFloorY, position.getZ());
        level.setBlock(probePos, GCBlocks.CRASHED_PROBE.defaultBlockState(), 2);

        if (level.getBlockEntity(probePos) instanceof CrashedProbeBlockEntity probe) {
            for (int slot = 0; slot < probe.getContainerSize(); ++slot) {
                if (rand.nextInt(3) == 0) {
                    ItemStack loot = switch (rand.nextInt(5)) {
                        case 0 -> new ItemStack(GCItems.COMPRESSED_TITANIUM, 1 + rand.nextInt(2));
                        case 1 -> new ItemStack(GCItems.COMPRESSED_IRON, 1 + rand.nextInt(3));
                        case 2 -> new ItemStack(GCItems.ADVANCED_WAFER, 1);
                        case 3 -> new ItemStack(GCItems.BEAM_CORE, 1);
                        default -> new ItemStack(GCItems.SOLAR_DUST, 1 + rand.nextInt(3));
                    };
                    probe.setItem(slot, loot);
                }
            }
        }

        return true;
    }
}
