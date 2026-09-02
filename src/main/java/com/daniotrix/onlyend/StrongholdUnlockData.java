package com.daniotrix.onlyend;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Guarda si ya se desbloqueó la generación del Stronghold del End y en qué chunk exacto se decidió
 * forzarla (ver EyeOfEnderUnlock), para que sobreviva a un reinicio del server en vez de tener que
 * volver a lanzar un Ojo de Ender cada vez.
 */
public final class StrongholdUnlockData extends SavedData {
	public static final Codec<StrongholdUnlockData> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
							Codec.BOOL.fieldOf("unlocked").forGetter(StrongholdUnlockData::isUnlocked),
							Codec.INT.fieldOf("target_chunk_x").forGetter(StrongholdUnlockData::targetChunkX),
							Codec.INT.fieldOf("target_chunk_z").forGetter(StrongholdUnlockData::targetChunkZ)
					)
					.apply(instance, StrongholdUnlockData::new)
	);

	public static final SavedDataType<StrongholdUnlockData> TYPE =
			new SavedDataType<>(OnlyEnd.id("stronghold_unlock"), StrongholdUnlockData::new, CODEC, DataFixTypes.LEVEL);

	private boolean unlocked;
	private int targetChunkX;
	private int targetChunkZ;

	public StrongholdUnlockData() {
		this(false, 0, 0);
	}

	public StrongholdUnlockData(boolean unlocked, int targetChunkX, int targetChunkZ) {
		this.unlocked = unlocked;
		this.targetChunkX = targetChunkX;
		this.targetChunkZ = targetChunkZ;
	}

	public boolean isUnlocked() {
		return this.unlocked;
	}

	public int targetChunkX() {
		return this.targetChunkX;
	}

	public int targetChunkZ() {
		return this.targetChunkZ;
	}

	public void markUnlocked(int targetChunkX, int targetChunkZ) {
		if (!this.unlocked) {
			this.unlocked = true;
			this.targetChunkX = targetChunkX;
			this.targetChunkZ = targetChunkZ;
			this.setDirty();
		}
	}
}
