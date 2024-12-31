package me.mrhua269.chlorophyll.mixins.network;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingEntity;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
    @Redirect(method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;executeBlocking(Ljava/lang/Runnable;)V"))
    public void mainThreadRedirect$disconnect(MinecraftServer minecraftServer, Runnable runnable) {
        if (minecraftServer.isStopped()){
            minecraftServer.execute(runnable);
            return;
        }

        final ServerCommonPacketListenerImpl thisHandler = (ServerCommonPacketListenerImpl) (Object) this;

        if (thisHandler instanceof ServerGamePacketListenerImpl gamePacketListener){
            ((ITaskSchedulingLevel) gamePacketListener.player.serverLevel()).chlorophyll$getTickLoop().executeBlocking(runnable);
            return;
        }

        minecraftServer.execute(runnable);
    }

    @Inject(method = "onDisconnect", at = @At(value = "RETURN"))
    public void onDisconnect$schedulerRetire(DisconnectionDetails reason, CallbackInfo ci) {
        if (((Object) this) instanceof ServerGamePacketListenerImpl gamePacketListener) {
            ((ITaskSchedulingEntity) gamePacketListener.player).chlorophyll$getTaskScheduler().destroy();
        }
    }
}
