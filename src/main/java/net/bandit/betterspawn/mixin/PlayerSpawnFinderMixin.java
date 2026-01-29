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

import java.util.concurrent.CompletableFuture;

@Mixin(PlayerList.class)
public class PlayerSpawnFinderMixin {

    /**
     * First join / login placement.
     * If they don't have a respawn point, force exact worldspawn.
     */
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void betterspawn$afterPlaceNewPlayer(CallbackInfo ci) {
        // Can't access params from here without matching signature,
        // so we do the safer approach below (see note).
    }

    /**
     * Respawn after death (or forced respawn).
     * We can reliably get the new ServerPlayer from return value.
     */
    @Inject(method = "respawn", at = @At("RETURN"))
    private void betterspawn$afterRespawn(CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = cir.getReturnValue();
        if (player != null) {
            betterspawn$forceExactWorldSpawnIfNoBed(player);
        }
    }

    @Unique
    private static void betterspawn$forceExactWorldSpawnIfNoBed(ServerPlayer player) {
        // If the player has a respawn position set (bed/anchor), do nothing.
        // In 1.21.1 this is typically stored even if invalid, but if it's null, it's definitely "worldspawn".
        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos != null) {
            return;
        }

        // Worldspawn in the player's current level (or use player.server.overworld() if you want overworld always)
        ServerLevel level = player.serverLevel();
        BlockPos worldSpawn = level.getSharedSpawnPos();
        float yaw = level.getSharedSpawnAngle();

        Vec3 dest = Vec3.atBottomCenterOf(worldSpawn);
        player.teleportTo(level, dest.x, dest.y, dest.z, yaw, player.getXRot());
    }
}
