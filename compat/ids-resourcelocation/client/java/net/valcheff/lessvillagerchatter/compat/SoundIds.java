package net.valcheff.lessvillagerchatter.compat;

import net.minecraft.client.resources.sounds.SoundInstance;

// Sound ids are ResourceLocations up to 1.21.10.
public final class SoundIds {
	private SoundIds() {
	}

	public static String namespace(SoundInstance sound) {
		return sound.getLocation().getNamespace();
	}

	public static String path(SoundInstance sound) {
		return sound.getLocation().getPath();
	}
}
