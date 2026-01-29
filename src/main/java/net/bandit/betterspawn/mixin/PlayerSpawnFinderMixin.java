package net.bandit.betterspawn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(PlayerSpawnFinder.class)
public class PlayerSpawnFinderMixin {

    @Inject(
            method = "findSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void forceExactSpawn(ServerLevel level, BlockPos pos,
                                        CallbackInfoReturnable<CompletableFuture<Vec3>> cir) {
        cir.setReturnValue(CompletableFuture.completedFuture(Vec3.atBottomCenterOf(pos)));
    }

    @Inject(
            method = "fixupSpawnHeight(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void disableHeightFixup(CollisionGetter level, BlockPos pos,
                                           CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(Vec3.atBottomCenterOf(pos));
    }
}
