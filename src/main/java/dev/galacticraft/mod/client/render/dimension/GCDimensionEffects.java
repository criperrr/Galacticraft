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

import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.world.dimension.GCDimensions;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.resources.ResourceLocation;

public class GCDimensionEffects {
    public static final ResourceLocation MOON = Constant.id("moon");
    public static final ResourceLocation MARS = Constant.id("mars");
    public static final ResourceLocation VENUS = Constant.id("venus");
    public static final ResourceLocation ASTEROID = Constant.id("asteroid");
    public static final ResourceLocation SATELLITE = Constant.id("satellite");
    public static final ResourceLocation MERCURY = Constant.id("mercury");
    public static final ResourceLocation JUPITER = Constant.id("jupiter");
    public static final ResourceLocation SATURN = Constant.id("saturn");
    public static final ResourceLocation URANUS = Constant.id("uranus");
    public static final ResourceLocation NEPTUNE = Constant.id("neptune");
    public static final ResourceLocation TITAN = Constant.id("titan");
    public static final ResourceLocation ENCELADUS = Constant.id("enceladus");
    public static final ResourceLocation TITANIA = Constant.id("titania");
    public static final ResourceLocation OBERON = Constant.id("oberon");
    public static final ResourceLocation TRITON = Constant.id("triton");

    public static void register() {
        DimensionRenderingRegistry.registerDimensionEffects(MOON, MoonDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.MOON, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.MOON, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.MOON, MoonSkyRenderer.INSTANCE);

        DimensionRenderingRegistry.registerDimensionEffects(MARS, MarsDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.MARS, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.MARS, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.MARS, MarsSkyRenderer.INSTANCE);

        DimensionRenderingRegistry.registerDimensionEffects(VENUS, VenusDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.VENUS, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.VENUS, VenusWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.VENUS, VenusSkyRenderer.INSTANCE);

        DimensionRenderingRegistry.registerDimensionEffects(ASTEROID, AsteroidDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.ASTEROID, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.ASTEROID, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.ASTEROID, AsteroidSkyRenderer.INSTANCE);

        DimensionRenderingRegistry.registerDimensionEffects(SATELLITE, AsteroidDimensionEffects.INSTANCE);

        DimensionRenderingRegistry.registerDimensionEffects(MERCURY, SpaceDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.MERCURY, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.MERCURY, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.MERCURY, MercurySkyRenderer.INSTANCE);

        DimensionRenderingRegistry.registerDimensionEffects(JUPITER, OuterPlanetDimensionEffects.JUPITER);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.JUPITER, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.JUPITER, GasGiantAtmosphereRenderer.JUPITER);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.JUPITER, OuterPlanetSkyRenderer.JUPITER);

        DimensionRenderingRegistry.registerDimensionEffects(SATURN, OuterPlanetDimensionEffects.SATURN);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.SATURN, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.SATURN, GasGiantAtmosphereRenderer.SATURN);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.SATURN, OuterPlanetSkyRenderer.SATURN);

        DimensionRenderingRegistry.registerDimensionEffects(URANUS, OuterPlanetDimensionEffects.URANUS);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.URANUS, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.URANUS, GasGiantAtmosphereRenderer.URANUS);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.URANUS, OuterPlanetSkyRenderer.URANUS);

        DimensionRenderingRegistry.registerDimensionEffects(NEPTUNE, OuterPlanetDimensionEffects.NEPTUNE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.NEPTUNE, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.NEPTUNE, GasGiantAtmosphereRenderer.NEPTUNE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.NEPTUNE, OuterPlanetSkyRenderer.NEPTUNE);

        DimensionRenderingRegistry.registerDimensionEffects(TITAN, TitanDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.TITAN, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.TITAN, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.TITAN, MoonParentSkyRenderer.TITAN);

        DimensionRenderingRegistry.registerDimensionEffects(ENCELADUS, SpaceDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.ENCELADUS, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.ENCELADUS, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.ENCELADUS, MoonParentSkyRenderer.ENCELADUS);

        DimensionRenderingRegistry.registerDimensionEffects(TITANIA, SpaceDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.TITANIA, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.TITANIA, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.TITANIA, MoonParentSkyRenderer.TITANIA);

        DimensionRenderingRegistry.registerDimensionEffects(OBERON, SpaceDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.OBERON, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.OBERON, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.OBERON, MoonParentSkyRenderer.OBERON);

        DimensionRenderingRegistry.registerDimensionEffects(TRITON, SpaceDimensionEffects.INSTANCE);
        DimensionRenderingRegistry.registerCloudRenderer(GCDimensions.TRITON, EmptyCloudRenderer.INSTANCE);
        DimensionRenderingRegistry.registerWeatherRenderer(GCDimensions.TRITON, EmptyWeatherRenderer.INSTANCE);
        DimensionRenderingRegistry.registerSkyRenderer(GCDimensions.TRITON, MoonParentSkyRenderer.TRITON);
    }
}
