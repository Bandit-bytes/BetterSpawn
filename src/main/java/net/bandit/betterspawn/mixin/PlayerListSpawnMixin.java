package net.bandit.betterspawn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(PlayerList.class)
public class PlayerListSpawnMixin {

    @Shadow private net.minecraft.server.MinecraftServer server;

    @Inject(
            method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At("TAIL")
    )
    private void betterspawn$forceWorldSpawnOnFirstJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        // If they have a real respawn config (bed/anchor), don't mess with it.
        if (player.getRespawnConfig() != null) return;

        ServerLevel level = (ServerLevel) player.level();
        ServerLevelData data = (ServerLevelData) level.getLevelData();

        // World spawn X/Z (whatever /setworldspawn is)
        BlockPos raw = data.getRespawnData().pos();
        float yaw = data.getRespawnData().yaw();

        // Compute Y using NO_LEAVES heightmap so we don’t land on canopy roofs.
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, raw.getX(), raw.getZ());
        BlockPos fixed = new BlockPos(raw.getX(), y, raw.getZ());

        Vec3 dest = Vec3.atBottomCenterOf(fixed);

        // Run after join flow, on server thread
        this.server.execute(() -> {
            player.teleportTo(
                    level,
                    dest.x, dest.y, dest.z,
                    EnumSet.noneOf(Relative.class),
                    yaw,
                    player.getXRot(),
                    false
            );
            player.setDeltaMovement(0, 0, 0);
            player.hurtMarked = true;
        });
    }
}
