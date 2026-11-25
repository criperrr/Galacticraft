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

public class DeimosSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource SILICATE_ROCK = state(GCBlocks.ASTEROID_ROCK_1);
    private static final RuleSource SMOOTH_REGOLITH = state(GCBlocks.MERCURY_REGOLITH);
    private static final RuleSource SURFACE_ROCK = state(GCBlocks.MOON_SURFACE_ROCK);
    private static final RuleSource MOON_ROCK = state(GCBlocks.MOON_ROCK);
    private static final RuleSource LUNASLATE = state(GCBlocks.LUNASLATE);

    public static final RuleSource DEIMOS = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isSmoothPlains = SurfaceRules.isBiome(GCBiomes.Deimos.SMOOTH_PLAINS);
        ConditionSource isCraterSlopes = SurfaceRules.isBiome(GCBiomes.Deimos.CRATER_SLOPES);
        ConditionSource isCoreCaverns = SurfaceRules.isBiome(GCBiomes.Deimos.CORE_CAVERNS);

        ConditionSource isBelow56 = SurfaceRules.not(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(56), 0));
        ConditionSource blendNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.10D, 0.50D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(blendNoise, MOON_ROCK),
                        SILICATE_ROCK
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isSmoothPlains, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(blendNoise, SMOOTH_REGOLITH),
                                                SURFACE_ROCK
                                        )),
                                        SurfaceRules.ifTrue(isCraterSlopes, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(blendNoise, MOON_ROCK),
                                                SILICATE_ROCK
                                        )),
                                        SMOOTH_REGOLITH
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, MOON_ROCK)
                )
        );

        RuleSource caveRules = SurfaceRules.ifTrue(
                isBelow56,
                SurfaceRules.ifTrue(
                        isCoreCaverns,
                        SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, LUNASLATE),
                                SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, MOON_ROCK)
                        )
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                caveRules,
                SILICATE_ROCK
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("deimos"), MapCodec.unit(DEIMOS));
    }
}
