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

package dev.galacticraft.mod.content.vehicle;

import dev.galacticraft.api.vehicle.VehicleBenchCategory;
import dev.galacticraft.api.vehicle.VehicleRegistry;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.item.GCItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Deterministic fabrication catalog for the specialized aerospace vehicle workbenches
 * (Chassis, Electric Propulsion, Wheel/Locomotion, and Avionics/Accessories).
 */
public final class VehicleFabricatorRecipes {
    public static final ResourceLocation TOOL_VEHICLE_WRENCH = Constant.id("vehicle_wrench");
    public static final ResourceLocation TOOL_DRONE_REMOTE = Constant.id("drone_remote_controller");

    public record IngredientCost(@NotNull Item item, int count) {
    }

    public record Blueprint(
            @NotNull ResourceLocation id,
            @NotNull VehicleBenchCategory category,
            @NotNull String translationKey,
            @NotNull Supplier<ItemStack> resultSupplier,
            @NotNull List<IngredientCost> costs
    ) {
    }

    public static final List<Blueprint> ALL = List.of(
            // Chassis Assembly Bench
            new Blueprint(VehicleRegistry.CHASSIS_SCOUT_ROVER, VehicleBenchCategory.CHASSIS, "item.galacticraft.scout_rover_chassis",
                    () -> new ItemStack(GCItems.SCOUT_ROVER_CHASSIS),
                    List.of(new IngredientCost(Items.IRON_INGOT, 8), new IngredientCost(Items.COPPER_INGOT, 4))),
            new Blueprint(VehicleRegistry.CHASSIS_UTILITY_BUGGY, VehicleBenchCategory.CHASSIS, "item.galacticraft.utility_buggy_chassis",
                    () -> new ItemStack(GCItems.UTILITY_BUGGY_CHASSIS),
                    List.of(new IngredientCost(Items.IRON_INGOT, 14), new IngredientCost(Items.COPPER_INGOT, 6), new IngredientCost(Items.REDSTONE, 4))),
            new Blueprint(VehicleRegistry.CHASSIS_HEAVY_HAULER, VehicleBenchCategory.CHASSIS, "item.galacticraft.heavy_hauler_chassis",
                    () -> new ItemStack(GCItems.HEAVY_HAULER_CHASSIS),
                    List.of(new IngredientCost(Items.IRON_INGOT, 24), new IngredientCost(Items.COPPER_INGOT, 10), new IngredientCost(Items.REDSTONE, 8))),
            new Blueprint(VehicleRegistry.CHASSIS_MICRO_DRONE, VehicleBenchCategory.CHASSIS, "item.galacticraft.micro_drone_chassis",
                    () -> new ItemStack(GCItems.MICRO_DRONE_CHASSIS),
                    List.of(new IngredientCost(Items.IRON_INGOT, 6), new IngredientCost(Items.GOLD_INGOT, 4), new IngredientCost(Items.REDSTONE, 6))),
            new Blueprint(VehicleRegistry.CHASSIS_LIGHT_HELICOPTER, VehicleBenchCategory.CHASSIS, "item.galacticraft.light_helicopter_chassis",
                    () -> new ItemStack(GCItems.LIGHT_HELICOPTER_CHASSIS),
                    List.of(new IngredientCost(Items.IRON_INGOT, 16), new IngredientCost(Items.GLASS, 6), new IngredientCost(Items.COPPER_INGOT, 8))),
            new Blueprint(VehicleRegistry.CHASSIS_HEAVY_VTOL, VehicleBenchCategory.CHASSIS, "item.galacticraft.heavy_vtol_chassis",
                    () -> new ItemStack(GCItems.HEAVY_VTOL_CHASSIS),
                    List.of(new IngredientCost(Items.IRON_INGOT, 28), new IngredientCost(Items.GOLD_INGOT, 8), new IngredientCost(Items.REDSTONE, 10))),

            // Wheel, Suspension & Rotor Blades Bench
            new Blueprint(VehicleRegistry.PART_STANDARD_WHEELS, VehicleBenchCategory.LOCOMOTION, "item.galacticraft.standard_wheel_set",
                    () -> new ItemStack(GCItems.STANDARD_WHEEL_SET),
                    List.of(new IngredientCost(Items.IRON_INGOT, 2), new IngredientCost(Items.COAL, 2))),
            new Blueprint(VehicleRegistry.PART_HEAVY_TRACKS, VehicleBenchCategory.LOCOMOTION, "item.galacticraft.heavy_tracks",
                    () -> new ItemStack(GCItems.HEAVY_TRACKS),
                    List.of(new IngredientCost(Items.IRON_INGOT, 4), new IngredientCost(Items.COAL, 3))),
            new Blueprint(VehicleRegistry.PART_ROCKER_BOGIE_6X6, VehicleBenchCategory.LOCOMOTION, "item.galacticraft.rocker_bogie_6x6",
                    () -> new ItemStack(GCItems.ROCKER_BOGIE_6X6),
                    List.of(new IngredientCost(Items.IRON_INGOT, 5), new IngredientCost(Items.COPPER_INGOT, 2), new IngredientCost(Items.COAL, 3))),
            new Blueprint(VehicleRegistry.PART_COAXIAL_MARS_ROTOR, VehicleBenchCategory.LOCOMOTION, "item.galacticraft.coaxial_mars_rotor",
                    () -> new ItemStack(GCItems.COAXIAL_MARS_ROTOR),
                    List.of(new IngredientCost(Items.IRON_INGOT, 3), new IngredientCost(Items.COAL, 4), new IngredientCost(Items.GOLD_INGOT, 2))),
            new Blueprint(VehicleRegistry.PART_HEAVY_LIFT_ROTOR, VehicleBenchCategory.LOCOMOTION, "item.galacticraft.heavy_lift_rotor",
                    () -> new ItemStack(GCItems.HEAVY_LIFT_ROTOR),
                    List.of(new IngredientCost(Items.IRON_INGOT, 8), new IngredientCost(Items.COPPER_INGOT, 4), new IngredientCost(Items.COAL, 4))),

            // Electric Propulsion Bench (100% Electric, including Vacuum VTOL Thrusters)
            new Blueprint(VehicleRegistry.PART_BATTERY_BANK, VehicleBenchCategory.POWERPLANT, "item.galacticraft.battery_bank",
                    () -> new ItemStack(GCItems.BATTERY_BANK),
                    List.of(new IngredientCost(Items.COPPER_INGOT, 6), new IngredientCost(Items.REDSTONE, 8))),
            new Blueprint(VehicleRegistry.PART_SOLAR_ARRAY, VehicleBenchCategory.POWERPLANT, "item.galacticraft.solar_array_module",
                    () -> new ItemStack(GCItems.SOLAR_ARRAY_MODULE),
                    List.of(new IngredientCost(Items.GLASS, 6), new IngredientCost(Items.LAPIS_LAZULI, 6), new IngredientCost(Items.COPPER_INGOT, 4))),
            new Blueprint(VehicleRegistry.PART_RTG_CORE, VehicleBenchCategory.POWERPLANT, "item.galacticraft.rtg_core_module",
                    () -> new ItemStack(GCItems.RTG_CORE_MODULE),
                    List.of(new IngredientCost(Items.IRON_INGOT, 8), new IngredientCost(Items.GOLD_INGOT, 6), new IngredientCost(Items.REDSTONE_BLOCK, 2))),
            new Blueprint(VehicleRegistry.PART_LOX_ENGINE, VehicleBenchCategory.POWERPLANT, "item.galacticraft.lox_fuel_engine",
                    () -> new ItemStack(GCItems.LOX_FUEL_ENGINE),
                    List.of(new IngredientCost(Items.IRON_INGOT, 10), new IngredientCost(Items.COPPER_INGOT, 8), new IngredientCost(Items.REDSTONE, 8))),
            new Blueprint(VehicleRegistry.PART_VTOL_THRUSTER, VehicleBenchCategory.POWERPLANT, "item.galacticraft.vtol_thruster",
                    () -> new ItemStack(GCItems.VTOL_THRUSTER),
                    List.of(new IngredientCost(Items.IRON_INGOT, 8), new IngredientCost(Items.GOLD_INGOT, 4), new IngredientCost(Items.REDSTONE_BLOCK, 1), new IngredientCost(Items.DIAMOND, 1))),

            // Avionics, Seats & Accessories Bench
            new Blueprint(TOOL_VEHICLE_WRENCH, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.vehicle_wrench",
                    () -> new ItemStack(GCItems.VEHICLE_WRENCH),
                    List.of(new IngredientCost(Items.IRON_INGOT, 4), new IngredientCost(Items.COPPER_INGOT, 2))),
            new Blueprint(TOOL_DRONE_REMOTE, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.drone_remote_controller",
                    () -> new ItemStack(GCItems.DRONE_REMOTE_CONTROLLER),
                    List.of(new IngredientCost(Items.IRON_INGOT, 4), new IngredientCost(Items.COPPER_INGOT, 4), new IngredientCost(Items.REDSTONE, 6), new IngredientCost(Items.GLASS_PANE, 2))),
            new Blueprint(VehicleRegistry.PART_PILOT_SEAT, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.pilot_cockpit_seat",
                    () -> new ItemStack(GCItems.PILOT_COCKPIT_SEAT),
                    List.of(new IngredientCost(Items.IRON_INGOT, 4), new IngredientCost(Items.LEATHER, 3), new IngredientCost(Items.REDSTONE, 2))),
            new Blueprint(VehicleRegistry.PART_PASSENGER_SEAT, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.passenger_seat",
                    () -> new ItemStack(GCItems.PASSENGER_SEAT),
                    List.of(new IngredientCost(Items.IRON_INGOT, 3), new IngredientCost(Items.LEATHER, 3))),
            new Blueprint(VehicleRegistry.PART_CREW_BENCH, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.crew_bench_seat",
                    () -> new ItemStack(GCItems.CREW_BENCH_SEAT),
                    List.of(new IngredientCost(Items.IRON_INGOT, 5), new IngredientCost(Items.LEATHER, 6))),
            new Blueprint(VehicleRegistry.PART_CARGO_RACK, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.cargo_rack_bay",
                    () -> new ItemStack(GCItems.CARGO_RACK_BAY),
                    List.of(new IngredientCost(Items.IRON_INGOT, 4), new IngredientCost(Items.CHEST, 2))),
            new Blueprint(VehicleRegistry.PART_HEAVY_CARGO_BAY, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.heavy_cargo_container",
                    () -> new ItemStack(GCItems.HEAVY_CARGO_CONTAINER),
                    List.of(new IngredientCost(Items.IRON_INGOT, 10), new IngredientCost(Items.CHEST, 4))),
            new Blueprint(VehicleRegistry.PART_CAMERA_MAST, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.camera_mast_360",
                    () -> new ItemStack(GCItems.CAMERA_MAST_360),
                    List.of(new IngredientCost(Items.IRON_INGOT, 3), new IngredientCost(Items.GLASS_PANE, 3), new IngredientCost(Items.REDSTONE, 4))),
            new Blueprint(VehicleRegistry.PART_DRILL_ARM, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.geological_drill_arm",
                    () -> new ItemStack(GCItems.GEOLOGICAL_DRILL_ARM),
                    List.of(new IngredientCost(Items.IRON_INGOT, 8), new IngredientCost(Items.DIAMOND, 2), new IngredientCost(Items.REDSTONE, 4))),
            new Blueprint(VehicleRegistry.PART_AI_CORE, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.ai_core_module",
                    () -> new ItemStack(GCItems.AI_CORE_MODULE),
                    List.of(new IngredientCost(Items.GOLD_INGOT, 4), new IngredientCost(Items.REDSTONE, 8), new IngredientCost(Items.DIAMOND, 1))),

            // Customizable Dashboard Gauges / Screens (Avionics & Accessories Bench)
            new Blueprint(VehicleRegistry.PART_GAUGE_SPEEDOMETER, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.gauge_speedometer",
                    () -> new ItemStack(GCItems.GAUGE_SPEEDOMETER),
                    List.of(new IngredientCost(Items.GLASS_PANE, 2), new IngredientCost(Items.REDSTONE, 4), new IngredientCost(Items.IRON_INGOT, 2))),
            new Blueprint(VehicleRegistry.PART_GAUGE_POWER, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.gauge_battery_fuel",
                    () -> new ItemStack(GCItems.GAUGE_BATTERY_FUEL),
                    List.of(new IngredientCost(Items.GLASS_PANE, 2), new IngredientCost(Items.REDSTONE, 4), new IngredientCost(Items.COPPER_INGOT, 2))),
            new Blueprint(VehicleRegistry.PART_GAUGE_ATMOSPHERE, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.gauge_atmosphere_o2",
                    () -> new ItemStack(GCItems.GAUGE_ATMOSPHERE_O2),
                    List.of(new IngredientCost(Items.GLASS_PANE, 2), new IngredientCost(Items.REDSTONE, 4), new IngredientCost(Items.LAPIS_LAZULI, 2))),
            new Blueprint(VehicleRegistry.PART_GAUGE_COMPASS, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.gauge_compass_nav",
                    () -> new ItemStack(GCItems.GAUGE_COMPASS_NAV),
                    List.of(new IngredientCost(Items.GLASS_PANE, 2), new IngredientCost(Items.COMPASS, 1), new IngredientCost(Items.REDSTONE, 2))),
            new Blueprint(VehicleRegistry.PART_GAUGE_HORIZON, VehicleBenchCategory.ACCESSORIES, "item.galacticraft.gauge_horizon_radar",
                    () -> new ItemStack(GCItems.GAUGE_HORIZON_RADAR),
                    List.of(new IngredientCost(Items.GLASS_PANE, 2), new IngredientCost(Items.REDSTONE, 4), new IngredientCost(Items.GOLD_INGOT, 2)))
    );

