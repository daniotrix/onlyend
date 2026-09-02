package com.daniotrix.onlyend;

import java.util.Map;
import java.util.Optional;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;

import com.daniotrix.onlyend.mixin.BaseSpawnerAccessor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;

/**
 * La fortaleza reutiliza el generador de piezas original de la fortaleza del
 * Nether (no es algo que se pueda cambiar por datapack), así que sigue
 * construyéndose con ladrillos del Nether. Esto la recorre justo después de
 * generarse y cambia esos bloques por su equivalente del End, conservando la
 * forma de escaleras/losas/etc. mediante sus mismas propiedades de bloque.
 */
public final class ReskinNetherFortress {
	private static final Map<Block, Block> BLOCK_REMAP = Map.of(
			Blocks.NETHER_BRICKS, Blocks.PURPUR_BLOCK,
			Blocks.CRACKED_NETHER_BRICKS, Blocks.PURPUR_BLOCK,
			Blocks.CHISELED_NETHER_BRICKS, Blocks.PURPUR_PILLAR,
			Blocks.RED_NETHER_BRICKS, Blocks.PURPUR_BLOCK,
			Blocks.NETHER_BRICK_STAIRS, Blocks.PURPUR_STAIRS,
			Blocks.NETHER_BRICK_SLAB, Blocks.PURPUR_SLAB,
			Blocks.NETHER_BRICK_WALL, Blocks.PURPUR_PILLAR,
			Blocks.NETHER_BRICK_FENCE, Blocks.END_ROD,
			Blocks.NETHERRACK, Blocks.END_STONE);

	// El spawner de Blaze que trae la pieza original casi nunca llega a disparar de verdad (compite con
	// los Blazes que ya aparecen solos por el resto de la fortaleza, entre otras cosas), así que en vez de
	// seguir peleando con eso lo convertimos en un spawner de Shulker — tiene más sentido en el End de
	// todos modos. Los Blazes ya se consiguen bien con la aparición ambiente (ver end_fortress.json).
	private static final int SPAWNER_MAX_NEARBY_ENTITIES = 24;

	private ReskinNetherFortress() {
	}

	public static void register() {
		ServerChunkEvents.CHUNK_GENERATE.register((level, chunk) -> {
			if (level.dimension() != Level.END) {
				return;
			}

			// Un fortaleza completa ocupa muchos chunks, pero cada StructureStart solo vive de verdad en el
			// chunk donde "empezó" — los demás chunks que toca solo guardan una referencia. startsForStructure
			// resuelve eso y nos dice si este chunk es parte de la estructura, sin importar en qué chunk se originó.
			boolean touchesFortress = !level.structureManager()
					.startsForStructure(chunk.getPos(), structure -> structure instanceof NetherFortressStructure)
					.isEmpty();

			if (touchesFortress) {
				reskin(chunk);
			}
		});
	}

	// Varias piezas de la fortaleza (las que arman puentes/pasillos) bajan columnas de soporte con
	// fillColumnDown, que rellena en línea recta hasta el fondo del mundo — muy por debajo del bounding
	// box que la pieza reporta. Por eso no alcanza con acotar el rango vertical a esa caja: hay que
	// recorrer la columna completa del chunk. Es seguro porque BLOCK_REMAP solo tiene bloques exclusivos
	// del set de la fortaleza del Nether, que nunca aparecen naturalmente en el End.
	private static void reskin(LevelChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		int minX = chunkPos.getMinBlockX();
		int maxX = chunkPos.getMaxBlockX();
		int minZ = chunkPos.getMinBlockZ();
		int maxZ = chunkPos.getMaxBlockZ();

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = chunk.getMinY(); y <= chunk.getMaxY(); y++) {
					BlockPos pos = new BlockPos(x, y, z);
					BlockState oldState = chunk.getBlockState(pos);

					if (oldState.is(Blocks.SPAWNER)) {
						reskinSpawner(chunk, pos);
						continue;
					}

					Block replacement = BLOCK_REMAP.get(oldState.getBlock());

					if (replacement != null) {
						chunk.setBlockState(pos, copyShape(oldState, replacement));
					}
				}
			}
		}
	}

	private static void reskinSpawner(LevelChunk chunk, BlockPos pos) {
		BlockEntity blockEntity = chunk.getBlockEntity(pos);

		if (!(blockEntity instanceof SpawnerBlockEntity spawnerEntity)) {
			return;
		}

		// Ni setEntityId ni loadWithComponents sirven acá: los dos terminan llamando algo que toca el
		// nivel real (setChanged() uno, la carga de NBT el otro) para marcar el chunk como modificado o
		// elegir una entidad de exhibición — y ese chunk todavía se está generando, así que se traba
		// esperándose a sí mismo (deadlock, mata el servidor el watchdog). Construir el SpawnData a mano y
		// escribirlo directo con el accessor no toca el nivel para nada.
		CompoundTag entityTag = new CompoundTag();
		entityTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityTypes.SHULKER).toString());
		SpawnData shulkerSpawn = new SpawnData(entityTag, Optional.empty(), Optional.empty());

		BaseSpawner spawner = spawnerEntity.getSpawner();
		BaseSpawnerAccessor accessor = (BaseSpawnerAccessor) spawner;
		accessor.onlyend$setNextSpawnData(shulkerSpawn);
		accessor.onlyend$setSpawnPotentials(WeightedList.<SpawnData>builder().add(shulkerSpawn, 1).build());

		if (accessor.onlyend$getMaxNearbyEntities() < SPAWNER_MAX_NEARBY_ENTITIES) {
			accessor.onlyend$setMaxNearbyEntities(SPAWNER_MAX_NEARBY_ENTITIES);
		}
	}

	private static BlockState copyShape(BlockState oldState, Block newBlock) {
		BlockState newState = newBlock.defaultBlockState();

		for (Property<?> property : oldState.getProperties()) {
			if (newState.hasProperty(property)) {
				newState = copyProperty(oldState, newState, property);
			}
		}

		return newState;
	}

	private static <T extends Comparable<T>> BlockState copyProperty(BlockState oldState, BlockState newState, Property<T> property) {
		return newState.setValue(property, oldState.getValue(property));
	}
}
