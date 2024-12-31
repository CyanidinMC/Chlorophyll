package me.mrhua269.chlorophyll.utils;

import me.mrhua269.chlorophyll.impl.ChlorophyllLevelTickLoop;
import org.jetbrains.annotations.Contract;

public class TickThread extends ca.spottedleaf.moonrise.common.util.TickThread { // Bypass the default thread checks by Moonrise
    private final boolean isWorldThread;
    public ChlorophyllLevelTickLoop currentTickLoop;

    public TickThread(Runnable task, String name, boolean isWorldThread){
        super(task, name);
        this.isWorldThread = isWorldThread;
    }

    public boolean isWorldThread() {
        return this.isWorldThread;
    }

    @Contract(pure = true)
    public static TickThread currentThread(){
        if (Thread.currentThread() instanceof TickThread tickThread){
            return tickThread;
        }

        return null;
    }

    public static boolean isTickThread(){
        return Thread.currentThread() instanceof TickThread;
    }
}
