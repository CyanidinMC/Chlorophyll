package me.mrhua269.chlorophyll.mixins;

import me.mrhua269.chlorophyll.utils.TickThread;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public class LevelMixin {
    @Shadow @Final private Thread thread;

    @Shadow @Final private ResourceKey<Level> dimension;

    @Redirect(method = "getBlockEntity", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
    private Thread mainThreadCheckByPass$getBlockEntity(){
        return byPassMainThreadCheckIfTickThread();
    }

    @Unique
    private Thread byPassMainThreadCheckIfTickThread(){
        if (!TickThread.isTickThread()){
            return Thread.currentThread();
        }

        return this.thread;
    }
}
