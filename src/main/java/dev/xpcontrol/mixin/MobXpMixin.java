package dev.xpcontrol.mixin;

import dev.xpcontrol.XpConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobXpMixin {

    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void xpcontrol$modifyXp(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
        Mob self = (Mob) (Object) this;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).toString();
        cir.setReturnValue(XpConfig.apply(id, cir.getReturnValue()));
    }
}
