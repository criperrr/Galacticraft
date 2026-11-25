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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.mod.Constant;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Data-driven {@link SimpleSynchronousResourceReloadListener} reading {@link ManualEntry} JSON definitions
 * from {@code data/<namespace>/manual_entries/<tab>/<id>.json} (and optional client resource pack overrides
 * on {@code F3+T}), maintaining sorted tab lists, research-unlock reverse indices, and item-to-entry lookup.
 */
public final class ManualManager implements SimpleSynchronousResourceReloadListener {
    public static final ManualManager INSTANCE = new ManualManager();
    public static final ResourceLocation ID = Constant.id("manual_manager");
    public static final ResourceLocation CLIENT_ID = Constant.id("manual_client_reloader");
    public static final String DIRECTORY = "manual_entries";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<ResourceLocation, ManualEntry> entries = new LinkedHashMap<>();
    private final Map<ManualTab, List<ManualEntry>> byTab = new EnumMap<>(ManualTab.class);
    private final Map<ResourceLocation, List<ManualEntry>> byRequiredResearch = new HashMap<>();
    private final Map<ResourceLocation, ResourceLocation> itemIdToEntry = new HashMap<>();
    private int lastLoadedJsonCount = 0;

    private ManualManager() {
        resetToBuiltInDefaults();
    }

    @Override
    public @NotNull ResourceLocation getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager manager) {
        Map<ResourceLocation, ManualEntry> loaded = loadFromResources(manager, DIRECTORY);
        this.lastLoadedJsonCount = loaded.size();
        if (!loaded.isEmpty()) {
            applyLoadedEntries(loaded);
            Constant.LOGGER.info("Loaded {} data-driven Aerospace Manual entries ({} total active).", loaded.size(), this.entries.size());
        } else {
            resetToBuiltInDefaults();
        }
    }

    /**
     * Client-side reload listener enabling {@code F3+T} hot-reload of optional asset overrides
     * under {@code assets/<namespace>/manual_entries/} without wiping server/built-in entries.
     */
    public SimpleSynchronousResourceReloadListener createClientReloader() {
        return new SimpleSynchronousResourceReloadListener() {
            @Override
            public @NotNull ResourceLocation getFabricId() {
                return CLIENT_ID;
            }

            @Override
            public void onResourceManagerReload(@NotNull ResourceManager manager) {
                Map<ResourceLocation, ManualEntry> overrides = loadFromResources(manager, DIRECTORY);
                if (!overrides.isEmpty()) {
                    mergeOverrides(overrides);
                    Constant.LOGGER.info("Hot-reloaded {} client Aerospace Manual overrides.", overrides.size());
                } else if (ManualManager.this.entries.isEmpty()) {
                    resetToBuiltInDefaults();
                }
            }
        };
    }

    public static @NotNull Map<ResourceLocation, ManualEntry> loadFromResources(
            @NotNull ResourceManager manager,
            @NotNull String directory
    ) {
        Map<ResourceLocation, ManualEntry> loaded = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(directory, path -> path.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                DataResult<ManualEntry> parsed = ManualEntry.CODEC.parse(JsonOps.INSTANCE, json);
                parsed.resultOrPartial(err -> Constant.LOGGER.warn("Invalid Aerospace Manual entry {}: {}", fileId, err))
                        .ifPresent(manualEntry -> loaded.put(manualEntry.id(), manualEntry));
            } catch (Exception e) {
                Constant.LOGGER.warn("Failed to read Aerospace Manual entry {}: {}", fileId, e.toString());
            }
        }
        return loaded;
    }

    public synchronized void applyLoadedEntries(@NotNull Map<ResourceLocation, ManualEntry> loaded) {
        this.entries.clear();
        for (ManualEntry builtIn : ManualCatalog.builtInEntries()) {
            this.entries.put(builtIn.id(), builtIn);
        }
        this.entries.putAll(loaded);
        rebuildIndices();
    }

    public synchronized void mergeOverrides(@NotNull Map<ResourceLocation, ManualEntry> overrides) {
        if (this.entries.isEmpty()) {
            for (ManualEntry builtIn : ManualCatalog.builtInEntries()) {
                this.entries.put(builtIn.id(), builtIn);
            }
        }
        this.entries.putAll(overrides);
        rebuildIndices();
    }

    public synchronized void resetToBuiltInDefaults() {
        this.entries.clear();
        for (ManualEntry builtIn : ManualCatalog.builtInEntries()) {
            this.entries.put(builtIn.id(), builtIn);
        }
        rebuildIndices();
    }

    private void rebuildIndices() {
        for (ManualTab tab : ManualTab.values()) {
            this.byTab.put(tab, new ArrayList<>());
        }
        this.byRequiredResearch.clear();
        this.itemIdToEntry.clear();

        List<ManualEntry> sorted = new ArrayList<>(this.entries.values());
        sorted.sort(Comparator.comparingInt(ManualEntry::order).thenComparing(e -> e.id().toString()));

        for (ManualEntry entry : sorted) {
            this.byTab.get(entry.tab()).add(entry);
            ResourceLocation req = entry.requiredResearchId();
            if (req != null) {
                this.byRequiredResearch.computeIfAbsent(req, k -> new ArrayList<>()).add(entry);
            }
            this.itemIdToEntry.putIfAbsent(entry.iconItem(), entry.id());
            for (ResourceLocation rel : entry.relatedItems()) {
                this.itemIdToEntry.putIfAbsent(rel, entry.id());
            }
        }
    }

    public synchronized @NotNull Collection<ManualEntry> allEntries() {
        return Collections.unmodifiableList(new ArrayList<>(this.entries.values()));
    }

    public synchronized @NotNull List<ManualEntry> entriesForTab(@NotNull ManualTab tab) {
        return Collections.unmodifiableList(new ArrayList<>(this.byTab.getOrDefault(tab, List.of())));
    }

    public synchronized @Nullable ManualEntry get(@Nullable ResourceLocation id) {
        return id == null ? null : this.entries.get(id);
    }

    public synchronized @NotNull List<ManualEntry> entriesUnlockedBy(@NotNull ResourceLocation researchNodeId) {
        return Collections.unmodifiableList(new ArrayList<>(this.byRequiredResearch.getOrDefault(researchNodeId, List.of())));
    }

    public synchronized @NotNull Optional<ResourceLocation> findEntryForItem(@Nullable Item item) {
        if (item == null || item == Items.AIR) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.itemIdToEntry.get(BuiltInRegistries.ITEM.getKey(item)));
    }

    public synchronized @NotNull Optional<ResourceLocation> findEntryForItemId(@Nullable ResourceLocation itemId) {
        if (itemId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.itemIdToEntry.get(itemId));
    }

    public int lastLoadedJsonCount() {
        return this.lastLoadedJsonCount;
    }

    public synchronized int totalEntryCount() {
        return this.entries.size();
    }
}
