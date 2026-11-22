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

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.render.astro.AstroColorUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;

public class ModelManager {
    private static final HashMap<String, Model> MODELS = new HashMap<>();
    private static final HashMap<String, ModelData> MODEL_DATAS = new HashMap<>();
    public static final double[] NO_PHASE_REDUCTION = new double[]{0.0};
    public static final ModelData DEFAULTS = genDefaults();

    public static ModelData genDefaults() {
        ModelData defaults = new ModelData();
        defaults.parent = null;
        defaults.renderer = Model.Renderer.MAIN;
        defaults.texture = "galacticraft:textures/astro/placeholder.png";
        defaults.phases = null;
        defaults.glare = "galacticraft:textures/astro/glare.png";
        defaults.refColor = 0xFFFFFF;
        defaults.phaseCurveCoefficients = NO_PHASE_REDUCTION;
        return defaults;
    }

    public static void clearModelData() {
        MODEL_DATAS.clear();
        MODELS.clear();
    }

    public static void addModelData(ResourceLocation path, InputStream stream) {
        String id = path.getPath().replace("astro/planet_models/", "").replace(".json", "");
        Gson gson = new Gson();
        ModelData data = gson.fromJson(new JsonReader(new InputStreamReader(stream)), ModelData.class);
        if (data != null) {
            MODEL_DATAS.put(id, data);
        }
    }

    public static void genModels() {
        for (String id : MODEL_DATAS.keySet()) {
            ModelData data = MODEL_DATAS.get(id);
            int depth = 10;
            while (data.parent != null && depth-- > 0) {
                String parentID = data.parent;
                ModelData parentData = MODEL_DATAS.get(parentID);
                if (parentData != null) {
                    data.merge(parentData);
                } else {
                    Constant.LOGGER.warn("[AstroSky] Unable to load parent planet model: {}", parentID);
                    break;
                }
            }
            data.merge(DEFAULTS);
            MODELS.put(id, new Model(data));
        }
    }

    public static Model getModel(String id) {
        return MODELS.get(id);
    }

    public static class Model {
        public final Renderer renderer;
        private final ResourceLocation texture;
        private final ResourceLocation phases;
        private final ResourceLocation glare;
        private final Vector3f refColor;
        private final double[] phaseCurveCoefficients;

        public Model(ModelData data) {
            this.renderer = data.renderer;
            this.texture = data.texture != null ? ResourceLocation.parse(data.texture) : null;
            this.phases = data.phases != null ? ResourceLocation.parse(data.phases) : null;
            this.glare = data.glare != null ? ResourceLocation.parse(data.glare) : null;
            this.refColor = AstroColorUtils.fromHex(data.refColor);
            this.phaseCurveCoefficients = data.phaseCurveCoefficients;
        }

        public Renderer getRenderer() {
            return this.renderer;
        }

        public void useTexture() {
            if (this.texture != null) {
                RenderSystem.setShaderTexture(0, this.texture);
            }
        }

        public boolean hasPhases() {
            return this.phases != null;
        }

        public void usePhases() {
            if (this.phases != null) {
                RenderSystem.setShaderTexture(0, this.phases);
            }
        }

        public void useGlare() {
            if (this.glare != null) {
                RenderSystem.setShaderTexture(0, this.glare);
            }
        }

        public long useColor(Vector3f color) {
            float red = this.refColor.x > 0.0F ? color.x / this.refColor.x : color.x;
            float green = this.refColor.y > 0.0F ? color.y / this.refColor.y : color.y;
            float blue = this.refColor.z > 0.0F ? color.z / this.refColor.z : color.z;
            Vector3f shaderColor = new Vector3f(red, green, blue);
            float magnitude = shaderColor.get(shaderColor.maxComponent());
            int renderCount = Math.max(1, Mth.ceil(magnitude));
            shaderColor.mul(magnitude / (float) renderCount);
            RenderSystem.setShaderColor(shaderColor.x, shaderColor.y, shaderColor.z, 1.0F);
            return Math.min(4, renderCount);
        }

        public double getPhaseReduction(double phase) {
            if (this.phaseCurveCoefficients == null) {
                return 0.0;
            }
            double total = 0.0;
            for (int i = 0; i < this.phaseCurveCoefficients.length; i++) {
                total += this.phaseCurveCoefficients[i] * Math.pow(phase, (double) i);
            }
            return total;
        }

        public enum Renderer {
            MAIN,
            SUN,
            MOON
        }
    }

    public static class ModelData {
        public String parent = null;
        public Model.Renderer renderer = null;
        public String texture = null;
        public String phases = null;
        public String glare = null;
        public double maxAlbedo = Double.NaN;
        public int refColor = -1;
        public double[] phaseCurveCoefficients = null;

        private static boolean isDefined(Object obj) {
            if (obj instanceof Integer i) {
                return i != -1;
            }
            if (obj instanceof Double d) {
                return !Double.isNaN(d);
            }
            return obj != null;
        }

        public void merge(ModelData parent) {
            this.parent = null;
            try {
                for (Field field : ModelData.class.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object childValue = field.get(this);
                    Object parentValue = field.get(parent);
                    if (!isDefined(childValue) && isDefined(parentValue)) {
                        field.set(this, parentValue);
                    }
                }
            } catch (IllegalAccessException e) {
                Constant.LOGGER.error("[AstroSky] Failed to merge planet model data", e);
            }
        }
    }
}
