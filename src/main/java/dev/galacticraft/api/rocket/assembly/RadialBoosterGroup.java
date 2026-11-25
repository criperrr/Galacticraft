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

package dev.galacticraft.api.rocket.assembly;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * Immutable group of symmetrically mounted radial boosters (SRBs or liquid strap-on cores)
 * attached to a {@link RocketStageNode}.
 */
public record RadialBoosterGroup(
        @NotNull String groupId,
        @NotNull RadialSymmetry symmetry,
        @NotNull CrossfeedMode crossfeedMode,
        boolean solidBooster,
        int drainPriorityLayer,
        double radialOffsetBlocks,
        @NotNull String propellantId,
        long fuelCapacityDroplets,
        long currentFuelDroplets,
        @NotNull List<MountedPartNode> parts,
        boolean decoupled
) {
    public RadialBoosterGroup {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId must not be blank");
        }
        Objects.requireNonNull(symmetry, "symmetry");
        Objects.requireNonNull(crossfeedMode, "crossfeedMode");
        if (drainPriorityLayer < 0) {
            throw new IllegalArgumentException("drainPriorityLayer must be >= 0");
        }
        if (!Double.isFinite(radialOffsetBlocks) || radialOffsetBlocks < 0.0) {
            radialOffsetBlocks = 0.0;
        }
        propellantId = (propellantId == null || propellantId.isBlank()) ? "rp1" : propellantId;
        fuelCapacityDroplets = Math.max(0L, fuelCapacityDroplets);
        currentFuelDroplets = Math.clamp(currentFuelDroplets, 0L, fuelCapacityDroplets);
        parts = List.copyOf(Objects.requireNonNull(parts, "parts"));
    }

    public double dryMassKg() {
        double sum = 0.0;
        for (MountedPartNode part : this.parts) {
            sum += part.stats().dryWeightKg();
        }
        return sum;
    }

    public double totalThrustKn() {
        double sum = 0.0;
        for (MountedPartNode part : this.parts) {
            sum += part.stats().thrustKn();
        }
        return sum;
    }

    public @NotNull RadialBoosterGroup withCurrentFuel(long nextFuel) {
        return new RadialBoosterGroup(
                this.groupId,
                this.symmetry,
                this.crossfeedMode,
                this.solidBooster,
                this.drainPriorityLayer,
                this.radialOffsetBlocks,
                this.propellantId,
                this.fuelCapacityDroplets,
                Math.clamp(nextFuel, 0L, this.fuelCapacityDroplets),
                this.parts,
                this.decoupled
        );
    }

    public @NotNull RadialBoosterGroup asDecoupled() {
        if (this.decoupled) {
            return this;
        }
        return new RadialBoosterGroup(
                this.groupId,
                this.symmetry,
                this.crossfeedMode,
                this.solidBooster,
                this.drainPriorityLayer,
                this.radialOffsetBlocks,
                this.propellantId,
                this.fuelCapacityDroplets,
                this.currentFuelDroplets,
                this.parts,
                true
        );
    }

    public static final Codec<RadialBoosterGroup> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("group_id").forGetter(RadialBoosterGroup::groupId),
            RadialSymmetry.CODEC.fieldOf("symmetry").forGetter(RadialBoosterGroup::symmetry),
            CrossfeedMode.CODEC.optionalFieldOf("crossfeed_mode", CrossfeedMode.ISOLATED).forGetter(RadialBoosterGroup::crossfeedMode),
            Codec.BOOL.optionalFieldOf("solid_booster", false).forGetter(RadialBoosterGroup::solidBooster),
            Codec.INT.optionalFieldOf("drain_priority_layer", 0).forGetter(RadialBoosterGroup::drainPriorityLayer),
            Codec.DOUBLE.optionalFieldOf("radial_offset_blocks", 0.0).forGetter(RadialBoosterGroup::radialOffsetBlocks),
            Codec.STRING.optionalFieldOf("propellant_id", "rp1").forGetter(RadialBoosterGroup::propellantId),
            Codec.LONG.optionalFieldOf("fuel_capacity_droplets", 0L).forGetter(RadialBoosterGroup::fuelCapacityDroplets),
            Codec.LONG.optionalFieldOf("current_fuel_droplets", 0L).forGetter(RadialBoosterGroup::currentFuelDroplets),
            MountedPartNode.CODEC.listOf().fieldOf("parts").forGetter(RadialBoosterGroup::parts),
            Codec.BOOL.optionalFieldOf("decoupled", false).forGetter(RadialBoosterGroup::decoupled)
    ).apply(instance, RadialBoosterGroup::new));

    public static final StreamCodec<ByteBuf, RadialBoosterGroup> STREAM_CODEC = StreamCodec.of(
            (buf, group) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, group.groupId);
                RadialSymmetry.STREAM_CODEC.encode(buf, group.symmetry);
                CrossfeedMode.STREAM_CODEC.encode(buf, group.crossfeedMode);
                ByteBufCodecs.BOOL.encode(buf, group.solidBooster);
                ByteBufCodecs.VAR_INT.encode(buf, group.drainPriorityLayer);
                ByteBufCodecs.DOUBLE.encode(buf, group.radialOffsetBlocks);
                ByteBufCodecs.STRING_UTF8.encode(buf, group.propellantId);
                ByteBufCodecs.VAR_LONG.encode(buf, group.fuelCapacityDroplets);
                ByteBufCodecs.VAR_LONG.encode(buf, group.currentFuelDroplets);
                MountedPartNode.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, group.parts);
                ByteBufCodecs.BOOL.encode(buf, group.decoupled);
            },
            buf -> new RadialBoosterGroup(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    RadialSymmetry.STREAM_CODEC.decode(buf),
                    CrossfeedMode.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    MountedPartNode.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );
}
