package me.mrhua269.chlorophyll.mixins;

import ca.spottedleaf.moonrise.common.util.TickThread;
import net.minecraft.util.thread.BlockableEventLoop;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockableEventLoop.class)
public abstract class BlockableEventLoopMixin {
    @Inject(method = "isSameThread", at = @At(value = "RETURN"), cancellable = true)
    public void onIsSameThreadCall(@NotNull CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TickThread.isTickThread());
    }
}
