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

package dev.galacticraft.mod.content.block.entity;

import com.mojang.logging.LogUtils;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.multiblock.BoundsShape;
import dev.galacticraft.api.multiblock.MultiVoxelMaster;
import dev.galacticraft.api.rocket.part.data.PartIntegrity;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartStats;
import dev.galacticraft.api.rocket.part.modular.FootprintMode;
import dev.galacticraft.api.rocket.part.modular.RocketPartCategory;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.attachments.MultiblockLinkManager;
import dev.galacticraft.mod.content.GCBlockEntityTypes;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.block.multiblock.AbstractBoundsProxyBlock;
import dev.galacticraft.mod.content.block.multiblock.RocketPartBoundsBlock;
import dev.galacticraft.mod.content.block.rocket.ModularRocketPartBlock;
import dev.galacticraft.mod.content.rocket.part.RocketPartCatalog;
import dev.galacticraft.mod.content.rocket.part.RocketPartCostCalculator;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Master {@link BlockEntity} for a world-placed {@link ModularRocketPartBlock}. Owns the part's
 * {@link RocketPartConfig}, physical {@link RocketPartStats}, {@link PartIntegrity}, walkable
 * airlock hatch state, and multi-voxel {@link RocketPartBoundsBlock} proxy lifecycle.
 */
public class ModularRocketPartBlockEntity extends BlockEntity implements MultiVoxelMaster {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String TAG_CONFIG = "PartConfig";
    private static final String TAG_STATS = "PartStats";
    private static final String TAG_INTEGRITY = "PartIntegrity";
    private static final String TAG_COMPOSITION = "PartComposition";
    private static final String TAG_HATCH_OPEN = "HatchOpen";
    private static final String TAG_ANIM_ACTIVE = "AnimActive";
    private static final String TAG_OCCUPIED_PROXIES = "OccupiedProxies";
    private static final String TAG_ASSEMBLED_TERMINAL_POS = "AssembledTerminalPos";

    private RocketPartConfig config;
    private @Nullable RocketPartStats stats;
    private @Nullable PartIntegrity integrity;
    private @Nullable RocketPartComposition composition;
    private final List<BlockPos> occupiedProxies = new ArrayList<>();
    private @Nullable BlockPos assembledTerminalPos = null;
    private boolean hatchOpen = false;
    private boolean animationActive = false;
    private long animationStartTick = 0L;
    private boolean tearingDown = false;
    private boolean tornDown = false;

    public ModularRocketPartBlockEntity(BlockPos pos, BlockState state) {
        super(GCBlockEntityTypes.MODULAR_ROCKET_PART, pos, state);
        RocketPartCategory fallbackCategory = state.getBlock() instanceof ModularRocketPartBlock partBlock
                ? partBlock.getCategory()
                : RocketPartCategory.COMMAND;
        this.config = RocketPartCatalog.getInstance().defaultConfigFor(fallbackCategory);
    }

    public @NotNull RocketPartConfig getConfig() {
        return this.config;
    }

    public @Nullable RocketPartStats getStats() {
        return this.stats;
    }

    public @Nullable PartIntegrity getIntegrity() {
        return this.integrity;
    }

    /**
     * Updates this world-placed part's structural integrity (e.g., from planetary thermal,
     * hyperbaric pressure, or sulfuric acid corrosion wear) and synchronizes to clients.
     */
    public void setIntegrity(@NotNull PartIntegrity nextIntegrity) {
        Objects.requireNonNull(nextIntegrity, "nextIntegrity");
        if (!Objects.equals(this.integrity, nextIntegrity)) {
            this.integrity = nextIntegrity;
            this.sync();
        }
    }

    /**
     * Applies structural integrity wear to this world-placed modular rocket part.
     */
    public @NotNull PartIntegrity damageIntegrity(double amount) {
        PartIntegrity current = this.integrity != null ? this.integrity : PartIntegrity.pristine();
        if (amount <= 0.0) {
            return current;
        }
        PartIntegrity next = current.damage(amount);
        this.setIntegrity(next);
        return next;
    }

    public @Nullable RocketPartComposition getComposition() {
        return this.composition;
    }

    public @NotNull List<BlockPos> getOccupiedProxies() {
        return Collections.unmodifiableList(this.occupiedProxies);
    }

