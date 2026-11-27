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

package dev.galacticraft.mod.world.gen.surfacerule;

import com.mojang.serialization.MapCodec;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.world.biome.GCBiomes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules.ConditionSource;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import org.jetbrains.annotations.NotNull;

public class TitanSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource THOLIN_SAND = state(GCBlocks.TITAN_THOLIN_SAND);
    private static final RuleSource HYDROCARBON_CRUST = state(GCBlocks.TITAN_HYDROCARBON_CRUST);
    private static final RuleSource WATER_ICE_ROCK = state(GCBlocks.TITAN_WATER_ICE_ROCK);
    private static final RuleSource DENSE_ICE = state(GCBlocks.DENSE_ICE);

    public static final RuleSource TITAN = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isMethaneSeas = SurfaceRules.isBiome(GCBiomes.Titan.METHANE_SEAS);
        ConditionSource isTholinDunes = SurfaceRules.isBiome(GCBiomes.Titan.THOLIN_DUNES);
        ConditionSource isIceHighlands = SurfaceRules.isBiome(GCBiomes.Titan.ICE_HIGHLANDS);
        ConditionSource isCaverns = SurfaceRules.isBiome(GCBiomes.Titan.CRYO_CAVERNS);
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.20D, 0.45D);
        ConditionSource belowSeaLevel = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(63), 0));

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(primaryNoise, WATER_ICE_ROCK),
                        HYDROCARBON_CRUST
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(belowSeaLevel, HYDROCARBON_CRUST),
                                        SurfaceRules.ifTrue(isMethaneSeas, HYDROCARBON_CRUST),
                                        SurfaceRules.ifTrue(isTholinDunes, THOLIN_SAND),
                                        SurfaceRules.ifTrue(isIceHighlands, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, WATER_ICE_ROCK),
                                                HYDROCARBON_CRUST
                                        )),
                                        THOLIN_SAND
                                )
                        ),
                        SurfaceRules.ifTrue(
                                SurfaceRules.UNDER_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isTholinDunes, THOLIN_SAND),
                                        HYDROCARBON_CRUST
                                )
                        )
                )
        );

        RuleSource cavernRules = SurfaceRules.ifTrue(
                isCaverns,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.FLOOR), HYDROCARBON_CRUST),
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.CEILING), DENSE_ICE)
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                cavernRules,
                WATER_ICE_ROCK
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("titan"), MapCodec.unit(TITAN));
    }
}
