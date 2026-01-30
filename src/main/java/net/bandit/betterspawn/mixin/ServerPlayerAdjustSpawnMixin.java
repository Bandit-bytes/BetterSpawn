package net.bandit.betterspawn.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerAdjustSpawnMixin {

    @Inject(method = "adjustSpawnLocation", at = @At("HEAD"), cancellable = true)
    private void betterspawn$noAdjustSpawn(ServerLevel level, BlockPos pos, CallbackInfoReturnable<BlockPos> cir) {
        cir.setReturnValue(pos);
    }
}
