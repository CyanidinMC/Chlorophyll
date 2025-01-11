package me.mrhua269.chlorophyll.mixins.commands;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingEntity;
import net.minecraft.server.commands.KillCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KillCommand.class)
public class KillCommandMixin {
    @Redirect(method = "kill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;kill(Lnet/minecraft/server/level/ServerLevel;)V"))
    private static void kill$mainThreadReturn(Entity entity, ServerLevel serverLevel) {
        ((ITaskSchedulingEntity) entity).chlorophyll$getTaskScheduler().schedule(() -> entity.kill(serverLevel));
    }
}
