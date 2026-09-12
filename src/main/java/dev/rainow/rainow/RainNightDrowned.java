package dev.rainow.rainow;

import dev.rainow.rainow.mixin.MobAccessor;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.gamerules.GameRules;

public final class RainNightDrowned {
   private static final Identifier MOUNT_HEALTH_ID = Identifier.fromNamespaceAndPath("rainow", "knight_mount_health");
   private static final Identifier FOLLOW_RANGE_ID = Identifier.fromNamespaceAndPath("rainow", "knight_follow_range");
   private static final Map<Difficulty, Integer> CAP = Map.of(Difficulty.EASY, 2, Difficulty.NORMAL, 4, Difficulty.HARD, 6);
   private static final WeakHashMap<Entity, Boolean> TRACKED = new WeakHashMap<>();

   private RainNightDrowned() {
   }

   public static void tick(ServerLevel level) {
      cleanup(level);
      if ((Boolean)level.getGameRules().get(GameRules.SPAWN_MOBS) && (Boolean)level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
         if (level.isRaining()) {
            boolean thunder = level.isThundering();
            if (!thunder && level.isBrightOutside()) {
               return;
            }

            if (level.getGameTime() % 80L == 0L) {
               Integer cap = CAP.get(level.getDifficulty());
               if (thunder) {
                  cap = cap * 2;
               }

               if (cap != null) {
                  RandomSource random = level.getRandom();

                  for (ServerPlayer player : level.players()) {
                     if (TRACKED.size() >= cap) {
                        return;
                     }

                     if (random.nextInt(thunder ? 3 : 6) == 0) {
                        spawnPack(level, player, random, thunder);
                     }
                  }
               }
            }
         }
      }
   }

   private static void spawnPack(ServerLevel level, ServerPlayer player, RandomSource random, boolean thunder) {
      double angle = random.nextDouble() * Math.PI * 2.0;
      double distance = 12.0 + random.nextDouble() * 12.0;
      int centerX = Mth.floor(player.getX() + Math.cos(angle) * distance);
      int centerZ = Mth.floor(player.getZ() + Math.sin(angle) * distance);
      int anchor = RainbowUtil.flightAnchor(level, centerX, centerZ);
      if (level.isRainingAt(new BlockPos(centerX, anchor + 2, centerZ))) {
         int pack = (thunder ? 2 : 1) + random.nextInt(thunder ? 2 : 2);
         boolean knightPending = random.nextInt(thunder ? 5 : 10) == 0;

         for (int i = 0; i < pack && TRACKED.size() < (int)(capOf(level) * (thunder ? 2.0 : 1.0)); i++) {
            int x = centerX + random.nextInt(5) - 2;
            int z = centerZ + random.nextInt(5) - 2;
            int y = RainbowUtil.flightAnchor(level, x, z) + 2 + random.nextInt(7);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isAir() && level.isRainingAt(pos) && level.getBrightness(LightLayer.BLOCK, pos) == 0) {
               if (knightPending) {
                  knightPending = false;
                  spawnDolphinKnight(level, pos, random);
               } else {
                  Drowned drowned = (Drowned)EntityTypes.DROWNED.create(level, EntitySpawnReason.NATURAL);
                  if (drowned != null) {
                     drowned.setPos(x + 0.5, y, z + 0.5);
                     drowned.setYRot(random.nextFloat() * 360.0F);
                     drowned.fallDistance = 0.0;
                     drowned.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
                     if (!drowned.getMainHandItem().is(Items.TRIDENT) && random.nextFloat() < 0.0933F) {
                        drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
                     }

                     ((MobAccessor)drowned).rainbow$enchantSpawnEquipment(level, random, level.getCurrentDifficultyAt(pos));
                     if (drowned.getMainHandItem().is(Items.TRIDENT)
                        && !drowned.isPassenger()
                        && !drowned.isBaby()
                        && random.nextFloat() < (thunder ? 0.75F : 0.5F)
                        && !level.getBiome(pos).is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)) {
                        ZombieNautilus mount = (ZombieNautilus)EntityTypes.ZOMBIE_NAUTILUS.create(level, EntitySpawnReason.JOCKEY);
                        if (mount != null) {
                           mount.snapTo(drowned.getX(), drowned.getY(), drowned.getZ(), drowned.getYRot(), 0.0F);
                           mount.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
                           drowned.startRiding(mount, false, false);
                           level.addFreshEntity(mount);
                        }
                     }

                     if (level.addFreshEntity(drowned)) {
                        TRACKED.put(drowned, Boolean.TRUE);
                     }
                  }
               }
            }
         }
      }
   }

   private static int capOf(ServerLevel level) {
      Integer cap = CAP.get(level.getDifficulty());
      return cap == null ? 0 : cap;
   }

   public static void spawnDolphinKnight(ServerLevel level, BlockPos pos, RandomSource random) {
      Dolphin dolphin = (Dolphin)EntityTypes.DOLPHIN.create(level, EntitySpawnReason.JOCKEY);
      Drowned drowned = (Drowned)EntityTypes.DROWNED.create(level, EntitySpawnReason.EVENT);
      if (dolphin != null && drowned != null) {
         dolphin.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
         dolphin.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.JOCKEY, null);
         dolphin.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(MOUNT_HEALTH_ID, 20.0, Operation.ADD_VALUE));
         dolphin.setHealth(dolphin.getMaxHealth());
         level.addFreshEntity(dolphin);
         drowned.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
         drowned.setYRot(random.nextFloat() * 360.0F);
         drowned.fallDistance = 0.0;
         drowned.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
         drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SPEAR));
         AttributeInstance followRange = drowned.getAttribute(Attributes.FOLLOW_RANGE);
         if (followRange != null) {
            followRange.addPermanentModifier(new AttributeModifier(FOLLOW_RANGE_ID, 1.0, Operation.ADD_MULTIPLIED_TOTAL));
         }

         ((MobAccessor)drowned).rainbow$enchantSpawnEquipment(level, random, level.getCurrentDifficultyAt(pos));
         drowned.startRiding(dolphin, false, false);
         if (level.addFreshEntity(drowned)) {
            TRACKED.put(drowned, Boolean.TRUE);
         }
      }
   }

   /** Nautilus trident knight (debug command): trident-wielding drowned on
    * a zombie nautilus -- mirrors the natural jockey spawn, tracked like a
    * knight so caps/cleanup treat it the same. */
   public static void spawnNautilusTridentKnight(ServerLevel level, BlockPos pos, RandomSource random) {
      ZombieNautilus mount = (ZombieNautilus)EntityTypes.ZOMBIE_NAUTILUS.create(level, EntitySpawnReason.JOCKEY);
      Drowned drowned = (Drowned)EntityTypes.DROWNED.create(level, EntitySpawnReason.EVENT);
      if (mount != null && drowned != null) {
         mount.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
         mount.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.JOCKEY, null);
         drowned.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
         drowned.setYRot(random.nextFloat() * 360.0F);
         drowned.fallDistance = 0.0;
         drowned.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
         drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
         ((MobAccessor)drowned).rainbow$enchantSpawnEquipment(level, random, level.getCurrentDifficultyAt(pos));
         drowned.startRiding(mount, false, false);
         level.addFreshEntity(mount);
         if (level.addFreshEntity(drowned)) {
            TRACKED.put(drowned, Boolean.TRUE);
         }
      }
   }

   private static void cleanup(ServerLevel level) {
      if (!TRACKED.isEmpty()) {
         TRACKED.keySet().removeIf(Entity::isRemoved);
      }
   }
}
