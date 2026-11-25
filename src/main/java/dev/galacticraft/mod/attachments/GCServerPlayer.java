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

package dev.galacticraft.mod.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.api.rocket.RocketData;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class GCServerPlayer {
    private RocketData rocketData;
    public NonNullList<ItemStack> stacks = NonNullList.withSize(2, ItemStack.EMPTY);
    public long fuel;
    private @Nullable RocketAssemblyGraph modularRocketGraph;
    private long modularRocketEnergy;
    private long modularRocketOxygen;

    public static final Codec<GCServerPlayer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RocketData.CODEC.optionalFieldOf("rocket_data").forGetter(o -> Optional.ofNullable(o.rocketData)),
            Codec.LONG.fieldOf("fuel").forGetter(GCServerPlayer::getFuel),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("rocket_stacks").forGetter(GCServerPlayer::getRocketStacks),
            RocketAssemblyGraph.CODEC.optionalFieldOf("modular_rocket_graph").forGetter(o -> Optional.ofNullable(o.modularRocketGraph)),
            Codec.LONG.optionalFieldOf("modular_rocket_energy", 0L).forGetter(GCServerPlayer::getModularRocketEnergy),
            Codec.LONG.optionalFieldOf("modular_rocket_oxygen", 0L).forGetter(GCServerPlayer::getModularRocketOxygen)
    ).apply(instance, (data, fuel, stacks, graph, energy, oxygen) -> new GCServerPlayer(
            data.orElse(null), fuel, stacks, graph.orElse(null), energy, oxygen
    )));

    public static GCServerPlayer get(ServerPlayer player) {
        return player.getAttachedOrCreate(GCAttachments.SERVER_PLAYER, () -> new GCServerPlayer(player));
    }

    public GCServerPlayer(ServerPlayer player) {
    }

    public GCServerPlayer(RocketData data, long fuel, List<ItemStack> stacks) {
        this(data, fuel, stacks, null, 0L, 0L);
    }

    public GCServerPlayer(
            RocketData data,
            long fuel,
            List<ItemStack> stacks,
            @Nullable RocketAssemblyGraph modularRocketGraph,
            long modularRocketEnergy,
            long modularRocketOxygen
    ) {
        this.rocketData = data;
        this.fuel = fuel;
        this.stacks = NonNullList.of(ItemStack.EMPTY, stacks.toArray(ItemStack[]::new));
        this.modularRocketGraph = modularRocketGraph;
        this.modularRocketEnergy = Math.max(0L, modularRocketEnergy);
        this.modularRocketOxygen = Math.max(0L, modularRocketOxygen);
    }

    public @Nullable RocketAssemblyGraph getModularRocketGraph() {
        return this.modularRocketGraph;
    }

    public void setModularRocketGraph(@Nullable RocketAssemblyGraph graph) {
        this.modularRocketGraph = (graph != null && !graph.isEmpty()) ? graph : null;
    }

    public boolean hasModularRocketGraph() {
        return this.modularRocketGraph != null && !this.modularRocketGraph.isEmpty();
    }

    public long getModularRocketEnergy() {
        return this.modularRocketEnergy;
    }

    public void setModularRocketEnergy(long energy) {
        this.modularRocketEnergy = Math.max(0L, energy);
    }

    public long getModularRocketOxygen() {
        return this.modularRocketOxygen;
    }

    public void setModularRocketOxygen(long oxygen) {
        this.modularRocketOxygen = Math.max(0L, oxygen);
    }

    public void clearModularRocketState() {
        this.modularRocketGraph = null;
        this.modularRocketEnergy = 0L;
        this.modularRocketOxygen = 0L;
    }

    public RocketData getRocketData() {
        return rocketData;
    }

    public void setRocketData(RocketData rocketData) {
        this.rocketData = rocketData;
    }

    public NonNullList<ItemStack> getRocketStacks() {
        return this.stacks;
    }

    public void setRocketStacks(NonNullList<ItemStack> rocketStacks) {
        this.stacks = rocketStacks;
    }

    public long getFuel() {
        return fuel;
    }

    public void setFuel(long fuel) {
        this.fuel = fuel;
    }

    public void setRocketItem(ItemStack rocketItem) {
        for (int stack = 0; stack < getRocketStacks().size(); stack++)
            if (getRocketStacks().get(stack).isEmpty())
                if (stack == getRocketStacks().size() - 1)
                    getRocketStacks().set(stack, rocketItem);
    }

    public void setLaunchpadStack(ItemStack launchpad) {
        for (int stack = 0; stack < getRocketStacks().size(); stack++)
            if (getRocketStacks().get(stack).isEmpty())
                if (stack == getRocketStacks().size() - 2)
                    getRocketStacks().set(stack, launchpad == null ? ItemStack.EMPTY : launchpad);
    }
}
