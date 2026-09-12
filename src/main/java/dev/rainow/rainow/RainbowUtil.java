package dev.rainow.rainow;

import dev.rainow.rainow.ai.KnightSwoopGoal;
import dev.rainow.rainow.ai.RainSurfacingGoal;
import dev.rainow.rainow.mixin.MobAccessor;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public final class RainbowUtil {
   public static final int MAX_ALTITUDE_ABOVE_GROUND = 10;
   public static final TagKey<EntityType<?>> RAIN_SWIMMERS = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("rainow", "rain_swimmers"));
   public static final TagKey<EntityType<?>> RAIN_WALKERS = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("rainow", "rain_walkers"));
   private static final WeakHashMap<Mob, Boolean> CRUISE_ATTACHED = new WeakHashMap<>();
   private static final Identifier RAIN_SPEED_ID = Identifier.fromNamespaceAndPath("rainow", "rain_speed");
   private static final double RAIN_SPEED_BONUS = 0.5;
   private static final double RAIN_WATER_EFFICIENCY_BONUS = 1.0;
   private static final float KNIGHT_BASE_DAMAGE = 3.0F;
   private static final float KNIGHT_MOUNTED_DAMAGE = 7.0F;

   private RainbowUtil() {
   }

   public static boolean isRainSoaked(Entity entity) {
      Level level = entity.level();
      return level.isRainingAt(entity.blockPosition()) || level.isRainingAt(BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ()));
   }

   public static boolean isRainFlightCandidate(Entity entity) {
      return entity instanceof AbstractFish || BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(RAIN_SWIMMERS);
   }

   public static boolean isGroundDweller(Entity entity) {
      return BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(RAIN_WALKERS);
   }

   public static boolean shouldRainSwim(Entity entity) {
      return isRainFlightCandidate(entity) && isRainSoaked(entity) && (!isGroundDweller(entity) || !entity.onGround() || isInFluidWater(entity));
   }

   public static void updateRainPose(Mob mob) {
      if (mob instanceof Drowned drowned) {
         Pose pose = drowned.getPose();
         boolean swimming = shouldRainSwim(drowned) && !drowned.onGround() && !isInFluidWater(drowned);
         if (swimming && pose == Pose.STANDING) {
            drowned.setPose(Pose.SWIMMING);
         } else if (!swimming && pose == Pose.SWIMMING && !drowned.isSwimming()) {
            drowned.setPose(Pose.STANDING);
         }
      }
   }

   public static void applyRainSpeedBoost(Mob mob) {
      if (!mob.level().isClientSide()) {
         Drowned rider = null;
         if (mob instanceof Drowned drowned) {
            rider = drowned;
         } else if (mob.isVehicle()) {
            for (Entity passenger : mob.getPassengers()) {
               if (passenger instanceof Drowned drowned) {
                  rider = drowned;
                  break;
               }
            }
         }

         if (rider != null) {
            boolean active = rider.level().isRaining() && isRainSoaked(rider) && !rider.onGround() && !isInFluidWater(rider);
            applyIf(active, mob, Attributes.MOVEMENT_SPEED, RAIN_SPEED_ID, 0.5, Operation.ADD_MULTIPLIED_TOTAL);
            applyIf(active, mob, Attributes.WATER_MOVEMENT_EFFICIENCY, RAIN_SPEED_ID, 1.0, Operation.ADD_VALUE);
            boolean mountedKnight = rider.isPassenger() && rider.getRootVehicle() instanceof Dolphin;
            // 0.2.4.7: the +4 mounted bonus is RETIRED. It compensated for
            // the old "ten stabs nine whiffs" contact rate; since 0.2.4.2 the
            // contact stab is deterministic and charged (24/24, dmg 13-19 in
            // the 0.2.4.6 log) -- keeping the bonus double-compensated and
            // inverted the two-tier design (mounted out-damaging
            // dismounted). Both states now write the stock 3; the toggle
            // stays as a dormant tuning hook.
            AttributeInstance attackDamage = rider.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
               float wanted = mountedKnight ? 3.0F : 3.0F;
               if (attackDamage.getBaseValue() != wanted) {
                  attackDamage.setBaseValue(wanted);
               }
            }
         }
      }
   }

   private static void applyIf(boolean active, Mob mob, Holder<Attribute> attribute, Identifier id, double amount, Operation operation) {
      AttributeInstance attr = mob.getAttribute(attribute);
      if (attr != null) {
         boolean has = attr.getModifier(id) != null;
         if (active && !has) {
            attr.addTransientModifier(new AttributeModifier(id, amount, operation));
         } else if (!active && has) {
            attr.removeModifier(id);
         }
      }
   }

   public static boolean isInFluidWater(Entity entity) {
      return entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER);
   }

   public static void attachCruiseGoalIfNeeded(Mob mob) {
      if (!mob.level().isClientSide() && CRUISE_ATTACHED.put(mob, Boolean.TRUE) == null) {
         if (mob instanceof Drowned drowned) {
            ((MobAccessor)mob).rainbow$getGoalSelector().addGoal(3, new RainSurfacingGoal(mob));
            ((MobAccessor)drowned).rainbow$getGoalSelector().addGoal(1, new KnightSwoopGoal(drowned));
            ((MobAccessor)drowned).rainbow$getGoalSelector().addGoal(2, new SpearUseGoal(drowned, 1.0, 1.0, 5.0F, 3.0F));
         } else if (!(mob instanceof AbstractFish) && !isGroundDweller(mob)) {
            if (mob instanceof AbstractNautilus) {
               ((MobAccessor)mob).rainbow$getGoalSelector().addGoal(7, new RainSurfacingGoal(mob));
            } else {
               ((MobAccessor)mob).rainbow$getGoalSelector().addGoal(2, new RainSurfacingGoal(mob));
            }
         }
      }
   }

   public static int flightAnchor(LevelReader level, int x, int z) {
      int terrain = level.getHeight(Types.MOTION_BLOCKING, x, z);
      int y = terrain + 1;
      int maxY = Math.min(level.getMaxY(), y + 96);

      boolean sawWater;
      for (sawWater = false; y < maxY && level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.WATER); y++) {
         sawWater = true;
      }

      return sawWater ? y : terrain;
   }

   public static double groundHeight(Entity entity) {
      return flightAnchor(entity.level(), Mth.floor(entity.getX()), Mth.floor(entity.getZ()));
   }


   public static boolean withinFlightCeilingFor(Entity entity, LevelReader level, int x, int y, int z) {
      return y <= ceilingFor(entity, level, x, z);
   }

   public static int ceilingFor(Entity entity, LevelReader level, int x, int z) {
      return !(entity instanceof Drowned) && !hasDrownedRider(entity) ? anchorFor(entity, level, x, z) + 10 : level.getMaxY() - 1;
   }

   public static boolean hasDrownedRider(Entity entity) {
      if (!entity.isVehicle()) {
         return false;
      } else {
         for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof Drowned) {
               return true;
            }
         }

         return false;
      }
   }

   public static int anchorFor(Entity entity, LevelReader level, int x, int z) {
      return entity instanceof AbstractNautilus ? level.getHeight(Types.MOTION_BLOCKING, x, z) : flightAnchor(level, x, z);
   }

   public static boolean isAboveFlightCeiling(Entity entity) {
      return entity.getY() > ceilingFor(entity, entity.level(), Mth.floor(entity.getX()), Mth.floor(entity.getZ()));
   }

   public static void clampToCeiling(Mob mob) {
      if (isAboveFlightCeiling(mob)) {
         Vec3 motion = mob.getDeltaMovement();
         mob.setDeltaMovement(motion.x, Math.min(motion.y, -0.05), motion.z);
      }
   }
}
