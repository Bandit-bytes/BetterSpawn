package net.bandit.betterspawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "betterspawn")
public class BetterSpawnForgeEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forceExactWorldSpawnIfNoBed(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forceExactWorldSpawnIfNoBed(player);
        }
    }

    private static void forceExactWorldSpawnIfNoBed(ServerPlayer player) {
        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos != null) return;
        ServerLevel level = player.serverLevel();
        BlockPos spawn = level.getSharedSpawnPos();
        float yaw = level.getSharedSpawnAngle();
        Vec3 dest = Vec3.atBottomCenterOf(spawn);
        player.teleportTo(level, dest.x, dest.y, dest.z, yaw, player.getXRot());
    }
}
