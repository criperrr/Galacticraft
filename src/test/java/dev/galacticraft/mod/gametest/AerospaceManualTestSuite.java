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

package dev.galacticraft.mod.gametest;

import dev.galacticraft.api.accessor.ResearchAccessor;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.attachments.GCAttachments;
import dev.galacticraft.mod.attachments.ManualReaderAttachment;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.manual.ManualCatalog;
import dev.galacticraft.mod.manual.ManualEntry;
import dev.galacticraft.mod.manual.ManualManager;
import dev.galacticraft.mod.research.ResearchCatalog;
import dev.galacticraft.mod.research.ResearchManager;
import dev.galacticraft.mod.research.TeamResearchState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Optional;

/**
 * Automated GameTest suite for Plan 12 — Interactive Aerospace Manual ({@code AEROSPACE_MANUAL}),
 * verifying data-driven reload, recipe availability, Fabric Data Attachment reader persistence,
 * item-to-entry reverse index, and live locked -> unlocked transitions via {@link ResearchAccessor}.
 */
@SuppressWarnings("UnstableApiUsage")
public class AerospaceManualTestSuite implements GalacticraftGameTest {

    @GameTest(template = EMPTY_STRUCTURE, batch = "aerospaceManual", timeoutTicks = 100)
    public void manualLoadedFromDatapackAndRecipesExistTest(GameTestHelper context) {
        if (ManualManager.INSTANCE.totalEntryCount() < ManualCatalog.builtInEntries().size()) {
            context.fail("ManualManager loaded fewer entries than expected: " + ManualManager.INSTANCE.totalEntryCount());
            return;
        }
        if (ManualManager.INSTANCE.lastLoadedJsonCount() < ManualCatalog.builtInEntries().size()) {
            context.fail("Expected data-driven JSON entries loaded by ManualManager on server data reload, got "
                    + ManualManager.INSTANCE.lastLoadedJsonCount());
            return;
        }

        // Verify crafting recipes for AEROSPACE_MANUAL are registered
        Optional<RecipeHolder<?>> mainRecipe = context.getLevel().getRecipeManager().byKey(Constant.id("aerospace_manual"));
        Optional<RecipeHolder<?>> altRecipe = context.getLevel().getRecipeManager().byKey(Constant.id("aerospace_manual_from_aluminum"));
        if (mainRecipe.isEmpty() || altRecipe.isEmpty()) {
            context.fail("Expected both aerospace_manual crafting recipes to exist in RecipeManager");
            return;
        }

        // Verify reverse item lookup index
        Optional<ResourceLocation> manualEntry = ManualManager.INSTANCE.findEntryForItem(GCItems.AEROSPACE_MANUAL);
        Optional<ResourceLocation> niobiumEntry = ManualManager.INSTANCE.findEntryForItem(GCItems.FERRONIOBIUM_INGOT);
        if (manualEntry.isEmpty() || niobiumEntry.isEmpty()) {
            context.fail("ManualManager reverse item index missing AEROSPACE_MANUAL or FERRONIOBIUM_INGOT");
            return;
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, batch = "aerospaceManual", timeoutTicks = 100)
    public void manualAttachmentPersistenceAndResearchUnlockReflectionTest(GameTestHelper context) {
        TeamResearchState research = ResearchManager.get(context.getLevel());
        research.resetAll();

        ManualEntry sabatier = ManualManager.INSTANCE.get(ManualCatalog.ENTRY_CHAIN_SABATIER);
        if (sabatier == null) {
            context.fail("Missing ENTRY_CHAIN_SABATIER in ManualManager");
            return;
        }

        ResearchAccessor liveAccessor = research::isUnlocked;
        if (sabatier.isUnlocked(liveAccessor)) {
            context.fail("ENTRY_CHAIN_SABATIER must be locked before MARTIAN_SABATIER_ISRU is researched");
            return;
        }

        research.unlockNode(ResearchCatalog.MARTIAN_SABATIER_ISRU);
        if (!sabatier.isUnlocked(liveAccessor)) {
            context.fail("ENTRY_CHAIN_SABATIER must become unlocked immediately when MARTIAN_SABATIER_ISRU is unlocked");
            return;
        }

        // Verify Fabric Data Attachment API state persistence on Player without Player mixins
        Player mockPlayer = context.makeMockPlayer(GameType.SURVIVAL);
        ManualReaderAttachment initial = mockPlayer.getAttachedOrCreate(GCAttachments.MANUAL_READER);
        if (initial.isRead(ManualCatalog.ENTRY_MISSION_OVERVIEW)) {
            context.fail("Fresh player should not have read entries yet");
            return;
        }

        ManualReaderAttachment updated = initial
                .withRead(ManualCatalog.ENTRY_MISSION_OVERVIEW)
                .withFavoriteToggled(ManualCatalog.ENTRY_CHAIN_SABATIER)
                .withPinnedMission(new ManualReaderAttachment.PinnedMission(
                        ManualCatalog.ENTRY_LAUNCH_CHECKLIST_MOON,
                        "Moon Launch",
                        List.of("Oxygen Mask"),
                        List.of(Constant.id("oxygen_mask")),
                        false
                ));
        mockPlayer.setAttached(GCAttachments.MANUAL_READER, updated);

        ManualReaderAttachment fetched = mockPlayer.getAttached(GCAttachments.MANUAL_READER);
        if (fetched == null
                || !fetched.isRead(ManualCatalog.ENTRY_MISSION_OVERVIEW)
                || !fetched.isFavorite(ManualCatalog.ENTRY_CHAIN_SABATIER)
                || fetched.pinnedMission().isEmpty()
                || !fetched.pinnedMission().get().entryId().equals(ManualCatalog.ENTRY_LAUNCH_CHECKLIST_MOON)) {
            context.fail("GCAttachments.MANUAL_READER failed to store/retrieve reader state: " + fetched);
            return;
        }

        context.succeed();
    }
}
