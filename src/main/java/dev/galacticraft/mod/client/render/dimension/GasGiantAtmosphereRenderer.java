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
import dev.galacticraft.mod.events.GCGasGiantAtmosphereHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Cinematic 3D atmospheric cloud-deck, zonal jet-stream wind, and deep superstorm lightning renderer
 * for Gas &amp; Ice Giants (Jupiter, Saturn, Uranus, Neptune).
 * <p>
 * Renders:
 * <ul>
 *   <li>4 real-world-altitude volumetric banded cloud decks ({@code Y = 192, 148, 92, 26}) visible continuously
 *       from the abyssal floor all the way past the {@code Y = 216} aerostatic zone up to exosphere exit.</li>
 *   <li>Animated zonal wind ribbons &amp; howling storm gusts flowing horizontally along each planet's jet streams.</li>
 *   <li>Dense multi-branch 3D storm lightning bolts and volumetric sheet-lightning cloud illumination in the lower
 *       storm layers ({@code Y < 175}), intensifying dramatically with depth.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class GasGiantAtmosphereRenderer implements DimensionRenderingRegistry.WeatherRenderer {
    public static final GasGiantAtmosphereRenderer JUPITER = new GasGiantAtmosphereRenderer(
            0.94F, 0.78F, 0.56F,
            0.78F, 0.40F, 0.20F,
            0.52F, 0.24F, 0.13F,
            0.98F, 0.84F, 0.66F,
            1.00F, 0.90F, 0.74F, // Warm superbolt amber-white lightning
            1.35F,
            1.00F
    );

    public static final GasGiantAtmosphereRenderer SATURN = new GasGiantAtmosphereRenderer(
            0.96F, 0.88F, 0.64F,
            0.82F, 0.66F, 0.40F,
            0.56F, 0.42F, 0.22F,
            1.00F, 0.92F, 0.74F,
            1.00F, 0.95F, 0.78F, // Golden-white dragon-storm lightning
            1.15F,
            0.88F
    );

    public static final GasGiantAtmosphereRenderer URANUS = new GasGiantAtmosphereRenderer(
            0.66F, 0.94F, 0.96F,
            0.34F, 0.76F, 0.82F,
            0.16F, 0.48F, 0.58F,
            0.80F, 0.98F, 1.00F,
            0.72F, 0.98F, 1.00F, // Electric cryo-cyan discharges
            0.95F,
            0.62F
    );

    public static final GasGiantAtmosphereRenderer NEPTUNE = new GasGiantAtmosphereRenderer(
            0.58F, 0.78F, 1.00F,
            0.22F, 0.46F, 0.92F,
            0.12F, 0.24F, 0.64F,
            0.76F, 0.90F, 1.00F,
            0.76F, 0.84F, 1.00F, // Supersonic violet-blue plasma bolts
            1.85F,
            0.75F
    );

    private static final CloudDeck[] CLOUD_DECKS = new CloudDeck[]{
            // High sunlit wispy ammonia/methane cirrus just beneath the Y=216 aerostatic deck
            new CloudDeck(192.0D, 7.0F, 0.64F, 0.90F, 20.0F, 1.15F, 0.10F),
            // Primary dense banded cloud ocean
            new CloudDeck(148.0D, 12.0F, 0.86F, 1.40F, 22.0F, 0.96F, -0.02F),
            // Lower turbulent superstorm deck (heavily lit by sheet lightning)
            new CloudDeck(92.0D, 14.0F, 0.88F, 1.85F, 24.0F, 0.84F, -0.08F),
            // Deep pressurized mist canopy above the hyperbaric abyssal mantle
            new CloudDeck(26.0D, 10.0F, 0.72F, 1.25F, 22.0F, 0.74F, -0.10F)
    };

    private static final int GRID_HALF = 12; // 24x24 cells -> ~528x528 blocks around camera
    private static final int BASE_WIND_RIBBONS = 60;
    private static final int STORM_GUST_RIBBONS = 56;
    private static final int MAX_ACTIVE_BOLTS = 8;

    private final float bandAR, bandAG, bandAB;
    private final float bandBR, bandBG, bandBB;
    private final float deepR, deepG, deepB;
    private final float windR, windG, windB;
    private final float boltR, boltG, boltB;
    private final float windSpeedScale;
    private final float stormActivity;

    private final List<StormBolt> activeBolts = new ArrayList<>();
    private long lastBoltTick = Long.MIN_VALUE;

    public GasGiantAtmosphereRenderer(
            float bandAR, float bandAG, float bandAB,
            float bandBR, float bandBG, float bandBB,
            float deepR, float deepG, float deepB,
            float windR, float windG, float windB,
            float boltR, float boltG, float boltB,
            float windSpeedScale,
            float stormActivity
    ) {
        this.bandAR = bandAR;
        this.bandAG = bandAG;
        this.bandAB = bandAB;
        this.bandBR = bandBR;
        this.bandBG = bandBG;
        this.bandBB = bandBB;
        this.deepR = deepR;
        this.deepG = deepG;
        this.deepB = deepB;
        this.windR = windR;
        this.windG = windG;
        this.windB = windB;
        this.boltR = boltR;
        this.boltG = boltG;
        this.boltB = boltB;
        this.windSpeedScale = windSpeedScale;
        this.stormActivity = stormActivity;
    }

    @Override
    public void render(WorldRenderContext context) {
        ClientLevel level = context.world();
        if (level == null) {
            return;
        }
        Vec3 camPos = context.camera().getPosition();
        double camX = camPos.x();
        double camY = camPos.y();
        double camZ = camPos.z();

        // Fade out smoothly only when ascending into space (Y = 400 -> 700), NEVER at Y = 200!
        if (camY >= Constant.ATMOSPHERE_EXIT_END) {
            return;
        }
        float spaceFade = 1.0F;
        if (camY > Constant.ATMOSPHERE_FADE_START) {
            float t = Mth.clamp((float) ((camY - Constant.ATMOSPHERE_FADE_START) / (double) (Constant.ATMOSPHERE_EXIT_END - Constant.ATMOSPHERE_FADE_START)), 0.0F, 1.0F);
            spaceFade = 1.0F - t * t * (3.0F - 2.0F * t);
        }
        if (spaceFade <= 0.01F) {
            return;
        }

        long gameTime = level.getGameTime();
        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        double timeSec = ((gameTime & 0x3FFFFL) + partialTicks) * 0.05D;
        float celestialAngle = level.getTimeOfDay(partialTicks);
        float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * Mth.TWO_PI) * 2.0F + 0.25F), 0.0F, 1.0F);
        float dayLight = Mth.lerp(1.0F - starBrightness, 0.46F, 1.05F);
        float optical = GCGasGiantAtmosphereHandler.computeOpticalTransmittance(level.dimension(), camY);
        float ambientLit = dayLight * (0.50F + 0.50F * optical);

        // Storm intensity ramps sharply below Y = 175 and peaks in the lower storm decks (Y = -50..135)
        float deepStormFactor = Mth.clamp((float) ((185.0D - camY) / 115.0D), 0.0F, 1.0F);
        updateStormBolts(level, gameTime, camX, camY, camZ, deepStormFactor);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 1. Render all 4 multi-altitude 3D volumetric cloud decks (sorted far -> near vertically)
        int[] order = new int[]{0, 1, 2, 3};
        for (int i = 1; i < order.length; i++) {
            int key = order[i];
            double keyDist = Math.abs(CLOUD_DECKS[key].altitudeY - camY);
            int j = i - 1;
            while (j >= 0 && Math.abs(CLOUD_DECKS[order[j]].altitudeY - camY) < keyDist) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = key;
        }

        for (int idx : order) {
            renderCloudDeck(CLOUD_DECKS[idx], idx, camX, camY, camZ, timeSec, ambientLit, spaceFade, gameTime, partialTicks);
        }

        // 2. Render flowing 3D zonal wind ribbons & howling storm gusts
        if (camY <= 360.0D) {
            renderZonalWindRibbons(level, camX, camY, camZ, timeSec, ambientLit, spaceFade, deepStormFactor);
        }

        // 3. Render additive 3D branching storm lightning bolts between cloud decks & down toward the abyssal mantle
        if (!this.activeBolts.isEmpty()) {
            renderStormLightningBolts(camX, camY, camZ, gameTime, partialTicks, spaceFade);
        }

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    private void updateStormBolts(ClientLevel level, long gameTime, double camX, double camY, double camZ, float deepStormFactor) {
        if (gameTime == this.lastBoltTick) {
            return;
        }
        this.lastBoltTick = gameTime;

        Iterator<StormBolt> it = this.activeBolts.iterator();
        while (it.hasNext()) {
            if (gameTime >= it.next().expireTick) {
                it.remove();
            }
        }

        // Distant bolts visible below from aerostatic altitudes, intensifying heavily in lower storm decks (Y < 170)
        if (camY > 280.0D || this.activeBolts.size() >= MAX_ACTIVE_BOLTS) {
            return;
        }

        // Spawn probability per tick: modest high up, dramatic frequent superstorms in lower layers
        float spawnProb = (0.012F + 0.14F * deepStormFactor * deepStormFactor) * this.stormActivity;
        if (level.random.nextFloat() < spawnProb) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double minDist = deepStormFactor > 0.35F ? 22.0D : 55.0D;
            double maxDist = deepStormFactor > 0.35F ? 115.0D : 165.0D;
            double dist = minDist + level.random.nextDouble() * (maxDist - minDist);
            double bx = camX + Math.cos(angle) * dist;
            double bz = camZ + Math.sin(angle) * dist;

            // Choose between cloud-to-cloud superbolts and cloud-to-abyssal-mantle bolts depending on player altitude
            double topY;
            double bottomY;
            if (camY < 60.0D && level.random.nextFloat() < 0.70F) {
                topY = 90.0D + level.random.nextDouble() * 45.0D;
                bottomY = -42.0D + level.random.nextDouble() * 35.0D;
            } else if (camY < 165.0D) {
                topY = 148.0D + level.random.nextDouble() * 35.0D;
                bottomY = 15.0D + level.random.nextDouble() * 65.0D;
            } else {
                // Distant bolts glowing beneath the aerostatic platform in the lower storm cloud deck
                topY = 148.0D + level.random.nextDouble() * 20.0D;
                bottomY = 65.0D + level.random.nextDouble() * 35.0D;
            }

            int duration = 8 + level.random.nextInt(10);
            long seed = level.random.nextLong();
            this.activeBolts.add(new StormBolt(bx, topY, bottomY, bz, gameTime, gameTime + duration, seed));

            // Play atmospheric rolling thunder when inside or near the storm layers
            if (deepStormFactor > 0.15F && dist < 120.0D && level.random.nextFloat() < 0.55F) {
                float vol = (float) Mth.clamp((1.0D - dist / 140.0D) * (0.45D + 0.95D * deepStormFactor), 0.15D, 1.4D);
                float pitch = 0.55F + level.random.nextFloat() * 0.35F;
                level.playLocalSound(bx, (topY + bottomY) * 0.5D, bz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, vol, pitch, false);
            }
        }
    }

    private void renderCloudDeck(
            CloudDeck deck,
            int deckIdx,
            double camX,
            double camY,
            double camZ,
            double timeSec,
            float ambientLit,
            float spaceFade,
            long gameTime,
            float partialTicks
    ) {
        double vertDist = Math.abs(camY - deck.altitudeY);
        if (vertDist > 360.0D) {
            return;
        }
        // Keep decks richly visible even when hovering high above at Y=220..320!
        float verticalFade = 1.0F - Mth.clamp((float) ((vertDist - 240.0D) / 120.0D), 0.0F, 1.0F);
        float deckOpacity = deck.baseAlpha * verticalFade * spaceFade;
        if (deckOpacity <= 0.015F) {
            return;
        }

        float cell = deck.cellSize;
        float maxRadius = GRID_HALF * cell;
        double baseCellX = Math.floor(camX / cell) * cell;
        double baseCellZ = Math.floor(camZ / cell) * cell;

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        boolean hasVerts = false;

        float halfThick = deck.thickness * 0.5F;
        for (int slice = 0; slice < 2; slice++) {
            float yOffset = slice == 0 ? halfThick : -halfThick;
            float sliceShade = slice == 0 ? 1.08F : 0.72F;
            float sliceAlphaScale = slice == 0 ? 1.0F : 0.76F;
            float relY = (float) (deck.altitudeY + yOffset - camY);

            float planeDist = Math.abs(relY);
            float nearFade = Mth.clamp(planeDist / 3.5F, 0.28F, 1.0F);

            for (int gx = -GRID_HALF; gx < GRID_HALF; gx++) {
                double wx0 = baseCellX + gx * cell;
                double wx1 = wx0 + cell;
                float rx0 = (float) (wx0 - camX);
                float rx1 = (float) (wx1 - camX);

                for (int gz = -GRID_HALF; gz < GRID_HALF; gz++) {
                    double wz0 = baseCellZ + gz * cell;
                    double wz1 = wz0 + cell;
                    float rz0 = (float) (wz0 - camZ);
                    float rz1 = (float) (wz1 - camZ);

                    float cx = (rx0 + rx1) * 0.5F;
                    float cz = (rz0 + rz1) * 0.5F;
                    float rDist = Mth.sqrt(cx * cx + cz * cz);
                    if (rDist > maxRadius) {
                        continue;
                    }

                    double bandSign = Math.sin(wz0 * 0.012D) >= 0.0D ? 1.0D : -1.0D;
                    double driftX = timeSec * deck.driftSpeed * this.windSpeedScale * bandSign * 9.5D;
                    float density = sampleCloudDensity(wx0 + cell * 0.5D - driftX, wz0 + cell * 0.5D, deckIdx);
                    float cloudMask = Mth.clamp((density - deck.threshold) * 2.15F, 0.0F, 1.0F);
                    if (cloudMask <= 0.02F) {
                        continue;
                    }

                    float edgeFade = 1.0F - Mth.clamp((rDist - maxRadius * 0.56F) / (maxRadius * 0.44F), 0.0F, 1.0F);
                    edgeFade = edgeFade * edgeFade * (3.0F - 2.0F * edgeFade);

                    float alpha = Mth.clamp(cloudMask * deckOpacity * sliceAlphaScale * nearFade * edgeFade, 0.0F, 0.92F);
                    if (alpha <= 0.01F) {
                        continue;
                    }

                    float bandT = 0.5F + 0.5F * Mth.sin((float) (wz0 * 0.018D + Math.sin((wx0 - driftX) * 0.008D) * 1.4D + deckIdx * 0.9D));
                    float depthBlend = deckIdx / 3.0F;
                    float baseR = Mth.lerp(bandT, this.bandBR, this.bandAR);
                    float baseG = Mth.lerp(bandT, this.bandBG, this.bandAG);
                    float baseB = Mth.lerp(bandT, this.bandBB, this.bandAB);

                    float r = Mth.lerp(depthBlend * 0.55F, baseR, this.deepR) * deck.brightness * sliceShade * ambientLit;
                    float g = Mth.lerp(depthBlend * 0.55F, baseG, this.deepG) * deck.brightness * sliceShade * ambientLit;
                    float b = Mth.lerp(depthBlend * 0.55F, baseB, this.deepB) * deck.brightness * sliceShade * ambientLit;

                    // Volumetric sheet-lightning cloud illumination from every nearby active storm bolt!
                    if (!this.activeBolts.isEmpty()) {
                        double cellCenterX = wx0 + cell * 0.5D;
                        double cellCenterZ = wz0 + cell * 0.5D;
                        float totalFlash = 0.0F;
                        for (StormBolt bolt : this.activeBolts) {
                            double dx = cellCenterX - bolt.x;
                            double dz = cellCenterZ - bolt.z;
                            double dSq = dx * dx + dz * dz;
                            if (dSq < 11000.0D) {
                                float flicker = bolt.computeFlicker(gameTime, partialTicks);
                                totalFlash += flicker * (float) Math.exp(-dSq / 2600.0D);
                            }
                        }
                        if (totalFlash > 0.01F) {
                            float glow = Math.min(0.85F, totalFlash * 0.72F);
                            r = Mth.clamp(r + glow * this.boltR, 0.0F, 1.0F);
                            g = Mth.clamp(g + glow * this.boltG, 0.0F, 1.0F);
                            b = Mth.clamp(b + glow * this.boltB, 0.0F, 1.0F);
                        }
                    }

                    r = Mth.clamp(r, 0.0F, 1.0F);
                    g = Mth.clamp(g, 0.0F, 1.0F);
                    b = Mth.clamp(b, 0.0F, 1.0F);

                    float y00 = relY + sampleWaveY(wx0 - driftX, wz0, deckIdx);
                    float y10 = relY + sampleWaveY(wx1 - driftX, wz0, deckIdx);
                    float y11 = relY + sampleWaveY(wx1 - driftX, wz1, deckIdx);
                    float y01 = relY + sampleWaveY(wx0 - driftX, wz1, deckIdx);

                    bb.addVertex(rx0, y00, rz0).setColor(r, g, b, alpha);
                    bb.addVertex(rx1, y10, rz0).setColor(r, g, b, alpha);
                    bb.addVertex(rx1, y11, rz1).setColor(r, g, b, alpha);

                    bb.addVertex(rx0, y00, rz0).setColor(r, g, b, alpha);
                    bb.addVertex(rx1, y11, rz1).setColor(r, g, b, alpha);
                    bb.addVertex(rx0, y01, rz1).setColor(r, g, b, alpha);
                    hasVerts = true;
                }
            }
        }

        if (hasVerts) {
            MeshData mesh = bb.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        } else {
            MeshData empty = bb.build();
            if (empty != null) {
                empty.close();
            }
        }
    }

    /**
     * Renders sleek 3D translucent wind streamlines &amp; howling storm gusts whipping along the planet's jet streams.
     */
    private void renderZonalWindRibbons(
            ClientLevel level,
            double camX,
            double camY,
            double camZ,
            double timeSec,
            float ambientLit,
            float spaceFade,
            float deepStormFactor
    ) {
        // Gentle stratospheric wisps at Y=216..320, fierce howling storm streaks at Y=-50..185
        float cloudIntensity = 1.0F - Mth.clamp((float) ((camY - 210.0D) / 140.0D), 0.0F, 0.72F);
        float ribbonOpacity = (0.36F + 0.36F * cloudIntensity + 0.14F * deepStormFactor) * spaceFade;
        if (ribbonOpacity <= 0.02F) {
            return;
        }

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        boolean hasVerts = false;
        double boxSize = 96.0D;
        double halfBox = boxSize * 0.5D;
        double boxHeight = 52.0D;
        double halfHeight = boxHeight * 0.5D;

        float wr = Mth.clamp(this.windR * ambientLit, 0.0F, 1.0F);
        float wg = Mth.clamp(this.windG * ambientLit, 0.0F, 1.0F);
        float wb = Mth.clamp(this.windB * ambientLit, 0.0F, 1.0F);

        int totalRibbons = BASE_WIND_RIBBONS + Math.round(STORM_GUST_RIBBONS * deepStormFactor);

        for (int i = 0; i < totalRibbons; i++) {
            boolean isStormGust = i >= BASE_WIND_RIBBONS;
            float h1 = hashFloat(i * 17 + 1);
            float h2 = hashFloat(i * 31 + 7);
            float h3 = hashFloat(i * 47 + 13);

            double localZ = wrapCentered((h2 * boxSize) - camZ, boxSize) - halfBox;
            double worldZ = camZ + localZ;
            double localY = wrapCentered((h3 * boxHeight) - camY, boxHeight) - halfHeight;
            double worldY = camY + localY;

            double windAccel = GCGasGiantAtmosphereHandler.computeZonalWindAccel(level.dimension(), worldY, worldZ);
            double bandSign = windAccel != 0.0D ? Math.signum(windAccel) : (Math.sin(worldZ * 0.012D) >= 0.0D ? 1.0D : -1.0D);
            double speed = (isStormGust ? 28.0D : (15.0D + 17.0D * cloudIntensity)) * this.windSpeedScale * (0.75D + 0.5D * h1);

            double localX = wrapCentered((h1 * boxSize) + timeSec * speed * bandSign - camX, boxSize) - halfBox;

            float dist = Mth.sqrt((float) (localX * localX + localY * localY + localZ * localZ));
            if (dist < 3.5F || dist > 48.0F) {
                continue;
            }
            float distFade = Mth.clamp((dist - 3.5F) / 5.5F, 0.0F, 1.0F)
                    * (1.0F - Mth.clamp((dist - 32.0F) / 16.0F, 0.0F, 1.0F));
            float peakAlpha = ribbonOpacity * distFade * (0.65F + 0.35F * h2) * (isStormGust ? 0.85F : 1.0F);
            if (peakAlpha <= 0.015F) {
                continue;
            }

            float halfLen = (isStormGust ? 7.5F : (5.5F + 7.5F * cloudIntensity)) * (0.8F + 0.4F * h3);
            float halfThickY = (isStormGust ? 0.11F : 0.14F) + 0.13F * h1;
            float halfThickZ = 0.22F + 0.18F * h2;
            float rx = (float) localX;
            float ry = (float) localY;
            float rz = (float) localZ;
            float wave = Mth.sin((float) (timeSec * (isStormGust ? 4.5D : 2.2D) + i)) * (isStormGust ? 0.65F : 0.35F);
            float slantY = isStormGust ? -1.4F * (float) bandSign : 0.0F;

            addTaperedRibbon(bb, rx - halfLen, ry - slantY, rz, rx, ry + wave, rz, rx + halfLen, ry + slantY, rz, 0.0F, halfThickY, 0.0F, wr, wg, wb, peakAlpha);
            addTaperedRibbon(bb, rx - halfLen, ry - slantY, rz, rx, ry + wave, rz, rx + halfLen, ry + slantY, rz, 0.0F, 0.0F, halfThickZ, wr, wg, wb, peakAlpha * 0.85F);
            hasVerts = true;
        }

        if (hasVerts) {
            MeshData mesh = bb.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        } else {
            MeshData empty = bb.build();
            if (empty != null) {
                empty.close();
            }
        }
    }

    /**
     * Renders additive glowing 3D jagged lightning bolts with secondary branches arcing through the storm decks.
     */
    private void renderStormLightningBolts(
            double camX,
            double camY,
            double camZ,
            long gameTime,
            float partialTicks,
            float spaceFade
    ) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        boolean hasVerts = false;

        for (StormBolt bolt : this.activeBolts) {
            float flicker = bolt.computeFlicker(gameTime, partialTicks) * spaceFade;
            if (flicker <= 0.02F) {
                continue;
            }

            Random rng = new Random(bolt.seed);
            int segments = 10;
            float[] px = new float[segments + 1];
            float[] py = new float[segments + 1];
            float[] pz = new float[segments + 1];

            float curX = (float) (bolt.x - camX);
            float curZ = (float) (bolt.z - camZ);
            float startY = (float) (bolt.topY - camY);
            float endY = (float) (bolt.bottomY - camY);

            px[0] = curX;
            py[0] = startY;
            pz[0] = curZ;

            for (int s = 1; s <= segments; s++) {
                float t = (float) s / segments;
                curX += (rng.nextFloat() - 0.5F) * 14.0F;
                curZ += (rng.nextFloat() - 0.5F) * 14.0F;
                px[s] = curX;
                py[s] = Mth.lerp(t, startY, endY);
                pz[s] = curZ;
            }

            // Outer glowing plasma halo + bright white-hot inner core
            for (int s = 0; s < segments; s++) {
                addLightningSegment(bb, px[s], py[s], pz[s], px[s + 1], py[s + 1], pz[s + 1], 1.85F, this.boltR, this.boltG, this.boltB, flicker * 0.35F);
                addLightningSegment(bb, px[s], py[s], pz[s], px[s + 1], py[s + 1], pz[s + 1], 0.55F, 1.0F, 1.0F, 1.0F, flicker * 0.92F);

                // Forked secondary storm branches on mid segments
                if (s >= 2 && s <= segments - 3 && rng.nextFloat() < 0.48F) {
                    float bx0 = px[s];
                    float by0 = py[s];
                    float bz0 = pz[s];
                    float dirX = (rng.nextFloat() - 0.5F) * 20.0F;
                    float dirZ = (rng.nextFloat() - 0.5F) * 20.0F;
                    float dropY = -12.0F - rng.nextFloat() * 14.0F;
                    float bx1 = bx0 + dirX * 0.5F;
                    float by1 = by0 + dropY * 0.5F;
                    float bz1 = bz0 + dirZ * 0.5F;
                    float bx2 = bx0 + dirX + (rng.nextFloat() - 0.5F) * 8.0F;
                    float by2 = by0 + dropY;
                    float bz2 = bz0 + dirZ + (rng.nextFloat() - 0.5F) * 8.0F;

                    addLightningSegment(bb, bx0, by0, bz0, bx1, by1, bz1, 1.15F, this.boltR, this.boltG, this.boltB, flicker * 0.28F);
                    addLightningSegment(bb, bx0, by0, bz0, bx1, by1, bz1, 0.34F, 1.0F, 1.0F, 1.0F, flicker * 0.78F);
                    addLightningSegment(bb, bx1, by1, bz1, bx2, by2, bz2, 0.85F, this.boltR, this.boltG, this.boltB, flicker * 0.20F);
                    addLightningSegment(bb, bx1, by1, bz1, bx2, by2, bz2, 0.22F, 1.0F, 1.0F, 1.0F, flicker * 0.62F);
                }
            }
            hasVerts = true;
        }

        if (hasVerts) {
            MeshData mesh = bb.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        } else {
            MeshData empty = bb.build();
            if (empty != null) {
                empty.close();
            }
        }

        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
        );
    }

    private static void addLightningSegment(
            BufferBuilder bb,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float halfW,
            float r, float g, float b, float alpha
    ) {
        // Crossed X/Z ribbon quads so the lightning bolt has full thickness from all 360-degree viewing angles
        bb.addVertex(x0 - halfW, y0, z0).setColor(r, g, b, alpha);
        bb.addVertex(x0 + halfW, y0, z0).setColor(r, g, b, alpha);
        bb.addVertex(x1 + halfW, y1, z1).setColor(r, g, b, alpha);

        bb.addVertex(x0 - halfW, y0, z0).setColor(r, g, b, alpha);
        bb.addVertex(x1 + halfW, y1, z1).setColor(r, g, b, alpha);
        bb.addVertex(x1 - halfW, y1, z1).setColor(r, g, b, alpha);

        bb.addVertex(x0, y0, z0 - halfW).setColor(r, g, b, alpha);
        bb.addVertex(x0, y0, z0 + halfW).setColor(r, g, b, alpha);
        bb.addVertex(x1, y1, z1 + halfW).setColor(r, g, b, alpha);

        bb.addVertex(x0, y0, z0 - halfW).setColor(r, g, b, alpha);
        bb.addVertex(x1, y1, z1 + halfW).setColor(r, g, b, alpha);
        bb.addVertex(x1, y1, z1 - halfW).setColor(r, g, b, alpha);
    }

    private static void addTaperedRibbon(
            BufferBuilder bb,
            float x0, float y0, float z0,
            float xm, float ym, float zm,
            float x1, float y1, float z1,
            float nx, float ny, float nz,
            float r, float g, float b, float midAlpha
    ) {
        bb.addVertex(x0, y0, z0).setColor(r, g, b, 0.0F);
        bb.addVertex(xm + nx, ym + ny, zm + nz).setColor(r, g, b, midAlpha);
        bb.addVertex(xm - nx, ym - ny, zm - nz).setColor(r, g, b, midAlpha);

        bb.addVertex(xm + nx, ym + ny, zm + nz).setColor(r, g, b, midAlpha);
        bb.addVertex(x1, y1, z1).setColor(r, g, b, 0.0F);
        bb.addVertex(xm - nx, ym - ny, zm - nz).setColor(r, g, b, midAlpha);
    }

    private static float sampleCloudDensity(double x, double z, int deckIdx) {
        double sx = x * 0.011D + deckIdx * 3.7D;
        double sz = z * 0.026D - deckIdx * 2.1D;
        double warp = Math.sin(sx * 0.7D + sz * 0.45D) * 1.35D;
        double band = Math.sin(sz + warp) * 0.55D;
        double billow = Math.cos(sx * 1.45D - sz * 0.85D) * 0.30D;
        double fine = Math.sin(sx * 2.9D + sz * 2.1D) * 0.15D;
        return (float) (0.52D + 0.48D * (band + billow + fine));
    }

    private static float sampleWaveY(double x, double z, int deckIdx) {
        return Mth.sin((float) (x * 0.022D + deckIdx)) * 1.4F + Mth.cos((float) (z * 0.035D - deckIdx)) * 1.1F;
    }

    private static double wrapCentered(double v, double period) {
        double m = v % period;
        return m < 0.0D ? m + period : m;
    }

    private static float hashFloat(int n) {
        n = (n << 13) ^ n;
        int t = (n * (n * n * 15731 + 789221) + 1376312589) & 0x7FFFFFFF;
        return (float) t / (float) 0x7FFFFFFF;
    }

    private record CloudDeck(
            double altitudeY,
            float thickness,
            float baseAlpha,
            float driftSpeed,
            float cellSize,
            float brightness,
            float threshold
    ) {
    }

    private record StormBolt(double x, double topY, double bottomY, double z, long spawnTick, long expireTick, long seed) {
        float computeFlicker(long gameTime, float partialTicks) {
            float age = (gameTime - this.spawnTick) + partialTicks;
            float life = Math.max(1.0F, (float) (this.expireTick - this.spawnTick));
            float norm = Mth.clamp(age / life, 0.0F, 1.0F);
            // Realistic double-strike lightning flicker envelope
            float envelope = (1.0F - norm) * (1.0F - norm);
            float pulse = 0.65F + 0.35F * Math.abs(Mth.sin(age * 2.4F + (this.seed & 15)));
            return Mth.clamp(envelope * pulse * 1.35F, 0.0F, 1.0F);
        }
    }
}
