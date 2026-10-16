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

package dev.galacticraft.api;

import dev.galacticraft.impl.network.c2s.TeamNamePayload;
import dev.galacticraft.impl.network.s2c.OxygenUpdatePayload;
import dev.galacticraft.impl.network.s2c.RemoveSatellitePayload;
import dev.galacticraft.mod.network.c2s.BubbleMaxPayload;
import dev.galacticraft.mod.network.c2s.ControlEntityPayload;
import dev.galacticraft.mod.network.s2c.BubbleSizePayload;
import dev.galacticraft.mod.network.s2c.FootprintRemovedPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAndProtocolBaselineTest {
    @Test
    public void apiPackageHasZeroModImports() throws IOException {
        // Working directory during JUnit execution is Galacticraft/run, so resolve ../src/main/java/dev/galacticraft/api
        Path apiDir = Path.of("..", "src", "main", "java", "dev", "galacticraft", "api").normalize();
        if (!Files.isDirectory(apiDir)) {
            apiDir = Path.of("src", "main", "java", "dev", "galacticraft", "api").normalize();
        }
        assertTrue(Files.isDirectory(apiDir), "Expected API source directory to exist at " + apiDir.toAbsolutePath());

        try (Stream<Path> files = Files.walk(apiDir)) {
            List<Path> javaFiles = files.filter(p -> p.toString().endsWith(".java")).toList();
            assertTrue(!javaFiles.isEmpty(), "Expected API source files to be present");

            for (Path file : javaFiles) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String trimmed = lines.get(i).trim();
                    assertTrue(
                            !trimmed.startsWith("import dev.galacticraft.mod."),
                            "Forbidden mod import in API file " + file + ":" + (i + 1) + " -> " + trimmed
                    );
                }
            }
        }
    }

    @Test
    public void apiConstantsAndResourceKeys() {
        ResourceLocation id = APIConstants.id("moon");
        assertEquals("galacticraft", id.getNamespace());
        assertEquals("moon", id.getPath());
    }

    @Test
    public void controlEntityPayloadStreamCodecRoundtrip() {
        ControlEntityPayload original = new ControlEntityPayload(0.75F, -0.5F, true, false, true, false, true, false, true);
        ByteBuf buf = Unpooled.buffer();
        try {
            ControlEntityPayload.STREAM_CODEC.encode(buf, original);
            ControlEntityPayload decoded = ControlEntityPayload.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded);
            assertEquals(0, buf.readableBytes(), "StreamCodec should leave zero unread bytes");
        } finally {
            buf.release();
        }
    }

    @Test
    public void footprintRemovedPacketStreamCodecRoundtrip() {
        FootprintRemovedPacket original = new FootprintRemovedPacket(42L, new BlockPos(12, 68, -34));
        ByteBuf buf = Unpooled.buffer();
        try {
            FootprintRemovedPacket.STREAM_CODEC.encode(buf, original);
            FootprintRemovedPacket decoded = FootprintRemovedPacket.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded);
            assertEquals(0, buf.readableBytes(), "StreamCodec should leave zero unread bytes");
        } finally {
            buf.release();
        }
    }

    @Test
    public void oxygenUpdatePayloadStreamCodecRoundtrip() {
        BitSet sectionBits = new BitSet(4096);
        sectionBits.set(0);
        sectionBits.set(512);
        sectionBits.set(4095);
        OxygenUpdatePayload.OxygenData[] sections = new OxygenUpdatePayload.OxygenData[]{
                new OxygenUpdatePayload.OxygenData((byte) 3, sectionBits)
        };
        OxygenUpdatePayload original = new OxygenUpdatePayload(123456789L, sections);

        ByteBuf buf = Unpooled.buffer();
        try {
            OxygenUpdatePayload.CODEC.encode(buf, original);
            OxygenUpdatePayload decoded = OxygenUpdatePayload.CODEC.decode(buf);
            assertEquals(original.chunk(), decoded.chunk());
            assertEquals(1, decoded.data().length);
            assertEquals((byte) 3, decoded.data()[0].section());
            assertEquals(sectionBits, decoded.data()[0].data());
            assertEquals(0, buf.readableBytes(), "StreamCodec should leave zero unread bytes");
        } finally {
            buf.release();
        }
    }

    @Test
    public void satelliteAndTeamNamePayloadsRoundtrip() {
        RemoveSatellitePayload removePayload = new RemoveSatellitePayload(APIConstants.id("earth_station_1"));
        TeamNamePayload teamPayload = new TeamNamePayload("Apollo 11 Crew");
        BubbleSizePayload bubbleSize = new BubbleSizePayload(new BlockPos(-10, 80, 25), 6.5F);
        BubbleMaxPayload bubbleMax = new BubbleMaxPayload((byte) 8);

        ByteBuf buf = Unpooled.buffer();
        try {
            RemoveSatellitePayload.CODEC.encode(buf, removePayload);
            assertEquals(removePayload, RemoveSatellitePayload.CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());

            TeamNamePayload.CODEC.encode(buf, teamPayload);
            assertEquals(teamPayload, TeamNamePayload.CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());

            BubbleSizePayload.STREAM_CODEC.encode(buf, bubbleSize);
            assertEquals(bubbleSize, BubbleSizePayload.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());

            BubbleMaxPayload.STREAM_CODEC.encode(buf, bubbleMax);
            assertEquals(bubbleMax, BubbleMaxPayload.STREAM_CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }

        assertThrows(IllegalArgumentException.class, () -> new TeamNamePayload("A".repeat(33)));
        assertArrayEquals(new byte[]{3}, new byte[]{sectionsIndexTest((byte) 3)});
    }

    private static byte sectionsIndexTest(byte b) {
        return b;
    }
}
