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

package dev.galacticraft.mod.client.render.astro.planets;

import dev.galacticraft.mod.client.render.astro.AstroMath;
import dev.galacticraft.mod.client.render.astro.planets.position.Center;
import dev.galacticraft.mod.client.render.astro.planets.position.PlayerPosition;
import dev.galacticraft.mod.client.render.astro.planets.position.Positioner;
import org.joml.Vector3d;

import java.util.HashMap;

public class Body {
    public Positioner positioner;
    protected String id;
    protected Body parent;
    protected HashMap<String, Body> children;
    protected String[] aliases = new String[0];

    public Body(String id, Positioner positioner) {
        this.positioner = positioner;
        this.id = id;
        this.parent = null;
        this.children = new HashMap<>();
    }

    public Vector3d getPosition(double time, Vector3d offset) {
        Vector3d pos = this.positioner.getPositionRelative(time, offset);
        return this.parent != null ? this.parent.getPosition(time, pos) : pos;
    }

    public Vector3d getPosition(double time) {
        return this.getPosition(time, new Vector3d(0.0, 0.0, 0.0));
    }

    protected Positioner genMountPositioner(String mountType, double[] mountArgs) {
        return switch (mountType) {
            case "walk" -> {
                if (mountArgs.length >= 4) {
                    yield new PlayerPosition(mountArgs[0], mountArgs[1], mountArgs[2], mountArgs[3]);
                } else if (mountArgs.length >= 1) {
                    yield new PlayerPosition(mountArgs[0]);
                } else {
                    yield new PlayerPosition();
                }
            }
            default -> new Center();
        };
    }

    public Body genMount(String mountType, double[] mountArgs) {
        Positioner mountPositioner = this.genMountPositioner(mountType, mountArgs);
        Body mount = new Body("OBSERVER", mountPositioner);
        mount.setParent(this);
        return mount;
    }

    public double getTotalMagnitude(StarBody sun, Body obs, double time) {
        return 44.0;
    }

    public Vector3d getAngularPosition(Body obs, double time) {
        Vector3d pos = this.getPosition(time);
        Vector3d toObs = pos.sub(obs.getPosition(time));
        return this.getAngularPosition(toObs, time);
    }

    public Vector3d getAngularPosition(Vector3d direction, double time) {
        direction = new Vector3d(direction).negate();
        direction.rotateX(Math.toRadians(-23.45F));
        double rightAscension = AstroMath.toDegrees(direction.angleSigned(new Vector3d(1.0, 0.0, 0.0), new Vector3d(0.0, -1.0, 0.0)));
        double declination = -AstroMath.atan2(direction.y, new Vector3d(direction.x, 0.0, direction.z).length());
        double distance = direction.length();
        return new Vector3d(rightAscension, declination, distance);
    }

    protected void render(StarBody sun, Body obs, double time) {
    }

    public void renderTree(StarBody sun, Body obs, double time) {
        if (this != obs) {
            this.render(sun, obs, time);
        }
        for (Body child : this.children.values()) {
            child.renderTree(sun, obs, time);
        }
    }

    public String getID() {
        return this.id;
    }

    public int getDepth() {
        int depth = 0;
        for (int i = 0; i < this.id.length(); i++) {
            if (this.id.charAt(i) == '.') {
                depth++;
            }
        }
        return depth;
    }

    @Override
    public String toString() {
        return this.getID();
    }

    public Body getParent() {
        return this.parent;
    }

    public void setParent(Body parent) {
        this.parent = parent;
    }

    public Body putChild(String localID, Body body) {
        String[] split = localID.split("\\.", 2);
        String childId = split[0];
        if (childId == null || childId.isEmpty()) {
            return null;
        }
        if (split.length > 1) {
            Body child = this.children.get(childId);
            if (child == null) {
                return null;
            }
            return child.putChild(split[1], body);
        }
        body.setParent(this);
        this.children.put(childId, body);
        return body;
    }

    public Body findChild(String localID) {
        String[] split = localID.split("\\.", 2);
        String childId = split[0];
        if (childId == null || childId.isEmpty()) {
            return null;
        }
        Body child = this.children.get(childId);
        if (split.length != 1 && child != null) {
            return child.findChild(split[1]);
        }
        return child;
    }

    public void removeChildren() {
        for (Body child : this.children.values()) {
            child.removeChildren();
        }
        this.children.clear();
    }

    public Body alias(String... aliases) {
        this.aliases = aliases;
        return this;
    }

    public String[] getAliases() {
        return this.aliases;
    }
}
