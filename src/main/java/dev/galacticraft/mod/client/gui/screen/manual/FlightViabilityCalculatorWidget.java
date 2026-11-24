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

package dev.galacticraft.mod.client.gui.screen.manual;

import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.content.rocket.part.RocketMaterialRegistry;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.content.rocket.physics.RocketFlightValidator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Interactive Planetary Flight Viability Calculator embedded in Tab 2 of the Aerospace Manual.
 * Builds a live {@link RocketFlightValidator.VehicleSnapshot} from selected origin, destination,
 * structural alloy, propulsion class, fuel tanks, power source, and protective coatings, then evaluates
 * it directly through {@link RocketFlightValidator#evaluate} (T/W, Tsiolkovsky dV, K, atm, H2SO4,
 * 1/r^2 solar power, and boil-off).
 */
@Environment(EnvType.CLIENT)
public final class FlightViabilityCalculatorWidget {
    private static final String[] ENGINE_CLASSES = {
            "RP-1 (Merlin)", "Methalox (Raptor)", "Hydrolox (RL-10)", "NERVA Nuclear", "DFD He-3 Fusion", "VASIMR Plasma"
    };
    private static final String[] ENGINE_FAMILIES = {
            "engine_merlin_rp1", "engine_raptor_methalox", "engine_rl10_hydrolox",
            "engine_nerva_nuclear", "engine_dfd_helium3", "engine_vasimr_xenon"
    };
    private static final String[] TANK_FAMILIES = {
            "tank_fuel_rp1", "tank_methalox_bulkhead", "tank_hydrolox_supercryo",
            "tank_hydrolox_supercryo", "tank_hydrolox_supercryo", "tank_xenon_copv"
    };
    private static final double[] ENGINE_THRUST_KN = {920.0, 2200.0, 1450.0, 1200.0, 3200.0, 2600.0};
    private static final long[] TANK_CAPACITY_BUCKETS = {24L, 48L, 64L, 72L, 84L, 96L};
    private static final String[] POWER_OPTIONS = {"Solar Array", "RTG Radioisotope", "D-3He Reactor"};

    private final List<PlanetaryEnvironment> planets = PlanetaryEnvironment.SOLAR_SYSTEM_PRESETS;
    private final List<RocketMaterialProfile> materials = RocketMaterialRegistry.structuralProfiles();

    private int originIdx = 0; // Earth
    private int destIdx = 1;   // Moon
    private int materialIdx = 3; // Ferroniobium
    private int engineIdx = 0;   // RP-1
    private int tankCount = 2;   // 1..5
    private int powerIdx = 0;    // Solar
    private boolean ablativeTps = false;
    private boolean leadCoat = false;
    private boolean sofiCryo = false;

    private RocketFlightValidator.FlightValidationReport cachedReport;
    private int lastX;
    private int lastY;
    private int lastW;

    public FlightViabilityCalculatorWidget() {
        if (this.materialIdx >= this.materials.size()) {
            this.materialIdx = 0;
        }
        recompute();
    }

    public int preferredHeight() {
        int hazardLines = this.cachedReport != null ? Math.min(4, this.cachedReport.hazards().size()) : 0;
        return 192 + hazardLines * 11;
    }

    public @NotNull List<String> buildPinSummaryLines() {
        PlanetaryEnvironment orig = this.planets.get(this.originIdx);
        PlanetaryEnvironment dest = this.planets.get(this.destIdx);
        RocketMaterialProfile mat = this.materials.get(this.materialIdx);
        return List.of(
                orig.displayName() + " -> " + dest.displayName(),
                "Hull: " + mat.displayName(),
                "Prop: " + ENGINE_CLASSES[this.engineIdx] + " x" + this.tankCount + " tanks",
                String.format(Locale.ROOT, "dV %.0f/%.0f | T/W %.2f",
                        this.cachedReport.maxDeltaV(), this.cachedReport.requiredDeltaV(), this.cachedReport.originTwr())
        );
    }

