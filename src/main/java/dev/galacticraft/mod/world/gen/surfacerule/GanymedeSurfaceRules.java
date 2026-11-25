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

public class GanymedeSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource MOON_ROCK = state(GCBlocks.MOON_ROCK);
    private static final RuleSource LUNASLATE = state(GCBlocks.LUNASLATE);
    private static final RuleSource DARK_BASALT = state(GCBlocks.MERCURY_BASALT);
    private static final RuleSource DENSE_ICE = state(GCBlocks.DENSE_ICE);
    private static final RuleSource PACKED_ICE = state(Blocks.PACKED_ICE);
    private static final RuleSource SNOW = state(Blocks.SNOW_BLOCK);

    public static final RuleSource GANYMEDE = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isSulci = SurfaceRules.isBiome(GCBiomes.Ganymede.SULCI_RIDGES);
        ConditionSource isDarkRegio = SurfaceRules.isBiome(GCBiomes.Ganymede.DARK_REGIO);
        ConditionSource isPolarFrost = SurfaceRules.isBiome(GCBiomes.Ganymede.POLAR_FROST);
        ConditionSource isIceCaverns = SurfaceRules.isBiome(GCBiomes.Ganymede.ICE_CAVERNS);

        ConditionSource isBelow56 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(56), 0));
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.12D, 0.45D);
        ConditionSource sulciNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.05D, 0.48D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(primaryNoise, DENSE_ICE),
                        LUNASLATE
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isSulci, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(sulciNoise, PACKED_ICE),
                                                SurfaceRules.ifTrue(primaryNoise, DENSE_ICE),
                                                MOON_ROCK
                                        )),
                                        SurfaceRules.ifTrue(isDarkRegio, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, DARK_BASALT),
                                                LUNASLATE
                                        )),
                                        SurfaceRules.ifTrue(isPolarFrost, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, SNOW),
                                                PACKED_ICE
                                        )),
                                        MOON_ROCK
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, MOON_ROCK)
                )
        );

        RuleSource caveRules = SurfaceRules.ifTrue(
                isBelow56,
                SurfaceRules.ifTrue(
                        isIceCaverns,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryNoise, DENSE_ICE),
                                        LUNASLATE
                                )),
                                SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, DENSE_ICE)
                        )
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                caveRules,
                LUNASLATE
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("ganymede"), MapCodec.unit(GANYMEDE));
    }
}
