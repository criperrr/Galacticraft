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

package dev.galacticraft.mod.client.render.astro;

import java.util.Random;

public class AstroMath {
    public static double toRadians(double angle) {
        return Math.toRadians(angle);
    }

    public static double toDegrees(double angle) {
        return Math.toDegrees(angle);
    }

    public static float toRadians(float angle) {
        return angle / 360.0F * 2.0F * (float) Math.PI;
    }

    public static double sin(double angle) {
        return Math.sin(Math.toRadians(angle));
    }

    public static double cos(double angle) {
        return Math.cos(Math.toRadians(angle));
    }

    public static double tan(double angle) {
        return Math.tan(Math.toRadians(angle));
    }

    public static double asin(double value) {
        return Math.toDegrees(Math.asin(value));
    }

    public static double acos(double value) {
        return Math.toDegrees(Math.acos(value));
    }

    public static double atan(double value) {
        return Math.toDegrees(Math.atan(value));
    }

    public static double atan2(double y, double x) {
        return Math.toDegrees(Math.atan2(y, x));
    }

    public static double getApparentDiameter(double trueDiameter, double distance) {
        return 2.0 * atan2(trueDiameter / 2.0, distance);
    }

    public static double getLinear(double magn) {
        return Math.pow(2.512, -magn);
    }

    public static double getMagn(double linear) {
        return -Math.log(linear) / Math.log(2.512);
    }

    public static double magnadd(double a, double b) {
        return getMagn(getLinear(a) + getLinear(b));
    }

    public static double log10(double value) {
        return Math.log10(value);
    }

    public static double pow(double value, double power) {
        return Math.pow(value, power);
    }

    public static double square(double value) {
        return value * value;
    }

    public static double sqrt(double value) {
        return Math.sqrt(value);
    }

    public static double cbrt(double value) {
        return Math.cbrt(value);
    }

    public static double abs(double value) {
        return Math.abs(value);
    }

    public static double mod(double value, double div) {
        return (value % div + div) % div;
    }

    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static float min(float a, float b) {
        return Math.min(a, b);
    }

    public static double clamp(double value, double min, double max) {
        if (value > max) {
            return max;
        }
        return Math.max(value, min);
    }

    public static int clamp(int value, int min, int max) {
        if (value > max) {
            return max;
        }
        return Math.max(value, min);
    }

    public static double randCoord(Random rand) {
        return rand.nextDouble() * 2.0 - 1.0;
    }
}
