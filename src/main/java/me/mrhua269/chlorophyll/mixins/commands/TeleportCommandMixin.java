package me.mrhua269.chlorophyll.mixins.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.LookAt;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {
    @Shadow @Final private static SimpleCommandExceptionType INVALID_POSITION;

    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    private static void performTeleport(CommandSourceStack commandSourceStack, Entity entity, ServerLevel serverLevel, double d, double e, double f, Set<Relative> set, float g, float h, @Nullable LookAt lookAt) throws CommandSyntaxException {
        BlockPos blockPos = BlockPos.containing(d, e, f);
        if (!Level.isInSpawnableBounds(blockPos)) {
            throw INVALID_POSITION.create();
        } else {
            double i = set.contains(Relative.X) ? d - entity.getX() : d;
            double j = set.contains(Relative.Y) ? e - entity.getY() : e;
            double k = set.contains(Relative.Z) ? f - entity.getZ() : f;
            float l = set.contains(Relative.Y_ROT) ? g - entity.getYRot() : g;
            float m = set.contains(Relative.X_ROT) ? h - entity.getXRot() : h;
            float n = Mth.wrapDegrees(l);
            float o = Mth.wrapDegrees(m);

            ((ITaskSchedulingEntity) entity).chlorophyll$getTaskScheduler().schedule(() -> {
                if (entity.teleportTo(serverLevel, i, j, k, set, n, o, true)) {
                    if (lookAt != null) {
                        lookAt.perform(commandSourceStack, entity);
                    }

                    label46: {
                        if (entity instanceof LivingEntity) {
                            LivingEntity livingEntity = (LivingEntity)entity;
                            if (livingEntity.isFallFlying()) {
                                break label46;
                            }
                        }

                        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                        entity.setOnGround(true);
                    }

                    if (entity instanceof PathfinderMob pathfinderMob) {
                        pathfinderMob.getNavigation().stop();
                    }
                }
            });
        }
    }
}
