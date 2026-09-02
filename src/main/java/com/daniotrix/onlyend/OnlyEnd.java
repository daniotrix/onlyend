package com.daniotrix.onlyend;

import net.fabricmc.api.ModInitializer;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlyEnd implements ModInitializer {
	public static final String MOD_ID = "onlyend";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Feature<NoneFeatureConfiguration> NORMAL_ISLAND_FEATURE =
			Registry.register(BuiltInRegistries.FEATURE, id("normal_island"), new NormalIslandFeature(NoneFeatureConfiguration.CODEC));

	public static final Feature<NoneFeatureConfiguration> LAVA_GATEWAY_FEATURE =
			Registry.register(BuiltInRegistries.FEATURE, id("lava_gateway"), new LavaGatewayFeature(NoneFeatureConfiguration.CODEC));

	@Override
	public void onInitialize() {
		LOGGER.info("Only End cargado.");

		EndSpawnManager.register();
		EndWorldGenModifications.register();
		VoidSafetyNet.register();
		ReskinNetherFortress.register();
		ReskinStronghold.register();
		MinDistanceRandomSpreadPlacement.register();
		StrongholdUnlockAwarePlacement.register();
		EyeOfEnderUnlock.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
