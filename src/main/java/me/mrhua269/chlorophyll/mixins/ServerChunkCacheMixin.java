package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingMinecraftServer;
import me.mrhua269.chlorophyll.utils.TickThread;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkCache.class)
public class ServerChunkCacheMixin {
    @Shadow @Final
    ServerLevel level;

    @Inject(method = "pollTask", at = @At(value = "HEAD"), cancellable = true)
    public void onPollTaskCall(CallbackInfoReturnable<Boolean> cir) {
        if (((ITaskSchedulingMinecraftServer) this.level.getServer()).chlorophyll$shouldPollChunkTasks()) {
            return;
        }

        final TickThread tickThread = TickThread.currentThread();

        if (tickThread == null) {
            cir.setReturnValue(false);
            return;
        }

        final ServerLevel self = this.level;

        if (self != null && self != tickThread.currentTickLoop.getOwnedLevel()) {
            cir.setReturnValue(false);
        }
    }
}
