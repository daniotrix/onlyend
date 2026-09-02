package com.daniotrix.onlyend;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * El Stronghold del End no existe hasta que algún jugador usa su primer Ojo de Ender en el End — en
 * ese momento buscamos un punto entre MIN_DISTANCE_BLOCKS y MAX_DISTANCE_BLOCKS de donde está parado
 * que además caiga en un bioma de END_FORTRESS_BIOMES (igual que exige la estructura en su JSON), lo
 * guardamos, y forzamos la generación de ese chunk ahí mismo. Sin ese chequeo de bioma, Structure.
 * generate() descarta la pieza en silencio (StructureStart.INVALID_START) si el punto elegido cae en
 * vacío o en un bioma no listado — el End es mayormente islas sueltas separadas por vacío, así que un
 * punto al azar cae fuera de bioma válido la mayoría de las veces. StrongholdUnlockAwarePlacement solo
 * acepta ese único chunk ya encontrado como posición válida.
 *
 * TODOS los lanzamientos del Ojo de Ender en el End los resolvemos nosotros a mano, no solo el
 * primero: ChunkGenerator.findNearestMapStructure (lo que usa tanto EnderEyeItem.use() en
 * lanzamientos normales como el comando /locate) solo sabe buscar estructuras cuyo placement sea
 * exactamente minecraft:concentric_rings o minecraft:random_spread — cualquier otro tipo de
 * placement, como el nuestro, queda invisible para esa búsqueda sin importar la distancia ni si ya se
 * generó. Como nosotros ya sabemos exactamente dónde está (lo elegimos y lo guardamos), no hace falta
 * que vanilla lo "busque": simplemente apuntamos siempre hacia esa ubicación guardada.
 */
public final class EyeOfEnderUnlock {
	private static final TagKey<Biome> END_FORTRESS_BIOMES = TagKey.create(Registries.BIOME, OnlyEnd.id("end_fortress_biomes"));
	private static final int MIN_DISTANCE_BLOCKS = 2000;
	private static final int MAX_DISTANCE_BLOCKS = 3000;
	private static final int DISTANCE_STEP_BLOCKS = 64;
	private static final int MAX_ANGLE_ATTEMPTS = 64;

	private static volatile boolean unlocked;
	private static volatile int targetChunkX;
	private static volatile int targetChunkZ;

	private EyeOfEnderUnlock() {
	}

	public static boolean matchesTarget(int chunkX, int chunkZ) {
		return unlocked && chunkX == targetChunkX && chunkZ == targetChunkZ;
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ServerLevel endLevel = server.getLevel(Level.END);

			if (endLevel == null) {
				unlocked = false;
				return;
			}

			StrongholdUnlockData data = endLevel.getDataStorage().computeIfAbsent(StrongholdUnlockData.TYPE);
			unlocked = data.isUnlocked();
			targetChunkX = data.targetChunkX();
			targetChunkZ = data.targetChunkZ();
		});

		UseItemCallback.EVENT.register((player, level, hand) -> {
			ItemStack itemStack = player.getItemInHand(hand);

			if (level.dimension() != Level.END || !itemStack.is(Items.ENDER_EYE)) {
				return InteractionResult.PASS;
			}

			if (!(level instanceof ServerLevel endLevel)) {
				return InteractionResult.PASS;
			}

			if (unlocked) {
				return throwEyeTowards(endLevel, player, hand, itemStack, storedTargetCenter());
			}

			BlockPos target = findValidTarget(endLevel, player);

			if (target == null) {
				if (player instanceof ServerPlayer serverPlayer) {
					serverPlayer.sendSystemMessage(Component.literal("No se encontró un lugar válido para el Stronghold cerca — probá de nuevo."), true);
				}

				return InteractionResult.FAIL;
			}

			forceUnlockAt(endLevel, target);
			return throwEyeTowards(endLevel, player, hand, itemStack, target);
		});
	}

	private static BlockPos storedTargetCenter() {
		return new BlockPos(SectionPos.sectionToBlockCoord(targetChunkX, 8), 0, SectionPos.sectionToBlockCoord(targetChunkZ, 8));
	}

	// Prueba varias direcciones al azar y, en cada una, recorre la franja de distancia pedida buscando
	// el primer punto que caiga en un bioma de END_FORTRESS_BIOMES — igual que hace vanilla con
	// concentric_rings (findBiomeHorizontal), pero centrado en el jugador en vez del origen del mundo.
	private static BlockPos findValidTarget(ServerLevel endLevel, Player player) {
		RandomSource random = endLevel.getRandom();
		int originX = player.getBlockX();
		int originZ = player.getBlockZ();

		for (int attempt = 0; attempt < MAX_ANGLE_ATTEMPTS; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;

			for (int distance = MIN_DISTANCE_BLOCKS; distance <= MAX_DISTANCE_BLOCKS; distance += DISTANCE_STEP_BLOCKS) {
				int x = originX + (int) Math.round(Math.cos(angle) * distance);
				int z = originZ + (int) Math.round(Math.sin(angle) * distance);
				BlockPos candidate = new BlockPos(x, 0, z);

				if (endLevel.getBiome(candidate).is(END_FORTRESS_BIOMES)) {
					return candidate;
				}
			}
		}

		return null;
	}

	private static void forceUnlockAt(ServerLevel endLevel, BlockPos target) {
		int chunkX = SectionPos.blockToSectionCoord(target.getX());
		int chunkZ = SectionPos.blockToSectionCoord(target.getZ());

		unlocked = true;
		targetChunkX = chunkX;
		targetChunkZ = chunkZ;
		endLevel.getDataStorage().computeIfAbsent(StrongholdUnlockData.TYPE).markUnlocked(chunkX, chunkZ);

		// Fuerza la generación de ese chunk ya mismo — no hace falta que el jugador lo explore para que
		// el stronghold exista ahí.
		endLevel.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);

		OnlyEnd.LOGGER.info(
				"Only End: Stronghold forzado en el chunk [{}, {}] (~{}, ~{})",
				chunkX, chunkZ, target.getX(), target.getZ()
		);
	}

	private static InteractionResult throwEyeTowards(ServerLevel endLevel, Player player, InteractionHand hand, ItemStack itemStack, BlockPos target) {
		player.startUsingItem(hand);

		EyeOfEnder eyeOfEnder = new EyeOfEnder(endLevel, player.getX(), player.getY(0.5), player.getZ());
		eyeOfEnder.setItem(itemStack);
		eyeOfEnder.signalTo(Vec3.atLowerCornerOf(target));
		endLevel.gameEvent(GameEvent.PROJECTILE_SHOOT, eyeOfEnder.position(), GameEvent.Context.of(player));
		endLevel.addFreshEntity(eyeOfEnder);

		if (player instanceof ServerPlayer serverPlayer) {
			CriteriaTriggers.USED_ENDER_EYE.trigger(serverPlayer, target);
		}

		float pitch = Mth.lerp(endLevel.getRandom().nextFloat(), 0.33F, 0.5F);
		endLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, pitch);
		itemStack.consume(1, player);
		player.awardStat(Stats.ITEM_USED.get(Items.ENDER_EYE));

		return InteractionResult.SUCCESS;
	}
}
