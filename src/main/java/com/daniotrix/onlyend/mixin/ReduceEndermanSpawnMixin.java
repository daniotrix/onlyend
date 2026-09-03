package com.daniotrix.onlyend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;

/**
 * Recorta la aparición de Endermans en la fuente: esto se revisa justo antes
 * de que el juego lo agregue de verdad al mundo (todavía no lo vio ningún
 * cliente), así que rechazar el intento acá no se nota como que "aparece y
 * después desaparece" — directamente no llega a aparecer.
 */
@Mixin(NaturalSpawner.class)
public class ReduceEndermanSpawnMixin {
	private static final float ENDERMAN_SPAWN_CHANCE = 0.1f;

	@Inject(method = "isValidPositionForMob", at = @At("RETURN"), cancellable = true)
	private static void onlyend$reduceEndermanSpawns(ServerLevel level, Mob mob, double distanceSquared, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() || !(mob instanceof EnderMan) || level.dimension() != Level.END) {
			return;
		}

		if (level.getRandom().nextFloat() >= ENDERMAN_SPAWN_CHANCE) {
			cir.setReturnValue(false);
		}
	}
}
