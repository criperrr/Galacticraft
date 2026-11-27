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

public class TritonSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource NITROGEN_ICE = state(GCBlocks.TRITON_NITROGEN_ICE);
    private static final RuleSource CANTALOUPE_CRUST = state(GCBlocks.TRITON_CANTALOUPE_CRUST);
    private static final RuleSource CRYO_BASALT = state(GCBlocks.TRITON_CRYO_BASALT);
    private static final RuleSource BLUE_ICE = state(Blocks.BLUE_ICE);
    private static final RuleSource SNOW = state(Blocks.SNOW_BLOCK);

    public static final RuleSource TRITON = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isCantaloupe = SurfaceRules.isBiome(GCBiomes.Triton.CANTALOUPE_TERRAIN);
        ConditionSource isGeysers = SurfaceRules.isBiome(GCBiomes.Triton.NITROGEN_GEYSERS);
        ConditionSource isPolarCap = SurfaceRules.isBiome(GCBiomes.Triton.POLAR_ICE_CAP);
        ConditionSource isCaverns = SurfaceRules.isBiome(GCBiomes.Triton.SUPERIONIC_CAVERNS);
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.15D, 0.45D);
        ConditionSource streakNoise = SurfaceRules.noiseCondition(Noises.CALCITE, 0.10D, 0.55D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(streakNoise, CRYO_BASALT),
                        NITROGEN_ICE
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isCantaloupe, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, CANTALOUPE_CRUST),
                                                NITROGEN_ICE
                                        )),
                                        SurfaceRules.ifTrue(isGeysers, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(streakNoise, CRYO_BASALT),
                                                SurfaceRules.ifTrue(primaryNoise, CANTALOUPE_CRUST),
                                                NITROGEN_ICE
                                        )),
                                        SurfaceRules.ifTrue(isPolarCap, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(streakNoise, CRYO_BASALT),
                                                NITROGEN_ICE
                                        )),
                                        CANTALOUPE_CRUST
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, NITROGEN_ICE)
                )
        );

        RuleSource cavernRules = SurfaceRules.ifTrue(
                isCaverns,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.FLOOR), CRYO_BASALT),
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.CEILING), BLUE_ICE)
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                cavernRules,
                NITROGEN_ICE
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("triton"), MapCodec.unit(TRITON));
    }
}
