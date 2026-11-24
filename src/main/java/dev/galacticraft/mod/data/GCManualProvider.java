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

package dev.galacticraft.mod.data;

import dev.galacticraft.mod.manual.ManualCatalog;
import dev.galacticraft.mod.manual.ManualEntry;
import dev.galacticraft.mod.manual.ManualManager;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * DataGen provider exporting all Aerospace Manual entries as Codec-validated JSON files
 * under {@code data/galacticraft/manual_entries/<tab>/<id>.json}.
 */
public class GCManualProvider implements DataProvider {
    private final FabricDataOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public GCManualProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput writer) {
        PackOutput.PathProvider pathProvider = this.output.createPathProvider(PackOutput.Target.DATA_PACK, ManualManager.DIRECTORY);
        return this.registries.thenCompose(provider -> {
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (ManualEntry entry : ManualCatalog.builtInEntries()) {
                ResourceLocation fileId = entry.id();
                Path target = pathProvider.json(fileId);
                futures.add(DataProvider.saveStable(writer, provider, ManualEntry.CODEC, entry, target));
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public @NotNull String getName() {
        return "Galacticraft Aerospace Manual";
    }
}
