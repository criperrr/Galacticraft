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

package dev.galacticraft.mod.world.gen.density;

import dev.galacticraft.mod.Constant;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class GCDensityFunctionTypes {
    private GCDensityFunctionTypes() {
    }

    public static void register() {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Constant.id("analytical_erosion"), AnalyticalErosionDensity.CODEC_MAP);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Constant.id("crater_field"), CraterFieldDensityFunction.CODEC_MAP);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Constant.id("shield_volcano"), ShieldVolcanoDensityFunction.CODEC_MAP);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Constant.id("aeolian_dune"), AeolianDuneDensityFunction.CODEC_MAP);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Constant.id("smooth_min"), SmoothMinDensityFunction.CODEC_MAP);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Constant.id("skylight_shaft"), SkylightShaftDensityFunction.CODEC_MAP);
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Constant.id("worley_cave"), WorleyCaveDensityFunction.CODEC_MAP);
    }
}
