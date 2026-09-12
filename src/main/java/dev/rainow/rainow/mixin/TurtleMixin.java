package dev.rainow.rainow.mixin;

import dev.rainow.rainow.ai.RainSurfacingGoal;
import net.minecraft.world.entity.animal.turtle.Turtle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Turtle.class})
public abstract class TurtleMixin {
   @Inject(
      method = {"registerGoals"},
      at = {@At("TAIL")}
   )
   private void rainbow$addRainGoal(CallbackInfo ci) {
      Turtle self = (Turtle)(Object)this;
      ((MobAccessor)self).rainbow$getGoalSelector().addGoal(7, new RainSurfacingGoal(self));
   }
}
