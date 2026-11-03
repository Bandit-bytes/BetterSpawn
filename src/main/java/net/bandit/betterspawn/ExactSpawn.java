package net.bandit.betterspawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ExactSpawn {
    private ExactSpawn() {}

    /**
     * The configured spawn wins whenever a player can safely stand there. The wider search has its own
     * stricter floor rules, which must not reject this position. The neighbours cover a spawn recorded
     * inside a partial block such as a slab (above) or one block over the floor (below).
     */
    public static BlockPos find(ServerLevel level, BlockPos center) {
        if (canStandAt(level, center)) return center;
        if (canStandAt(level, center.above())) return center.above();
        if (canStandAt(level, center.below())) return center.below();
        return null;
    }

    private static boolean canStandAt(ServerLevel level, BlockPos pos) {
        int y = pos.getY();
        if (y <= level.getMinY() + 1) return false;
        if (y >= level.getMaxY() - 2) return false;

        if (!isFeetPassable(level, pos)) return false;
        if (!isHeadPassable(level, pos.above())) return false;

        BlockPos floorPos = pos.below();
        return isSafeFloor(level, floorPos, level.getBlockState(floorPos));
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

    private static boolean isSafeFloor(ServerLevel level, BlockPos floorPos, BlockState floor) {
        if (floor.isAir()) return false;
        if (!floor.getFluidState().isEmpty()) return false;
        if (floor.getCollisionShape(level, floorPos).isEmpty()) return false;

        if (floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS) || floor.is(BlockTags.CAMPFIRES)) return false;
        return !floor.is(Blocks.POWDER_SNOW);
    }
}
