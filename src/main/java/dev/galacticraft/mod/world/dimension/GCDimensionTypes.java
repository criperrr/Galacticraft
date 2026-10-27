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

package dev.galacticraft.mod.world.dimension;

import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.tag.GCBlockTags;
import dev.galacticraft.mod.tag.GCDimensionTypeTags;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

public class GCDimensionTypes {
    public static final ResourceKey<DimensionType> MOON = key("moon");
    public static final ResourceKey<DimensionType> MARS = key("mars");
    public static final ResourceKey<DimensionType> VENUS = key("venus");
    public static final ResourceKey<DimensionType> ASTEROID = key("asteroid");
    public static final ResourceKey<DimensionType> MERCURY = key("mercury");
    public static final ResourceKey<DimensionType> JUPITER = key("jupiter");
    public static final ResourceKey<DimensionType> SATURN = key("saturn");
    public static final ResourceKey<DimensionType> URANUS = key("uranus");
    public static final ResourceKey<DimensionType> NEPTUNE = key("neptune");

    public static void bootstrapRegistries(BootstrapContext<DimensionType> context) {
        context.register(MOON, new DimensionType(
                OptionalLong.empty(), // fixedTime
                true, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks (doesn't explode, we can cancel working later)
                false, // respawnAnchorWorks (doesn't explode, we can cancel working later)
                -32, // minY
                384, // height
                384, // logicalHeight
                GCBlockTags.INFINIBURN_MOON, // infiniburn
                Constant.id("moon"), // fixme: GCDimensionEffects somehow seems to load client classes
                0.1f, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));

        context.register(MARS, new DimensionType(
                OptionalLong.empty(), // fixedTime
                true, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks (doesn't explode, we can cancel working later)
                false, // respawnAnchorWorks
                -32, // minY
                384, // height
                384, // logicalHeight
                GCBlockTags.INFINIBURN_MARS, // infiniburn
                Constant.id("mars"), // effectsLocation
                0.1F, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));

        context.register(VENUS, new DimensionType(
                OptionalLong.empty(), // fixedTime
                true, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks (doesn't explode, we can cancel working later)
                false, // respawnAnchorWorks (doesn't explode, we can cancel working later)
                -32, // minY
                384, // height
                384, // logicalHeight
                GCBlockTags.INFINIBURN_VENUS, // infiniburn
                Constant.id("venus"), // effectsLocation // fixme
                0.1F, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));
        context.register(ASTEROID, new DimensionType(
                OptionalLong.of(6000L), // fixedTime: perpetual space
                false, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0,  // coordinateScale
                false, // bedWorks
                false, // respawnAnchorWorks
                -64, // minY
                384, // height
                384, // logicalHeight
                GCBlockTags.INFINIBURN_ASTEROID, // infiniburn
                Constant.id("asteroid"), // effectsLocation // fixme
                0.05F, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));

        context.register(MERCURY, new DimensionType(
                OptionalLong.empty(), // fixedTime (has long day/night cycle)
                true, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks
                false, // respawnAnchorWorks
                -64, // minY
                384, // height (total height from -64 to 320 = 384)
                384, // logicalHeight
                GCBlockTags.INFINIBURN_MERCURY, // infiniburn
                Constant.id("mercury"), // effectsLocation
                0.05F, // ambientLight (dark space sky)
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0) // No monster spawning as requested!
        ));

        context.register(JUPITER, new DimensionType(
                OptionalLong.empty(), // fixedTime (rapid 9.925h rotation)
                true, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks
                false, // respawnAnchorWorks
                -64, // minY
                384, // height
                384, // logicalHeight
                GCBlockTags.INFINIBURN_JUPITER, // infiniburn
                Constant.id("jupiter"), // effectsLocation
                0.08F, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));

        context.register(SATURN, new DimensionType(
                OptionalLong.empty(), // fixedTime (rapid 10.656h rotation)
                true, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks
                false, // respawnAnchorWorks
                -64, // minY
                384, // height
                384, // logicalHeight
                GCBlockTags.INFINIBURN_SATURN, // infiniburn
                Constant.id("saturn"), // effectsLocation
                0.08F, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));

        context.register(URANUS, new DimensionType(
                OptionalLong.empty(), // fixedTime (17.24h rotation)
                true, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks
                false, // respawnAnchorWorks
                -64, // minY
                384, // height
                384, // logicalHeight
                GCBlockTags.INFINIBURN_URANUS, // infiniburn
                Constant.id("uranus"), // effectsLocation
                0.06F, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));

        context.register(NEPTUNE, new DimensionType(
                OptionalLong.empty(), // fixedTime (16.11h rotation)
                true, // hasSkyLight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0, // coordinateScale
                true, // bedWorks
                false, // respawnAnchorWorks
                -64, // minY
                384, // height
                384, // logicalHeight
                GCBlockTags.INFINIBURN_NEPTUNE, // infiniburn
                Constant.id("neptune"), // effectsLocation
                0.05F, // ambientLight
                new DimensionType.MonsterSettings(false, false, UniformInt.of(0, 0), 0)
        ));
    }

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull ResourceKey<DimensionType> key(@NotNull String id) {
        return Constant.key(Registries.DIMENSION_TYPE, id);
    }

    public static boolean canMobsSpawnNaturally(@NotNull LevelAccessor level) {
        if (level instanceof Level l) {
            return canMobsSpawnNaturally(l);
        }
        return false;
    }

    public static boolean canMobsSpawnNaturally(@NotNull Level level) {
        if (level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER || level.dimension() == Level.END) {
            return true;
        }
        if (level.galacticraft$hasDimensionTypeTag(GCDimensionTypeTags.ALLOWS_MOB_SPAWNING)) {
            return true;
        }
        Holder<CelestialBody<?, ?>> holder = level.galacticraft$getCelestialBody();
        if (holder != null) {
            return holder.value().atmosphere().breathable();
        }
        return true;
    }
}
