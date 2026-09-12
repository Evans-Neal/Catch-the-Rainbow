package dev.rainow.rainow.mixin;

import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({SpearUseGoal.class})
public abstract class SpearGoalTuningMixin {
   @ModifyConstant(
      method = {"tick"},
      constant = {@Constant(
         intValue = 9
      )}
   )
   private int rainbow$cooldownNear(int original) {
      return 4;
   }

   @ModifyConstant(
      method = {"tick"},
      constant = {@Constant(
         intValue = 11
      )}
   )
   private int rainbow$cooldownFar(int original) {
      return 5;
   }

   @ModifyConstant(
      method = {"<clinit>"},
      constant = {@Constant(
         intValue = 100
      )}
   )
   private static int rainbow$shorterCoast(int original) {
      return 60;
   }
}
