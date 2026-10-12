package net.valcheff.lessvillagerchatter.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.Level;
import net.valcheff.lessvillagerchatter.ChatterConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ThreadLocalRandom;

// Mob.baseTick() waits getAmbientSoundInterval() ticks (vanilla 80) after each idle sound before the
// random roll can fire again. Neither Villager nor WanderingTrader override it, so lengthen it here.
@Mixin(Mob.class)
public class AmbientIntervalMixin {
	@Inject(method = "getAmbientSoundInterval", at = @At("HEAD"), cancellable = true)
	private void lessvillagerchatter$longerInterval(CallbackInfoReturnable<Integer> cir) {
		int interval = lessvillagerchatter$intervalFor(this);
		if (interval > 0) {
			// With variation on, report the longest silence; resetAmbientSoundTime() then shortens it at random.
			// Trade code also uses this value, so its "yes/no" sound throttle stays at vanilla's 1 s.
			cir.setReturnValue(ChatterConfig.maxInterval(interval));
		}
	}

	// Vanilla sets ambientSoundTime = -getAmbientSoundInterval() after each idle sound. Take a random amount
	// back off, so the silence lands anywhere in interval * (1 +/- variation) with the same average.
	@Inject(method = "resetAmbientSoundTime", at = @At("TAIL"))
	private void lessvillagerchatter$varySilence(CallbackInfo ci) {
		int interval = lessvillagerchatter$intervalFor(this);
		int spread = interval * ChatterConfig.variation / 100;
		if (interval > 0 && spread > 0) {
			((Mob) (Object) this).ambientSoundTime += ThreadLocalRandom.current().nextInt(2 * spread + 1);
		}
	}

	// ambientSoundTime is not saved, so every villager in a freshly loaded or generated village starts at 0
	// in the same tick. The random part of the timer adds only ~2 s, which barely spreads them apart once the
	// silence is long, so they stay in step and "hmm" together. Optionally start each one at a random point of its cycle.
	@Inject(method = "<init>", at = @At("RETURN"))
	private void lessvillagerchatter$randomPhase(EntityType<? extends Mob> type, Level level, CallbackInfo ci) {
		int interval = lessvillagerchatter$intervalFor(this);
		if (interval > 0 && ChatterConfig.randomStart) {
			((Mob) (Object) this).ambientSoundTime = -ThreadLocalRandom.current().nextInt(interval);
		}
	}

	private static int lessvillagerchatter$intervalFor(Object mob) {
		if (mob instanceof Villager) return ChatterConfig.villagerInterval;
		if (mob instanceof WanderingTrader) return ChatterConfig.traderInterval;
		return 0;
	}
}
