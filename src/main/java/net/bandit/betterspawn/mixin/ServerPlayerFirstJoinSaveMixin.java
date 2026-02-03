package net.bandit.betterspawn.mixin;

import net.bandit.betterspawn.BetterSpawnFirstJoin;
import net.bandit.betterspawn.BetterSpawnTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerFirstJoinSaveMixin implements BetterSpawnFirstJoin {

    @Unique
    private boolean betterspawn$firstJoinDone = false;

    @Override
    public boolean betterspawn$firstJoinDone() {
        return this.betterspawn$firstJoinDone;
    }

    @Override
    public void betterspawn$setFirstJoinDone(boolean value) {
        this.betterspawn$firstJoinDone = value;
    }

    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void betterspawn$copyFlagOnRespawn(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        if (oldPlayer instanceof BetterSpawnFirstJoin old) {
            this.betterspawn$firstJoinDone = old.betterspawn$firstJoinDone();
        }
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void betterspawn$readFlag(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(BetterSpawnTags.ROOT, CompoundTag.TAG_COMPOUND)) {
            CompoundTag root = tag.getCompound(BetterSpawnTags.ROOT);
            this.betterspawn$firstJoinDone = root.getBoolean(BetterSpawnTags.FIRST_JOIN_DONE);
        } else {
            this.betterspawn$firstJoinDone = false;
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void betterspawn$writeFlag(CompoundTag tag, CallbackInfo ci) {
        CompoundTag root = tag.getCompound(BetterSpawnTags.ROOT);
        root.putBoolean(BetterSpawnTags.FIRST_JOIN_DONE, this.betterspawn$firstJoinDone);
        tag.put(BetterSpawnTags.ROOT, root);
    }
}
