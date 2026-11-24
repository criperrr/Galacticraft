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

package dev.galacticraft.mod.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.mod.Constant;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-data reload listener loading data-driven {@link ResearchNode} JSON files from
 * {@code data/<namespace>/research_tree/*.json} using {@link ResearchNode#CODEC} and updating
 * {@link ResearchCatalog} live on server start and {@code /reload}.
 */
public final class ResearchTreeDataManager implements SimpleSynchronousResourceReloadListener {
    public static final ResearchTreeDataManager INSTANCE = new ResearchTreeDataManager();
    public static final ResourceLocation ID = Constant.id("research_tree_data_manager");
    public static final String DIRECTORY = "research_tree";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private int lastLoadedCount = 0;

    private ResearchTreeDataManager() {
    }

    @Override
    public @NotNull ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager manager) {
        Map<ResourceLocation, ResearchNode> loaded = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(DIRECTORY, path -> path.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                DataResult<ResearchNode> parsed = ResearchNode.CODEC.parse(JsonOps.INSTANCE, json);
                parsed.resultOrPartial(err -> Constant.LOGGER.warn("Invalid research tree node {}: {}", fileId, err))
                        .ifPresent(node -> loaded.put(node.id(), node));
            } catch (Exception e) {
                Constant.LOGGER.warn("Failed to read research tree node {}: {}", fileId, e.toString());
            }
        }
        this.lastLoadedCount = loaded.size();
        if (!loaded.isEmpty()) {
            ResearchCatalog.applyDataDrivenNodes(loaded);
            Constant.LOGGER.info("Loaded {} data-driven research tree nodes ({} total active).", loaded.size(), ResearchCatalog.totalNodeCount());
        } else {
            ResearchCatalog.resetToBuiltInDefaults();
        }
    }

    public int getLastLoadedCount() {
        return this.lastLoadedCount;
    }
}
