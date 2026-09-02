package com.daniotrix.onlyend;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * El Stronghold del End no tiene una grilla de posiciones "naturales" como minecraft:random_spread o
 * minecraft:concentric_rings — el único chunk válido es el que EyeOfEnderUnlock eligió (a una
 * distancia aleatoria del punto donde el jugador lanzó su primer Ojo de Ender) y forzó a generar en
 * el momento del lanzamiento. Antes de ese lanzamiento, o para cualquier otro chunk, esto nunca es
 * una celda de estructura válida.
 */
public final class StrongholdUnlockAwarePlacement extends StructurePlacement {
	public static final MapCodec<StrongholdUnlockAwarePlacement> CODEC = RecordCodecBuilder.mapCodec(
			instance -> placementCodec(instance).apply(instance, StrongholdUnlockAwarePlacement::new)
	);

	public static final StructurePlacementType<StrongholdUnlockAwarePlacement> TYPE =
			Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, OnlyEnd.id("stronghold_unlock_aware"), () -> CODEC);

	public StrongholdUnlockAwarePlacement(
			Vec3i locateOffset,
			StructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
			float frequency,
			int salt,
			Optional<StructurePlacement.ExclusionZone> exclusionZone
	) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState generatorState, int sourceX, int sourceZ) {
		return EyeOfEnderUnlock.matchesTarget(sourceX, sourceZ);
	}

	@Override
	public StructurePlacementType<?> type() {
		return TYPE;
	}

	public static void register() {
		// Cuerpo vacío: alcanza con tocar la clase para que corran sus inicializadores estáticos
		// (CODEC y TYPE) antes de que el juego empiece a leer los structure_set del datapack.
	}
}
