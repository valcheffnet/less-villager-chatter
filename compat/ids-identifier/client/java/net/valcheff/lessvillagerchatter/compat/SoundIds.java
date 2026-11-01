package net.valcheff.lessvillagerchatter.compat;

import net.minecraft.client.resources.sounds.SoundInstance;

// From 1.21.11 on, sound ids are Identifiers.
public final class SoundIds {
	private SoundIds() {
	}

	public static String namespace(SoundInstance sound) {
		return sound.getIdentifier().getNamespace();
	}

	public static String path(SoundInstance sound) {
		return sound.getIdentifier().getPath();
	}
}
