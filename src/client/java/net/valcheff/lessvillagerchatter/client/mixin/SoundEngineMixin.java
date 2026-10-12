package net.valcheff.lessvillagerchatter.client.mixin;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.valcheff.lessvillagerchatter.ChatterConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// "Use my volume": play villager / trader sounds at this client's own levels, whatever volume the server sent.
// The sound's own sounds.json volume is kept; only the part that came from the server is replaced.
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
	@Shadow
	private float calculateVolume(float volume, SoundSource source) {
		throw new AssertionError();
	}

	@Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("HEAD"), cancellable = true)
	private void lessvillagerchatter$ownVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
		if (!ChatterConfig.useOwnVolume) return;
		Identifier id = sound.getIdentifier();
		if (!"minecraft".equals(id.getNamespace())) return;
		int percent;
		if (id.getPath().startsWith("entity.villager.")) percent = ChatterConfig.villagerVolume;
		else if (id.getPath().startsWith("entity.wandering_trader.")) percent = ChatterConfig.traderVolume;
		else return;

		float volume = sound.getVolume();
		if (sound instanceof SoundVolumeAccessor accessor && accessor.lessvillagerchatter$getVolume() > 0) {
			volume /= accessor.lessvillagerchatter$getVolume();
		}
		cir.setReturnValue(calculateVolume(volume * percent / 100f, sound.getSource()));
	}
}
