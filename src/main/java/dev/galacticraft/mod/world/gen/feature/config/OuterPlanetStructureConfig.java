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
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.jetbrains.annotations.NotNull;

public record OuterPlanetStructureConfig(
        StructureKind kind,
        int minHeight,
        int maxHeight,
        int minRadius,
        int maxRadius,
        float tiltStrength,
        BlockStateProvider primaryBlock,
        BlockStateProvider secondaryBlock,
        BlockStateProvider coreOrFluidBlock,
        BlockStateProvider accentOrCrownBlock
) implements FeatureConfiguration {

    public static final Codec<OuterPlanetStructureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StructureKind.CODEC.fieldOf("kind").forGetter(OuterPlanetStructureConfig::kind),
            Codec.intRange(2, 64).fieldOf("min_height").forGetter(OuterPlanetStructureConfig::minHeight),
            Codec.intRange(2, 80).fieldOf("max_height").forGetter(OuterPlanetStructureConfig::maxHeight),
            Codec.intRange(1, 12).fieldOf("min_radius").forGetter(OuterPlanetStructureConfig::minRadius),
            Codec.intRange(1, 14).fieldOf("max_radius").forGetter(OuterPlanetStructureConfig::maxRadius),
            Codec.floatRange(0.0F, 1.2F).fieldOf("tilt_strength").forGetter(OuterPlanetStructureConfig::tiltStrength),
            BlockStateProvider.CODEC.fieldOf("primary_block").forGetter(OuterPlanetStructureConfig::primaryBlock),
            BlockStateProvider.CODEC.fieldOf("secondary_block").forGetter(OuterPlanetStructureConfig::secondaryBlock),
            BlockStateProvider.CODEC.fieldOf("core_or_fluid_block").forGetter(OuterPlanetStructureConfig::coreOrFluidBlock),
            BlockStateProvider.CODEC.fieldOf("accent_or_crown_block").forGetter(OuterPlanetStructureConfig::accentOrCrownBlock)
    ).apply(instance, OuterPlanetStructureConfig::new));

    public enum StructureKind implements StringRepresentable {
        TILTED_SPIRE("tilted_spire"),
        LIGHTNING_FULGURITE("fulgurite"),
        HEXAGON_TERRACE("hexagon_terrace"),
        WIND_RIDGE("wind_ridge"),
        CRYO_GEYSER("cryo_geyser"),
        ABYSSAL_SPELEOTHEM("speleothem"),
        DIAMONDBERG("diamondberg");

        public static final Codec<StructureKind> CODEC = StringRepresentable.fromEnum(StructureKind::values);
        private final String name;

        StructureKind(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name;
        }
    }
}
