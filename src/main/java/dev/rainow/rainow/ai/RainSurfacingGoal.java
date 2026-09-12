package dev.rainow.rainow.ai;

import dev.rainow.rainow.RainbowUtil;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public class RainSurfacingGoal extends Goal {
   private static final double DEFAULT_SPEED = 1.0;
   private static final double DEFAULT_RANGE = 12.0;
   private static final int BREACH_TOP = 6;
   private static final long MIN_SEARCH_INTERVAL = 20L;
   private static final long PATH_FAIL_COOLDOWN = 100L;
   private static final int MAX_PATH_SEARCHES = 3;
   private static final double PLAYER_ACTIVITY_RADIUS = 64.0;
   private final Mob mob;
   private final PathNavigation navigation;
   private final double speed;
   private final double range;
   private long nextSearchTime;

   public RainSurfacingGoal(Mob mob) {
      this(mob, 1.0, 12.0);
   }

   public RainSurfacingGoal(Mob mob, double speed, double range) {
      this.mob = mob;
      this.navigation = mob.getNavigation();
      this.speed = speed;
      this.range = range;
      this.nextSearchTime = mob.level().getGameTime() + mob.getRandom().nextInt(100);
      this.setFlags(EnumSet.of(Flag.MOVE));
   }

   private boolean searchGatePassed() {
      return this.mob.level().getGameTime() >= this.nextSearchTime;
   }

   public boolean canUse() {
      Mob mob = this.mob;
      if (!RainbowUtil.isRainFlightCandidate(mob) || !RainbowUtil.isRainSoaked(mob)) {
         return false;
      } else if (mob.isVehicle() || mob.getTarget() != null) {
         return false;
      } else if (!this.searchGatePassed()) {
         return false;
      } else {
         return !RainbowUtil.isInFluidWater(mob) && !mob.onGround() ? mob.level().getNearestPlayer(mob, 64.0) != null : false;
      }
   }

   public boolean canContinueToUse() {
      Mob mob = this.mob;
      if (!mob.isVehicle() && mob.getTarget() == null) {
         return !RainbowUtil.isRainSoaked(mob) ? false : !RainbowUtil.isInFluidWater(mob) && !mob.onGround();
      } else {
         return false;
      }
   }

   public void tick() {
      if (this.searchGatePassed() && this.navigation.isDone()) {
         Mob mob = this.mob;
         if (RainbowUtil.isRainSoaked(mob) && !mob.isVehicle() && mob.getTarget() == null && !RainbowUtil.isInFluidWater(mob) && !mob.onGround()) {
            this.findTarget();
         }
      }
   }

   public void stop() {
      this.navigation.stop();
   }

   private boolean findTarget() {
      RandomSource random = this.mob.getRandom();
      Level level = this.mob.level();
      boolean airborne = !RainbowUtil.isInFluidWater(this.mob) && !this.mob.onGround();
      int searches = 0;

      for (int attempt = 0; attempt < 16; attempt++) {
         double x;
         double z;
         if (airborne && attempt < 8) {
            double heading = Math.toRadians(this.mob.yBodyRot);
            double turn = (random.nextDouble() - 0.5) * Math.PI;
            double distance = this.range * (0.5 + random.nextDouble() * 0.5);
            x = this.mob.getX() - Math.sin(heading + turn) * distance;
            z = this.mob.getZ() + Math.cos(heading + turn) * distance;
         } else {
            x = this.mob.getX() + (random.nextDouble() * 2.0 - 1.0) * this.range;
            z = this.mob.getZ() + (random.nextDouble() * 2.0 - 1.0) * this.range;
         }

         int bx = Mth.floor(x);
         int bz = Mth.floor(z);
         int terrainY = level.getHeight(Types.MOTION_BLOCKING, bx, bz);
         int anchor = RainbowUtil.anchorFor(this.mob, level, bx, bz);
         int ceiling = RainbowUtil.ceilingFor(this.mob, level, bx, bz);
         int y;
         if (RainbowUtil.isInFluidWater(this.mob)) {
            int surfaceY = this.surfaceYAt(level, bx, bz, terrainY);
            y = surfaceY + 1 + random.nextInt(6);
         } else {
            int current = Mth.floor(this.mob.getY());
            y = Mth.clamp(current + random.nextInt(5) - 2, anchor + 2, ceiling);
         }

         BlockPos pos = new BlockPos(bx, y, bz);
         if (y <= ceiling && level.getBlockState(pos).isAir() && level.isRainingAt(pos)) {
            if (this.navigation.moveTo(x, y, z, this.speed)) {
               this.nextSearchTime = level.getGameTime() + 20L;
               return true;
            }

            if (++searches >= 3) {
               break;
            }
         }
      }

      this.nextSearchTime = level.getGameTime() + 100L;
      return false;
   }

   private int surfaceYAt(Level level, int bx, int bz, int terrainY) {
      for (int y = terrainY + 1; y < level.getMaxY(); y++) {
         if (!level.getBlockState(new BlockPos(bx, y, bz)).getFluidState().is(FluidTags.WATER)) {
            return y;
         }
      }

      return terrainY + 1;
   }
}
