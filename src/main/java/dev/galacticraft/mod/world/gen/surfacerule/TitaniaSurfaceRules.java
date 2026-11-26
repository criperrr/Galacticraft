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

public class TitaniaSurfaceRules {
    private static final RuleSource BEDROCK = state(Blocks.BEDROCK);
    private static final RuleSource ICE_SILICATE = state(GCBlocks.TITANIA_ICE_SILICATE);
    private static final RuleSource FROST_CRUST = state(GCBlocks.TITANIA_FROST_CRUST);
    private static final RuleSource CHASM_SLATE = state(GCBlocks.TITANIA_CHASM_SLATE);
    private static final RuleSource DENSE_ICE = state(GCBlocks.DENSE_ICE);

    public static final RuleSource TITANIA = createDefaultRule();

    public static @NotNull RuleSource createDefaultRule() {
        ConditionSource isChasmata = SurfaceRules.isBiome(GCBiomes.Titania.MESSINA_CHASMATA);
        ConditionSource isFrostPlains = SurfaceRules.isBiome(GCBiomes.Titania.FROST_PLAINS);
        ConditionSource isHighlands = SurfaceRules.isBiome(GCBiomes.Titania.SCARP_HIGHLANDS);
        ConditionSource isCaverns = SurfaceRules.isBiome(GCBiomes.Titania.FAULT_CAVERNS);
        ConditionSource primaryNoise = SurfaceRules.noiseCondition(Noises.SURFACE, -0.15D, 0.45D);

        RuleSource steepCliffs = SurfaceRules.ifTrue(
                SurfaceRules.steep(),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(isChasmata, CHASM_SLATE),
                        SurfaceRules.ifTrue(primaryNoise, CHASM_SLATE),
                        ICE_SILICATE
                )
        );

        RuleSource surfaceRules = SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.sequence(
                        steepCliffs,
                        SurfaceRules.ifTrue(
                                SurfaceRules.ON_FLOOR,
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(isChasmata, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, CHASM_SLATE),
                                                ICE_SILICATE
                                        )),
                                        SurfaceRules.ifTrue(isFrostPlains, FROST_CRUST),
                                        SurfaceRules.ifTrue(isHighlands, SurfaceRules.sequence(
                                                SurfaceRules.ifTrue(primaryNoise, ICE_SILICATE),
                                                FROST_CRUST
                                        )),
                                        FROST_CRUST
                                )
                        ),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, ICE_SILICATE)
                )
        );

        RuleSource cavernRules = SurfaceRules.ifTrue(
                isCaverns,
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.FLOOR), CHASM_SLATE),
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(1, false, 0, CaveSurface.CEILING), DENSE_ICE)
                )
        );

        return SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)), BEDROCK),
                surfaceRules,
                cavernRules,
                ICE_SILICATE
        );
    }

    private static @NotNull RuleSource state(@NotNull Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    public static void register() {
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Constant.id("titania"), MapCodec.unit(TITANIA));
    }
}
