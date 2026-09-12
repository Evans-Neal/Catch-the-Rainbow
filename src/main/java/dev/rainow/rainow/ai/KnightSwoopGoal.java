package dev.rainow.rainow.ai;

import dev.rainow.rainow.RainbowUtil;
import java.util.EnumSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Aerial knight combat. One invariant rules the movement: THE BODY ALWAYS
 * LEADS THE VELOCITY (a dolphin visually swims along its facing -- any gap
 * between yRot and deltaMovement reads as strafing/backpedaling, which
 * dolphins cannot do). Phase layout:
 *
 * - CIRCLE: orbit waypoints derived from "the ring point ahead of the current
 *   look vector" (mirrored to the far side after a failed route); an orbit
 *   governor clamps horizontal/vertical speed against the rain-physics
 *   gravity compounding artifact.
 * - DIVE: pathfinding steers the ROUTE (over fences, through 1-block gaps),
 *   body rotated toward the current path waypoint at a capped rate and
 *   velocity written ALONG the view vector; an acceleration cap smooths the
 *   orbit->dive transition. Inside 8 blocks with clear line of sight:
 *   committed direct homing with an overshoot guard. Path done with clear
 *   line of sight = chase (a fleeing target outlives a stale path); no
 *   route at all with no line of sight = break off, do not grind walls.
 * - CLIMB: forward + up along the look vector (breach past the target); on
 *   collision, pure vertical escape -- up and over the obstacle.
 *
 * Damage: the rider raises its spear during the dive (windup at 12 blocks,
 * sized against the weapon's delayTicks so the vanilla ray sweep connects
 * mid-flight); contact is resolved through the REAL vanilla stab path with
 * the KineticWeapon charge math at KNIGHT_CHARGE_SCALE. Mount-contact is
 * the reliable trigger (fallbackHit); the vanilla eye-ray itself is
 * silenced for mounted knights (KineticWeaponSilenceMixin) because a
 * remember-before-test graze poisons the 10-tick stab cooldown.
 */
public class KnightSwoopGoal extends Goal {
   /** Knight-specific scale on the spear charge bonus: the rain-boosted dive
    * reaches ~19 m/s where a player stab tops out around 10, so the raw
    * vanilla formula (+20) overshoots. 0.5 keeps the charge feel at roughly
    * half the speed bonus. Tunable. */
   private static final float KNIGHT_CHARGE_SCALE = 0.5F;
   private final Drowned rider;
   private KnightSwoopGoal.Phase phase = KnightSwoopGoal.Phase.CIRCLE;
   private int cooldown;
   private int diveTicks;
   private int climbTicks;
   private boolean homingEngaged;
   private double lastDistSqr;
   private boolean orbitFlip;
   private boolean stabArmed;
   private double lastDiveSpeed;

