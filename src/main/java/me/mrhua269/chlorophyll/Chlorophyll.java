package me.mrhua269.chlorophyll;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import me.mrhua269.chlorophyll.impl.ChlorophyllLevelTickLoop;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import me.mrhua269.chlorophyll.utils.TickThread;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class Chlorophyll implements ModInitializer {
    public static final Logger logger = LogUtils.getLogger();
    private static final Map<ServerLevel, ChlorophyllLevelTickLoop> tickLoops = Maps.newConcurrentMap();
    private static final AtomicInteger threadIdGenerator = new AtomicInteger();
    public static final ScheduledThreadPoolExecutor workerPool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), task -> {
        final Thread wrapped = new TickThread(task, "Chlorophyll World Scheduler Thread - " + threadIdGenerator.getAndIncrement(), true);
        wrapped.setPriority(8);
        wrapped.setContextClassLoader(MinecraftServer.class.getClassLoader());
        return wrapped;
    });
    public static MinecraftServer server;

    public static void killAllAndAwait(){
        for (ServerLevel level : server.getAllLevels()){
            logger.info("Kill signalled to level {}", level.dimension().location());
            ((ITaskSchedulingLevel) level).chlorophyll$getTickLoop().killSignal();
        }

        for (ServerLevel level : server.getAllLevels()){
            while (((ITaskSchedulingLevel) level).chlorophyll$getTickLoop().isTicking()) {
                Thread.yield();
                LockSupport.parkNanos(1_000);
            }
        }

        workerPool.shutdown();
        while (true){
            try {
                if (workerPool.awaitTermination(1, TimeUnit.SECONDS)){
                    break;
                }
            }catch (Exception ignored){}
        }
    }

    @Override
    public void onInitialize() {

    }
}
