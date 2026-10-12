package net.valcheff.lessvillagerchatter.client.mixin;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// The volume the sound was started with (for entity sounds: the volume the server sent).
@Mixin(AbstractSoundInstance.class)
public interface SoundVolumeAccessor {
	@Accessor("volume")
	float lessvillagerchatter$getVolume();
}
