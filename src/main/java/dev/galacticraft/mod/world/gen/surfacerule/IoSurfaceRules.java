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

public class IoSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource SULFUR_YELLOW = state(GCBlocks.IO_SULFUR_CRUST);
    private static final RuleSource SULFUR_ORANGE = state(GCBlocks.IO_VOLCANIC_ROCK);
    private static final RuleSource PUMICE = state(GCBlocks.IO_SULFUR_CRUST);
    private static final RuleSource SOFT_VOLCANIC = state(GCBlocks.IO_VOLCANIC_ROCK);
    private static final RuleSource HARD_VOLCANIC = state(GCBlocks.IO_VOLCANIC_ROCK);
    private static final RuleSource SCORCHED_ROCK = state(GCBlocks.IO_MAGMA_CRUST);
    private static final RuleSource VOLCANIC_ROCK = state(GCBlocks.IO_SILICATE_BASALT);
    private static final RuleSource BASALT = state(GCBlocks.IO_SILICATE_BASALT);
    private static final RuleSource BLACKSTONE = state(GCBlocks.IO_SILICATE_BASALT);
    private static final RuleSource MAGMA = state(Blocks.MAGMA_BLOCK);

    public static final RuleSource IO = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isSulfurPlains = SurfaceRules.isBiome(GCBiomes.Io.SULFUR_PLAINS);
        ConditionSource isCaldera = SurfaceRules.isBiome(GCBiomes.Io.VOLCANIC_CALDERA);
        ConditionSource isSilicateFlows = SurfaceRules.isBiome(GCBiomes.Io.SILICATE_FLOWS);
        ConditionSource isMagmaTubes = SurfaceRules.isBiome(GCBiomes.Io.MAGMA_TUBES);

        ConditionSource isBelow56 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(56), 0));
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.15D, 0.45D);
        ConditionSource secondaryNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.08D, 0.55D);
        ConditionSource magmaPatchNoise = SurfaceRules.noiseCondition(Noises.GRAVEL, 0.25D, 0.70D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(primaryNoise, BASALT),
                        SurfaceRules.ifTrue(secondaryNoise, VOLCANIC_ROCK),
                        HARD_VOLCANIC
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isSulfurPlains, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, SULFUR_YELLOW),
                                                SurfaceRules.ifTrue(secondaryNoise, SULFUR_ORANGE),
                                                PUMICE
                                        )),
                                        SurfaceRules.ifTrue(isCaldera, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(magmaPatchNoise, MAGMA),
                                                SurfaceRules.ifTrue(primaryNoise, SCORCHED_ROCK),
                                                BLACKSTONE
                                        )),
                                        SurfaceRules.ifTrue(isSilicateFlows, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, BASALT),
                                                SurfaceRules.ifTrue(secondaryNoise, VOLCANIC_ROCK),
                                                SCORCHED_ROCK
                                        )),
                                        SULFUR_YELLOW
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SOFT_VOLCANIC)
                )
        );

        RuleSource caveRules = SurfaceRules.ifTrue(
                isBelow56,
                SurfaceRules.ifTrue(
                        isMagmaTubes,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(magmaPatchNoise, MAGMA),
                                        SurfaceRules.ifTrue(primaryNoise, BLACKSTONE),
                                        SCORCHED_ROCK
                                )),
                                SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, BASALT)
                        )
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                caveRules,
                HARD_VOLCANIC
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("io"), MapCodec.unit(IO));
    }
}