    private VehicleFabricatorRecipes() {
    }

    public static @NotNull List<Blueprint> forCategory(@NotNull VehicleBenchCategory category) {
        if (category == VehicleBenchCategory.UNIVERSAL) {
            return ALL;
        }
        return ALL.stream().filter(bp -> category.accepts(bp.category())).toList();
    }

    public static boolean isAllowedOn(@NotNull Blueprint blueprint, @NotNull VehicleBenchCategory category) {
        return category.accepts(blueprint.category());
    }

    public static @Nullable Blueprint find(@NotNull ResourceLocation id) {
        for (Blueprint bp : ALL) {
            if (bp.id().equals(id)) {
                return bp;
            }
        }
        return null;
    }

    public static int countIngredient(@NotNull Container inputContainer, int inputSlotCount, @NotNull Item item) {
        int found = 0;
        for (int i = 0; i < inputSlotCount; i++) {
            ItemStack stack = inputContainer.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    public static boolean hasMaterials(@NotNull Container inputContainer, int inputSlotCount, @NotNull Blueprint blueprint) {
        for (IngredientCost cost : blueprint.costs()) {
            if (countIngredient(inputContainer, inputSlotCount, cost.item()) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    public static boolean consumeMaterials(@NotNull Container inputContainer, int inputSlotCount, @NotNull Blueprint blueprint) {
        if (!hasMaterials(inputContainer, inputSlotCount, blueprint)) {
            return false;
        }
        for (IngredientCost cost : blueprint.costs()) {
            int remaining = cost.count();
            for (int i = 0; i < inputSlotCount && remaining > 0; i++) {
                ItemStack stack = inputContainer.getItem(i);
                if (!stack.isEmpty() && stack.is(cost.item())) {
                    int take = Math.min(remaining, stack.getCount());
                    stack.shrink(take);
                    remaining -= take;
                }
            }
        }
        inputContainer.setChanged();
        return true;
    }
}
