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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import dev.galacticraft.mod.client.render.astro.AstroSkyRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Authoritative space sky renderer for Mercury, featuring a massive blazing orbital Sun,
 * dense 24/7 star field in deep space vacuum, and Mercury planet body rendered underneath
 * the player at high altitudes.
 */
public class MercurySkyRenderer extends SpaceSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    public static final MercurySkyRenderer INSTANCE = new MercurySkyRenderer();

    private final float sunSize = 35.0F;

    public MercurySkyRenderer() {
    }

    @Override
    public void render(WorldRenderContext context) {
        RenderSystem.disableBlend();
        RenderSystem.depthMask(false);

        float partialTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
        PoseStack matrices = new PoseStack();
        matrices.mulPose(context.positionMatrix());
        float celestialAngle = context.world().getTimeOfDay(partialTicks);

        // 1 & 2. Realistic AstroCraft Mercury sky (blazing close Sun, Venus, Earth, Milky Way & stars)
        if (AstroSkyEngine.isInitialized()) {
            context.profiler().push("astro_mercury_sky");
            AstroSkyRenderer.renderSpaceDimensionSky(matrices, context.projectionMatrix(), 1.0F, false);
            context.profiler().pop();
        } else {
            context.profiler().push("stars");
            matrices.pushPose();
            StarManager.rotateForCelestialAngle(matrices, celestialAngle);
            float starBrightness = Mth.clamp(1.0F - (Mth.cos(celestialAngle * (float) (Math.PI * 2)) * 2.0F + 0.25F), 0.0F, 1.0F);
            starBrightness = starBrightness * starBrightness * 0.5F + 0.3F;
            this.starManager.render(matrices, context.projectionMatrix(), context.world(), partialTicks, starBrightness);
            matrices.popPose();
            context.profiler().pop();
        }

        // 3. Mercury Planet Below Player (Pixel art rendered below at high altitudes)
        var player = Minecraft.getInstance().player;
        double playerY = player != null ? (player.getVehicle() != null ? player.getVehicle().getY() : player.getY()) : 64.0D;
        PlanetUnderPlayerRenderer.renderPlanetBelow(
                matrices,
                context.world(),
                Constant.CelestialBody.MERCURY,
                null,
                playerY,
                partialTicks,
                Constant.ATMOSPHERE_EXIT_START,
                1.0F,
                0.0F,
                0.0F,
                0.0F
        );

        RenderSystem.depthMask(true);
    }
}
