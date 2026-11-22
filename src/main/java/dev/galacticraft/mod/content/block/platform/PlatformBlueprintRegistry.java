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

package dev.galacticraft.mod.content.block.platform;

import dev.galacticraft.api.platform.PlatformSize;
import dev.galacticraft.api.platform.PlatformType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Authoritative registry of full-scale structural blueprints for all 8 modular rocket platforms
 * (2 horizontal integration hangars and 6 vertical launch platforms).
 */
public final class PlatformBlueprintRegistry {
    public static final PlatformBlueprint HORIZONTAL_1X1 = buildHorizontal1x1();
    public static final PlatformBlueprint HORIZONTAL_3X3 = buildHorizontal3x3();
    public static final PlatformBlueprint VERTICAL_1X1 = buildVertical1x1();
    public static final PlatformBlueprint VERTICAL_3X3 = buildVertical3x3();
    public static final PlatformBlueprint VERTICAL_5X5 = buildVertical5x5();
    public static final PlatformBlueprint VERTICAL_7X7 = buildVertical7x7();
    public static final PlatformBlueprint VERTICAL_9X9 = buildVertical9x9();
    public static final PlatformBlueprint VERTICAL_11X11 = buildVertical11x11();

    private static final Map<String, PlatformBlueprint> BY_KEY;

    static {
        Map<String, PlatformBlueprint> map = new LinkedHashMap<>();
        register(map, HORIZONTAL_1X1);
        register(map, HORIZONTAL_3X3);
        register(map, VERTICAL_1X1);
        register(map, VERTICAL_3X3);
        register(map, VERTICAL_5X5);
        register(map, VERTICAL_7X7);
        register(map, VERTICAL_9X9);
        register(map, VERTICAL_11X11);
        BY_KEY = Collections.unmodifiableMap(map);
    }

    private PlatformBlueprintRegistry() {
    }

    private static void register(Map<String, PlatformBlueprint> map, PlatformBlueprint blueprint) {
        map.put(blueprint.size().modelKey(blueprint.type()), blueprint);
    }

    /**
     * Resolves the blueprint for the requested platform archetype and size, clamping unsupported
     * combinations (such as horizontal platforms larger than 3x3) cleanly.
     */
    public static PlatformBlueprint get(PlatformType type, PlatformSize size) {
        PlatformType resolvedType = type != null ? type : PlatformType.VERTICAL;
        PlatformSize clampedSize = PlatformSize.clampForType(resolvedType, size);
        PlatformBlueprint blueprint = BY_KEY.get(clampedSize.modelKey(resolvedType));
        return blueprint != null ? blueprint : VERTICAL_1X1;
    }

    public static Collection<PlatformBlueprint> all() {
        return BY_KEY.values();
    }

    public static Map<String, PlatformBlueprint> byModelKey() {
        return BY_KEY;
    }

    /**
     * Horizontal 1x1 Integration Hangar:
     * 3x9 pad deck, 6 truss columns supporting twin crane rails + 2 overhead bridge beams at Y=3,
     * and 4 upgrade sockets on the West wing.
     */
    private static PlatformBlueprint buildHorizontal1x1() {
        PlatformBlueprint.Builder b = PlatformBlueprint.builder(PlatformType.HORIZONTAL, PlatformSize.SIZE_1X1);
        // 3x9 pad deck: X in [-1..1], Z in [-4..4], Y = 0
        b.fill(-1, 1, 0, 0, -4, 4, PlatformBlockRole.PAD);
        // 6 gantry truss columns at X = +-2, Z in {-4, 0, 4} supporting rails at Y = 3
        for (int x : new int[]{-2, 2}) {
            for (int z : new int[]{-4, 0, 4}) {
                b.fill(x, x, 0, 2, z, z, PlatformBlockRole.TRUSS);
            }
        }
        // Twin crane rails along X = +-2, Z in [-4..4] at Y = 3 + dual overhead bridges across X in [-1..1], Z in {-2, 2}
        b.fill(-2, -2, 3, 3, -4, 4, PlatformBlockRole.CRANE_RAIL);
        b.fill(2, 2, 3, 3, -4, 4, PlatformBlockRole.CRANE_RAIL);
        b.fill(-1, 1, 3, 3, -2, -2, PlatformBlockRole.CRANE_RAIL);
        b.fill(-1, 1, 3, 3, 2, 2, PlatformBlockRole.CRANE_RAIL);
        // 2 upgrade sockets on West wing (X = -3, Z in {-1, 1}, Y = 0)
        b.add(-3, 0, -1, PlatformBlockRole.UPGRADE_SOCKET);
        b.add(-3, 0, 1, PlatformBlockRole.UPGRADE_SOCKET);
        b.clearance(-1, 1, -4, 1, 2, 4);
        return b.build();
    }

