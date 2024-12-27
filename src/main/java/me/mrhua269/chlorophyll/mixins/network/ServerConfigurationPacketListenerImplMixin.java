package me.mrhua269.chlorophyll.mixins.network;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin {
    @Shadow public ClientInformation clientInformation;

    @Redirect(method = "handleConfigurationFinished", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V"))
    public void redirect$placeNewPlayer(PlayerList playerList, Connection connection, @NotNull ServerPlayer serverPlayer, CommonListenerCookie commonListenerCookie) {
        final ServerCommonPacketListenerImpl thisCommonHandler = (ServerCommonPacketListenerImpl) (Object) this;
        ((ITaskSchedulingLevel) serverPlayer.serverLevel()).chlorophyll$getTickLoop().schedule(() ->
                playerList.placeNewPlayer(thisCommonHandler.connection, serverPlayer, thisCommonHandler.createCookie(this.clientInformation))
        );
    }
}
