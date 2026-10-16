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

import dev.galacticraft.api.accessor.GearInventoryProvider;
import dev.galacticraft.machinelib.api.transfer.ResourceFlow;
import dev.galacticraft.machinelib.api.transfer.ResourceType;
import dev.galacticraft.machinelib.api.util.BlockFace;
import dev.galacticraft.mod.api.wire.Wire;
import dev.galacticraft.mod.api.wire.impl.WireNetworkImpl;
import dev.galacticraft.mod.content.GCAccessorySlots;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.entity.machine.EnergyStorageModuleBlockEntity;
import dev.galacticraft.mod.content.item.GCItems;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

public class WireTestSuite implements GalacticraftGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void wireConnectionTest(GameTestHelper context) {
        final var pos0 = new BlockPos(0, 1, 0);
        final var pos1 = new BlockPos(0, 2, 0);
        final var pos2 = new BlockPos(0, 3, 0);
        context.setBlock(pos0, GCBlocks.ALUMINUM_WIRE);
        context.setBlock(pos1, GCBlocks.ALUMINUM_WIRE);
        context.setBlock(pos2, GCBlocks.ALUMINUM_WIRE);
        final var be0 = context.getBlockEntity(pos0);
        final var be1 = context.getBlockEntity(pos1);
        final var be2 = context.getBlockEntity(pos2);
        if (!(be0 instanceof Wire wire0)) {
            context.fail(String.format("Expected a wire but found %s!", be0), pos0);
        } else if (!(be1 instanceof Wire wire1)) {
            context.fail(String.format("Expected a wire but found %s!", be1), pos1);
        } else if (!(be2 instanceof Wire wire2)) {
            context.fail(String.format("Expected a wire but found %s!", be2), pos2);
        } else {
            if (wire0.getNetwork() == null) {
                context.fail("Expected a wire network but got null!", pos0);
            } else if (wire1.getNetwork() == null) {
                context.fail("Expected a wire network but got null!", pos1);
            } else if (wire2.getNetwork() == null) {
                context.fail("Expected a wire network but got null!", pos2);
            } else {
                if (wire0.getNetwork() != wire1.getNetwork()) {
                    context.fail(String.format("Expected wire networks at %s and %s to be the same!", pos0, pos1));
                } else if (wire1.getNetwork() != wire2.getNetwork()) {
                    context.fail(String.format("Expected wire networks at %s and %s to be the same!", pos1, pos2));
                } else {
                    if (((WireNetworkImpl) wire0.getNetwork()).getWires().size() != 3) {
                        context.fail("Not all wires are registered in the network!");
                    } else {
                        context.succeedWhen(() -> {
                            context.destroyBlock(pos1);
                            if (((WireNetworkImpl) wire0.getNetwork()).getWires().size() != 1) {
                                context.fail(String.format("Expected wire network with 1 wire but found %s wires!", ((WireNetworkImpl) wire0.getNetwork()).getWires().size()), pos0);
                            } else if (((PipeOrWireHelper) () -> ((WireNetworkImpl) wire2.getNetwork()).getWires().size()).count() != 1) {
                                context.fail(String.format("Expected wire network with 1 wire but found %s wires!", ((WireNetworkImpl) wire2.getNetwork()).getWires().size()), pos2);
                            } else if (!wire1.getNetwork().markedForRemoval()) {
                                if (!be1.isRemoved()) {
                                    context.fail("Expected wire to be removed!", pos1);
                                } else {
                                    context.fail("Expected removed wire network to be marked for removal!", pos1);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void wireEnergyTransferAndRollbackTest(GameTestHelper context) {
        final var wirePos0 = new BlockPos(0, 1, 0);
        final var wirePos1 = new BlockPos(0, 2, 0);
        final var machinePos = new BlockPos(0, 3, 0);

        context.setBlock(wirePos0, GCBlocks.ALUMINUM_WIRE);
        context.setBlock(wirePos1, GCBlocks.ALUMINUM_WIRE);
        context.setBlock(machinePos, GCBlocks.ENERGY_STORAGE_MODULE);

        final var wireBe0 = context.getBlockEntity(wirePos0);
        final var wireBe1 = context.getBlockEntity(wirePos1);
        final var machineBe = context.getBlockEntity(machinePos);

        if (!(wireBe0 instanceof Wire wire0) || !(wireBe1 instanceof Wire wire1)) {
            context.fail("Expected aluminum wires at pos0 and pos1!", wirePos0);
            return;
        }
        if (!(machineBe instanceof EnergyStorageModuleBlockEntity esm)) {
            context.fail("Expected EnergyStorageModuleBlockEntity at machinePos!", machinePos);
            return;
        }

        esm.getIOConfig().get(BlockFace.BOTTOM).setOption(ResourceType.ENERGY, ResourceFlow.INPUT);
        wire1.getNetwork().updateConnection( context.absolutePos(wirePos1), context.absolutePos(machinePos), Direction.UP);

        EnergyStorage wireInput = EnergyStorage.SIDED.find(context.getLevel(), context.absolutePos(wirePos0), Direction.DOWN);
        if (wireInput == null) {
            context.fail("Expected EnergyStorage.SIDED on wirePos0!", wirePos0);
            return;
        }

        // 1. Test aborted transaction rollback
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = wireInput.insert(120, tx);
            if (inserted <= 0) {
                context.fail("Expected positive energy insertion into wire network during transaction!", wirePos0);
                return;
            }
            if (esm.energyStorage().getAmount() != inserted) {
                context.fail("Expected ESM energy amount to match inserted amount inside transaction!", machinePos);
                return;
            }
            tx.abort();
        }

        if (esm.energyStorage().getAmount() != 0) {
            context.fail("Expected ESM energy amount to roll back to 0 after transaction abort, got " + esm.energyStorage().getAmount(), machinePos);
            return;
        }

        // 2. Test committed transaction persistence
        long committedAmount;
        try (Transaction tx = Transaction.openOuter()) {
            committedAmount = wireInput.insert(120, tx);
            tx.commit();
        }

        if (committedAmount <= 0 || esm.energyStorage().getAmount() != committedAmount) {
            context.fail("Expected committed energy to persist in ESM, got " + esm.energyStorage().getAmount(), machinePos);
            return;
        }

        context.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void gearInventoryPersistenceTest(GameTestHelper context) {
        Wolf wolf = context.spawn(EntityType.WOLF, new BlockPos(0, 1, 0));
        GearInventoryProvider provider = (GearInventoryProvider) wolf;

        provider.galacticraft$getGearInv().setItem(GCAccessorySlots.OXYGEN_MASK_SLOT, new ItemStack(GCItems.OXYGEN_MASK));
        provider.galacticraft$getGearInv().setItem(GCAccessorySlots.OXYGEN_GEAR_SLOT, new ItemStack(GCItems.OXYGEN_GEAR));

        if (!provider.galacticraft$hasMaskAndGear()) {
            context.fail("Expected entity to report hasMaskAndGear() == true after equipping mask and gear!");
            return;
        }

        CompoundTag tag = new CompoundTag();
        provider.galacticraft$writeGearToNbt(tag);

        provider.galacticraft$getGearInv().clearContent();
        if (provider.galacticraft$hasMask() || provider.galacticraft$hasGear()) {
            context.fail("Expected gear inventory to be empty after clearContent()!");
            return;
        }

        provider.galacticraft$readGearFromNbt(tag);
        if (!provider.galacticraft$hasMaskAndGear()) {
            context.fail("Expected gear inventory to restore mask and gear from NBT!");
            return;
        }

        wolf.discard();
        context.succeed();
    }

    @FunctionalInterface
    private interface PipeOrWireHelper {
        int count();
    }
}
