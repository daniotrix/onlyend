package com.daniotrix.onlyend;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * Igual que minecraft:random_spread, pero además descarta cualquier celda de la grilla cuyo chunk
 * elegido caiga a menos de min_distance_from_origin bloques del origen del mundo (0,0) — que es
 * donde siempre termina apareciendo el jugador en el End (ver EndSpawnManager). Ni spacing ni
 * separation garantizan esto por sí solos: la celda que cubre el origen puede rodar un offset de 0
 * y terminar la estructura justo ahí. Esto no mueve la estructura más lejos, simplemente esa celda
 * no genera nada, así que nunca hay una instancia dentro del radio configurado.
 */
public final class MinDistanceRandomSpreadPlacement extends RandomSpreadStructurePlacement {
	public static final MapCodec<MinDistanceRandomSpreadPlacement> CODEC = RecordCodecBuilder.<MinDistanceRandomSpreadPlacement>mapCodec(
			instance -> placementCodec(instance)
					.and(
							instance.group(
									Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
									Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
									RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType),
									Codec.intRange(0, 1_000_000).fieldOf("min_distance_from_origin").forGetter(MinDistanceRandomSpreadPlacement::minDistanceFromOrigin)
							)
					)
					.apply(instance, MinDistanceRandomSpreadPlacement::new)
	).validate(MinDistanceRandomSpreadPlacement::validate);

	public static final StructurePlacementType<MinDistanceRandomSpreadPlacement> TYPE =
			Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, OnlyEnd.id("min_distance_random_spread"), () -> CODEC);

	private final int minDistanceFromOrigin;

	private static DataResult<MinDistanceRandomSpreadPlacement> validate(MinDistanceRandomSpreadPlacement placement) {
		return placement.spacing() <= placement.separation()
				? DataResult.error(() -> "Spacing has to be larger than separation")
				: DataResult.success(placement);
	}

	public MinDistanceRandomSpreadPlacement(
			Vec3i locateOffset,
			StructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
			float frequency,
			int salt,
			Optional<StructurePlacement.ExclusionZone> exclusionZone,
			int spacing,
			int separation,
			RandomSpreadType spreadType,
			int minDistanceFromOrigin
	) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, spreadType);
		this.minDistanceFromOrigin = minDistanceFromOrigin;
	}

	public int minDistanceFromOrigin() {
		return this.minDistanceFromOrigin;
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int sourceX, int sourceZ) {
		if (!super.isPlacementChunk(state, sourceX, sourceZ)) {
			return false;
		}

		long blockX = (long) sourceX << 4;
		long blockZ = (long) sourceZ << 4;
		double distanceSquared = (double) (blockX * blockX + blockZ * blockZ);

		return distanceSquared >= (double) this.minDistanceFromOrigin * this.minDistanceFromOrigin;
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
