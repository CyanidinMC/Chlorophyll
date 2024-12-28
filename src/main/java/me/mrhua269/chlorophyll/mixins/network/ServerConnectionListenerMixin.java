package me.mrhua269.chlorophyll.mixins.network;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;

@Mixin(ServerConnectionListener.class)
public class ServerConnectionListenerMixin {
    @Shadow @Final
    List<Connection> connections;

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final
    MinecraftServer server;

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public void tick() {
        synchronized(this.connections) {
            Iterator<Connection> iterator = this.connections.iterator();

            while (iterator.hasNext()) {
                final Connection connection = iterator.next();

                if (connection.isConnecting()) {
                    continue;
                }

                boolean shouldTick = connection.getPacketListener() instanceof ServerLoginPacketListenerImpl
                        || connection.getPacketListener() instanceof ServerStatusPacketListenerImpl
                        || connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl;

                if (!shouldTick) {
                    continue;
                }

                if (connection.isConnected()) {
                    try {
                        connection.tick();
                    } catch (Exception ex) {
                        LOGGER.warn("Failed to handle packet for {}", connection.getLoggableAddress(this.server.logIPs()), ex);
                        final Component component = Component.literal("Internal server error");

                        connection.send(new ClientboundDisconnectPacket(component), PacketSendListener.thenRun(() -> connection.disconnect(component)));
                        connection.setReadOnly();
                    }
                } else {
                    iterator.remove();
                    connection.handleDisconnection();
                }
            }
        }
    }

}