    /**
     * Horizontal 3x3 Integration Hangar:
     * 5x13 pad deck, 8 truss columns supporting twin crane rails + 3 overhead bridge beams at Y=5,
     * and 4 upgrade sockets on the West wing.
     */
    private static PlatformBlueprint buildHorizontal3x3() {
        PlatformBlueprint.Builder b = PlatformBlueprint.builder(PlatformType.HORIZONTAL, PlatformSize.SIZE_3X3);
        // 5x13 pad deck: X in [-2..2], Z in [-6..6], Y = 0
        b.fill(-2, 2, 0, 0, -6, 6, PlatformBlockRole.PAD);
        // 8 truss columns at X = +-3, Z in {-6, -2, 2, 6} supporting rails at Y = 5
        for (int x : new int[]{-3, 3}) {
            for (int z : new int[]{-6, -2, 2, 6}) {
                b.fill(x, x, 0, 4, z, z, PlatformBlockRole.TRUSS);
            }
        }
        // Twin crane rails along X = +-3, Z in [-6..6] at Y = 5 + 3 overhead bridge rails across X in [-2..2]
        b.fill(-3, -3, 5, 5, -6, 6, PlatformBlockRole.CRANE_RAIL);
        b.fill(3, 3, 5, 5, -6, 6, PlatformBlockRole.CRANE_RAIL);
        for (int z : new int[]{-4, 0, 4}) {
            b.fill(-2, 2, 5, 5, z, z, PlatformBlockRole.CRANE_RAIL);
        }
        // 4 upgrade sockets on West wing (X = -4, Z in {-3, -1, 1, 3}, Y = 0)
        for (int z : new int[]{-3, -1, 1, 3}) {
            b.add(-4, 0, z, PlatformBlockRole.UPGRADE_SOCKET);
        }
        b.clearance(-2, 1, -6, 2, 4, 6);
        return b.build();
    }

    /**
     * Vertical 1x1 Launch Platform:
     * 3x3 pad deck, twin rear truss towers at (+-2, 0..5, -2) with base and mid-level tie-beams,
     * overhead crane rail across X in [-2..2] at Y=6, Z=-2, and 3 upgrade sockets on the West wing.
     */
    private static PlatformBlueprint buildVertical1x1() {
        PlatformBlueprint.Builder b = PlatformBlueprint.builder(PlatformType.VERTICAL, PlatformSize.SIZE_1X1);
        b.fill(-1, 1, 0, 0, -1, 1, PlatformBlockRole.PAD);
        b.fill(-2, -2, 0, 5, -2, -2, PlatformBlockRole.TRUSS);
        b.fill(2, 2, 0, 5, -2, -2, PlatformBlockRole.TRUSS);
        b.fill(-1, 1, 0, 0, -2, -2, PlatformBlockRole.TRUSS);
        b.fill(-1, 1, 3, 3, -2, -2, PlatformBlockRole.TRUSS);
        b.fill(-2, 2, 6, 6, -2, -2, PlatformBlockRole.CRANE_RAIL);
        b.fill(-3, -3, 0, 0, -1, 1, PlatformBlockRole.UPGRADE_SOCKET);
        b.clearance(-1, 1, -1, 1, 5, 1);
        return b.build();
    }

