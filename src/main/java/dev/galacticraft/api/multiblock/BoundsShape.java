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

package dev.galacticraft.api.multiblock;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public enum BoundsShape implements StringRepresentable {
    FULL("full", Shapes.block(), Shapes.block()),
    SLAB_8PX("slab_8px", BakedShapes.SLAB_8PX, BakedShapes.SLAB_8PX),
    FLOOR_4PX("floor_4px", BakedShapes.FLOOR_4PX, BakedShapes.FLOOR_4PX),
    OPEN_HATCH("open_hatch", BakedShapes.THIN_2PX, Shapes.empty()),
    ELEVATOR_FLOOR("elevator_floor", BakedShapes.THIN_2PX, BakedShapes.THIN_2PX);

    public static final StringRepresentable.EnumCodec<BoundsShape> CODEC = StringRepresentable.fromEnum(BoundsShape::values);

    private final String serializedName;
    private final VoxelShape shape;
    private final VoxelShape collisionShape;

    BoundsShape(String serializedName, VoxelShape shape, VoxelShape collisionShape) {
        this.serializedName = serializedName;
        this.shape = shape;
        this.collisionShape = collisionShape;
    }

    @Override
    public @NotNull String getSerializedName() {
        return this.serializedName;
    }

    public @NotNull VoxelShape getShape() {
        return this.shape;
    }

    public @NotNull VoxelShape getCollisionShape() {
        return this.collisionShape;
    }

    private static final class BakedShapes {
        private static final VoxelShape SLAB_8PX = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
        private static final VoxelShape FLOOR_4PX = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
        private static final VoxelShape THIN_2PX = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
    }
}
