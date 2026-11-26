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

public class EnceladusSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource ICE_CRUST = state(GCBlocks.ENCELADUS_ICE_CRUST);
    private static final RuleSource TIGER_STRIPE = state(GCBlocks.ENCELADUS_TIGER_STRIPE_ICE);
    private static final RuleSource SEABED_ROCK = state(GCBlocks.ENCELADUS_SEABED_ROCK);
    private static final RuleSource SNOW = state(Blocks.SNOW_BLOCK);
    private static final RuleSource PACKED_ICE = state(Blocks.PACKED_ICE);
    private static final RuleSource BLUE_ICE = state(Blocks.BLUE_ICE);

    public static final RuleSource ENCELADUS = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isSnowPlains = SurfaceRules.isBiome(GCBiomes.Enceladus.SNOW_PLAINS);
        ConditionSource isTigerStripes = SurfaceRules.isBiome(GCBiomes.Enceladus.TIGER_STRIPES);
        ConditionSource isRidges = SurfaceRules.isBiome(GCBiomes.Enceladus.CRATERED_RIDGES);
        ConditionSource isOcean = SurfaceRules.isBiome(GCBiomes.Enceladus.HYDROTHERMAL_OCEAN);

        ConditionSource isBelowSeafloor = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(-26), 0));
        ConditionSource isBelowIceCeiling = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(30), 0));
        ConditionSource isBelowSeamountTop = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(-14), 0));
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.35D, 1.0D);
        ConditionSource stripeNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.14D, 0.60D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(isTigerStripes, TIGER_STRIPE),
                        SurfaceRules.ifTrue(stripeNoise, BLUE_ICE),
                        ICE_CRUST
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isSnowPlains, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, SNOW),
                                                ICE_CRUST
                                        )),
                                        SurfaceRules.ifTrue(isTigerStripes, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(stripeNoise, TIGER_STRIPE),
                                                SurfaceRules.ifTrue(primaryNoise, BLUE_ICE),
                                                SNOW
                                        )),
                                        SurfaceRules.ifTrue(isRidges, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, PACKED_ICE),
                                                SNOW
                                        )),
                                        SNOW
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, PACKED_ICE)
                )
        );

        RuleSource oceanRules = SurfaceRules.ifTrue(
                isBelowIceCeiling,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(isBelowSeafloor, SEABED_ROCK),
                        SurfaceRules.ifTrue(isOcean, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(
                                        SurfaceRules.stoneDepthCheck(2, false, 0, CaveSurface.CEILING),
                                        SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, BLUE_ICE),
                                                ICE_CRUST
                                        )
                                ),
                                SurfaceRules.ifTrue(
                                        isBelowSeamountTop,
                                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(3, false, 0, CaveSurface.FLOOR), SEABED_ROCK)
                                )
                        ))
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                oceanRules,
                ICE_CRUST
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("enceladus"), MapCodec.unit(ENCELADUS));
    }
}