    public @Nullable BlockPos getAssembledTerminalPos() {
        return this.assembledTerminalPos;
    }

    public boolean isAssembledOnPlatform() {
        return this.assembledTerminalPos != null;
    }

    public void setAssembledTerminalPos(@Nullable BlockPos terminalPos) {
        BlockPos normalized = terminalPos != null ? terminalPos.immutable() : null;
        if (!Objects.equals(this.assembledTerminalPos, normalized)) {
            this.assembledTerminalPos = normalized;
            this.sync();
        }
    }

    public void clearAssembledTerminalPosQuiet() {
        this.assembledTerminalPos = null;
    }

    public boolean isHatchOpen() {
        return this.hatchOpen;
    }

    public boolean isAnimationActive() {
        return this.animationActive;
    }

    public long getAnimationStartTick() {
        return this.animationStartTick;
    }

    /**
     * Returns whether this part belongs to a family with mechanical/deployable animations that can
     * be triggered from an assembled launch platform's rocket construction menu.
     */
    public boolean supportsAnimationTrigger() {
        if (this.config == null) {
            return false;
        }
        String family = this.config.familyId();
        if (family == null || family.isEmpty()) {
            return false;
        }
        return family.startsWith("power_solar_array")
                || family.startsWith("thermal_radiator")
                || family.startsWith("landing_gear")
                || family.startsWith("aero_grid_fin")
                || family.startsWith("cargo_bay_")
                || family.startsWith("docking_port_")
                || family.startsWith("science_magnetometer")
                || family.startsWith("science_orbital_camera")
                || family.startsWith("science_isru")
                || family.startsWith("science_orbital_laboratory")
                || family.startsWith("science_sample_containment_bay")
                || family.startsWith("decoupler_")
                || family.startsWith("engine_")
                || family.startsWith("rcs_quad")
                || family.startsWith("tank_upgrade_")
                || family.startsWith("probe_")
                || family.startsWith("capsule_")
                || family.startsWith("cockpit_");
    }

    /**
     * Enables or disables this part's triggered mechanical animation on the server (invoked from
     * an assembled launch platform's rocket menu). Placed parts remain static by default.
     */
    public boolean setAnimationActive(boolean active) {
        if (this.level == null || this.level.isClientSide || this.tearingDown || this.tornDown) {
            return false;
        }
        if (active && !this.supportsAnimationTrigger()) {
            return false;
        }
        if (this.animationActive == active) {
            return false;
        }
        this.animationActive = active;
        this.animationStartTick = this.level.getGameTime();
        this.level.playSound(
                null,
                this.worldPosition,
                active ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                SoundSource.BLOCKS,
                0.7F,
                1.0F
        );
        this.sync();
        return true;
    }

    public boolean toggleAnimation() {
        return this.setAnimationActive(!this.animationActive);
    }

