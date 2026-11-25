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

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.AttachmentNodeType;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.CrossfeedMode;
import dev.galacticraft.api.rocket.assembly.AttachmentNode.RadialSymmetry;
import dev.galacticraft.api.rocket.assembly.RocketAssemblyGraph.LocalSeatOffset;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.network.c2s.AttachRadialBoosterPayload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RocketAssemblyGraph API Codecs, StreamCodecs, AttachmentNodes & Staging Transitions")
class RocketAssemblyGraphTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
    }

    @Test
    @DisplayName("AttachmentNode mating rules enforce polarity, diameter continuity, and cluster/payload bounds")
    void attachmentNodeMatingRules() {
        AttachmentNode top3 = new AttachmentNode("top", AttachmentNodeType.COAXIAL_TOP, 0, 2, 0, Direction.UP, 3, true, CrossfeedMode.ISOLATED, RadialSymmetry.NONE);
        AttachmentNode bot3 = new AttachmentNode("bot", AttachmentNodeType.COAXIAL_BOTTOM, 0, 0, 0, Direction.DOWN, 3, true, CrossfeedMode.ISOLATED, RadialSymmetry.NONE);
        AttachmentNode bot5 = new AttachmentNode("bot5", AttachmentNodeType.COAXIAL_BOTTOM, 0, 0, 0, Direction.DOWN, 5, true, CrossfeedMode.ISOLATED, RadialSymmetry.NONE);
        AttachmentNode radA = new AttachmentNode("radA", AttachmentNodeType.RADIAL, 1.5, 1, 0, Direction.EAST, 3, true, CrossfeedMode.CONTINUOUS_CROSSFEED, RadialSymmetry.EIGHT);
        AttachmentNode radB = new AttachmentNode("radB", AttachmentNodeType.RADIAL, -0.5, 1, 0, Direction.WEST, 1, true, CrossfeedMode.ISOLATED, RadialSymmetry.EIGHT);
        AttachmentNode cluster5 = new AttachmentNode("cluster", AttachmentNodeType.ENGINE_CLUSTER, 0, 0, 0, Direction.DOWN, 5, true, CrossfeedMode.ONE_WAY_DOWN, RadialSymmetry.NONE);
        AttachmentNode payload3 = new AttachmentNode("payload", AttachmentNodeType.PAYLOAD, 0, 0.25, 0, Direction.UP, 3, false, CrossfeedMode.ISOLATED, RadialSymmetry.NONE);

        assertTrue(top3.canMateWith(bot3));
        assertTrue(bot3.canMateWith(top3));
        assertFalse(top3.canMateWith(bot5), "Mismatched coaxial diameters must not mate");
        assertFalse(top3.canMateWith(top3), "Same coaxial polarity must not mate");
        assertTrue(radA.canMateWith(radB));
        assertFalse(radA.canMateWith(bot3));
        assertTrue(cluster5.canMateWith(top3), "3x3 engine top fits on 5x5 engine cluster socket");
        assertTrue(payload3.canMateWith(bot3), "3x3 payload fits inside 3x3 payload rail");
        assertFalse(payload3.canMateWith(bot5), "5x5 payload must not fit inside 3x3 payload rail");

        // Verify AttachmentNode CODEC & STREAM_CODEC roundtrip
        Tag nbt = AttachmentNode.CODEC.encodeStart(NbtOps.INSTANCE, radA).getOrThrow(AssertionError::new);
        assertEquals(radA, AttachmentNode.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(AssertionError::new));

        ByteBuf buf = Unpooled.buffer();
        try {
            AttachmentNode.STREAM_CODEC.encode(buf, radA);
            assertEquals(radA, AttachmentNode.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    @DisplayName("RocketAssemblyGraph CODEC (NbtOps & JsonOps) and STREAM_CODEC roundtrip cleanly with 0 unread bytes")
    void fullGraphCodecAndStreamCodecRoundtrip() {
        RocketPartConfig engineCfg = new RocketPartConfig("engine_falcon1d_3x3_2b", "engine_falcon1d", RocketPartCategory.ENGINE, 3, 3, 2, RocketLivery.WHITE, false, false);
        RocketPartConfig tankCfg = new RocketPartConfig("tank_fuel_rp1_3x3_2b", "tank_fuel_rp1", RocketPartCategory.TANK, 3, 3, 2, RocketLivery.ORANGE, false, false);
        RocketPartConfig srbCfg = new RocketPartConfig("srb_ares_1x1_2b", "srb_ares", RocketPartCategory.ENGINE, 1, 1, 2, RocketLivery.WHITE, false, false);
        RocketPartConfig upperTankCfg = new RocketPartConfig("tank_hydrolox_supercryo_3x3_2b", "tank_hydrolox_supercryo", RocketPartCategory.TANK, 3, 3, 2, RocketLivery.BLUE, false, false);
        RocketPartConfig capCfg = new RocketPartConfig("capsule_gemini_3x3_2b", "capsule_gemini", RocketPartCategory.COMMAND, 3, 3, 2, RocketLivery.WHITE, true, false);

        MountedPartNode stage0Engine = new MountedPartNode(new BlockPos(0, 1, 0), engineCfg, RocketPartStats.builder().dryWeight(800).thrust(1200).build(), PartIntegrity.pristine(), Optional.empty(), 0, false);
        MountedPartNode stage0Tank = new MountedPartNode(new BlockPos(0, 3, 0), tankCfg, RocketPartStats.builder().dryWeight(900).fuelBuckets(40).build(), PartIntegrity.pristine(), Optional.empty(), 0, false);
        MountedPartNode boosterEast = new MountedPartNode(new BlockPos(3, 1, 0), srbCfg, RocketPartStats.builder().dryWeight(400).thrust(650).fuelBuckets(12).build(), PartIntegrity.pristine(), Optional.empty(), 0, true);
        MountedPartNode boosterWest = new MountedPartNode(new BlockPos(-3, 1, 0), srbCfg, RocketPartStats.builder().dryWeight(400).thrust(650).fuelBuckets(12).build(), PartIntegrity.pristine(), Optional.empty(), 0, true);

        RadialBoosterGroup srbPair = new RadialBoosterGroup(
                "stage0_radial_0", RadialSymmetry.TWO, CrossfeedMode.ISOLATED, true, 0, 3.0,
                "solid", 24L * 81_000L, 24L * 81_000L, List.of(boosterEast, boosterWest), false
        );

        RocketStageNode stage0 = new RocketStageNode(
                0, Optional.of(new BlockPos(0, 5, 0)), true, "rp1",
                40L * 81_000L, 35L * 81_000L, List.of(stage0Engine, stage0Tank), List.of(srbPair), false
        );

        MountedPartNode stage1Tank = new MountedPartNode(new BlockPos(0, 6, 0), upperTankCfg, RocketPartStats.builder().dryWeight(650).fuelBuckets(25).build(), PartIntegrity.pristine(), Optional.empty(), 1, false);
        MountedPartNode stage1Cap = new MountedPartNode(new BlockPos(0, 8, 0), capCfg, RocketPartStats.builder().dryWeight(500).build(), PartIntegrity.pristine(), Optional.empty(), 1, false);
        RocketStageNode stage1 = new RocketStageNode(
                1, Optional.empty(), false, "hydrolox",
                25L * 81_000L, 25L * 81_000L, List.of(stage1Tank, stage1Cap), List.of(), false
        );

        RocketAssemblyGraph graph = new RocketAssemblyGraph(
                List.of(stage0, stage1),
                List.of(new LocalSeatOffset(0.0, 8.8, 0.0, capCfg.modelId())),
                9,
                3.5,
                true,
                true,
                true
        );

        // 1. NbtOps CODEC roundtrip
        Tag nbt = RocketAssemblyGraph.CODEC.encodeStart(NbtOps.INSTANCE, graph).getOrThrow(AssertionError::new);
        RocketAssemblyGraph fromNbt = RocketAssemblyGraph.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(AssertionError::new);
        assertEquals(graph, fromNbt);

        // 2. JsonOps CODEC roundtrip
        JsonElement json = RocketAssemblyGraph.CODEC.encodeStart(JsonOps.INSTANCE, graph).getOrThrow(AssertionError::new);
        RocketAssemblyGraph fromJson = RocketAssemblyGraph.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(AssertionError::new);
        assertEquals(graph, fromJson);

        // 3. STREAM_CODEC roundtrip
        ByteBuf buf = Unpooled.buffer();
        try {
            RocketAssemblyGraph.STREAM_CODEC.encode(buf, graph);
            assertTrue(buf.readableBytes() > 0);
            RocketAssemblyGraph decoded = RocketAssemblyGraph.STREAM_CODEC.decode(buf);
            assertEquals(0, buf.readableBytes(), "STREAM_CODEC must leave 0 unread bytes");
            assertEquals(graph, decoded);
        } finally {
            buf.release();
        }

        // 4. Immutable staging & radial booster jettison transitions
        assertNotNull(graph.activeStage());
        assertEquals(0, graph.activeStage().stageIndex());
        double initialRadius = graph.maxRadiusBlocks();

        RocketAssemblyGraph afterBoosterSep = graph.withoutRadialGroup(0, "stage0_radial_0");
        assertTrue(afterBoosterSep.maxRadiusBlocks() < initialRadius, "Jettisoning radial boosters must shrink maxRadiusBlocks");
        assertEquals(stage0.coreDryMassKg() + stage1.totalDryMassKg(), afterBoosterSep.totalDryMassKg(), 1.0e-6);

        RocketAssemblyGraph afterStage0Sep = afterBoosterSep.withoutStage(0);
        assertNotNull(afterStage0Sep.activeStage());
        assertEquals(1, afterStage0Sep.activeStage().stageIndex());
        assertEquals(4, afterStage0Sep.totalHeightBlocks(), "Remaining stage 1 height should recompute to 4 blocks");

        RocketAssemblyGraph allSeparated = afterStage0Sep.withoutStage(1);
        assertNull(allSeparated.activeStage());
        assertEquals(0.0, allSeparated.totalDryMassKg(), 1.0e-6);
    }

    @Test
    @DisplayName("AttachRadialBoosterPayload STREAM_CODEC roundtrips cleanly with 0 unread bytes")
    void attachRadialBoosterPayloadRoundtrip() {
        AttachRadialBoosterPayload original = new AttachRadialBoosterPayload(
                new BlockPos(42, 68, -120),
                List.of(0, 1, 2),
                RadialSymmetry.FOUR_CARDINAL,
                CrossfeedMode.CONTINUOUS_CROSSFEED
        );
        ByteBuf buf = Unpooled.buffer();
        try {
            AttachRadialBoosterPayload.STREAM_CODEC.encode(buf, original);
            AttachRadialBoosterPayload decoded = AttachRadialBoosterPayload.STREAM_CODEC.decode(buf);
            assertEquals(0, buf.readableBytes());
            assertEquals(original, decoded);
        } finally {
            buf.release();
        }
    }
}
