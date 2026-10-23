package net.bandit.betterspawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;


public final class SpawnFinder {
    private SpawnFinder() {}

    public static final int HORIZONTAL_RADIUS = 32;
    public static final int VERTICAL_SCAN = 24;
    private static final int SURFACE_VERTICAL_SCAN = 48;

    private static final int ROOF_GAP_SCAN = 6;

    private enum Result { INVALID, ROOF, OK }

    public static BlockPos findSafeSpawnNear(ServerLevel level, BlockPos center) {
        return findSafeSpawnNear(level, center, HORIZONTAL_RADIUS, VERTICAL_SCAN);
    }

    public static BlockPos findSafeSpawnNear(ServerLevel level, BlockPos center, int radius, int verticalScan) {
        int cx = center.getX();
        int cz = center.getZ();
        int cy = center.getY();

        BlockPos[] roofFallback = new BlockPos[1];

        BlockPos direct = findSafeAtXZ(level, cx, cz, cy, verticalScan, roofFallback);
        if (direct != null) return direct;

        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                BlockPos p1 = findSafeAtXZ(level, cx + dx, cz + r, cy, verticalScan, roofFallback);
                if (p1 != null) return p1;
                BlockPos p2 = findSafeAtXZ(level, cx + dx, cz - r, cy, verticalScan, roofFallback);
                if (p2 != null) return p2;
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                BlockPos p1 = findSafeAtXZ(level, cx + r, cz + dz, cy, verticalScan, roofFallback);
                if (p1 != null) return p1;
                BlockPos p2 = findSafeAtXZ(level, cx - r, cz + dz, cy, verticalScan, roofFallback);
                if (p2 != null) return p2;
            }
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        BlockPos surface = findSafeAtXZ(level, cx, cz, surfaceY, SURFACE_VERTICAL_SCAN, roofFallback);
        if (surface != null) return surface;

        if (roofFallback[0] != null) return roofFallback[0];

        return new BlockPos(cx, surfaceY, cz);
    }

    private static BlockPos findSafeAtXZ(ServerLevel level, int x, int z, int startY, int verticalScan, BlockPos[] roofFallback) {
        for (int d = 0; d <= verticalScan; d++) {
            BlockPos down = check(level, x, startY - d, z, roofFallback);
            if (down != null) return down;

            if (d != 0) {
                BlockPos up = check(level, x, startY + d, z, roofFallback);
                if (up != null) return up;
            }
        }
        return null;
    }

    private static BlockPos check(ServerLevel level, int x, int y, int z, BlockPos[] roofFallback) {
        BlockPos pos = new BlockPos(x, y, z);
        Result r = evaluate(level, pos);
        if (r == Result.OK) return pos;
        if (r == Result.ROOF && roofFallback[0] == null) roofFallback[0] = pos;
        return null;
    }

    private static Result evaluate(ServerLevel level, BlockPos pos) {
        int y = pos.getY();
        if (y <= level.getMinBuildHeight() + 1) return Result.INVALID;
        if (y >= level.getMaxBuildHeight() - 2) return Result.INVALID;

        if (!isFeetPassable(level, pos)) return Result.INVALID;
        if (!isHeadPassable(level, pos.above())) return Result.INVALID;

        BlockPos floorPos = pos.below();
        BlockState floor = level.getBlockState(floorPos);
        if (!isGoodFloor(level, floorPos, floor)) return Result.INVALID;

        if (isStructureRoof(level, floorPos)) return Result.ROOF;

        return Result.OK;
    }

    private static boolean isFeetPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        if (isHarmfulToStandIn(state)) return false;

        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() || shape.max(Direction.Axis.Y) <= 0.25;
    }

    private static boolean isHeadPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        if (isHarmfulToStandIn(state)) return false;
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isHarmfulToStandIn(BlockState state) {
        return state.is(BlockTags.FIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.COBWEB)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.NETHER_PORTAL)
                || state.is(Blocks.END_PORTAL);
    }

    private static boolean isGoodFloor(ServerLevel level, BlockPos floorPos, BlockState floor) {
        if (floor.isAir()) return false;
        if (!floor.getFluidState().isEmpty()) return false;
        if (floor.getCollisionShape(level, floorPos).isEmpty()) return false;

        if (floor.is(BlockTags.LEAVES)) return false;
        if (floor.is(BlockTags.LOGS)) return false;
        if (floor.is(Blocks.BROWN_MUSHROOM_BLOCK) || floor.is(Blocks.RED_MUSHROOM_BLOCK) || floor.is(Blocks.MUSHROOM_STEM)) return false;

        if (floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS) || floor.is(BlockTags.CAMPFIRES)) return false;
        if (floor.is(Blocks.POWDER_SNOW)) return false;

        return true;
    }


    private static boolean isStructureRoof(ServerLevel level, BlockPos floorPos) {
        if (!hasOpenSpaceBelow(level, floorPos)) return false;
        return level.structureManager().getStructureWithPieceAt(floorPos, holder -> true).isValid();
    }

    private static boolean hasOpenSpaceBelow(ServerLevel level, BlockPos floorPos) {
        int run = 0;
        BlockPos.MutableBlockPos m = floorPos.mutable();
        for (int i = 1; i <= ROOF_GAP_SCAN; i++) {
            m.move(Direction.DOWN);
            if (m.getY() <= level.getMinBuildHeight()) return false;
            BlockState s = level.getBlockState(m);
            boolean open = s.getFluidState().isEmpty() && s.getCollisionShape(level, m).isEmpty();
            if (open) {
                if (++run >= 2) return true;
            } else {
                run = 0;
            }
        }
        return false;
    }
}
