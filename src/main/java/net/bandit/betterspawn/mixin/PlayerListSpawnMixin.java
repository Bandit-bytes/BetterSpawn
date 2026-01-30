package net.bandit.betterspawn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

    @Shadow private MinecraftServer server;

    private static final int HORIZONTAL_RADIUS = 32;
    private static final int VERTICAL_SCAN = 24;
    private static final int SURFACE_VERTICAL_SCAN = 48;

    @Inject(
            method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At("TAIL")
    )
    private void betterspawn$forceWorldSpawnOnFirstJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (player.getRespawnConfig() != null) return;

        ServerLevel level = (ServerLevel) player.level();
        ServerLevelData data = (ServerLevelData) level.getLevelData();

        BlockPos raw = data.getRespawnData().pos();
        float yaw = data.getRespawnData().yaw();

        this.server.execute(() -> {
            BlockPos best = findSafeSpawnNear(level, raw, HORIZONTAL_RADIUS, VERTICAL_SCAN);
            Vec3 dest = Vec3.atBottomCenterOf(best);

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

    private static BlockPos findSafeSpawnNear(ServerLevel level, BlockPos center, int radius, int verticalScan) {
        int cx = center.getX();
        int cz = center.getZ();

        int cy = center.getY() + 1;

        BlockPos direct = findSafeAtXZ(level, cx, cz, cy, verticalScan);
        if (direct != null) return direct;
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                int x1 = cx + dx;

                BlockPos p1 = findSafeAtXZ(level, x1, cz + r, cy, verticalScan);
                if (p1 != null) return p1;

                BlockPos p2 = findSafeAtXZ(level, x1, cz - r, cy, verticalScan);
                if (p2 != null) return p2;
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                int z1 = cz + dz;

                BlockPos p1 = findSafeAtXZ(level, cx + r, z1, cy, verticalScan);
                if (p1 != null) return p1;

                BlockPos p2 = findSafeAtXZ(level, cx - r, z1, cy, verticalScan);
                if (p2 != null) return p2;
            }
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        BlockPos surface = findSafeAtXZ(level, cx, cz, surfaceY, SURFACE_VERTICAL_SCAN);
        if (surface != null) return surface;

        return new BlockPos(cx, surfaceY, cz);
    }

    private static BlockPos findSafeAtXZ(ServerLevel level, int x, int z, int startY, int verticalScan) {
        for (int d = 0; d <= verticalScan; d++) {
            BlockPos up = candidateAt(level, x, startY + d, z);
            if (up != null) return up;

            if (d != 0) {
                BlockPos down = candidateAt(level, x, startY - d, z);
                if (down != null) return down;
            }
        }
        return null;
    }

    private static BlockPos candidateAt(ServerLevel level, int x, int y, int z) {
        if (y <= level.getMinY() + 1) return null;
        if (y >= level.getMaxY() - 2) return null;

        BlockPos pos = new BlockPos(x, y, z);

        if (!level.getBlockState(pos).isAir()) return null;
        if (!level.getBlockState(pos.above()).isAir()) return null;

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);

        if (below.isAir()) return null;
        if (below.is(Blocks.WATER) || below.is(Blocks.LAVA)) return null;
        if (below.getCollisionShape(level, belowPos).isEmpty()) return null;

        if (below.is(BlockTags.LEAVES)) return null;
        if (below.is(BlockTags.LOGS)) return null;

        return pos;
    }
}
