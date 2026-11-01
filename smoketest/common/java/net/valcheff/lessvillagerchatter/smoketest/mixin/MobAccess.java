package net.valcheff.lessvillagerchatter.smoketest.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mob.class)
public interface MobAccess {
	@Invoker("resetAmbientSoundTime")
	void lessvillagerchattertest$resetAmbientSoundTime();
}
