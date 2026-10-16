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

package dev.galacticraft.api.rocket;

import dev.galacticraft.api.APIConstants;
import dev.galacticraft.api.registry.RocketRegistries;
import dev.galacticraft.api.rocket.part.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EitherHolder;

import java.util.Optional;

public class RocketPrefabs {
    private static final ResourceKey<RocketCone<?, ?>> TIER_1_CONE = APIConstants.key(RocketRegistries.ROCKET_CONE, "tier_1");
    private static final ResourceKey<RocketBody<?, ?>> TIER_1_BODY = APIConstants.key(RocketRegistries.ROCKET_BODY, "tier_1");
    private static final ResourceKey<RocketFin<?, ?>> TIER_1_FIN = APIConstants.key(RocketRegistries.ROCKET_FIN, "tier_1");
    private static final ResourceKey<RocketEngine<?, ?>> TIER_1_ENGINE = APIConstants.key(RocketRegistries.ROCKET_ENGINE, "tier_1");
    private static final ResourceKey<RocketUpgrade<?, ?>> STORAGE_UPGRADE = APIConstants.key(RocketRegistries.ROCKET_UPGRADE, "storage");

    public static final RocketData MISSING = new RocketData(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), 0);
    public static final RocketData TIER_1 = new RocketData(
            new EitherHolder<>(TIER_1_CONE),
            new EitherHolder<>(TIER_1_BODY),
            new EitherHolder<>(TIER_1_FIN),
            null,
            new EitherHolder<>(TIER_1_ENGINE),
            null,
            null,
            0xFFFFFFFF
    );
    public static final RocketData TIER_1_STORAGE_UPGRADE = new RocketData(
            new EitherHolder<>(TIER_1_CONE),
            new EitherHolder<>(TIER_1_BODY),
            new EitherHolder<>(TIER_1_FIN),
            null,
            new EitherHolder<>(TIER_1_ENGINE),
            new EitherHolder<>(STORAGE_UPGRADE),
            null,
            0xFFFFFFFF
    );
    // Same as t1 for now
    public static final RocketData TIER_2 = new RocketData(
            new EitherHolder<>(TIER_1_CONE),
            new EitherHolder<>(TIER_1_BODY),
            new EitherHolder<>(TIER_1_FIN),
            null,
            new EitherHolder<>(TIER_1_ENGINE),
            null,
            null,
            0xFFFFFFFF
    );
}
