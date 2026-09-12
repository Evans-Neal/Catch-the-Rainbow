package dev.rainow.rainow.mixin;

import dev.rainow.rainow.RainbowUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Inject(
      method = {"isInWater"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$rainIsWater(CallbackInfoReturnable<Boolean> cir) {
      Entity self = (Entity)(Object)this;
      if (RainbowUtil.shouldRainSwim(self)) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void rainbow$attachCruiseAndClamp(CallbackInfo ci) {
      if ((Object)this instanceof Mob mob && !mob.level().isClientSide()) {
         RainbowUtil.attachCruiseGoalIfNeeded(mob);
         RainbowUtil.applyRainSpeedBoost(mob);
         RainbowUtil.updateRainPose(mob);
         if (!mob.onGround() && !RainbowUtil.isInFluidWater(mob) && RainbowUtil.isRainFlightCandidate(mob) && RainbowUtil.isRainSoaked(mob)) {
            RainbowUtil.clampToCeiling(mob);
         }
      }
   }
}
