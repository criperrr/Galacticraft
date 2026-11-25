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

package dev.galacticraft.mod.manual;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Server-safe indirection bridge so common classes ({@code AerospaceManualItem},
 * {@code SyncResearchProgressPayload}) can trigger client-only Manual screens and Toasts
 * without referencing {@code net.minecraft.client.*} on dedicated servers.
 */
public final class ManualClientBridge {
    private static volatile Consumer<@Nullable ResourceLocation> openManualHandler = entryId -> { };
    private static volatile Consumer<@NotNull Collection<ResourceLocation>> researchUnlockedHandler = nodes -> { };

    private ManualClientBridge() {
    }

    public static void registerClientHandlers(
            @NotNull Consumer<@Nullable ResourceLocation> opener,
            @NotNull Consumer<@NotNull Collection<ResourceLocation>> unlockNotifier
    ) {
        openManualHandler = opener;
        researchUnlockedHandler = unlockNotifier;
    }

    public static void openManual(@Nullable ResourceLocation entryId) {
        openManualHandler.accept(entryId);
    }

    public static void onResearchNodesUnlocked(@NotNull Collection<ResourceLocation> newlyUnlockedNodeIds) {
        if (!newlyUnlockedNodeIds.isEmpty()) {
            researchUnlockedHandler.accept(newlyUnlockedNodeIds);
        }
    }
}
