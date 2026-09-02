package com.daniotrix.onlyend;

import java.util.Set;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import com.daniotrix.onlyend.mixin.EntityLevelAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.LevelData;

/**
 * Hace que cualquier jugador, al entrar o al reaparecer, termine siempre en
 * el End — no en la plataforma clásica junto al Dragón, sino cerca de una
 * End City, para que no sea tan fácil pero tampoco imposible.
 */
public final class EndSpawnManager {
	private static final TagKey<Structure> END_CITY_SEARCH_TAG = TagKey.create(Registries.STRUCTURE, OnlyEnd.id("end_city_search"));
	private static final int SEARCH_RADIUS = 100;
	private static final int MIN_SAFE_GROUND_Y = 20;
	private static final int GROUND_SEARCH_MAX_RADIUS = 64;
	private static final int GROUND_SEARCH_STEP = 4;

	private static BlockPos cachedSpawnPos;

	private EndSpawnManager() {
	}

	public static void register() {
		// Busca la plataforma de aparición apenas termina de arrancar el servidor, no cuando se conecta el primer
		// jugador — hacerlo tan pronto como alguien entra puede correr antes de que el juego esté listo para buscar
		// estructuras, y a veces la búsqueda fallaba sin motivo aparente.
		ServerLifecycleEvents.SERVER_STARTED.register(EndSpawnManager::prepareSpawn);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendToEnd(handler.getPlayer()));
	}

	private static void prepareSpawn(MinecraftServer server) {
		ServerLevel endLevel = server.getLevel(Level.END);

		if (endLevel != null) {
			// Cada mundo que se crea es un servidor nuevo con su propio terreno, pero esta clase sigue viva
			// mientras el juego esté abierto — sin este reseteo, un mundo nuevo heredaría la ubicación calculada
			// para el mundo anterior, que ya no tiene nada que ver con el terreno real de este.
			cachedSpawnPos = null;
			findSpawnPos(endLevel);
		}
	}

	/**
	 * Manda al jugador a la isla del Dragón de verdad (la plataforma clásica junto a los pilares de
	 * obsidiana), asegurando que esa plataforma exista — acá nunca se creó de la forma normal, porque
	 * los jugadores jamás entran al End por un portal real desde el Overworld. Se usa cuando alguien
	 * cruza el portal del Stronghold (ver EndCreditsRedirectMixin): a diferencia del punto de
	 * aparición inicial (cerca de una End City, ver findSpawnPos), este sí lleva al lugar de la pelea
	 * contra el Dragón.
	 */
	public static void teleportToDragonIsland(ServerPlayer player) {
		ServerLevel endLevel = player.level().getServer().getLevel(Level.END);

		if (endLevel == null) {
			return;
		}

		BlockPos landing = ServerLevel.END_SPAWN_POINT;
		EndPlatformFeature.createEndPlatform(endLevel, landing.below(), true);
		player.teleportTo(endLevel, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, Set.<Relative>of(), 0.0f, 0.0f, false);
	}

	/**
	 * Igual que sendToEnd, pero teletransporta siempre, sin importar si el jugador ya está en el End —
	 * sendToEnd solo mueve al jugador cuando todavía NO está en el End (es una red de seguridad para
	 * cuando recién se conecta), así que no sirve para volver desde la propia isla del Dragón hacia el
	 * punto de aparición inicial (ver EndCreditsRedirectMixin).
	 */
	public static void teleportToInitialSpawn(ServerPlayer player) {
		ServerLevel endLevel = player.level().getServer().getLevel(Level.END);

		if (endLevel == null) {
			return;
		}

		BlockPos spawnPos = findSpawnPos(endLevel);
		player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(Level.END, spawnPos, 0.0f, 0.0f), true), false);
		player.teleportTo(endLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, Set.<Relative>of(), 0.0f, 0.0f, false);
	}

	/** Asegura la plataforma de llegada y manda al jugador al End si todavía no está ahí (red de seguridad para cuando ya está conectado). */
	public static void sendToEnd(ServerPlayer player) {
		ServerLevel currentLevel = player.level();
		ServerLevel endLevel = currentLevel.getServer().getLevel(Level.END);

		if (endLevel == null) {
			return;
		}

		BlockPos spawnPos = findSpawnPos(endLevel);

		player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(Level.END, spawnPos, 0.0f, 0.0f), true), false);

		if (player.level().dimension() != Level.END) {
			player.teleportTo(endLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, Set.<Relative>of(), 0.0f, 0.0f, false);
		}
	}

	/**
	 * Pone al jugador directamente en el End antes de que se le mande ni un
	 * solo chunk del Overworld — se usa justo al inicio de
	 * {@code PlayerList.placeNewPlayer}, cuando todavía no existe ni la
	 * conexión de juego, así que el cliente nunca llega a ver el Overworld.
	 */
	public static void placeDirectlyInEnd(ServerPlayer player, ServerLevel endLevel) {
		BlockPos spawnPos = findSpawnPos(endLevel);

		((EntityLevelAccessor) (Object) player).onlyend$setLevel(endLevel);
		// El modo de juego (el que valida romper/poner bloques) guarda su propia referencia al nivel por separado
		// de la entidad — si no se actualiza también, se queda validando contra el Overworld y deshace cada bloque roto.
		player.gameMode.setLevel(endLevel);
		player.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
		player.setYRot(0.0f);
		player.setXRot(0.0f);
	}

	// Busca la End City más cercana al punto clásico de llegada y prepara una plataforma segura ahí; si no encuentra ninguna, usa exactamente el mismo punto y la misma plataforma que ya usa el juego al entrar por un portal real (sin inventarle una altura nueva).
	static BlockPos findSpawnPos(ServerLevel endLevel) {
		if (cachedSpawnPos != null) {
			return cachedSpawnPos;
		}

		BlockPos searchOrigin = ServerLevel.END_SPAWN_POINT;
		BlockPos cityPos = endLevel.findNearestMapStructure(END_CITY_SEARCH_TAG, searchOrigin, SEARCH_RADIUS, false);
		BlockPos cityGround = cityPos != null ? findNearbySolidGround(endLevel, cityPos.getX(), cityPos.getZ()) : null;

		BlockPos landing;

		if (cityGround != null) {
			// Ya hay suelo de verdad ahí, no hace falta inventar una plataforma de obsidiana encima.
			landing = cityGround.above();
		} else {
			// Sin ciudad cerca, el único lugar seguro conocido es el punto clásico sobre el vacío — ahí sí hace falta la plataforma.
			landing = searchOrigin;
			EndPlatformFeature.createEndPlatform(endLevel, landing.below(), true);
		}

		OnlyEnd.LOGGER.info("Only End: apareciendo en {}", landing);
		cachedSpawnPos = landing;
		return landing;
	}

	// La City no siempre tiene suelo sólido justo en la esquina donde el juego la registra, así que busca en anillos cada vez más anchos hasta encontrar una columna con terreno de verdad debajo.
	private static BlockPos findNearbySolidGround(ServerLevel endLevel, int centerX, int centerZ) {
		for (int radius = 0; radius <= GROUND_SEARCH_MAX_RADIUS; radius += GROUND_SEARCH_STEP) {
			for (int dx = -radius; dx <= radius; dx += GROUND_SEARCH_STEP) {
				for (int dz = -radius; dz <= radius; dz += GROUND_SEARCH_STEP) {
					boolean onRingEdge = Math.abs(dx) == radius || Math.abs(dz) == radius;

					if (!onRingEdge) {
						continue;
					}

					int x = centerX + dx;
					int z = centerZ + dz;
					// La estructura ya se sabe dónde va a estar (por el algoritmo de ubicación), pero eso no
					// significa que ese chunk ya esté generado de verdad — hay que forzarlo antes de medir su altura.
					endLevel.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
					int groundY = endLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

					if (groundY >= endLevel.getMinY() + MIN_SAFE_GROUND_Y) {
						return new BlockPos(x, groundY, z);
					}
				}
			}
		}

		return null;
	}
}
