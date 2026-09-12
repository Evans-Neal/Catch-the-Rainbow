package dev.rainow.rainow.mixin;

import dev.rainow.rainow.ai.RainSurfacingGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractFish.class})
public abstract class AbstractFishMixin {
   @Inject(
      method = {"registerGoals"},
      at = {@At("TAIL")}
   )
   private void rainbow$addRainGoal(CallbackInfo ci) {
      AbstractFish fish = (AbstractFish)(Object)this;
      MobAccessor accessor = (MobAccessor)fish;
      accessor.rainbow$getGoalSelector().addGoal(7, new RainSurfacingGoal(fish));
   }
}
