package dev.rainow.rainow.mixin;

import dev.rainow.rainow.RainbowUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AmphibiousNodeEvaluator.class})
public abstract class AmphibiousNodeEvaluatorMixin {
   @Inject(
      method = {"getPathType"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$rainPathType(PathfindingContext context, int x, int y, int z, CallbackInfoReturnable<PathType> cir) {
      Mob mob = ((NodeEvaluatorAccessor)this).rainbow$getMob();
      if (mob != null && RainbowUtil.isRainFlightCandidate(mob) && (!RainbowUtil.isGroundDweller(mob) || !mob.onGround() || mob.isInWater())) {
         Level level = mob.level();
         BlockPos pos = new BlockPos(x, y, z);
         if (RainbowUtil.withinFlightCeilingFor(mob, level, x, y, z) && level.isRainingAt(pos) && context.getBlockState(pos).isAir()) {
            cir.setReturnValue(PathType.WATER);
         }
      }
   }
}
