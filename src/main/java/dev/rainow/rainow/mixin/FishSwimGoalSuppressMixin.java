package dev.rainow.rainow.mixin;

import dev.rainow.rainow.RainbowUtil;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"net.minecraft.world.entity.animal.fish.AbstractFish$FishSwimGoal"}
)
public abstract class FishSwimGoalSuppressMixin {
   @Inject(
      method = {"canUse"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$noWaterWanderWhileRainFlying(CallbackInfoReturnable<Boolean> cir) {
      AbstractFish fish = ((FishSwimGoalAccessor)this).rainbow$getFish();
      if (RainbowUtil.isRainFlightCandidate(fish) && RainbowUtil.isRainSoaked(fish) && !RainbowUtil.isInFluidWater(fish) && !RainbowUtil.isGroundDweller(fish)) {
         cir.setReturnValue(false);
      }
   }
}
