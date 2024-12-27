package me.mrhua269.chlorophyll.mixins.portals;

import me.mrhua269.chlorophyll.utils.bridges.ITaskSchedulingLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Set;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
    /**
     * @author MrHua269
     * @reason Worldized ticking
     */
    @Overwrite
    public TeleportTransition getPortalDestination(@NotNull ServerLevel serverLevel, Entity entity, BlockPos blockPos) {
        ResourceKey<Level> resourceKey = serverLevel.dimension() == Level.END ? Level.OVERWORLD : Level.END;
        ServerLevel serverLevel2 = serverLevel.getServer().getLevel(resourceKey);
        if (serverLevel2 == null) {
            return null;
        } else {
            boolean bl = resourceKey == Level.END;
            BlockPos blockPos2 = bl ? ServerLevel.END_SPAWN_POINT : serverLevel2.getSharedSpawnPos();
            Vec3 vec3 = blockPos2.getBottomCenter();
            float f;
            Set<Relative> set;
            if (bl) {
                final Vec3 finalVec = vec3;
                ((ITaskSchedulingLevel) serverLevel2).chlorophyll$getTickLoop().schedule(() -> EndPlatformFeature.createEndPlatform(serverLevel2, BlockPos.containing(finalVec).below(), true));

                f = Direction.WEST.toYRot();
                set = Relative.union(Relative.DELTA, Set.of(Relative.X_ROT));
                if (entity instanceof ServerPlayer) {
                    vec3 = vec3.subtract(0.0, 1.0, 0.0);
                }
            } else {
                f = 0.0F;
                set = Relative.union(Relative.DELTA, Relative.ROTATION);
                if (entity instanceof ServerPlayer serverPlayer) {
                    return serverPlayer.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
                }

                vec3 = entity.adjustSpawnLocation(serverLevel2, blockPos2).getBottomCenter();
            }

            return new TeleportTransition(serverLevel2, vec3, Vec3.ZERO, f, 0.0F, set, TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
        }
    }
}
