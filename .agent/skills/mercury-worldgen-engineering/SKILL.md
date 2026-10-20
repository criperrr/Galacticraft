---
name: mercury-worldgen-engineering
description: >-
  Exhaustive technical post-mortem, architectural reference, and engineering guide for
  high-performance planetary terrain generation, 3D monumental continuous caves, SurfaceRules,
  NoiseRouter pipelines, and vanilla-balanced carver connections in Minecraft 1.21+ (Fabric / Galacticraft).
---

# Procedural Planetary Worldgen & Monumental Caves Engineering: The Mercury Case Study

This document is an exhaustive technical reference and post-mortem detailing the architecture, mathematical principles, engine traps, failure modes, and battle-tested solutions developed while designing the procedural terrain and subterranean cave systems for planet Mercury in Minecraft 1.21+ (Galacticraft Fabric).

---

## 1. Executive Summary & Minecraft 1.21 Worldgen Architecture

In Minecraft 1.21+, chunk generation is divided into discrete, multithreaded pipeline stages executed across worker threads:

```mermaid
flowchart LR
    A["MultiNoise Biome Sampling\n(6D Climate Space)"] --> B["Noise Generation\n(DensityFunction Graph &\nCell Interpolation)"]
    B --> C["Surface Rules\n(Block Dressing &\nStratigraphy)"]
    C --> D["World Carvers\n(Caves, Canyons, Rifts)"]
    D --> E["Features & Ores\n(PlacedFeature / Veins)"]
```

### The Performance Budget
- **Target:** Chunk generation must complete under **50 ms** average per chunk during high-speed flight / teleports.
- **Initial Baseline on Mercury:** **7,272.5 ms** (completely unplayable, freezing server ticks).
- **Optimized Final State:** **31.60 ms** (p95: 52.42 ms) with monumental continuous caverns, pillars, lava lakes, and full geological stratigraphy intact.

---

## 2. Core Technical Difficulties & Proven Solutions

### 2.1. The 7,200ms Worldgen Freeze (Exponential Density Graph Evaluation)

#### The Problem
When custom 3D planetary caves (colossal lava tubes, megabreccia halls, tectonic chasms, hollow labyrinths, and pillars) were initially introduced, generation times spiked to **over 7.2 seconds per chunk**. Profiling revealed that 98.4% of the time was spent in `NoiseChunk.doFill()`.

#### Root Cause
In Minecraft's chunk generator, each chunk is partitioned into $4 \times 4 \times 8$ block cells ($4 \times 4$ horizontally, $8$ vertically). 
- If a function is marked with `DensityFunctions.interpolated(...)`, the engine samples the density graph **only at the 8 corners of each $4 \times 4 \times 8$ cell** and computes intermediate block densities via hardware-friendly trilinear interpolation.
- If 3D Perlin/Simplex noise nodes are placed inside `final_density` **without interpolation**, the engine evaluates the full trigonometric noise graph for **every single block voxel** ($16 \times 16 \times 256 = 65,536$ evaluations per chunk).
- Multiplying several 3D noises together created an exponential call-tree blowing past millions of float calculations per chunk.

#### The Solution
1. **Wrap 3D Base Functions in `DensityFunctions.interpolated(...)`**:
   Any 3D noise evaluated across space (such as cave cheese, noodle ridges, pillars, and roughness) must be wrapped in `DensityFunctions.interpolated(...)` or derived from interpolated coordinate bases.
2. **Apply Coordinate Caching**:
   - `flatCache(cache2d(...))` for 2D horizontal noise (continents, erosion, ridges, crawlway paths).
   - `cacheOnce(...)` for shared sub-expressions evaluated multiple times within the same vertical block sample.
3. **Result:** Chunk noise generation dropped from **7,272.5 ms to 31.6 ms** (a **230x speedup**).

---

### 2.2. The "Bread-Slicer" Bug (Critical Engine Trap)

#### The Problem
During optimization, wrapping `final_density` in `DensityFunctions.interpolated(finalDensity)` caused the planet to generate as dozens of parallel, razor-thin vertical slices separated by massive voids across entire continents ("Bread-Slicer" artifact).

