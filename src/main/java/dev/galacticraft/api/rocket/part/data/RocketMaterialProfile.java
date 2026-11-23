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

package dev.galacticraft.api.rocket.part.data;

import com.mojang.serialization.Codec;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.fluid.FluidData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * Physical modifier profile and slot-role eligibility for an aerospace structural material or
 * functional sub-component used in the modular Rocket Part Assembler.
 *
 * @param id                  Unique registry identifier for this material profile.
 * @param displayName         Human-readable material name for UI and tooltips.
 * @param itemIds             Canonical item IDs matching this profile (first entry is primary icon).
 * @param itemTag             Optional item tag enabling compatibility with tagged variants.
 * @param requiredFluidId     Optional required fluid ID inside a fluid container (e.g. {@code galacticraft:helium_3}).
 * @param allowedRoles        Set of workbench slot roles where this profile is valid.
 * @param massModifier        Additive fractional dry-mass delta (e.g. {@code -0.40} for -40% mass, {@code +0.75} for +75%).
 * @param thermalBonusKelvin  Additive thermal tolerance delta in Kelvin.
 * @param pressureMultiplier  Multiplicative external/internal pressure tolerance factor.
 * @param integrityMultiplier Multiplicative structural integrity factor.
 * @param thrustBonus         Additive fractional engine thrust bonus (e.g. {@code +0.35} for +35% thrust).
 * @param capacityBonus       Additive fractional propellant capacity bonus (e.g. {@code +0.30} for +30% capacity).
 * @param boilOffMultiplier   Multiplicative cryogenic boil-off rate factor ({@code 0.0} = zero boil-off, {@code 1.0} = baseline).
 * @param rgbColor            24-bit RGB color used for UI swatches and tooltip formatting.
 * @param traits              Passive engineering traits granted by this material/sub-component.
 */
