package net.bandit.betterspawn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

@Mixin(PlayerList.class)
public class PlayerListDeathRespawnMixin {

    private static final int HORIZONTAL_RADIUS = 32;
    private static final int VERTICAL_SCAN = 24;
    private static final int SURFACE_VERTICAL_SCAN = 48;

    @Inject(method = "respawn", at = @At("RETURN"))
    private void betterspawn$death(CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer player = cir.getReturnValue();
        if (player == null) return;

        ServerPlayer.RespawnConfig cfg = player.getRespawnConfig();
        if (cfg != null && hasValidPersonalRespawn(player, cfg)) return;

        ServerLevel level = (ServerLevel) player.level();
        ServerLevelData data = (ServerLevelData) level.getLevelData();

        BlockPos worldSpawn = data.getRespawnData().pos();
        float yaw = data.getRespawnData().yaw();

        BlockPos safe = findSafeSpawnNear(level, worldSpawn);
        Vec3 dest = Vec3.atBottomCenterOf(safe);

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

    @Unique
    private static boolean hasValidPersonalRespawn(ServerPlayer player, ServerPlayer.RespawnConfig cfg) {
        MinecraftServer srv = player.level().getServer();
        if (srv == null) return false;

        ServerLevel level = srv.getLevel(cfg.respawnData().dimension());
        if (level == null) return false;

        BlockPos pos = cfg.respawnData().pos();
        BlockState state = level.getBlockState(pos);

        if (state.getBlock() instanceof RespawnAnchorBlock) {
            boolean forced = cfg.forced();
            int charge = state.getValue(RespawnAnchorBlock.CHARGE);
            return (forced || charge > 0) && RespawnAnchorBlock.canSetSpawn(level, pos);
        }

        if (state.getBlock() instanceof BedBlock) {
            BedRule rule = level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos);
            return rule.canSetSpawn(level);
        }

        if (cfg.forced()) {
            BlockState above = level.getBlockState(pos.above());
            return state.getBlock().isPossibleToRespawnInThis(state)
                    && above.getBlock().isPossibleToRespawnInThis(above);
        }

        return false;
    }

    private static BlockPos findSafeSpawnNear(ServerLevel level, BlockPos center) {
        int cx = center.getX();
        int cz = center.getZ();
        int cy = center.getY();

        BlockPos direct = findSafeAtXZ(level, cx, cz, cy);
        if (direct != null) return direct;

        for (int r = 0; r <= HORIZONTAL_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                BlockPos p1 = findSafeAtXZ(level, cx + dx, cz + r, cy);
                if (p1 != null) return p1;

                BlockPos p2 = findSafeAtXZ(level, cx + dx, cz - r, cy);
                if (p2 != null) return p2;
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                BlockPos p1 = findSafeAtXZ(level, cx + r, cz + dz, cy);
                if (p1 != null) return p1;

                BlockPos p2 = findSafeAtXZ(level, cx - r, cz + dz, cy);
                if (p2 != null) return p2;
            }
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        BlockPos surface = findSafeAtXZ(level, cx, cz, surfaceY + 1);
        return surface != null ? surface : new BlockPos(cx, surfaceY + 1, cz);
    }

    private static BlockPos findSafeAtXZ(ServerLevel level, int x, int z, int startY) {
        for (int d = 0; d <= VERTICAL_SCAN; d++) {
            BlockPos up = candidate(level, x, startY + d, z);
            if (up != null) return up;

            if (d != 0) {
                BlockPos down = candidate(level, x, startY - d, z);
                if (down != null) return down;
            }
        }
        return null;
    }

    private static BlockPos candidate(ServerLevel level, int x, int y, int z) {
        if (y <= level.getMinY() + 1 || y >= level.getMaxY() - 2) return null;

        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);

        if (!level.getBlockState(pos).isAir()) return null;
        if (!level.getBlockState(pos.above()).isAir()) return null;

        if (ground.isAir()) return null;
        if (ground.is(Blocks.WATER) || ground.is(Blocks.LAVA)) return null;
        if (ground.getCollisionShape(level, below).isEmpty()) return null;
        if (ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS)) return null;

        return pos;
    }
}