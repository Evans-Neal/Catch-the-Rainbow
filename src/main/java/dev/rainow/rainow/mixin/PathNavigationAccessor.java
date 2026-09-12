package dev.rainow.rainow.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PathNavigation.class})
public interface PathNavigationAccessor {
   @Accessor("mob")
   Mob rainbow$getMob();
}
