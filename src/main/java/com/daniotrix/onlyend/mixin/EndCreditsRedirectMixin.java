package com.daniotrix.onlyend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.daniotrix.onlyend.EndSpawnManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;

/**
 * EndPortalBlock.entityInside() llama a showEndCredits() cada vez que un jugador pisa un portal del
 * End estando ya en el End y todavía no "vio los créditos" — como acá los jugadores siempre están en
 * el End y nunca pasan por la entrada clásica, cualquier portal terminaría disparando el final del
 * juego. Hay dos portales distintos que llegan acá por el mismo método, sin ningún dato que los
 * distinga a simple vista: el del Stronghold (antes de pelear) y el de salida que aparece recién
 * cuando se mata al Dragón. Los separamos con EnderDragonFight.hasPreviouslyKilledDragon() en vez de
 * la distancia al origen (que fallaba: el portal de salida real terminaba clasificado como "lejos" y
 * mandaba al jugador de vuelta a la isla del Dragón en bucle en lugar de mostrar los créditos).
 *
 * Si el Dragón todavía no murió nunca, cualquier portal que dispare esto solo puede ser el del
 * Stronghold — no existe ningún portal de salida real todavía — así que cancelamos y mandamos a la
 * isla del Dragón. Si el Dragón ya murió al menos una vez, dejamos que el método original siga: eso
 * hace aparecer los créditos de verdad y, al terminar (PERFORM_RESPAWN), PlayerList.respawn() reaparece
 * al jugador en su punto de reaparición configurado — que EndSpawnManager ya deja fijado en el End en
 * cada entrada/respawn — así que nunca intenta ir al Overworld ni choca con BlockDimensionTravelMixin.
 */
@Mixin(ServerPlayer.class)
public class EndCreditsRedirectMixin {
	@Inject(method = "showEndCredits", at = @At("HEAD"), cancellable = true)
	private void onlyend$redirectStrongholdPortal(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		ServerLevel endLevel = player.level().getServer().getLevel(Level.END);
		EnderDragonFight fight = endLevel != null ? endLevel.getDragonFight() : null;
		boolean dragonEverKilled = fight != null && fight.hasPreviouslyKilledDragon();

		if (!dragonEverKilled) {
			EndSpawnManager.teleportToDragonIsland(player);
			ci.cancel();
		}
	}
}