#### Root Cause
Minecraft's `NoiseInterpolator` manages its own internal stepping variables (`stepX`, `stepY`, `stepZ`) across the cell grid.
- When `finalDensity` itself is wrapped in `interpolated(...)`, and internally consumes sub-expressions that also use `interpolated(...)`, the recursive calls reset the parent interpolator's internal step offsets on every query.
- This causes the $X$ or $Z$ axis interpolation factor to freeze at $0.0$, collapsing 3D space into parallel 2D planes.

#### The Golden Rule
```java
// ❌ FATAL ERROR - Causes Bread-Slicer parallel world slicing:
context.register(FINAL_DENSITY, DensityFunctions.interpolated(finalDensity));

// ✅ CORRECT PATTERN (Vanilla Overworld standard):
// Only sub-functions are wrapped in interpolated(...).
// finalDensity is passed directly into NoiseRouter through blendDensity:
return new NoiseRouter(
        ...,
        solidTerrain,                              // initialDensityWithoutJaggedness
        DensityFunctions.blendDensity(finalDensity), // finalDensity
        ...
);
```

---

### 2.3. The 90-Degree Orthogonal Cliffs ("Minecraft Stairs" Bug)

#### The Problem
Planetary escarpments, mountain crater rims, and lobate scarps generated as perfectly straight walls aligned to chunk boundaries, with unnatural 90-degree right-angle turns every 4 blocks.

#### Root Cause
Minecraft vanilla wraps `NoiseRouterData.CONTINENTS`, `SHIFT_X`, and `SHIFT_Z` in `flat_cache(...)`. 
- `flat_cache` evaluates the noise value **only once per $4 \times 4$ horizontal column**.
- When continental splines have steep slopes (e.g., elevation jumping 40 blocks over a small continental interval), the $4 \times 4$ column evaluation creates sharp, discrete step functions with horizontal flat tops and vertical sheer drops.

#### The Solution
1. **Continuous Un-cached Coordinates**:
   Compute shift offsets continuously without `flat_cache`:
   ```java
   DensityFunction shiftX = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.25, 0.0);
   DensityFunction shiftZ = DensityFunctions.noise(noiseRegistry.getOrThrow(Noises.SHIFT), 0.0, 0.25);
   ```
2. **Continuous Continental Sampler**:
   ```java
   DensityFunction continents = registerAndWrap(context, CONTINENTS,
           DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, noiseRegistry.getOrThrow(Noises.CONTINENTALNESS))
   );
   ```
3. **Organic 3D Roughness**:
   Inject an un-cached `BlendedNoise` perturbation ($0.25 - 0.40$ amplitude) into the vertical gradient to introduce rock shelves, overhangs, and natural erosion slopes that shatter orthogonal grid lines.

---

### 2.4. Cave Floor Surface Regolith Sarcophagus (`preliminarySurfaceLevel` Collision)

#### The Problem
Monumental caverns generated with correct shape and performance, but **their floors were carpeted with surface regolith, ice, and volatile deposits**. The game was treating the deep subterranean floors ($Y = -40 \text{ to } 40$) as if they were open to the planetary sky!

#### Root Cause: The Dual Surface Rule Mechanism
In Minecraft 1.21:
1. `SurfaceRules` evaluate whether a block is on the surface using conditions like `SurfaceRules.abovePreliminarySurface()`.
2. `abovePreliminarySurface()` does **not** perform raycasts to the sky. Instead, it queries `NoiseChunk.preliminarySurfaceLevel(x, z)`.
3. `preliminarySurfaceLevel` estimates the surface height by sampling `NoiseRouter.initialDensityWithoutJaggedness()` from the top of the world downward until density crosses $> 0$.
4. In our initial setup, `initialDensityWithoutJaggedness` pointed to `final_density` (which included cave carvings).
5. When the sampler stepped into a chunk with a monumental cave, the density above the cave was negative (air). The first solid surface it encountered was the **floor of the cave**!
6. The engine concluded: *"The surface of this planet is at $Y = -30$ (the cave floor)!"*
7. `abovePreliminarySurface()` returned `true` inside the cave, and `SURFACE_GENERATION` painted the cavern floor with surface regolith, completely ignoring underground basalt rules!

