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
import net.minecraft.world.level.levelgen.structure.structures.StrongholdStructure;

/**
 * Mismo enfoque que ReskinNetherFortress: recorre la columna completa del chunk (por las mismas
 * columnas de soporte que bajan hasta el fondo del mundo) y reemplaza los bloques de piedra por sus
 * equivalentes del End, las puertas de madera/hierro por una única puerta de Birch (el tono de
 * madera más parecido al color del End Stone) y el spawner de Lepisma por uno de Endermite.
 */
public final class ReskinStronghold {
	private static final Map<Block, Block> BLOCK_REMAP = Map.of(
			Blocks.STONE_BRICKS, Blocks.END_STONE_BRICKS,
			Blocks.CRACKED_STONE_BRICKS, Blocks.END_STONE_BRICKS,
			Blocks.MOSSY_STONE_BRICKS, Blocks.PURPUR_BLOCK,
			Blocks.INFESTED_STONE_BRICKS, Blocks.END_STONE_BRICKS,
			Blocks.STONE_BRICK_STAIRS, Blocks.END_STONE_BRICK_STAIRS,
			Blocks.STONE_BRICK_SLAB, Blocks.END_STONE_BRICK_SLAB,
			Blocks.COBBLESTONE, Blocks.END_STONE,
			Blocks.COBBLESTONE_STAIRS, Blocks.END_STONE_BRICK_STAIRS,
			Blocks.SMOOTH_STONE_SLAB, Blocks.END_STONE_BRICK_SLAB);

	private ReskinStronghold() {
	}

	public static void register() {
		ServerChunkEvents.CHUNK_GENERATE.register((level, chunk) -> {
			if (level.dimension() != Level.END) {
				return;
			}

			boolean touchesStronghold = !level.structureManager()
					.startsForStructure(chunk.getPos(), structure -> structure instanceof StrongholdStructure)
					.isEmpty();

			if (touchesStronghold) {
				reskin(chunk);
			}
		});
	}

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

					if (oldState.is(Blocks.OAK_DOOR) || oldState.is(Blocks.IRON_DOOR)) {
						chunk.setBlockState(pos, copyShape(oldState, Blocks.BIRCH_DOOR));
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

		// Mismo motivo que en ReskinNetherFortress: el chunk todavía se está generando, así que hay
		// que escribir el SpawnData a mano con el accessor en vez de usar setEntityId/loadWithComponents.
		CompoundTag entityTag = new CompoundTag();
		entityTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityTypes.ENDERMITE).toString());
		SpawnData endermiteSpawn = new SpawnData(entityTag, Optional.empty(), Optional.empty());

		BaseSpawner spawner = spawnerEntity.getSpawner();
		BaseSpawnerAccessor accessor = (BaseSpawnerAccessor) spawner;
		accessor.onlyend$setNextSpawnData(endermiteSpawn);
		accessor.onlyend$setSpawnPotentials(WeightedList.<SpawnData>builder().add(endermiteSpawn, 1).build());
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
