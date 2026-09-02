package com.daniotrix.onlyend.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Deja llamar al {@code setLevel} protegido de {@link Entity} desde fuera del paquete del juego. */
@Mixin(Entity.class)
public interface EntityLevelAccessor {
	@Invoker("setLevel")
	void onlyend$setLevel(Level level);
}
