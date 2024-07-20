package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.utils.TickThread;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {
    @Shadow @Final
    Thread mainThread;

    @Redirect(method = "getChunkFuture", at = @At(value = "INVOKE", target= "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
    public Thread mainThreadCheckByPass$getChunkFuture() {
        return byPassMainThreadCheckIfTickThread();
    }

    @Redirect(method = "getChunk", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
    public Thread mainThreadCheckByPass$getChunk() {
        return byPassMainThreadCheckIfTickThread();
    }

    @Redirect(method = "getChunkNow", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
    public Thread mainThreadCheckByPass$getChunkNow() {
        return byPassMainThreadCheckIfTickThread();
    }

    @Unique
    private Thread byPassMainThreadCheckIfTickThread(){
        if (!TickThread.isTickThread()){
            return Thread.currentThread();
        }

        return this.mainThread;
    }
}
