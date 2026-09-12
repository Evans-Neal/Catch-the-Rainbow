package dev.rainow.rainow.mixin;

import net.minecraft.world.entity.animal.fish.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   targets = {"net.minecraft.world.entity.animal.fish.AbstractFish$FishSwimGoal"}
)
public interface FishSwimGoalAccessor {
   @Accessor("fish")
   AbstractFish rainbow$getFish();
}
