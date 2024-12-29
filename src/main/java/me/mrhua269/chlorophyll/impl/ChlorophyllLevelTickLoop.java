package me.mrhua269.chlorophyll.impl;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingEntity;
import me.mrhua269.chlorophyll.utils.TickThread;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;

public class ChlorophyllLevelTickLoop implements Runnable, Executor {
    private static final Logger logger = LogUtils.getLogger();

    private final ServerLevel ownedLevel;
    private final ScheduledThreadPoolExecutor masterPool;
    private volatile Future<?> currentTickTask;

    private final Set<Connection> connections = Sets.newConcurrentHashSet();
    private final Queue<Runnable> taskScope = Queues.newConcurrentLinkedQueue();

    private int tickCount;
    private int autoSaveCountDown = 6000;
    private long lastTickTime;
    private volatile boolean scheduleNext = true;
    private volatile boolean isTicking = false;

    public ChlorophyllLevelTickLoop(ServerLevel ownedLevel, ScheduledThreadPoolExecutor masterPool) {
        this.ownedLevel = ownedLevel;
        this.masterPool = masterPool;
    }

    @NotNull
    public ServerLevel getOwnedLevel(){
        return this.ownedLevel;
    }

    public void killSignal(){
        this.scheduleNext = false;

        if (this.currentTickTask != null && !this.currentTickTask.isCancelled()){
            this.currentTickTask.cancel(true);
        }
    }

    public boolean isTicking(){
        return this.isTicking;
    }

    @Override
    public void run() {
        TickThread.currentThread().currentTickLoop = this;
        this.isTicking = true;

        this.tickCount++;
        final long tickStart = System.nanoTime();
        try {
            this.internalTick();
        }catch (Exception e){
            logger.error("Error while ticking level", e);
        }finally {
            TickThread.currentThread().currentTickLoop = null;
            this.isTicking = false;
            final long timeEscaped = System.nanoTime() - tickStart;
            final long sleep = 1000000000L / 20L - timeEscaped;

            this.lastTickTime = timeEscaped;

            if (this.scheduleNext){
                if (sleep > 0){
                    this.currentTickTask = this.masterPool.schedule(this, sleep, TimeUnit.NANOSECONDS);
                }else{
                    this.currentTickTask = this.masterPool.submit(this);
                }
            }else{
                logger.info("Terminated tick loop for level {}", this.ownedLevel.dimension().location());
            }
        }
    }

    public long getLastTickTime() {
        return this.lastTickTime;
    }

    private void saveLevel(){
        this.ownedLevel.save(null, false,  this.ownedLevel.noSave);

        if (this.ownedLevel == this.ownedLevel.getServer().overworld()){
            ServerLevelData serverLevelData = this.ownedLevel.getServer().getWorldData().overworldData();
            serverLevelData.setWorldBorder(this.ownedLevel.getWorldBorder().createSettings());
            this.ownedLevel.getServer().getWorldData().setCustomBossEvents(this.ownedLevel.getServer().getCustomBossEvents().save(this.ownedLevel.getServer().registryAccess()));
            this.ownedLevel.getServer().storageSource.saveDataTag(this.ownedLevel.getServer().registryAccess(), this.ownedLevel.getServer().getWorldData(), this.ownedLevel.getServer().getPlayerList().getSingleplayerData());
        }
    }

    private void savePlayers(){
        for (Connection connection : this.connections){
            final ServerPlayer player = ((ServerGamePacketListenerImpl) connection.getPacketListener()).player;
            player.getServer().getPlayerList().save(player);
        }
    }

    public void addConnection(Connection connection){
        this.connections.add(connection);
    }

    public void removeConnection(Connection connection){
        this.connections.remove(connection);
    }

    public void schedule(){
        this.masterPool.submit(this);
    }

    public Future<?> getCurrentTickTask() {
        return this.currentTickTask;
    }

    private void processMainThreadTasks(){
        Runnable task;
        while ((task = this.taskScope.poll()) != null){
            try {
                task.run();
            }catch (Throwable t){
                logger.error("Error while running main thread task", t);
            }
        }

        for (;;) {
            // Poll chunk system tasks
            if (!this.ownedLevel.getChunkSource().pollTask()) {
                break;
            }

            // Leave nothing here because we won't and shouldn't block here
        }
    }

    private void tickConnections(){
        Iterator<Connection> iterator = this.connections.iterator();

        while(iterator.hasNext()) {
            Connection connection = iterator.next();
            if (connection.isConnected()) {
                try {
                    connection.tick();
                } catch (Exception e) {
                    logger.warn("Failed to handle packet for {}", connection.getLoggableAddress(this.ownedLevel.getServer().logIPs()), e);

                    Component component = Component.literal("Internal server error");
                    connection.send(new ClientboundDisconnectPacket(component), PacketSendListener.thenRun(() -> connection.disconnect(component)));
                    connection.setReadOnly();
                }
            } else {
                iterator.remove();
                connection.handleDisconnection();
            }
        }
    }

    private void tickEntitySchedulers() {
        for (Entity entity : this.ownedLevel.getAllEntities()) {
            ((ITaskSchedulingEntity) entity).chlorophyll$getTaskScheduler().runTasks();
        }
    }

    private void internalTick(){
        this.processMainThreadTasks();

        this.tickEntitySchedulers();

        for (Connection connection : this.connections){
            ((ServerGamePacketListenerImpl) connection.getPacketListener()).suspendFlushing();
        }

        this.ownedLevel.getServer().synchronizeTime(this.ownedLevel);
        this.ownedLevel.tick(() -> true);
        this.tickConnections();

        for (Connection connection : this.connections){
            final ServerGamePacketListenerImpl packetHandler = ((ServerGamePacketListenerImpl) connection.getPacketListener());
            packetHandler.chunkSender.sendNextChunks(packetHandler.player);
            packetHandler.resumeFlushing();
        }

        this.autoSaveCountDown--;
        if (this.autoSaveCountDown <= 0){
            this.autoSaveCountDown = 6000;
            this.savePlayers();
            this.saveLevel();
        }
    }

    public void schedule(Runnable task){
        this.taskScope.offer(task);
    }

    public void executeBlocking(Runnable task){
        final TickThread tickThread = TickThread.currentThread();

        if (tickThread != null && tickThread.isWorldThread()){
            task.run();
            return;
        }

        CompletableFuture.runAsync(task, this::schedule).join();
    }

    public int getTickCount() {
        return this.tickCount;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        this.taskScope.offer(command);
    }
}