   public KnightSwoopGoal(Drowned rider) {
      this.rider = rider;
      this.cooldown = 20 + rider.getRandom().nextInt(30);
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   private Dolphin mount() {
      return this.rider.getRootVehicle() instanceof Dolphin dolphin ? dolphin : null;
   }

   public boolean canUse() {
      return this.rider.isPassenger() && this.mount() != null && this.rider.getTarget() != null && RainbowUtil.isRainSoaked(this.rider);
   }

   public boolean canContinueToUse() {
      return this.canUse();
   }

   public void start() {
      this.phase = KnightSwoopGoal.Phase.CIRCLE;
   }

   public void stop() {
      this.phase = KnightSwoopGoal.Phase.CIRCLE;
      this.rider.getNavigation().stop();
      if (this.rider.isUsingItem()) {
         this.rider.stopUsingItem();
      }
   }

   public void tick() {
      Dolphin mount = this.mount();
      LivingEntity target = this.rider.getTarget();
      if (mount != null && target != null) {
         this.rider.lookAt(target, 30.0F, 30.0F);
         switch (this.phase) {
            case CIRCLE:
               this.tickCircle(mount, target);
               break;
            case DIVE:
               this.tickDive(mount, target);
               break;
            case CLIMB:
               this.tickClimb(mount, target);
         }
      }
   }

   private void tickCircle(Dolphin mount, LivingEntity target) {
      if (this.cooldown > 0) {
         this.cooldown--;
      }

      Vec3 center = target.position();
      Vec3 ahead = mount.position().add(mount.getViewVector(1.0F).scale(8.0));
      double angle = Math.atan2(ahead.z - center.z, ahead.x - center.x);
      if (this.orbitFlip) {
         angle += Math.PI;
      }

      double wx = center.x + Math.cos(angle) * 6.0;
      double wz = center.z + Math.sin(angle) * 6.0;
      double wy = center.y + 3.5;
      boolean arrived = Vec3.atLowerCornerOf(mount.blockPosition()).add(0.0, 0.5, 0.0).distanceToSqr(wx, wy, wz) < 4.0;
      if (arrived || this.rider.getNavigation().isDone()) {
         if (this.rider.getNavigation().moveTo(wx, wy, wz, 1.4)) {
            this.orbitFlip = false;
         } else {
            this.orbitFlip = !this.orbitFlip;
            Vec3 mv = mount.getDeltaMovement();
            mount.setDeltaMovement(mv.scale(0.8));
         }
      }

      // 0.2.4.6 orbit governor: the dolphin's travelInWater override skips
      // the vanilla water buoyancy (getFluidFallingAdjustedMovement), so full
      // gravity compounds on the ring's descending segments -- the observed
      // "circle phase sudden speed surge". Clamp the orbit speed; the dive
      // has its own ramp and is unaffected.
      Vec3 gvMv = mount.getDeltaMovement();
      double gvH = gvMv.horizontalDistance();
      if (gvH > 0.5) {
         double k = 0.5 / gvH;
         gvMv = new Vec3(gvMv.x * k, gvMv.y, gvMv.z * k);
      }
      double gvVy = Mth.clamp(gvMv.y, -0.35, 0.35);
      mount.setDeltaMovement(new Vec3(gvMv.x, gvVy, gvMv.z));

      if (this.cooldown <= 0) {
         this.lastDiveSpeed = mount.getDeltaMovement().horizontalDistance();
         this.phase = KnightSwoopGoal.Phase.DIVE;
         this.diveTicks = 50;
         this.homingEngaged = false;
         this.lastDistSqr = Double.MAX_VALUE;
         this.stabArmed = false;
      }
   }

   private void tickDive(Dolphin mount, LivingEntity target) {
      this.diveTicks--;
      Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
      Vec3 to = aim.subtract(mount.position());
      double dist = to.length();
      if (dist <= 12.0 && !this.stabArmed) {
         if (!this.rider.isUsingItem()) {
            this.rider.startUsingItem(InteractionHand.MAIN_HAND);
         }

         this.stabArmed = true;
      }

      if (this.stabArmed && !this.rider.isUsingItem()) {
         this.beginClimb();
      } else {
         boolean losClose = dist < 8.0 && mount.hasLineOfSight(target);
         Vec3 steer;
         if (losClose) {
            if (!this.homingEngaged) {
               this.homingEngaged = true;
               this.lastDistSqr = to.lengthSqr();
               this.rider.getNavigation().stop();
            }

            double distSqr = to.lengthSqr();
            if (distSqr > this.lastDistSqr + 0.02) {
               this.beginClimb();
               return;
            }

            this.lastDistSqr = distSqr;
            steer = aim;
         } else {
            this.homingEngaged = false;
            boolean repath = this.rider.getNavigation().getPath() == null || this.diveTicks % 10 == 0;
            if (repath) {
               this.rider.getNavigation().moveTo(aim.x, aim.y, aim.z, 1.4);
            }

            Path path = this.rider.getNavigation().getPath();
            if (path == null || this.rider.getNavigation().isDone()) {
               this.rider.stopUsingItem();
               this.beginClimb();
               return;
            }

            steer = path.getNextEntityPos(mount);
         }

         Vec3 s = steer.subtract(mount.position());
         double sh = Math.sqrt(s.x * s.x + s.z * s.z);
         float yawD = (float)(Mth.atan2(s.z, s.x) * 180.0 / (float) Math.PI) - 90.0F;
         float pitchD = Mth.clamp((float)(-(Mth.atan2(s.y, sh) * 180.0 / (float) Math.PI)), -60.0F, 60.0F);
         mount.setYRot(Mth.approachDegrees(mount.getYRot(), yawD, 18.0F));
         mount.setXRot(Mth.clamp(Mth.approachDegrees(mount.getXRot(), pitchD, 14.0F), -60.0F, 60.0F));
         mount.yBodyRot = mount.getYRot();
         mount.yHeadRot = mount.getYRot();
         double ramp = Mth.clamp(1.0 - dist / 36.0, 0.0, 1.0);
         double v = Mth.lerp(ramp, 0.45, 0.95);
         // 0.2.4.6: acceleration cap -- the raw distance ramp jumped straight
         // from orbit cruising (~0.26) to near-max the tick the dive began
         // when the target was close (the "盘旋阶段速度暴涨" artifact)
         v = Mth.clamp(v, this.lastDiveSpeed - 0.08, this.lastDiveSpeed + 0.045);
         this.lastDiveSpeed = v;
         mount.setDeltaMovement(mount.getViewVector(1.0F).scale(v));
         if (mount.getBoundingBox().inflate(0.5).intersects(target.getBoundingBox())) {
            if (this.rider.isUsingItem()) {
               this.rider.releaseUsingItem();
            }

            this.fallbackHit(target);
            this.beginClimb();
         } else if (this.diveTicks <= 0 || mount.hurtTime > 0) {
            if (this.rider.isUsingItem()) {
               this.rider.stopUsingItem();
            }

            this.beginClimb();
         } else if (mount.horizontalCollision && this.diveTicks % 5 == 0) {
            this.rider.stopUsingItem();
            this.beginClimb();
         }
      }
   }

   private void tickClimb(Dolphin mount, LivingEntity target) {
      this.climbTicks--;
      Vec3 velocity;
      if (mount.horizontalCollision) {
         velocity = new Vec3(0.0, 0.35, 0.0);
      } else {
         Vec3 forward = mount.getViewVector(1.0F);
         if (forward.y < 0.0) {
            forward = new Vec3(forward.x, 0.0, forward.z).normalize();
         }

         velocity = forward.scale(0.3).add(0.0, 0.32, 0.0);
      }

      mount.setDeltaMovement(velocity);
      if (this.climbTicks <= 0) {
         this.phase = KnightSwoopGoal.Phase.CIRCLE;
         this.cooldown = 20 + this.rider.getRandom().nextInt(30);
      }
   }

   private void beginClimb() {
      this.phase = KnightSwoopGoal.Phase.CLIMB;
      this.climbTicks = 8;
   }

   private void fallbackHit(LivingEntity target) {
      if (!(this.rider.level() instanceof ServerLevel level)) {
         return;
      } else {
         Dolphin mount = this.mount();
         if (mount == null) {
            return;
         } else {
            // 0.2.4.2: the vanilla KineticWeapon ray (rider eye, mob reach
            // 4.5*0.5=2.25 blocks, 1-block min gap) geometrically cannot hit
            // a target from a diving mount's back -- so the charge damage
            // almost never procs and the knight fell back to white-board
            // mobAttack. Keep the reliable mount-contact gate, but resolve it
            // through the REAL vanilla stab path (spear damage type,
            // enchantments, knockback) with the KineticWeapon charge math:
            // base attack + floor(relative speed along look * multiplier),
            // gated on the same damage-condition thresholds the ray used.
            ItemStack spear = this.rider.getMainHandItem();
            KineticWeapon kinetic = (KineticWeapon)spear.get(DataComponents.KINETIC_WEAPON);
            float multiplier = kinetic != null ? kinetic.damageMultiplier() : 1.0F;
            Vec3 look = this.rider.getLookAngle();
            double attackerSpeed = look.dot(KineticWeapon.getMotion(mount));
            double relativeSpeed = Math.max(0.0, attackerSpeed - look.dot(KineticWeapon.getMotion(target)));
            boolean charged = kinetic != null
               && kinetic.damageConditions().isPresent()
               && ((KineticWeapon.Condition)kinetic.damageConditions().get()).test(0, attackerSpeed, relativeSpeed, 0.2F);
            float base = (float)this.rider.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float damage = base + (charged ? (float)Mth.floor((float)(relativeSpeed * multiplier * KNIGHT_CHARGE_SCALE)) : 0.0F);
            if (!this.rider.wasRecentlyStabbed(target, 10)) {
               this.rider.rememberStabbedEntity(target);
               this.rider.stabAttack(EquipmentSlot.MAINHAND, target, damage, true, true, false);
            }
         }
      }
   }

   private static enum Phase {
      CIRCLE,
      DIVE,
      CLIMB;
   }
}
