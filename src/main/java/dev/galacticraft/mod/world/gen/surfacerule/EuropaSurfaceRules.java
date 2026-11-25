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
import org.jetbrains.annotations.NotNull;

public class EuropaSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource DENSE_ICE = state(GCBlocks.DENSE_ICE);
    private static final RuleSource PACKED_ICE = state(Blocks.PACKED_ICE);
    private static final RuleSource BLUE_ICE = state(Blocks.BLUE_ICE);
    private static final RuleSource SNOW = state(Blocks.SNOW_BLOCK);
    private static final RuleSource LINEAE_STAIN = state(GCBlocks.MARS_SUB_SURFACE_ROCK);
    private static final RuleSource SEABED_BASALT = state(GCBlocks.MOON_BASALT);

    public static final RuleSource EUROPA = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isIcePlains = SurfaceRules.isBiome(GCBiomes.Europa.ICE_PLAINS);
        ConditionSource isLineae = SurfaceRules.isBiome(GCBiomes.Europa.LINEAE_RIDGES);
        ConditionSource isChaos = SurfaceRules.isBiome(GCBiomes.Europa.CHAOS_TERRAIN);
        ConditionSource isOcean = SurfaceRules.isBiome(GCBiomes.Europa.SUBSURFACE_OCEAN);

        ConditionSource isBelowSeafloor = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(-24), 0));
        ConditionSource isBelowIceCeiling = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(32), 0));
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.15D, 0.45D);
        ConditionSource lineaeNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.12D, 0.55D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(lineaeNoise, BLUE_ICE),
                        DENSE_ICE
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isIcePlains, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, SNOW),
                                                PACKED_ICE
                                        )),
                                        SurfaceRules.ifTrue(isLineae, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(lineaeNoise, LINEAE_STAIN),
                                                SurfaceRules.ifTrue(primaryNoise, PACKED_ICE),
                                                DENSE_ICE
                                        )),
                                        SurfaceRules.ifTrue(isChaos, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, BLUE_ICE),
                                                SurfaceRules.ifTrue(lineaeNoise, LINEAE_STAIN),
                                                DENSE_ICE
                                        )),
                                        PACKED_ICE
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, PACKED_ICE)
                )
        );

        // Subsurface ocean floor (silicate basalt seabed below Y = -24) & blue ice ceiling
        RuleSource oceanRules = SurfaceRules.ifTrue(
                isBelowIceCeiling,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(isBelowSeafloor, SEABED_BASALT),
                        SurfaceRules.ifTrue(isOcean, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, BLUE_ICE),
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SEABED_BASALT)
                        ))
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                oceanRules,
                DENSE_ICE
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("europa"), MapCodec.unit(EUROPA));
    }
}
