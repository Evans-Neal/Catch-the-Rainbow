package dev.rainow.rainow.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KineticWeapon.class})
public abstract class KineticWeaponSilenceMixin {
   @Inject(
      method = {"damageEntities(Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void rainbow$knightNoRay(ItemStack stack, int ticksRemaining, LivingEntity livingEntity, EquipmentSlot equipmentSlot, CallbackInfo ci) {
      // 0.2.4.2: the spear eye-ray (mob reach 4.5*0.5 = 2.25 blocks, fired
      // from an elevated seat at dive speed) geometrically cannot hit from a
      // diving mount's back -- and worse, damageEntities remembers the stab
      // BEFORE testing its conditions, so even a zero-damage graze poisons
      // the 10-tick stab cooldown and swallows the real contact hit.
      // Knight riders get one deterministic damage path: the contact stab in
      // KnightSwoopGoal. Vanilla spear wielding is untouched.
      if (livingEntity instanceof Drowned drowned && drowned.getRootVehicle() instanceof Dolphin) {
         ci.cancel();
      }
   }
}
