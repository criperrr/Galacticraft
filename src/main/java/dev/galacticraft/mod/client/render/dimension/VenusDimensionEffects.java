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

package dev.galacticraft.mod.client.render.dimension;

import dev.galacticraft.mod.api.dimension.GalacticDimensionEffects;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.world.gen.noise.DensityCellular2D;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Dimension effects for Venus, faithfully restoring its authentic dense sulfuric atmosphere:
 * rich warm golden-yellow atmospheric fog by day, warm glowing amber by night,
 * Geoffrey Landis cloud layer transition above Y=200 into space, and volcanic geothermal ground effects.
 */
@Environment(EnvType.CLIENT)
public class VenusDimensionEffects extends GalacticDimensionEffects {
    public static final VenusDimensionEffects INSTANCE = new VenusDimensionEffects();
    private final Minecraft minecraft = Minecraft.getInstance();

    public VenusDimensionEffects() {
        super(Float.NaN, false, SkyType.NORMAL, false, false);
    }

    public VenusDimensionEffects(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight) {
        super(cloudLevel, hasGround, skyType, forceBrightLightmap, constantAmbientLight);
    }

    @Override
    public Vec3 getFogColor(ClientLevel level, float partialTicks, Vec3 cameraPos, CubicSampler.Vec3Fetcher fetcher) {
        float night = getStarBrightness(level, partialTicks);
        float day = 1.0F - night;
        float r = (203.0F / 255.0F) * day + (131.0F / 255.0F) * night;
        float g = (147.0F / 255.0F) * day + (108.0F / 255.0F) * night;
        float b = (0.0F / 255.0F) * day + (46.0F / 255.0F) * night;

        // Geoffrey Landis cloud layer transition: above Y=200, dense sulfuric fog fades into space
        if (cameraPos.y() > 200.0D) {
            float spaceProgress = Mth.clamp((float) ((cameraPos.y() - 200.0D) / 250.0D), 0.0F, 1.0F);
            float atmosphereAlpha = 1.0F - spaceProgress;
            return new Vec3(r * atmosphereAlpha, g * atmosphereAlpha, b * atmosphereAlpha);
        }

        return new Vec3(r, g, b);
    }

    @Override
    public Vec3 getSkyColor(ClientLevel level, float partialTicks) {
        float night = getStarBrightness(level, partialTicks);
        float day = 1.0F - night;
        float r = (255.0F / 255.0F) * day + (118.0F / 255.0F) * night;
        float g = (207.0F / 255.0F) * day + (89.0F / 255.0F) * night;
        float b = (81.0F / 255.0F) * day + (21.0F / 255.0F) * night;
        return new Vec3(r, g, b);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor;
    }

    @Override
    public boolean isFoggyAt(int camX, int camY) {
        return false;
    }

    @Override
    public float[] getSunriseColor(float skyAngle, float tickDelta) {
        return null;
    }

    @Override
    public boolean tickRain(@NotNull ClientLevel level, Camera camera, int ticks) {
        tickGeothermalGroundEffects(level, camera, ticks);
        VenusWeatherRenderer.INSTANCE.tickLightning(level, camera);
        return true;
    }

    /**
     * Makes hotter regions of Venus (geothermal hotspots, volcano slopes, magma/scorched rock patches, and fallen meteors)
     * visibly vent rising smoke plumes, embers, and lava sparks directly out of the ground.
     */
    private void tickGeothermalGroundEffects(ClientLevel level, Camera camera, int ticks) {
        if (this.minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
            return;
        }

        RandomSource random = RandomSource.create((long) ticks * 982451653L ^ 0x5DEECE66DL);
        BlockPos cameraPos = BlockPos.containing(camera.getPosition());
        int samples = this.minecraft.options.particles().get() == ParticleStatus.DECREASED ? 6 : 14;

        for (int i = 0; i < samples; i++) {
            int dx = random.nextInt(48) - 24;
            int dz = random.nextInt(48) - 24;
            int wx = cameraPos.getX() + dx;
            int wz = cameraPos.getZ() + dz;

            BlockPos surfaceTop = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(wx, 0, wz));
            BlockPos groundPos = surfaceTop.below();
            if (groundPos.getY() <= level.getMinBuildHeight() || Math.abs(groundPos.getY() - cameraPos.getY()) > 28) {
                continue;
            }

            BlockState state = level.getBlockState(groundPos);
            FluidState fluid = level.getFluidState(groundPos);

            boolean isHotBlock = fluid.is(FluidTags.LAVA)
                    || state.is(Blocks.MAGMA_BLOCK)
                    || state.is(GCBlocks.SCORCHED_VENUS_ROCK)
                    || state.is(GCBlocks.VOLCANIC_ROCK)
                    || state.is(GCBlocks.FALLEN_METEOR);
            boolean isBasaltPatch = state.is(Blocks.BASALT) || state.is(Blocks.SMOOTH_BASALT);

            double regionalHeat = DensityCellular2D.sampleThermalHotspot(1337L, wx, wz);
            boolean inHotZone = regionalHeat > 0.56;

            if (!isHotBlock && !(inHotZone && (isBasaltPatch || random.nextInt(4) == 0))) {
                continue;
            }

            double px = groundPos.getX() + 0.15 + random.nextDouble() * 0.7;
            double py = groundPos.getY() + 1.02;
            double pz = groundPos.getZ() + 0.15 + random.nextDouble() * 0.7;

            double vx = (random.nextDouble() - 0.5) * 0.02;
            double vy = 0.04 + random.nextDouble() * 0.08;
            double vz = (random.nextDouble() - 0.5) * 0.02;

            if (fluid.is(FluidTags.LAVA) || state.is(Blocks.MAGMA_BLOCK) || state.is(GCBlocks.FALLEN_METEOR)) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, px, py, pz, vx, vy * 1.15, vz);
                if (random.nextInt(4) == 0) {
                    level.addParticle(ParticleTypes.LAVA, px, py, pz, 0.0, 0.05, 0.0);
                }
                if (random.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py + 0.1, pz, vx * 0.5, 0.06, vz * 0.5);
                }
                if (random.nextInt(45) == 0) {
                    level.playLocalSound(groundPos, SoundEvents.LAVA_POP, SoundSource.AMBIENT, 0.18F, 0.75F + random.nextFloat() * 0.35F, false);
                }
            } else if (isHotBlock || regionalHeat > 0.68) {
                level.addParticle(ParticleTypes.SMOKE, px, py, pz, vx, vy, vz);
                if (random.nextInt(5) == 0) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, px, py, pz, vx * 0.8, vy * 0.9, vz * 0.8);
                }
                if (random.nextInt(9) == 0) {
                    level.addParticle(ParticleTypes.FLAME, px, py + 0.05, pz, vx * 0.3, 0.02, vz * 0.3);
                }
            } else if (inHotZone && random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SMOKE, px, py, pz, vx * 0.7, vy * 0.75, vz * 0.7);
            }
        }
    }
}
