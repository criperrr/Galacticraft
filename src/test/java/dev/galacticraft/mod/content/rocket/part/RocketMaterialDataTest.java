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

package dev.galacticraft.mod.content.rocket.part;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.galacticraft.api.component.GCDataComponents;
import dev.galacticraft.api.gas.Gases;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile;
import dev.galacticraft.api.rocket.part.data.RocketMaterialProfile.SlotRole;
import dev.galacticraft.api.rocket.part.data.RocketMaterialTrait;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition;
import dev.galacticraft.api.rocket.part.data.RocketPartComposition.SlotMaterialEntry;
import dev.galacticraft.api.rocket.part.modular.RocketLivery;
import dev.galacticraft.api.rocket.part.modular.RocketPartConfig;
import dev.galacticraft.mod.content.GCBlocks;
import dev.galacticraft.mod.content.item.FluidCanisterItem;
import dev.galacticraft.mod.content.item.GCItems;
import dev.galacticraft.mod.content.item.ModularRocketPartBlockItem;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tinkers' Aerospace Material Profiles, Traits, Registry & RocketPartComposition Codecs")
class RocketMaterialDataTest {
    private static RegistryAccess registryAccess;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GCDataComponents.init();
        Gases.init();
        GCBlocks.register();
        GCItems.register();
        registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    @Test
    @DisplayName("RocketMaterialTrait CODEC, STREAM_CODEC and byName roundtrip every trait")
    void materialTraitCodecAndStreamCodecRoundtrip() {
        for (RocketMaterialTrait trait : RocketMaterialTrait.values()) {
            assertEquals(trait, RocketMaterialTrait.byName(trait.getSerializedName()));
            assertFalse(trait.displayName().isBlank());
            assertFalse(trait.description().isBlank());

            Tag nbt = RocketMaterialTrait.CODEC.encodeStart(NbtOps.INSTANCE, trait)
                    .getOrThrow(AssertionError::new);
            RocketMaterialTrait decodedNbt = RocketMaterialTrait.CODEC.parse(NbtOps.INSTANCE, nbt)
                    .getOrThrow(AssertionError::new);
            assertEquals(trait, decodedNbt);

            ByteBuf buf = Unpooled.buffer();
            try {
                RocketMaterialTrait.STREAM_CODEC.encode(buf, trait);
                RocketMaterialTrait decodedBuf = RocketMaterialTrait.STREAM_CODEC.decode(buf);
                assertEquals(0, buf.readableBytes());
                assertEquals(trait, decodedBuf);
            } finally {
                buf.release();
            }
        }
    }

