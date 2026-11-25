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

package dev.galacticraft.mod.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.client.gui.screen.manual.AerospaceManualScreen;
import dev.galacticraft.mod.client.gui.screen.manual.AerospaceTabletAnimator;
import dev.galacticraft.mod.content.rocket.physics.PlanetaryEnvironment;
import dev.galacticraft.mod.manual.ManualEntry;
import dev.galacticraft.mod.manual.ManualPage;
import dev.galacticraft.mod.manual.ManualTab;
import dev.galacticraft.mod.manual.PlanetarySurveyPrecision;
import dev.galacticraft.mod.research.ClientResearchCache;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchNode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 3D Aerospace Tablet item &amp; first-person two-handed renderer for {@code GCItems.AEROSPACE_MANUAL}.
 *
 * <p>Renders a rugged metallic 3D sci-fi tablet with beveled titanium chassis, rubberized grip handles,
 * comms antenna, status LEDs, and a <b>live, non-interactive glass screen</b> displaying the exact tab
 * and chapter content the player was last viewing in {@link AerospaceManualScreen}.
 */
@Environment(EnvType.CLIENT)
public final class AerospaceTabletItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    public static final AerospaceTabletItemRenderer INSTANCE = new AerospaceTabletItemRenderer();

    private static final ResourceLocation SURFACE_TEX = Constant.id("textures/item/aerospace_tablet_surface.png");

    // 3D Tablet dimensions in model-space units (16:10 widescreen aerospace pad)
    private static final float BODY_W = 0.82F;
    private static final float BODY_H = 0.54F;
    private static final float BODY_D = 0.046F;
    private static final float SCREEN_W = 0.68F;
    private static final float SCREEN_H = 0.42F;

    private boolean renderingLiveScreen = false;

    private AerospaceTabletItemRenderer() {
    }

    @Override
    public void render(
            ItemStack stack,
            ItemDisplayContext mode,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        poseStack.pushPose();
        boolean drawLiveScreen = true;

        switch (mode) {
            case GUI -> {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-24.0F));
                poseStack.scale(1.06F, 1.06F, 1.06F);
            }
            case GROUND -> {
                poseStack.translate(0.5F, 0.36F, 0.5F);
                poseStack.scale(0.62F, 0.62F, 0.62F);
            }
            case FIXED -> {
                poseStack.translate(0.5F, 0.5F, 0.48F);
                poseStack.scale(0.95F, 0.95F, 0.95F);
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.52F, 0.42F, 0.36F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(172.0F));
                poseStack.scale(0.58F, 0.58F, 0.58F);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.48F, 0.42F, 0.36F);
                poseStack.mulPose(Axis.XP.rotationDegrees(-24.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(188.0F));
                poseStack.scale(0.58F, 0.58F, 0.58F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                poseStack.translate(0.5F, 0.5F, 0.5F);
            }
            default -> {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                poseStack.scale(0.75F, 0.75F, 0.75F);
                drawLiveScreen = false;
            }
        }

        renderTablet3D(poseStack, buffer, light, overlay, drawLiveScreen);
        poseStack.popPose();
    }

    /**
     * Renders the first-person 3D Aerospace Tablet:
     * <ul>
     *   <li><b>Idle ({@code anim == 0}):</b> Held cleanly in <b>ONE hand only</b> on the right/left side
     *       with the screen 100% unobstructed so the player can read the last tab/page while walking.</li>
     *   <li><b>Opening ({@code RMB / G}, {@code anim: 0 -> 1}):</b> Smoothly glides to eye center while the
     *       <b>second hand rises up</b> to grip the opposite titanium side handle and the camera zooms in.</li>
     * </ul>
     */
    public void renderFirstPersonTablet(
            @NotNull AbstractClientPlayer player,
            @NotNull HumanoidArm primaryArm,
            float partialTicks,
            float equippedProgress,
            float swingProgress,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int combinedLight
    ) {
        float anim = AerospaceTabletAnimator.getProgress();
        // Once the interactive tablet GUI is locked in at full zoom, skip redrawing the 3D hand behind it
        if (AerospaceTabletAnimator.screenRevealAlpha() >= 0.94F) {
            return;
        }

        float idleFactor = 1.0F - anim;
        float sideSign = primaryArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        float equipDrop = equippedProgress * 0.40F * idleFactor;
        float swingSin = Mth.sin(Mth.sqrt(swingProgress) * Mth.PI) * idleFactor;
        float breath = Mth.sin((player.tickCount + partialTicks) * 0.08F) * 0.005F * idleFactor;

        poseStack.pushPose();

        // Idle: held comfortably off to one side in ONE hand. Open: smoothly raised to dead center!
        float tx = Mth.lerp(anim, sideSign * 0.31F, 0.0F);
        float ty = Mth.lerp(anim, -0.26F - equipDrop + breath, -0.015F);
        float tz = Mth.lerp(anim, -0.74F, -0.44F);
        float yawDeg = Mth.lerp(anim, -sideSign * 18.0F, 0.0F);
        float pitchDeg = Mth.lerp(anim, -14.0F + swingSin * 8.0F, 0.0F);
        float scale = Mth.lerp(anim, 0.78F, 1.00F);

        poseStack.translate(tx, ty, tz);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg));
        poseStack.scale(scale, scale, scale);

        // Render the 3D Aerospace Tablet + live backlit screen first
        renderTablet3D(poseStack, buffer, combinedLight, OverlayTexture.NO_OVERLAY, true);

        // Render primary holding hand behind/outer edge of the titanium handle (NEVER obstructing the glass!)
        if (!player.isInvisible()) {
            renderSideGrippingHand(player, primaryArm, 1.0F, poseStack, buffer, combinedLight);

            // Second hand ONLY rises up to grip the opposite handle when opening (RMB / G)!
            if (anim > 0.02F) {
                HumanoidArm secondaryArm = primaryArm.getOpposite();
                renderSideGrippingHand(player, secondaryArm, anim, poseStack, buffer, combinedLight);
            }
        }

        poseStack.popPose();
    }

    /**
     * Renders a single player forearm cleanly positioned along the outer-back titanium grip rail
     * ({@code z < 0}, outside {@code |x| > BODY_W * 0.5}) so it grips the edge without ever clipping
     * in front of the backlit display.
     */
    private void renderSideGrippingHand(
            AbstractClientPlayer player,
            HumanoidArm arm,
            float raiseFactor,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int combinedLight
    ) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (!(dispatcher.getRenderer(player) instanceof PlayerRenderer playerRenderer)) {
            return;
        }

        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float dropY = (1.0F - raiseFactor) * 0.55F;

        poseStack.pushPose();
        // Anchor strictly outside the left/right bezel and behind the screen plane (z = -0.14)
        poseStack.translate(side * (BODY_W * 0.50F + 0.14F), -0.34F - dropY, -0.14F);
        poseStack.mulPose(Axis.YP.rotationDegrees(side * 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(38.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(side * -34.0F));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        if (arm == HumanoidArm.RIGHT) {
            playerRenderer.renderRightHand(poseStack, buffer, combinedLight, player);
        } else {
            playerRenderer.renderLeftHand(poseStack, buffer, combinedLight, player);
        }
        poseStack.popPose();
    }

    /**
     * Builds and renders the shaded 3D Aerospace Tablet chassis + live screen content in model space.
     */
    public void renderTablet3D(
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int light,
            int overlay,
            boolean drawLiveScreen
    ) {
        VertexConsumer solid = buffer.getBuffer(RenderType.entitySolid(SURFACE_TEX));
        PoseStack.Pose pose = poseStack.last();

        float hw = BODY_W * 0.5F;
        float hh = BODY_H * 0.5F;
        float hd = BODY_D * 0.5F;

        // 1. Main metallic gunmetal-titanium chassis
        drawBox(solid, pose, -hw, -hh, -hd, hw, hh, hd, 0xFF233247, light, overlay);

        // 2. Lighter brushed-alloy front bezel lip
        drawBox(solid, pose, -hw + 0.015F, -hh + 0.015F, hd * 0.55F, hw - 0.015F, hh - 0.015F, hd + 0.006F, 0xFF364B66, light, overlay);

        // 3. Left & Right ergonomic tactical rubberized grip handles
        float gripW = 0.055F;
        drawBox(solid, pose, -hw - 0.025F, -hh + 0.03F, -hd - 0.008F, -hw + gripW * 0.5F, hh - 0.03F, hd + 0.014F, 0xFF131B26, light, overlay);
        drawBox(solid, pose, hw - gripW * 0.5F, -hh + 0.03F, -hd - 0.008F, hw + 0.025F, hh - 0.03F, hd + 0.014F, 0xFF131B26, light, overlay);

        // 4. Glowing cyan side telemetry strips on grips (FULL_BRIGHT)
        drawBox(solid, pose, -hw - 0.027F, -0.10F, -0.004F, -hw - 0.020F, 0.10F, 0.010F, 0xFF38BDF8, LightTexture.FULL_BRIGHT, overlay);
        drawBox(solid, pose, hw + 0.020F, -0.10F, -0.004F, hw + 0.027F, 0.10F, 0.010F, 0xFF38BDF8, LightTexture.FULL_BRIGHT, overlay);

        // 5. Compact top-right orbital telemetry antenna stub
        drawBox(solid, pose, hw - 0.09F, hh, -0.010F, hw - 0.065F, hh + 0.065F, 0.010F, 0xFF475569, light, overlay);
        drawBox(solid, pose, hw - 0.092F, hh + 0.055F, -0.012F, hw - 0.063F, hh + 0.072F, 0.012F, 0xFF38BDF8, LightTexture.FULL_BRIGHT, overlay);

        // 6. Top bezel front camera lens & green power status LED
        drawBox(solid, pose, -0.014F, hh - 0.038F, hd + 0.004F, 0.014F, hh - 0.018F, hd + 0.010F, 0xFF0A101A, light, overlay);
        drawBox(solid, pose, 0.030F, hh - 0.034F, hd + 0.005F, 0.044F, hh - 0.022F, hd + 0.010F, 0xFF4ADE80, LightTexture.FULL_BRIGHT, overlay);

        // 7. Backplate aerospace ridge & cyan badge stripe (visible in 3rd person / ground)
        drawBox(solid, pose, -hw * 0.72F, -hh * 0.65F, -hd - 0.008F, hw * 0.72F, hh * 0.65F, -hd, 0xFF1A2636, light, overlay);
        drawBox(solid, pose, -hw * 0.55F, 0.04F, -hd - 0.010F, hw * 0.55F, 0.07F, -hd - 0.007F, 0xFF38BDF8, LightTexture.FULL_BRIGHT, overlay);

        // 8. Recessed backlit display glass + cyan border
        float sw = SCREEN_W * 0.5F;
        float sh = SCREEN_H * 0.5F;
        float screenZ = hd + 0.0075F;
        drawFrontQuad(solid, pose, -sw - 0.008F, -sh - 0.008F, sw + 0.008F, sh + 0.008F, screenZ, 0xFF38BDF8, LightTexture.FULL_BRIGHT, overlay);
        drawFrontQuad(solid, pose, -sw, -sh, sw, sh, screenZ + 0.0012F, 0xFF08111F, LightTexture.FULL_BRIGHT, overlay);

        if (drawLiveScreen && !this.renderingLiveScreen) {
            this.renderingLiveScreen = true;
            try {
                renderLiveScreenDisplay(poseStack, buffer, screenZ + 0.0028F, overlay);
            } finally {
                this.renderingLiveScreen = false;
            }
        }
    }

    /**
     * Renders the live non-interactive preview of the exact tab &amp; chapter the player was last viewing
     * directly onto the 3D tablet glass surface in model space.
     */
    private void renderLiveScreenDisplay(
            PoseStack poseStack,
            MultiBufferSource buffer,
            float screenZ,
            int overlay
    ) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ManualTab activeTab = AerospaceManualScreen.getSessionTab();
        @Nullable ManualEntry entry = AerospaceManualScreen.getSessionEntry();
        int era = AerospaceManualScreen.getCurrentEra();
        boolean unlocked = entry == null || entry.isUnlocked(ClientResearchCache.unlockedNodes());

        // Virtual 220 x 136 screen coordinate system mapped onto SCREEN_W x SCREEN_H
        final float canvasW = 220.0F;
        final float canvasH = 136.0F;
        float scaleX = SCREEN_W / canvasW;
        float scaleY = SCREEN_H / canvasH;

        poseStack.pushPose();
        poseStack.translate(-SCREEN_W * 0.5F, SCREEN_H * 0.5F, screenZ);
        poseStack.scale(scaleX, -scaleY, 0.001F);

        VertexConsumer ui = buffer.getBuffer(RenderType.entitySolid(SURFACE_TEX));
        PoseStack.Pose p = poseStack.last();

        // Top header bar
        drawUiRect(ui, p, 0, 0, canvasW, 16, 0.0F, 0xFF0E1D34, overlay);
        drawUiRect(ui, p, 0, 16, canvasW, 17.5F, 0.0F, activeTab.accentColor(), overlay);

        // 6 Tab pills strip showing which tab the player was on!
        ManualTab[] tabs = ManualTab.values();
        float tabW = (canvasW - 8.0F) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            ManualTab t = tabs[i];
            float tx = 4.0F + i * tabW;
            boolean sel = t == activeTab;
            drawUiRect(ui, p, tx, 20, tx + tabW - 2.0F, 32, 0.0F, sel ? 0xFF1A3A5C : 0xFF0C182B, overlay);
            drawUiRect(ui, p, tx, 20, tx + tabW - 2.0F, sel ? 22.0F : 21.0F, 0.1F, sel ? t.accentColor() : 0xFF233B57, overlay);
        }

        // Main live card background
        drawUiRect(ui, p, 4, 36, canvasW - 4, 120, 0.0F, 0xFF0C1728, overlay);
        drawUiRect(ui, p, 4, 36, 6.5F, 120, 0.1F, unlocked ? activeTab.accentColor() : 0xFFF59E0B, overlay);

        // Bottom prompt strip
        drawUiRect(ui, p, 0, 123, canvasW, canvasH, 0.0F, 0xFF0C1A2E, overlay);

        // Render crisp FULL_BRIGHT text slightly in front of UI quads (z = +0.5 in scaled canvas space)
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.6F);

        drawScreenText(font, poseStack, buffer, "AEROSPACE PAD • ERA " + era, 6, 4, 0xFF7DD3FC);
        String spBadge = "SP:" + ClientResearchCache.sciencePoints();
        drawScreenText(font, poseStack, buffer, spBadge, canvasW - font.width(spBadge) - 6, 4, 0xFF94A3B8);

        for (int i = 0; i < tabs.length; i++) {
            ManualTab t = tabs[i];
            float tx = 4.0F + i * tabW;
            boolean sel = t == activeTab;
            String lbl = Component.translatable(t.translationKey()).getString();
            String shortLbl = font.plainSubstrByWidth(lbl, (int) (tabW - 4));
            drawScreenText(font, poseStack, buffer, shortLbl, tx + 2.0F, 23, sel ? 0xFFFFFFFF : 0xFF7C8FA6);
        }

        // Active entry title & live content summary
        if (entry != null) {
            String title = (unlocked ? "" : "[LOCKED] ") + Component.translatable(entry.titleKey()).getString();
            drawScreenText(font, poseStack, buffer, font.plainSubstrByWidth(title, 186), 11, 41, unlocked ? 0xFFF8FAFC : 0xFFFBBF24);

            List<String> previewLines = buildLivePreviewLines(activeTab, entry, unlocked);
            float lineY = 55.0F;
            for (int i = 0; i < Math.min(5, previewLines.size()); i++) {
                String line = font.plainSubstrByWidth(previewLines.get(i), 198);
                int col = (i == 0 && !unlocked) ? 0xFFF59E0B : (i == 0 ? 0xFF7DD3FC : 0xFFCBD5E1);
                drawScreenText(font, poseStack, buffer, line, 11, lineY, col);
                lineY += 12.0F;
            }
        } else {
            drawScreenText(font, poseStack, buffer, "Ready — Open to inspect", 11, 52, 0xFFE2E8F0);
        }

        String footer = "[RMB / G] OPEN TABLET";
        drawScreenText(font, poseStack, buffer, footer, (canvasW - font.width(footer)) * 0.5F, 126, 0xFF38BDF8);
        poseStack.popPose();

        // Miniature entry item icon on the top-right of the live display card (guard against self-recursion)
        if (entry != null) {
            Item iconItem = BuiltInRegistries.ITEM.get(entry.iconItem());
            if (iconItem instanceof dev.galacticraft.mod.content.item.AerospaceManualItem) {
                iconItem = Items.COMPASS;
            }
            if (iconItem != Items.AIR) {
                poseStack.pushPose();
                poseStack.translate(canvasW - 18.0F, 48.0F, 1.5F);
                poseStack.scale(14.0F, -14.0F, 14.0F);
                mc.getItemRenderer().renderStatic(
                        new ItemStack(iconItem),
                        ItemDisplayContext.GUI,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        poseStack,
                        buffer,
                        mc.level,
                        0
                );
                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static List<String> buildLivePreviewLines(ManualTab tab, ManualEntry entry, boolean unlocked) {
        if (!unlocked) {
            ResearchNode req = ResearchCatalog.get(entry.requiredResearchId());
            String lab = req != null ? ("Lab: " + req.sourceLabName() + " (" + req.spCost() + " SP)") : "Research required";
            return List.of(
                    "Research Required:",
                    lab,
                    Component.translatable(entry.lockHintKey()).getString()
            );
        }

        for (ManualPage page : entry.pages()) {
            if (page instanceof ManualPage.PlanetStratigraphyPage planetPage) {
                PlanetaryEnvironment env = PlanetaryEnvironment.byId(planetPage.bodyId());
                if (env != null) {
                    PlanetarySurveyPrecision prec = PlanetarySurveyPrecision.evaluateLive(planetPage.bodyId());
                    return List.of(
                            "Precision: " + prec.precisionPct() + "% (" + (prec.isExact() ? "VERIFIED" : "PROBABLE RANGE") + ")",
                            "G: " + prec.formatGravity(env) + " | P: " + prec.formatPressure(env),
                            "Temp: " + prec.formatTemperatureC(env),
                            "dV: " + prec.formatDeltaV(env) + " | Solar: " + prec.formatSolar(env)
                    );
                }
            }
            if (page instanceof ManualPage.LaunchChecklistPage checkPage) {
                return List.of(
                        "Checklist (" + checkPage.requirements().size() + " items):",
                        "• " + Component.translatable(checkPage.requirements().get(0).labelKey()).getString(),
                        checkPage.requirements().size() > 1 ? ("• " + Component.translatable(checkPage.requirements().get(1).labelKey()).getString()) : "",
                        "Open tablet to pin to HUD"
                );
            }
            if (page instanceof ManualPage.PartMaterialPage matPage && matPage.includeFlightSimulator()) {
                return List.of(
                        "Flight Viability Simulator",
                        AerospaceManualScreen.getLastFlightSummary(),
                        "Live T/W, dV, Thermal & Pressure",
                        "Open tablet to tune rocket"
                );
            }
            if (page instanceof ManualPage.Multiblock3DPage mbPage) {
                return List.of(
                        "3D Multiblock Blueprint",
                        Component.translatable(mbPage.titleKey()).getString(),
                        "Layers: " + mbPage.layers().size() + " | Drag 3D in tablet",
                        Component.translatable(mbPage.captionKey()).getString()
                );
            }
            if (page instanceof ManualPage.FlowchartPage flowPage) {
                return List.of(
                        "Process Flowchart (" + flowPage.steps().size() + " steps)",
                        flowPage.steps().isEmpty() ? "" : Component.translatable(flowPage.steps().get(0).titleKey()).getString(),
                        flowPage.steps().size() > 1 ? Component.translatable(flowPage.steps().get(1).titleKey()).getString() : "",
                        Component.translatable(flowPage.captionKey()).getString()
                );
            }
            if (page instanceof ManualPage.TextPage textPage) {
                return List.of(
                        textPage.headingKey().isEmpty() ? tab.name() : Component.translatable(textPage.headingKey()).getString(),
                        Component.translatable(textPage.bodyKey()).getString(),
                        "Unlocked Nodes: " + ClientResearchCache.unlockedNodes().size() + "/" + ResearchCatalog.totalNodeCount()
                );
            }
        }
        return List.of(
                "Tab: " + Component.translatable(tab.translationKey()).getString(),
                "Unlocked: " + ClientResearchCache.unlockedNodes().size() + "/" + ResearchCatalog.totalNodeCount()
        );
    }

    private static void drawScreenText(
            Font font,
            PoseStack poseStack,
            MultiBufferSource buffer,
            String text,
            float x,
            float y,
            int argb
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        font.drawInBatch(
                text,
                x,
                y,
                argb,
                false,
                poseStack.last().pose(),
                buffer,
                Font.DisplayMode.NORMAL,
                0,
                LightTexture.FULL_BRIGHT
        );
    }

    private static void drawUiRect(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float x2,
            float y2,
            float z,
            int argb,
            int overlay
    ) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        // Note: Y is inverted (-scaleY) in UI space, so winding is kept counter-clockwise facing +Z
        consumer.addVertex(pose, x1, y1, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x1, y2, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x2, y2, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x2, y1, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
    }

    private static void drawFrontQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float x2,
            float y2,
            float z,
            int argb,
            int light,
            int overlay
    ) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        consumer.addVertex(pose, x1, y1, z).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x2, y1, z).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x2, y2, z).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
        consumer.addVertex(pose, x1, y2, z).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
    }

    private static void drawBox(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int argb,
            int light,
            int overlay
    ) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        // +Z (Front)
        quad(consumer, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a, light, overlay, 0, 0, 1);
        // -Z (Back)
        quad(consumer, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, (int) (r * 0.82F), (int) (g * 0.82F), (int) (b * 0.82F), a, light, overlay, 0, 0, -1);
        // +Y (Top)
        quad(consumer, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, Math.min(255, (int) (r * 1.12F)), Math.min(255, (int) (g * 1.12F)), Math.min(255, (int) (b * 1.12F)), a, light, overlay, 0, 1, 0);
        // -Y (Bottom)
        quad(consumer, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, (int) (r * 0.72F), (int) (g * 0.72F), (int) (b * 0.72F), a, light, overlay, 0, -1, 0);
        // +X (Right)
        quad(consumer, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, (int) (r * 0.88F), (int) (g * 0.88F), (int) (b * 0.88F), a, light, overlay, 1, 0, 0);
        // -X (Left)
        quad(consumer, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, (int) (r * 0.88F), (int) (g * 0.88F), (int) (b * 0.88F), a, light, overlay, -1, 0, 0);
    }

    private static void quad(
            VertexConsumer c, PoseStack.Pose pose,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            int r, int g, int b, int a,
            int light, int overlay,
            float nx, float ny, float nz
    ) {
        c.addVertex(pose, x0, y0, z0).setColor(r, g, b, a).setUv(0, 1).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        c.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        c.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setUv(1, 0).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        c.addVertex(pose, x3, y3, z3).setColor(r, g, b, a).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
