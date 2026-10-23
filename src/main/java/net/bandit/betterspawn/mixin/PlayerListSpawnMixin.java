package net.bandit.betterspawn.mixin;

import net.bandit.betterspawn.BetterSpawnFirstJoin;
import net.bandit.betterspawn.SpawnFinder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stats;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListSpawnMixin {

    @Shadow @Final private MinecraftServer server;

    @Inject(
            method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At("TAIL")
    )
    private void betterspawn$forceWorldSpawnOnFirstJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        BetterSpawnFirstJoin firstJoin = (BetterSpawnFirstJoin) player;

        if (firstJoin.betterspawn$firstJoinDone()) return;
        firstJoin.betterspawn$setFirstJoinDone(true);
        if (betterspawn$isExistingPlayer(player)) return;

        ServerLevel level = player.serverLevel();
        BlockPos raw = level.getSharedSpawnPos();
        float yaw = level.getSharedSpawnAngle();

        this.server.tell(new net.minecraft.server.TickTask(this.server.getTickCount() + 1, () -> {
            if (player.hasDisconnected()) return;
            BlockPos best = SpawnFinder.findSafeSpawnNear(level, raw);
            Vec3 dest = Vec3.atBottomCenterOf(best);

            player.teleportTo(level, dest.x, dest.y, dest.z, yaw, player.getXRot());
            player.setDeltaMovement(0, 0, 0);
            player.fallDistance = 0;
            player.hurtMarked = true;
        }));
    }

    @Unique
    private static boolean betterspawn$isExistingPlayer(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 0;
    }
}
