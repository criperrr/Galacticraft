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

package dev.galacticraft.impl.internal.mixin.research;

import dev.galacticraft.api.accessor.ServerResearchAccessor;
import dev.galacticraft.impl.network.s2c.ResearchUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements ServerResearchAccessor {
    @Shadow
    public abstract boolean isCreative();

    private final @Unique Set<ResourceLocation> unlockedRecipes = new HashSet<>();
    private final @Unique Map<String, Integer> domainPoints = new HashMap<>();
    private final @Unique Set<ResourceLocation> mappedBodies = new HashSet<>();

    @Override
    public boolean galacticraft$isUnlocked(ResourceLocation id) {
        if (this.isCreative()) return true;
        return this.unlockedRecipes.contains(id);
    }

    @Override
    public @org.jetbrains.annotations.NotNull Set<ResourceLocation> galacticraft$getUnlockedResearch() {
        return Collections.unmodifiableSet(this.unlockedRecipes);
    }

    @Override
    public int galacticraft$getDomainPoints(@org.jetbrains.annotations.NotNull String domainId) {
        return this.domainPoints.getOrDefault(domainId, 0);
    }

    @Override
    public @org.jetbrains.annotations.NotNull Map<String, Integer> galacticraft$getAllDomainPoints() {
        return Collections.unmodifiableMap(this.domainPoints);
    }

    @Override
    public boolean galacticraft$isCelestialBodyMapped(@org.jetbrains.annotations.NotNull ResourceLocation bodyId) {
        if (this.isCreative()) return true;
        return this.mappedBodies.contains(bodyId);
    }

    @Override
    public @org.jetbrains.annotations.NotNull Set<ResourceLocation> galacticraft$getMappedCelestialBodies() {
        return Collections.unmodifiableSet(this.mappedBodies);
    }

    @Override
    public void galacticraft$unlockRocketPartRecipes(ResourceLocation... ids) {
        Collections.addAll(this.unlockedRecipes, ids);

        ServerPlayNetworking.send((ServerPlayer) (Object) this, new ResearchUpdatePayload(true, List.of(ids)));
    }

    @Override
    public void galacticraft$unlearnRocketPartRecipes(ResourceLocation... ids) {
        for (ResourceLocation id : ids) {
            this.unlockedRecipes.remove(id);
        }

        ServerPlayNetworking.send((ServerPlayer) (Object) this, new ResearchUpdatePayload(false, List.of(ids)));
    }

    @Override
    public void galacticraft$addDomainPoints(@org.jetbrains.annotations.NotNull String domainId, int delta) {
        int next = Math.max(0, this.domainPoints.getOrDefault(domainId, 0) + delta);
        this.domainPoints.put(domainId, next);
    }

    @Override
    public void galacticraft$markCelestialBodyMapped(@org.jetbrains.annotations.NotNull ResourceLocation bodyId) {
        this.mappedBodies.add(bodyId);
    }

    @Override
    public void galacticraft$applyResearchState(
            @org.jetbrains.annotations.NotNull Set<ResourceLocation> unlockedNodes,
            @org.jetbrains.annotations.NotNull Map<String, Integer> domainPoints,
            @org.jetbrains.annotations.NotNull Set<ResourceLocation> mappedBodies
    ) {
        this.unlockedRecipes.clear();
        this.unlockedRecipes.addAll(unlockedNodes);
        this.domainPoints.clear();
        this.domainPoints.putAll(domainPoints);
        this.mappedBodies.clear();
        this.mappedBodies.addAll(mappedBodies);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void galacticraft_readCustomDataFromNbt(CompoundTag nbt, CallbackInfo ci) {
        this.unlockedRecipes.clear();
        ListTag list = nbt.getList("gcResearch", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation parsed = ResourceLocation.tryParse(list.getString(i));
            if (parsed != null) {
                this.unlockedRecipes.add(parsed);
            }
        }
        this.domainPoints.clear();
        if (nbt.contains("gcResearchDomains", Tag.TAG_COMPOUND)) {
            CompoundTag domainsTag = nbt.getCompound("gcResearchDomains");
            for (String key : domainsTag.getAllKeys()) {
                this.domainPoints.put(key, domainsTag.getInt(key));
            }
        }
        this.mappedBodies.clear();
        ListTag mappedList = nbt.getList("gcMappedBodies", Tag.TAG_STRING);
        for (int i = 0; i < mappedList.size(); i++) {
            ResourceLocation parsed = ResourceLocation.tryParse(mappedList.getString(i));
            if (parsed != null) {
                this.mappedBodies.add(parsed);
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void galacticraft_writeCustomDataToNbt(CompoundTag nbt, CallbackInfo ci) {
        ListTag list = new ListTag();
        for (ResourceLocation id : this.unlockedRecipes) {
            list.add(StringTag.valueOf(id.toString()));
        }
        nbt.put("gcResearch", list);

        CompoundTag domainsTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : this.domainPoints.entrySet()) {
            domainsTag.putInt(entry.getKey(), entry.getValue());
        }
        nbt.put("gcResearchDomains", domainsTag);

        ListTag mappedList = new ListTag();
        for (ResourceLocation id : this.mappedBodies) {
            mappedList.add(StringTag.valueOf(id.toString()));
        }
        nbt.put("gcMappedBodies", mappedList);
    }
}
