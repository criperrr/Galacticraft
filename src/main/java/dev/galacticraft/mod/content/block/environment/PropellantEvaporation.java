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

package dev.galacticraft.mod.content.block.environment;

import dev.galacticraft.api.gas.GasComposition;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.particle.GCParticleTypes;
import dev.galacticraft.mod.tag.GCBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Real-world cryogenic and volatile propellant evaporation thermodynamics and airtight dome containment.
 * - Propellant liquids evaporate when exposed to vacuum (< 0.05 bar) or temperatures above their boiling point.
 * - Open-air evaporation vents harmlessly to air with vapor plumes.
 * - Evaporation inside an enclosed dome or room (<= 512 airtight blocks) traps the vapor as PropellantGasBlock,
 *   stratified by molar buoyancy (H2/CH4 rise, O2 fills evenly & grants breathability, Xe/Hydrazine pool low).
 * - Breaching the dome wall depressurizes and vents all connected trapped gas cleanly to air.
 */
public final class PropellantEvaporation {
    public static final float MIN_ATMOSPHERE_PRESSURE_BAR = 0.05F;
    public static final int MAX_DOME_VOLUME = 512;
    public static final int SOURCE_EXPANSION_VOLUME = 64;
    public static final int FLOWING_EXPANSION_VOLUME = 6;

    private PropellantEvaporation() {
    }

    public static double getAmbientTemperatureC(Level level, BlockPos pos) {
        double tempC;
        if (level.dimensionType().ultraWarm()) {
            tempC = 450.0;
        } else {
            Holder<CelestialBody<?, ?>> bodyHolder = level.galacticraft$getCelestialBody();
            if (bodyHolder != null) {
                tempC = bodyHolder.value().atmosphere().temperature();
            } else {
                float biomeTemp = level.getBiome(pos).value().getBaseTemperature();
                tempC = (biomeTemp - 0.5) * 30.0 + 10.0;
            }
        }

        // Actively sealed life-support rooms are climate-controlled to ~20°C
        if (level.galacticraft$getSealerManager().isBlockSealed(pos)) {
            tempC = Math.max(tempC, 20.0);
        }

        // Local intense heat sources (lava, fire, magma, campfires, lit furnaces)
        for (Direction dir : Direction.values()) {
            BlockState adj = level.getBlockState(pos.relative(dir));
            if (isHotHeatSource(adj)) {
                tempC += 300.0;
                break;
            }
        }
        return tempC;
    }

    public static float getAmbientPressureBar(Level level, BlockPos pos) {
        float pressure;
        Holder<CelestialBody<?, ?>> bodyHolder = level.galacticraft$getCelestialBody();
        if (bodyHolder != null) {
            GasComposition atmosphere = bodyHolder.value().atmosphere();
            pressure = atmosphere != null ? atmosphere.pressure() : 0.0F;
        } else {
            pressure = level.getDefaultBreathable() ? 1.0F : 0.0F;
        }

        if (level.galacticraft$getSealerManager().isBlockSealed(pos)) {
            pressure = Math.max(pressure, 1.0F);
        }
        return pressure;
    }

    public static boolean shouldEvaporate(Level level, BlockPos pos, double boilingPointC) {
        return shouldEvaporate(getAmbientTemperatureC(level, pos), getAmbientPressureBar(level, pos), boilingPointC);
    }

    public static boolean shouldEvaporate(double ambientTempC, float pressureBar, double boilingPointC) {
        return pressureBar < MIN_ATMOSPHERE_PRESSURE_BAR || ambientTempC > boilingPointC;
    }

    public static boolean shouldEvaporateInAtmosphere(@Nullable CelestialBody<?, ?> body, double boilingPointC) {
        if (body == null) {
            return shouldEvaporate(20.0, 1.0F, boilingPointC);
        }
        GasComposition atmosphere = body.atmosphere();
        if (atmosphere == null) {
            return true;
        }
        return shouldEvaporate(atmosphere.temperature(), atmosphere.pressure(), boilingPointC);
    }

