package dev.rainow.rainow.mixin;

import dev.rainow.rainow.RainbowUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.SwimNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SwimNodeEvaluator.class})
public abstract class SwimNodeEvaluatorMixin {
   @Inject(
      method = {"getPathTypeOfMob"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$rainPathType(PathfindingContext context, int x, int y, int z, Mob pathMob, CallbackInfoReturnable<PathType> cir) {
      if (RainbowUtil.isRainFlightCandidate(pathMob)) {
         Level level = pathMob.level();
         BlockPos pos = new BlockPos(x, y, z);
         if (RainbowUtil.withinFlightCeilingFor(pathMob, level, x, y, z) && level.isRainingAt(pos)) {
            int width = Mth.floor(pathMob.getBbWidth() + 1.0F);
            int height = Mth.floor(pathMob.getBbHeight() + 1.0F);

            for (int xx = x; xx < x + width; xx++) {
               for (int yy = y; yy < y + height; yy++) {
                  for (int zz = z; zz < z + width; zz++) {
                     BlockState state = context.getBlockState(new BlockPos(xx, yy, zz));
                     if (!state.isAir() && !state.getFluidState().is(FluidTags.WATER)) {
                        return;
                     }
                  }
               }
            }

            cir.setReturnValue(PathType.WATER);
         }
      }
   }
}
