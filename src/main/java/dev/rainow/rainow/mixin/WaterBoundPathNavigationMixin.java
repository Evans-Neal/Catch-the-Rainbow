package dev.rainow.rainow.mixin;

import dev.rainow.rainow.RainbowUtil;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WaterBoundPathNavigation.class})
public abstract class WaterBoundPathNavigationMixin {
   @Inject(
      method = {"canUpdatePath"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$allowAirNavigation(CallbackInfoReturnable<Boolean> cir) {
      Mob mob = ((PathNavigationAccessor)this).rainbow$getMob();
      if (mob != null && RainbowUtil.isRainFlightCandidate(mob) && RainbowUtil.isRainSoaked(mob)) {
         cir.setReturnValue(true);
      }
   }
}
