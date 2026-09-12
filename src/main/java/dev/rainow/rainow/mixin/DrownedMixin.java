package dev.rainow.rainow.mixin;

import dev.rainow.rainow.RainbowUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Drowned.class})
public abstract class DrownedMixin {
   @Inject(
      method = {"wantsToSwim"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$rainWantsToSwim(CallbackInfoReturnable<Boolean> cir) {
      Drowned self = (Drowned)(Object)this;
      if (RainbowUtil.isRainFlightCandidate(self) && RainbowUtil.isRainSoaked(self)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"okTarget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$rainHunter(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
      Drowned self = (Drowned)(Object)this;
      if (target != null && RainbowUtil.isRainSoaked(self)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"updateSwimming"},
      at = {@At("TAIL")}
   )
   private void rainbow$rainSwimFlag(CallbackInfo ci) {
      Drowned self = (Drowned)(Object)this;
      if (!self.level().isClientSide()
         && !self.isPassenger()
         && RainbowUtil.shouldRainSwim(self)
         && !self.onGround()
         && !RainbowUtil.isInFluidWater(self)
         && !self.isSwimming()) {
         self.setSwimming(true);
      }
   }
}
