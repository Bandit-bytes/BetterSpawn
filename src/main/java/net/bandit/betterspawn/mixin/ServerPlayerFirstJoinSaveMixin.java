package net.bandit.betterspawn.mixin;

import net.bandit.betterspawn.BetterSpawnFirstJoin;
import net.bandit.betterspawn.BetterSpawnTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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


    @Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V", at = @At("TAIL"))
    private void betterspawn$copyFlagOnRespawn(ServerPlayer oldPlayer, boolean keepEverything, CallbackInfo ci) {
        if (oldPlayer instanceof BetterSpawnFirstJoin old) {
            this.betterspawn$firstJoinDone = old.betterspawn$firstJoinDone();
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void betterspawn$readFlag(ValueInput in, CallbackInfo ci) {
        in.child(BetterSpawnTags.ROOT).ifPresentOrElse(
                root -> this.betterspawn$firstJoinDone =
                        root.getBooleanOr(BetterSpawnTags.FIRST_JOIN_DONE, false),
                () -> this.betterspawn$firstJoinDone = false
        );
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void betterspawn$writeFlag(ValueOutput out, CallbackInfo ci) {
        ValueOutput root = out.child(BetterSpawnTags.ROOT);
        root.putBoolean(BetterSpawnTags.FIRST_JOIN_DONE, this.betterspawn$firstJoinDone);
    }
}