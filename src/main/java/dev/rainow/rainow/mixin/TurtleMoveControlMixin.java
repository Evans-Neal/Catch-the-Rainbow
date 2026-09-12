package dev.rainow.rainow.mixin;

import dev.rainow.rainow.RainbowUtil;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   targets = {"net.minecraft.world.entity.animal.turtle.Turtle$TurtleMoveControl"}
)
public abstract class TurtleMoveControlMixin {
   @Inject(
      method = {"updateSpeed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$noHomePenaltyWhileCruising(CallbackInfo ci) {
      Mob mob = ((MoveControlAccessor)this).rainbow$getMob();
      if (RainbowUtil.isRainFlightCandidate(mob) && RainbowUtil.isRainSoaked(mob) && !mob.onGround() && !RainbowUtil.isInFluidWater(mob)) {
         mob.setDeltaMovement(mob.getDeltaMovement().add(0.0, 0.005, 0.0));
         ci.cancel();
      }
   }
}
