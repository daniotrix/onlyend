package com.daniotrix.onlyend;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Convierte de a poco una isla del End en una isla "normal" — pasto y tierra
 * arriba, piedra abajo — para que se pueda conseguir madera y piedra sin
 * salir del End. Sigue el borde superior de la isla desde donde cae, y baja
 * transformando cada columna hasta un tope, así que islas grandes solo se
 * convierten en parte (no toda la isla entera de una).
 */
public final class NormalIslandFeature extends Feature<NoneFeatureConfiguration> {
	// El juego solo garantiza que se pueda leer/escribir con seguridad hasta un radio chico alrededor del chunk que
	// se está generando; pasarse de ahí puede corromper chunks vecinos. Por eso esto se queda bien cerca del origen,
	// aunque la isla real sea mucho más grande — islas grandes simplemente se van convirtiendo de a partes con el tiempo.
	private static final int MAX_RADIUS_FROM_ORIGIN = 12;
	private static final int MAX_SURFACE_BLOCKS = 200;
	private static final int DIRT_DEPTH = 4;
	private static final int MAX_STONE_DEPTH = 40;

	public NormalIslandFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		BlockPos origin = context.origin();

		BlockPos surface = findEndStoneSurface(level, origin);

		if (surface == null) {
			return false;
		}

		Set<BlockPos> islandTop = floodFillTop(level, surface, origin);

		if (islandTop.size() < 16) {
			return false;
		}

		for (BlockPos top : islandTop) {
			terraformColumn(level, top);
		}

		return true;
	}

	private BlockPos findEndStoneSurface(WorldGenLevel level, BlockPos origin) {
		int x = origin.getX();
		int z = origin.getZ();
		int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

		for (int y = topY; y > level.getMinY(); y--) {
			BlockPos pos = new BlockPos(x, y, z);

			if (level.getBlockState(pos).is(Blocks.END_STONE) && level.getBlockState(pos.above()).isAir()) {
				return pos;
			}
		}

		return null;
	}

	// Recorre el borde superior de la isla (con algo de tolerancia de altura para seguir pendientes suaves), sin
	// alejarse nunca más de MAX_RADIUS_FROM_ORIGIN del punto donde cayó, para no salirse del área segura del chunk.
	private Set<BlockPos> floodFillTop(WorldGenLevel level, BlockPos start, BlockPos origin) {
		Set<BlockPos> visited = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		visited.add(start);

		int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

		while (!queue.isEmpty() && visited.size() < MAX_SURFACE_BLOCKS) {
			BlockPos current = queue.poll();

			for (int[] direction : directions) {
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos next = current.offset(direction[0], dy, direction[1]);

					if (visited.contains(next) || !withinSafeRadius(next, origin)) {
						continue;
					}

					if (level.getBlockState(next).is(Blocks.END_STONE) && level.getBlockState(next.above()).isAir()) {
						visited.add(next);
						queue.add(next);
					}
				}
			}
		}

		return visited;
	}

	private boolean withinSafeRadius(BlockPos pos, BlockPos origin) {
		int dx = pos.getX() - origin.getX();
		int dz = pos.getZ() - origin.getZ();
		return dx * dx + dz * dz <= MAX_RADIUS_FROM_ORIGIN * MAX_RADIUS_FROM_ORIGIN;
	}

	private void terraformColumn(WorldGenLevel level, BlockPos top) {
		level.setBlock(top, Blocks.GRASS_BLOCK.defaultBlockState(), 2);

		int depth = 1;

		for (; depth <= DIRT_DEPTH; depth++) {
			BlockPos pos = top.below(depth);

			if (!level.getBlockState(pos).is(Blocks.END_STONE)) {
				return;
			}

			level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
		}

		for (; depth <= MAX_STONE_DEPTH; depth++) {
			BlockPos pos = top.below(depth);
			BlockState state = level.getBlockState(pos);

			if (!state.is(Blocks.END_STONE)) {
				return;
			}

			level.setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
		}
	}
}
