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

package dev.galacticraft.mod.client.render.vehicle;

import dev.galacticraft.mod.Constant;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource-cached Wavefront {@code .obj} parser for modular vehicles and parts.
 * Supports {@code v}, {@code vt}, {@code vn}, {@code o}/{@code g} named groups,
 * triangles and quads (triangles are cleanly duplicated on the 4th vertex for MC quad buffers).
 */
public final class ObjModelLoader {
    private static final Map<ResourceLocation, ObjMeshModel> CACHE = new ConcurrentHashMap<>();

    private ObjModelLoader() {
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static @NotNull ObjMeshModel getOrLoad(@NotNull ResourceLocation location) {
        return CACHE.computeIfAbsent(location, ObjModelLoader::loadInternal);
    }

    private static @NotNull ObjMeshModel loadInternal(@NotNull ResourceLocation location) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            Constant.LOGGER.warn("Missing vehicle OBJ model resource: {}", location);
            return new ObjMeshModel(Map.of());
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
            return parse(reader);
        } catch (Exception e) {
            Constant.LOGGER.error("Failed to parse vehicle OBJ model: {}", location, e);
            return new ObjMeshModel(Map.of());
        }
    }

    public static @NotNull ObjMeshModel parse(@NotNull BufferedReader reader) throws Exception {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        Map<String, List<ObjMeshModel.Quad>> groupQuads = new LinkedHashMap<>();

        String currentGroup = "default";
        groupQuads.put(currentGroup, new ArrayList<>());

        String rawLine;
        while ((rawLine = reader.readLine()) != null) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "o", "g" -> {
                    currentGroup = parts.length > 1 ? parts[1] : "default";
                    groupQuads.computeIfAbsent(currentGroup, k -> new ArrayList<>());
                }
                case "v" -> {
                    if (parts.length >= 4) {
                        positions.add(new float[]{
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        });
                    }
                }
                case "vt" -> {
                    if (parts.length >= 3) {
                        uvs.add(new float[]{
                                Float.parseFloat(parts[1]),
                                1.0F - Float.parseFloat(parts[2]) // Flip V for OpenGL/MC top-left origin
                        });
                    }
                }
                case "vn" -> {
                    if (parts.length >= 4) {
                        normals.add(new float[]{
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        });
                    }
                }
                case "f" -> {
                    int n = parts.length - 1;
                    if (n < 3) {
                        break;
                    }
                    ObjMeshModel.Vertex[] verts = new ObjMeshModel.Vertex[n];
                    for (int i = 0; i < n; i++) {
                        verts[i] = parseFaceVertex(parts[i + 1], positions, uvs, normals);
                    }
                    // Compute geometric face normal if missing
                    if (verts[0].nx() == 0.0F && verts[0].ny() == 0.0F && verts[0].nz() == 0.0F) {
                        Vector3f e1 = new Vector3f(verts[1].x() - verts[0].x(), verts[1].y() - verts[0].y(), verts[1].z() - verts[0].z());
                        Vector3f e2 = new Vector3f(verts[2].x() - verts[0].x(), verts[2].y() - verts[0].y(), verts[2].z() - verts[0].z());
                        Vector3f fn = e1.cross(e2);
                        if (fn.lengthSquared() > 1.0E-6F) {
                            fn.normalize();
                            for (int i = 0; i < n; i++) {
                                ObjMeshModel.Vertex v = verts[i];
                                verts[i] = new ObjMeshModel.Vertex(v.x(), v.y(), v.z(), v.u(), v.v(), fn.x, fn.y, fn.z);
                            }
                        }
                    }
                    List<ObjMeshModel.Quad> list = groupQuads.computeIfAbsent(currentGroup, k -> new ArrayList<>());
                    if (n == 3) {
                        list.add(new ObjMeshModel.Quad(verts[0], verts[1], verts[2], verts[2]));
                    } else if (n == 4) {
                        list.add(new ObjMeshModel.Quad(verts[0], verts[1], verts[2], verts[3]));
                    } else {
                        // Fan-triangulate convex N-gons into degenerate quads
                        for (int i = 1; i + 1 < n; i++) {
                            list.add(new ObjMeshModel.Quad(verts[0], verts[i], verts[i + 1], verts[i + 1]));
                        }
                    }
                }
                default -> {
                }
            }
        }

        Map<String, ObjMeshModel.Group> built = new LinkedHashMap<>();
        for (Map.Entry<String, List<ObjMeshModel.Quad>> entry : groupQuads.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                built.put(entry.getKey(), new ObjMeshModel.Group(entry.getKey(), List.copyOf(entry.getValue())));
            }
        }
        return new ObjMeshModel(built);
    }

    private static ObjMeshModel.Vertex parseFaceVertex(
            String token,
            List<float[]> positions,
            List<float[]> uvs,
            List<float[]> normals
    ) {
        String[] idx = token.split("/");
        int vi = resolveIndex(Integer.parseInt(idx[0]), positions.size());
        float[] p = positions.get(vi);
        float u = 0.0F;
        float v = 0.0F;
        if (idx.length > 1 && !idx[1].isEmpty()) {
            int ti = resolveIndex(Integer.parseInt(idx[1]), uvs.size());
            float[] t = uvs.get(ti);
            u = t[0];
            v = t[1];
        }
        float nx = 0.0F;
        float ny = 0.0F;
        float nz = 0.0F;
        if (idx.length > 2 && !idx[2].isEmpty()) {
            int ni = resolveIndex(Integer.parseInt(idx[2]), normals.size());
            float[] norm = normals.get(ni);
            nx = norm[0];
            ny = norm[1];
            nz = norm[2];
        }
        return new ObjMeshModel.Vertex(p[0], p[1], p[2], u, v, nx, ny, nz);
    }

    private static int resolveIndex(int raw, int size) {
        int idx = raw > 0 ? raw - 1 : size + raw;
        return Math.max(0, Math.min(size - 1, idx));
    }
}