    public void recompute() {
        PlanetaryEnvironment orig = this.planets.get(this.originIdx);
        PlanetaryEnvironment dest = this.planets.get(this.destIdx);
        RocketMaterialProfile hull = this.materials.get(this.materialIdx);

        List<RocketMaterialTrait> traits = new ArrayList<>(hull.traits());
        if (this.ablativeTps) {
            traits.add(RocketMaterialTrait.ABLATIVE_TPS);
        }
        if (this.leadCoat) {
            traits.add(RocketMaterialTrait.ACID_RADIATION_SHIELD);
        }
        if (this.sofiCryo) {
            traits.add(RocketMaterialTrait.SOFI_INSULATION);
            if (this.engineIdx >= 4) {
                traits.add(RocketMaterialTrait.SUPERFLUID_CRYO_2K);
            }
        }
        if (this.engineIdx == 4) {
            traits.add(RocketMaterialTrait.ANEUTRONIC_HE3_PLASMA);
        }
        if (this.powerIdx == 2) {
            traits.add(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR);
        }

        RocketPartComposition comp = new RocketPartComposition(List.of(
                new RocketPartComposition.SlotMaterialEntry(0, "structural_hull", hull.primaryItemId() != null ? hull.primaryItemId() : Constant.id("compressed_steel"), traits)
        ));

        double hullTempK = Math.max(400.0, 900.0 + hull.thermalBonusKelvin() + (this.ablativeTps ? 600.0 : 0.0));
        double hullAtm = Math.max(1.0, 30.0 * hull.pressureMultiplier());
        double massMult = Math.max(0.35, 1.0 + hull.massModifier());
        double thrustMult = Math.max(0.8, 1.0 + hull.thrustBonus());
        double boilMult = this.sofiCryo ? (this.engineIdx >= 4 ? 0.0 : 0.15) : 1.0;

        List<RocketFlightValidator.PartSnapshot> parts = new ArrayList<>();

        // Command capsule
        RocketPartConfig cmdCfg = new RocketPartConfig(
                "capsule_sim", "capsule_orion", RocketPartCategory.COMMAND, 3, 3, 2, RocketLivery.WHITE, false, false);
        RocketPartStats cmdStats = new RocketPartStats(3200.0 * massMult, 0.0, 0L, hullTempK, hullAtm);
        parts.add(new RocketFlightValidator.PartSnapshot(cmdCfg, cmdStats, PartIntegrity.pristine(100), comp, boilMult));

        // Fuel tanks
        long dropletsPerTank = Math.round(TANK_CAPACITY_BUCKETS[this.engineIdx] * RocketFlightValidator.DROPLETS_PER_BUCKET * (1.0 + hull.capacityBonus()));
        for (int t = 0; t < this.tankCount; t++) {
            RocketPartConfig tankCfg = new RocketPartConfig(
                    "tank_sim_" + t, TANK_FAMILIES[this.engineIdx], RocketPartCategory.TANK, 3, 3, 2, RocketLivery.WHITE, false, false);
            RocketPartStats tankStats = new RocketPartStats(1800.0 * massMult, 0.0, dropletsPerTank, hullTempK, hullAtm);
            parts.add(new RocketFlightValidator.PartSnapshot(tankCfg, tankStats, PartIntegrity.pristine(100), comp, boilMult));
        }

        // Engine
        RocketPartConfig engCfg = new RocketPartConfig(
                "engine_sim", ENGINE_FAMILIES[this.engineIdx], RocketPartCategory.ENGINE, 3, 3, 2, RocketLivery.WHITE, false, false);
        RocketPartStats engStats = new RocketPartStats(2400.0 * massMult, ENGINE_THRUST_KN[this.engineIdx] * thrustMult, 0L, hullTempK + 400.0, hullAtm);
        parts.add(new RocketFlightValidator.PartSnapshot(engCfg, engStats, PartIntegrity.pristine(100), comp, boilMult));

        // Power utility module
        String powerFamily = switch (this.powerIdx) {
            case 1 -> "power_rtg_radioisotope";
            case 2 -> "power_rtg_fusion";
            default -> "power_solar_array";
        };
        RocketPartConfig pwrCfg = new RocketPartConfig(
                "power_sim", powerFamily, RocketPartCategory.UTILITY, 3, 3, 1, RocketLivery.WHITE, false, false);
        RocketPartStats pwrStats = new RocketPartStats(600.0 * massMult, 0.0, 0L, hullTempK, hullAtm);
        parts.add(new RocketFlightValidator.PartSnapshot(pwrCfg, pwrStats, PartIntegrity.pristine(100), comp, boilMult));

        long totalFuel = dropletsPerTank * this.tankCount;
        RocketFlightValidator.VehicleSnapshot vehicle = new RocketFlightValidator.VehicleSnapshot(parts, totalFuel, 50_000L, null);
        this.cachedReport = RocketFlightValidator.evaluate(vehicle, orig, dest);
    }

    public void render(@NotNull GuiGraphics graphics, @NotNull Font font, int x, int y, int w, int mouseX, int mouseY) {
        int h = preferredHeight();
        this.lastX = x;
        this.lastY = y;
        this.lastW = w;

        graphics.fill(x, y, x + w, y + h, 0xCC0D1829);
        graphics.fill(x, y, x + w, y + 1, 0xFFF59E0B);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF1E3A5F);

        String title = Component.translatable("manual.galacticraft.sim.title").getString();
        graphics.drawString(font, title, x + 6, y + 5, 0xFFFBBF24, false);