    public static int computeBoilDelayTicks(Level level, BlockPos pos, double boilingPointC) {
        float pressure = getAmbientPressureBar(level, pos);
        double temp = getAmbientTemperatureC(level, pos);
        if (pressure < MIN_ATMOSPHERE_PRESSURE_BAR) {
            return 12;
        }
        double superheat = temp - boilingPointC;
        if (superheat >= 250.0) {
            return 10;
        }
        if (superheat >= 120.0) {
            return 18;
        }
        return 26;
    }

    public static boolean isHotHeatSource(BlockState state) {
        if (isIgnitionSource(state) || state.is(Blocks.MAGMA_BLOCK)) {
            return true;
        }
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    public static boolean isIgnitionSource(BlockState state) {
        return state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.LAVA)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE);
    }

    public static boolean isAirtightBoundary(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(GCBlocks.PROPELLANT_GAS) || state.getBlock() instanceof LiquidBlock) {
            return false;
        }
        if (state.is(GCBlockTags.SEALABLE)) {
            return true;
        }
        return state.isCollisionShapeFullBlock(level, pos) && !state.is(GCBlockTags.UNSEALABLE);
    }

    /**
     * Bounded BFS matching SealerManager's airtight boundary semantics.
     * Returns all enclosed interior positions if the volume is airtight and <= maxVolume, or null if open/leaking.
     */
    public static @Nullable List<BlockPos> findEnclosedDomeVolume(Level level, BlockPos origin, int maxVolume) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());

        while (!queue.isEmpty()) {
            BlockPos current = queue.pollFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (!level.isInWorldBounds(current)) {
                return null;
            }
            if (visited.size() > maxVolume) {
                return null;
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (visited.contains(next)) {
                    continue;
                }
                if (!level.isInWorldBounds(next)) {
                    return null;
                }
                BlockState nextState = level.getBlockState(next);
                if (!isAirtightBoundary(level, next, nextState)) {
                    queue.addLast(next);
                }
            }
        }
        return new ArrayList<>(visited);
    }

    /**
     * Evaluates evaporation for a propellant liquid block and either vents it to open air or traps
     * the resulting vapor inside the surrounding airtight dome.
     */
    public static boolean evaporateLiquidBlock(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            PropellantGasBlock.GasKind gasKind,
            double boilingPointC
    ) {
        if (!shouldEvaporate(level, pos, boilingPointC)) {
            return false;
        }

        boolean isSource = state.hasProperty(LiquidBlock.LEVEL) && state.getValue(LiquidBlock.LEVEL) == 0;
        List<BlockPos> dome = findEnclosedDomeVolume(level, pos, MAX_DOME_VOLUME);

        // Acoustic & visual boil-off feedback
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, isSource ? 0.35F : 0.14F, 1.55F + level.random.nextFloat() * 0.35F);

        if (dome == null) {
            // Open atmosphere or vacuum -> boils off and dissipates
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.sendParticles(gasKind.createMistParticle(), pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, isSource ? 12 : 4, 0.35, 0.45, 0.35, 0.03);
            if (gasKind == PropellantGasBlock.GasKind.OXYGEN || gasKind == PropellantGasBlock.GasKind.HYDROGEN) {
                level.sendParticles(GCParticleTypes.CRYOGENIC_PARTICLE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, isSource ? 8 : 3, 0.3, 0.35, 0.3, 0.04);
            }
            return true;
        }

        // Enclosed dome/room -> gas stays trapped inside the dome!
        BlockState gasState = GCBlocks.PROPELLANT_GAS.defaultBlockState().setValue(PropellantGasBlock.GAS, gasKind);
        level.setBlockAndUpdate(pos, gasState);

        int expansionBudget = isSource ? SOURCE_EXPANSION_VOLUME : FLOWING_EXPANSION_VOLUME;
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos interiorPos : dome) {
            if (interiorPos.equals(pos)) {
                continue;
            }
            BlockState interiorState = level.getBlockState(interiorPos);
            // Also flash-evaporate any trailing flowing arms of the same fluid so they don't flow back over new gas
            if (isSource && interiorState.is(state.getBlock()) && interiorState.hasProperty(LiquidBlock.LEVEL) && interiorState.getValue(LiquidBlock.LEVEL) > 0) {
                level.setBlockAndUpdate(interiorPos, gasState);
                continue;
            }
            if (interiorState.isAir()) {
                candidates.add(interiorPos);
            }
        }

        // Stratify trapped gas inside the dome according to molar buoyancy
        if (gasKind.buoyancy() > 0) {
            // Lighter than air (H2, CH4): accumulate against the dome ceiling first
            candidates.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed()
                    .thenComparingDouble(p -> p.distSqr(pos)));
        } else if (gasKind.buoyancy() < 0) {
            // Heavier than air (Xe, Hydrazine): pool along the dome floor first
            candidates.sort(Comparator.comparingInt((BlockPos p) -> p.getY())
                    .thenComparingDouble(p -> p.distSqr(pos)));
        } else {
            // Neutral buoyancy (O2): expand evenly outward from the evaporation source
            candidates.sort(Comparator.comparingDouble(p -> p.distSqr(pos)));
        }

        int placed = 0;
        for (BlockPos target : candidates) {
            if (placed >= expansionBudget) {
                break;
            }
            level.setBlockAndUpdate(target, gasState);
            placed++;
        }

        level.sendParticles(gasKind.createMistParticle(), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 16, 0.6, 0.6, 0.6, 0.02);
        return true;
    }

    /**
     * Checks whether a trapped PropellantGasBlock is still inside an airtight dome.
     * If the dome has been breached, depressurizes all connected gas blocks back to air.
     *
     * @return true if the dome was breached and vented.
     */
    public static boolean checkDomeContainment(ServerLevel level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!originState.is(GCBlocks.PROPELLANT_GAS)) {
            return false;
        }
        if (findEnclosedDomeVolume(level, origin, MAX_DOME_VOLUME) != null) {
            return false;
        }

        PropellantGasBlock.GasKind kind = originState.getValue(PropellantGasBlock.GAS);
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin.immutable());

        int vented = 0;
        while (!queue.isEmpty() && vented < MAX_DOME_VOLUME) {
            BlockPos current = queue.pollFirst();
            if (!visited.add(current)) {
                continue;
            }
            BlockState state = level.getBlockState(current);
            if (!state.is(GCBlocks.PROPELLANT_GAS)) {
                continue;
            }
            level.setBlockAndUpdate(current, Blocks.AIR.defaultBlockState());
            vented++;
            if ((vented & 3) == 1) {
                level.sendParticles(kind.createMistParticle(), current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5, 4, 0.35, 0.35, 0.35, 0.04);
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!visited.contains(next) && level.getBlockState(next).is(GCBlocks.PROPELLANT_GAS)) {
                    queue.addLast(next);
                }
            }
        }

        if (vented > 0) {
            level.playSound(null, origin, SoundEvents.BREEZE_IDLE_AIR, SoundSource.BLOCKS, 0.5F, 1.35F);
        }
        return vented > 0;
    }

    /**
     * Ignites connected flammable trapped propellant gases (Hydrogen, Methane, Hydrazine) when exposed to fire or lava.
     */
    public static void igniteFlammableGas(ServerLevel level, BlockPos origin) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(origin.immutable());

        int burned = 0;
        while (!queue.isEmpty() && burned < 96) {
            BlockPos current = queue.pollFirst();
            if (!visited.add(current)) {
                continue;
            }
            BlockState state = level.getBlockState(current);
            if (!state.is(GCBlocks.PROPELLANT_GAS) || !state.getValue(PropellantGasBlock.GAS).flammable()) {
                continue;
            }
            level.setBlockAndUpdate(current, Blocks.AIR.defaultBlockState());
            burned++;
            if ((burned & 1) == 1) {
                level.sendParticles(ParticleTypes.FLAME, current.getX() + 0.5, current.getY() + 0.5, current.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.05);
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!visited.contains(next)) {
                    BlockState nextState = level.getBlockState(next);
                    if (nextState.is(GCBlocks.PROPELLANT_GAS) && nextState.getValue(PropellantGasBlock.GAS).flammable()) {
                        queue.addLast(next);
                    }
                }
            }
        }

        if (burned > 0) {
            float power = Math.min(3.0F, 0.8F + burned * 0.04F);
            level.explode(null, origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5, power, Level.ExplosionInteraction.NONE);
        }
    }
}
