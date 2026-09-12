package dev.rainow.rainow.mixin;

import dev.rainow.rainow.RainNightDrowned;
import dev.rainow.rainow.RainSpawns;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerLevel.class})
public abstract class ServerLevelMixin {
   @Inject(
      method = {"tick(Ljava/util/function/BooleanSupplier;)V"},
      at = {@At("TAIL")}
   )
   private void rainbow$rainSpawns(CallbackInfo ci) {
      RainSpawns.tick((ServerLevel)(Object)this);
      RainNightDrowned.tick((ServerLevel)(Object)this);
   }
}
