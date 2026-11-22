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

package dev.galacticraft.mod.client.render.astro.planets.sources;

import dev.galacticraft.mod.client.render.astro.AstroColorUtils;
import dev.galacticraft.mod.client.render.astro.planets.Body;
import dev.galacticraft.mod.client.render.astro.planets.Planet;
import dev.galacticraft.mod.client.render.astro.planets.position.Orbit;

import java.util.List;

public class EssentialBodies implements Source {
    private static final List<Body> BODIES = List.of(
            new Planet(
                    "sun.earth",
                    new Orbit(1.0, 1.0, 0.0167, 1.57).sync(0.0, 348.73936, 102.94719, 101.56419),
                    "moon",
                    0.0,
                    0.367,
                    AstroColorUtils.fromHex(0x2F97FF)
            ).alias("earth")
    );

    @Override
    public Iterable<Body> getBodies() {
        return BODIES;
    }
}
