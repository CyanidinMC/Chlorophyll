package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingMinecraftServer;
import me.mrhua269.chlorophyll.utils.TickThread;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.BooleanSupplier;

@Mixin(value = MinecraftServer.class, priority = 5000)
public abstract class MinecraftServerMixin implements ITaskSchedulingMinecraftServer {

    @Shadow public abstract Iterable<ServerLevel> getAllLevels();

    @Shadow public abstract ServerConnectionListener getConnection();

    @Shadow private PlayerList playerList;

    @Shadow @Final private List<Runnable> tickables;

    @Shadow private int tickCount;
    @Unique private boolean shouldPollChunkTask = true;

    @Inject(method = "pollTaskInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;isSprinting()Z", shift = At.Shift.BEFORE), cancellable = true, order = 80)
    public void onPollingWorldChunkTasks(@NotNull CallbackInfoReturnable<Boolean> cir){
        if (!this.shouldPollChunkTask){
            cir.setReturnValue(false); //We will do it on world's own tickloop
        }
    }

    @Redirect(method = "autoSave", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveEverything(ZZZ)Z"))
    public boolean saveEverything$Kill(MinecraftServer instance, boolean bl, boolean bl2, boolean bl3){
        return false; //We will do it on world's own tickloop
    }

    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V"))
    public void onTickChildren(MinecraftServer minecraftServer, BooleanSupplier booleanSupplier){
        Chlorophyll.server = (MinecraftServer) (Object)this;
        this.shouldPollChunkTask = false;

        // Active the tick loops
        for (ServerLevel level : this.getAllLevels()){
            ((ITaskSchedulingLevel) level).chlorophyll$setupTickLoop();
        }

        // Time
        for (ServerLevel level : this.getAllLevels()) {
            level.tickTime();
        }

        // Tick base connections
        this.getConnection().tick();

        // Player list
        this.playerList.tick();

        // Server GUI
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

    @Inject(method = "stopServer", at = @At(value = "HEAD"))
    public void onServerStop(CallbackInfo ci){
        Chlorophyll.killAllAndAwait();
        this.shouldPollChunkTask = true;
    }

    @Override
    public boolean chlorophyll$shouldPollChunkTasks() {
        return this.shouldPollChunkTask;
    }
}
