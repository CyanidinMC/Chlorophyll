package me.mrhua269.chlorophyll.mixins.network;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.FutureChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.Executor;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Redirect(method = "tryHandleChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;execute(Ljava/lang/Runnable;)V"))
    public void mainThreadTask$handleChat(MinecraftServer instance, Runnable runnable){
        ((ITaskSchedulingLevel) this.player.serverLevel()).chlorophyll$getTickLoop().schedule(runnable);
    }

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "Lnet/minecraft/util/FutureChain;"))
    public FutureChain chatSignFutureChain$init(Executor executor){
        return new FutureChain(((ITaskSchedulingLevel) this.player.serverLevel()).chlorophyll$getTickLoop());
    }
}
