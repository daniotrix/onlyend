package com.daniotrix.onlyend;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Copia exacta de la forma de EndGatewayFeature (la jaula de bedrock en cruz alrededor del bloque
 * central) pero poniendo lava en vez de un End Gateway real en el centro — así el override de
 * end_gateway_return.json sigue viéndose igual que el original (mismo bedrock, mismo hueco de aire),
 * solo que ya no hay portal que lleve a ningún lado.
 */
public final class LavaGatewayFeature extends Feature<NoneFeatureConfiguration> {
	public LavaGatewayFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		BlockPos origin = context.origin();
		WorldGenLevel level = context.level();

		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-1, -2, -1), origin.offset(1, 2, 1))) {
			boolean sameX = pos.getX() == origin.getX();
			boolean sameY = pos.getY() == origin.getY();
			boolean sameZ = pos.getZ() == origin.getZ();
			boolean end = Math.abs(pos.getY() - origin.getY()) == 2;

			if (sameX && sameY && sameZ) {
				this.setBlock(level, pos.immutable(), Blocks.LAVA.defaultBlockState());
			} else if (sameY) {
				this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
			} else if (end && sameX && sameZ) {
				this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
			} else if ((sameX || sameZ) && !end) {
				this.setBlock(level, pos, Blocks.BEDROCK.defaultBlockState());
			} else {
				this.setBlock(level, pos, Blocks.AIR.defaultBlockState());
			}
		}

		return true;
	}
}