    @Test
    @DisplayName("RocketMaterialRegistry registers all 14 canonical structural materials with unique traits and expected modifiers")
    void registryContainsAll14StructuralMaterials() {
        List<RocketMaterialProfile> structural = RocketMaterialRegistry.structuralProfiles();
        assertEquals(14, structural.size(), "Must register exactly 14 structural aerospace materials");

        Set<RocketMaterialTrait> seenTraits = EnumSet.noneOf(RocketMaterialTrait.class);
        for (RocketMaterialProfile profile : structural) {
            assertTrue(profile.isStructural(), "Structural profile must support STRUCTURAL_HULL: " + profile.id());
            assertNotNull(profile.primaryItemId(), "Structural profile must have a primary item ID: " + profile.id());
            assertFalse(profile.traits().isEmpty(), "Structural profile must grant at least one trait: " + profile.id());
            assertTrue(seenTraits.add(profile.traits().getFirst()),
                    "Duplicate primary structural trait for " + profile.id());
            assertTrue(RocketMaterialRegistry.getById(profile.id()).isPresent());
        }

        // Spot-check key Table 5.1 structural numbers
        RocketMaterialProfile titanium = RocketMaterialRegistry.resolve(new ItemStack(GCItems.COMPRESSED_TITANIUM), SlotRole.STRUCTURAL_HULL)
                .orElseThrow();
        assertEquals(-0.40, titanium.massModifier(), 1.0e-6);
        assertEquals(750.0, titanium.thermalBonusKelvin(), 1.0e-6);
        assertEquals(2.10, titanium.pressureMultiplier(), 1.0e-6);
        assertEquals(1.75, titanium.integrityMultiplier(), 1.0e-6);
        assertEquals(List.of(RocketMaterialTrait.AEROSPACE_GRADE), titanium.traits());

        RocketMaterialProfile lead = RocketMaterialRegistry.resolve(new ItemStack(GCItems.LEAD_INGOT), SlotRole.STRUCTURAL_HULL)
                .orElseThrow();
        assertEquals(0.75, lead.massModifier(), 1.0e-6);
        assertEquals(List.of(RocketMaterialTrait.ACID_RADIATION_SHIELD), lead.traits());

        RocketMaterialProfile niobium = RocketMaterialRegistry.resolve(new ItemStack(GCItems.COMPRESSED_NIOBIUM), SlotRole.STRUCTURAL_HULL)
                .orElseThrow();
        assertEquals(1400.0, niobium.thermalBonusKelvin(), 1.0e-6);
        assertEquals(List.of(RocketMaterialTrait.VACUUM_REFRACTORY), niobium.traits());

        RocketMaterialProfile lonsdaleite = RocketMaterialRegistry.resolve(new ItemStack(GCItems.LONSDALEITE_GEM), SlotRole.STRUCTURAL_HULL)
                .orElseThrow();
        assertEquals(4.50, lonsdaleite.pressureMultiplier(), 1.0e-6);
        assertEquals(3.00, lonsdaleite.integrityMultiplier(), 1.0e-6);
        assertEquals(List.of(RocketMaterialTrait.CRYSTALLINE_LATTICE), lonsdaleite.traits());

        // Palladium and Magnesium resolve for both compressed plates and ingots
        assertEquals(
                RocketMaterialRegistry.resolve(new ItemStack(GCItems.COMPRESSED_PALLADIUM), SlotRole.STRUCTURAL_HULL),
                RocketMaterialRegistry.resolve(new ItemStack(GCItems.PALLADIUM_INGOT), SlotRole.STRUCTURAL_HULL)
        );
        assertEquals(
                RocketMaterialRegistry.resolve(new ItemStack(GCItems.COMPRESSED_MAGNESIUM), SlotRole.STRUCTURAL_HULL),
                RocketMaterialRegistry.resolve(new ItemStack(GCItems.MAGNESIUM_INGOT), SlotRole.STRUCTURAL_HULL)
        );
    }

