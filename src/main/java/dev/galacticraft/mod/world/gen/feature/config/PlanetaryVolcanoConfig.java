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
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customizable planetary volcano configuration based on {@code TFCVolcanoes}
 * ({@code CinderFeatureNoise}, {@code DensityCellular2D}, {@code TremorEvent.VolcanoType}, and {@code PyroclasticBomb}).
 * Allows registering volcanoes in specific biomes with configurable per-biome frequencies, cellular region spacing,
 * morphology types, rock palettes, and pyroclastic ore ejecta.
 */
public record PlanetaryVolcanoConfig(
        Map<ResourceLocation, Float> biomeFrequencies,
        float defaultFrequency,
        double regionSize,
        double spread,
        int minRadius,
        int maxRadius,
        int minHeight,
        int maxHeight,
        int minCraterRadius,
        int maxCraterRadius,
        VolcanoType volcanoType,
        RockPalette rockPalette,
        List<BlockState> pyroclasticOres,
        int pyroclasticOreChance,
        boolean spawnPyroclasticScoria,
        boolean spawnFumaroles
) implements FeatureConfiguration {

    public record RockPalette(
            BlockStateProvider extrusiveRock,
            BlockStateProvider intrusiveRock,
            BlockStateProvider scorchedRock,
            BlockStateProvider magmaRock,
            BlockStateProvider fluidBlock
    ) {
        public static final Codec<RockPalette> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("extrusive_rock").forGetter(RockPalette::extrusiveRock),
                BlockStateProvider.CODEC.fieldOf("intrusive_rock").forGetter(RockPalette::intrusiveRock),
                BlockStateProvider.CODEC.fieldOf("scorched_rock").forGetter(RockPalette::scorchedRock),
                BlockStateProvider.CODEC.fieldOf("magma_rock").forGetter(RockPalette::magmaRock),
                BlockStateProvider.CODEC.fieldOf("fluid_block").forGetter(RockPalette::fluidBlock)
        ).apply(instance, RockPalette::new));
    }

    public static final Codec<PlanetaryVolcanoConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.FLOAT).fieldOf("biome_frequencies").forGetter(PlanetaryVolcanoConfig::biomeFrequencies),
            Codec.FLOAT.fieldOf("default_frequency").forGetter(PlanetaryVolcanoConfig::defaultFrequency),
            Codec.DOUBLE.fieldOf("region_size").forGetter(PlanetaryVolcanoConfig::regionSize),
            Codec.DOUBLE.fieldOf("spread").forGetter(PlanetaryVolcanoConfig::spread),
            Codec.INT.fieldOf("min_radius").forGetter(PlanetaryVolcanoConfig::minRadius),
            Codec.INT.fieldOf("max_radius").forGetter(PlanetaryVolcanoConfig::maxRadius),
            Codec.INT.fieldOf("min_height").forGetter(PlanetaryVolcanoConfig::minHeight),
            Codec.INT.fieldOf("max_height").forGetter(PlanetaryVolcanoConfig::maxHeight),
            Codec.INT.fieldOf("min_crater_radius").forGetter(PlanetaryVolcanoConfig::minCraterRadius),
            Codec.INT.fieldOf("max_crater_radius").forGetter(PlanetaryVolcanoConfig::maxCraterRadius),
            VolcanoType.CODEC.fieldOf("volcano_type").forGetter(PlanetaryVolcanoConfig::volcanoType),
            RockPalette.CODEC.fieldOf("rock_palette").forGetter(PlanetaryVolcanoConfig::rockPalette),
            BlockState.CODEC.listOf().fieldOf("pyroclastic_ores").forGetter(PlanetaryVolcanoConfig::pyroclasticOres),
            Codec.INT.fieldOf("pyroclastic_ore_chance").forGetter(PlanetaryVolcanoConfig::pyroclasticOreChance),
            Codec.BOOL.fieldOf("spawn_pyroclastic_scoria").forGetter(PlanetaryVolcanoConfig::spawnPyroclasticScoria),
            Codec.BOOL.fieldOf("spawn_fumaroles").forGetter(PlanetaryVolcanoConfig::spawnFumaroles)
    ).apply(instance, PlanetaryVolcanoConfig::new));

    public BlockStateProvider extrusiveRock() {
        return this.rockPalette.extrusiveRock();
    }

    public BlockStateProvider intrusiveRock() {
        return this.rockPalette.intrusiveRock();
    }

    public BlockStateProvider scorchedRock() {
        return this.rockPalette.scorchedRock();
    }

    public BlockStateProvider magmaRock() {
        return this.rockPalette.magmaRock();
    }

    public BlockStateProvider fluidBlock() {
        return this.rockPalette.fluidBlock();
    }

    /**
     * Ported from {@code CinderFeatureNoise.getFrequency(Holder<Biome>)}:
     * returns the volcano generation frequency in [0.0, 1.0] for the given biome.
     */
    public float getFrequency(Holder<Biome> biome) {
        return biome.unwrapKey()
                .map(key -> this.biomeFrequencies.getOrDefault(key.location(), this.defaultFrequency))
                .orElse(this.defaultFrequency);
    }

    /**
     * Volcano morphology types ported from {@code tfcvolcanoes.util.TremorEvent.getVolcanoTypes}.
     */
    public enum VolcanoType implements StringRepresentable {
        CINDER_CONE("cinder"),
        STRATOVOLCANO("strato"),
        SHIELD("shield"),
        TUFF_RING("tuff"),
        MIXED("mixed");

        public static final Codec<VolcanoType> CODEC = StringRepresentable.fromEnum(VolcanoType::values);
        private final String serializedName;

        VolcanoType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<ResourceLocation, Float> biomeFrequencies = new HashMap<>();
        private float defaultFrequency = 0.0F;
        private double regionSize = 560.0;
        private double spread = 0.75;
        private int minRadius = 36;
        private int maxRadius = 62;
        private int minHeight = 24;
        private int maxHeight = 46;
        private int minCraterRadius = 8;
        private int maxCraterRadius = 15;
        private VolcanoType volcanoType = VolcanoType.MIXED;
        private BlockStateProvider extrusiveRock = BlockStateProvider.simple(Blocks.BASALT.defaultBlockState());
        private BlockStateProvider intrusiveRock = BlockStateProvider.simple(GCBlocks.HARD_VENUS_ROCK.defaultBlockState());
        private BlockStateProvider scorchedRock = BlockStateProvider.simple(GCBlocks.SCORCHED_VENUS_ROCK.defaultBlockState());
        private BlockStateProvider magmaRock = BlockStateProvider.simple(Blocks.MAGMA_BLOCK.defaultBlockState());
        private BlockStateProvider fluidBlock = BlockStateProvider.simple(Blocks.LAVA.defaultBlockState());
        private final List<BlockState> pyroclasticOres = new ArrayList<>();
        private int pyroclasticOreChance = 5;
        private boolean spawnPyroclasticScoria = true;
        private boolean spawnFumaroles = true;

        public Builder biomeFrequency(ResourceKey<Biome> biome, float frequency) {
            this.biomeFrequencies.put(biome.location(), frequency);
            return this;
        }

        public Builder defaultFrequency(float defaultFrequency) {
            this.defaultFrequency = defaultFrequency;
            return this;
        }

        public Builder regionSize(double regionSize) {
            this.regionSize = regionSize;
            return this;
        }

        public Builder spread(double spread) {
            this.spread = spread;
            return this;
        }

        public Builder cellularGrid(double regionSize, double spread) {
            this.regionSize = regionSize;
            this.spread = spread;
            return this;
        }

        public Builder radiusRange(int minRadius, int maxRadius) {
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            return this;
        }

        public Builder heightRange(int minHeight, int maxHeight) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            return this;
        }

        public Builder craterRadiusRange(int minCraterRadius, int maxCraterRadius) {
            this.minCraterRadius = minCraterRadius;
            this.maxCraterRadius = maxCraterRadius;
            return this;
        }

        public Builder volcanoType(VolcanoType volcanoType) {
            this.volcanoType = volcanoType;
            return this;
        }

        public Builder rocks(
                BlockState extrusive,
                BlockState intrusive,
                BlockState scorched,
                BlockState magma,
                BlockState fluid
        ) {
            this.extrusiveRock = BlockStateProvider.simple(extrusive);
            this.intrusiveRock = BlockStateProvider.simple(intrusive);
            this.scorchedRock = BlockStateProvider.simple(scorched);
            this.magmaRock = BlockStateProvider.simple(magma);
            this.fluidBlock = BlockStateProvider.simple(fluid);
            return this;
        }

        public Builder addPyroclasticOre(BlockState oreState) {
            this.pyroclasticOres.add(oreState);
            return this;
        }

        public Builder pyroclasticOreChance(int chance) {
            this.pyroclasticOreChance = Math.max(1, chance);
            return this;
        }

        public Builder spawnPyroclasticScoria(boolean spawn) {
            this.spawnPyroclasticScoria = spawn;
            return this;
        }

        public Builder spawnFumaroles(boolean spawn) {
            this.spawnFumaroles = spawn;
            return this;
        }

        public PlanetaryVolcanoConfig build() {
            return new PlanetaryVolcanoConfig(
                    Map.copyOf(this.biomeFrequencies),
                    this.defaultFrequency,
                    this.regionSize,
                    this.spread,
                    this.minRadius,
                    this.maxRadius,
                    this.minHeight,
                    this.maxHeight,
                    this.minCraterRadius,
                    this.maxCraterRadius,
                    this.volcanoType,
                    new RockPalette(
                            this.extrusiveRock,
                            this.intrusiveRock,
                            this.scorchedRock,
                            this.magmaRock,
                            this.fluidBlock
                    ),
                    List.copyOf(this.pyroclasticOres),
                    this.pyroclasticOreChance,
                    this.spawnPyroclasticScoria,
                    this.spawnFumaroles
            );
        }
    }
}
