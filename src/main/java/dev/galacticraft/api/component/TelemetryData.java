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
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable telemetry payload recorded onto {@code RECORDED_DATA_DRIVE} items by onboard
 * {@code science_*} modular rocket instruments during launch, orbital scans, planetary entry,
 * radiation/magnetosphere sweeps, autonomous ISRU sampling, 0G orbital lab runs, and
 * catastrophic thermal/structural failures ("learn from disaster" +200% bonus).
 */
public record TelemetryData(
        @NotNull ResourceLocation planetId,
        @NotNull String planetName,
        @NotNull EventType eventType,
        @NotNull String scienceModuleId,
        @NotNull String primaryDomainId,
        int sciencePoints,
        int domainPoints,
        @NotNull String gasCompositionSummary,
        @NotNull String diagnosticNote,
        int qualityPct
) {
    public enum EventType implements StringRepresentable {
        ASCENT_MAX_Q("ascent_max_q", "Ascent / Max-Q"),
        ORBITAL_SCAN("orbital_scan", "Orbital Scan"),
        RADIATION_BELT("radiation_belt", "Radiation Belt"),
        MAGNETOSPHERE("magnetosphere", "Magnetosphere Sweep"),
        THERMAL_FAILURE("thermal_failure", "Thermal/Structural Failure (+200%)"),
        ATMOSPHERE_ENTRY("atmosphere_entry", "Atmospheric Entry"),
        SURFACE_ISRU_SAMPLE("surface_isru_sample", "Autonomous ISRU Sampling"),
        ZERO_G_ORBITAL_LAB("zero_g_orbital_lab", "0G Orbital Lab Synthesis");

        public static final Codec<EventType> CODEC = StringRepresentable.fromEnum(EventType::values);
        public static final StreamCodec<ByteBuf, EventType> STREAM_CODEC = ByteBufCodecs.idMapper(
                i -> values()[Math.floorMod(i, values().length)],
                EventType::ordinal
        );

        private final String id;
        private final String displayName;

        EventType(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public @NotNull String id() {
            return this.id;
        }

        public @NotNull String displayName() {
            return this.displayName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.id;
        }
    }

    public static final Codec<TelemetryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("planet_id").forGetter(TelemetryData::planetId),
            Codec.STRING.fieldOf("planet_name").forGetter(TelemetryData::planetName),
            EventType.CODEC.fieldOf("event_type").forGetter(TelemetryData::eventType),
            Codec.STRING.fieldOf("science_module_id").forGetter(TelemetryData::scienceModuleId),
            Codec.STRING.fieldOf("primary_domain_id").forGetter(TelemetryData::primaryDomainId),
            Codec.INT.fieldOf("science_points").forGetter(TelemetryData::sciencePoints),
            Codec.INT.fieldOf("domain_points").forGetter(TelemetryData::domainPoints),
            Codec.STRING.optionalFieldOf("gas_composition", "").forGetter(TelemetryData::gasCompositionSummary),
            Codec.STRING.optionalFieldOf("diagnostic_note", "").forGetter(TelemetryData::diagnosticNote),
            Codec.INT.optionalFieldOf("quality_pct", 100).forGetter(TelemetryData::qualityPct)
    ).apply(instance, TelemetryData::new));

    public static final StreamCodec<ByteBuf, TelemetryData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull TelemetryData decode(@NotNull ByteBuf buf) {
            ResourceLocation planetId = ResourceLocation.STREAM_CODEC.decode(buf);
            String planetName = ByteBufCodecs.STRING_UTF8.decode(buf);
            EventType eventType = EventType.STREAM_CODEC.decode(buf);
            String moduleId = ByteBufCodecs.STRING_UTF8.decode(buf);
            String domainId = ByteBufCodecs.STRING_UTF8.decode(buf);
            int sp = ByteBufCodecs.VAR_INT.decode(buf);
            int dom = ByteBufCodecs.VAR_INT.decode(buf);
            String gas = ByteBufCodecs.STRING_UTF8.decode(buf);
            String note = ByteBufCodecs.STRING_UTF8.decode(buf);
            int quality = ByteBufCodecs.VAR_INT.decode(buf);
            return new TelemetryData(planetId, planetName, eventType, moduleId, domainId, sp, dom, gas, note, quality);
        }

        @Override
        public void encode(@NotNull ByteBuf buf, @NotNull TelemetryData value) {
            ResourceLocation.STREAM_CODEC.encode(buf, value.planetId());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.planetName());
            EventType.STREAM_CODEC.encode(buf, value.eventType());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.scienceModuleId());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.primaryDomainId());
            ByteBufCodecs.VAR_INT.encode(buf, value.sciencePoints());
            ByteBufCodecs.VAR_INT.encode(buf, value.domainPoints());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.gasCompositionSummary());
            ByteBufCodecs.STRING_UTF8.encode(buf, value.diagnosticNote());
            ByteBufCodecs.VAR_INT.encode(buf, value.qualityPct());
        }
    };

    public TelemetryData {
        Objects.requireNonNull(planetId, "planetId");
        Objects.requireNonNull(planetName, "planetName");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(scienceModuleId, "scienceModuleId");
        Objects.requireNonNull(primaryDomainId, "primaryDomainId");
        sciencePoints = Math.max(0, sciencePoints);
        domainPoints = Math.max(0, domainPoints);
        gasCompositionSummary = gasCompositionSummary == null ? "" : gasCompositionSummary;
        diagnosticNote = diagnosticNote == null ? "" : diagnosticNote;
        qualityPct = Math.max(50, Math.min(350, qualityPct));
    }

    public boolean isThermalFailureBlackBox() {
        return this.eventType == EventType.THERMAL_FAILURE;
    }
}
