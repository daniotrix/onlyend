package com.daniotrix.onlyend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;

/**
 * Cambia campos del spawner directamente, sin pasar por métodos como
 * {@code load}/{@code setEntityId} (que recargan NBT o llaman
 * {@code setChanged()}) — esos disparan lógica interna que lee o marca
 * bloques del propio chunk que todavía se está generando, y eso se traba a
 * sí mismo (deadlock, mata el servidor el watchdog).
 */
@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {
	@Accessor("maxNearbyEntities")
	void onlyend$setMaxNearbyEntities(int value);

	@Accessor("maxNearbyEntities")
	int onlyend$getMaxNearbyEntities();

	@Accessor("nextSpawnData")
	void onlyend$setNextSpawnData(SpawnData data);

	@Accessor("spawnPotentials")
	void onlyend$setSpawnPotentials(WeightedList<SpawnData> potentials);
}