        boolean go = this.cachedReport.canLiftoffOrigin()
                && this.cachedReport.hasEnoughDeltaV()
                && !this.cachedReport.hasCriticalHazards();
        String status = go ? "GO FOR LAUNCH" : "NO-GO / HAZARD";
        int sw = font.width(status);
        graphics.fill(x + w - sw - 10, y + 3, x + w - 4, y + 14, go ? 0xFF065F46 : 0xFF7F1D1D);
        graphics.drawString(font, status, x + w - sw - 7, y + 5, go ? 0xFF6EE7B7 : 0xFFFCA5A5, false);

        // Selector rows (2 columns)
        int halfW = (w - 16) / 2;
        int rx1 = x + 6;
        int rx2 = x + 10 + halfW;

        drawCycleControl(graphics, font, rx1, y + 19, halfW, "Origin: " + this.planets.get(this.originIdx).displayName(), mouseX, mouseY);
        drawCycleControl(graphics, font, rx2, y + 19, halfW, "Target: " + this.planets.get(this.destIdx).displayName(), mouseX, mouseY);
        drawCycleControl(graphics, font, rx1, y + 35, halfW, "Hull: " + this.materials.get(this.materialIdx).displayName(), mouseX, mouseY);
        drawCycleControl(graphics, font, rx2, y + 35, halfW, "Eng: " + ENGINE_CLASSES[this.engineIdx], mouseX, mouseY);
        drawCycleControl(graphics, font, rx1, y + 51, halfW, "Tanks: " + this.tankCount + "x", mouseX, mouseY);
        drawCycleControl(graphics, font, rx2, y + 51, halfW, "Power: " + POWER_OPTIONS[this.powerIdx], mouseX, mouseY);

        // Coating toggles
        int togW = (w - 20) / 3;
        drawToggle(graphics, font, x + 6, y + 67, togW, "Ablative TPS", this.ablativeTps, mouseX, mouseY);
        drawToggle(graphics, font, x + 10 + togW, y + 67, togW, "Lead Anti-Acid", this.leadCoat, mouseX, mouseY);
        drawToggle(graphics, font, x + 14 + togW * 2, y + 67, togW, "SOFI / Cryo", this.sofiCryo, mouseX, mouseY);

        // Telemetry results box
        int resY = y + 85;
        graphics.fill(x + 6, resY, x + w - 6, resY + 94, 0xAA070E1A);
        graphics.renderOutline(x + 6, resY, w - 12, 94, 0xFF1F3B5C);

        RocketFlightValidator.FlightValidationReport r = this.cachedReport;
        drawMetricRow(graphics, font, x + 10, resY + 5, w - 20,
                String.format(Locale.ROOT, "T/W Origin: %.2f   |   T/W Target: %s",
                        r.originTwr(), Double.isInfinite(r.destinationTwr()) ? "0G OK" : String.format(Locale.ROOT, "%.2f", r.destinationTwr())),
                r.canLiftoffOrigin() ? 0xFF4ADE80 : 0xFFF87171);

        drawMetricRow(graphics, font, x + 10, resY + 17, w - 20,
                String.format(Locale.ROOT, "Delta-V: %.0f / %.0f m/s (Isp %.0fs)", r.maxDeltaV(), r.requiredDeltaV(), r.effectiveIspSeconds()),
                r.hasEnoughDeltaV() ? 0xFF4ADE80 : 0xFFFBBF24);

        drawMetricRow(graphics, font, x + 10, resY + 29, w - 20,
                String.format(Locale.ROOT, "Thermal: %.0fK hull vs %.0fK peak", r.minThermalToleranceKelvin(), r.destinationPeakTempKelvin()),
                r.thermalSafe() ? 0xFF4ADE80 : 0xFFF87171);

        drawMetricRow(graphics, font, x + 10, resY + 41, w - 20,
                String.format(Locale.ROOT, "Pressure: %.1f atm hull vs %.1f atm", r.minPressureToleranceAtm(), r.destinationPressureAtm()),
                r.pressureSafe() ? 0xFF4ADE80 : 0xFFF87171);

        drawMetricRow(graphics, font, x + 10, resY + 53, w - 20,
                String.format(Locale.ROOT, "Solar 1/r^2: %.4fx  |  Net Power: %+d GJ/t", r.solarIrradianceFactor(), r.netPowerPerTick()),
                r.powerDeficit() ? 0xFFFBBF24 : 0xFF38BDF8);

        drawMetricRow(graphics, font, x + 10, resY + 65, w - 20,
                r.acidHazard() ? "H2SO4 Acid: EXPOSED (add Lead/Lonsdaleite!)" : "Acid / Corrosion: PROTECTED",
                r.acidHazard() ? 0xFFF87171 : 0xFF4ADE80);

