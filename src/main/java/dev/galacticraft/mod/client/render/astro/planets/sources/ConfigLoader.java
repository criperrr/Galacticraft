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

import dev.galacticraft.mod.client.render.astro.planets.Body;
import dev.galacticraft.mod.client.render.astro.planets.ConfigurablePlanet;
import dev.galacticraft.mod.client.render.astro.planets.ConfigurableStar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ConfigLoader implements Source {
    private final HashMap<Supplier<List<? extends Object>>, String> configListEntries = new HashMap<>();

    public ConfigLoader mountList(Supplier<List<? extends Object>> supplier, String mount) {
        this.configListEntries.put(supplier, mount);
        return this;
    }

    @Override
    public Iterable<Body> getBodies() {
        ArrayList<Body> bodies = new ArrayList<>();
        for (Map.Entry<Supplier<List<? extends Object>>, String> entry : this.configListEntries.entrySet()) {
            List<? extends Object> configList = entry.getKey().get();
            String mount = entry.getValue();
            for (Object config : configList) {
                this.loadEntry(bodies, config, mount, null);
            }
        }
        return bodies;
    }

    public void loadEntry(List<Body> bodies, Object config, String mount, Object primary) {
        if (config instanceof ConfigurablePlanet planet) {
            Body body = planet.genPlanet(mount);
            bodies.add(body);
            for (ConfigurablePlanet.ConfigurableMoon moon : planet.moons) {
                this.loadEntry(bodies, moon, body.getID(), config);
            }
        } else if (config instanceof ConfigurablePlanet.ConfigurableMoon moon) {
            Body body = moon.genPlanet(mount, (ConfigurablePlanet) primary);
            bodies.add(body);
        } else if (config instanceof ConfigurableStar star) {
            Body body = star.genStar(mount);
            bodies.add(body);
            for (ConfigurablePlanet planet : star.planets) {
                this.loadEntry(bodies, planet, body.getID(), null);
            }
            for (ConfigurableStar.ConfigurableCompanion companion : star.companions) {
                this.loadEntry(bodies, companion, body.getID(), null);
            }
        } else if (config instanceof ConfigurableStar.ConfigurableCompanion star) {
            Body body = star.genStar(mount);
            bodies.add(body);
            for (ConfigurablePlanet planet : star.planets) {
                this.loadEntry(bodies, planet, body.getID(), null);
            }
            for (ConfigurableStar.ConfigurableCompanion companion : star.companions) {
                this.loadEntry(bodies, companion, body.getID(), null);
            }
        }
    }
}
