package com.daniotrix.onlyend;

import java.util.function.Predicate;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Ajustes al mundo del End para que se pueda progresar sin depender de otras
 * dimensiones: algunas islas se vuelven "normales" (pasto, tierra y piedra en
 * vez de piedra del End) para conseguir madera y piedra, con animales de
 * granja pastando ahí.
 */
public final class EndWorldGenModifications {
	private EndWorldGenModifications() {
	}

	public static void register() {
		Predicate<BiomeSelectionContext> islandBiomes = BiomeSelectors.includeByKey(
				Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.SMALL_END_ISLANDS);

		// Primero convierte parte de la isla en terreno normal, y recién después le pone árboles encima.
		ResourceKey<PlacedFeature> normalIsland = ResourceKey.create(Registries.PLACED_FEATURE, OnlyEnd.id("normal_island"));
		BiomeModifications.addFeature(islandBiomes, GenerationStep.Decoration.LOCAL_MODIFICATIONS, normalIsland);

		// Varios tipos de árboles de distintos biomas del Overworld, para que no todas las islas se vean igual.
		BiomeModifications.addFeature(islandBiomes, GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_PLAINS);
		BiomeModifications.addFeature(islandBiomes, GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_BIRCH);
		BiomeModifications.addFeature(islandBiomes, GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_TAIGA);
		BiomeModifications.addFeature(islandBiomes, GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_SAVANNA);
		BiomeModifications.addFeature(islandBiomes, GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_JUNGLE);

		// Solo aparecen donde de verdad hay pasto (o sea, en las islas ya convertidas), en cualquier otro lado no tienen dónde pararse.
		BiomeModifications.addSpawn(islandBiomes, MobCategory.CREATURE, EntityTypes.SHEEP, 10, 2, 4);
		BiomeModifications.addSpawn(islandBiomes, MobCategory.CREATURE, EntityTypes.COW, 8, 2, 4);
		BiomeModifications.addSpawn(islandBiomes, MobCategory.CREATURE, EntityTypes.PIG, 8, 2, 4);
		BiomeModifications.addSpawn(islandBiomes, MobCategory.CREATURE, EntityTypes.CHICKEN, 8, 2, 4);

		// El Enderman es el único monstruo del End, así que se queda con todos los intentos de generación de
		// monstruos; en el Overworld ese mismo peso se reparte entre zombies, esqueletos, creepers, etc. Bajarle
		// el grupo (de 4 fijo a 1-2) ya achica el "combo" que aparece de una sola vez; el límite real de
		// densidad total lo pone EndermanPopulationLimiter.
		Predicate<BiomeSelectionContext> allEndBiomes = BiomeSelectors.foundInTheEnd();
		BiomeModifications.create(OnlyEnd.id("fewer_endermen")).add(ModificationPhase.REPLACEMENTS, allEndBiomes, context -> {
			context.getMobSpawnSettings().removeSpawnsOfEntityType(EntityTypes.ENDERMAN);
			context.getMobSpawnSettings().addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(EntityTypes.ENDERMAN, 1, 2), 10);
		});
	}
}