        String boilStr = r.boilOffDropletsPerMinute() <= 0.001
                ? "Cryo Boil-Off: ZERO"
                : String.format(Locale.ROOT, "Cryo Boil-Off: %.2f B/min", r.boilOffDropletsPerMinute() / RocketFlightValidator.DROPLETS_PER_BUCKET);
        drawMetricRow(graphics, font, x + 10, resY + 77, w - 20, boilStr,
                r.boilOffDropletsPerMinute() <= 0.001 ? 0xFF38BDF8 : 0xFFFBBF24);

        // Hazards list
        int hy = resY + 98;
        int maxH = Math.min(4, r.hazards().size());
        for (int i = 0; i < maxH; i++) {
            RocketFlightValidator.FlightHazard haz = r.hazards().get(i);
            int col = switch (haz.severity()) {
                case CRITICAL -> 0xFFF87171;
                case WARNING -> 0xFFFBBF24;
                case INFO -> 0xFF7DD3FC;
            };
            String line = "[" + haz.severity().name() + "] " + haz.summary();
            graphics.drawString(font, font.plainSubstrByWidth(line, w - 14), x + 8, hy + i * 11, col, false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 && button != 1) {
            return false;
        }
        int dir = button == 0 ? 1 : -1;
        int halfW = (this.lastW - 16) / 2;
        int rx1 = this.lastX + 6;
        int rx2 = this.lastX + 10 + halfW;

        if (inRect(mouseX, mouseY, rx1, this.lastY + 19, halfW, 13)) {
            this.originIdx = Math.floorMod(this.originIdx + dir, this.planets.size());
            recompute();
            return true;
        }
        if (inRect(mouseX, mouseY, rx2, this.lastY + 19, halfW, 13)) {
            this.destIdx = Math.floorMod(this.destIdx + dir, this.planets.size());
            recompute();
            return true;
        }
        if (inRect(mouseX, mouseY, rx1, this.lastY + 35, halfW, 13)) {
            this.materialIdx = Math.floorMod(this.materialIdx + dir, this.materials.size());
            recompute();
            return true;
        }
        if (inRect(mouseX, mouseY, rx2, this.lastY + 35, halfW, 13)) {
            this.engineIdx = Math.floorMod(this.engineIdx + dir, ENGINE_CLASSES.length);
            recompute();
            return true;
        }
        if (inRect(mouseX, mouseY, rx1, this.lastY + 51, halfW, 13)) {
            this.tankCount = 1 + Math.floorMod(this.tankCount - 1 + dir, 5);
            recompute();
            return true;
        }
        if (inRect(mouseX, mouseY, rx2, this.lastY + 51, halfW, 13)) {
            this.powerIdx = Math.floorMod(this.powerIdx + dir, POWER_OPTIONS.length);
            recompute();
            return true;
        }

        int togW = (this.lastW - 20) / 3;
        if (inRect(mouseX, mouseY, this.lastX + 6, this.lastY + 67, togW, 13)) {
            this.ablativeTps = !this.ablativeTps;
            recompute();
            return true;
        }
        if (inRect(mouseX, mouseY, this.lastX + 10 + togW, this.lastY + 67, togW, 13)) {
            this.leadCoat = !this.leadCoat;
            recompute();
            return true;
        }
        if (inRect(mouseX, mouseY, this.lastX + 14 + togW * 2, this.lastY + 67, togW, 13)) {
            this.sofiCryo = !this.sofiCryo;
            recompute();
            return true;
        }
        return false;
    }

    private static void drawCycleControl(GuiGraphics g, Font font, int x, int y, int w, String text, int mx, int my) {
        boolean hov = inRect(mx, my, x, y, w, 13);
        g.fill(x, y, x + w, y + 13, hov ? 0xFF1E3A5F : 0xFF132238);
        g.renderOutline(x, y, w, 13, hov ? 0xFF38BDF8 : 0xFF2B4C73);
        g.drawString(font, font.plainSubstrByWidth(text, w - 8), x + 4, y + 3, 0xFFE2E8F0, false);
    }

    private static void drawToggle(GuiGraphics g, Font font, int x, int y, int w, String label, boolean on, int mx, int my) {
        boolean hov = inRect(mx, my, x, y, w, 13);
        int bg = on ? 0xFF065F46 : (hov ? 0xFF1E3A5F : 0xFF132238);
        int border = on ? 0xFF34D399 : (hov ? 0xFF38BDF8 : 0xFF2B4C73);
        g.fill(x, y, x + w, y + 13, bg);
        g.renderOutline(x, y, w, 13, border);
        String txt = (on ? "✔ " : "☐ ") + label;
        g.drawString(font, font.plainSubstrByWidth(txt, w - 6), x + 3, y + 3, on ? 0xFFD1FAE5 : 0xFF94A3B8, false);
    }

    private static void drawMetricRow(GuiGraphics g, Font font, int x, int y, int maxW, String text, int color) {
        g.drawString(font, font.plainSubstrByWidth(text, maxW), x, y, color, false);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
