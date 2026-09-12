package dev.rainow.rainow.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rainow.rainow.RainbowUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class RainSwimAssistMixin {
   private static final int IDLE_HOP_CHANCE = 140;
   private static final int COMBAT_HOP_CHANCE = 90;
   private static final double LAUNCH_POWER = 0.6;
   private static final double HOVER_FLOOR = 3.0;
   private static final double IMPULSE_RADIUS = 64.0;
   private static final int POP_CHANCE = 300;
   private static final double POP_POWER = 1.6;
   private static final int BEACH_LAUNCH_CHANCE = 50;
   private static final double WATER_HOVER_DEPTH = 5.0;
   private static final double WATER_HOVER_GAIN = 0.12;
   private static final double WATER_HOVER_BLEND = 0.35;
   private static final double WATER_HOVER_MAX = 0.7;

   @Inject(
      method = {"travel(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")}
   )
   private void rainbow$rainAssist(Vec3 input, CallbackInfo ci) {
      LivingEntity self = (LivingEntity)(Object)this;
      if (!self.level().isClientSide() && self instanceof Mob mob) {
         if (self instanceof AbstractFish
            && RainbowUtil.isRainFlightCandidate(self)
            && !RainbowUtil.isGroundDweller(self)
            && !self.isVehicle()
            && self.level().isRaining()
            && self.level().getNearestPlayer(self, 64.0) != null) {
            Vec3 mv = self.getDeltaMovement();
            if (RainbowUtil.isInFluidWater(self)) {
               boolean headNearAir = self.level()
                  .getBlockState(new BlockPos(Mth.floor(self.getX()), Mth.floor(self.getY()) + 2, Mth.floor(self.getZ())))
                  .isAir();
               if (headNearAir && mv.y < 0.5 && self.getRandom().nextInt(300) == 0) {
                  Vec3 v = self.getDeltaMovement();
                  self.setDeltaMovement(v.x, Math.max(v.y, 1.6), v.z);
               }
            } else if (self.onGround() && RainbowUtil.isRainSoaked(self) && self.getRandom().nextInt(50) == 0) {
               Vec3 v = self.getDeltaMovement();
               self.setDeltaMovement(v.x, Math.max(v.y, 0.6), v.z);
            } else if (!self.onGround()) {
               int wd = waterDepthBelow(self, 8);
               if (wd > 0 && wd < 5.0) {
                  double targetVy = Mth.clamp((5.0 - wd) * 0.12, 0.0, 0.7);
                  Vec3 m = self.getDeltaMovement();
                  if (m.y < targetVy) {
                     self.setDeltaMovement(m.x, m.y * 0.65 + targetVy * 0.35, m.z);
                  }
               }
            }
         }

         if (RainbowUtil.isRainFlightCandidate(self) && RainbowUtil.isRainSoaked(self)) {
            if (RainbowUtil.isGroundDweller(self) && !self.onGround() && !RainbowUtil.isInFluidWater(self) && self.tickCount % 10 == 0) {
               MoveControl mc = mob.getMoveControl();
               if (mc.hasWanted() && !mob.getNavigation().isDone()) {
                  double dx = mc.getWantedX() - self.getX();
                  double dz = mc.getWantedZ() - self.getZ();
                  double dy = mc.getWantedY() - self.getY();
                  Vec3 mm = self.getDeltaMovement();
                  if (Math.hypot(dx, dz) < 1.0 && dy < 0.0 && Math.hypot(mm.x, mm.z) < 0.08) {
                     mob.getNavigation().stop();
                  }
               }
            }

            if (RainbowUtil.isGroundDweller(self) && self.onGround() && !RainbowUtil.isInFluidWater(self) && !self.isVehicle()) {
               boolean chasing = mob.getTarget() != null;
               int chance = chasing ? 90 : 140;
               if (self.getRandom().nextInt(chance) == 0) {
                  Vec3 v = self.getDeltaMovement();
                  self.setDeltaMovement(v.x, Math.max(v.y, 0.6), v.z);
               }
            }

            if (!self.onGround() && RainbowUtil.isGroundDweller(self) && mob.getTarget() == null) {
               Vec3 motion = self.getDeltaMovement();
               double h = self.getY() - RainbowUtil.groundHeight(self);
               if (h < 3.0) {
                  self.setDeltaMovement(motion.x, Math.max(motion.y, 0.08), motion.z);
               } else if (h > 6.0 && motion.y > 0.0) {
                  self.setDeltaMovement(motion.x, Math.min(motion.y, 0.0), motion.z);
               }
            }

            if (!self.onGround() && RainbowUtil.isGroundDweller(self) && !RainbowUtil.isInFluidWater(self) && !self.isVehicle()) {
               int wd = waterDepthBelow(self, 8);
               if (wd > 0 && wd < 5.0) {
                  double targetVy = Mth.clamp((5.0 - wd) * 0.12, 0.0, 0.7);
                  Vec3 m = self.getDeltaMovement();
                  if (m.y < targetVy) {
                     self.setDeltaMovement(m.x, m.y * 0.65 + targetVy * 0.35, m.z);
                  }
               }
            }

            RainbowUtil.clampToCeiling(mob);
         }
      }
   }

   @ModifyExpressionValue(
      method = {"travel(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;shouldTravelInFluid(Lnet/minecraft/world/level/material/FluidState;)Z"
      )}
   )
   private boolean rainbow$airIsWater(boolean wouldTravelInFluid) {
      if (wouldTravelInFluid) {
         return true;
      } else {
         LivingEntity self = (LivingEntity)(Object)this;
         return self.level().isClientSide()
            ? false
            : RainbowUtil.isRainFlightCandidate(self) && RainbowUtil.isRainSoaked(self) && !RainbowUtil.isGroundDweller(self);
      }
   }

   private static int waterDepthBelow(LivingEntity self, int max) {
      BlockPos feet = self.blockPosition();

      for (int d = 1; d <= max; d++) {
         if (self.level().getFluidState(feet.below(d)).is(FluidTags.WATER)) {
            return d;
         }
      }

      return 0;
   }
}
