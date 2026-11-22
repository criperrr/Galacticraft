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

import java.util.List;

public class AstroCurveConfig {
    public List<Double> xs;
    public List<Double> ys;
    public Interpolation interpolation;

    public AstroCurveConfig(List<Double> xs, List<Double> ys, Interpolation interpolation) {
        this.xs = xs;
        this.ys = ys;
        this.interpolation = interpolation;
    }

    public boolean isValid() {
        return this.xs != null && this.ys != null && this.xs.size() == this.ys.size() && this.xs.size() >= 2;
    }

    private int getInterval(double x) {
        for (int i = 0; i < this.xs.size() - 1; i++) {
            double start = this.xs.get(i);
            double end = this.xs.get(i + 1);
            if (x >= start && x < end) {
                return i;
            }
        }
        return -1;
    }

    private double getLinear(double x) {
        int i = this.getInterval(x);
        double x1 = this.xs.get(i);
        double y1 = this.ys.get(i);
        double x2 = this.xs.get(i + 1);
        double y2 = this.ys.get(i + 1);
        double k = (y2 - y1) / (x2 - x1);
        return y1 + k * (x - x1);
    }

    private double getQuadratic(double x) {
        int i = Math.max(Math.min(this.getInterval(x), this.xs.size() - 2), 1);
        double x0 = this.xs.get(i - 1);
        double y0 = this.ys.get(i - 1);
        double x1 = this.xs.get(i);
        double y1 = this.ys.get(i);
        double x2 = this.xs.get(i + 1);
        double y2 = this.ys.get(i + 1);
        double l0 = (x - x1) * (x - x2) / (x0 - x1) / (x0 - x2);
        double l1 = (x - x0) * (x - x2) / (x1 - x0) / (x1 - x2);
        double l2 = (x - x0) * (x - x1) / (x2 - x0) / (x2 - x1);
        return y0 * l0 + y1 * l1 + y2 * l2;
    }

    public double get(double x) {
        if (!this.isValid() || Double.isNaN(x)) {
            return 0.0;
        }
        if (x <= this.xs.get(0)) {
            return this.ys.get(0);
        }
        int last = this.xs.size() - 1;
        if (x >= this.xs.get(last)) {
            return this.ys.get(last);
        }
        if (this.interpolation == Interpolation.QUADRATIC && this.xs.size() >= 3) {
            return this.getQuadratic(x);
        }
        return this.getLinear(x);
    }

    public enum Interpolation {
        LINEAR,
        QUADRATIC
    }
}
