package net.bandit.betterspawn.mixin;

import net.bandit.betterspawn.BetterSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionTransition.class)
public class DimensionTransitionSpawnMixin {

    // Respawning with no bed, or a missing one, gets its position from here.
    @Inject(method = "findAdjustedSharedSpawnPos", at = @At("HEAD"), cancellable = true)
    private static void betterspawn$safeRespawnPos(ServerLevel level, Entity entity, CallbackInfoReturnable<Vec3> cir) {
        if (entity instanceof ServerPlayer) {
            cir.setReturnValue(Vec3.atBottomCenterOf(BetterSpawnFinder.findSafeSpawnNear(level, level.getSharedSpawnPos())));
        }
    }
}
