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
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable snapshot of a single modular rocket part belonging to a {@link RocketAssemblyGraph}.
 *
 * @param relativePos      Position relative to the pad center deck {@code (0, 0, 0)}.
 * @param config           Part geometry, category, diameter, height, and livery.
 * @param stats            Computed physical stats (thrust, dry mass, fuel capacity, tolerances).
 * @param integrity        Current structural integrity of the part.
 * @param composition      Optional Tinkers-style material composition for emergent physics traits.
 * @param stageIndex       Zero-based stage index ({@code 0} = bottom-most stage).
 * @param radialAttachment Whether this part belongs to a radial booster group rather than the core stack.
 */
public record MountedPartNode(
        @NotNull BlockPos relativePos,
        @NotNull RocketPartConfig config,
        @NotNull RocketPartStats stats,
        @NotNull PartIntegrity integrity,
        @NotNull Optional<RocketPartComposition> composition,
        int stageIndex,
        boolean radialAttachment
) {
    public MountedPartNode {
        relativePos = Objects.requireNonNull(relativePos, "relativePos").immutable();
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(integrity, "integrity");
        composition = Objects.requireNonNull(composition, "composition");
        if (stageIndex < 0) {
            throw new IllegalArgumentException("stageIndex must be >= 0, got " + stageIndex);
        }
    }

    public static final Codec<MountedPartNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("relative_pos").forGetter(MountedPartNode::relativePos),
            RocketPartConfig.CODEC.fieldOf("config").forGetter(MountedPartNode::config),
            RocketPartStats.CODEC.fieldOf("stats").forGetter(MountedPartNode::stats),
            PartIntegrity.CODEC.fieldOf("integrity").forGetter(MountedPartNode::integrity),
            RocketPartComposition.CODEC.optionalFieldOf("composition").forGetter(MountedPartNode::composition),
            Codec.INT.optionalFieldOf("stage_index", 0).forGetter(MountedPartNode::stageIndex),
            Codec.BOOL.optionalFieldOf("radial_attachment", false).forGetter(MountedPartNode::radialAttachment)
    ).apply(instance, MountedPartNode::new));

    public static final StreamCodec<ByteBuf, MountedPartNode> STREAM_CODEC = StreamCodec.of(
            (buf, node) -> {
                BlockPos.STREAM_CODEC.encode(buf, node.relativePos);
                RocketPartConfig.STREAM_CODEC.encode(buf, node.config);
                RocketPartStats.STREAM_CODEC.encode(buf, node.stats);
                PartIntegrity.STREAM_CODEC.encode(buf, node.integrity);
                ByteBufCodecs.optional(RocketPartComposition.STREAM_CODEC).encode(buf, node.composition);
                ByteBufCodecs.VAR_INT.encode(buf, node.stageIndex);
                ByteBufCodecs.BOOL.encode(buf, node.radialAttachment);
            },
            buf -> new MountedPartNode(
                    BlockPos.STREAM_CODEC.decode(buf),
                    RocketPartConfig.STREAM_CODEC.decode(buf),
                    RocketPartStats.STREAM_CODEC.decode(buf),
                    PartIntegrity.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(RocketPartComposition.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );
}
