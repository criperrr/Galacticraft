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

package dev.galacticraft.mod.manual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Polymorphic data-driven page blocks for {@link ManualEntry}, dispatched via {@code Codec.dispatch("type", ...)}.
 * Supports continuous vertical scrollable rendering and hot-reload.
 */
public sealed interface ManualPage permits
        ManualPage.TextPage,
        ManualPage.CraftingPage,
        ManualPage.InfuserPage,
        ManualPage.PartMaterialPage,
        ManualPage.Multiblock3DPage,
        ManualPage.PlanetStratigraphyPage,
        ManualPage.FlowchartPage,
        ManualPage.LaunchChecklistPage {

    enum Type implements StringRepresentable {
        TEXT("text", TextPage.CODEC),
        CRAFTING("crafting", CraftingPage.CODEC),
        INFUSER("infuser", InfuserPage.CODEC),
        PART_MATERIAL("part_material", PartMaterialPage.CODEC),
        MULTIBLOCK_3D("multiblock_3d", Multiblock3DPage.CODEC),
        PLANET_STRATIGRAPHY("planet_stratigraphy", PlanetStratigraphyPage.CODEC),
        FLOWCHART("flowchart", FlowchartPage.CODEC),
        LAUNCH_CHECKLIST("launch_checklist", LaunchChecklistPage.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String serializedName;
        private final MapCodec<? extends ManualPage> codec;

        Type(String serializedName, MapCodec<? extends ManualPage> codec) {
            this.serializedName = serializedName;
            this.codec = codec;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }

        public @NotNull MapCodec<? extends ManualPage> codec() {
            return this.codec;
        }
    }

    Codec<ManualPage> CODEC = Type.CODEC.dispatch("type", ManualPage::type, Type::codec);

    @NotNull Type type();

    /**
     * Narrative section with heading, body text, optional callout banner ({@code info|warn|tip}),
     * interactive item icon strip, and cross-reference links to other manual entries.
     */
    record TextPage(
            @NotNull String headingKey,
            @NotNull String bodyKey,
            @NotNull String calloutStyle,
            @NotNull String calloutKey,
            @NotNull List<ResourceLocation> showcaseItems,
            @NotNull List<ResourceLocation> linkEntries
    ) implements ManualPage {
        public static final MapCodec<TextPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("heading", "").forGetter(TextPage::headingKey),
                Codec.STRING.fieldOf("body").forGetter(TextPage::bodyKey),
                Codec.STRING.optionalFieldOf("callout_style", "info").forGetter(TextPage::calloutStyle),
                Codec.STRING.optionalFieldOf("callout", "").forGetter(TextPage::calloutKey),
                ResourceLocation.CODEC.listOf().optionalFieldOf("showcase_items", List.of()).forGetter(TextPage::showcaseItems),
                ResourceLocation.CODEC.listOf().optionalFieldOf("link_entries", List.of()).forGetter(TextPage::linkEntries)
        ).apply(instance, TextPage::new));

        @Override
        public @NotNull Type type() {
            return Type.TEXT;
        }
    }

    /**
     * Live recipe viewer block querying {@code ClientLevel.getRecipeManager()} for Vanilla Crafting,
     * Smelting/Blasting, Compressor, Circuit Fabricator, Electric Arc Furnace, or Telemetry recipes.
     */
    record CraftingPage(
            @NotNull String titleKey,
            @NotNull ResourceLocation recipeId,
            @NotNull String captionKey
    ) implements ManualPage {
        public static final MapCodec<CraftingPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("title", "").forGetter(CraftingPage::titleKey),
                ResourceLocation.CODEC.fieldOf("recipe_id").forGetter(CraftingPage::recipeId),
                Codec.STRING.optionalFieldOf("caption", "").forGetter(CraftingPage::captionKey)
        ).apply(instance, CraftingPage::new));

        @Override
        public @NotNull Type type() {
            return Type.CRAFTING;
        }
    }

    /**
     * Specialized industrial machine recipe block (Vacuum Metallurgic Infuser, Chemical Leaching Plant,
     * Composite Autoclave, Sabatier Reactor, Electrolytic Separator, Isotopic Centrifuge).
     */
    record InfuserPage(
            @NotNull String titleKey,
            @NotNull ResourceLocation recipeId,
            @NotNull String machineKey,
            @NotNull List<ResourceLocation> inputItems,
            @NotNull ResourceLocation outputItem,
            @NotNull String processNoteKey
    ) implements ManualPage {
        public static final MapCodec<InfuserPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("title", "").forGetter(InfuserPage::titleKey),
                ResourceLocation.CODEC.fieldOf("recipe_id").forGetter(InfuserPage::recipeId),
                Codec.STRING.fieldOf("machine").forGetter(InfuserPage::machineKey),
                ResourceLocation.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(InfuserPage::inputItems),
                ResourceLocation.CODEC.fieldOf("output").forGetter(InfuserPage::outputItem),
                Codec.STRING.optionalFieldOf("note", "").forGetter(InfuserPage::processNoteKey)
        ).apply(instance, InfuserPage::new));

        @Override
        public @NotNull Type type() {
            return Type.INFUSER;
        }
    }

    /**
     * Aerospace Tinkers' material cards pulling live profiles from {@code RocketMaterialRegistry},
     * optionally embedding the interactive Planetary Flight Viability Calculator.
     */
    record PartMaterialPage(
            @NotNull String titleKey,
            @NotNull List<ResourceLocation> materialIds,
            boolean includeFlightSimulator
    ) implements ManualPage {
        public static final MapCodec<PartMaterialPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("title", "").forGetter(PartMaterialPage::titleKey),
                ResourceLocation.CODEC.listOf().optionalFieldOf("materials", List.of()).forGetter(PartMaterialPage::materialIds),
                Codec.BOOL.optionalFieldOf("include_flight_simulator", false).forGetter(PartMaterialPage::includeFlightSimulator)
        ).apply(instance, PartMaterialPage::new));

        @Override
        public @NotNull Type type() {
            return Type.PART_MATERIAL;
        }
    }

    /**
     * Interactive Ponder-style 3D isometric multiblock viewer with drag rotation and layer stepping.
     * {@code layers} are ordered bottom-to-top (Y = 0, 1, ...), each layer being a list of Z rows of single-char keys.
     */
    record Multiblock3DPage(
            @NotNull String titleKey,
            @NotNull String captionKey,
            @NotNull List<List<String>> layers,
            @NotNull Map<String, ResourceLocation> palette
    ) implements ManualPage {
        public static final MapCodec<Multiblock3DPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("title").forGetter(Multiblock3DPage::titleKey),
                Codec.STRING.optionalFieldOf("caption", "").forGetter(Multiblock3DPage::captionKey),
                Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(Multiblock3DPage::layers),
                Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).fieldOf("palette").forGetter(Multiblock3DPage::palette)
        ).apply(instance, Multiblock3DPage::new));

        @Override
        public @NotNull Type type() {
            return Type.MULTIBLOCK_3D;
        }
    }

    /**
     * Planetary geological & atmospheric telemetry sheet with a visual Y-depth ore stratigraphy chart.
     */
    record PlanetStratigraphyPage(
            @NotNull String titleKey,
            @NotNull ResourceLocation bodyId,
            @NotNull List<OreBand> ores,
            @NotNull String hazardsKey
    ) implements ManualPage {
        public record OreBand(
                @NotNull ResourceLocation oreItem,
                @NotNull String labelKey,
                int minY,
                int maxY,
                double rarityMultiplier,
                int color
        ) {
            public static final Codec<OreBand> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("ore_item").forGetter(OreBand::oreItem),
                    Codec.STRING.fieldOf("label").forGetter(OreBand::labelKey),
                    Codec.INT.fieldOf("min_y").forGetter(OreBand::minY),
                    Codec.INT.fieldOf("max_y").forGetter(OreBand::maxY),
                    Codec.DOUBLE.fieldOf("rarity").forGetter(OreBand::rarityMultiplier),
                    Codec.INT.optionalFieldOf("color", 0xFF38BDF8).forGetter(OreBand::color)
            ).apply(instance, OreBand::new));
        }

        public static final MapCodec<PlanetStratigraphyPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("title").forGetter(PlanetStratigraphyPage::titleKey),
                ResourceLocation.CODEC.fieldOf("body_id").forGetter(PlanetStratigraphyPage::bodyId),
                OreBand.CODEC.listOf().optionalFieldOf("ores", List.of()).forGetter(PlanetStratigraphyPage::ores),
                Codec.STRING.optionalFieldOf("hazards", "").forGetter(PlanetStratigraphyPage::hazardsKey)
        ).apply(instance, PlanetStratigraphyPage::new));

        @Override
        public @NotNull Type type() {
            return Type.PLANET_STRATIGRAPHY;
        }
    }

    /**
     * Visual industrial / chemical process flowchart (e.g., Niobium Aluminothermy, Martian Sabatier ISRU,
     * Venusian Acid Leaching, Lunar Helium-3 Fusion).
     */
    record FlowchartPage(
            @NotNull String titleKey,
            @NotNull List<FlowStep> steps,
            @NotNull String captionKey
    ) implements ManualPage {
        public record FlowStep(
                @NotNull ResourceLocation iconItem,
                @NotNull String titleKey,
                @NotNull String detailKey,
                int accentColor
        ) {
            public static final Codec<FlowStep> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("icon").forGetter(FlowStep::iconItem),
                    Codec.STRING.fieldOf("title").forGetter(FlowStep::titleKey),
                    Codec.STRING.optionalFieldOf("detail", "").forGetter(FlowStep::detailKey),
                    Codec.INT.optionalFieldOf("color", 0xFF38BDF8).forGetter(FlowStep::accentColor)
            ).apply(instance, FlowStep::new));
        }

        public static final MapCodec<FlowchartPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("title").forGetter(FlowchartPage::titleKey),
                FlowStep.CODEC.listOf().fieldOf("steps").forGetter(FlowchartPage::steps),
                Codec.STRING.optionalFieldOf("caption", "").forGetter(FlowchartPage::captionKey)
        ).apply(instance, FlowchartPage::new));

        @Override
        public @NotNull Type type() {
            return Type.FLOWCHART;
        }
    }

    /**
     * Live pre-launch readiness checklist evaluating the player's current inventory and GC accessory slots,
     * with one-click pinning to the in-game HUD overlay.
     */
    record LaunchChecklistPage(
            @NotNull String titleKey,
            @NotNull ResourceLocation targetBodyId,
            @NotNull List<ChecklistRequirement> requirements
    ) implements ManualPage {
        public record ChecklistRequirement(
                @NotNull String labelKey,
                @NotNull List<ResourceLocation> acceptedItems,
                int minCount,
                boolean critical
        ) {
            public static final Codec<ChecklistRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("label").forGetter(ChecklistRequirement::labelKey),
                    ResourceLocation.CODEC.listOf().fieldOf("accepted_items").forGetter(ChecklistRequirement::acceptedItems),
                    Codec.INT.optionalFieldOf("min_count", 1).forGetter(ChecklistRequirement::minCount),
                    Codec.BOOL.optionalFieldOf("critical", true).forGetter(ChecklistRequirement::critical)
            ).apply(instance, ChecklistRequirement::new));
        }

        public static final MapCodec<LaunchChecklistPage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("title").forGetter(LaunchChecklistPage::titleKey),
                ResourceLocation.CODEC.fieldOf("target_body").forGetter(LaunchChecklistPage::targetBodyId),
                ChecklistRequirement.CODEC.listOf().fieldOf("requirements").forGetter(LaunchChecklistPage::requirements)
        ).apply(instance, LaunchChecklistPage::new));

        @Override
        public @NotNull Type type() {
            return Type.LAUNCH_CHECKLIST;
        }
    }
}
