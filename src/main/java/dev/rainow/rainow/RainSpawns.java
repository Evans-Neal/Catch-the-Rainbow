package dev.rainow.rainow;

import java.util.List;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public final class RainSpawns {
   /** Tier-2 thermometer thresholds (land biomes only -- see pickPool). */
   private static final float COLD_BELOW = 0.45F;
   private static final float WARM_ABOVE = 0.95F;
   private static final WeakHashMap<Entity, Boolean> RAIN_BORN = new WeakHashMap<>();
   private static final List<RainSpawns.WeightedFish> POOL_COLD = List.of(
      new RainSpawns.WeightedFish(EntityTypes.COD, 45), new RainSpawns.WeightedFish(EntityTypes.SALMON, 45)
   );
   private static final List<RainSpawns.WeightedFish> POOL_TEMPERATE = List.of(
      new RainSpawns.WeightedFish(EntityTypes.COD, 30),
      new RainSpawns.WeightedFish(EntityTypes.SALMON, 30),
      new RainSpawns.WeightedFish(EntityTypes.TROPICAL_FISH, 12),
      new RainSpawns.WeightedFish(EntityTypes.PUFFERFISH, 8),
      new RainSpawns.WeightedFish(EntityTypes.DOLPHIN, 4),
      new RainSpawns.WeightedFish(EntityTypes.NAUTILUS, 3),
      new RainSpawns.WeightedFish(EntityTypes.TADPOLE, 3)
   );
   private static final List<RainSpawns.WeightedFish> POOL_WARM = List.of(
      new RainSpawns.WeightedFish(EntityTypes.TROPICAL_FISH, 32),
      new RainSpawns.WeightedFish(EntityTypes.PUFFERFISH, 25),
      new RainSpawns.WeightedFish(EntityTypes.DOLPHIN, 8),
      new RainSpawns.WeightedFish(EntityTypes.NAUTILUS, 4),
      new RainSpawns.WeightedFish(EntityTypes.COD, 5),
      new RainSpawns.WeightedFish(EntityTypes.SALMON, 5),
      new RainSpawns.WeightedFish(EntityTypes.TADPOLE, 4)
   );

   private RainSpawns() {
   }

   // 0.2.4.6: a three-tier thermometer, zero hardcoded biome tables.
   // Tier 1 -- the biome's own WATER_AMBIENT spawner list: vanilla's own
   // cold/warm judgment (tropical fish/pufferfish only in warm+lukewarm
   // waters; salmon/cold rivers; mangrove swamps list tropical fish). This
   // tier sees EVERY ocean+river, so it covers exactly the biomes whose
   // temperature field is dead (all oceans frozen to 0.5 in 26.2).
   // Tier 2 -- the temperature field itself: still honest on LAND biomes
   // (taiga 0.25, windswept hills 0.2, jungle 0.95), and land is the only
   // place this tier ever runs because oceans never fall through tier 1.
   // Tier 3 -- biome-id substring match, kept as the last resort for
   // modded edge cases with empty lists and mid-range temperatures.
   // The lists/pools are kept SEPARATE concerns: the classifier only picks
   // WHICH pool rains; custom pool members (dolphin, nautilus, tadpole)
   // ride their pool's whole classification, never filtered.
   private static List<RainSpawns.WeightedFish> pickPool(ServerLevel level, BlockPos pos) {
      boolean warmMarker = false;
      boolean coldMarker = false;

      for (Weighted<MobSpawnSettings.SpawnerData> entry : level.getBiome(pos).value().getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap()) {
         EntityType<?> type = entry.value().type();
         if (type == EntityTypes.TROPICAL_FISH || type == EntityTypes.PUFFERFISH) {
            warmMarker = true;
         } else if (type == EntityTypes.COD || type == EntityTypes.SALMON) {
            coldMarker = true;
         }
      }

      if (warmMarker) {
         return POOL_WARM;
      } else if (coldMarker) {
         return POOL_COLD;
      } else {
         float temperature = level.getBiome(pos).value().getBaseTemperature();
         if (temperature < COLD_BELOW) {
            return POOL_COLD;
         } else if (temperature >= WARM_ABOVE) {
            return POOL_WARM;
         } else {
            String biomeId = level.getBiome(pos).unwrapKey().map(key -> key.identifier().getPath()).orElse("");
            if (biomeId.contains("cold") || biomeId.contains("frozen")) {
               return POOL_COLD;
            } else if (biomeId.contains("warm")) {
               return POOL_WARM;
            } else {
               return POOL_TEMPERATE;
            }
         }
      }
   }

   private static EntityType<?> rollFish(List<RainSpawns.WeightedFish> pool, RandomSource random) {
      int total = 0;

      for (RainSpawns.WeightedFish entry : pool) {
         total += entry.weight();
      }

      int roll = random.nextInt(total);

      for (RainSpawns.WeightedFish entry : pool) {
         roll -= entry.weight();
         if (roll < 0) {
            return entry.type();
         }
      }

      return EntityTypes.COD;
   }

   public static boolean isRainBorn(Entity entity) {
      return RAIN_BORN.containsKey(entity);
   }

   public static void tick(ServerLevel level) {
      cleanup(level);
      if ((Boolean)level.getGameRules().get(GameRules.SPAWN_MOBS)) {
         if (level.getGameTime() % 40L == 0L) {
            RandomSource random = level.getRandom();

            for (ServerPlayer player : level.players()) {
               if (RAIN_BORN.size() >= 40) {
                  return;
               }

               if (random.nextInt(4) == 0) {
                  spawnPack(level, player, random);
               }
            }
         }
      }
   }

   private static void spawnPack(ServerLevel level, ServerPlayer player, RandomSource random) {
      double angle = random.nextDouble() * Math.PI * 2.0;
      double distance = 12.0 + random.nextDouble() * 12.0;
      Vec3 offset = new Vec3(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
      int centerX = Mth.floor(player.getX() + offset.x);
      int centerZ = Mth.floor(player.getZ() + offset.z);
      int anchor = RainbowUtil.flightAnchor(level, centerX, centerZ);
      if (level.isRainingAt(new BlockPos(centerX, anchor + 2, centerZ))) {
         int pack = 2 + random.nextInt(3);

         for (int i = 0; i < pack; i++) {
            int x = centerX + random.nextInt(5) - 2;
            int z = centerZ + random.nextInt(5) - 2;
            int y = RainbowUtil.flightAnchor(level, x, z) + 2 + random.nextInt(7);
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isAir() && level.isRainingAt(pos)) {
               Entity fish = rollFish(pickPool(level, pos), random).create(level, EntitySpawnReason.EVENT);
               if (fish != null) {
                  fish.setPos(x + 0.5, y, z + 0.5);
                  fish.setYRot(random.nextFloat() * 360.0F);
                  fish.fallDistance = 0.0;
                  if (level.addFreshEntity(fish)) {
                     RAIN_BORN.put(fish, Boolean.TRUE);
                  }
               }
            }
         }
      }
   }

   private static void cleanup(ServerLevel level) {
      if (!RAIN_BORN.isEmpty()) {
         RAIN_BORN.keySet().removeIf(entity -> {
            if (entity.isRemoved()) {
               return true;
            } else {
               return entity.level() != level ? false : RainbowUtil.isInFluidWater(entity);
            }
         });
      }
   }

   private record WeightedFish(EntityType<?> type, int weight) {
   }
}
