package net.bandit.betterspawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Betterspawn.MODID)
public class BetterSpawnNeoForgeEvents {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> forceExactWorldSpawnIfNoBed(player));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> forceExactWorldSpawnIfNoBed(player));
        }
    }

    private static void forceExactWorldSpawnIfNoBed(ServerPlayer player) {
        if (player.getRespawnPosition() != null) return;
        ServerLevel level = player.serverLevel();

        BlockPos spawn = level.getSharedSpawnPos();
        float yaw = level.getSharedSpawnAngle();

        Vec3 dest = Vec3.atBottomCenterOf(spawn);
        player.teleportTo(level, dest.x, dest.y, dest.z, yaw, player.getXRot());
    }
}
