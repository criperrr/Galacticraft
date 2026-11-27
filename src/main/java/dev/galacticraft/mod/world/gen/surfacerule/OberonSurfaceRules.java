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

public class OberonSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource DARK_ROCK = state(GCBlocks.OBERON_DARK_ROCK);
    private static final RuleSource REDDISH_REGOLITH = state(GCBlocks.OBERON_REDDISH_REGOLITH);
    private static final RuleSource IMPACT_BRECCIA = state(GCBlocks.OBERON_IMPACT_BRECCIA);
    private static final RuleSource DENSE_ICE = state(GCBlocks.DENSE_ICE);

    public static final RuleSource OBERON = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isRedPlains = SurfaceRules.isBiome(GCBiomes.Oberon.RED_PLAINS);
        ConditionSource isImpactPeaks = SurfaceRules.isBiome(GCBiomes.Oberon.IMPACT_PEAKS);
        ConditionSource isDarkBasins = SurfaceRules.isBiome(GCBiomes.Oberon.DARK_BASINS);
        ConditionSource isCaverns = SurfaceRules.isBiome(GCBiomes.Oberon.ANCIENT_CAVERNS);
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.15D, 0.45D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(primaryNoise, IMPACT_BRECCIA),
                        DARK_ROCK
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isRedPlains, REDDISH_REGOLITH),
                                        SurfaceRules.ifTrue(isImpactPeaks, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, IMPACT_BRECCIA),
                                                REDDISH_REGOLITH
                                        )),
                                        SurfaceRules.ifTrue(isDarkBasins, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, DARK_ROCK),
                                                REDDISH_REGOLITH
                                        )),
                                        REDDISH_REGOLITH
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, DARK_ROCK)
                )
        );

        RuleSource cavernRules = SurfaceRules.ifTrue(
                isCaverns,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.FLOOR), IMPACT_BRECCIA),
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.CEILING), DENSE_ICE)
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                cavernRules,
                DARK_ROCK
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("oberon"), MapCodec.unit(OBERON));
    }
}
