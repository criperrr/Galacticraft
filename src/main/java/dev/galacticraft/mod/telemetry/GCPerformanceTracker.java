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

package dev.galacticraft.mod.telemetry;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deep real-time performance telemetry for Galacticraft and Vanilla Minecraft engine.
 * Tracks per-dimension chunk generation stage latencies and client-side render latencies.
 */
public final class GCPerformanceTracker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SLOW_CHUNK_THRESHOLD_NANOS = 80_000_000L; // 80ms threshold for slow chunk warning
    private static final long LOG_INTERVAL_NANOS = 5_000_000_000L; // Every 5 seconds

    private static final Map<String, DimensionMetrics> DIMENSION_METRICS = new ConcurrentHashMap<>();
    private static final RenderMetrics CLIENT_RENDER_METRICS = new RenderMetrics();
    private static final AtomicLong LAST_LOG_TIME = new AtomicLong(System.nanoTime());
    private static final AtomicLong TOTAL_CHUNKS_GENERATED = new AtomicLong(0);

    private static BufferedWriter csvWriter = null;
    private static boolean csvInitialized = false;

    private GCPerformanceTracker() {}

    private static synchronized void initCsv() {
        if (csvInitialized) return;
        csvInitialized = true;
        try {
            File logDir = new File("run/logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            File csvFile = new File(logDir, "galacticraft-performance.csv");
            csvWriter = new BufferedWriter(new FileWriter(csvFile, true));
            if (csvFile.length() == 0) {
                csvWriter.write("Timestamp,Dimension,ChunkX,ChunkZ,TotalMs,NoiseMs,CarversMs,SurfaceMs,FeaturesMs,BiomesMs\n");
                csvWriter.flush();
            }
        } catch (IOException e) {
            LOGGER.error("[GC-Perf] Failed to initialize performance CSV log", e);
        }
    }

    public static void recordStage(String dimension, String stage, long nanos) {
        DIMENSION_METRICS.computeIfAbsent(dimension, DimensionMetrics::new).record(stage, nanos);
    }

    public static void recordSlowChunk(String dimension, int chunkX, int chunkZ, long totalNanos, long biomesNanos, long noiseNanos, long surfaceNanos, long carverNanos, long featureNanos) {
        TOTAL_CHUNKS_GENERATED.incrementAndGet();
        double totalMs = totalNanos / 1_000_000.0;
        double noiseMs = noiseNanos / 1_000_000.0;
        double carverMs = carverNanos / 1_000_000.0;
        double surfaceMs = surfaceNanos / 1_000_000.0;
        double featureMs = featureNanos / 1_000_000.0;
        double biomesMs = biomesNanos / 1_000_000.0;

        if (totalNanos > SLOW_CHUNK_THRESHOLD_NANOS) {
            LOGGER.warn("[GC-Perf] ⚠️ SLOW CHUNK [{}, {}] in '{}': {:.1f}ms (Noise: {:.1f}ms [{:.0f}%], Carvers: {:.1f}ms [{:.0f}%], Surface: {:.1f}ms [{:.0f}%], Features: {:.1f}ms [{:.0f}%], Biomes: {:.1f}ms [{:.0f}%])",
                    chunkX, chunkZ, dimension, totalMs,
                    noiseMs, (noiseMs / totalMs) * 100.0,
                    carverMs, (carverMs / totalMs) * 100.0,
                    surfaceMs, (surfaceMs / totalMs) * 100.0,
                    featureMs, (featureMs / totalMs) * 100.0,
                    biomesMs, (biomesMs / totalMs) * 100.0);
        }

        initCsv();
        if (csvWriter != null) {
            try {
                csvWriter.write(String.format(Locale.ROOT, "%d,%s,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                        System.currentTimeMillis(), dimension, chunkX, chunkZ, totalMs, noiseMs, carverMs, surfaceMs, featureMs, biomesMs));
                csvWriter.flush();
            } catch (IOException ignored) {}
        }

        checkPeriodicLog();
    }

    public static void recordClientRender(String stage, long nanos) {
        CLIENT_RENDER_METRICS.record(stage, nanos);
    }

    private static void checkPeriodicLog() {
        long now = System.nanoTime();
        long last = LAST_LOG_TIME.get();
        if (now - last > LOG_INTERVAL_NANOS && LAST_LOG_TIME.compareAndSet(last, now)) {
            printPeriodicSummary();
        }
    }

    public static void printPeriodicSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================== [GALACTICRAFT PERFORMANCE TELEMETRY] ====================\n");
        for (DimensionMetrics dm : DIMENSION_METRICS.values()) {
            sb.append(dm.formatSummary()).append("\n");
        }
        sb.append(CLIENT_RENDER_METRICS.formatSummary()).append("\n");
        sb.append("===============================================================================");
        LOGGER.info(sb.toString());
    }

    public static List<String> buildChatReport() {
        List<String> lines = new ArrayList<>();
        lines.add("§6=== Galacticraft Performance Metrics ===");
        for (DimensionMetrics dm : DIMENSION_METRICS.values()) {
            lines.addAll(dm.formatChatLines());
        }
        lines.addAll(CLIENT_RENDER_METRICS.formatChatLines());
        lines.add("§6=======================================");
        return lines;
    }

    public static final class DimensionMetrics {
        private final String dimension;
        private final Map<String, RollingStats> stages = new ConcurrentHashMap<>();

        public DimensionMetrics(String dimension) {
            this.dimension = dimension;
        }

        public void record(String stage, long nanos) {
            stages.computeIfAbsent(stage, k -> new RollingStats()).record(nanos);
        }

        public String formatSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Dim: %-25s | Stages:\n", dimension));
            for (Map.Entry<String, RollingStats> entry : stages.entrySet()) {
                RollingStats stats = entry.getValue();
                sb.append(String.format("   • %-12s: Avg: %6.2fms | p95: %6.2fms | Max: %6.2fms (samples: %d)\n",
                        entry.getKey(), stats.getAvgMs(), stats.getP95Ms(), stats.getMaxMs(), stats.count));
            }
            return sb.toString();
        }

        public List<String> formatChatLines() {
            List<String> list = new ArrayList<>();
            list.add("§eDim: §f" + dimension);
            for (Map.Entry<String, RollingStats> entry : stages.entrySet()) {
                RollingStats stats = entry.getValue();
                list.add(String.format("  §7%-10s §aAvg: %.1fms §ep95: %.1fms §cMax: %.1fms",
                        entry.getKey() + ":", stats.getAvgMs(), stats.getP95Ms(), stats.getMaxMs()));
            }
            return list;
        }
    }

    public static final class RenderMetrics {
        private final Map<String, RollingStats> stages = new ConcurrentHashMap<>();

        public void record(String stage, long nanos) {
            stages.computeIfAbsent(stage, k -> new RollingStats()).record(nanos);
        }

        public String formatSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Client Rendering:\n");
            for (Map.Entry<String, RollingStats> entry : stages.entrySet()) {
                RollingStats stats = entry.getValue();
                sb.append(String.format("   • %-12s: Avg: %6.2fms | p95: %6.2fms | Max: %6.2fms\n",
                        entry.getKey(), stats.getAvgMs(), stats.getP95Ms(), stats.getMaxMs()));
            }
            return sb.toString();
        }

        public List<String> formatChatLines() {
            List<String> list = new ArrayList<>();
            list.add("§bClient Render Metrics:");
            for (Map.Entry<String, RollingStats> entry : stages.entrySet()) {
                RollingStats stats = entry.getValue();
                list.add(String.format("  §7%-10s §aAvg: %.1fms §ep95: %.1fms §cMax: %.1fms",
                        entry.getKey() + ":", stats.getAvgMs(), stats.getP95Ms(), stats.getMaxMs()));
            }
            return list;
        }
    }

    public static final class RollingStats {
        private static final int CAPACITY = 128;
        private final long[] ring = new long[CAPACITY];
        private int writeIndex = 0;
        private int count = 0;
        private long maxNanos = 0;

        public synchronized void record(long nanos) {
            ring[writeIndex] = nanos;
            writeIndex = (writeIndex + 1) % CAPACITY;
            if (count < CAPACITY) count++;
            if (nanos > maxNanos) maxNanos = nanos;
        }

        public synchronized double getAvgMs() {
            if (count == 0) return 0.0;
            long sum = 0;
            for (int i = 0; i < count; i++) {
                sum += ring[i];
            }
            return (sum / (double) count) / 1_000_000.0;
        }

        public synchronized double getP95Ms() {
            if (count == 0) return 0.0;
            long[] copy = new long[count];
            System.arraycopy(ring, 0, copy, 0, count);
            Arrays.sort(copy);
            int idx = (int) Math.min(count - 1, Math.round(count * 0.95));
            return copy[idx] / 1_000_000.0;
        }

        public synchronized double getMaxMs() {
            return maxNanos / 1_000_000.0;
        }
    }
}
