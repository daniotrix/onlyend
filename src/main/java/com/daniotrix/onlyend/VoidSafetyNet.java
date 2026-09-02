package com.daniotrix.onlyend;

import java.util.Set;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;

/**
 * Si alguien se cae al vacío y sigue bajando mucho, lo manda de vuelta a la
 * plataforma antes de que llegue a morir ahí — el propio juego tiene un bug
 * conocido que crashea al procesar esa muerte en esta versión, así que la
 * forma segura de evitarlo es no dejar que pase.
 */
public final class VoidSafetyNet {
	private static final double FALL_LIMIT_BELOW_MIN_Y = 32.0;

	private VoidSafetyNet() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerLevel endLevel = server.getLevel(Level.END);

			if (endLevel == null) {
				return;
			}

			double limitY = endLevel.getMinY() - FALL_LIMIT_BELOW_MIN_Y;

			for (ServerPlayer player : endLevel.players()) {
				if (player.getY() < limitY) {
					rescue(player, endLevel);
				}
			}
		});
	}

	private static void rescue(ServerPlayer player, ServerLevel endLevel) {
		BlockPos spawnPos = EndSpawnManager.findSpawnPos(endLevel);

		player.teleportTo(endLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, Set.<Relative>of(), 0.0f, 0.0f, false);
		player.setDeltaMovement(0.0, 0.0, 0.0);
		player.fallDistance = 0.0;
	}
}
