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

package dev.galacticraft.mod.research;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Data-driven KSP+ research node spanning the 5 Tech Branches, 5 Scientific Domains,
 * 6 Technological Eras, primary Science Points ({@code spCost}), secondary Domain points,
 * optional physical Milestone prerequisite, and 2D KSP Tech Tree coordinates ({@code gridX, gridY}).
 */
public record ResearchNode(
        @NotNull ResourceLocation id,
        int era,
        @NotNull ResearchBranch branch,
        @NotNull ResearchDomain domain,
        int spCost,
        int requiredDomainPoints,
        @Nullable String requiredMilestone,
        int gridX,
        int gridY,
        @NotNull List<ResourceLocation> prerequisites,
        @NotNull ResearchLocationRequirement locationRequirement,
        @NotNull String sourceLabName,
        @NotNull String title,
        @NotNull String summary,
        @NotNull List<String> unlocks,
        @NotNull ResourceLocation iconItemId,
        @NotNull List<ResourceLocation> unlockedRecipes
) {
    public ResearchNode(
            @NotNull ResourceLocation id,
            int era,
            @NotNull ResearchBranch branch,
            @NotNull ResearchDomain domain,
            int spCost,
            int requiredDomainPoints,
            @Nullable String requiredMilestone,
            int gridX,
            int gridY,
            @NotNull List<ResourceLocation> prerequisites,
            @NotNull ResearchLocationRequirement locationRequirement,
            @NotNull String sourceLabName,
            @NotNull String title,
            @NotNull String summary,
            @NotNull List<String> unlocks,
            @NotNull ResourceLocation iconItemId
    ) {
        this(id, era, branch, domain, spCost, requiredDomainPoints, requiredMilestone, gridX, gridY,
                prerequisites, locationRequirement, sourceLabName, title, summary, unlocks, iconItemId, List.of());
    }

    private static final MapCodec<ResearchNode> BASE_MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ResearchNode::id),
            Codec.INT.fieldOf("era").forGetter(ResearchNode::era),
            ResearchBranch.CODEC.fieldOf("branch").forGetter(ResearchNode::branch),
            ResearchDomain.CODEC.fieldOf("domain").forGetter(ResearchNode::domain),
            Codec.INT.optionalFieldOf("sp_cost", 20).forGetter(ResearchNode::spCost),
            Codec.INT.optionalFieldOf("required_domain_points", 10).forGetter(ResearchNode::requiredDomainPoints),
            Codec.STRING.optionalFieldOf("required_milestone").forGetter(n -> Optional.ofNullable(n.requiredMilestone)),
            Codec.INT.optionalFieldOf("grid_x", 0).forGetter(ResearchNode::gridX),
            Codec.INT.optionalFieldOf("grid_y", 0).forGetter(ResearchNode::gridY),
            ResourceLocation.CODEC.listOf().optionalFieldOf("prerequisites", List.of()).forGetter(ResearchNode::prerequisites),
            ResearchLocationRequirement.CODEC.optionalFieldOf("location_requirement", ResearchLocationRequirement.ANY).forGetter(ResearchNode::locationRequirement),
            Codec.STRING.optionalFieldOf("source_lab", "Research Workbench").forGetter(ResearchNode::sourceLabName),
            Codec.STRING.fieldOf("title").forGetter(ResearchNode::title),
            Codec.STRING.fieldOf("summary").forGetter(ResearchNode::summary),
            Codec.STRING.listOf().optionalFieldOf("unlocks", List.of()).forGetter(ResearchNode::unlocks),
            ResourceLocation.CODEC.fieldOf("icon_item").forGetter(ResearchNode::iconItemId)
    ).apply(instance, (id, era, branch, domain, spCost, domPts, milestone, gx, gy, prereqs, loc, lab, title, summary, unlocks, icon) ->
            new ResearchNode(id, era, branch, domain, spCost, domPts, milestone.orElse(null), gx, gy, prereqs, loc, lab, title, summary, unlocks, icon)));

    private static final MapCodec<List<ResourceLocation>> UNLOCKED_RECIPES_MAP_CODEC =
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocked_recipes", List.of());

    public static final Codec<ResearchNode> CODEC = Codec.mapPair(BASE_MAP_CODEC, UNLOCKED_RECIPES_MAP_CODEC)
            .codec()
            .xmap(
                    pair -> pair.getFirst().withUnlockedRecipes(pair.getSecond()),
                    node -> Pair.of(node, node.unlockedRecipes())
            );

    public @NotNull ResearchNode withUnlockedRecipes(@NotNull List<ResourceLocation> recipes) {
        return new ResearchNode(this.id, this.era, this.branch, this.domain, this.spCost, this.requiredDomainPoints,
                this.requiredMilestone, this.gridX, this.gridY, this.prerequisites, this.locationRequirement,
                this.sourceLabName, this.title, this.summary, this.unlocks, this.iconItemId, List.copyOf(recipes));
    }

    public boolean hasMilestoneRequirement() {
        return this.requiredMilestone != null && !this.requiredMilestone.isEmpty();
    }
}