    @Test
    @DisplayName("Role-specific sub-components and Helium-3 fluid canisters resolve to distinct engineering traits")
    void roleSpecificAndHelium3Resolution() {
        // 1. Helium-3 filled FLUID_CANISTER vs empty / water-filled FLUID_CANISTER in TANK_PROPELLANT_CELL
        ItemStack he3Canister = FluidCanisterItem.getFilledCanister(GCItems.FLUID_CANISTER, Gases.HELIUM_3);
        RocketMaterialProfile he3CanisterProfile = RocketMaterialRegistry.resolve(he3Canister, SlotRole.TANK_PROPELLANT_CELL)
                .orElseThrow();
        assertEquals(List.of(RocketMaterialTrait.SUPERFLUID_CRYO_2K), he3CanisterProfile.traits());
        assertEquals(0.0, he3CanisterProfile.boilOffMultiplier(), 1.0e-6);

        ItemStack emptyCanister = new ItemStack(GCItems.FLUID_CANISTER);
        RocketMaterialProfile emptyCanisterProfile = RocketMaterialRegistry.resolve(emptyCanister, SlotRole.TANK_PROPELLANT_CELL)
                .orElseThrow();
        assertFalse(emptyCanisterProfile.traits().contains(RocketMaterialTrait.SUPERFLUID_CRYO_2K));
        assertEquals(0.30, emptyCanisterProfile.capacityBonus(), 1.0e-6);

        ItemStack waterCanister = FluidCanisterItem.getFilledCanister(GCItems.FLUID_CANISTER, Fluids.WATER);
        RocketMaterialProfile waterCanisterProfile = RocketMaterialRegistry.resolve(waterCanister, SlotRole.TANK_PROPELLANT_CELL)
                .orElseThrow();
        assertFalse(waterCanisterProfile.traits().contains(RocketMaterialTrait.SUPERFLUID_CRYO_2K));

        // 2. HELIUM3_CELL resolves differently by category role (TANK / ENGINE / PAYLOAD)
        ItemStack he3Cell = new ItemStack(GCItems.HELIUM3_CELL);
        assertEquals(
                List.of(RocketMaterialTrait.SUPERFLUID_CRYO_2K),
                RocketMaterialRegistry.resolve(he3Cell, SlotRole.TANK_PROPELLANT_CELL).orElseThrow().traits()
        );
        assertEquals(
                List.of(RocketMaterialTrait.ANEUTRONIC_HE3_PLASMA),
                RocketMaterialRegistry.resolve(he3Cell, SlotRole.ENGINE_CHAMBER).orElseThrow().traits()
        );
        assertEquals(
                List.of(RocketMaterialTrait.MILLIKELVIN_CONTAINMENT),
                RocketMaterialRegistry.resolve(he3Cell, SlotRole.PAYLOAD_CORE).orElseThrow().traits()
        );

        // 3. HELIUM3_FUSION_CELL grants ANEUTRONIC_BOARD_REACTOR in COMMAND & UTILITY
        ItemStack fusionCell = new ItemStack(GCItems.HELIUM3_FUSION_CELL);
        assertEquals(
                List.of(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR),
                RocketMaterialRegistry.resolve(fusionCell, SlotRole.COMMAND_AVIONICS).orElseThrow().traits()
        );
        assertEquals(
                List.of(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR),
                RocketMaterialRegistry.resolve(fusionCell, SlotRole.UTILITY_CONTROL).orElseThrow().traits()
        );

        // 4. Context-sensitive items (ISOTHERMAL_FABRIC, FLUID_MANIPULATOR, AMBIENT_THERMAL_CONTROLLER)
        assertTrue(RocketMaterialRegistry.resolve(new ItemStack(GCItems.ISOTHERMAL_FABRIC), SlotRole.COMMAND_NOSE_RECOVERY)
                .orElseThrow().traits().contains(RocketMaterialTrait.SUPERSONIC_CHUTE));
        assertTrue(RocketMaterialRegistry.resolve(new ItemStack(GCItems.ISOTHERMAL_FABRIC), SlotRole.TANK_PROPELLANT_CELL)
                .orElseThrow().traits().contains(RocketMaterialTrait.SOFI_INSULATION));

        assertEquals(
                List.of(RocketMaterialTrait.HIGH_FLOW_MANIFOLD),
                RocketMaterialRegistry.resolve(new ItemStack(GCItems.FLUID_MANIPULATOR), SlotRole.TANK_LOWER_VALVE).orElseThrow().traits()
        );
        assertEquals(
                List.of(RocketMaterialTrait.STAGED_COMBUSTION),
                RocketMaterialRegistry.resolve(new ItemStack(GCItems.FLUID_MANIPULATOR), SlotRole.ENGINE_TURBOPUMP).orElseThrow().traits()
        );

        assertEquals(
                List.of(RocketMaterialTrait.ACTIVE_CLIMATE_CONTROL),
                RocketMaterialRegistry.resolve(new ItemStack(GCItems.AMBIENT_THERMAL_CONTROLLER), SlotRole.COMMAND_AVIONICS).orElseThrow().traits()
        );
        assertEquals(
                List.of(RocketMaterialTrait.ACTIVE_ZERO_BOILOFF),
                RocketMaterialRegistry.resolve(new ItemStack(GCItems.AMBIENT_THERMAL_CONTROLLER), SlotRole.TANK_LOWER_VALVE).orElseThrow().traits()
        );

        // 5. Invalid item for role returns empty
        assertEquals(Optional.empty(), RocketMaterialRegistry.resolve(new ItemStack(GCItems.HELIUM3_FUSION_CELL), SlotRole.STRUCTURAL_HULL));
    }

