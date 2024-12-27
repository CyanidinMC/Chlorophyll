package me.mrhua269.chlorophyll.mixins.network;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingEntity;
import net.minecraft.ReportedException;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PacketUtils.class)
public abstract class PacketUtilsMixin {
    @Contract(pure = true)
    @Shadow
    public static <T extends PacketListener> @Nullable ReportedException makeReportedException(Exception exception, Packet<T> packet, T packetListener) {
        return null;
    }

    @Shadow @Final private static Logger LOGGER;

    /**
     * @author MrHua269
     * @reason Worldized tick
     */
    @Overwrite
    public static <T extends PacketListener> void ensureRunningOnSameThread(Packet<T> packet, T packetListener, @NotNull BlockableEventLoop<?> blockableEventLoop) throws RunningOnDifferentThreadException {
        Runnable scheduledHandle = () -> {
            if (packetListener.shouldHandleMessage(packet)) {
                try {
                    packet.handle(packetListener);
                } catch (Exception e) {
                    if (e instanceof ReportedException reportedException) {
                        if (reportedException.getCause() instanceof OutOfMemoryError) {
                            throw makeReportedException(e, packet, packetListener);
                        }
                    }

                    packetListener.onPacketError(packet, e);
                }
            } else {
                LOGGER.debug("Ignoring packet due to disconnection: {}", packet);
            }
        };

        if (!blockableEventLoop.isSameThread()) {
            if (packetListener instanceof ServerLoginPacketListenerImpl || packetListener instanceof ServerConfigurationPacketListenerImpl || packetListener instanceof ServerStatusPacketListenerImpl){
                blockableEventLoop.executeIfPossible(scheduledHandle);
            }else{
                ((ITaskSchedulingEntity) ((ServerGamePacketListenerImpl) packetListener).player).chlorophyll$getTaskScheduler().schedule(scheduledHandle);
            }

            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        }
    }
}
