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

package dev.galacticraft.api.rocket.part.modular;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable configuration describing a fabricated or world-placed modular rocket part instance.
 *
 * @param modelId          Unique catalog model identifier (stem of the exported {@code .gcrm} asset).
 * @param familyId         Logical part family identifier shared across parametric size variants.
 * @param category         Functional category determining the backing block and schematic slot layout.
 * @param diameter         Bottom footprint diameter in blocks ({@code >= 1}).
 * @param topDiameter      Top mating diameter in blocks ({@code >= 1}, differs from {@code diameter} on adapters).
 * @param heightBlocks     Vertical span in blocks ({@code >= 1}).
 * @param livery           Hull paint livery applied to the rendered texture.
 * @param walkableInterior Whether this part provides a hollow walkable interior with an operable airlock hatch.
 * @param radial           Whether this part mounts radially as a single-column attachment.
 */
public record RocketPartConfig(
        @NotNull String modelId,
        @NotNull String familyId,
        @NotNull RocketPartCategory category,
        int diameter,
        int topDiameter,
        int heightBlocks,
        @NotNull RocketLivery livery,
        boolean walkableInterior,
        boolean radial
) {
    public RocketPartConfig {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId must not be blank");
        }
        if (familyId == null || familyId.isBlank()) {
            throw new IllegalArgumentException("familyId must not be blank");
        }
        Objects.requireNonNull(category, "category");
        if (diameter < 1) {
            throw new IllegalArgumentException("diameter must be >= 1, got " + diameter);
        }
        if (topDiameter < 1) {
            throw new IllegalArgumentException("topDiameter must be >= 1, got " + topDiameter);
        }
        if (heightBlocks < 1) {
            throw new IllegalArgumentException("heightBlocks must be >= 1, got " + heightBlocks);
        }
        Objects.requireNonNull(livery, "livery");
    }

    public static final Codec<RocketPartConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("model_id").forGetter(RocketPartConfig::modelId),
            Codec.STRING.fieldOf("family_id").forGetter(RocketPartConfig::familyId),
            RocketPartCategory.CODEC.fieldOf("category").forGetter(RocketPartConfig::category),
            Codec.INT.fieldOf("diameter").forGetter(RocketPartConfig::diameter),
            Codec.INT.fieldOf("top_diameter").forGetter(RocketPartConfig::topDiameter),
            Codec.INT.fieldOf("height_blocks").forGetter(RocketPartConfig::heightBlocks),
            RocketLivery.CODEC.optionalFieldOf("livery", RocketLivery.WHITE).forGetter(RocketPartConfig::livery),
            Codec.BOOL.optionalFieldOf("walkable_interior", false).forGetter(RocketPartConfig::walkableInterior),
            Codec.BOOL.optionalFieldOf("radial", false).forGetter(RocketPartConfig::radial)
    ).apply(instance, RocketPartConfig::new));

    public static final StreamCodec<ByteBuf, RocketPartConfig> STREAM_CODEC = StreamCodec.of(
            (buf, config) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, config.modelId);
                ByteBufCodecs.STRING_UTF8.encode(buf, config.familyId);
                RocketPartCategory.STREAM_CODEC.encode(buf, config.category);
                ByteBufCodecs.VAR_INT.encode(buf, config.diameter);
                ByteBufCodecs.VAR_INT.encode(buf, config.topDiameter);
                ByteBufCodecs.VAR_INT.encode(buf, config.heightBlocks);
                RocketLivery.STREAM_CODEC.encode(buf, config.livery);
                ByteBufCodecs.BOOL.encode(buf, config.walkableInterior);
                ByteBufCodecs.BOOL.encode(buf, config.radial);
            },
            buf -> new RocketPartConfig(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    RocketPartCategory.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    RocketLivery.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );

    public @NotNull RocketPartConfig withLivery(@NotNull RocketLivery newLivery) {
        Objects.requireNonNull(newLivery, "newLivery");
        if (newLivery == this.livery) {
            return this;
        }
        return new RocketPartConfig(
                this.modelId,
                this.familyId,
                this.category,
                this.diameter,
                this.topDiameter,
                this.heightBlocks,
                newLivery,
                this.walkableInterior,
                this.radial
        );
    }

    public @NotNull FootprintMode footprintMode() {
        if (this.radial) {
            return FootprintMode.RADIAL_1B;
        }
        if (this.walkableInterior && this.diameter >= 3) {
            return FootprintMode.HOLLOW_WITH_HATCH;
        }
        return FootprintMode.SOLID_OCTAGON;
    }
}
