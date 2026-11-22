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

package dev.galacticraft.mod.client.render.rocket.part;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketPartGuiPreviewMathTest {
    @Test
    void fitScaleKeepsSmallAndColossalPartsWithinViewportAndSafeDepth() {
        // 1x1x1 CubeSat in PartAssemblerScreen viewport (126x72)
        float smallScale = RocketPartGuiPreviewRenderer.computeFitScale(
                1.0F, 1.0F, 1.0F,
                126, 72,
                5.0F, 1.0F, 56.0F
        );
        assertTrue(smallScale > 20.0F, "1x1x1 part should scale up crisply in preview");
        assertTrue(0.5F * (float) Math.sqrt(3.0) * smallScale <= 56.01F, "3D half-depth must stay within safe Z budget");

        // 11x11x1 Thrust Puck (wide pancake) in PartAssemblerScreen viewport (126x72)
        float wideScale = RocketPartGuiPreviewRenderer.computeFitScale(
                11.0F, 1.0F, 11.0F,
                126, 72,
                5.0F, 1.0F, 56.0F
        );
        float wideHorizSpanPx = (float) Math.hypot(11.0, 11.0) * wideScale;
        assertTrue(wideHorizSpanPx <= 116.5F, "11x11 wide part must fit horizontally inside 126px viewport with padding");
        assertTrue(0.5F * (float) Math.sqrt(121.0 + 1.0 + 121.0) * wideScale <= 56.01F, "Wide part Z half-depth must stay within safe budget");

        // Tall 5x5 multi-stage rocket (height 18 blocks) in VAB viewport (140x149), zoomed in to max
        float tallScaleZoomed = RocketPartGuiPreviewRenderer.computeFitScale(
                5.0F, 18.0F, 5.0F,
                140, 149,
                8.0F, 2.5F, 128.0F
        );
        float tallHalfDepthPx = 0.5F * (float) Math.sqrt(25.0 + 324.0 + 25.0) * tallScaleZoomed;
        assertTrue(tallHalfDepthPx <= 128.05F, "Zoomed tall rocket must never exceed safe orthographic Z budget");
    }

    @Test
    void projectedBandHitTestMatchesExactBounds() {
        var band = new RocketPartGuiPreviewRenderer.ProjectedBand(2, 40, 60, 100, 95, true, false);
        assertTrue(band.contains(40, 60));
        assertTrue(band.contains(70, 75));
        assertTrue(band.contains(100, 95));
        assertFalse(band.contains(39.9, 75));
        assertFalse(band.contains(70, 59.9));
        assertFalse(band.contains(100.1, 75));
        assertFalse(band.contains(70, 95.1));
    }
}
