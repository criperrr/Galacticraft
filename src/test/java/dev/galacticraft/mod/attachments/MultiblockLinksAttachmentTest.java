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

import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.block.platform.PlatformBlockRole;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprint;
import dev.galacticraft.mod.content.block.platform.PlatformBlueprintRegistry;
import dev.galacticraft.mod.network.c2s.HangarTerminalActionPayload;
import dev.galacticraft.mod.screen.HangarTerminalOpeningData;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Multiblock links, bounds shapes, platform enums, blueprints and hangar payloads")
class MultiblockLinksAttachmentTest {
    private static final double EPSILON = 1.0E-9;
    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static RegistryAccess registryAccess;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    private static final BlockPos PROXY_A = new BlockPos(12, 64, -7);
    private static final BlockPos PROXY_B = new BlockPos(-12345, -64, 98765);
    private static final BlockPos PROXY_C = new BlockPos(30_000_000, 319, -30_000_000);
    private static final BlockPos MASTER_1 = new BlockPos(-3, 70, 4);
    private static final BlockPos MASTER_2 = new BlockPos(-33_000_000, -2048, 33_000_000);

    private static MultiblockLinksAttachment sample() {
        return MultiblockLinksAttachment.EMPTY
                .withLink(PROXY_A.asLong(), MASTER_1.asLong())
                .withLink(PROXY_B.asLong(), MASTER_2.asLong())
                .withLink(PROXY_C.asLong(), MASTER_1.asLong());
    }

