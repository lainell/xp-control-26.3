package dev.xpcontrol.mixin;

import dev.xpcontrol.XpConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * getExperienceReward(ServerLevel, Entity) is declared (final) in LivingEntity, not in Mob.
 * We hook its return value and only change it for real mobs (never for players).
 */
@Mixin(LivingEntity.class)
public abstract class MobXpMixin {

    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void xpcontrol$modifyXp(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof Mob mob)) {
            return;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        cir.setReturnValue(XpConfig.apply(id, cir.getReturnValue()));
    }
}
