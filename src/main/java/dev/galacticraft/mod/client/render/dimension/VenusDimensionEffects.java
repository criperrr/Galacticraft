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
import dev.galacticraft.mod.particle.GCParticleTypes;
import dev.galacticraft.mod.world.gen.noise.DensityCellular2D;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class VenusDimensionEffects extends GalacticDimensionEffects {
    public static final VenusDimensionEffects INSTANCE = new VenusDimensionEffects(240.0F, true, SkyType.NORMAL, true, true);
    private final Minecraft minecraft = Minecraft.getInstance();
    private int rainSoundTime;

    public VenusDimensionEffects(float cloudLevel, boolean hasGround, SkyType skyType, boolean forceBrightLightmap, boolean constantAmbientLight) {
        super(cloudLevel, hasGround, skyType, forceBrightLightmap, constantAmbientLight);
    }

    @Override
    public Vec3 getFogColor(ClientLevel level, float partialTicks, Vec3 cameraPos, CubicSampler.Vec3Fetcher fetcher) {
        float night = getStarBrightness(level, 1.0F);
        float day = 1.0F - night;
        float dayColR = 203.0F / 255.0F;
        float dayColG = 147.0F / 255.0F;
        float dayColB = 0.0F / 255.0F;
        float nightColR = 131.0F / 255.0F;
        float nightColG = 108.0F / 255.0F;
        float nightColB = 46.0F / 255.0F;
        return new Vec3(dayColR * day + nightColR * night, dayColG * day + nightColG * night, dayColB * day + nightColB * night);
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false;
    }

    @Override
    public Vec3 getSkyColor(ClientLevel level, float partialTicks) {
        float night = getStarBrightness(level, 1.0F);
        float day = 1.0F - night;
        float dayColR = 255.0F / 255.0F;
        float dayColG = 207.0F / 255.0F;
        float dayColB = 81.0F / 255.0F;
        float nightColR = 118.0F / 255.0F;
        float nightColG = 89.0F / 255.0F;
        float nightColB = 21.0F / 255.0F;
        return new Vec3(dayColR * day + nightColR * night, dayColG * day + nightColG * night, dayColB * day + nightColB * night);
    }

    @Override
    public boolean tickRain(@NotNull ClientLevel level, Camera camera, int ticks) {
        // 1. GC4 TickHandlerClientVenus: tick ambient Venus sky lightning & thunder every client tick
        VenusWeatherRenderer.INSTANCE.tickLightning(level, camera);

        // 2. TFCVolcanoes (TFCVHelpers.spawnSmoke & TremorEvent.trySpawnSurfaceSmoke):
        // Emit rising smoke, embers, and charring particles from hotter geothermal zones and volcanic blocks on the ground
        tickGeothermalGroundEffects(level, camera, ticks);

        float rainLevel = level.getRainLevel(1.0F);
        float rainStrength = rainLevel / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
        if (!(rainStrength <= 0.0F)) {
            RandomSource random = RandomSource.create((long) ticks * 312987231L);
            BlockPos cameraPos = BlockPos.containing(camera.getPosition());
            BlockPos below = null;
            int particleAmount = (int) (100.0F * rainStrength * rainStrength) / (this.minecraft.options.particles().get() == ParticleStatus.DECREASED ? 2 : 1);

            for (int j = 0; j < particleAmount; ++j) {
                int localX = random.nextInt(LevelRenderer.RAIN_DIAMETER) - LevelRenderer.RAIN_RADIUS;
                int localZ = random.nextInt(LevelRenderer.RAIN_DIAMETER) - LevelRenderer.RAIN_RADIUS;
                BlockPos height = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, cameraPos.offset(localX, 0, localZ));
                if (height.getY() > level.getMinBuildHeight() && height.getY() <= cameraPos.getY() + 10 && height.getY() >= cameraPos.getY() - 10) {
                    Biome biome = level.getBiome(height).value();
                    if (biome.getPrecipitationAt(height) == Biome.Precipitation.RAIN) {
                        below = height.below();
                        if (this.minecraft.options.particles().get() == ParticleStatus.MINIMAL) {
                            break;
                        }

                        double xOffset = random.nextDouble();
                        double zOffset = random.nextDouble();
                        BlockState blockState = level.getBlockState(below);
                        FluidState fluidState = level.getFluidState(below);
                        VoxelShape shape = blockState.getCollisionShape(level, below);
                        double baseYOff = shape.max(Direction.Axis.Y, xOffset, zOffset);
                        double fluidHeight = fluidState.getHeight(level, below);
                        double yOffset = Math.max(baseYOff, fluidHeight);
                        SimpleParticleType simpleParticleType = fluidState.is(FluidTags.LAVA)
                                || blockState.is(Blocks.MAGMA_BLOCK)
                                || blockState.is(GCBlocks.SCORCHED_VENUS_ROCK)
                                || blockState.is(GCBlocks.VOLCANIC_ROCK)
                                || CampfireBlock.isLitCampfire(blockState)
                                ? ParticleTypes.SMOKE
                                : GCParticleTypes.SPLASH_VENUS;
                        this.minecraft.level.addParticle(simpleParticleType, (double) below.getX() + xOffset, (double) below.getY() + yOffset, (double) below.getZ() + zOffset, 0.0, 0.0, 0.0);
                    }
                }
            }

            if (getSoundInterval(rainLevel) > 0) {
                if (below != null && random.nextInt(getSoundInterval(rainLevel)) < this.rainSoundTime++) {
                    playWeatherSounds(level, below, cameraPos, random);
                }
            } else if (below != null && 0 < rainSoundTime++) {
                playWeatherSounds(level, below, cameraPos, random);
            }
        }

        return true;
    }

    /**
     * Ported from {@code TFCVHelpers.spawnSmoke}, {@code TFCVHelpers.sendCharringParticles}, and {@code TremorEvent.trySpawnSurfaceSmoke}:
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

            // Sample regional geothermal intensity so some regions of the planet are visibly much hotter than others
            double regionalHeat = DensityCellular2D.sampleThermalHotspot(1337L, wx, wz);
            boolean inHotZone = regionalHeat > 0.56;

            if (!isHotBlock && !(inHotZone && (isBasaltPatch || random.nextInt(4) == 0))) {
                continue;
            }

            double px = groundPos.getX() + 0.15 + random.nextDouble() * 0.7;
            double py = groundPos.getY() + 1.02;
            double pz = groundPos.getZ() + 0.15 + random.nextDouble() * 0.7;

            // TFCVHelpers.spawnSmoke: upward velocity with subtle horizontal drift
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

    public void playWeatherSounds(ClientLevel level, BlockPos below, BlockPos cameraPos, RandomSource random) {
        this.rainSoundTime = 0;
        if (below.getY() > cameraPos.getY() + 1
                && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, cameraPos).getY() > Mth.floor((float) cameraPos.getY())) {
            this.minecraft.level.playLocalSound(below, SoundEvents.LAVA_EXTINGUISH, SoundSource.WEATHER, 0.025F, 0.6F + random.nextFloat() * 0.2F, false);
        } else {
            this.minecraft.level.playLocalSound(below, SoundEvents.LAVA_EXTINGUISH, SoundSource.WEATHER, 0.04F, 0.8F + random.nextFloat() * 0.06F + random.nextFloat() * 0.06F, false);
        }
    }

    public int getSoundInterval(float rainStrength) {
        int result = 80 - (int) (rainStrength * 88F);
        return result > 0 ? result : 0;
    }
}
