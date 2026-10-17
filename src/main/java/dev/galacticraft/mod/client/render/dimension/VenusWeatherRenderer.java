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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.Constant;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class VenusWeatherRenderer implements DimensionRenderingRegistry.WeatherRenderer {
    public static final VenusWeatherRenderer INSTANCE = new VenusWeatherRenderer();
    private final float[] rainSizeX = new float[1024];
    private final float[] rainSizeZ = new float[1024];
    private static final ResourceLocation RAIN_TEXTURES = Constant.id("textures/environment/rain_venus.png");
    private final Map<BlockPos, Integer> lightning = new HashMap<>();

    public VenusWeatherRenderer() {
        for (int i = 0; i < 32; ++i) {
            float f1 = (float) (i - 16);
            for (int j = 0; j < 32; ++j) {
                float f = (float) (j - 16);
                float f2 = Mth.sqrt(f * f + f1 * f1);
                this.rainSizeX[i << 5 | j] = -f1 / f2;
                this.rainSizeZ[i << 5 | j] = f / f2;
            }
        }
    }

    /**
     * Port of GC4 Legacy {@code TickHandlerClientVenus.onClientTick}:
     * Spawns distant atmospheric lightning bolts in the Venusian sky and plays thunder sounds.
     */
    public void tickLightning(ClientLevel level, Camera camera) {
        Iterator<Map.Entry<BlockPos, Integer>> it = this.lightning.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            int val = entry.getValue();
            if (val - 1 <= 0) {
                it.remove();
            } else {
                entry.setValue(val - 1);
            }
        }

        float rainLevel = level.getRainLevel(1.0F);
        if (level.random.nextInt(300 + (int) (800.0F * rainLevel)) == 0) {
            double freq = level.random.nextDouble() * Math.PI * 2.0D;
            double dist = 180.0D;
            Vec3 camPos = camera.getPosition();
            double dX = dist * Math.cos(freq);
            double dZ = dist * Math.sin(freq);
            double posX = camPos.x() + dX;
            double posY = 70.0D + level.random.nextInt(30);
            double posZ = camPos.z() + dZ;
            this.lightning.put(BlockPos.containing(posX, posY, posZ), 20);
            level.playLocalSound(
                    posX, posY, posZ,
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER,
                    500.0F + level.random.nextFloat() * 500.0F,
                    1.0F + level.random.nextFloat() * 0.2F,
                    false
            );
        }
    }

    /**
     * Port of GC4 Legacy {@code FakeLightningBoltRenderer.renderBolt}.
     */
    private void renderBolt(long seed, double x, double y, double z) {
        double[] adouble = new double[8];
        double[] adouble1 = new double[8];
        double d3 = 0.0D;
        double d4 = 0.0D;
        Random random = new Random(seed);

        for (int i = 7; i >= 0; --i) {
            adouble[i] = d3;
            adouble1[i] = d4;
            d3 += random.nextInt(11) - 5;
            d4 += random.nextInt(11) - 5;
        }

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (int k1 = 0; k1 < 4; ++k1) {
            Random random1 = new Random(seed);

            for (int j = 0; j < 3; ++j) {
                int k = 7;
                int l = 0;

                if (j > 0) {
                    k = 7 - j;
                }

                if (j > 0) {
                    l = k - 2;
                }

                double d5 = adouble[k] - d3;
                double d6 = adouble1[k] - d4;

                for (int i1 = k; i1 >= l; --i1) {
                    double d7 = d5;
                    double d8 = d6;

                    if (j == 0) {
                        d5 += random1.nextInt(11) - 5;
                        d6 += random1.nextInt(11) - 5;
                    } else {
                        d5 += random1.nextInt(31) - 15;
                        d6 += random1.nextInt(31) - 15;
                    }

                    BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                    double d9 = 0.1D + k1 * 0.2D;

                    if (j == 0) {
                        d9 *= i1 * 0.1D + 1.0D;
                    }

                    double d10 = 0.1D + k1 * 0.2D;

                    if (j == 0) {
                        d10 *= (i1 - 1) * 0.1D + 1.0D;
                    }

                    for (int j1 = 0; j1 < 5; ++j1) {
                        double d11 = x + 0.5D - d9;
                        double d12 = z + 0.5D - d9;

                        if (j1 == 1 || j1 == 2) {
                            d11 += d9 * 2.0D;
                        }

                        if (j1 == 2 || j1 == 3) {
                            d12 += d9 * 2.0D;
                        }

                        double d13 = x + 0.5D - d10;
                        double d14 = z + 0.5D - d10;

                        if (j1 == 1 || j1 == 2) {
                            d13 += d10 * 2.0D;
                        }

                        if (j1 == 2 || j1 == 3) {
                            d14 += d10 * 2.0D;
                        }

                        buffer.addVertex((float) (d13 + d5), (float) (y + i1 * 16), (float) (d14 + d6)).setColor(0.45F, 0.45F, 0.5F, 0.3F);
                        buffer.addVertex((float) (d11 + d7), (float) (y + (i1 + 1) * 16), (float) (d12 + d8)).setColor(0.45F, 0.45F, 0.5F, 0.3F);
                    }

                    BufferUploader.drawWithShader(buffer.buildOrThrow());
                }
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }

    @Override
    public void render(WorldRenderContext context) {
        Vec3 camPos = context.camera().getPosition();
        double camX = camPos.x();
        double camY = camPos.y();
        double camZ = camPos.z();
        ClientLevel level = context.world();
        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        float strength = level.getRainLevel(partialTicks);

        if (!this.lightning.isEmpty()) {
            for (Map.Entry<BlockPos, Integer> entry : this.lightning.entrySet()) {
                BlockPos pos = entry.getKey();
                long seed = entry.getValue() / 10 + pos.getX() + pos.getZ();
                renderBolt(seed, pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ);
            }
        }

        if (strength > 0.0F) {
            context.lightmapTextureManager().turnOnLightLayer();

            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();

            int r = 4;
            if (Minecraft.useFancyGraphics()) {
                r = 8;
            }

            RenderSystem.depthMask(Minecraft.useShaderTransparency());
            int drawFlag = -1;
            RenderSystem.setShader(GameRenderer::getParticleShader);
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
            int px = Mth.floor(camX);
            int py = Mth.floor(camY);
            int pz = Mth.floor(camZ);

            BufferBuilder buffer = null;

            for (int z = pz - r; z <= pz + r; ++z) {
                int indexZ = (z - pz + 16) * 32;
                for (int x = px - r; x <= px + r; ++x) {
                    mutablePos.set(x, camY, z);
                    int yHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 4 - (int) (4.8F * strength);
                    int y = py - r;
                    int ymax = py + r;

                    if (y < yHeight) {
                        y = yHeight;
                    }

                    if (ymax < yHeight) {
                        ymax = yHeight;
                    }

                    int yBase = yHeight;

                    if (yHeight < py) {
                        yBase = py;
                    }

                    if (y != ymax) {
                        RandomSource random = RandomSource.create(x * x * 3121L + x * 45238971L ^ z * z * 418711L + z * 13761L);

                        if (drawFlag != 0) {
                            if (drawFlag >= 0 && buffer != null) {
                                BufferUploader.drawWithShader(buffer.buildOrThrow());
                            }

                            drawFlag = 0;
                            RenderSystem.setShaderTexture(0, RAIN_TEXTURES);
                            buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        int index = indexZ + x - px + 16;
                        double dx = (double) this.rainSizeX[index] * 0.5D;
                        double dz = (double) this.rainSizeZ[index] * 0.5D;
                        int u = context.worldRenderer().ticks & 131071;
                        int dy = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
                        float h = 3.0F + random.nextFloat();
                        float w = -((float) (u + dy) + partialTicks) / 80.0F * h;
                        float vOff = w % 32.0F;
                        double yo = random.nextDouble() / 1.8D;
                        double xx = x + 0.5D - camX;
                        double zz = z + 0.5D - camZ;
                        float rr = (float) Math.sqrt(xx * xx + zz * zz) / r;
                        float alpha = ((1.0F - rr * rr) * 0.5F + 0.5F) * strength / 0.6F; // 0.6F is the max rainstrength on Venus
                        mutablePos.set(x, yBase, z);
                        int light = LevelRenderer.getLightColor(level, mutablePos);
                        double xc = x + 0.5D;
                        double zc = z + 0.5D;
                        buffer.addVertex((float) (xc - camX - dx), (float) (ymax - yo - camY), (float) (zc - camZ - dz)).setUv(0.0F, (float) y * 0.25F + vOff).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light)
                                .addVertex((float) (xc - camX + dx), (float) (ymax - yo - camY), (float) (zc - camZ + dz)).setUv(1.0F, (float) y * 0.25F + vOff).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light)
                                .addVertex((float) (xc - camX + dx), (float) (y - yo - camY), (float) (zc - camZ + dz)).setUv(1.0F, (float) ymax * 0.25F + vOff).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light)
                                .addVertex((float) (xc - camX - dx), (float) (y - yo - camY), (float) (zc - camZ - dz)).setUv(0.0F, (float) ymax * 0.25F + vOff).setColor(1.0F, 1.0F, 1.0F, alpha).setLight(light);
                    }
                }
            }

            if (drawFlag >= 0) {
                BufferUploader.drawWithShader(buffer.buildOrThrow());
            }

            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            context.lightmapTextureManager().turnOffLightLayer();
        }
    }
}
