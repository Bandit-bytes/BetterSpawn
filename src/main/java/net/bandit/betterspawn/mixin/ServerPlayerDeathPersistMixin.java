package net.bandit.betterspawn.mixin;

import net.bandit.betterspawn.BetterSpawnPendingRespawn;
import net.bandit.betterspawn.BetterSpawnTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerDeathPersistMixin implements BetterSpawnPendingRespawn {

    @Unique private boolean betterspawn$pendingRespawn = false;
    @Unique private String betterspawn$lastDeathMessageText = null;

    @Inject(method = "die", at = @At("TAIL"))
    private void betterspawn$markPendingRespawn(DamageSource source, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;

        this.betterspawn$pendingRespawn = true;

        Component msg = self.getCombatTracker().getDeathMessage();
        this.betterspawn$lastDeathMessageText = msg.getString();
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void betterspawn$readPending(ValueInput in, CallbackInfo ci) {
        in.child(BetterSpawnTags.ROOT).ifPresentOrElse(root -> {
            this.betterspawn$pendingRespawn =
                    root.getBooleanOr(BetterSpawnTags.PENDING_RESPAWN, false);

            this.betterspawn$lastDeathMessageText =
                    root.getStringOr(BetterSpawnTags.LAST_DEATH_MSG_TEXT, null);
        }, () -> {
            this.betterspawn$pendingRespawn = false;
            this.betterspawn$lastDeathMessageText = null;
        });
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void betterspawn$writePending(ValueOutput out, CallbackInfo ci) {
        ValueOutput root = out.child(BetterSpawnTags.ROOT);
        root.putBoolean(BetterSpawnTags.PENDING_RESPAWN, this.betterspawn$pendingRespawn);

        if (this.betterspawn$lastDeathMessageText != null) {
            root.putString(BetterSpawnTags.LAST_DEATH_MSG_TEXT, this.betterspawn$lastDeathMessageText);
        }
    }

    @Override
    public boolean betterspawn$isPendingRespawn() {
        return this.betterspawn$pendingRespawn;
    }

    @Override
    public String betterspawn$getLastDeathMessageText() {
        return this.betterspawn$lastDeathMessageText;
    }

    @Override
    public void betterspawn$clearPendingRespawn() {
        this.betterspawn$pendingRespawn = false;
        this.betterspawn$lastDeathMessageText = null;
    }
}