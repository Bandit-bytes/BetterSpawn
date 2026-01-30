package net.bandit.betterspawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.EnumSet;

@EventBusSubscriber(modid = Betterspawn.MODID)
public class BetterSpawnNeoForgeEvents {

    private static final String FIRST_JOIN_TAG = "betterspawn_first_join_done";

    private static final int HORIZONTAL_RADIUS = 32;
    private static final int VERTICAL_SCAN = 24;
    private static final int SURFACE_VERTICAL_SCAN = 48;

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.getPersistentData().getBoolean(FIRST_JOIN_TAG).orElse(false)) return;

        ServerLevel level = (ServerLevel) player.level();
        MinecraftServer server = level.getServer();

        server.execute(() -> {
            if (player.getPersistentData().getBoolean(FIRST_JOIN_TAG).orElse(false)) return;

            forceSafeWorldSpawn(player);
            player.getPersistentData().putBoolean(FIRST_JOIN_TAG, true);
        });
    }

    private static void forceSafeWorldSpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        var data = (ServerLevelData) level.getLevelData();
        var spawn = data.getRespawnData();

        BlockPos raw = spawn.pos();
        float yaw = spawn.yaw();

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
    }

    private static BlockPos findSafeSpawnNear(ServerLevel level, BlockPos center, int radius, int verticalScan) {
        int cx = center.getX();
        int cz = center.getZ();

        // Bias above the stored Y so setting spawn "on the floor block" feels right
        int cy = center.getY() + 1;

        // 1) Try exact X/Z near the configured Y first
        BlockPos direct = findSafeAtXZ(level, cx, cz, cy, verticalScan);
        if (direct != null) return direct;

        // 2) Spiral around X/Z scanning around configured Y
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

        // 3) Fallback: surface at spawn X/Z (still tree-safe due to candidateAt)
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

        if (below.is(BlockTags.LEAVES)) return null;
        if (below.is(BlockTags.LOGS)) return null;

        if (below.isAir()) return null;
        if (below.is(Blocks.WATER) || below.is(Blocks.LAVA)) return null;
        if (below.getCollisionShape(level, belowPos).isEmpty()) return null;

        return pos;
    }
}