#### The Solution: True Crust Decoupling
Decouple the solid terrain crust from the cave carvings:
```java
// 1. Create a pure crust density function representing intact solid terrain:
DensityFunction solidTerrain = DensityFunctions.add(
        baseHeight,
        DensityFunctions.add(offset, DensityFunctions.add(smoothRelief, roughness3d))
);
context.register(Mercury.SOLID_TERRAIN, solidTerrain);

// 2. Wire initialDensityWithoutJaggedness EXCLUSIVELY to solidTerrain:
new NoiseRouter(
        ...,
        densityLookup.getOrThrow(Mercury.SOLID_TERRAIN), // initialDensityWithoutJaggedness
        DensityFunctions.blendDensity(finalDensity),     // finalDensity
        ...
);
```
- Because `initialDensityWithoutJaggedness` now samples `SOLID_TERRAIN`, `preliminarySurfaceLevel` always finds the true outer planetary crust ($Y = 120 \text{ to } 220$).
- Cave floors are far below `preliminarySurfaceLevel`, so `abovePreliminarySurface()` returns `false`.
- Surface regolith is strictly confined to the exterior planet, while cave floors correctly receive basalt, shock breccia, graphite rock, and lava lakes.

---

### 2.5. Subterranean Biome Leaking (MultiNoise 6D Distance Space)

#### The Problem
Certain surface biomes (like Caloris Basin or Inter-Crater Plains) were generating inside deep caverns, and conversely, underground biomes (like `Hollow Labyrinth`) were leaking onto high mountain peaks.

#### Root Cause
1. **Static Depth Function:** `mercuryDepth` was hardcoded to `yClampedGradient(128, -64, 0.0, 1.5)`. On high mountains ($Y = 180$), caves at $Y = 140$ had `depth = 0.0` (surface biome). In deep basins ($Y = 75$), open-air ground had `depth = 0.41` (cave biome).
2. **Euclidean Distance Space Fallback:** Minecraft's `MultiNoiseBiomeSource` selects biomes using 6D Euclidean distance:
   $$\text{dist}^2 = \Delta T^2 + \Delta H^2 + \Delta C^2 + \Delta E^2 + \Delta D^2 + \Delta W^2$$
   If subterranean biome definitions have gaps in Continentalness ($C$) or Weirdness ($W$), the sampler falls back to the nearest surface biome even when deep underground!

#### The Solution
1. **Dynamic Terrain-Normalized Depth:**
   Align `depth` with the local terrain surface (vanilla Overworld formula):
   ```java
   // Depth crosses 0.0 exactly at local terrain elevation:
   DensityFunction mercuryDepth = registerAndWrap(context, Mercury.DEPTH,
           DensityFunctions.add(mercuryBaseHeight, mercuryOffset)
   );
   ```
2. **Complete Subterranean Climate Space Coverage:**
   For `depth > 0.18`, underground biomes must span the full $[-1.0, 1.0]$ interval for Continentalness, Erosion, and Temperature, partitioning only by `Weirdness` (structural fault type) and `Depth` (crustal depth stratum).

---

### 2.6. The Hermetic Ceiling Trap vs The Swiss-Cheese Overkill

#### The Problem (Two Consecutive Extremes)
- **Extreme 1 (The Hermetic Ceiling):** To prevent massive 40-block cheese caves from destroying mountain ridges and plains, we added a ceiling taper (`surfaceCeilingTaper = (-25 * solidTerrain + 3.5).clamp(0, 3.5)`). While it successfully protected the surface, it created an impenetrable 15-block stone ceiling across 100% of the planet. **There were zero natural openings from the surface into the underworld.**
- **Extreme 2 (The Swiss-Cheese Overkill):** Removing the ceiling taper from noodle caves and stacking 5 different carvers with high probabilities ($0.05 - 0.15$) and oversized radii ($2.2 - 3.8$) caused the surface to look like shredded Swiss cheese, with parallel trenches and holes gouging every single hill.

#### The Solution: The Vanilla-Balanced Carver Architecture
In Minecraft vanilla, surface-to-underworld connections do **not** rely on noise tearing up the surface. They rely on an exquisitely tuned, sparse suite of `WorldCarver` instances:

1. **Keep ALL 3D Noise Caves Subterranean:**
   Wrap all large halls, lava tubes, and noodle caves under `surfaceCeilingTaper`. The planetary surface remains pristine, solid, and majestic.