public record RocketMaterialProfile(
        @NotNull ResourceLocation id,
        @NotNull String displayName,
        @NotNull List<ResourceLocation> itemIds,
        @Nullable TagKey<Item> itemTag,
        @Nullable ResourceLocation requiredFluidId,
        @NotNull Set<SlotRole> allowedRoles,
        double massModifier,
        double thermalBonusKelvin,
        double pressureMultiplier,
        double integrityMultiplier,
        double thrustBonus,
        double capacityBonus,
        double boilOffMultiplier,
        int rgbColor,
        @NotNull List<RocketMaterialTrait> traits
) {
    /**
     * Functional role of a crafting slot inside a modular rocket part schematic grid.
     */
    public enum SlotRole implements StringRepresentable {
        STRUCTURAL_HULL("structural_hull", "Structural Hull"),
        COMMAND_NOSE_RECOVERY("command_nose_recovery", "Nose Recovery / TPS"),
        COMMAND_VIEWPORT("command_viewport", "Viewport / Optics"),
        COMMAND_AVIONICS("command_avionics", "Avionics & Power Core"),
        TANK_PROPELLANT_CELL("tank_propellant_cell", "Propellant Cell & Cryo"),
        TANK_LOWER_VALVE("tank_lower_valve", "Lower Bulkhead & Valves"),
        ENGINE_INJECTOR("engine_injector", "Injector & Control"),
        ENGINE_TURBOPUMP("engine_turbopump", "Turbopump Feed"),
        ENGINE_CHAMBER("engine_chamber", "Combustion Chamber / Core"),
        ENGINE_NOZZLE_THROAT("engine_nozzle_throat", "Nozzle Thermal Throat"),
        ADAPTER_BASE_RING("adapter_base_ring", "Base Ring / Blast Shield"),
        PAYLOAD_CORE("payload_core", "Payload Core"),
        UTILITY_MOUNT("utility_mount", "Utility Mount"),
        UTILITY_ACTUATOR("utility_actuator", "Actuator / Panel"),
        UTILITY_CONTROL("utility_control", "Control / Power Core");

        public static final Codec<SlotRole> CODEC = StringRepresentable.fromEnum(SlotRole::values);

        private static final IntFunction<SlotRole> BY_ID = ByIdMap.continuous(
                SlotRole::ordinal,
                values(),
                ByIdMap.OutOfBoundsStrategy.ZERO
        );

        public static final StreamCodec<ByteBuf, SlotRole> STREAM_CODEC = ByteBufCodecs.idMapper(
                BY_ID,
                SlotRole::ordinal
        );

        private static final Map<String, SlotRole> BY_NAME;

        static {
            Map<String, SlotRole> map = new HashMap<>();
            for (SlotRole role : values()) {
                map.put(role.serializedName, role);
            }
            BY_NAME = Collections.unmodifiableMap(map);
        }

        private final String serializedName;
        private final String displayName;

        SlotRole(String serializedName, String displayName) {
            this.serializedName = serializedName;
            this.displayName = displayName;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.serializedName;
        }

        public @NotNull String displayName() {
            return this.displayName;
        }

        public static @Nullable SlotRole byName(@Nullable String name) {
            if (name == null || name.isBlank()) {
                return null;
            }
            return BY_NAME.get(name.toLowerCase(Locale.ROOT));
        }
    }

    public RocketMaterialProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        Objects.requireNonNull(itemIds, "itemIds");
        Objects.requireNonNull(allowedRoles, "allowedRoles");
        Objects.requireNonNull(traits, "traits");

        itemIds = List.copyOf(new LinkedHashSet<>(itemIds));
        if (itemIds.isEmpty() && itemTag == null) {
            throw new IllegalArgumentException("RocketMaterialProfile " + id + " must specify at least one itemId or itemTag");
        }
        if (allowedRoles.isEmpty()) {
            throw new IllegalArgumentException("RocketMaterialProfile " + id + " must allow at least one SlotRole");
        }
        allowedRoles = Collections.unmodifiableSet(EnumSet.copyOf(allowedRoles));
        massModifier = sanitizeFinite(massModifier, 0.0);
        thermalBonusKelvin = sanitizeFinite(thermalBonusKelvin, 0.0);
        pressureMultiplier = Math.max(0.1, sanitizeFinite(pressureMultiplier, 1.0));
        integrityMultiplier = Math.max(0.1, sanitizeFinite(integrityMultiplier, 1.0));
        thrustBonus = sanitizeFinite(thrustBonus, 0.0);
        capacityBonus = sanitizeFinite(capacityBonus, 0.0);
        boilOffMultiplier = Math.max(0.0, sanitizeFinite(boilOffMultiplier, 1.0));
        rgbColor = rgbColor & 0xFFFFFF;
        traits = List.copyOf(new LinkedHashSet<>(traits));
    }

    public @Nullable ResourceLocation primaryItemId() {
        return this.itemIds.isEmpty() ? null : this.itemIds.getFirst();
    }

    public boolean supportsRole(@NotNull SlotRole role) {
        return this.allowedRoles.contains(role);
    }

    public boolean isStructural() {
        return this.allowedRoles.contains(SlotRole.STRUCTURAL_HULL);
    }

    public boolean requiresFluid() {
        return this.requiredFluidId != null;
    }

    /**
     * Checks whether {@code stack} satisfies this material profile's item/tag and optional fluid constraints.
     */
    public boolean matches(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (this.requiredFluidId != null) {
            FluidData fluidData = stack.get(GCDataComponents.FLUID_DATA);
            if (fluidData == null || fluidData.amount() <= 0L || fluidData.variant().isBlank()) {
                return false;
            }
            ResourceLocation actualFluidId = BuiltInRegistries.FLUID.getKey(fluidData.variant().getFluid());
            if (!this.requiredFluidId.equals(actualFluidId)) {
                return false;
            }
        }
        ResourceLocation stackItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (this.itemIds.contains(stackItemId)) {
            return true;
        }
        return this.itemTag != null && stack.is(this.itemTag);
    }

    public static @NotNull Builder builder(@NotNull ResourceLocation id, @NotNull String displayName) {
        return new Builder(id, displayName);
    }

    private static double sanitizeFinite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final String displayName;
        private final List<ResourceLocation> itemIds = new ArrayList<>();
        private @Nullable TagKey<Item> itemTag;
        private @Nullable ResourceLocation requiredFluidId;
        private final EnumSet<SlotRole> allowedRoles = EnumSet.noneOf(SlotRole.class);
        private double massModifier = 0.0;
        private double thermalBonusKelvin = 0.0;
        private double pressureMultiplier = 1.0;
        private double integrityMultiplier = 1.0;
        private double thrustBonus = 0.0;
        private double capacityBonus = 0.0;
        private double boilOffMultiplier = 1.0;
        private int rgbColor = 0xCCCCCC;
        private final List<RocketMaterialTrait> traits = new ArrayList<>();

        private Builder(ResourceLocation id, String displayName) {
            this.id = Objects.requireNonNull(id, "id");
            this.displayName = Objects.requireNonNull(displayName, "displayName");
        }

        public Builder item(@NotNull ItemLike itemLike) {
            Objects.requireNonNull(itemLike, "itemLike");
            this.itemIds.add(BuiltInRegistries.ITEM.getKey(itemLike.asItem()));
            return this;
        }

        public Builder items(@NotNull ItemLike... itemLikes) {
            for (ItemLike itemLike : itemLikes) {
                this.item(itemLike);
            }
            return this;
        }

        public Builder itemId(@NotNull ResourceLocation itemId) {
            this.itemIds.add(Objects.requireNonNull(itemId, "itemId"));
            return this;
        }

        public Builder tag(@Nullable TagKey<Item> itemTag) {
            this.itemTag = itemTag;
            return this;
        }

        public Builder requiredFluid(@Nullable ResourceLocation fluidId) {
            this.requiredFluidId = fluidId;
            return this;
        }

        public Builder roles(@NotNull SlotRole... roles) {
            Collections.addAll(this.allowedRoles, roles);
            return this;
        }

        public Builder roles(@NotNull Collection<SlotRole> roles) {
            this.allowedRoles.addAll(roles);
            return this;
        }

        public Builder massModifier(double massModifier) {
            this.massModifier = massModifier;
            return this;
        }

        public Builder thermalBonusKelvin(double thermalBonusKelvin) {
            this.thermalBonusKelvin = thermalBonusKelvin;
            return this;
        }

        public Builder pressureMultiplier(double pressureMultiplier) {
            this.pressureMultiplier = pressureMultiplier;
            return this;
        }

        public Builder integrityMultiplier(double integrityMultiplier) {
            this.integrityMultiplier = integrityMultiplier;
            return this;
        }

        public Builder thrustBonus(double thrustBonus) {
            this.thrustBonus = thrustBonus;
            return this;
        }

        public Builder capacityBonus(double capacityBonus) {
            this.capacityBonus = capacityBonus;
            return this;
        }

        public Builder boilOffMultiplier(double boilOffMultiplier) {
            this.boilOffMultiplier = boilOffMultiplier;
            return this;
        }

        public Builder color(int rgbColor) {
            this.rgbColor = rgbColor;
            return this;
        }

        public Builder trait(@NotNull RocketMaterialTrait trait) {
            this.traits.add(Objects.requireNonNull(trait, "trait"));
            return this;
        }

        public Builder traits(@NotNull RocketMaterialTrait... traits) {
            Collections.addAll(this.traits, traits);
            return this;
        }

        public @NotNull RocketMaterialProfile build() {
            return new RocketMaterialProfile(
                    this.id,
                    this.displayName,
                    this.itemIds,
                    this.itemTag,
                    this.requiredFluidId,
                    this.allowedRoles,
                    this.massModifier,
                    this.thermalBonusKelvin,
                    this.pressureMultiplier,
                    this.integrityMultiplier,
                    this.thrustBonus,
                    this.capacityBonus,
                    this.boilOffMultiplier,
                    this.rgbColor,
                    this.traits
            );
        }
    }
}
