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

import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchNode;
import dev.galacticraft.mod.research.ResearchTreeDataManager;
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
 * DataGen provider exporting the entire 5-Branch / 6-Era Aerospace R&amp;D Tech Tree
 * as data-driven Codec JSON files under {@code data/galacticraft/research_tree/*.json}.
 */
public class GCResearchTreeProvider implements DataProvider {
    private final FabricDataOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public GCResearchTreeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.output = output;
        this.registries = registries;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput writer) {
        PackOutput.PathProvider pathProvider = this.output.createPathProvider(PackOutput.Target.DATA_PACK, ResearchTreeDataManager.DIRECTORY);
        return this.registries.thenCompose(provider -> {
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (ResearchNode node : ResearchCatalog.builtInNodes()) {
                String cleanPath = node.id().getPath().startsWith("research/")
                        ? node.id().getPath().substring("research/".length())
                        : node.id().getPath();
                ResourceLocation fileId = ResourceLocation.fromNamespaceAndPath(node.id().getNamespace(), cleanPath);
                Path target = pathProvider.json(fileId);
                futures.add(DataProvider.saveStable(writer, provider, ResearchNode.CODEC, node, target));
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public @NotNull String getName() {
        return "Galacticraft Research Tree";
    }
}
