package com.daniotrix.onlyend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.daniotrix.onlyend.EndSpawnManager;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;

/**
 * Pone al jugador en el End antes de que {@code placeNewPlayer} arme siquiera
 * la conexión de juego — así el cliente nunca recibe ni un chunk del
 * Overworld, y no quedan restos visuales de estar ahí ni un instante.
 */
@Mixin(PlayerList.class)
public class RedirectInitialSpawnMixin {
	@Inject(method = "placeNewPlayer", at = @At("HEAD"))
	private void onlyend$forceEndBeforePlacement(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		ServerLevel endLevel = player.level().getServer().getLevel(Level.END);

		if (endLevel != null && player.level().dimension() != Level.END) {
			EndSpawnManager.placeDirectlyInEnd(player, endLevel);
		}
	}
}