    public @NotNull Direction getFacing() {
        BlockState state = this.getBlockState();
        if (state.hasProperty(ModularRocketPartBlock.FACING)) {
            return state.getValue(ModularRocketPartBlock.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Initializes this part's configuration, stats, and integrity upon placement, spawning and
     * chunk-linking all multi-voxel {@link RocketPartBoundsBlock} proxy cells on the server.
     */
    public void initializePart(
            @NotNull RocketPartConfig config,
            @Nullable RocketPartStats stats,
            @Nullable PartIntegrity integrity,
            @NotNull Direction facing
    ) {
        this.initializePart(config, stats, integrity, null, facing);
    }

    /**
     * Initializes this part's configuration, stats, integrity, and material composition upon
     * placement, spawning and chunk-linking all multi-voxel {@link RocketPartBoundsBlock} proxy
     * cells on the server.
     */
    public void initializePart(
            @NotNull RocketPartConfig config,
            @Nullable RocketPartStats stats,
            @Nullable PartIntegrity integrity,
            @Nullable RocketPartComposition composition,
            @NotNull Direction facing
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(facing, "facing");

        this.config = config;
        this.stats = stats != null ? stats : RocketPartCostCalculator.calculate(config).stats();
        this.integrity = integrity != null ? integrity : PartIntegrity.pristine();
        this.composition = (composition != null && !composition.isEmpty()) ? composition : null;
        this.hatchOpen = false;
        this.tornDown = false;

        if (this.level == null || this.level.isClientSide) {
            return;
        }

        BlockState masterState = this.getBlockState();
        if (masterState.getBlock() instanceof ModularRocketPartBlock) {
            BlockState updatedMaster = masterState
                    .setValue(ModularRocketPartBlock.FACING, facing)
                    .setValue(ModularRocketPartBlock.HATCH_OPEN, false);
            if (updatedMaster != masterState) {
                this.level.setBlock(this.worldPosition, updatedMaster, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }

        Map<BlockPos, BoundsShape> localFootprint = FootprintMode.computeLocalFootprint(config, false);
        this.occupiedProxies.clear();
        BlockState proxyBase = GCBlocks.ROCKET_PART_BOUNDS.defaultBlockState();

        for (Map.Entry<BlockPos, BoundsShape> entry : localFootprint.entrySet()) {
            BlockPos localPos = entry.getKey();
            if (localPos.equals(BlockPos.ZERO)) {
                continue;
            }
            BlockPos worldPos = FootprintMode.toWorldPos(this.worldPosition, facing, localPos).immutable();
            BlockState proxyState = proxyBase.setValue(AbstractBoundsProxyBlock.SHAPE, entry.getValue());
            this.level.setBlock(worldPos, proxyState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            this.occupiedProxies.add(worldPos);
        }

        if (!this.occupiedProxies.isEmpty()) {
            MultiblockLinkManager.linkAll(this.level, this.occupiedProxies, this.worldPosition);
        }

        this.sync();
    }

    /**
     * Toggles the airlock hatch state for walkable-interior rocket parts, updating hatch proxy shapes
     * in place without unlinking or disturbing the surrounding multiblock footprint.
     *
     * @return {@code true} if the hatch state toggled.
     */
    public boolean toggleHatch() {
        if (this.level == null || this.level.isClientSide || this.tearingDown || this.tornDown) {
            return false;
        }
        if (!this.config.walkableInterior() || this.config.footprintMode() != FootprintMode.HOLLOW_WITH_HATCH) {
            return false;
        }

        this.hatchOpen = !this.hatchOpen;
        Direction facing = this.getFacing();

        BlockState masterState = this.getBlockState();
        if (masterState.getBlock() instanceof ModularRocketPartBlock
                && masterState.hasProperty(ModularRocketPartBlock.HATCH_OPEN)) {
            this.level.setBlock(
                    this.worldPosition,
                    masterState.setValue(ModularRocketPartBlock.HATCH_OPEN, this.hatchOpen),
                    Block.UPDATE_ALL
            );
        }

        BoundsShape targetShape = this.hatchOpen ? BoundsShape.OPEN_HATCH : BoundsShape.FULL;
        for (BlockPos localHatch : FootprintMode.computeLocalHatchPositions(this.config)) {
            BlockPos worldHatch = FootprintMode.toWorldPos(this.worldPosition, facing, localHatch);
            BlockState existing = this.level.getBlockState(worldHatch);
            if (existing.getBlock() instanceof RocketPartBoundsBlock
                    && existing.hasProperty(AbstractBoundsProxyBlock.SHAPE)) {
                this.level.setBlock(
                        worldHatch,
                        existing.setValue(AbstractBoundsProxyBlock.SHAPE, targetShape),
                        Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE
                );
            }
        }

        this.level.playSound(
                null,
                this.worldPosition,
                this.hatchOpen ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS,
                0.9F,
                0.85F
        );

        this.sync();
        return true;
    }

    /**
     * Atomically tears down this part and all of its chunk-linked {@link RocketPartBoundsBlock}
     * proxies, dropping at most one configured {@link ItemStack} when {@code dropItem} is {@code true}.
     */
    public void tearDown(@Nullable Level level, @Nullable BlockPos brokenPos, @Nullable Player player, boolean dropItem) {
        if (level == null || level.isClientSide || this.tearingDown || this.tornDown) {
            return;
        }

        if (this.assembledTerminalPos != null
                && level.isLoaded(this.assembledTerminalPos)
                && level.getBlockEntity(this.assembledTerminalPos) instanceof HangarTerminalBlockEntity terminal
                && terminal.isPartOfAssembledRocket(this.worldPosition)
                && !terminal.isTearingDownRocket()) {
            terminal.tearDownAssembledRocket(level, brokenPos != null ? brokenPos : this.worldPosition, player, dropItem);
            return;
        }

        this.tearingDown = true;
        this.tornDown = true;
        try {
            ItemStack dropStack = dropItem ? this.createConfiguredStack() : ItemStack.EMPTY;

            List<BlockPos> proxies = new ArrayList<>(this.occupiedProxies);
            this.occupiedProxies.clear();

            if (!proxies.isEmpty()) {
                MultiblockLinkManager.unlinkAll(level, proxies);
                for (BlockPos proxyPos : proxies) {
                    if (level.getBlockState(proxyPos).getBlock() instanceof RocketPartBoundsBlock) {
                        level.setBlock(proxyPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }

            if (level.getBlockState(this.worldPosition).getBlock() instanceof ModularRocketPartBlock) {
                level.setBlock(this.worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }

            if (dropItem && !dropStack.isEmpty()) {
                BlockPos dropPos = brokenPos != null ? brokenPos : this.worldPosition;
                Block.popResource(level, dropPos, dropStack);
            }
        } finally {
            this.tearingDown = false;
        }
    }

    /**
     * Builds a single configured {@link ItemStack} preserving this part's configuration, stats,
     * integrity, and material composition.
     */
    public @NotNull ItemStack createConfiguredStack() {
        ItemStack stack = new ItemStack(ModularRocketPartBlock.getBlockForCategory(this.config.category()));
        stack.set(GCDataComponents.ROCKET_PART_CONFIG, this.config);
        RocketPartStats resolvedStats = this.stats != null
                ? this.stats
                : RocketPartCostCalculator.calculate(this.config).stats();
        stack.set(GCDataComponents.ROCKET_PART_STATS, resolvedStats);
        PartIntegrity resolvedIntegrity = this.integrity != null ? this.integrity : PartIntegrity.pristine();
        stack.set(GCDataComponents.PART_INTEGRITY, resolvedIntegrity);
        if (this.composition != null && !this.composition.isEmpty()) {
            stack.set(GCDataComponents.ROCKET_PART_COMPOSITION, this.composition);
        }
        return stack;
    }

    public void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    // --- MultiVoxelMaster ---

    @Override
    public BlockPos getMasterPos() {
        return this.worldPosition;
    }

    @Override
    public void onProxyBroken(Level level, BlockPos proxyPos, @Nullable Player player, boolean dropItems) {
        this.tearDown(level, proxyPos, player, dropItems);
    }

    @Override
    public InteractionResult onProxyUse(Level level, BlockPos proxyPos, Player player, BlockHitResult hit) {
        if (this.config.walkableInterior()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            this.toggleHatch();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getProxyCloneItemStack(LevelReader level, BlockPos proxyPos) {
        return this.createConfiguredStack();
    }

    @Override
    public boolean isPartOfStructure(BlockPos pos) {
        return pos.equals(this.worldPosition) || this.occupiedProxies.contains(pos);
    }

    @Override
    public @Nullable Storage<FluidVariant> getProxyFluidStorage(BlockPos proxyPos, @Nullable Direction side) {
        HangarTerminalBlockEntity terminal = this.resolveAssembledTerminal();
        return terminal != null ? terminal.getRocketFluidStorage() : null;
    }

    @Override
    public @Nullable EnergyStorage getProxyEnergyStorage(BlockPos proxyPos, @Nullable Direction side) {
        HangarTerminalBlockEntity terminal = this.resolveAssembledTerminal();
        return terminal != null ? terminal.getRocketEnergyStorage() : null;
    }

    private @Nullable HangarTerminalBlockEntity resolveAssembledTerminal() {
        if (this.level == null || this.assembledTerminalPos == null || !this.level.isLoaded(this.assembledTerminalPos)) {
            return null;
        }
        if (this.level.getBlockEntity(this.assembledTerminalPos) instanceof HangarTerminalBlockEntity terminal
                && !terminal.isRemoved()
                && terminal.isRocketAssembled()
                && terminal.isPartOfAssembledRocket(this.worldPosition)) {
            return terminal;
        }
        return null;
    }

    // --- Persistence & Sync ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        RocketPartConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config)
                .resultOrPartial(err -> LOGGER.error("Failed to save RocketPartConfig at {}: {}", this.worldPosition, err))
                .ifPresent(encoded -> tag.put(TAG_CONFIG, encoded));

        if (this.stats != null) {
            RocketPartStats.CODEC.encodeStart(NbtOps.INSTANCE, this.stats)
                    .resultOrPartial(err -> LOGGER.error("Failed to save RocketPartStats at {}: {}", this.worldPosition, err))
                    .ifPresent(encoded -> tag.put(TAG_STATS, encoded));
        }

        if (this.integrity != null) {
            PartIntegrity.CODEC.encodeStart(NbtOps.INSTANCE, this.integrity)
                    .resultOrPartial(err -> LOGGER.error("Failed to save PartIntegrity at {}: {}", this.worldPosition, err))
                    .ifPresent(encoded -> tag.put(TAG_INTEGRITY, encoded));
        }

        if (this.composition != null && !this.composition.isEmpty()) {
            RocketPartComposition.CODEC.encodeStart(NbtOps.INSTANCE, this.composition)
                    .resultOrPartial(err -> LOGGER.error("Failed to save RocketPartComposition at {}: {}", this.worldPosition, err))
                    .ifPresent(encoded -> tag.put(TAG_COMPOSITION, encoded));
        }

        tag.putBoolean(TAG_HATCH_OPEN, this.hatchOpen);
        tag.putBoolean(TAG_ANIM_ACTIVE, this.animationActive);
        if (this.assembledTerminalPos != null) {
            tag.putLong(TAG_ASSEMBLED_TERMINAL_POS, this.assembledTerminalPos.asLong());
        }

        if (!this.occupiedProxies.isEmpty()) {
            long[] packed = new long[this.occupiedProxies.size()];
            for (int i = 0; i < this.occupiedProxies.size(); i++) {
                packed[i] = this.occupiedProxies.get(i).asLong();
            }
            tag.putLongArray(TAG_OCCUPIED_PROXIES, packed);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_CONFIG)) {
            RocketPartConfig.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_CONFIG))
                    .resultOrPartial(err -> LOGGER.error("Failed to load RocketPartConfig at {}: {}", this.worldPosition, err))
                    .ifPresent(loaded -> this.config = loaded);
        }

        if (tag.contains(TAG_STATS)) {
            this.stats = RocketPartStats.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_STATS))
                    .resultOrPartial(err -> LOGGER.error("Failed to load RocketPartStats at {}: {}", this.worldPosition, err))
                    .orElse(null);
        } else {
            this.stats = null;
        }

        if (tag.contains(TAG_INTEGRITY)) {
            this.integrity = PartIntegrity.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_INTEGRITY))
                    .resultOrPartial(err -> LOGGER.error("Failed to load PartIntegrity at {}: {}", this.worldPosition, err))
                    .orElse(null);
        } else {
            this.integrity = null;
        }

        if (tag.contains(TAG_COMPOSITION)) {
            RocketPartComposition loaded = RocketPartComposition.CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_COMPOSITION))
                    .resultOrPartial(err -> LOGGER.error("Failed to load RocketPartComposition at {}: {}", this.worldPosition, err))
                    .orElse(null);
            this.composition = (loaded != null && !loaded.isEmpty()) ? loaded : null;
        } else {
            this.composition = null;
        }

        this.hatchOpen = tag.getBoolean(TAG_HATCH_OPEN);
        boolean loadedAnimActive = tag.getBoolean(TAG_ANIM_ACTIVE);
        if (loadedAnimActive && !this.animationActive && this.level != null) {
            this.animationStartTick = this.level.getGameTime();
        }
        this.animationActive = loadedAnimActive;
        this.assembledTerminalPos = tag.contains(TAG_ASSEMBLED_TERMINAL_POS, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(TAG_ASSEMBLED_TERMINAL_POS))
                : null;

        this.occupiedProxies.clear();
        if (tag.contains(TAG_OCCUPIED_PROXIES, Tag.TAG_LONG_ARRAY)) {
            long[] packed = tag.getLongArray(TAG_OCCUPIED_PROXIES);
            for (long packedPos : packed) {
                this.occupiedProxies.add(BlockPos.of(packedPos));
            }
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
