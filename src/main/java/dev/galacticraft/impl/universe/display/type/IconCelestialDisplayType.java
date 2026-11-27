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

package dev.galacticraft.impl.universe.display.type;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import dev.galacticraft.api.universe.display.CelestialDisplayType;
import dev.galacticraft.impl.universe.display.config.IconCelestialDisplayConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.util.Graphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL32C;

public class IconCelestialDisplayType extends CelestialDisplayType<IconCelestialDisplayConfig> {
    public static final IconCelestialDisplayType INSTANCE = new IconCelestialDisplayType(IconCelestialDisplayConfig.CODEC);

    protected IconCelestialDisplayType(Codec<IconCelestialDisplayConfig> codec) {
        super(codec);
    }

    @Override
    public Vector4f render(GuiGraphics graphics, int size, double mouseX, double mouseY, float delta, IconCelestialDisplayConfig config) {
        float realSize = config.scale() * size;
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(config.texture());
        texture.bind();
        int width = GlStateManager._getTexLevelParameter(GL32C.GL_TEXTURE_2D, 0, GL32C.GL_TEXTURE_WIDTH);
        int height = GlStateManager._getTexLevelParameter(GL32C.GL_TEXTURE_2D, 0, GL32C.GL_TEXTURE_HEIGHT);
        int uWidth = config.width();
        int vHeight = config.height();
        if (config.u() == 0 && config.v() == 0 && width > 0 && height > 0) {
            uWidth = width;
            vHeight = height;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float rotation = 0.0F;
        if (this.getClass() == IconCelestialDisplayType.class && Minecraft.getInstance().level != null) {
            float time = Minecraft.getInstance().level.getGameTime() + delta;
            int hash = Math.abs(config.texture().hashCode());
            float phase = (hash % 360);
            if (config.decoration().isPresent()) {
                // Gentle axial sway for ringed planets so rings stay cleanly aligned
                rotation = Mth.sin(time * 0.015F + phase * Mth.DEG_TO_RAD) * 6.0F;
            } else {
                // Astronomically proportional slow axial rotation (deg/tick)
                float speed = getProportionalSpinSpeed(config.texture().getPath());
                rotation = (time * speed + phase) % 360.0F;
            }
        }

        graphics.pose().pushPose();
        if (rotation != 0.0F) {
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        }

        Graphics.blitCentered(graphics.pose().last().pose(), 0.0f, 0.0f, realSize, realSize, 0.0f, config.u(), config.v(), uWidth, vHeight, width, height, config.texture());

        config.decoration().ifPresent(decoration -> {
            float decoSize = realSize / 3.0F;
            float decoW = Math.abs(decoration.xScale()) * decoSize;
            float decoH = Math.abs(decoration.yScale()) * decoSize;
            Graphics.blitCentered(graphics.pose().last().pose(), 0.0f, 0.0f, decoW, decoH, 0.0f, decoration.u(), decoration.v(), decoration.width(), decoration.height(), 32, 32, decoration.texture());
        });

        graphics.pose().popPose();
        RenderSystem.disableBlend();

        return new Vector4f(config.scale() * -size, config.scale() * -size, (config.scale() * size) * 2, (config.scale() * size) * 2);
    }

    @Override
    public ResourceLocation rocketOverlay(IconCelestialDisplayConfig config) {
        return config.rocketOverlay().orElse(Constant.CelestialOverlay.EARTH);
    }

    private static float getProportionalSpinSpeed(String texturePath) {
        // Real-life inspired relative rotation rates (Earth = 0.22 deg/tick; moons tidally locked proportional to orbit)
        if (texturePath.endsWith("/sol.png") || texturePath.endsWith("/sun.png")) return 0.08F;
        if (texturePath.endsWith("/jupiter.png")) return 0.36F;
        if (texturePath.endsWith("/neptune.png")) return 0.28F;
        if (texturePath.endsWith("/earth.png")) return 0.22F;
        if (texturePath.endsWith("/mars.png")) return 0.21F;
        if (texturePath.endsWith("/mercury.png")) return 0.06F;
        if (texturePath.endsWith("/venus.png")) return -0.04F; // retrograde slow spin
        // Moons: tidally locked proportional to real orbital periods (days)
        if (texturePath.endsWith("/phobos.png")) return 0.45F;     // 0.32d
        if (texturePath.endsWith("/deimos.png")) return 0.24F;     // 1.26d
        if (texturePath.endsWith("/enceladus.png")) return 0.22F;  // 1.37d
        if (texturePath.endsWith("/io.png")) return 0.20F;         // 1.77d
        if (texturePath.endsWith("/europa.png")) return 0.14F;     // 3.55d
        if (texturePath.endsWith("/rhea.png")) return 0.12F;       // 4.52d
        if (texturePath.endsWith("/triton.png")) return -0.11F;    // -5.88d retrograde
        if (texturePath.endsWith("/ganymede.png")) return 0.10F;   // 7.15d
        if (texturePath.endsWith("/titania.png")) return 0.09F;    // 8.71d
        if (texturePath.endsWith("/oberon.png")) return 0.075F;    // 13.46d
        if (texturePath.endsWith("/titan.png")) return 0.07F;      // 15.95d
        if (texturePath.endsWith("/callisto.png")) return 0.068F;  // 16.69d
        if (texturePath.endsWith("/moon.png")) return 0.055F;      // 27.32d
        if (texturePath.endsWith("/iapetus.png")) return 0.035F;   // 79.32d
        return 0.14F;
    }
}
