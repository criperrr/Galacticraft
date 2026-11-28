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

package dev.galacticraft.api.vehicle.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pluggable server-side navigation strategy selected from a modular vehicle's active domain
 * (ground 2.5D costmap, rotorcraft 3D coarse-grid, or fixed-wing 3D Dubins and glide-slope).
 */
public interface VehicleNavigator {
    void setTarget(@NotNull BlockPos targetPos);

    @Nullable BlockPos getTarget();

    void tickPlanning(@NotNull ServerLevel level, @NotNull AutonomousVehicleContext vehicle, int maxNodesPerTick);

    @NotNull ControlCommand computeControl(@NotNull ServerLevel level, @NotNull AutonomousVehicleContext vehicle);

    @NotNull NavStatus getStatus();

    @NotNull List<Vec3> getCurrentPath();

    void blacklistCell(@NotNull BlockPos pos, long gameTime, long ttlTicks);

    void reset();
}
