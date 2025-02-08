package me.mrhua269.chlorophyll.mixins;

import ca.spottedleaf.moonrise.common.util.TickThread;
import me.mrhua269.chlorophyll.Chlorophyll;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingMinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(BlockableEventLoop.class)
public abstract class BlockableEventLoopMixin {
    @Shadow @Final private String name;

    @Inject(method = "isSameThread", at = @At(value = "RETURN"), cancellable = true)
    public void onIsSameThreadCall(@NotNull CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TickThread.isTickThread());
    }

    @Inject(method = "execute", at = @At(value = "HEAD"))
    public void onExecute(Runnable runnable, @NotNull CallbackInfo ci) {
        if (Objects.equals(this.name, "Server")){
            if (Chlorophyll.server != null) {
                if (!((ITaskSchedulingMinecraftServer) Chlorophyll.server).chlorophyll$shouldPollChunkTasks()){
                    if (me.mrhua269.chlorophyll.utils.TickThread.isTickThread()) {
                        new Throwable().printStackTrace();
                    }
                }

            }
        }
    }

    @Inject(method = "pollTask", at = @At(value = "HEAD"))
    public void onPollTask(CallbackInfoReturnable<Boolean> cir) {
        if (this.name.contains("Chunk source main thread executor")){
            if (Chlorophyll.server != null) {
                if (!((ITaskSchedulingMinecraftServer) Chlorophyll.server).chlorophyll$shouldPollChunkTasks()){
                    if (!me.mrhua269.chlorophyll.utils.TickThread.isTickThread()) {
                        final me.mrhua269.chlorophyll.utils.TickThread curr = me.mrhua269.chlorophyll.utils.TickThread.currentThread();

                        if (curr.currentTickLoop != null) {
                            if (curr.currentTickLoop.getOwnedLevel().getChunkSource().mainThreadProcessor != (Object)this) {
                                System.out.println(curr.currentTickLoop.getOwnedLevel().dimension().location() + "     " + ((BlockableEventLoop)(Object)this).name());
                                new Throwable(curr.currentTickLoop.getOwnedLevel().dimension().location().toString() + "     " + ((BlockableEventLoop)(Object)this).name()).printStackTrace();
                            }
                        }else {
                            System.out.println("NULL");
                            new Throwable("NULL LOOP").printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
