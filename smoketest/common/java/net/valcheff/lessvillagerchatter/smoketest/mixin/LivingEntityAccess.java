package net.valcheff.lessvillagerchatter.smoketest.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccess {
	@Invoker("getSoundVolume")
	float lessvillagerchattertest$getSoundVolume();
}
