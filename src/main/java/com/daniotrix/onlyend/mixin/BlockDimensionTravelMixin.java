package com.daniotrix.onlyend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * Cancela cualquier viaje de un jugador a una dimensión que no sea el End
 * (portal del Nether, portal de salida del End hacia el Overworld, etc.).
 */
@Mixin(ServerPlayer.class)
public class BlockDimensionTravelMixin {
	@Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
	private void onlyend$blockLeavingEnd(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> cir) {
		if (transition.newLevel().dimension() != Level.END) {
			cir.setReturnValue((ServerPlayer) (Object) this);
		}
	}
}
