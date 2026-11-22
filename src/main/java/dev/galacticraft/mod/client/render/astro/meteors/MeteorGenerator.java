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

package dev.galacticraft.mod.client.render.astro.meteors;

import dev.galacticraft.mod.client.render.astro.AstroSkyEngine;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class MeteorGenerator {
    private static Random rand = new Random("METEOR".hashCode());
    private static final ArrayList<Meteor> QUEUE = new ArrayList<>();
    private static int queueIndex = 0;
    private static final ArrayList<Meteor> ACTIVE = new ArrayList<>();
    private static long lastDay = Long.MIN_VALUE;

    private static double randomSignedDouble() {
        return rand.nextDouble(-1.0, 1.0);
    }

    private static Vector3d randomVector3d() {
        return new Vector3d(randomSignedDouble(), randomSignedDouble(), randomSignedDouble());
    }

    private static Meteor genSporadic(double time) {
        return genRandom(time, randomVector3d());
    }

    private static Meteor genRandom(double time, Vector3d radiant) {
        double alpha = rand.nextDouble();
        Vector3d direction = randomVector3d();
        Vector3d color = randomVector3d();
        color.x = 0.8 + 0.2 * Math.cbrt(Math.abs(color.x));
        color.y = 0.75 + 0.25 * Math.cbrt(Math.abs(color.y));
        color.z = 0.7 + 0.3 * Math.cbrt(Math.abs(color.z));
        return new Meteor(time, alpha, radiant, direction, color.normalize());
    }

    private static double randomStartTime() {
        return 24000.0 * (double) lastDay - 18000.0 + 24000.0 * rand.nextDouble();
    }

    public static long calcToday() {
        return (AstroSkyEngine.getWorldTime() + 18000L) / 24000L;
    }

    public static boolean shouldGenForNight() {
        return calcToday() != lastDay;
    }

    public static void genForNight() {
        lastDay = calcToday();
        rand = new Random("METEOR".hashCode() + lastDay);
        QUEUE.clear();
        ACTIVE.clear();

        for (MeteorShower shower : MeteorShower.SHOWERS) {
            if (shower.isActive()) {
                double intensity = shower.getIntensity();
                long count = Math.round(intensity * 0.1 * 20.0 * ((double) AstroSkyEngine.CONFIG.totalMeteors / 100.0));
                Vector3d radiant = shower.getRadiant();
                for (long i = 0L; i < count; i++) {
                    QUEUE.add(genRandom(randomStartTime(), radiant));
                }
            }
        }

        long count = Math.round((double) AstroSkyEngine.CONFIG.sporadicMeteors * 0.1 * 20.0 * ((double) AstroSkyEngine.CONFIG.totalMeteors / 100.0));
        for (long i = 0L; i < count; i++) {
            QUEUE.add(genSporadic(randomStartTime()));
        }

        QUEUE.sort(Comparator.comparingDouble(Meteor::getStart));
        queueIndex = 0;
    }

    private static Meteor popMeteor(double time) {
        if (queueIndex >= QUEUE.size()) {
            return null;
        }
        Meteor meteor = QUEUE.get(queueIndex);
        if (meteor.isBegun(time)) {
            queueIndex++;
            return meteor;
        }
        return null;
    }

    public static ArrayList<Meteor> getActive(double time) {
        Meteor meteor;
        while ((meteor = popMeteor(time)) != null) {
            ACTIVE.add(meteor);
        }
        ACTIVE.removeIf(am -> am.isFinished(time));
        return ACTIVE;
    }
}
