package net.valcheff.lessvillagerchatter.smoketest.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SoundEngine.class)
public interface SoundEngineAccess {
	@Invoker("calculateVolume")
	float lessvillagerchattertest$calculateVolume(SoundInstance sound);
}
