package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.Chlorophyll;
import me.mrhua269.chlorophyll.impl.ChlorophyllLevelTickLoop;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerLevel.class)
public class ServerLevelMixin implements ITaskSchedulingLevel {
    @Unique
    private boolean setup = false;
    @Unique
    @Final
    private final ChlorophyllLevelTickLoop tickLoop = new ChlorophyllLevelTickLoop((ServerLevel) (Object)this, Chlorophyll.workerPool);

    @Override
    public ChlorophyllLevelTickLoop chlorophyll$getTickLoop() {
        return this.tickLoop;
    }

    @Override
    public void chlorophyll$setupTickLoop() {
        if (!this.setup) {
            Chlorophyll.logger.info("Crating tick loop for level {}", ((Level)(Object) this).dimension().location());
            this.tickLoop.schedule();
            this.setup = true;
        }
    }
}
