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

package dev.galacticraft.mod.content.teleporters;

import com.mojang.serialization.Codec;
import dev.galacticraft.api.universe.celestialbody.CelestialBody;
import dev.galacticraft.api.universe.celestialbody.landable.teleporter.config.CelestialTeleporterConfig;
import dev.galacticraft.api.universe.celestialbody.landable.teleporter.type.CelestialTeleporterType;
import dev.galacticraft.impl.universe.celestialbody.landable.teleporter.config.DefaultCelestialTeleporterConfig;
import dev.galacticraft.mod.Constant;
import dev.galacticraft.mod.attachments.GCServerPlayer;
import dev.galacticraft.mod.content.GCEntityTypes;
import dev.galacticraft.mod.content.entity.vehicle.LanderEntity;
import dev.galacticraft.mod.content.entity.vehicle.ModularRocketFlightEntity;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;

public class LanderCelestialTeleporterType<Config extends CelestialTeleporterConfig> extends CelestialTeleporterType<Config> {
    public static final LanderCelestialTeleporterType<DefaultCelestialTeleporterConfig> INSTANCE = new LanderCelestialTeleporterType<>(DefaultCelestialTeleporterConfig.CODEC);

    public LanderCelestialTeleporterType(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public void onEnterAtmosphere(ServerLevel level, Entity entity, CelestialBody<?, ?> body, CelestialBody<?, ?> fromBody, Config config) {
        double reentryY = Constant.REENTRY_HEIGHT;
        if (entity instanceof ServerPlayer player) {
            GCServerPlayer gcPlayer = GCServerPlayer.get(player);
            if (gcPlayer.hasModularRocketGraph()) {
                double modularEntryY = Math.min(reentryY, level.getMaxBuildHeight() + 140.0);
                player.teleportTo(level, player.getX(), modularEntryY, player.getZ(), NO_RELATIVE_MOVEMENT, 0.0F, 0.0F);
                ModularRocketFlightEntity descentRocket = new ModularRocketFlightEntity(GCEntityTypes.MODULAR_ROCKET_FLIGHT, level);
                descentRocket.setPos(player.getX(), modularEntryY, player.getZ());
                descentRocket.initializeFlightState(
                        gcPlayer.getModularRocketGraph(),
                        gcPlayer.getModularRocketEnergy(),
                        gcPlayer.getModularRocketOxygen(),
                        Direction.NORTH,
                        null,
                        level.getSeaLevel(),
                        ModularRocketFlightEntity.FlightPhase.RETRO_DESCENT,
                        List.of(player.getUUID()),
                        Map.of()
                );
                gcPlayer.clearModularRocketState();
                if (level.addFreshEntity(descentRocket)) {
                    player.startRiding(descentRocket, true);
                    return;
                }
            }
        }
        entity.teleportTo(level, entity.getX(), reentryY, entity.getZ(), NO_RELATIVE_MOVEMENT, -90.0F, 45.0F);
        if (entity instanceof ServerPlayer player) {
            LanderEntity lander = new LanderEntity(player);
            level.addFreshEntity(lander);
            lander.setPos(player.getX(), reentryY, player.getZ());
            player.startRiding(lander, true);
        }
    }
}
