package net.bandit.betterspawn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//1.20.1
@Mixin(PlayerList.class)
public class PlayerSpawnFinderMixin {

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void betterspawn$afterPlaceNewPlayer(CallbackInfo ci) {
    }
    @Inject(method = "respawn", at = @At("RETURN"))
    private void betterspawn$afterRespawn(CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = cir.getReturnValue();
        if (player != null) {
            betterspawn$forceExactWorldSpawnIfNoBed(player);
        }
    }

    @Unique
    private static void betterspawn$forceExactWorldSpawnIfNoBed(ServerPlayer player) {
        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos != null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos worldSpawn = level.getSharedSpawnPos();
        float yaw = level.getSharedSpawnAngle();

        Vec3 dest = Vec3.atBottomCenterOf(worldSpawn);
        player.teleportTo(level, dest.x, dest.y, dest.z, yaw, player.getXRot());
    }
}
