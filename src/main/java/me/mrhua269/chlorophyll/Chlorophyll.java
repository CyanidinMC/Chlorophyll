package me.mrhua269.chlorophyll;

import com.mojang.logging.LogUtils;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import me.mrhua269.chlorophyll.utils.TickThread;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class Chlorophyll implements ModInitializer {
    public static final Logger logger = LogUtils.getLogger();

    private static final AtomicInteger threadIdGenerator = new AtomicInteger();
    public static ScheduledThreadPoolExecutor workerPool;

    private static ChlorophyllConfig chlorophyllConfig;

    public static MinecraftServer server;

    public static ChlorophyllConfig getConfig() {
        return chlorophyllConfig;
    }

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

    private static void initExecutor() {
        logger.info("Using {} threads for server level ticking.", chlorophyllConfig.worker_thread_count);

        workerPool = new ScheduledThreadPoolExecutor(chlorophyllConfig.worker_thread_count, task -> {
            final Thread wrapped = new TickThread(task, "Chlorophyll World Scheduler Thread - " + threadIdGenerator.getAndIncrement(), true);
            wrapped.setPriority(8);
            wrapped.setContextClassLoader(MinecraftServer.class.getClassLoader());
            return wrapped;
        });
    }

    @Override
    public void onInitialize() {
        AutoConfig.register(ChlorophyllConfig.class, Toml4jConfigSerializer::new);

        chlorophyllConfig = AutoConfig.getConfigHolder(ChlorophyllConfig.class).getConfig();
        initExecutor();
    }
}
