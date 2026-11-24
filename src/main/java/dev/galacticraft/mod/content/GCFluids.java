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

package dev.galacticraft.mod.content;

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;

public class GCFluids {
    public static final FlowingFluid CRUDE_OIL = new CrudeOilFluid.Still();
    public static final FlowingFluid FLOWING_CRUDE_OIL = new CrudeOilFluid.Flowing();
    public static final FlowingFluid FUEL = new FuelFluid.Still();
    public static final FlowingFluid FLOWING_FUEL = new FuelFluid.Flowing();
    public static final FlowingFluid SULFURIC_ACID = new SulfuricAcidFluid.Still();
    public static final FlowingFluid FLOWING_SULFURIC_ACID = new SulfuricAcidFluid.Flowing();
    public static final FlowingFluid LIQUID_OXYGEN = new LiquidOxygenFluid.Still();
    public static final FlowingFluid FLOWING_LIQUID_OXYGEN = new LiquidOxygenFluid.Flowing();
    public static final FlowingFluid LIQUID_HYDROGEN = new LiquidHydrogenFluid.Still();
    public static final FlowingFluid FLOWING_LIQUID_HYDROGEN = new LiquidHydrogenFluid.Flowing();
    public static final FlowingFluid LIQUID_METHANE = new LiquidMethaneFluid.Still();
    public static final FlowingFluid FLOWING_LIQUID_METHANE = new LiquidMethaneFluid.Flowing();
    public static final FlowingFluid MONOPROPELLANT = new MonopropellantFluid.Still();
    public static final FlowingFluid FLOWING_MONOPROPELLANT = new MonopropellantFluid.Flowing();
    public static final FlowingFluid LIQUID_XENON = new LiquidXenonFluid.Still();
    public static final FlowingFluid FLOWING_LIQUID_XENON = new LiquidXenonFluid.Flowing();

    public static void register() {
        register(Constant.Fluid.CRUDE_OIL_STILL, CRUDE_OIL);
        register(Constant.Fluid.CRUDE_OIL_FLOWING, FLOWING_CRUDE_OIL);
        register(Constant.Fluid.FUEL_STILL, FUEL);
        register(Constant.Fluid.FUEL_FLOWING, FLOWING_FUEL);
        register(Constant.Fluid.SULFURIC_ACID_STILL, SULFURIC_ACID);
        register(Constant.Fluid.SULFURIC_ACID_FLOWING, FLOWING_SULFURIC_ACID);
        register(Constant.Fluid.LIQUID_OXYGEN_STILL, LIQUID_OXYGEN);
        register(Constant.Fluid.LIQUID_OXYGEN_FLOWING, FLOWING_LIQUID_OXYGEN);
        register(Constant.Fluid.LIQUID_HYDROGEN_STILL, LIQUID_HYDROGEN);
        register(Constant.Fluid.LIQUID_HYDROGEN_FLOWING, FLOWING_LIQUID_HYDROGEN);
        register(Constant.Fluid.LIQUID_METHANE_STILL, LIQUID_METHANE);
        register(Constant.Fluid.LIQUID_METHANE_FLOWING, FLOWING_LIQUID_METHANE);
        register(Constant.Fluid.MONOPROPELLANT_STILL, MONOPROPELLANT);
        register(Constant.Fluid.MONOPROPELLANT_FLOWING, FLOWING_MONOPROPELLANT);
        register(Constant.Fluid.LIQUID_XENON_STILL, LIQUID_XENON);
        register(Constant.Fluid.LIQUID_XENON_FLOWING, FLOWING_LIQUID_XENON);
    }

    public static void registerFluidVariantAttributes() {
        FluidVariantAttributes.register(CRUDE_OIL, new GCFluidAttribute(
                Component.translatable(GCBlocks.CRUDE_OIL.getDescriptionId())
                        .setStyle(Constant.Text.DARK_GRAY_STYLE),
                FluidConstants.LAVA_VISCOSITY,
                false
        ));
        FluidVariantAttributes.register(FUEL, new GCFluidAttribute(
                Component.translatable(GCBlocks.FUEL.getDescriptionId())
                        .setStyle(Constant.Text.GOLD_STYLE),
                2000,
                false
        ));
        FluidVariantAttributes.register(SULFURIC_ACID, new GCFluidAttribute(
                Component.translatable(GCBlocks.SULFURIC_ACID.getDescriptionId())
                        .setStyle(Constant.Text.GREEN_STYLE),
                FluidConstants.WATER_VISCOSITY,
                false
        ));
        FluidVariantAttributes.register(LIQUID_OXYGEN, new GCFluidAttribute(
                Component.translatable(GCBlocks.LIQUID_OXYGEN.getDescriptionId())
                        .setStyle(Constant.Text.AQUA_STYLE),
                1200,
                false
        ));
        FluidVariantAttributes.register(LIQUID_HYDROGEN, new GCFluidAttribute(
                Component.translatable(GCBlocks.LIQUID_HYDROGEN.getDescriptionId())
                        .setStyle(Constant.Text.AQUA_STYLE),
                600,
                true
        ));
        FluidVariantAttributes.register(LIQUID_METHANE, new GCFluidAttribute(
                Component.translatable(GCBlocks.LIQUID_METHANE.getDescriptionId())
                        .setStyle(Constant.Text.GREEN_STYLE),
                1100,
                false
        ));
        FluidVariantAttributes.register(MONOPROPELLANT, new GCFluidAttribute(
                Component.translatable(GCBlocks.MONOPROPELLANT.getDescriptionId())
                        .setStyle(Constant.Text.LIGHT_PURPLE_STYLE),
                1400,
                false
        ));
        FluidVariantAttributes.register(LIQUID_XENON, new GCFluidAttribute(
                Component.translatable(GCBlocks.LIQUID_XENON.getDescriptionId())
                        .setStyle(Constant.Text.LIGHT_PURPLE_STYLE),
                2200,
                false
        ));
    }

    private static void register(String id, Fluid fluid) {
        Registry.register(BuiltInRegistries.FLUID, Constant.id(id), fluid);
    }
}
