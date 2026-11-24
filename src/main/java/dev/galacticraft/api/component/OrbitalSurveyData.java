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

package dev.galacticraft.api.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Multispectral orbital cartography payload stored on {@code ORBITAL_SURVEY_MAP} items,
 * produced by {@code science_orbital_camera_multispectral} during orbital sweeps.
 * Records real ore hotspot coordinates, detected {@code CRASHED_PROBE} coordinates, and biome data.
 */
public record OrbitalSurveyData(
        @NotNull ResourceLocation planetId,
        @NotNull String planetName,
        int oreHotspotX,
        int oreHotspotY,
        int oreHotspotZ,
        @NotNull String oreLabel,
        int probeX,
        int probeZ,
        boolean probeDetected,
        @NotNull String biomeName
) {
    public static final Codec<OrbitalSurveyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("planet_id").forGetter(OrbitalSurveyData::planetId),
            Codec.STRING.fieldOf("planet_name").forGetter(OrbitalSurveyData::planetName),
            Codec.INT.fieldOf("ore_x").forGetter(OrbitalSurveyData::oreHotspotX),
            Codec.INT.fieldOf("ore_y").forGetter(OrbitalSurveyData::oreHotspotY),
            Codec.INT.fieldOf("ore_z").forGetter(OrbitalSurveyData::oreHotspotZ),
            Codec.STRING.fieldOf("ore_label").forGetter(OrbitalSurveyData::oreLabel),
            Codec.INT.optionalFieldOf("probe_x", 0).forGetter(OrbitalSurveyData::probeX),
            Codec.INT.optionalFieldOf("probe_z", 0).forGetter(OrbitalSurveyData::probeZ),
            Codec.BOOL.optionalFieldOf("probe_detected", false).forGetter(OrbitalSurveyData::probeDetected),
            Codec.STRING.optionalFieldOf("biome_name", "Unknown").forGetter(OrbitalSurveyData::biomeName)
    ).apply(instance, OrbitalSurveyData::new));

    public static final StreamCodec<ByteBuf, OrbitalSurveyData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull OrbitalSurveyData decode(@NotNull ByteBuf buf) {
            ResourceLocation planetId = ResourceLocation.STREAM_CODEC.decode(buf);
            String planetName = ByteBufCodecs.STRING_UTF8.decode(buf);
            int ox = ByteBufCodecs.VAR_INT.decode(buf);
            int oy = ByteBufCodecs.VAR_INT.decode(buf);
            int oz = ByteBufCodecs.VAR_INT.decode(buf);
            String oreLabel = ByteBufCodecs.STRING_UTF8.decode(buf);
            int px = ByteBufCodecs.VAR_INT.decode(buf);
            int pz = ByteBufCodecs.VAR_INT.decode(buf);
            boolean probeDetected = ByteBufCodecs.BOOL.decode(buf);
            String biomeName = ByteBufCodecs.STRING_UTF8.decode(buf);
            return new OrbitalSurveyData(planetId, planetName, ox, oy, oz, oreLabel, px, pz, probeDetected, biomeName);
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull OrbitalSurveyData value) {
            ResourceLocation.STREAM_CODEC.encode(buf, value.planetId());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.planetName());
            ByteBufCodecs.VAR_INT.encode(buf, value.oreHotspotX());
            ByteBufCodecs.VAR_INT.encode(buf, value.oreHotspotY());
            ByteBufCodecs.VAR_INT.encode(buf, value.oreHotspotZ());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.oreLabel());
            ByteBufCodecs.VAR_INT.encode(buf, value.probeX());
            ByteBufCodecs.VAR_INT.encode(buf, value.probeZ());
            ByteBufCodecs.BOOL.encode(buf, value.probeDetected());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.biomeName());
        }
    };

    public OrbitalSurveyData {
        Objects.requireNonNull(planetId, "planetId");
        Objects.requireNonNull(planetName, "planetName");
        Objects.requireNonNull(oreLabel, "oreLabel");
        biomeName = biomeName == null ? "Unknown" : biomeName;
    }
}