    /**
     * Vertical 3x3 Launch Platform:
     * 5x5 pad deck, twin rear truss towers at (+-3, 0..7, -3) with base/mid cross-braces + side
     * stabilizers, overhead crane rail + forward arms at Y=8, and 4 upgrade sockets on the West wing.
     */
    private static PlatformBlueprint buildVertical3x3() {
        PlatformBlueprint.Builder b = PlatformBlueprint.builder(PlatformType.VERTICAL, PlatformSize.SIZE_3X3);
        b.fill(-2, 2, 0, 0, -2, 2, PlatformBlockRole.PAD);
        b.fill(-3, -3, 0, 7, -3, -3, PlatformBlockRole.TRUSS);
        b.fill(3, 3, 0, 7, -3, -3, PlatformBlockRole.TRUSS);
        b.fill(-2, 2, 0, 0, -3, -3, PlatformBlockRole.TRUSS);
        b.fill(-2, 2, 4, 4, -3, -3, PlatformBlockRole.TRUSS);
        b.fill(-3, -3, 0, 4, 0, 0, PlatformBlockRole.TRUSS);
        b.fill(3, 3, 0, 4, 0, 0, PlatformBlockRole.TRUSS);
        b.fill(-3, 3, 8, 8, -3, -3, PlatformBlockRole.CRANE_RAIL);
        b.fill(-3, -3, 8, 8, -2, 0, PlatformBlockRole.CRANE_RAIL);
        b.fill(3, 3, 8, 8, -2, 0, PlatformBlockRole.CRANE_RAIL);
        for (int z : new int[]{-2, -1, 1, 2}) {
            b.add(-4, 0, z, PlatformBlockRole.UPGRADE_SOCKET);
        }
        b.clearance(-2, 1, -2, 2, 7, 2);
        return b.build();
    }

    /**
     * Vertical 5x5 Launch Platform:
     * 7x7 pad deck, West-NW 2x2 gantry tower + East-NE auxiliary mast + mid-height service bridge,
     * crane rail cantilever + twin forward arms at Y=12, and 6 upgrade sockets on the East wing.
     */
    private static PlatformBlueprint buildVertical5x5() {
        PlatformBlueprint.Builder b = PlatformBlueprint.builder(PlatformType.VERTICAL, PlatformSize.SIZE_5X5);
        b.fill(-3, 3, 0, 0, -3, 3, PlatformBlockRole.PAD);
        b.fill(-5, -4, 0, 12, -5, -4, PlatformBlockRole.TRUSS);
        b.fill(4, 4, 0, 11, -4, -4, PlatformBlockRole.TRUSS);
        b.fill(-3, 3, 6, 6, -4, -4, PlatformBlockRole.TRUSS);
        b.fill(-3, 4, 12, 12, -4, -4, PlatformBlockRole.CRANE_RAIL);
        b.fill(-4, -4, 12, 12, -3, 1, PlatformBlockRole.CRANE_RAIL);
        b.fill(4, 4, 12, 12, -3, 1, PlatformBlockRole.CRANE_RAIL);
        for (int z : new int[]{-3, -2, -1, 1, 2, 3}) {
            b.add(5, 0, z, PlatformBlockRole.UPGRADE_SOCKET);
        }
        b.clearance(-3, 1, -3, 3, 11, 3);
        return b.build();
    }