    @Test
    @DisplayName("RocketPartComposition CODEC (NbtOps & JsonOps) and STREAM_CODEC roundtrip hybrid hull + viewport with 0 unread bytes")
    void compositionCodecStreamCodecAndDataComponentRoundtrip() {
        ResourceLocation titaniumId = BuiltInRegistries.ITEM.getKey(GCItems.COMPRESSED_TITANIUM);
        ResourceLocation leadId = BuiltInRegistries.ITEM.getKey(GCItems.LEAD_INGOT);
        ResourceLocation olivineGlassId = BuiltInRegistries.ITEM.getKey(GCBlocks.OLIVINE_GLASS.asItem());
        ResourceLocation fusionCellId = BuiltInRegistries.ITEM.getKey(GCItems.HELIUM3_FUSION_CELL);

        // Deliberately pass unsorted slot indices to verify deterministic ascending sort
        RocketPartComposition original = new RocketPartComposition(List.of(
                SlotMaterialEntry.of(7, SlotRole.COMMAND_AVIONICS, fusionCellId, List.of(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR)),
                SlotMaterialEntry.of(5, SlotRole.STRUCTURAL_HULL, leadId, List.of(RocketMaterialTrait.ACID_RADIATION_SHIELD)),
                SlotMaterialEntry.of(4, SlotRole.COMMAND_VIEWPORT, olivineGlassId, List.of(RocketMaterialTrait.REFRACTORY_CANOPY)),
                SlotMaterialEntry.of(3, SlotRole.STRUCTURAL_HULL, titaniumId, List.of(RocketMaterialTrait.AEROSPACE_GRADE))
        ));

        assertEquals(4, original.size());
        assertEquals(3, original.entries().get(0).slotIndex());
        assertEquals(4, original.entries().get(1).slotIndex());
        assertEquals(5, original.entries().get(2).slotIndex());
        assertEquals(7, original.entries().get(3).slotIndex());
        assertTrue(original.hasTrait(RocketMaterialTrait.AEROSPACE_GRADE));
        assertTrue(original.hasTrait(RocketMaterialTrait.ACID_RADIATION_SHIELD));
        assertTrue(original.hasTrait(RocketMaterialTrait.REFRACTORY_CANOPY));
        assertTrue(original.hasTrait(RocketMaterialTrait.ANEUTRONIC_BOARD_REACTOR));
        assertEquals(4, original.allTraits().size());

        // 1. NbtOps roundtrip
        Tag encodedNbt = RocketPartComposition.CODEC.encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow(AssertionError::new);
        assertTrue(encodedNbt instanceof CompoundTag);
        RocketPartComposition decodedNbt = RocketPartComposition.CODEC.parse(NbtOps.INSTANCE, encodedNbt)
                .getOrThrow(AssertionError::new);
        assertEquals(original, decodedNbt);

        // 2. JsonOps roundtrip
        JsonElement encodedJson = RocketPartComposition.CODEC.encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow(AssertionError::new);
        RocketPartComposition decodedJson = RocketPartComposition.CODEC.parse(JsonOps.INSTANCE, encodedJson)
                .getOrThrow(AssertionError::new);
        assertEquals(original, decodedJson);

        // 3. Raw ByteBuf roundtrip
        ByteBuf rawBuf = Unpooled.buffer();
        try {
            RocketPartComposition.STREAM_CODEC.encode(rawBuf, original);
            assertTrue(rawBuf.readableBytes() > 0);
            RocketPartComposition decodedRaw = RocketPartComposition.STREAM_CODEC.decode(rawBuf);
            assertEquals(0, rawBuf.readableBytes(), "Raw ByteBuf must have 0 unread bytes after decode");
            assertEquals(original, decodedRaw);
        } finally {
            rawBuf.release();
        }

        // 4. RegistryFriendlyByteBuf roundtrip
        RegistryFriendlyByteBuf regBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            RocketPartComposition.STREAM_CODEC.encode(regBuf, original);
            RocketPartComposition decodedReg = RocketPartComposition.STREAM_CODEC.decode(regBuf);
            assertEquals(0, regBuf.readableBytes(), "RegistryFriendlyByteBuf must have 0 unread bytes after decode");
            assertEquals(original, decodedReg);
        } finally {
            regBuf.release();
        }

        // 5. DataComponent attachment & ItemStack copy preservation
        RocketPartConfig geminiConfig = RocketPartCatalog.getInstance().getPart("capsule_gemini_3x3_2b")
                .toConfig(RocketLivery.WHITE);
        ItemStack stack = ModularRocketPartBlockItem.createConfiguredStack(geminiConfig, null, null, original);
        assertEquals(original, stack.get(GCDataComponents.ROCKET_PART_COMPOSITION));
        assertEquals(original, stack.copy().get(GCDataComponents.ROCKET_PART_COMPOSITION));
    }
}
