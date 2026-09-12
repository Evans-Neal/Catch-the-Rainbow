package dev.rainow.rainow.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MoveControl.class})
public interface MoveControlAccessor {
   @Accessor("mob")
   Mob rainbow$getMob();
}
