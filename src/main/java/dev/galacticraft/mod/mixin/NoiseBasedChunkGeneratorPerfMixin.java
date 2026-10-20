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

package dev.galacticraft.mod.mixin;

import dev.galacticraft.mod.telemetry.GCPerformanceTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorPerfMixin {

    @Unique
    private static String gc$resolveDimension(StructureManager structureManager) {
        if (structureManager instanceof StructureManagerAccessor accessor) {
            LevelAccessor level = accessor.galacticraft$getLevel();
            if (level instanceof ServerLevel serverLevel) {
                return serverLevel.dimension().location().toString();
            } else if (level instanceof WorldGenRegion worldGenRegion) {
                return worldGenRegion.getLevel().dimension().location().toString();
            }
        }
        return "unknown";
    }

    @Inject(method = "createBiomes", at = @At("RETURN"))
    private void gc$measureBiomes(RandomState randomState, Blender blender, StructureManager structureManager, ChunkAccess chunkAccess, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        String dimension = gc$resolveDimension(structureManager);
        long start = System.nanoTime();
        CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();
        cir.setReturnValue(originalFuture.thenApply(chunk -> {
            long duration = System.nanoTime() - start;
            GCPerformanceTracker.recordStage(dimension, "biomes", duration);
            return chunk;
        }));
    }

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void gc$measureNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunkAccess, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        String dimension = gc$resolveDimension(structureManager);
        int chunkX = chunkAccess.getPos().x;
        int chunkZ = chunkAccess.getPos().z;
        long start = System.nanoTime();
        CompletableFuture<ChunkAccess> originalFuture = cir.getReturnValue();
        cir.setReturnValue(originalFuture.thenApply(chunk -> {
            long duration = System.nanoTime() - start;
            GCPerformanceTracker.recordStage(dimension, "noise", duration);
            if (duration > 80_000_000L) { // If noise alone took >80ms
                GCPerformanceTracker.recordSlowChunk(dimension, chunkX, chunkZ, duration, 0, duration, 0, 0, 0);
            }
            return chunk;
        }));
    }

    @Unique
    private static final ThreadLocal<Long> gc$surfaceStartNano = new ThreadLocal<>();

    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("HEAD"))
    private void gc$buildSurfaceStart(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess, CallbackInfo ci) {
        gc$surfaceStartNano.set(System.nanoTime());
    }

    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("RETURN"))
    private void gc$buildSurfaceEnd(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess, CallbackInfo ci) {
        Long start = gc$surfaceStartNano.get();
        if (start != null) {
            long duration = System.nanoTime() - start;
            String dimension = region.getLevel().dimension().location().toString();
            GCPerformanceTracker.recordStage(dimension, "surface", duration);
        }
    }

    @Inject(method = "applyCarvers", at = @At("HEAD"))
    private void gc$applyCarversStart(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess, GenerationStep.Carving step, CallbackInfo ci) {
        gc$carverStartNano.set(System.nanoTime());
    }

    @Inject(method = "applyCarvers", at = @At("RETURN"))
    private void gc$applyCarversEnd(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess, GenerationStep.Carving step, CallbackInfo ci) {
        Long start = gc$carverStartNano.get();
        if (start != null) {
            long duration = System.nanoTime() - start;
            String dimension = region.getLevel().dimension().location().toString();
            GCPerformanceTracker.recordStage(dimension, "carvers", duration);
        }
    }

    @Unique
    private static final ThreadLocal<Long> gc$carverStartNano = new ThreadLocal<>();
}