    /**
     * Vertical 7x7 Red LUT Launch Platform:
     * 9x9 pad deck, Red LUT 2x2 gantry tower + East service mast + mid-height bridge at Y=9,
     * top crane rail crossbeam + forward arms at Y=18, and 8 upgrade sockets on the East wing.
     */
    private static PlatformBlueprint buildVertical7x7() {
        PlatformBlueprint.Builder b = PlatformBlueprint.builder(PlatformType.VERTICAL, PlatformSize.SIZE_7X7);
        b.fill(-4, 4, 0, 0, -4, 4, PlatformBlockRole.PAD);
        b.fill(-6, -5, 0, 18, -6, -5, PlatformBlockRole.TRUSS);
        b.fill(5, 5, 0, 17, -5, -5, PlatformBlockRole.TRUSS);
        b.fill(-4, 4, 9, 9, -5, -5, PlatformBlockRole.TRUSS);
        b.fill(-4, 5, 18, 18, -5, -5, PlatformBlockRole.CRANE_RAIL);
        b.fill(-5, -5, 18, 18, -4, 1, PlatformBlockRole.CRANE_RAIL);
        b.fill(5, 5, 18, 18, -4, 1, PlatformBlockRole.CRANE_RAIL);
        for (int z : new int[]{-4, -3, -2, -1, 1, 2, 3, 4}) {
            b.add(6, 0, z, PlatformBlockRole.UPGRADE_SOCKET);
        }
        b.clearance(-4, 1, -4, 4, 17, 4);
        return b.build();
    }

    /**
     * Vertical 9x9 Starbase Mechazilla Platform:
     * 11x11 pad deck, Mechazilla 2x2 tower (X in [-7,-6], Z in [-1..0], Y=0..30) with reinforced
     * base buttresses + chopstick crane rails at Y=28, and 10 upgrade sockets on the East wing.
     */
    private static PlatformBlueprint buildVertical9x9() {
        PlatformBlueprint.Builder b = PlatformBlueprint.builder(PlatformType.VERTICAL, PlatformSize.SIZE_9X9);
        b.fill(-5, 5, 0, 0, -5, 5, PlatformBlockRole.PAD);
        b.fill(-7, -6, 0, 30, -1, 0, PlatformBlockRole.TRUSS);
        b.fill(-7, -6, 0, 8, -3, -2, PlatformBlockRole.TRUSS);
        b.fill(-7, -6, 0, 8, 1, 2, PlatformBlockRole.TRUSS);
        b.fill(-5, 2, 28, 28, -3, -3, PlatformBlockRole.CRANE_RAIL);
        b.fill(-5, 2, 28, 28, 3, 3, PlatformBlockRole.CRANE_RAIL);
        b.fill(-6, -6, 20, 20, -4, -2, PlatformBlockRole.CRANE_RAIL);
        b.fill(-6, -6, 20, 20, 1, 3, PlatformBlockRole.CRANE_RAIL);
        for (int z : new int[]{-5, -4, -3, -2, -1, 1, 2, 3, 4, 5}) {
            b.add(7, 0, z, PlatformBlockRole.UPGRADE_SOCKET);
        }
        b.clearance(-5, 1, -5, 5, 27, 5);
        return b.build();
    }

    /**
     * Vertical 11x11 Twin Colossal Tower Platform:
     * 13x13 pad deck, Twin Colossal Towers at X=+-8 with mid-height rail wings at Y=20
     * and overhead sky-crane bridge at Y=38, plus 14 upgrade sockets on the East wing.
     */
    private static PlatformBlueprint buildVertical11x11() {
        PlatformBlueprint.Builder b = PlatformBlueprint.builder(PlatformType.VERTICAL, PlatformSize.SIZE_11X11);
        b.fill(-6, 6, 0, 0, -6, 6, PlatformBlockRole.PAD);
        b.fill(-8, -8, 0, 38, -1, 0, PlatformBlockRole.TRUSS);
        b.fill(8, 8, 0, 38, -1, 0, PlatformBlockRole.TRUSS);
        b.fill(-8, -8, 0, 10, -2, -2, PlatformBlockRole.TRUSS);
        b.fill(8, 8, 0, 10, -2, -2, PlatformBlockRole.TRUSS);
        b.fill(-7, 7, 38, 38, -1, 0, PlatformBlockRole.CRANE_RAIL);
        b.fill(-7, -7, 20, 20, -4, 4, PlatformBlockRole.CRANE_RAIL);
        b.fill(7, 7, 20, 20, -4, 4, PlatformBlockRole.CRANE_RAIL);
        b.fill(9, 9, 0, 0, -6, 7, PlatformBlockRole.UPGRADE_SOCKET);
        b.clearance(-6, 1, -6, 6, 37, 6);
        return b.build();
    }
}
