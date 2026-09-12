package dev.rainow.rainow.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Mob.class})
public interface MobAccessor {
   @Accessor("goalSelector")
   GoalSelector rainbow$getGoalSelector();

   @Invoker("populateDefaultEquipmentEnchantments")
   void rainbow$enchantSpawnEquipment(ServerLevelAccessor var1, RandomSource var2, DifficultyInstance var3);
}
