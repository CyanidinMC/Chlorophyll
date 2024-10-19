package me.mrhua269.chlorophyll;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import me.mrhua269.chlorophyll.impl.ChlorophyllLevelTickLoop;
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
    private static final Logger logger = LogUtils.getLogger();
    private static final Map<ServerLevel, ChlorophyllLevelTickLoop> tickLoops = Maps.newConcurrentMap();
    private static final AtomicInteger threadIdGenerator = new AtomicInteger();
    private static final ScheduledThreadPoolExecutor workerPool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), task -> {
        final Thread wrapped = new TickThread(task, "Chlorophyll World Scheduler Thread - " + threadIdGenerator.getAndIncrement(), true);
        wrapped.setPriority(8);
        wrapped.setContextClassLoader(MinecraftServer.class.getClassLoader());
        return wrapped;
    });
    public static boolean shouldRunTaskOnMain = true;

    public static void killAllAndAwait(){
        for (ChlorophyllLevelTickLoop tickLoop : tickLoops.values()){
            tickLoop.killSignal();
        }

        for (ChlorophyllLevelTickLoop tickLoop : tickLoops.values()){
            while (tickLoop.isTicking()){
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

    public static ChlorophyllLevelTickLoop getTickLoop(ServerLevel level){
        return tickLoops.get(level);
    }

    public static void checkAndCreateTickLoop(ServerLevel level){
        if (!tickLoops.containsKey(level)){
            logger.info("Crating tick loop for level {}", level.dimension().location());

            final ChlorophyllLevelTickLoop created = new ChlorophyllLevelTickLoop(level, workerPool);
            tickLoops.put(level, created);
            created.schedule();
        }
    }

    @Override
    public void onInitialize() {

    }
}
