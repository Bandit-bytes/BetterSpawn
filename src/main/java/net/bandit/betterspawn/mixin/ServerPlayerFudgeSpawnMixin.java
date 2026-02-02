package net.bandit.betterspawn.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerFudgeSpawnMixin {

    @Inject(method = "fudgeSpawnLocation", at = @At("HEAD"), cancellable = true)
    private void betterspawn$noFudgeSpawn(ServerLevel level, CallbackInfo ci) {
        // Cancel vanilla's spawn fudging entirely.
        ci.cancel();
    }
}
