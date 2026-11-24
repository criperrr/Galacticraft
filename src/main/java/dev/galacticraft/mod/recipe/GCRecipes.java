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

package dev.galacticraft.mod.recipe;

import dev.galacticraft.mod.Constant;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public class GCRecipes {
    public static final RecipeType<FabricationRecipe> FABRICATION_TYPE = new GCRecipeType<>();
    public static final RecipeType<CompressingRecipe> COMPRESSING_TYPE = new GCRecipeType<>();
    public static final RecipeType<RocketRecipe> ROCKET_TYPE = new GCRecipeType<>();
    public static final RecipeType<PartAssemblerRecipe> PART_ASSEMBLER_TYPE = new GCRecipeType<>();
    public static final RecipeType<ThermalDesorptionRecipe> THERMAL_DESORPTION_TYPE = new GCRecipeType<>();
    public static final RecipeType<FusionReactorRecipe> FUSION_REACTOR_TYPE = new GCRecipeType<>();
    public static final RecipeType<MetallurgicInfusingRecipe> METALLURGIC_INFUSING_TYPE = new GCRecipeType<>();
    public static final RecipeType<ChemicalLeachingRecipe> CHEMICAL_LEACHING_TYPE = new GCRecipeType<>();
    public static final RecipeType<CompositeAutoclaveRecipe> COMPOSITE_AUTOCLAVE_TYPE = new GCRecipeType<>();
    public static final RecipeType<ElectrolyticRecipe> ELECTROLYTIC_TYPE = new GCRecipeType<>();
    public static final RecipeType<SabatierRecipe> SABATIER_TYPE = new GCRecipeType<>();
    public static final RecipeType<CentrifugeRecipe> CENTRIFUGE_TYPE = new GCRecipeType<>();
    public static final RecipeType<TelemetryAnalysisRecipe> TELEMETRY_ANALYSIS_TYPE = new GCRecipeType<>();

    public static final FabricationRecipe.Serializer FABRICATION_SERIALIZER = FabricationRecipe.Serializer.INSTANCE;
    public static final RecipeSerializer<FlagRecipe> FLAG_SERIALIZER = new SimpleCraftingRecipeSerializer<>(FlagRecipe::new);
    public static final ShapelessCompressingRecipe.Serializer SHAPELESS_COMPRESSING_SERIALIZER = ShapelessCompressingRecipe.Serializer.INSTANCE;
    public static final ShapedCompressingRecipe.Serializer SHAPED_COMPRESSING_SERIALIZER = ShapedCompressingRecipe.Serializer.INSTANCE;
    public static final RocketRecipe.Serializer ROCKET_SERIALIZER = RocketRecipe.Serializer.INSTANCE;
    public static final PartAssemblerRecipe.Serializer PART_ASSEMBLER_SERIALIZER = PartAssemblerRecipe.Serializer.INSTANCE;
    public static final ThermalDesorptionRecipe.Serializer THERMAL_DESORPTION_SERIALIZER = ThermalDesorptionRecipe.Serializer.INSTANCE;
    public static final FusionReactorRecipe.Serializer FUSION_REACTOR_SERIALIZER = FusionReactorRecipe.Serializer.INSTANCE;
    public static final MetallurgicInfusingRecipe.Serializer METALLURGIC_INFUSING_SERIALIZER = MetallurgicInfusingRecipe.Serializer.INSTANCE;
    public static final ChemicalLeachingRecipe.Serializer CHEMICAL_LEACHING_SERIALIZER = ChemicalLeachingRecipe.Serializer.INSTANCE;
    public static final CompositeAutoclaveRecipe.Serializer COMPOSITE_AUTOCLAVE_SERIALIZER = CompositeAutoclaveRecipe.Serializer.INSTANCE;
    public static final ElectrolyticRecipe.Serializer ELECTROLYTIC_SERIALIZER = ElectrolyticRecipe.Serializer.INSTANCE;
    public static final SabatierRecipe.Serializer SABATIER_SERIALIZER = SabatierRecipe.Serializer.INSTANCE;
    public static final CentrifugeRecipe.Serializer CENTRIFUGE_SERIALIZER = CentrifugeRecipe.Serializer.INSTANCE;
    public static final TelemetryAnalysisRecipe.Serializer TELEMETRY_ANALYSIS_SERIALIZER = TelemetryAnalysisRecipe.Serializer.INSTANCE;
    public static final RecipeSerializer<EmergencyKitRecipe> EMERGENCY_KIT_SERIALIZER = new SimpleCraftingRecipeSerializer<>(EmergencyKitRecipe::new);

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.FABRICATION), FABRICATION_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.COMPRESSING), COMPRESSING_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.ROCKET), ROCKET_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.PART_ASSEMBLER), PART_ASSEMBLER_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.THERMAL_DESORPTION), THERMAL_DESORPTION_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.FUSION_REACTOR), FUSION_REACTOR_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.METALLURGIC_INFUSING), METALLURGIC_INFUSING_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.CHEMICAL_LEACHING), CHEMICAL_LEACHING_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.COMPOSITE_AUTOCLAVE), COMPOSITE_AUTOCLAVE_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.ELECTROLYTIC), ELECTROLYTIC_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.SABATIER), SABATIER_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.CENTRIFUGE), CENTRIFUGE_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Constant.id(Constant.Recipe.TELEMETRY_ANALYSIS), TELEMETRY_ANALYSIS_TYPE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.FABRICATION), FABRICATION_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.FLAG), FLAG_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.COMPRESSING_SHAPELESS), SHAPELESS_COMPRESSING_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.COMPRESSING_SHAPED), SHAPED_COMPRESSING_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.ROCKET), ROCKET_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.PART_ASSEMBLER), PART_ASSEMBLER_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.THERMAL_DESORPTION), THERMAL_DESORPTION_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.FUSION_REACTOR), FUSION_REACTOR_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.METALLURGIC_INFUSING), METALLURGIC_INFUSING_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.CHEMICAL_LEACHING), CHEMICAL_LEACHING_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.COMPOSITE_AUTOCLAVE), COMPOSITE_AUTOCLAVE_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.ELECTROLYTIC), ELECTROLYTIC_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.SABATIER), SABATIER_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.CENTRIFUGE), CENTRIFUGE_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.TELEMETRY_ANALYSIS), TELEMETRY_ANALYSIS_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Constant.id(Constant.Recipe.Serializer.EMERGENCY_KIT), EMERGENCY_KIT_SERIALIZER);
    }
}