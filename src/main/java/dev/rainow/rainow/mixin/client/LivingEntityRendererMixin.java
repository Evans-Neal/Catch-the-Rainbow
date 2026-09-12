package dev.rainow.rainow.mixin.client;

import dev.rainow.rainow.RainbowUtil;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.zombie.Drowned;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"},
      at = {@At("TAIL")}
   )
   private void rainbow$wetRenderState(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
      if (RainbowUtil.shouldRainSwim(entity)) {
         state.isInWater = true;
         if (entity instanceof Drowned) {
            state.pose = Pose.SWIMMING;
         }
      }
   }
}
