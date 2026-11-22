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

package dev.galacticraft.mod.content.rocket.part;

import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.Constant;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable catalog metadata specification for a single exported modular rocket part variant.
 */
public record RocketPartSpec(
        @NotNull String modelId,
        @NotNull String familyId,
        @NotNull String displayName,
        @NotNull RocketPartCategory category,
        boolean parametric,
        int diameter,
        int topDiameter,
        int heightBlocks,
        boolean walkableInterior,
        boolean radial,
        @NotNull ResourceLocation meshLocation,
        @NotNull Map<RocketLivery, ResourceLocation> textures
) {
    public RocketPartSpec {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId must not be blank");
        }
        if (familyId == null || familyId.isBlank()) {
            throw new IllegalArgumentException("familyId must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
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
        Objects.requireNonNull(meshLocation, "meshLocation");
        EnumMap<RocketLivery, ResourceLocation> copy = new EnumMap<>(RocketLivery.class);
        if (textures != null) {
            for (Map.Entry<RocketLivery, ResourceLocation> entry : textures.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    copy.put(entry.getKey(), entry.getValue());
                }
            }
        }
        textures = Collections.unmodifiableMap(copy);
    }

    /**
     * Converts this catalog specification into a serializable {@link RocketPartConfig} with the requested livery.
     */
    public @NotNull RocketPartConfig toConfig(@Nullable RocketLivery livery) {
        RocketLivery resolved = livery != null ? livery : RocketLivery.WHITE;
        return new RocketPartConfig(
                this.modelId,
                this.familyId,
                this.category,
                this.diameter,
                this.topDiameter,
                this.heightBlocks,
                resolved,
                this.walkableInterior,
                this.radial
        );
    }

    /**
     * Resolves the texture {@link ResourceLocation} for the requested livery, falling back cleanly
     * to {@link RocketLivery#WHITE}, any available livery texture, or a canonical category path.
     */
    public @NotNull ResourceLocation textureFor(@Nullable RocketLivery livery) {
        if (livery != null) {
            ResourceLocation exact = this.textures.get(livery);
            if (exact != null) {
                return exact;
            }
        }
        ResourceLocation white = this.textures.get(RocketLivery.WHITE);
        if (white != null) {
            return white;
        }
        if (!this.textures.isEmpty()) {
            return this.textures.values().iterator().next();
        }
        return Constant.id("textures/rocket_part/" + this.category.assetFolder() + "/" + this.modelId + "_white.png");
    }
}
