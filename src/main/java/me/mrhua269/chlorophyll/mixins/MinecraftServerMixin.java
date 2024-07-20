package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import me.mrhua269.chlorophyll.utils.TickThread;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract Iterable<ServerLevel> getAllLevels();

    @Shadow public abstract ServerConnectionListener getConnection();

    @Shadow private PlayerList playerList;

    @Shadow @Final private List<Runnable> tickables;

    @Shadow private int tickCount;
    @Unique private boolean shouldPollChunkTask = true;

    @Inject(method = "pollTaskInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;isSprinting()Z", shift = At.Shift.BEFORE), cancellable = true)
    public void onPollingWorldChunkTasks(@NotNull CallbackInfoReturnable<Boolean> cir){
        if (!this.shouldPollChunkTask){
            cir.setReturnValue(false); //We will do it on world's own tickloop
        }
    }

    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveEverything(ZZZ)Z"))
    public boolean saveEverything$Kill(MinecraftServer instance, boolean bl, boolean bl2, boolean bl3){
        return false; //We will do it on world's own tickloop
    }

    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V"))
    public void onTickChildren(MinecraftServer minecraftServer, BooleanSupplier booleanSupplier){
        this.shouldPollChunkTask = false;

        for (ServerLevel level : this.getAllLevels()){
            Chlorophyll.checkAndCreateTickLoop(level);
        }

        this.getConnection().tick();

        this.playerList.tick();

        for (Runnable tickable : this.tickables) {
            tickable.run();
        }
    }

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public int getTickCount(){
        final TickThread current = TickThread.currentThread();

        if (current == null) {
            return this.tickCount;
        }

        if (current.currentTickLoop == null) {
            return this.tickCount;
        }

        return current.currentTickLoop.getTickCount();
    }

    /**
     * @author MrHua269
     * @reason New async checkers
     */
    @Overwrite
    public static <S extends MinecraftServer> S spin(Function<Thread, S> function) {
        AtomicReference<S> atomicReference = new AtomicReference<>();

        Thread thread = new TickThread(() -> atomicReference.get().runServer(), "Server thread", false);
        thread.setUncaughtExceptionHandler((threadx, throwable) -> LOGGER.error("Uncaught exception in server thread", throwable));

        if (Runtime.getRuntime().availableProcessors() > 4) {
            thread.setPriority(8);
        }

        S minecraftServer = function.apply(thread);
        atomicReference.set(minecraftServer);
        thread.start();
        return minecraftServer;
    }

    @Inject(method = "stopServer", at = @At(value = "HEAD"))
    public void onServerStop(CallbackInfo ci){
        Chlorophyll.killAllAndAwait();
        this.shouldPollChunkTask = true;
    }
}
