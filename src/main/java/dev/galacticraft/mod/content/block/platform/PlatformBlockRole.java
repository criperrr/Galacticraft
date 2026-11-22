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

package dev.galacticraft.mod.content.block.platform;

import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.mod.content.GCBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Structural role for generic blocks composing a modular rocket platform skeleton.
 */
public enum PlatformBlockRole {
    PAD(() -> GCBlocks.REINFORCED_LAUNCH_PAD, BoundsShape.SLAB_8PX),
    TRUSS(() -> GCBlocks.GANTRY_TRUSS, BoundsShape.FULL),
    CRANE_RAIL(() -> GCBlocks.GANTRY_CRANE_RAIL, BoundsShape.FULL),
    UPGRADE_SOCKET(() -> GCBlocks.PLATFORM_UPGRADE_SOCKET, BoundsShape.FULL);

    private final Supplier<Block> blockSupplier;
    private final BoundsShape defaultFormedShape;

    PlatformBlockRole(Supplier<Block> blockSupplier, BoundsShape defaultFormedShape) {
        this.blockSupplier = blockSupplier;
        this.defaultFormedShape = defaultFormedShape;
    }

    public Block getBlock() {
        return this.blockSupplier.get();
    }

    public BoundsShape defaultFormedShape() {
        return this.defaultFormedShape;
    }

    public boolean matches(BlockState state) {
        return state.is(this.getBlock());
    }
}
