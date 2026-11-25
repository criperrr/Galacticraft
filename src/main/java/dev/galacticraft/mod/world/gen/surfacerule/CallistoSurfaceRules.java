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

public class CallistoSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource DARK_CRUST = state(GCBlocks.ASTEROID_ROCK_2);
    private static final RuleSource BASALT = state(GCBlocks.MOON_BASALT);
    private static final RuleSource BRECCIA = state(GCBlocks.MERCURY_SHOCK_BRECCIA);
    private static final RuleSource LUNASLATE = state(GCBlocks.LUNASLATE);
    private static final RuleSource DENSE_ICE = state(GCBlocks.DENSE_ICE);
    private static final RuleSource PACKED_ICE = state(Blocks.PACKED_ICE);

    public static final RuleSource CALLISTO = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isCrateredPlains = SurfaceRules.isBiome(GCBiomes.Callisto.CRATERED_PLAINS);
        ConditionSource isValhalla = SurfaceRules.isBiome(GCBiomes.Callisto.VALHALLA_RINGS);
        ConditionSource isIcePeaks = SurfaceRules.isBiome(GCBiomes.Callisto.ICE_PEAKS);
        ConditionSource isAncientCaverns = SurfaceRules.isBiome(GCBiomes.Callisto.ANCIENT_CAVERNS);

        ConditionSource isBelow56 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(56), 0));
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.12D, 0.48D);
        ConditionSource ringNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.08D, 0.50D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(ringNoise, DENSE_ICE),
                        BRECCIA
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isCrateredPlains, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, DARK_CRUST),
                                                BASALT
                                        )),
                                        SurfaceRules.ifTrue(isValhalla, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(ringNoise, DENSE_ICE),
                                                SurfaceRules.ifTrue(primaryNoise, BRECCIA),
                                                DARK_CRUST
                                        )),
                                        SurfaceRules.ifTrue(isIcePeaks, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, PACKED_ICE),
                                                DENSE_ICE
                                        )),
                                        DARK_CRUST
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, BASALT)
                )
        );

        RuleSource caveRules = SurfaceRules.ifTrue(
                isBelow56,
                SurfaceRules.ifTrue(
                        isAncientCaverns,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(primaryNoise, DENSE_ICE),
                                        LUNASLATE
                                )),
                                SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, LUNASLATE)
                        )
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                caveRules,
                BASALT
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("callisto"), MapCodec.unit(CALLISTO));
    }
}
