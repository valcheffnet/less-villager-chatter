package net.valcheff.lessvillagerchatter.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.valcheff.lessvillagerchatter.ChatterConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Every villager / trader sound the server plays (idle, work, trade yes/no) uses getSoundVolume(), and the
// volume travels in the sound packet, so vanilla clients hear it quieter too. Server side only: sounds the
// client plays by itself (hurt) are handled by the client's own volume setting instead.
@Mixin(LivingEntity.class)
public class SoundVolumeMixin {
	@Inject(method = "getSoundVolume", at = @At("RETURN"), cancellable = true)
	private void lessvillagerchatter$quieter(CallbackInfoReturnable<Float> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		int percent;
		if (self instanceof Villager) percent = ChatterConfig.villagerVolume;
		else if (self instanceof WanderingTrader) percent = ChatterConfig.traderVolume;
		else return;
		if (percent != 100 && !self.level().isClientSide()) {
			cir.setReturnValue(cir.getReturnValueF() * percent / 100f);
		}
	}
}