    private static RegistryFriendlyByteBuf newRegistryBuf() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
    }

    // =========================================================================
    // 1. Attachment semantics
    // =========================================================================

    @Nested
    @DisplayName("MultiblockLinksAttachment semantics")
    class AttachmentSemantics {
        @Test
        @DisplayName("EMPTY resolves no master for any position")
        void emptyAttachmentHasNoLinks() {
            MultiblockLinksAttachment empty = MultiblockLinksAttachment.EMPTY;
            assertTrue(empty.isEmpty());
            assertEquals(0, empty.size());
            assertNull(empty.getMasterPos(PROXY_A));
            assertNull(empty.getMasterPos(BlockPos.ZERO));
            assertNull(empty.getMasterPos(null), "null proxy position must resolve to null instead of throwing");
            assertFalse(empty.containsProxy(PROXY_A.asLong()));
            assertFalse(empty.containsProxy(Long.MIN_VALUE));
        }

        @Test
        @DisplayName("withLink returns a new instance and never mutates the original")
        void withLinkIsImmutable() {
            MultiblockLinksAttachment base = MultiblockLinksAttachment.EMPTY;
            MultiblockLinksAttachment one = base.withLink(PROXY_A.asLong(), MASTER_1.asLong());

            assertNotSame(base, one);
            assertTrue(base.isEmpty(), "shared EMPTY singleton must stay empty");
            assertEquals(1, one.size());
            assertEquals(MASTER_1, one.getMasterPos(PROXY_A));
            assertTrue(one.containsProxy(PROXY_A.asLong()));

            MultiblockLinksAttachment two = one.withLink(PROXY_B.asLong(), MASTER_2.asLong());
            assertNotSame(one, two);
            assertEquals(1, one.size(), "previous snapshot must not gain the new link");
            assertNull(one.getMasterPos(PROXY_B));
            assertEquals(2, two.size());
            assertEquals(MASTER_1, two.getMasterPos(PROXY_A));
            assertEquals(MASTER_2, two.getMasterPos(PROXY_B));
        }

        @Test
        @DisplayName("withLink overwrites a stale master and is a no-op for identical links")
        void withLinkReplacesAndDeduplicates() {
            MultiblockLinksAttachment one = MultiblockLinksAttachment.EMPTY.withLink(PROXY_A.asLong(), MASTER_1.asLong());
            assertSame(one, one.withLink(PROXY_A.asLong(), MASTER_1.asLong()), "identical link must return the same instance");

            MultiblockLinksAttachment relinked = one.withLink(PROXY_A.asLong(), MASTER_2.asLong());
            assertNotSame(one, relinked);
            assertEquals(1, relinked.size());
            assertEquals(MASTER_2, relinked.getMasterPos(PROXY_A));
            assertEquals(MASTER_1, one.getMasterPos(PROXY_A), "original must still point at the old master");
        }

        @Test
        @DisplayName("withLinks bulk-adds under one master without touching the source")
        void withLinksIsImmutableAndBatched() {
            MultiblockLinksAttachment base = MultiblockLinksAttachment.EMPTY.withLink(PROXY_C.asLong(), MASTER_2.asLong());
            LongArrayList proxies = new LongArrayList(new long[]{PROXY_A.asLong(), PROXY_B.asLong()});

            MultiblockLinksAttachment bulk = base.withLinks(proxies, MASTER_1.asLong());

            assertNotSame(base, bulk);
            assertEquals(1, base.size());
            assertEquals(3, bulk.size());
            assertEquals(MASTER_1, bulk.getMasterPos(PROXY_A));
            assertEquals(MASTER_1, bulk.getMasterPos(PROXY_B));
            assertEquals(MASTER_2, bulk.getMasterPos(PROXY_C), "pre-existing unrelated link must survive");

            assertSame(bulk, bulk.withLinks(proxies, MASTER_1.asLong()), "re-linking the same set must be a no-op");
            assertSame(base, base.withLinks(new LongArrayList(), MASTER_1.asLong()), "empty proxy list must be a no-op");
            assertSame(base, base.withLinks(null, MASTER_1.asLong()), "null proxy list must be a no-op");
        }

        @Test
        @DisplayName("withoutLink removes exactly one proxy and collapses to EMPTY at zero")
        void withoutLinkIsImmutable() {
            MultiblockLinksAttachment full = sample();
            MultiblockLinksAttachment minusA = full.withoutLink(PROXY_A.asLong());

            assertNotSame(full, minusA);
            assertEquals(3, full.size(), "source must not shrink");
            assertEquals(MASTER_1, full.getMasterPos(PROXY_A));
            assertEquals(2, minusA.size());
            assertNull(minusA.getMasterPos(PROXY_A));
            assertEquals(MASTER_2, minusA.getMasterPos(PROXY_B));
            assertEquals(MASTER_1, minusA.getMasterPos(PROXY_C));

            assertSame(full, full.withoutLink(BlockPos.ZERO.asLong()), "removing an unknown proxy must be a no-op");

            MultiblockLinksAttachment single = MultiblockLinksAttachment.EMPTY.withLink(PROXY_A.asLong(), MASTER_1.asLong());
            assertSame(MultiblockLinksAttachment.EMPTY, single.withoutLink(PROXY_A.asLong()),
                    "removing the last link must collapse to the EMPTY singleton so the attachment can be dropped");
        }

        @Test
        @DisplayName("withoutLinks removes a batch, ignores unknown entries and collapses to EMPTY")
        void withoutLinksIsImmutable() {
            MultiblockLinksAttachment full = sample();
            LongArrayList remove = new LongArrayList(new long[]{PROXY_A.asLong(), PROXY_C.asLong(), BlockPos.ZERO.asLong()});

            MultiblockLinksAttachment remaining = full.withoutLinks(remove);

            assertNotSame(full, remaining);
            assertEquals(3, full.size());
            assertEquals(1, remaining.size());
            assertNull(remaining.getMasterPos(PROXY_A));
            assertNull(remaining.getMasterPos(PROXY_C));
            assertEquals(MASTER_2, remaining.getMasterPos(PROXY_B));

            assertSame(full, full.withoutLinks(new LongArrayList(new long[]{BlockPos.ZERO.asLong()})), "no present proxy -> same instance");
            assertSame(full, full.withoutLinks(null));
            assertSame(full, full.withoutLinks(new LongArrayList()));
            assertSame(MultiblockLinksAttachment.EMPTY,
                    full.withoutLinks(new LongArrayList(new long[]{PROXY_A.asLong(), PROXY_B.asLong(), PROXY_C.asLong()})));
            assertSame(MultiblockLinksAttachment.EMPTY, MultiblockLinksAttachment.EMPTY.withoutLinks(remove));
        }

        @Test
        @DisplayName("equals/hashCode are structural and independent of insertion order")
        void structuralEquality() {
            MultiblockLinksAttachment forward = sample();
            MultiblockLinksAttachment reverse = MultiblockLinksAttachment.EMPTY
                    .withLink(PROXY_C.asLong(), MASTER_1.asLong())
                    .withLink(PROXY_B.asLong(), MASTER_2.asLong())
                    .withLink(PROXY_A.asLong(), MASTER_1.asLong());
            assertEquals(forward, reverse);
            assertEquals(forward.hashCode(), reverse.hashCode());
            assertFalse(forward.equals(forward.withLink(PROXY_A.asLong(), MASTER_2.asLong())));
            assertFalse(forward.equals(MultiblockLinksAttachment.EMPTY));
        }
    }

    // =========================================================================
    // 2. CODEC (NBT) and STREAM_CODEC
    // =========================================================================

    @Nested
    @DisplayName("MultiblockLinksAttachment codecs")
    class AttachmentCodecs {
        @Test
        @DisplayName("NBT CODEC roundtrip preserves positive and negative coordinates")
        void nbtRoundtrip() {
            MultiblockLinksAttachment original = sample();

            Tag encoded = MultiblockLinksAttachment.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow(AssertionError::new);
            assertTrue(encoded instanceof LongArrayTag, "links must persist as a compact LongArray, got " + encoded.getClass());
            assertEquals(original.size() * 2, ((LongArrayTag) encoded).getAsLongArray().length, "two longs (proxy, master) per link");

            MultiblockLinksAttachment decoded = MultiblockLinksAttachment.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow(AssertionError::new);
            assertEquals(original, decoded);
            assertEquals(3, decoded.size());
            assertEquals(MASTER_1, decoded.getMasterPos(PROXY_A));
            assertEquals(MASTER_2, decoded.getMasterPos(PROXY_B));
            assertEquals(MASTER_1, decoded.getMasterPos(PROXY_C));
            assertEquals(new BlockPos(-33_000_000, -2048, 33_000_000), decoded.getMasterPos(PROXY_B), "negative extremes must survive packing");
        }

        @Test
        @DisplayName("NBT CODEC roundtrip of the empty attachment")
        void nbtRoundtripEmpty() {
            Tag encoded = MultiblockLinksAttachment.CODEC.encodeStart(NbtOps.INSTANCE, MultiblockLinksAttachment.EMPTY).getOrThrow(AssertionError::new);
            assertEquals(0, ((LongArrayTag) encoded).getAsLongArray().length);
            MultiblockLinksAttachment decoded = MultiblockLinksAttachment.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow(AssertionError::new);
            assertSame(MultiblockLinksAttachment.EMPTY, decoded);
        }

        @Test
        @DisplayName("Odd-length LongArray decodes defensively by dropping the dangling element")
        void nbtOddLengthDecodesDefensively() {
            LongArrayTag odd = new LongArrayTag(new long[]{PROXY_A.asLong(), MASTER_1.asLong(), PROXY_B.asLong()});
            MultiblockLinksAttachment decoded = MultiblockLinksAttachment.CODEC.parse(NbtOps.INSTANCE, odd).getOrThrow(AssertionError::new);
            assertEquals(1, decoded.size(), "only the complete pair may survive");
            assertEquals(MASTER_1, decoded.getMasterPos(PROXY_A));
            assertNull(decoded.getMasterPos(PROXY_B), "dangling proxy without master must not resolve");

            LongArrayTag single = new LongArrayTag(new long[]{PROXY_A.asLong()});
            assertSame(MultiblockLinksAttachment.EMPTY,
                    MultiblockLinksAttachment.CODEC.parse(NbtOps.INSTANCE, single).getOrThrow(AssertionError::new));
        }

        @Test
        @DisplayName("STREAM_CODEC roundtrip consumes every byte and preserves entries")
        void streamRoundtrip() {
            MultiblockLinksAttachment original = sample();
            RegistryFriendlyByteBuf buf = newRegistryBuf();
            try {
                MultiblockLinksAttachment.STREAM_CODEC.encode(buf, original);
                assertTrue(buf.readableBytes() > 0 && buf.readableBytes() < 64,
                        "compact stream encoding must be small, got " + buf.readableBytes() + " bytes");

                MultiblockLinksAttachment decoded = MultiblockLinksAttachment.STREAM_CODEC.decode(buf);
                assertEquals(0, buf.readableBytes(), "decode must leave zero unread bytes");
                assertEquals(original, decoded);
                assertEquals(MASTER_1, decoded.getMasterPos(PROXY_A));
                assertEquals(MASTER_2, decoded.getMasterPos(PROXY_B));
                assertEquals(MASTER_1, decoded.getMasterPos(PROXY_C));
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("STREAM_CODEC compresses a dense 11x11 platform chunk far below Fabric's 32,502-byte sync limit")
        void streamRoundtripDenseColossalPlatform() {
            // Simulate 3,116+ proxies packed into a single 16x16 chunk (tower + deck + crane rails)
            LongArrayList proxies = new LongArrayList(3200);
            BlockPos master = new BlockPos(8, 64, 15);
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    // Pad deck at Y=64
                    proxies.add(BlockPos.asLong(lx, 64, lz));
                    // Two colossal towers + mid-level rails + top bridge
                    if ((lx <= 3 || lx >= 12) && lz >= 4 && lz <= 11) {
                        for (int y = 65; y <= 103; y++) {
                            proxies.add(BlockPos.asLong(lx, y, lz));
                        }
                    } else if (lz == 7 || lz == 8) {
                        proxies.add(BlockPos.asLong(lx, 84, lz));
                        proxies.add(BlockPos.asLong(lx, 102, lz));
                    }
                }
            }
            assertTrue(proxies.size() > 2500, "test must exceed the old 2,031-proxy 32KB ceiling, got " + proxies.size());

            MultiblockLinksAttachment original = MultiblockLinksAttachment.EMPTY.withLinks(proxies, master.asLong());
            RegistryFriendlyByteBuf buf = newRegistryBuf();
            try {
                MultiblockLinksAttachment.STREAM_CODEC.encode(buf, original);
                int encodedBytes = buf.readableBytes();
                assertTrue(encodedBytes < 4096,
                        "compact RLE encoding for " + original.size() + " proxies must stay tiny (<4KB, Fabric limit 32,502B), got " + encodedBytes + "B");

                MultiblockLinksAttachment decoded = MultiblockLinksAttachment.STREAM_CODEC.decode(buf);
                assertEquals(0, buf.readableBytes(), "decode must leave zero unread bytes");
                assertEquals(original, decoded);
                assertEquals(original.size(), decoded.size());
                assertEquals(master, decoded.getMasterPos(new BlockPos(0, 64, 0)));
                assertEquals(master, decoded.getMasterPos(new BlockPos(15, 103, 11)));
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("STREAM_CODEC empty attachment costs a single byte and decodes to EMPTY")
        void streamRoundtripEmpty() {
            RegistryFriendlyByteBuf buf = newRegistryBuf();
            try {
                MultiblockLinksAttachment.STREAM_CODEC.encode(buf, MultiblockLinksAttachment.EMPTY);
                assertEquals(1, buf.readableBytes());
                assertSame(MultiblockLinksAttachment.EMPTY, MultiblockLinksAttachment.STREAM_CODEC.decode(buf));
                assertEquals(0, buf.readableBytes());
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("STREAM_CODEC rejects an inflated or negative pair count instead of allocating")
        void streamRejectsBogusPairCount() {
            RegistryFriendlyByteBuf inflated = newRegistryBuf();
            try {
                inflated.writeVarInt(1_000_000);
                inflated.writeLong(PROXY_A.asLong());
                inflated.writeLong(MASTER_1.asLong());
                assertThrows(DecoderException.class, () -> MultiblockLinksAttachment.STREAM_CODEC.decode(inflated));
            } finally {
                inflated.release();
            }

            RegistryFriendlyByteBuf negative = newRegistryBuf();
            try {
                negative.writeVarInt(-1);
                negative.writeLong(PROXY_A.asLong());
                negative.writeLong(MASTER_1.asLong());
                assertThrows(DecoderException.class, () -> MultiblockLinksAttachment.STREAM_CODEC.decode(negative));
            } finally {
                negative.release();
            }
        }
    }

    // =========================================================================
    // 3. BoundsShape
    // =========================================================================

    @Nested
    @DisplayName("BoundsShape")
    class BoundsShapes {
        @Test
        @DisplayName("All five shapes have distinct serialized names and roundtrip through CODEC")
        void distinctSerializedNames() {
            assertEquals(5, BoundsShape.values().length);
            Set<String> names = new HashSet<>();
            for (BoundsShape shape : BoundsShape.values()) {
                assertTrue(names.add(shape.getSerializedName()), "duplicate serialized name " + shape.getSerializedName());
                Tag tag = BoundsShape.CODEC.encodeStart(NbtOps.INSTANCE, shape).getOrThrow(AssertionError::new);
                assertEquals(shape, BoundsShape.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(AssertionError::new));
            }
            assertEquals(Set.of("full", "slab_8px", "floor_4px", "open_hatch", "elevator_floor"), names);
        }

        @Test
        @DisplayName("Shape heights match their pixel definitions")
        void shapeHeights() {
            assertEquals(1.0, BoundsShape.FULL.getShape().max(Direction.Axis.Y), EPSILON);
            assertEquals(0.5, BoundsShape.SLAB_8PX.getShape().max(Direction.Axis.Y), EPSILON);
            assertEquals(0.25, BoundsShape.FLOOR_4PX.getShape().max(Direction.Axis.Y), EPSILON);
            assertEquals(0.125, BoundsShape.ELEVATOR_FLOOR.getShape().max(Direction.Axis.Y), EPSILON);
            assertEquals(0.5, BoundsShape.SLAB_8PX.getCollisionShape().max(Direction.Axis.Y), EPSILON);
            assertEquals(0.25, BoundsShape.FLOOR_4PX.getCollisionShape().max(Direction.Axis.Y), EPSILON);
            assertEquals(0.125, BoundsShape.ELEVATOR_FLOOR.getCollisionShape().max(Direction.Axis.Y), EPSILON);
            for (BoundsShape shape : new BoundsShape[]{BoundsShape.FULL, BoundsShape.SLAB_8PX, BoundsShape.FLOOR_4PX, BoundsShape.ELEVATOR_FLOOR}) {
                assertEquals(0.0, shape.getShape().min(Direction.Axis.Y), EPSILON, shape + " must start at the floor of the block");
                assertEquals(1.0, shape.getShape().max(Direction.Axis.X) - shape.getShape().min(Direction.Axis.X), EPSILON, shape + " spans the full X width");
                assertEquals(1.0, shape.getShape().max(Direction.Axis.Z) - shape.getShape().min(Direction.Axis.Z), EPSILON, shape + " spans the full Z width");
            }
        }

        @Test
        @DisplayName("OPEN_HATCH is walk-through (no collision) yet still selectable")
        void openHatchHasNoCollision() {
            assertTrue(BoundsShape.OPEN_HATCH.getCollisionShape().isEmpty(), "entities must pass through an open hatch");
            assertFalse(BoundsShape.OPEN_HATCH.getShape().isEmpty(), "outline must exist so the hatch can be targeted");
            assertEquals(0.125, BoundsShape.OPEN_HATCH.getShape().max(Direction.Axis.Y), EPSILON);
            for (BoundsShape shape : BoundsShape.values()) {
                if (shape != BoundsShape.OPEN_HATCH) {
                    assertFalse(shape.getCollisionShape().isEmpty(), shape + " must collide");
                }
            }
        }
    }

    // =========================================================================
    // 4. PlatformType / PlatformSize
    // =========================================================================

    @Nested
    @DisplayName("PlatformType and PlatformSize")
    class PlatformEnums {
        @Test
        @DisplayName("Horizontal supports only 1x1 and 3x3, vertical supports all six")
        void supportMatrix() {
            Set<PlatformSize> horizontal = new HashSet<>();
            for (PlatformSize size : PlatformSize.values()) {
                assertTrue(size.supports(PlatformType.VERTICAL), size + " must be valid for VERTICAL");
                if (size.supports(PlatformType.HORIZONTAL)) {
                    horizontal.add(size);
                }
            }
            assertEquals(6, PlatformSize.values().length);
            assertEquals(Set.of(PlatformSize.SIZE_1X1, PlatformSize.SIZE_3X3), horizontal);
        }

        @Test
        @DisplayName("clampForType keeps supported sizes and clamps the rest to 3x3 / null to 1x1")
        void clampForType() {
            assertEquals(PlatformSize.SIZE_3X3, PlatformSize.clampForType(PlatformType.HORIZONTAL, PlatformSize.SIZE_9X9));
            assertEquals(PlatformSize.SIZE_3X3, PlatformSize.clampForType(PlatformType.HORIZONTAL, PlatformSize.SIZE_5X5));
            assertEquals(PlatformSize.SIZE_3X3, PlatformSize.clampForType(PlatformType.HORIZONTAL, PlatformSize.SIZE_7X7));
            assertEquals(PlatformSize.SIZE_3X3, PlatformSize.clampForType(PlatformType.HORIZONTAL, PlatformSize.SIZE_11X11));
            assertEquals(PlatformSize.SIZE_1X1, PlatformSize.clampForType(PlatformType.HORIZONTAL, PlatformSize.SIZE_1X1));
            assertEquals(PlatformSize.SIZE_3X3, PlatformSize.clampForType(PlatformType.HORIZONTAL, PlatformSize.SIZE_3X3));
            assertEquals(PlatformSize.SIZE_1X1, PlatformSize.clampForType(PlatformType.VERTICAL, null));
            for (PlatformSize size : PlatformSize.values()) {
                assertEquals(size, PlatformSize.clampForType(PlatformType.VERTICAL, size));
            }
        }

        @Test
        @DisplayName("forPadSpan maps odd pad widths to the matching rocket bay size")
        void forPadSpan() {
            assertEquals(PlatformSize.SIZE_1X1, PlatformSize.forPadSpan(3));
            assertEquals(PlatformSize.SIZE_3X3, PlatformSize.forPadSpan(5));
            assertEquals(PlatformSize.SIZE_5X5, PlatformSize.forPadSpan(7));
            assertEquals(PlatformSize.SIZE_7X7, PlatformSize.forPadSpan(9));
            assertEquals(PlatformSize.SIZE_9X9, PlatformSize.forPadSpan(11));
            assertEquals(PlatformSize.SIZE_11X11, PlatformSize.forPadSpan(13));
            // boundaries: spans between canonical widths round up to the next bay that fits
            assertEquals(PlatformSize.SIZE_1X1, PlatformSize.forPadSpan(1));
            assertEquals(PlatformSize.SIZE_1X1, PlatformSize.forPadSpan(0));
            assertEquals(PlatformSize.SIZE_1X1, PlatformSize.forPadSpan(-4));
            assertEquals(PlatformSize.SIZE_3X3, PlatformSize.forPadSpan(4));
            assertEquals(PlatformSize.SIZE_5X5, PlatformSize.forPadSpan(6));
            assertEquals(PlatformSize.SIZE_11X11, PlatformSize.forPadSpan(12));
            assertEquals(PlatformSize.SIZE_11X11, PlatformSize.forPadSpan(99));
            // every size's own deck width must map back to itself
            for (PlatformSize size : PlatformSize.values()) {
                assertEquals(size, PlatformSize.forPadSpan(size.padDeckWidth()), size + " deck width must round-trip");
                assertEquals(size.diameter() + 2, size.padDeckWidth(), size + " deck is one block wider than the rocket on each side");
            }
        }

        @Test
        @DisplayName("modelKey is '<type>_<size>' and unique per combination")
        void modelKeys() {
            assertEquals("vertical_1x1", PlatformSize.SIZE_1X1.modelKey(PlatformType.VERTICAL));
            assertEquals("horizontal_3x3", PlatformSize.SIZE_3X3.modelKey(PlatformType.HORIZONTAL));
            assertEquals("vertical_11x11", PlatformSize.SIZE_11X11.modelKey(PlatformType.VERTICAL));
            Set<String> keys = new HashSet<>();
            for (PlatformType type : PlatformType.values()) {
                for (PlatformSize size : PlatformSize.values()) {
                    String key = size.modelKey(type);
                    assertEquals(type.getSerializedName() + "_" + size.getSerializedName(), key);
                    assertTrue(keys.add(key), "duplicate model key " + key);
                }
            }
            assertEquals(12, keys.size());
        }

        @Test
        @DisplayName("Tier and wing socket progression is strictly increasing; horizontal 1x1 has two sockets")
        void progression() {
            PlatformSize[] sizes = PlatformSize.values();
            for (int i = 1; i < sizes.length; i++) {
                assertTrue(sizes[i].maxTier() > sizes[i - 1].maxTier(), "maxTier must grow with bay size at " + sizes[i]);
                assertTrue(sizes[i].diameter() > sizes[i - 1].diameter());
                assertTrue(sizes[i].wingSocketCount() > sizes[i - 1].wingSocketCount());
            }
            assertEquals(3, PlatformSize.SIZE_1X1.wingSocketCount(PlatformType.VERTICAL));
            assertEquals(2, PlatformSize.SIZE_1X1.wingSocketCount(PlatformType.HORIZONTAL));
            assertEquals(4, PlatformSize.SIZE_3X3.wingSocketCount(PlatformType.HORIZONTAL));
        }

        @Test
        @DisplayName("Codecs, ordinal wrapping and name lookup with fallback")
        void codecsAndLookups() {
            for (PlatformType type : PlatformType.values()) {
                Tag tag = PlatformType.CODEC.encodeStart(NbtOps.INSTANCE, type).getOrThrow(AssertionError::new);
                assertEquals(type, PlatformType.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(AssertionError::new));
                ByteBuf buf = Unpooled.buffer();
                try {
                    PlatformType.STREAM_CODEC.encode(buf, type);
                    assertEquals(type, PlatformType.STREAM_CODEC.decode(buf));
                    assertEquals(0, buf.readableBytes());
                } finally {
                    buf.release();
                }
            }
            for (PlatformSize size : PlatformSize.values()) {
                Tag tag = PlatformSize.CODEC.encodeStart(NbtOps.INSTANCE, size).getOrThrow(AssertionError::new);
                assertEquals(size, PlatformSize.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow(AssertionError::new));
                ByteBuf buf = Unpooled.buffer();
                try {
                    PlatformSize.STREAM_CODEC.encode(buf, size);
                    assertEquals(size, PlatformSize.STREAM_CODEC.decode(buf));
                    assertEquals(0, buf.readableBytes());
                } finally {
                    buf.release();
                }
            }

            assertEquals(PlatformType.HORIZONTAL, PlatformType.byOrdinal(1));
            assertEquals(PlatformType.HORIZONTAL, PlatformType.byOrdinal(3), "ordinals wrap around");
            assertEquals(PlatformType.HORIZONTAL, PlatformType.byOrdinal(-1), "negative ordinals wrap around");
            assertEquals(PlatformSize.SIZE_11X11, PlatformSize.byOrdinal(-1));
            assertEquals(PlatformSize.SIZE_3X3, PlatformSize.byOrdinal(7));
            assertEquals(PlatformType.HORIZONTAL, PlatformType.byName("horizontal", PlatformType.VERTICAL));
            assertEquals(PlatformType.VERTICAL, PlatformType.byName("diagonal", PlatformType.VERTICAL));
            assertEquals(PlatformSize.SIZE_5X5, PlatformSize.byName("5x5", PlatformSize.SIZE_1X1));
            assertEquals(PlatformSize.SIZE_1X1, PlatformSize.byName("13x13", PlatformSize.SIZE_1X1));
        }
    }

    // =========================================================================
    // 5. PlatformBlueprintRegistry
    // =========================================================================

    @Nested
    @DisplayName("PlatformBlueprintRegistry")
    class Blueprints {
        private final Map<String, Integer> expectedMaxY = Map.of(
                "horizontal_1x1", 3,
                "horizontal_3x3", 5,
                "vertical_1x1", 6,
                "vertical_3x3", 8,
                "vertical_5x5", 12,
                "vertical_7x7", 18,
                "vertical_9x9", 30,
                "vertical_11x11", 38
        );

        @Test
        @DisplayName("All eight blueprints are registered under their model keys")
        void allEightRegistered() {
            assertEquals(8, PlatformBlueprintRegistry.all().size());
            assertEquals(8, PlatformBlueprintRegistry.byModelKey().size());
            assertEquals(expectedMaxY.keySet(), PlatformBlueprintRegistry.byModelKey().keySet());
            for (Map.Entry<String, PlatformBlueprint> entry : PlatformBlueprintRegistry.byModelKey().entrySet()) {
                PlatformBlueprint bp = entry.getValue();
                assertEquals(entry.getKey(), bp.size().modelKey(bp.type()), "registry key must derive from blueprint type and size");
                assertSame(bp, PlatformBlueprintRegistry.get(bp.type(), bp.size()));
            }
            Set<PlatformBlueprint> distinct = new HashSet<>(PlatformBlueprintRegistry.all());
            assertEquals(8, distinct.size(), "blueprints must be distinct instances");
        }

        @Test
        @DisplayName("get() clamps unsupported combinations and null arguments")
        void getClamps() {
            assertSame(PlatformBlueprintRegistry.HORIZONTAL_3X3, PlatformBlueprintRegistry.get(PlatformType.HORIZONTAL, PlatformSize.SIZE_9X9));
            assertSame(PlatformBlueprintRegistry.HORIZONTAL_3X3, PlatformBlueprintRegistry.get(PlatformType.HORIZONTAL, PlatformSize.SIZE_11X11));
            assertSame(PlatformBlueprintRegistry.HORIZONTAL_1X1, PlatformBlueprintRegistry.get(PlatformType.HORIZONTAL, PlatformSize.SIZE_1X1));
            assertSame(PlatformBlueprintRegistry.VERTICAL_3X3, PlatformBlueprintRegistry.get(null, PlatformSize.SIZE_3X3));
            assertSame(PlatformBlueprintRegistry.VERTICAL_1X1, PlatformBlueprintRegistry.get(PlatformType.VERTICAL, null));
            assertSame(PlatformBlueprintRegistry.VERTICAL_11X11, PlatformBlueprintRegistry.get(PlatformType.VERTICAL, PlatformSize.SIZE_11X11));
        }

        @Test
        @DisplayName("Every blueprint has all four structural roles, consistent counts and unique cells")
        void structuralInvariants() {
            for (PlatformBlueprint bp : PlatformBlueprintRegistry.all()) {
                String name = bp.size().modelKey(bp.type());
                assertFalse(bp.entries().isEmpty(), name + " must have entries");

                EnumMap<PlatformBlockRole, Integer> counted = new EnumMap<>(PlatformBlockRole.class);
                Set<BlockPos> cells = new HashSet<>();
                for (PlatformBlueprint.Entry entry : bp.entries()) {
                    assertTrue(cells.add(entry.localPos()), name + " has a duplicate cell " + entry.localPos());
                    counted.merge(entry.role(), 1, Integer::sum);
                    assertNotNull(entry.formedShape());
                    assertEquals(entry.role().defaultFormedShape(), entry.formedShape(), name + " uses the role default shape at " + entry.localPos());
                    if (entry.role() == PlatformBlockRole.PAD) {
                        assertEquals(0, entry.localPos().getY(), name + " pad deck must lie at launch level");
                    } else {
                        assertFalse(entry.localPos().getX() == 0 && entry.localPos().getY() == 0 && entry.localPos().getZ() == 0,
                                name + " non-pad structure must not occupy the pad center");
                    }
                }

                int sum = 0;
                for (PlatformBlockRole role : PlatformBlockRole.values()) {
                    int required = bp.requiredCounts().get(role);
                    assertTrue(required > 0, name + " must require at least one " + role);
                    assertEquals(counted.getOrDefault(role, 0), required, name + " required count for " + role + " must match its entries");
                    sum += required;
                }
                assertEquals(bp.entries().size(), sum, name + " requiredCounts must sum to entries.size()");
                assertEquals(expectedMaxY.get(name), bp.maxY(), name + " tower height");
                assertEquals(bp.size().wingSocketCount(bp.type()), bp.requiredCounts().get(PlatformBlockRole.UPGRADE_SOCKET),
                        name + " socket count must match the size's wing socket count");

                int maxY = Integer.MIN_VALUE;
                for (PlatformBlueprint.Entry entry : bp.entries()) {
                    maxY = Math.max(maxY, entry.localPos().getY());
                    assertTrue(entry.localPos().getX() >= bp.minX() && entry.localPos().getX() <= bp.maxX(), name + " X bounds");
                    assertTrue(entry.localPos().getZ() >= bp.minZ() && entry.localPos().getZ() <= bp.maxZ(), name + " Z bounds");
                }
                assertEquals(maxY, bp.maxY());
            }
        }

        @Test
        @DisplayName("Pad deck footprint and exact skeleton counts for the 1x1 platforms")
        void exactCounts() {
            for (PlatformSize size : PlatformSize.values()) {
                PlatformBlueprint vertical = PlatformBlueprintRegistry.get(PlatformType.VERTICAL, size);
                int width = size.padDeckWidth();
                assertEquals(width * width, vertical.requiredCounts().get(PlatformBlockRole.PAD), size + " vertical pad must be a full square deck");
            }

            PlatformBlueprint v1 = PlatformBlueprintRegistry.VERTICAL_1X1;
            assertEquals(9, v1.requiredCounts().get(PlatformBlockRole.PAD));
            assertEquals(18, v1.requiredCounts().get(PlatformBlockRole.TRUSS));
            assertEquals(5, v1.requiredCounts().get(PlatformBlockRole.CRANE_RAIL));
            assertEquals(3, v1.requiredCounts().get(PlatformBlockRole.UPGRADE_SOCKET));
            assertEquals(35, v1.entries().size());

            PlatformBlueprint h1 = PlatformBlueprintRegistry.HORIZONTAL_1X1;
            assertEquals(27, h1.requiredCounts().get(PlatformBlockRole.PAD), "3x9 hangar deck");
            assertEquals(18, h1.requiredCounts().get(PlatformBlockRole.TRUSS), "6 columns of height 3");
            assertEquals(24, h1.requiredCounts().get(PlatformBlockRole.CRANE_RAIL), "two 9-long rails and two 3-wide bridges");
            assertEquals(2, h1.requiredCounts().get(PlatformBlockRole.UPGRADE_SOCKET));
            assertEquals(71, h1.entries().size());
        }

        @Test
        @DisplayName("Rocket clearance volume never overlaps the structural skeleton")
        void clearanceDisjointFromSkeleton() {
            for (PlatformBlueprint bp : PlatformBlueprintRegistry.all()) {
                String name = bp.size().modelKey(bp.type());
                BlockPos min = bp.clearanceMin();
                BlockPos max = bp.clearanceMax();
                assertTrue(min.getX() <= max.getX() && min.getY() <= max.getY() && min.getZ() <= max.getZ(), name + " clearance box is inverted");
                assertTrue(min.getY() >= 1, name + " clearance starts above the pad deck");
                assertTrue(min.getX() <= 0 && max.getX() >= 0 && min.getZ() <= 0 && max.getZ() >= 0, name + " clearance must cover the pad center column");
                for (PlatformBlueprint.Entry entry : bp.entries()) {
                    BlockPos p = entry.localPos();
                    boolean inside = p.getX() >= min.getX() && p.getX() <= max.getX()
                            && p.getY() >= min.getY() && p.getY() <= max.getY()
                            && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
                    assertFalse(inside, name + " skeleton cell " + p + " (" + entry.role() + ") intrudes into the rocket clearance");
                }
            }
        }

        @Test
        @DisplayName("toWorldPos applies the documented Y rotation for each facing")
        void worldPosRotation() {
            BlockPos center = new BlockPos(100, 64, -50);
            BlockPos local = new BlockPos(2, 3, 5);
            assertEquals(new BlockPos(102, 67, -45), PlatformBlueprint.toWorldPos(center, Direction.NORTH, local));
            assertEquals(new BlockPos(95, 67, -48), PlatformBlueprint.toWorldPos(center, Direction.EAST, local));
            assertEquals(new BlockPos(98, 67, -55), PlatformBlueprint.toWorldPos(center, Direction.SOUTH, local));
            assertEquals(new BlockPos(105, 67, -52), PlatformBlueprint.toWorldPos(center, Direction.WEST, local));
            assertEquals(new BlockPos(102, 67, -45), PlatformBlueprint.toWorldPos(center, Direction.UP, local), "non-horizontal facing falls back to NORTH");
            // height is never altered by horizontal rotation
            for (Direction facing : HORIZONTALS) {
                assertEquals(67, PlatformBlueprint.toWorldPos(center, facing, local).getY());
                assertEquals(center, PlatformBlueprint.toWorldPos(center, facing, BlockPos.ZERO), "pad center is the rotation pivot");
            }
        }

        @Test
        @DisplayName("toLocalPos inverts toWorldPos for every facing across every blueprint cell")
        void worldLocalRoundtrip() {
            BlockPos[] centers = {new BlockPos(0, 64, 0), new BlockPos(-1234, -60, 5678), new BlockPos(30_000_000, 300, -30_000_000)};
            for (BlockPos center : centers) {
                for (Direction facing : HORIZONTALS) {
                    for (PlatformBlueprint bp : PlatformBlueprintRegistry.all()) {
                        Set<BlockPos> worldCells = new HashSet<>();
                        for (PlatformBlueprint.Entry entry : bp.entries()) {
                            BlockPos world = PlatformBlueprint.toWorldPos(center, facing, entry.localPos());
                            assertEquals(entry.localPos(), PlatformBlueprint.toLocalPos(center, facing, world),
                                    "roundtrip failed for " + bp.size().modelKey(bp.type()) + " facing " + facing);
                            worldCells.add(world);
                        }
                        assertEquals(bp.entries().size(), worldCells.size(), "rotation must be injective (no two cells collapse) for facing " + facing);
                    }
                }
            }
            // inverse also holds in the other direction for arbitrary world offsets
            BlockPos center = new BlockPos(10, 70, 10);
            for (Direction facing : HORIZONTALS) {
                BlockPos world = new BlockPos(-7, 80, 33);
                BlockPos local = PlatformBlueprint.toLocalPos(center, facing, world);
                assertEquals(world, PlatformBlueprint.toWorldPos(center, facing, local));
            }
        }

        @Test
        @DisplayName("emptySummary reports every cell missing and nothing placed without a level")
        void emptySummary() {
            for (PlatformBlueprint bp : PlatformBlueprintRegistry.all()) {
                PlatformBlueprint.ValidationSummary summary = bp.emptySummary();
                assertEquals(bp.entries().size(), summary.missingCount());
                assertEquals(0, summary.wrongCount());
                assertEquals(0, summary.clearanceBlockedCount());
                assertEquals(0, summary.totalPlaced());
                assertEquals(bp.entries().size(), summary.totalRequired());
                assertFalse(summary.readyToAssemble());
                for (PlatformBlockRole role : PlatformBlockRole.values()) {
                    assertEquals(0, summary.placedCounts().get(role));
                    assertEquals(bp.requiredCounts().get(role), summary.requiredCounts().get(role));
                }
            }
        }

        @Test
        @DisplayName("ValidationSummary.readyToAssemble requires zero missing, wrong and blocked cells")
        void readyToAssembleLogic() {
            Map<PlatformBlockRole, Integer> none = new EnumMap<>(PlatformBlockRole.class);
            assertTrue(new PlatformBlueprint.ValidationSummary(none, none, 0, 0, 0).readyToAssemble());
            assertFalse(new PlatformBlueprint.ValidationSummary(none, none, 1, 0, 0).readyToAssemble());
            assertFalse(new PlatformBlueprint.ValidationSummary(none, none, 0, 1, 0).readyToAssemble());
            assertFalse(new PlatformBlueprint.ValidationSummary(none, none, 0, 0, 1).readyToAssemble());
        }

        @Test
        @DisplayName("PlatformBlockRole default formed shapes: pad is a slab, structure is full")
        void roleFormedShapes() {
            assertEquals(BoundsShape.SLAB_8PX, PlatformBlockRole.PAD.defaultFormedShape());
            assertEquals(BoundsShape.FULL, PlatformBlockRole.TRUSS.defaultFormedShape());
            assertEquals(BoundsShape.FULL, PlatformBlockRole.CRANE_RAIL.defaultFormedShape());
            assertEquals(BoundsShape.FULL, PlatformBlockRole.UPGRADE_SOCKET.defaultFormedShape());
        }
    }

    // =========================================================================
    // 6. Hangar terminal network payloads
    // =========================================================================

    @Nested
    @DisplayName("HangarTerminal payload codecs")
    class HangarPayloads {
        @Test
        @DisplayName("Action payload roundtrips for every Action value with zero unread bytes")
        void actionPayloadRoundtrip() {
            BlockPos[] positions = {new BlockPos(5, 64, -9), new BlockPos(-30_000_000, -64, 30_000_000), BlockPos.ZERO};
            int[] values = {0, 1, 5, 300, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
            assertEquals(16, HangarTerminalActionPayload.Action.values().length);

            for (HangarTerminalActionPayload.Action action : HangarTerminalActionPayload.Action.values()) {
                for (BlockPos pos : positions) {
                    for (int value : values) {
                        HangarTerminalActionPayload original = new HangarTerminalActionPayload(pos, action, value);
                        ByteBuf buf = Unpooled.buffer();
                        try {
                            HangarTerminalActionPayload.STREAM_CODEC.encode(buf, original);
                            assertTrue(buf.readableBytes() > 0);
                            HangarTerminalActionPayload decoded = HangarTerminalActionPayload.STREAM_CODEC.decode(buf);
                            assertEquals(original, decoded, "payload mismatch for " + action + " " + pos + " " + value);
                            assertEquals(action, decoded.action());
                            assertEquals(pos, decoded.pos());
                            assertEquals(value, decoded.value());
                            assertEquals(0, buf.readableBytes(), "unread bytes for " + action);
                        } finally {
                            buf.release();
                        }
                    }
                }
            }
        }

        @Test
        @DisplayName("Multiple payloads written back to back decode in order without bleed-over")
        void actionPayloadFraming() {
            List<HangarTerminalActionPayload> sequence = List.of(
                    new HangarTerminalActionPayload(new BlockPos(1, 2, 3), HangarTerminalActionPayload.Action.SET_TYPE, 1),
                    new HangarTerminalActionPayload(new BlockPos(-4, -5, -6), HangarTerminalActionPayload.Action.SET_LAYER_Y, -1),
                    new HangarTerminalActionPayload(new BlockPos(7, 8, 9), HangarTerminalActionPayload.Action.DISASSEMBLE, 0)
            );
            ByteBuf buf = Unpooled.buffer();
            try {
                for (HangarTerminalActionPayload payload : sequence) {
                    HangarTerminalActionPayload.STREAM_CODEC.encode(buf, payload);
                }
                for (HangarTerminalActionPayload expected : sequence) {
                    assertEquals(expected, HangarTerminalActionPayload.STREAM_CODEC.decode(buf));
                }
                assertEquals(0, buf.readableBytes());
            } finally {
                buf.release();
            }
        }

        @Test
        @DisplayName("Action STREAM_CODEC encodes the ordinal and wraps out-of-range ids defensively")
        void actionEnumCodec() {
            for (HangarTerminalActionPayload.Action action : HangarTerminalActionPayload.Action.values()) {
                ByteBuf buf = Unpooled.buffer();
                try {
                    HangarTerminalActionPayload.Action.STREAM_CODEC.encode(buf, action);
                    assertEquals(1, buf.readableBytes(), "ordinal fits in a single VarInt byte");
                    assertEquals(action.ordinal(), buf.getByte(buf.readerIndex()));
                    assertEquals(action, HangarTerminalActionPayload.Action.STREAM_CODEC.decode(buf));
                    assertEquals(0, buf.readableBytes());
                } finally {
                    buf.release();
                }
            }
            ByteBuf wrapped = Unpooled.buffer();
            try {
                wrapped.writeByte(HangarTerminalActionPayload.Action.values().length + 2);
                assertEquals(HangarTerminalActionPayload.Action.values()[2], HangarTerminalActionPayload.Action.STREAM_CODEC.decode(wrapped));
                assertEquals(0, wrapped.readableBytes());
            } finally {
                wrapped.release();
            }
        }

        @Test
        @DisplayName("Payload exposes its custom payload type under galacticraft:hangar_terminal_action")
        void payloadType() {
            HangarTerminalActionPayload payload = new HangarTerminalActionPayload(BlockPos.ZERO, HangarTerminalActionPayload.Action.ASSEMBLE, 0);
            assertSame(HangarTerminalActionPayload.TYPE, payload.type());
            assertEquals(Constant.id("hangar_terminal_action"), HangarTerminalActionPayload.TYPE.id());
            assertEquals("galacticraft:hangar_terminal_action", HangarTerminalActionPayload.ID.toString());
        }

        @Test
        @DisplayName("HangarTerminalOpeningData roundtrip preserves the terminal position")
        void openingDataRoundtrip() {
            BlockPos[] positions = {new BlockPos(100, 70, -200), new BlockPos(-1, -1, -1), new BlockPos(33_554_431, 2047, -33_554_432)};
            for (BlockPos pos : positions) {
                HangarTerminalOpeningData original = new HangarTerminalOpeningData(pos);
                ByteBuf buf = Unpooled.buffer();
                try {
                    HangarTerminalOpeningData.STREAM_CODEC.encode(buf, original);
                    assertEquals(Long.BYTES, buf.readableBytes(), "BlockPos is packed into a single long");
                    HangarTerminalOpeningData decoded = HangarTerminalOpeningData.STREAM_CODEC.decode(buf);
                    assertEquals(original, decoded);
                    assertEquals(pos, decoded.pos());
                    assertEquals(0, buf.readableBytes());
                } finally {
                    buf.release();
                }
            }
        }
    }
}
