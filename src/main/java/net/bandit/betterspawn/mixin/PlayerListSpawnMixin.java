package net.bandit.betterspawn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListSpawnMixin {

    @Shadow private MinecraftServer server;

    @Inject(
            method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V",
            at = @At("TAIL")
    )
    private void betterspawn$forceWorldSpawnOnFirstJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (player.getRespawnPosition() != null) return;

        ServerLevel level = player.serverLevel();

        BlockPos raw = level.getSharedSpawnPos();
        float yaw = level.getSharedSpawnAngle();

        this.server.execute(() -> {
            BlockPos best = findNonTreeGroundNear(level, raw, 32);
            Vec3 dest = Vec3.atBottomCenterOf(best);

            player.teleportTo(level, dest.x, dest.y, dest.z, yaw, player.getXRot());

            player.setDeltaMovement(0, 0, 0);
            player.hurtMarked = true;
        });
    }

    private static BlockPos findNonTreeGroundNear(ServerLevel level, BlockPos center, int radius) {
        int cx = center.getX();
        int cz = center.getZ();

        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                int x1 = cx + dx;

                BlockPos p1 = candidate(level, x1, cz + r);
                if (p1 != null) return p1;

                BlockPos p2 = candidate(level, x1, cz - r);
                if (p2 != null) return p2;
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                int z1 = cz + dz;

                BlockPos p1 = candidate(level, cx + r, z1);
                if (p1 != null) return p1;

                BlockPos p2 = candidate(level, cx - r, z1);
                if (p2 != null) return p2;
            }
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        return new BlockPos(cx, y, cz);
    }

    private static BlockPos candidate(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        if (!level.getBlockState(pos).isAir()) return null;
        if (!level.getBlockState(pos.above()).isAir()) return null;

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);

        if (below.is(BlockTags.LEAVES)) return null;
        if (below.is(BlockTags.LOGS)) return null;
        if (below.isAir()) return null;
        if (below.is(Blocks.WATER) || below.is(Blocks.LAVA)) return null;
        if (below.getCollisionShape(level, belowPos).isEmpty()) return null;

        return pos;
    }
}
