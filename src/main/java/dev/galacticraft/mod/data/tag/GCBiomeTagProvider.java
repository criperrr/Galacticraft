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

package dev.galacticraft.mod.data.tag;

import dev.galacticraft.mod.tag.GCBiomeTags;
import dev.galacticraft.mod.world.biome.GCBiomes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import java.util.concurrent.CompletableFuture;

public class GCBiomeTagProvider extends FabricTagProvider<Biome> {
    public GCBiomeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> future) {
        super(output, Registries.BIOME, future);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.tag(GCBiomeTags.MOON)
                .add(GCBiomes.Moon.COMET_TUNDRA)
                .add(GCBiomes.Moon.BASALTIC_MARE)
                .add(GCBiomes.Moon.LUNAR_HIGHLANDS)
                .add(GCBiomes.Moon.LUNAR_LOWLANDS)
                .add(GCBiomes.Moon.OLIVINE_SPIKES);

        this.tag(GCBiomeTags.MARS)
                .add(GCBiomes.Mars.MARS_FLAT)
                .add(GCBiomes.Mars.MARS_CANYON)
                .add(GCBiomes.Mars.MARS_HIGHLANDS);

        this.tag(GCBiomeTags.MERCURY)
                .add(GCBiomes.Mercury.CALORIS_BASIN)
                .add(GCBiomes.Mercury.INTER_CRATER_PLAINS)
                .add(GCBiomes.Mercury.SMOOTH_LAVA_PLAINS)
                .add(GCBiomes.Mercury.CHAOTIC_TERRAIN)
                .add(GCBiomes.Mercury.POLAR_DARK_CRATERS)
                .add(GCBiomes.Mercury.HOLLOW_LABYRINTH)
                .add(GCBiomes.Mercury.MEGABRECCIA_CAVERNS)
                .add(GCBiomes.Mercury.COLOSSAL_LAVA_TUBES)
                .add(GCBiomes.Mercury.MAGMA_DEPTHS)
                .add(GCBiomes.Mercury.TECTONIC_CHASMS);

        this.tag(GCBiomeTags.VENUS)
                .add(GCBiomes.Venus.VENUS_VALLEY)
                .add(GCBiomes.Venus.VENUS_FLAT)
                .add(GCBiomes.Venus.VENUS_MOUNTAIN);

        this.tag(GCBiomeTags.ASTEROID)
                .add(GCBiomes.Asteroid.ASTEROID_FIELD);

        this.tag(GCBiomeTags.JUPITER)
                .add(GCBiomes.Jupiter.AMMONIA_PLAINS)
                .add(GCBiomes.Jupiter.GREAT_RED_SPOT)
                .add(GCBiomes.Jupiter.SULFURIC_BELTS)
                .add(GCBiomes.Jupiter.METALLIC_RIDGES)
                .add(GCBiomes.Jupiter.POLAR_AURORA_CRATERS)
                .add(GCBiomes.Jupiter.METALLIC_HYDROGEN_CAVERNS)
                .add(GCBiomes.Jupiter.CYCLONIC_RIFTS)
                .add(GCBiomes.Jupiter.CORE_MAGMA_DEPTHS);

        this.tag(GCBiomeTags.SATURN)
                .add(GCBiomes.Saturn.GOLDEN_HAZE_PLAINS)
                .add(GCBiomes.Saturn.HYDROCARBON_SEAS)
                .add(GCBiomes.Saturn.RING_SHADOW_HIGHLANDS)
                .add(GCBiomes.Saturn.AMMONIA_STORM_BANDS)
                .add(GCBiomes.Saturn.HEXAGON_POLAR_VORTEX)
                .add(GCBiomes.Saturn.HELIUM_RAIN_CAVERNS)
                .add(GCBiomes.Saturn.CRYSTALLINE_RING_TUBES)
                .add(GCBiomes.Saturn.MANTLE_CORE_ABYSS);

        this.tag(GCBiomeTags.URANUS)
                .add(GCBiomes.Uranus.FROZEN_METHANE_PLAINS)
                .add(GCBiomes.Uranus.MIRANDA_VERONA_CLIFFS)
                .add(GCBiomes.Uranus.CRYO_GEYSER_BASIN)
                .add(GCBiomes.Uranus.TILTED_POLAR_CAPS)
                .add(GCBiomes.Uranus.RING_DEBRIS_BADLANDS)
                .add(GCBiomes.Uranus.DIAMOND_MANTLE_CAVERNS)
                .add(GCBiomes.Uranus.GLACIAL_FAULT_CHASMS)
                .add(GCBiomes.Uranus.SUPERIONIC_ICE_DEPTHS);

        this.tag(GCBiomeTags.NEPTUNE)
                .add(GCBiomes.Neptune.AZURE_METHANE_PLAINS)
                .add(GCBiomes.Neptune.GREAT_DARK_SPOT)
                .add(GCBiomes.Neptune.TRITON_NITROGEN_GEYSERS)
                .add(GCBiomes.Neptune.SUPERSONIC_WIND_RIDGES)
                .add(GCBiomes.Neptune.CRYOGENIC_POLAR_ABYSS)
                .add(GCBiomes.Neptune.DIAMOND_RAIN_CAVERNS)
                .add(GCBiomes.Neptune.CRYO_MAGMA_TUBES)
                .add(GCBiomes.Neptune.ABYSSAL_MANTLE_DEPTHS);

        this.tag(GCBiomeTags.PHOBOS)
                .add(GCBiomes.Phobos.STICKNEY_BASIN)
                .add(GCBiomes.Phobos.TIDAL_GROOVES)
                .add(GCBiomes.Phobos.REGOLITH_HIGHLANDS)
                .add(GCBiomes.Phobos.FRACTURE_CAVERNS);

        this.tag(GCBiomeTags.DEIMOS)
                .add(GCBiomes.Deimos.SMOOTH_PLAINS)
                .add(GCBiomes.Deimos.CRATER_SLOPES)
                .add(GCBiomes.Deimos.CORE_CAVERNS);

        this.tag(GCBiomeTags.IO)
                .add(GCBiomes.Io.SULFUR_PLAINS)
                .add(GCBiomes.Io.VOLCANIC_CALDERA)
                .add(GCBiomes.Io.SILICATE_FLOWS)
                .add(GCBiomes.Io.MAGMA_TUBES);

        this.tag(GCBiomeTags.EUROPA)
                .add(GCBiomes.Europa.ICE_PLAINS)
                .add(GCBiomes.Europa.LINEAE_RIDGES)
                .add(GCBiomes.Europa.CHAOS_TERRAIN)
                .add(GCBiomes.Europa.SUBSURFACE_OCEAN);

        this.tag(GCBiomeTags.GANYMEDE)
                .add(GCBiomes.Ganymede.SULCI_RIDGES)
                .add(GCBiomes.Ganymede.DARK_REGIO)
                .add(GCBiomes.Ganymede.POLAR_FROST)
                .add(GCBiomes.Ganymede.ICE_CAVERNS);

        this.tag(GCBiomeTags.CALLISTO)
                .add(GCBiomes.Callisto.CRATERED_PLAINS)
                .add(GCBiomes.Callisto.VALHALLA_RINGS)
                .add(GCBiomes.Callisto.ICE_PEAKS)
                .add(GCBiomes.Callisto.ANCIENT_CAVERNS);

        this.tag(GCBiomeTags.MOON_RUINS_HAS_STRUCTURE)
                .add(GCBiomes.Moon.BASALTIC_MARE);
        this.tag(GCBiomeTags.MOON_BOSS_HAS_STRUCTURE)
                .addTag(GCBiomeTags.MOON);

        this.tag(BiomeTags.WITHOUT_PATROL_SPAWNS)
                .addTag(GCBiomeTags.MOON)
                .addTag(GCBiomeTags.MARS)
                .addTag(GCBiomeTags.MERCURY)
                .addTag(GCBiomeTags.VENUS)
                .addTag(GCBiomeTags.ASTEROID)
                .addTag(GCBiomeTags.JUPITER)
                .addTag(GCBiomeTags.SATURN)
                .addTag(GCBiomeTags.URANUS)
                .addTag(GCBiomeTags.NEPTUNE)
                .addTag(GCBiomeTags.PHOBOS)
                .addTag(GCBiomeTags.DEIMOS)
                .addTag(GCBiomeTags.IO)
                .addTag(GCBiomeTags.EUROPA)
                .addTag(GCBiomeTags.GANYMEDE)
                .addTag(GCBiomeTags.CALLISTO)
                .add(GCBiomes.SPACE);

        this.tag(BiomeTags.WITHOUT_WANDERING_TRADER_SPAWNS)
                .addTag(GCBiomeTags.MOON)
                .addTag(GCBiomeTags.MARS)
                .addTag(GCBiomeTags.MERCURY)
                .addTag(GCBiomeTags.VENUS)
                .addTag(GCBiomeTags.ASTEROID)
                .addTag(GCBiomeTags.JUPITER)
                .addTag(GCBiomeTags.SATURN)
                .addTag(GCBiomeTags.URANUS)
                .addTag(GCBiomeTags.NEPTUNE)
                .addTag(GCBiomeTags.PHOBOS)
                .addTag(GCBiomeTags.DEIMOS)
                .addTag(GCBiomeTags.IO)
                .addTag(GCBiomeTags.EUROPA)
                .addTag(GCBiomeTags.GANYMEDE)
                .addTag(GCBiomeTags.CALLISTO)
                .add(GCBiomes.SPACE);

        this.tag(BiomeTags.WITHOUT_ZOMBIE_SIEGES)
                .addTag(GCBiomeTags.MOON)
                .addTag(GCBiomeTags.MARS)
                .addTag(GCBiomeTags.MERCURY)
                .addTag(GCBiomeTags.VENUS)
                .addTag(GCBiomeTags.ASTEROID)
                .addTag(GCBiomeTags.JUPITER)
                .addTag(GCBiomeTags.SATURN)
                .addTag(GCBiomeTags.URANUS)
                .addTag(GCBiomeTags.NEPTUNE)
                .addTag(GCBiomeTags.PHOBOS)
                .addTag(GCBiomeTags.DEIMOS)
                .addTag(GCBiomeTags.IO)
                .addTag(GCBiomeTags.EUROPA)
                .addTag(GCBiomeTags.GANYMEDE)
                .addTag(GCBiomeTags.CALLISTO)
                .add(GCBiomes.SPACE);
    }
}