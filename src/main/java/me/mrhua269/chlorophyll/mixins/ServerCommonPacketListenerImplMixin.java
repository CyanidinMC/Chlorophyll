package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
            Chlorophyll.getTickLoop(gamePacketListener.player.serverLevel()).executeBlocking(runnable);
            return;
        }

        minecraftServer.execute(runnable);
    }
}
