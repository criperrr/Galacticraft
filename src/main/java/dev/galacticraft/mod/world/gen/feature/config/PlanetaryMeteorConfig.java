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

package dev.galacticraft.mod.world.gen.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.environment.FallenMeteorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Customizable configuration for planetary meteor impact craters and fallen meteor deposits.
 * Allows configuring impact probability, crater size, meteor core composition, scorched rim blocks,
 * and rich ore ejecta scattered around the crater for any planet or biome.
 */
public record PlanetaryMeteorConfig(
        float spawnProbability,
        int minCraterRadius,
        int maxCraterRadius,
        int minMeteorRadius,
        int maxMeteorRadius,
        BlockStateProvider meteorShellBlock,
        BlockStateProvider meteorCoreBlock,
        BlockStateProvider scorchedRimBlock,
        BlockStateProvider craterFloorBlock,
        List<BlockState> ejectaOres,
        int oreChance
) implements FeatureConfiguration {

    public static final Codec<PlanetaryMeteorConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("spawn_probability").forGetter(PlanetaryMeteorConfig::spawnProbability),
            Codec.intRange(3, 32).fieldOf("min_crater_radius").forGetter(PlanetaryMeteorConfig::minCraterRadius),
            Codec.intRange(4, 48).fieldOf("max_crater_radius").forGetter(PlanetaryMeteorConfig::maxCraterRadius),
            Codec.intRange(1, 8).fieldOf("min_meteor_radius").forGetter(PlanetaryMeteorConfig::minMeteorRadius),
            Codec.intRange(1, 12).fieldOf("max_meteor_radius").forGetter(PlanetaryMeteorConfig::maxMeteorRadius),
            BlockStateProvider.CODEC.fieldOf("meteor_shell_block").forGetter(PlanetaryMeteorConfig::meteorShellBlock),
            BlockStateProvider.CODEC.fieldOf("meteor_core_block").forGetter(PlanetaryMeteorConfig::meteorCoreBlock),
            BlockStateProvider.CODEC.fieldOf("scorched_rim_block").forGetter(PlanetaryMeteorConfig::scorchedRimBlock),
            BlockStateProvider.CODEC.fieldOf("crater_floor_block").forGetter(PlanetaryMeteorConfig::craterFloorBlock),
            BlockState.CODEC.listOf().fieldOf("ejecta_ores").forGetter(PlanetaryMeteorConfig::ejectaOres),
            Codec.intRange(1, 64).fieldOf("ore_chance").forGetter(PlanetaryMeteorConfig::oreChance)
    ).apply(instance, PlanetaryMeteorConfig::new));

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private float spawnProbability = 0.35F;
        private int minCraterRadius = 6;
        private int maxCraterRadius = 13;
        private int minMeteorRadius = 1;
        private int maxMeteorRadius = 3;
        private BlockStateProvider meteorShellBlock = BlockStateProvider.simple(
                GCBlocks.FALLEN_METEOR.defaultBlockState().setValue(FallenMeteorBlock.HEAT, 5)
        );
        private BlockStateProvider meteorCoreBlock = BlockStateProvider.simple(
                GCBlocks.RAW_METEORIC_IRON_BLOCK.defaultBlockState()
        );
        private BlockStateProvider scorchedRimBlock = BlockStateProvider.simple(
                GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState()
        );
        private BlockStateProvider craterFloorBlock = BlockStateProvider.simple(
                Blocks.MAGMA_BLOCK.defaultBlockState()
        );
        private final List<BlockState> ejectaOres = new ArrayList<>();
        private int oreChance = 6;

        public Builder spawnProbability(float probability) {
            this.spawnProbability = probability;
            return this;
        }

        public Builder craterRadiusRange(int min, int max) {
            this.minCraterRadius = min;
            this.maxCraterRadius = max;
            return this;
        }

        public Builder meteorRadiusRange(int min, int max) {
            this.minMeteorRadius = min;
            this.maxMeteorRadius = max;
            return this;
        }

        public Builder meteorBlocks(BlockState shell, BlockState core) {
            this.meteorShellBlock = BlockStateProvider.simple(shell);
            this.meteorCoreBlock = BlockStateProvider.simple(core);
            return this;
        }

        public Builder craterBlocks(BlockState scorchedRim, BlockState floor) {
            this.scorchedRimBlock = BlockStateProvider.simple(scorchedRim);
            this.craterFloorBlock = BlockStateProvider.simple(floor);
            return this;
        }

        public Builder addEjectaOre(BlockState oreState) {
            this.ejectaOres.add(oreState);
            return this;
        }

        public Builder oreChance(int chance) {
            this.oreChance = chance;
            return this;
        }

        public PlanetaryMeteorConfig build() {
            return new PlanetaryMeteorConfig(
                    this.spawnProbability,
                    this.minCraterRadius,
                    this.maxCraterRadius,
                    this.minMeteorRadius,
                    this.maxMeteorRadius,
                    this.meteorShellBlock,
                    this.meteorCoreBlock,
                    this.scorchedRimBlock,
                    this.craterFloorBlock,
                    List.copyOf(this.ejectaOres),
                    this.oreChance
            );
        }
    }
}
