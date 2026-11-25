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
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Typed structural, fluid, and payload mating interface on a modular rocket component.
 */
public record AttachmentNode(
        @NotNull String id,
        @NotNull AttachmentNodeType type,
        double localX,
        double localY,
        double localZ,
        @NotNull Direction normal,
        int diameterBlocks,
        boolean allowsCrossfeed,
        @NotNull CrossfeedMode defaultCrossfeed,
        @NotNull RadialSymmetry maxSymmetry
) {
    public AttachmentNode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(normal, "normal");
        if (diameterBlocks < 1) {
            throw new IllegalArgumentException("diameterBlocks must be >= 1, got " + diameterBlocks);
        }
        Objects.requireNonNull(defaultCrossfeed, "defaultCrossfeed");
        Objects.requireNonNull(maxSymmetry, "maxSymmetry");
    }

    /**
     * Checks whether this node can structurally mate with {@code other}.
     * Coaxial nodes require opposing polarity ({@code COAXIAL_TOP <-> COAXIAL_BOTTOM}) and matching diameters;
     * radial nodes require opposing or horizontal attachment polarity.
     */
    public boolean canMateWith(@NotNull AttachmentNode other) {
        Objects.requireNonNull(other, "other");
        return switch (this.type) {
            case COAXIAL_TOP -> other.type == AttachmentNodeType.COAXIAL_BOTTOM
                    && this.diameterBlocks == other.diameterBlocks;
            case COAXIAL_BOTTOM -> other.type == AttachmentNodeType.COAXIAL_TOP
                    && this.diameterBlocks == other.diameterBlocks;
            case RADIAL -> other.type == AttachmentNodeType.RADIAL;
            case ENGINE_CLUSTER -> other.type == AttachmentNodeType.COAXIAL_TOP
                    && other.diameterBlocks <= this.diameterBlocks;
            case PAYLOAD -> other.type == AttachmentNodeType.COAXIAL_BOTTOM
                    && other.diameterBlocks <= this.diameterBlocks;
        };
    }

    public static final Codec<AttachmentNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(AttachmentNode::id),
            AttachmentNodeType.CODEC.fieldOf("type").forGetter(AttachmentNode::type),
            Codec.DOUBLE.fieldOf("local_x").forGetter(AttachmentNode::localX),
            Codec.DOUBLE.fieldOf("local_y").forGetter(AttachmentNode::localY),
            Codec.DOUBLE.fieldOf("local_z").forGetter(AttachmentNode::localZ),
            Direction.CODEC.fieldOf("normal").forGetter(AttachmentNode::normal),
            Codec.INT.fieldOf("diameter_blocks").forGetter(AttachmentNode::diameterBlocks),
            Codec.BOOL.optionalFieldOf("allows_crossfeed", false).forGetter(AttachmentNode::allowsCrossfeed),
            CrossfeedMode.CODEC.optionalFieldOf("default_crossfeed", CrossfeedMode.ISOLATED).forGetter(AttachmentNode::defaultCrossfeed),
            RadialSymmetry.CODEC.optionalFieldOf("max_symmetry", RadialSymmetry.NONE).forGetter(AttachmentNode::maxSymmetry)
    ).apply(instance, AttachmentNode::new));

    public static final StreamCodec<ByteBuf, AttachmentNode> STREAM_CODEC = StreamCodec.of(
            (buf, node) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, node.id);
                AttachmentNodeType.STREAM_CODEC.encode(buf, node.type);
                ByteBufCodecs.DOUBLE.encode(buf, node.localX);
                ByteBufCodecs.DOUBLE.encode(buf, node.localY);
                ByteBufCodecs.DOUBLE.encode(buf, node.localZ);
                Direction.STREAM_CODEC.encode(buf, node.normal);
                ByteBufCodecs.VAR_INT.encode(buf, node.diameterBlocks);
                ByteBufCodecs.BOOL.encode(buf, node.allowsCrossfeed);
                CrossfeedMode.STREAM_CODEC.encode(buf, node.defaultCrossfeed);
                RadialSymmetry.STREAM_CODEC.encode(buf, node.maxSymmetry);
            },
            buf -> new AttachmentNode(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    AttachmentNodeType.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    Direction.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    CrossfeedMode.STREAM_CODEC.decode(buf),
                    RadialSymmetry.STREAM_CODEC.decode(buf)
            )
    );

    public enum AttachmentNodeType implements StringRepresentable {
        COAXIAL_TOP("coaxial_top"),
        COAXIAL_BOTTOM("coaxial_bottom"),
        RADIAL("radial"),
        ENGINE_CLUSTER("engine_cluster"),
        PAYLOAD("payload");

        public static final Codec<AttachmentNodeType> CODEC = StringRepresentable.fromEnum(AttachmentNodeType::values);
        public static final StreamCodec<ByteBuf, AttachmentNodeType> STREAM_CODEC = ByteBufCodecs.idMapper(
                i -> values()[Math.floorMod(i, values().length)],
                Enum::ordinal
        );

        private final String serializedName;

        AttachmentNodeType(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }
    }

    public enum CrossfeedMode implements StringRepresentable {
        ISOLATED("isolated"),
        CONTINUOUS_CROSSFEED("continuous_crossfeed"),
        ONE_WAY_DOWN("one_way_down"),
        ONE_WAY_UP("one_way_up");

        public static final Codec<CrossfeedMode> CODEC = StringRepresentable.fromEnum(CrossfeedMode::values);
        public static final StreamCodec<ByteBuf, CrossfeedMode> STREAM_CODEC = ByteBufCodecs.idMapper(
                i -> values()[Math.floorMod(i, values().length)],
                Enum::ordinal
        );

        private final String serializedName;

        CrossfeedMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public @NotNull CrossfeedMode toggleAsparagus() {
            return this == CONTINUOUS_CROSSFEED ? ISOLATED : CONTINUOUS_CROSSFEED;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }
    }

    /**
     * Octagonal radial symmetry modes matching GC5's 4 cardinal faces and 4 diagonal 45-degree chamfers.
     */
    public enum RadialSymmetry implements StringRepresentable {
        NONE("1x", 1, new float[]{0.0F}),
        TWO("2x", 2, new float[]{0.0F, 180.0F}),
        FOUR_CARDINAL("4x", 4, new float[]{0.0F, 90.0F, 180.0F, 270.0F}),
        FOUR_DIAGONAL("4x_diag", 4, new float[]{45.0F, 135.0F, 225.0F, 315.0F}),
        EIGHT("8x", 8, new float[]{0.0F, 45.0F, 90.0F, 135.0F, 180.0F, 225.0F, 270.0F, 315.0F});

        public static final Codec<RadialSymmetry> CODEC = StringRepresentable.fromEnum(RadialSymmetry::values);
        public static final StreamCodec<ByteBuf, RadialSymmetry> STREAM_CODEC = ByteBufCodecs.idMapper(
                i -> values()[Math.floorMod(i, values().length)],
                Enum::ordinal
        );

        private final String serializedName;
        private final int count;
        private final float[] azimuthDegrees;

        RadialSymmetry(String serializedName, int count, float[] azimuthDegrees) {
            this.serializedName = serializedName;
            this.count = count;
            this.azimuthDegrees = azimuthDegrees;
        }

        public int count() {
            return this.count;
        }

        public float[] azimuthDegrees() {
            return this.azimuthDegrees.clone();
        }

        public @NotNull RadialSymmetry next() {
            RadialSymmetry[] all = values();
            return all[(this.ordinal() + 1) % all.length];
        }

        public static @NotNull RadialSymmetry byOrdinal(int ordinal) {
            RadialSymmetry[] all = values();
            return all[Math.floorMod(ordinal, all.length)];
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }
    }
}
