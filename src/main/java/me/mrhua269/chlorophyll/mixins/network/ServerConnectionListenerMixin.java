package me.mrhua269.chlorophyll.mixins.network;

import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerConnectionListener.class)
public class ServerConnectionListenerMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;tick()V"))
    public void redirect$TickCheck(@NotNull Connection instance){
        boolean shouldTick = instance.getPacketListener() instanceof ServerLoginPacketListenerImpl
                || instance.getPacketListener() instanceof ServerStatusPacketListenerImpl
                || instance.getPacketListener() instanceof ServerConfigurationPacketListenerImpl;

        if (shouldTick) {
            instance.tick();
        }
    }
}
