package net.bandit.betterspawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class BetterSpawnFinder {
    private BetterSpawnFinder() {}

    private static final int HORIZONTAL_RADIUS = 32;
    private static final int VERTICAL_SCAN = 24;

    public static BlockPos findSafeSpawnNear(ServerLevel level, BlockPos center) {
        int cx = center.getX();
        int cz = center.getZ();

        BlockPos direct = findFloorBelow(level, cx, center.getY() + 1, cz);
        if (direct != null) return direct;

        for (int r = 0; r <= HORIZONTAL_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                BlockPos p1 = surfaceCandidate(level, cx + dx, cz + r);
                if (p1 != null) return p1;

                BlockPos p2 = surfaceCandidate(level, cx + dx, cz - r);
                if (p2 != null) return p2;
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                BlockPos p1 = surfaceCandidate(level, cx + r, cz + dz);
                if (p1 != null) return p1;

                BlockPos p2 = surfaceCandidate(level, cx - r, cz + dz);
                if (p2 != null) return p2;
            }
        }

        return new BlockPos(cx, surfaceY(level, cx, cz), cz);
    }

    // Stops at the first block in the way, so a rejected roof can't drop the player into a cave under it.
    private static BlockPos findFloorBelow(ServerLevel level, int x, int startY, int z) {
        for (int y = startY; y >= startY - VERTICAL_SCAN; y--) {
            BlockPos candidate = candidateAt(level, x, y, z);
            if (candidate != null) return candidate;

            if (!isPassable(level, new BlockPos(x, y, z))) return null;
        }
        return null;
    }

    private static BlockPos surfaceCandidate(ServerLevel level, int x, int z) {
        return candidateAt(level, x, surfaceY(level, x, z), z);
    }

    private static int surfaceY(ServerLevel level, int x, int z) {
        // getHeight reports the bottom of the world for chunks that aren't loaded yet.
        level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static BlockPos candidateAt(ServerLevel level, int x, int y, int z) {
        if (y <= level.getMinBuildHeight() + 1) return null;
        if (y >= level.getMaxBuildHeight() - 2) return null;

        BlockPos pos = new BlockPos(x, y, z);

        if (!isPassable(level, pos)) return null;
        if (!isPassable(level, pos.above())) return null;

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);

        if (below.isAir()) return null;
        if (below.is(Blocks.WATER) || below.is(Blocks.LAVA)) return null;
        if (below.getCollisionShape(level, belowPos).isEmpty()) return null;
        if (below.is(BlockTags.LEAVES)) return null;
        if (below.is(BlockTags.LOGS)) return null;
        if (isGeneratedBuilding(level, belowPos, below)) return null;

        return pos;
    }

    // Grass and snow layers still count as open space, otherwise real ground loses out to bare roofs.
    private static boolean isPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;

        return state.canBeReplaced()
                && state.getFluidState().isEmpty()
                && state.getCollisionShape(level, pos).isEmpty()
                && !state.is(BlockTags.FIRE);
    }

    private static boolean isGeneratedBuilding(ServerLevel level, BlockPos pos, BlockState state) {
        if (isTerrain(state)) return false;
        return level.structureManager().getStructureWithPieceAt(pos, structure -> true).isValid();
    }

    private static boolean isTerrain(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.DIRT_PATH);
    }
}