2. **Standard Normal Cave Entrances (`WorldCarver.CAVE`):**
   - Use exact vanilla Overworld proportions:
     - `probability`: **`0.14285715f`** (1 in 7 chunks).
     - `horizontalRadiusMultiplier`: `UniformFloat.of(0.7f, 1.4f)` (natural walkable tunnels, not canyon craters).
     - `verticalRadiusMultiplier`: `UniformFloat.of(0.8f, 1.3f)`.
     - `height`: `UniformHeight.of(aboveBottom(8), absolute(180))`.
3. **Sparse, Dramatic Geological Ravines (`WorldCarver.CANYON`):**
   - In vanilla, canyons/ravines are rare geological highlights:
     - `probability`: **`0.015f`** (1 in 66 chunks).
     - `width`: `ConstantFloat.of(3.0f)`.
     - `shape`: `TrapezoidFloat.of(0.0f, 6.0f, 2.0f)`, 3 yaw sections, V-shaped profile.
4. **Thematic Openings Distributed by Biome (Max 1 Special Carver per Biome):**
   - *Inter-Crater Plains:* Cave network + Collapsed Rille ravine (1 in 66).
   - *Smooth Lava Plains:* Cave network + Rare circular collapse skylight (1 in 100).
   - *Caloris Basin:* Cave network + Deep tectonic shear fissure (1 in 80).
   - *Chaotic Terrain:* Cave network + Arched scarp portal on mountain slopes (1 in 70).
   - *Polar Dark Craters:* Cave network + Permanent shadow collapse pit (1 in 100).

---

## 3. Structural Stratigraphy of Planet Mercury

```
Elevation (Y)
 ▲
 │  [ Y = 160 - 220 ] Chaotic Mountain Ridges & Rupes (Impact mega-scarps, Basalt peaks)
 │  [ Y = 110 - 150 ] Inter-Crater Plains (Ultramafic Regolith, Compacted Regolith sub-layer)
 │  [ Y = 70 - 100 ]  Caloris Impact Basin & Smooth Plains (Low-albedo Graphite, Volatile Hollows)
 ┼─────────────────── CRUST INTERFACE (Surface Ceiling Taper Shields Crust) ───────────────────
 │  [ Y = 65 - 115 ]  Volatile Hollow Labyrinth (Porous graphite, white sublimation deposits)
 │  [ Y = 20 - 95 ]   Colossal Lava Tubes (40-block conduits, smooth cooling basalt terraces)
 │  [ Y = -30 - 110]  Tectonic Chasms (Vertical extensional rifts, sheared fault breccia)
 │  [ Y = -30 - 50 ]  Megabreccia Caverns (Monumental domed halls, impact breccia speleothems)
 │  [ Y = -64 - -10]  Magma Depths (Basaltic lava oceans, magma crusts, massive rock pillars)
 │  [ Y = -64 - -54]  Bedrock Foundation (Bedrock Shield preventing any void breaches)
 ▼
```

---

## 4. Checklist for Future Planetary Worldgen (Mars, Venus, Moons)

When implementing procedural terrain for any new celestial body:

- [ ] **Density Interpolation:** Are all 3D base functions wrapped in `DensityFunctions.interpolated(...)`?
- [ ] **No Final Interpolation:** Is `final_density` free from `DensityFunctions.interpolated(...)`?
- [ ] **Continuous Splines:** Are elevation spline coordinate samplers free from `flat_cache`?
- [ ] **Crust Shielding:** Does `initialDensityWithoutJaggedness` point to `SOLID_TERRAIN` (without caves)?
- [ ] **Dynamic Depth:** Is `depth` computed as `add(base_height, offset)` rather than a fixed vertical gradient?
- [ ] **Subterranean Climate Partitioning:** Do underground biomes cover 100% of Continentalness and Erosion for `depth > 0.18`?
- [ ] **Balanced Surface Openings:** Is cave carver probability set to $\approx 0.14$, with ravines/canyons set to $\approx 0.015$ and radii $\le 1.4$?
- [ ] **Bedrock Shield:** Is an absolute gradient applied at $Y = \text{bottom}$ to ensure carvers and 3D noises never break into the void?
- [ ] **Telemetry Verification:** Does chunk generation execute under 50 ms in `GCPerformanceTracker`?
